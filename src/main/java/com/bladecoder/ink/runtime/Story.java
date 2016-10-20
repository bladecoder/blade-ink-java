package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import com.bladecoder.ink.runtime.CallStack.Element;

/**
 * A Story is the core class that represents a complete Ink narrative, and
 * manages the evaluation and state of it.
 */
public class Story extends RTObject implements VariablesState.VariableChanged {
	/**
	 * General purpose delegate definition for bound EXTERNAL function
	 * definitions from ink. Note that this version isn't necessary if you have
	 * a function with three arguments or less - see the overloads of
	 * BindExternalFunction.
	 */
	public interface ExternalFunction {
		Object call(Object[] args) throws Exception;
	}

	// Version numbers are for engine itself and story file, rather
	// than the story state save format (which is um, currently nonexistant)
	// -- old engine, new format: always fail
	// -- new engine, old format: possibly cope, based on this number
	// When incrementing the version number above, the question you
	// should ask yourself is:
	// -- Will the engine be able to load an old story file from
	// before I made these changes to the engine?
	// If possible, you should support it, though it's not as
	// critical as loading old save games, since it's an
	// in-development problem only.

	/**
	 * Delegate definition for variable observation - see ObserveVariable.
	 */
	public interface VariableObserver {
		void call(String variableName, Object newValue);
	}

	/**
	 * The current version of the ink story file format.
	 */
	public static final int inkVersionCurrent = 15;

	/**
	 * The minimum legacy version of ink that can be loaded by the current
	 * version of the code.
	 */
	public static final int inkVersionMinimumCompatible = 12;

	private Container mainContentContainer;

	/**
	 * An ink file can provide a fallback functions for when when an EXTERNAL
	 * has been left unbound by the client, and the fallback function will be
	 * called instead. Useful when testing a story in playmode, when it's not
	 * possible to write a client-side C# external function, but you don't want
	 * it to fail to run.
	 */
	private boolean allowExternalFunctionFallbacks;

	private HashMap<String, ExternalFunction> externals;

	private boolean hasValidatedExternals;

	private StoryState state;

	private Container temporaryEvaluationContainer;

	private HashMap<String, List<VariableObserver>> variableObservers;

	// Warning: When creating a Story using this constructor, you need to
	// call ResetState on it before use. Intended for compiler use only.
	// For normal use, use the constructor that takes a json string.
	Story(Container contentContainer) {
		mainContentContainer = contentContainer;
		externals = new HashMap<String, ExternalFunction>();
	}

	/**
	 * Construct a Story Object using a JSON String compiled through inklecate.
	 */
	public Story(String jsonString) throws Exception {
		this((Container) null);
		HashMap<String, Object> rootObject = SimpleJson.textToHashMap(jsonString);

		Object versionObj = rootObject.get("inkVersion");
		if (versionObj == null)
			throw new Exception("ink version number not found. Are you sure it's a valid .ink.json file?");

		int formatFromFile = versionObj instanceof String ? Integer.parseInt((String) versionObj) : (int) versionObj;

		if (formatFromFile > inkVersionCurrent) {
			throw new Exception("Version of ink used to build story was newer than the current verison of the engine");
		} else if (formatFromFile < inkVersionMinimumCompatible) {
			throw new Exception(
					"Version of ink used to build story is too old to be loaded by this version of the engine");
		} else if (formatFromFile != inkVersionCurrent) {
			System.out.println(
					"WARNING: Version of ink used to build story doesn't match current version of engine. Non-critical, but recommend synchronising.");
		}

		Object rootToken = rootObject.get("root");
		if (rootToken == null)
			throw new Exception("Root node for ink not found. Are you sure it's a valid .ink.json file?");

		mainContentContainer = Json.jTokenToRuntimeObject(rootToken) instanceof Container
				? (Container) Json.jTokenToRuntimeObject(rootToken) : null;

		resetState();
	}

	void addError(String message, boolean useEndLineNumber) throws Exception {
		DebugMetadata dm = currentDebugMetadata();

		if (dm != null) {
			int lineNum = useEndLineNumber ? dm.endLineNumber : dm.startLineNumber;
			message = String.format("RUNTIME ERROR: '%s' line %d: %s", dm.fileName, lineNum, message);
		} else {
			message = "RUNTIME ERROR: " + message;
		}

		state.addError(message);

		// In a broken state don't need to know about any other errors.
		state.forceEnd();
	}

	void Assert(boolean condition, Object... formatParams) throws Exception {
		Assert(condition, null, formatParams);
	}

	void Assert(boolean condition, String message, Object... formatParams) throws Exception {
		if (condition == false) {
			if (message == null) {
				message = "Story assert";
			}
			if (formatParams != null && formatParams.length > 0) {
				message = String.format(message, formatParams);
			}

			throw new Exception(message + " " + currentDebugMetadata());
		}
	}

	/**
	 * Most general form of function binding that returns an Object and takes an
	 * array of Object parameters. The only way to bind a function with more
	 * than 3 arguments.
	 * 
	 * @param funcName
	 *            EXTERNAL ink function name to bind to.
	 * @param func
	 *            The Java function to bind.
	 */
	public void bindExternalFunction(String funcName, ExternalFunction func) throws Exception {
		Assert(!externals.containsKey(funcName), "Function '" + funcName + "' has already been bound.");
		externals.put(funcName, func);
	}

	@SuppressWarnings("unchecked")
	public <T> T tryCoerce(Object value, Class<T> type) throws Exception {

		if (value == null)
			return null;

		if (type.isAssignableFrom(value.getClass()))
			return (T) value;

		if (value instanceof Float && type == Integer.class) {
			Integer intVal = (int) Math.round((Float) value);
			return (T) intVal;
		}

		if (value instanceof Integer && type == Float.class) {
			Float floatVal = Float.valueOf((Integer) value);
			return (T) floatVal;
		}

		if (value instanceof Integer && type == Boolean.class) {
			int intVal = (Integer) value;
			return (T) (intVal == 0 ? new Boolean(false) : new Boolean(true));
		}

		if (type == String.class) {
			return (T) value.toString();
		}

		Assert(false, "Failed to cast " + value.getClass().getCanonicalName() + " to " + type.getCanonicalName());

		return null;
	}

	/**
	 * Get any global tags associated with the story. These are defined as hash
	 * tags defined at the very top of the story.
	 * @throws Exception 
	 */
	public List<String> getGlobalTags() throws Exception {
		return tagsAtStartOfFlowContainerWithPathString("");
	}

	/**
	 * Gets any tags associated with a particular knot or knot.stitch.
	 * These are defined as hash tags defined at the very top of a
	 * knot or stitch. 
	 * @param path The path of the knot or stitch, in the form "knot" or 
	 * "knot.stitch".
	 * @throws Exception 
	 */
	public List<String> tagsForContentAtPath(String path) throws Exception {
		return tagsAtStartOfFlowContainerWithPathString(path);
	}

	List<String> tagsAtStartOfFlowContainerWithPathString (String pathString) throws Exception {
		Path path = new Path (pathString);

            // Expected to be global story, knot or stitch
            Container flowContainer = null;
            RTObject c = contentAtPath(path);

            if (c instanceof Container)
            	flowContainer = (Container)c;

            // First element of the above constructs is a compiled weave
            Container innerWeaveContainer = null;
            
            if(flowContainer.getContent().get(0) instanceof Container)
            	innerWeaveContainer = (Container)flowContainer.getContent().get(0);

            // Any initial tag objects count as the "main tags" associated with that story/knot/stitch
            List<String> tags = null;
            for ( RTObject c2 :innerWeaveContainer.getContent()) {
                Tag tag = null;
                
                if(c2 instanceof Tag)
                	tag = (Tag)c2;
                
                if (tag != null) {
                    if (tags == null) tags = new ArrayList<String> ();
                    tags.add (tag.getText());
                } else break;
            }

            return tags;
        }

	/**
	 * Useful when debugging a (very short) story, to visualise the state of the
	 * story. Add this call as a watch and open the extended text. A left-arrow
	 * mark will denote the current point of the story. It's only recommended
	 * that this is used on very short debug stories, since it can end up
	 * generate a large quantity of text otherwise.
	 */
	public String buildStringOfHierarchy() {
		StringBuilder sb = new StringBuilder();

		mainContentContainer.buildStringOfHierarchy(sb, 0, state.getCurrentContentObject());

		return sb.toString();
	}

	void callExternalFunction(String funcName, int numberOfArguments) throws Exception {
		Container fallbackFunctionContainer = null;

		ExternalFunction func = externals.get(funcName);

		// Try to use fallback function?
		if (func == null) {
			if (allowExternalFunctionFallbacks) {

				RTObject contentAtPath = contentAtPath(new Path(funcName));
				fallbackFunctionContainer = contentAtPath instanceof Container ? (Container) contentAtPath : null;

				Assert(fallbackFunctionContainer != null, "Trying to call EXTERNAL function '" + funcName
						+ "' which has not been bound, and fallback ink function could not be found.");

				// Divert direct into fallback function and we're done
				state.getCallStack().push(PushPopType.Function);
				state.setDivertedTargetObject(fallbackFunctionContainer);
				return;

			} else {
				Assert(false, "Trying to call EXTERNAL function '" + funcName
						+ "' which has not been bound (and ink fallbacks disabled).");
			}
		}

		// Pop arguments
		ArrayList<Object> arguments = new ArrayList<Object>();
		for (int i = 0; i < numberOfArguments; ++i) {
			Value<?> poppedObj = (Value<?>) state.popEvaluationStack();
			Object valueObj = poppedObj.getValueObject();
			arguments.add(valueObj);
		}

		// Reverse arguments from the order they were popped,
		// so they're the right way round again.
		Collections.reverse(arguments);

		// Run the function!
		Object funcResult = func.call(arguments.toArray());

		// Convert return value (if any) to the a type that the ink engine can
		// use
		RTObject returnObj = null;
		if (funcResult != null) {
			returnObj = AbstractValue.create(funcResult);
			Assert(returnObj != null, "Could not create ink value from returned Object of type "
					+ funcResult.getClass().getCanonicalName());
		} else {
			returnObj = new Void();
		}

		state.pushEvaluationStack(returnObj);
	}

	/**
	 * Check whether more content is available if you were to call Continue() -
	 * i.e. are we mid story rather than at a choice point or at the end.
	 * 
	 * @return true if it's possible to call Continue()
	 */
	public boolean canContinue() {
		return state.getCurrentContentObject() != null && !state.hasError();
	}

	/**
	 * Chooses the Choice from the currentChoices list with the given index.
	 * Internally, this sets the current content path to that pointed to by the
	 * Choice, ready to continue story evaluation.
	 */
	public void chooseChoiceIndex(int choiceIdx) throws Exception {
		List<Choice> choices = getCurrentChoices();
		Assert(choiceIdx >= 0 && choiceIdx < choices.size(), "choice out of range");

		// Replace callstack with the one from the thread at the choosing point,
		// so that we can jump into the right place in the flow.
		// This is important in case the flow was forked by a new thread, which
		// can create multiple leading edges for the story, each of
		// which has its own context.
		Choice choiceToChoose = choices.get(choiceIdx);
		state.getCallStack().setCurrentThread(choiceToChoose.getThreadAtGeneration());

		choosePath(choiceToChoose.getchoicePoint().getChoiceTarget().getPath());
	}

	void choosePath(Path path) throws Exception {
		state.setChosenPath(path);

		// Take a note of newly visited containers for read counts etc
		visitChangedContainersDueToDivert();
	}

	/**
	 * Change the current position of the story to the given path. From here you
	 * can call Continue() to evaluate the next line. The path String is a
	 * dot-separated path as used ly by the engine. These examples should work:
	 *
	 * myKnot myKnot.myStitch
	 *
	 * Note however that this won't necessarily work:
	 *
	 * myKnot.myStitch.myLabelledChoice
	 *
	 * ...because of the way that content is nested within a weave structure.
	 *
	 * @param A
	 *            dot-separted path string, as specified above.
	 */
	public void choosePathString(String path) throws Exception {
		choosePath(new Path(path));
	}

	RTObject contentAtPath(Path path) throws Exception {
		return mainContentContainer().contentAtPath(path);
	}

	/**
	 * Continue the story for one line of content, if possible. If you're not
	 * sure if there's more content available, for example if you want to check
	 * whether you're at a choice point or at the end of the story, you should
	 * call canContinue before calling this function.
	 * 
	 * @return The line of text content.
	 */
	public String Continue() throws StoryException, Exception {
		// TODO: Should we leave this to the client, since it could be
		// slow to iterate through all the content an extra time?
		if (!hasValidatedExternals)
			validateExternalBindings();

		return continueInternal();
	}

	String continueInternal() throws StoryException, Exception {
		if (!canContinue()) {
			throw new StoryException("Can't continue - should check canContinue before calling Continue");
		}

		state.resetOutput();

		state.setDidSafeExit(false);
		state.getVariablesState().setbatchObservingVariableChanges(true);

		// _previousContainer = null;

		try {

			StoryState stateAtLastNewline = null;

			// The basic algorithm here is:
			//
			// do { Step() } while( canContinue && !outputStreamEndsInNewline );
			//
			// But the complexity comes from:
			// - Stepping beyond the newline in case it'll be absorbed by glue
			// later
			// - Ensuring that non-text content beyond newlines are generated -
			// i.e. choices,
			// which are actually built out of text content.
			// So we have to take a snapshot of the state, continue
			// prospectively,
			// and rewind if necessary.
			// This code is slightly fragile :-/
			//

			do {

				// Run main step function (walks through content)
				step();

				// Run out of content and we have a default invisible choice
				// that we can follow?
				if (!canContinue()) {
					tryFollowDefaultInvisibleChoice();
				}

				// Don't save/rewind during String evaluation, which is e.g.
				// used for choices
				if (!getState().inStringEvaluation()) {

					// We previously found a newline, but were we just double
					// checking that
					// it wouldn't immediately be removed by glue?
					if (stateAtLastNewline != null) {

						// Cover cases that non-text generated content was
						// evaluated last step
						String currText = getCurrentText();
						int prevTextLength = stateAtLastNewline.currentText().length();

						// Output has been extended?
						if (!currText.equals(stateAtLastNewline.currentText())) {

							// Original newline still exists?
							if (currText.length() >= prevTextLength && currText.charAt(prevTextLength - 1) == '\n') {

								restoreStateSnapshot(stateAtLastNewline);
								break;
							}

							// Newline that previously existed is no longer
							// valid - e.g.
							// glue was encounted that caused it to be removed.
							else {
								stateAtLastNewline = null;
							}
						}

					}

					// Current content ends in a newline - approaching end of
					// our evaluation
					if (getState().outputStreamEndsInNewline()) {

						// If we can continue evaluation for a bit:
						// Create a snapshot in case we need to rewind.
						// We're going to continue stepping in case we see glue
						// or some
						// non-text content such as choices.
						if (canContinue()) {
							stateAtLastNewline = stateSnapshot();
						}

						// Can't continue, so we're about to exit - make sure we
						// don't have an old state hanging around.
						else {
							stateAtLastNewline = null;
						}

					}

				}

			} while (canContinue());

			// Need to rewind, due to evaluating further than we should?
			if (stateAtLastNewline != null) {
				restoreStateSnapshot(stateAtLastNewline);
			}

			// Finished a section of content / reached a choice point?
			if (!canContinue()) {

				if (getState().getCallStack().canPopThread()) {
					error("Thread available to pop, threads should always be flat by the end of evaluation?");
				}

				if (getCurrentChoices().size() == 0 && !getState().isDidSafeExit()
						&& temporaryEvaluationContainer == null) {
					if (getState().getCallStack().canPop(PushPopType.Tunnel)) {
						error("unexpectedly reached end of content. Do you need a '->->' to return from a tunnel?");
					} else if (getState().getCallStack().canPop(PushPopType.Function)) {
						error("unexpectedly reached end of content. Do you need a '~ return'?");
					} else if (!getState().getCallStack().canPop()) {
						error("ran out of content. Do you need a '-> DONE' or '-> END'?");
					} else {
						error("unexpectedly reached end of content for unknown reason. Please debug compiler!");
					}
				}

			}

		} catch (StoryException e) {
			addError(e.getMessage(), e.useEndLineNumber);
		} finally {

			getState().setDidSafeExit(false);

			state.getVariablesState().setbatchObservingVariableChanges(false);
		}

		return getCurrentText();
	}

	/**
	 * Continue the story until the next choice point or until it runs out of
	 * content. This is as opposed to the Continue() method which only evaluates
	 * one line of output at a time.
	 * 
	 * @return The resulting text evaluated by the ink engine, concatenated
	 *         together.
	 */
	public String continueMaximally() throws StoryException, Exception {
		StringBuilder sb = new StringBuilder();

		while (canContinue()) {
			sb.append(Continue());
		}

		return sb.toString();
	}

	DebugMetadata currentDebugMetadata() {
		DebugMetadata dm;

		// Try to get from the current path first
		RTObject currentContent = state.getCurrentContentObject();
		if (currentContent != null) {
			dm = currentContent.getDebugMetadata();
			if (dm != null) {
				return dm;
			}
		}

		// Move up callstack if possible
		for (int i = state.getCallStack().getElements().size() - 1; i >= 0; --i) {
			RTObject currentObj = state.getCallStack().getElements().get(i).currentRTObject;
			if (currentObj != null && currentObj.getDebugMetadata() != null) {
				return currentObj.getDebugMetadata();
			}
		}

		// Current/previous path may not be valid if we've just had an error,
		// or if we've simply run out of content.
		// As a last resort, try to grab something from the output stream
		for (int i = state.getOutputStream().size() - 1; i >= 0; --i) {
			RTObject outputObj = state.getOutputStream().get(i);
			dm = outputObj.getDebugMetadata();
			if (dm != null) {
				return dm;
			}
		}

		return null;
	}

	int currentLineNumber() throws Exception {
		DebugMetadata dm = currentDebugMetadata();
		if (dm != null) {
			return dm.startLineNumber;
		}
		return 0;
	}

	void error(String message) throws Exception {
		error(message, false);
	}

	// Throw an exception that gets caught and causes AddError to be called,
	// then exits the flow.
	void error(String message, boolean useEndLineNumber) throws Exception {
		StoryException e = new StoryException(message);
		e.useEndLineNumber = useEndLineNumber;
		throw e;
	}

	// Evaluate a "hot compiled" piece of ink content, as used by the REPL-like
	// CommandLinePlayer.
	RTObject evaluateExpression(Container exprContainer) throws StoryException, Exception {
		int startCallStackHeight = state.getCallStack().getElements().size();

		state.getCallStack().push(PushPopType.Tunnel);

		temporaryEvaluationContainer = exprContainer;

		state.goToStart();

		int evalStackHeight = state.getEvaluationStack().size();

		Continue();

		temporaryEvaluationContainer = null;

		// Should have fallen off the end of the Container, which should
		// have auto-popped, but just in case we didn't for some reason,
		// manually pop to restore the state (including currentPath).
		if (state.getCallStack().getElements().size() > startCallStackHeight) {
			state.getCallStack().pop();
		}

		int endStackHeight = state.getEvaluationStack().size();
		if (endStackHeight > evalStackHeight) {
			return state.popEvaluationStack();
		} else {
			return null;
		}

	}

	/**
	 * The list of Choice Objects available at the current point in the Story.
	 * This list will be populated as the Story is stepped through with the
	 * Continue() method. Once canContinue becomes false, this list will be
	 * fully populated, and is usually (but not always) on the final Continue()
	 * step.
	 */
	public List<Choice> getCurrentChoices() {

		// Don't include invisible choices for external usage.
		List<Choice> choices = new ArrayList<Choice>();
		for (Choice c : state.getCurrentChoices()) {
			if (!c.getchoicePoint().isInvisibleDefault()) {
				c.setIndex(choices.size());
				choices.add(c);
			}
		}

		return choices;
	}

	/**
	 * Gets a list of tags as defined with '#' in source that were seen during
	 * the latest Continue() call.
	 */
	public List<String> getCurrentTags() {
		return state.getCurrentTags();
	}

	/**
	 * Any errors generated during evaluation of the Story.
	 */
	public List<String> getCurrentErrors() {
		return state.getCurrentErrors();
	}

	/**
	 * The latest line of text to be generated from a Continue() call.
	 */
	public String getCurrentText() {
		return state.currentText();
	}

	/**
	 * The entire current state of the story including (but not limited to):
	 *
	 * * Global variables * Temporary variables * Read/visit and turn counts *
	 * The callstack and evaluation stacks * The current threads
	 *
	 */
	public StoryState getState() {
		return state;
	}

	/**
	 * The VariablesState Object contains all the global variables in the story.
	 * However, note that there's more to the state of a Story than just the
	 * global variables. This is a convenience accessor to the full state
	 * Object.
	 */
	public VariablesState getVariablesState() {
		return state.getVariablesState();
	}

	/**
	 * Whether the currentErrors list contains any errors.
	 */
	public boolean hasError() {
		return state.hasError();
	}

	boolean incrementContentPointer() {
		boolean successfulIncrement = true;

		Element currEl = state.getCallStack().currentElement();
		currEl.currentContentIndex++;

		// Each time we step off the end, we fall out to the next container, all
		// the
		// while we're in indexed rather than named content
		while (currEl.currentContentIndex >= currEl.currentContainer.getContent().size()) {

			successfulIncrement = false;

			Container nextAncestor = currEl.currentContainer.getParent() instanceof Container
					? (Container) currEl.currentContainer.getParent() : null;

			if (nextAncestor == null) {
				break;
			}

			int indexInAncestor = nextAncestor.getContent().indexOf(currEl.currentContainer);
			if (indexInAncestor == -1) {
				break;
			}

			currEl.currentContainer = nextAncestor;
			currEl.currentContentIndex = indexInAncestor + 1;

			successfulIncrement = true;
		}

		if (!successfulIncrement)
			currEl.currentContainer = null;

		return successfulIncrement;
	}

	void incrementVisitCountForContainer(Container container) {
		String containerPathStr = container.getPath().toString();
		Integer count = state.getVisitCounts().get(containerPathStr);

		if (count == null)
			count = 0;

		count++;
		state.getVisitCounts().put(containerPathStr, count);
	}

	// Does the expression result represented by this Object evaluate to true?
	// e.g. is it a Number that's not equal to 1?
	boolean isTruthy(RTObject obj) throws Exception {
		boolean truthy = false;
		if (obj instanceof Value) {
			Value<?> val = (Value<?>) obj;

			if (val instanceof DivertTargetValue) {
				DivertTargetValue divTarget = (DivertTargetValue) val;
				error("Shouldn't use a divert target (to " + divTarget.getTargetPath()
						+ ") as a conditional value. Did you intend a function call 'likeThis()' or a read count check 'likeThis'? (no arrows)");
				return false;
			}

			return val.isTruthy();
		}
		return truthy;
	}

	/**
	 * When the named global variable changes it's value, the observer will be
	 * called to notify it of the change. Note that if the value changes
	 * multiple times within the ink, the observer will only be called once, at
	 * the end of the ink's evaluation. If, during the evaluation, it changes
	 * and then changes back again to its original value, it will still be
	 * called. Note that the observer will also be fired if the value of the
	 * variable is changed externally to the ink, by directly setting a value in
	 * story.variablesState.
	 * 
	 * @param variableName
	 *            The name of the global variable to observe.
	 * @param observer
	 *            A delegate function to call when the variable changes.
	 */
	public void observeVariable(String variableName, VariableObserver observer) {
		if (variableObservers == null)
			variableObservers = new HashMap<String, List<VariableObserver>>();

		if (variableObservers.containsKey(variableName)) {
			variableObservers.get(variableName).add(observer);
		} else {
			List<VariableObserver> l = new ArrayList<VariableObserver>();
			l.add(observer);
			variableObservers.put(variableName, l);
		}
	}

	/**
	 * Convenience function to allow multiple variables to be observed with the
	 * same observer delegate function. See the singular ObserveVariable for
	 * details. The observer will get one call for every variable that has
	 * changed.
	 * 
	 * @param variableNames
	 *            The set of variables to observe.
	 * @param The
	 *            delegate function to call when any of the named variables
	 *            change.
	 */
	public void observeVariables(List<String> variableNames, VariableObserver observer) {
		for (String varName : variableNames) {
			observeVariable(varName, observer);
		}
	}

	/**
	 * Removes the variable observer, to stop getting variable change
	 * notifications. If you pass a specific variable name, it will stop
	 * observing that particular one. If you pass null (or leave it blank, since
	 * it's optional), then the observer will be removed from all variables that
	 * it's subscribed to.
	 * 
	 * @param observer
	 *            The observer to stop observing.
	 * @param specificVariableName
	 *            (Optional) Specific variable name to stop observing.
	 */
	public void removeVariableObserver(VariableObserver observer, String specificVariableName) {
		if (variableObservers == null)
			return;

		// Remove observer for this specific variable
		if (specificVariableName != null) {
			if (variableObservers.containsKey(specificVariableName)) {
				variableObservers.get(specificVariableName).remove(observer);
			}
		} else {
			// Remove observer for all variables
			for (List<VariableObserver> obs : variableObservers.values()) {
				obs.remove(observer);
			}
		}
	}

	public void removeVariableObserver(VariableObserver observer) {
		removeVariableObserver(observer, null);
	}

	@Override
	public void variableStateDidChangeEvent(String variableName, RTObject newValueObj) throws Exception {
		if (variableObservers == null)
			return;

		List<VariableObserver> observers = variableObservers.get(variableName);

		if (observers != null) {
			if (!(newValueObj instanceof Value)) {
				throw new Exception("Tried to get the value of a variable that isn't a standard type");
			}

			Value<?> val = (Value<?>) newValueObj;

			for (VariableObserver o : observers) {
				o.call(variableName, val.getValueObject());
			}
		}
	}

	Container mainContentContainer() {
		if (temporaryEvaluationContainer != null) {
			return temporaryEvaluationContainer;
		} else {
			return mainContentContainer;
		}
	}

	private void nextContent() throws Exception {
		// Setting previousContentObject is critical for
		// VisitChangedContainersDueToDivert
		state.setPreviousContentObject(state.getCurrentContentObject());

		// Divert step?
		if (state.getDivertedTargetObject() != null) {

			state.setCurrentContentObject(state.getDivertedTargetObject());
			state.setDivertedTargetObject(null);

			// Internally uses state.previousContentObject and
			// state.currentContentObject
			visitChangedContainersDueToDivert();

			// Diverted location has valid content?
			if (state.getCurrentContentObject() != null) {
				return;
			}

			// Otherwise, if diverted location doesn't have valid content,
			// drop down and attempt to increment.
			// This can happen if the diverted path is intentionally jumping
			// to the end of a container - e.g. a Conditional that's re-joining
		}

		boolean successfulPointerIncrement = incrementContentPointer();

		// Ran out of content? Try to auto-exit from a function,
		// or finish evaluating the content of a thread
		if (!successfulPointerIncrement) {

			boolean didPop = false;

			if (state.getCallStack().canPop(PushPopType.Function)) {

				// Pop from the call stack
				state.getCallStack().pop(PushPopType.Function);

				// This pop was due to dropping off the end of a function that
				// didn't return anything,
				// so in this case, we make sure that the evaluator has
				// something to chomp on if it needs it
				if (state.getInExpressionEvaluation()) {
					state.pushEvaluationStack(new Void());
				}

				didPop = true;
			}

			else if (state.getCallStack().canPopThread()) {
				state.getCallStack().popThread();

				didPop = true;
			}

			// Step past the point where we last called out
			if (didPop && state.getCurrentContentObject() != null) {
				nextContent();
			}
		}
	}

	// Note that this is O(n), since it re-evaluates the shuffle indices
	// from a consistent seed each time.
	// TODO: Is this the best algorithm it can be?
	int nextSequenceShuffleIndex() throws Exception {
		RTObject popEvaluationStack = state.popEvaluationStack();

		IntValue numElementsIntVal = popEvaluationStack instanceof IntValue ? (IntValue) popEvaluationStack : null;

		if (numElementsIntVal == null) {
			error("expected number of elements in sequence for shuffle index");
			return 0;
		}

		Container seqContainer = state.currentContainer();

		int numElements = numElementsIntVal.value;

		IntValue seqCountVal = (IntValue) state.popEvaluationStack();
		int seqCount = seqCountVal.value;
		int loopIndex = seqCount / numElements;
		int iterationIndex = seqCount % numElements;

		// Generate the same shuffle based on:
		// - The hash of this container, to make sure it's consistent
		// each time the runtime returns to the sequence
		// - How many times the runtime has looped around this full shuffle
		String seqPathStr = seqContainer.getPath().toString();
		int sequenceHash = 0;
		for (char c : seqPathStr.toCharArray()) {
			sequenceHash += c;
		}

		int randomSeed = sequenceHash + loopIndex + state.getStorySeed();

		Random random = new Random(randomSeed);

		ArrayList<Integer> unpickedIndices = new ArrayList<Integer>();
		for (int i = 0; i < numElements; ++i) {
			unpickedIndices.add(i);
		}

		for (int i = 0; i <= iterationIndex; ++i) {
			int chosen = random.nextInt(Integer.MAX_VALUE) % unpickedIndices.size();
			int chosenIndex = unpickedIndices.get(chosen);
			unpickedIndices.remove(chosen);

			if (i == iterationIndex) {
				return chosenIndex;
			}
		}

		throw new Exception("Should never reach here");
	}

	/**
	 * Checks whether contentObj is a control or flow Object rather than a piece
	 * of content, and performs the required command if necessary.
	 * 
	 * @return true if Object was logic or flow control, false if it's normal
	 *         content.
	 * @param contentObj
	 *            Content Object.
	 */
	boolean performLogicAndFlowControl(RTObject contentObj) throws Exception {
		if (contentObj == null) {
			return false;
		}

		// Divert
		if (contentObj instanceof Divert) {

			Divert currentDivert = (Divert) contentObj;

			if (currentDivert.isConditional()) {
				RTObject conditionValue = state.popEvaluationStack();

				// False conditional? Cancel divert
				if (!isTruthy(conditionValue))
					return true;
			}

			if (currentDivert.hasVariableTarget()) {
				String varName = currentDivert.getVariableDivertName();

				RTObject varContents = state.getVariablesState().getVariableWithName(varName);

				if (!(varContents instanceof DivertTargetValue)) {

					IntValue intContent = varContents instanceof IntValue ? (IntValue) varContents : null;

					String errorMessage = "Tried to divert to a target from a variable, but the variable (" + varName
							+ ") didn't contain a divert target, it ";
					if (intContent != null && intContent.value == 0) {
						errorMessage += "was empty/null (the value 0).";
					} else {
						errorMessage += "contained '" + varContents + "'.";
					}

					error(errorMessage);
				}

				DivertTargetValue target = (DivertTargetValue) varContents;
				state.setDivertedTargetObject(contentAtPath(target.getTargetPath()));

			} else if (currentDivert.isExternal()) {
				callExternalFunction(currentDivert.getTargetPathString(), currentDivert.getExternalArgs());
				return true;
			} else {
				state.setDivertedTargetObject(currentDivert.getTargetContent());
			}

			if (currentDivert.getPushesToStack()) {
				state.getCallStack().push(currentDivert.getStackPushType());
			}

			if (state.getDivertedTargetObject() == null && !currentDivert.isExternal()) {

				// Human readable name available - runtime divert is part of a
				// hard-written divert that to missing content
				if (currentDivert != null && currentDivert.getDebugMetadata().sourceName != null) {
					error("Divert target doesn't exist: " + currentDivert.getDebugMetadata().sourceName);
				} else {
					error("Divert resolution failed: " + currentDivert);
				}
			}

			return true;
		}

		// Start/end an expression evaluation? Or print out the result?
		else if (contentObj instanceof ControlCommand) {
			ControlCommand evalCommand = (ControlCommand) contentObj;

			int choiceCount;
			switch (evalCommand.getcommandType()) {

			case EvalStart:
				Assert(state.getInExpressionEvaluation() == false, "Already in expression evaluation?");
				state.setInExpressionEvaluation(true);
				break;

			case EvalEnd:
				Assert(state.getInExpressionEvaluation() == true, "Not in expression evaluation mode");
				state.setInExpressionEvaluation(false);
				break;

			case EvalOutput:

				// If the expression turned out to be empty, there may not be
				// anything on the stack
				if (state.getEvaluationStack().size() > 0) {

					RTObject output = state.popEvaluationStack();

					// Functions may evaluate to Void, in which case we skip
					// output
					if (!(output instanceof Void)) {
						// TODO: Should we really always blanket convert to
						// string?
						// It would be okay to have numbers in the output stream
						// the
						// only problem is when exporting text for viewing, it
						// skips over numbers etc.
						StringValue text = new StringValue(output.toString());

						state.pushToOutputStream(text);
					}

				}
				break;

			case NoOp:
				break;

			case Duplicate:
				state.pushEvaluationStack(state.peekEvaluationStack());
				break;

			case PopEvaluatedValue:
				state.popEvaluationStack();
				break;

			case PopFunction:
			case PopTunnel:

				PushPopType popType = evalCommand.getcommandType() == ControlCommand.CommandType.PopFunction
						? PushPopType.Function : PushPopType.Tunnel;

				if (state.getCallStack().currentElement().type != popType || !state.getCallStack().canPop()) {

					HashMap<PushPopType, String> names = new HashMap<PushPopType, String>();
					names.put(PushPopType.Function, "function return statement (~ return)");
					names.put(PushPopType.Tunnel, "tunnel onwards statement (->->)");

					String expected = names.get(state.getCallStack().currentElement().type);
					if (!state.getCallStack().canPop()) {
						expected = "end of flow (-> END or choice)";
					}

					String errorMsg = String.format("Found %s, when expected %s", names.get(popType), expected);

					error(errorMsg);
				}

				else {
					state.getCallStack().pop();
				}
				break;

			case BeginString:
				state.pushToOutputStream(evalCommand);

				Assert(state.getInExpressionEvaluation() == true,
						"Expected to be in an expression when evaluating a string");
				state.setInExpressionEvaluation(false);
				break;

			case EndString:

				// Since we're iterating backward through the content,
				// build a stack so that when we build the string,
				// it's in the right order
				Stack<RTObject> contentStackForString = new Stack<RTObject>();

				int outputCountConsumed = 0;
				for (int i = state.getOutputStream().size() - 1; i >= 0; --i) {
					RTObject obj = state.getOutputStream().get(i);

					outputCountConsumed++;

					ControlCommand command = obj instanceof ControlCommand ? (ControlCommand) obj : null;

					if (command != null && command.getcommandType() == ControlCommand.CommandType.BeginString) {
						break;
					}

					if (obj instanceof StringValue)
						contentStackForString.push(obj);
				}

				// Consume the content that was produced for this string
				state.getOutputStream()
						.subList(state.getOutputStream().size() - outputCountConsumed, state.getOutputStream().size())
						.clear();

				// Build String out of the content we collected
				StringBuilder sb = new StringBuilder();
				while (contentStackForString.size() > 0) {
					RTObject c = contentStackForString.pop();
					sb.append(c.toString());
				}

				// Return to expression evaluation (from content mode)
				state.setInExpressionEvaluation(true);
				state.pushEvaluationStack(new StringValue(sb.toString()));
				break;

			case ChoiceCount:
				choiceCount = getCurrentChoices().size();
				state.pushEvaluationStack(new IntValue(choiceCount));
				break;

			case TurnsSince:
				RTObject target = state.popEvaluationStack();
				if (!(target instanceof DivertTargetValue)) {
					String extraNote = "";
					if (target instanceof IntValue)
						extraNote = ". Did you accidentally pass a read count ('knot_name') instead of a target ('-> knot_name')?";
					error("TURNS_SINCE expected a divert target (knot, stitch, label name), but saw " + target
							+ extraNote);
					break;
				}

				DivertTargetValue divertTarget = target instanceof DivertTargetValue ? (DivertTargetValue) target
						: null;
				Container container = contentAtPath(divertTarget.getTargetPath()) instanceof Container
						? (Container) contentAtPath(divertTarget.getTargetPath()) : null;

				int turnCount = turnsSinceForContainer(container);
				state.pushEvaluationStack(new IntValue(turnCount));
				break;

			case Random: {
				IntValue maxInt = null;

				RTObject o = state.popEvaluationStack();

				if (o instanceof IntValue)
					maxInt = (IntValue) o;

				IntValue minInt = null;

				o = state.popEvaluationStack();

				if (o instanceof IntValue)
					minInt = (IntValue) o;

				if (minInt == null)
					error("Invalid value for minimum parameter of RANDOM(min, max)");

				if (maxInt == null)
					error("Invalid value for maximum parameter of RANDOM(min, max)");

				// +1 because it's inclusive of min and max, for e.g.
				// RANDOM(1,6) for a dice roll.
				int randomRange = maxInt.value - minInt.value + 1;
				if (randomRange <= 0)
					error("RANDOM was called with minimum as " + minInt.value + " and maximum as " + maxInt.value
							+ ". The maximum must be larger");

				int resultSeed = state.getStorySeed() + state.getPreviousRandom();
				Random random = new Random(resultSeed);

				int nextRandom = random.nextInt(Integer.MAX_VALUE);
				int chosenValue = (nextRandom % randomRange) + minInt.value;
				state.pushEvaluationStack(new IntValue(chosenValue));

				// Next random number (rather than keeping the Random object
				// around)
				state.setPreviousRandom(state.getPreviousRandom() + 1);
				break;
			}

			case SeedRandom:
				IntValue seed = null;

				RTObject o = state.popEvaluationStack();

				if (o instanceof IntValue)
					seed = (IntValue) o;

				if (seed == null)
					error("Invalid value passed to SEED_RANDOM");

				// Story seed affects both RANDOM and shuffle behaviour
				state.setStorySeed(seed.value);
				state.setPreviousRandom(0);

				// SEED_RANDOM returns nothing.
				state.pushEvaluationStack(new Void());
				break;

			case VisitIndex:
				int count = visitCountForContainer(state.currentContainer()) - 1; // index
																					// not
																					// count
				state.pushEvaluationStack(new IntValue(count));
				break;

			case SequenceShuffleIndex:
				int shuffleIndex = nextSequenceShuffleIndex();
				state.pushEvaluationStack(new IntValue(shuffleIndex));
				break;

			case StartThread:
				// Handled in main step function
				break;

			case Done:

				// We may exist in the context of the initial
				// act of creating the thread, or in the context of
				// evaluating the content.
				if (state.getCallStack().canPopThread()) {
					state.getCallStack().popThread();
				}

				// In normal flow - allow safe exit without warning
				else {
					state.setDidSafeExit(true);

					// Stop flow in current thread
					state.setCurrentContentObject(null);
				}

				break;

			// Force flow to end completely
			case End:
				state.forceEnd();
				break;

			default:
				error("unhandled ControlCommand: " + evalCommand);
				break;
			}

			return true;
		}

		// Variable assignment
		else if (contentObj instanceof VariableAssignment) {
			VariableAssignment varAss = (VariableAssignment) contentObj;
			RTObject assignedVal = state.popEvaluationStack();

			// When in temporary evaluation, don't create new variables purely
			// within
			// the temporary context, but attempt to create them globally
			// var prioritiseHigherInCallStack = _temporaryEvaluationContainer
			// != null;

			state.getVariablesState().assign(varAss, assignedVal);

			return true;
		}

		// Variable reference
		else if (contentObj instanceof VariableReference) {
			VariableReference varRef = (VariableReference) contentObj;
			RTObject foundValue = null;

			// Explicit read count value
			if (varRef.getPathForCount() != null) {

				Container container = varRef.getContainerForCount();
				int count = visitCountForContainer(container);
				foundValue = new IntValue(count);
			}

			// Normal variable reference
			else {

				foundValue = state.getVariablesState().getVariableWithName(varRef.getName());

				if (foundValue == null) {
					error("Uninitialised variable: " + varRef.getName());
					foundValue = new IntValue(0);
				}
			}

			state.getEvaluationStack().add(foundValue);

			return true;
		}

		// Native function call
		else if (contentObj instanceof NativeFunctionCall) {
			NativeFunctionCall func = (NativeFunctionCall) contentObj;
			List<RTObject> funcParams = state.popEvaluationStack(func.getNumberOfParameters());

			RTObject result = func.call(funcParams);
			state.getEvaluationStack().add(result);
			return true;
		}

		// No control content, must be ordinary content
		return false;
	}

	Choice processChoice(ChoicePoint choicePoint) throws Exception {
		boolean showChoice = true;

		// Don't create choice if choice point doesn't pass conditional
		if (choicePoint.hasCondition()) {
			RTObject conditionValue = state.popEvaluationStack();
			if (!isTruthy(conditionValue)) {
				showChoice = false;
			}
		}

		String startText = "";
		String choiceOnlyText = "";

		if (choicePoint.hasChoiceOnlyContent()) {
			StringValue choiceOnlyStrVal = (StringValue) state.popEvaluationStack();
			choiceOnlyText = choiceOnlyStrVal.value;
		}

		if (choicePoint.hasStartContent()) {
			StringValue startStrVal = (StringValue) state.popEvaluationStack();
			startText = startStrVal.value;
		}

		// Don't create choice if player has already read this content
		if (choicePoint.isOnceOnly()) {
			int visitCount = visitCountForContainer(choicePoint.getChoiceTarget());
			if (visitCount > 0) {
				showChoice = false;
			}
		}

		Choice choice = new Choice(choicePoint);
		choice.setThreadAtGeneration(state.getCallStack().getcurrentThread().copy());

		// We go through the full process of creating the choice above so
		// that we consume the content for it, since otherwise it'll
		// be shown on the output stream.
		if (!showChoice) {
			return null;
		}

		// Set final text for the choice
		choice.setText(startText + choiceOnlyText);

		return choice;
	}

	void recordTurnIndexVisitToContainer(Container container) {
		String containerPathStr = container.getPath().toString();
		state.getTurnIndices().put(containerPathStr, state.getCurrentTurnIndex());
	}

	/**
	 * Unwinds the callstack. Useful to reset the Story's evaluation without
	 * actually changing any meaningful state, for example if you want to exit a
	 * section of story prematurely and tell it to go elsewhere with a call to
	 * ChoosePathString(...). Doing so without calling ResetCallstack() could
	 * cause unexpected issues if, for example, the Story was in a tunnel
	 * already.
	 */
	public void resetCallstack() throws Exception {
		state.forceEnd();
	}

	/**
	 * Reset the runtime error list within the state.
	 */
	public void resetErrors() {
		state.resetErrors();
	}

	void resetGlobals() throws Exception {
		if (mainContentContainer.getNamedContent().containsKey("global decl")) {
			Path originalPath = getState().getCurrentPath();

			choosePathString("global decl");

			// Continue, but without validating external bindings,
			// since we may be doing this reset at initialisation time.
			continueInternal();

			getState().setCurrentPath(originalPath);
		}
	}

	/**
	 * Reset the Story back to its initial state as it was when it was first
	 * constructed.
	 */
	public void resetState() throws Exception {
		state = new StoryState(this);

		state.getVariablesState().setVariableChangedEvent(this);

		resetGlobals();
	}

	void restoreStateSnapshot(StoryState state) {
		this.state = state;
	}

	StoryState stateSnapshot() throws Exception {
		return state.copy();
	}

	void step() throws Exception {
		boolean shouldAddToStream = true;

		// Get current content
		RTObject currentContentObj = state.getCurrentContentObject();
		if (currentContentObj == null) {
			return;
		}

		// Step directly to the first element of content in a container (if
		// necessary)
		Container currentContainer = currentContentObj instanceof Container ? (Container) currentContentObj : null;

		while (currentContainer != null) {

			// Mark container as being entered
			visitContainer(currentContainer, true);

			// No content? the most we can do is step past it
			if (currentContainer.getContent().size() == 0)
				break;

			currentContentObj = currentContainer.getContent().get(0);
			state.getCallStack().currentElement().currentContentIndex = 0;
			state.getCallStack().currentElement().currentContainer = currentContainer;

			currentContainer = currentContentObj instanceof Container ? (Container) currentContentObj : null;
		}
		currentContainer = state.getCallStack().currentElement().currentContainer;

		// Is the current content Object:
		// - Normal content
		// - Or a logic/flow statement - if so, do it
		// Stop flow if we hit a stack pop when we're unable to pop (e.g.
		// return/done statement in knot
		// that was diverted to rather than called as a function)
		boolean isLogicOrFlowControl = performLogicAndFlowControl(currentContentObj);

		// Has flow been forced to end by flow control above?
		if (state.getCurrentContentObject() == null) {
			return;
		}

		if (isLogicOrFlowControl) {
			shouldAddToStream = false;
		}

		// Choice with condition?
		ChoicePoint choicePoint = currentContentObj instanceof ChoicePoint ? (ChoicePoint) currentContentObj : null;
		if (choicePoint != null) {
			Choice choice = processChoice(choicePoint);
			if (choice != null) {
				state.getCurrentChoices().add(choice);
			}

			currentContentObj = null;
			shouldAddToStream = false;
		}

		// If the container has no content, then it will be
		// the "content" itself, but we skip over it.
		if (currentContentObj instanceof Container) {
			shouldAddToStream = false;
		}

		// Content to add to evaluation stack or the output stream
		if (shouldAddToStream) {

			// If we're pushing a variable pointer onto the evaluation stack,
			// ensure that it's specific
			// to our current (possibly temporary) context index. And make a
			// copy of the pointer
			// so that we're not editing the original runtime Object.
			VariablePointerValue varPointer = currentContentObj instanceof VariablePointerValue
					? (VariablePointerValue) currentContentObj : null;

			if (varPointer != null && varPointer.getContextIndex() == -1) {

				// Create new Object so we're not overwriting the story's own
				// data
				int contextIdx = state.getCallStack().contextForVariableNamed(varPointer.getVariableName());
				currentContentObj = new VariablePointerValue(varPointer.getVariableName(), contextIdx);
			}

			// Expression evaluation content
			if (state.getInExpressionEvaluation()) {
				state.pushEvaluationStack(currentContentObj);
			}
			// Output stream content (i.e. not expression evaluation)
			else {
				state.pushToOutputStream(currentContentObj);
			}
		}

		// Increment the content pointer, following diverts if necessary
		nextContent();

		// Starting a thread should be done after the increment to the content
		// pointer,
		// so that when returning from the thread, it returns to the content
		// after this instruction.
		ControlCommand controlCmd = currentContentObj instanceof ControlCommand ? (ControlCommand) currentContentObj
				: null;
		if (controlCmd != null && controlCmd.getcommandType() == ControlCommand.CommandType.StartThread) {
			state.getCallStack().pushThread();
		}
	}

	/**
	 * The Story itself in JSON representation.
	 */
	public String toJsonString() throws Exception {
		List<?> rootContainerJsonList = (List<?>) Json.runtimeObjectToJToken(mainContentContainer);

		HashMap<String, Object> rootObject = new HashMap<String, Object>();
		rootObject.put("inkVersion", inkVersionCurrent);
		rootObject.put("root", rootContainerJsonList);

		return SimpleJson.HashMapToText(rootObject);
	}

	boolean tryFollowDefaultInvisibleChoice() throws Exception {
		List<Choice> allChoices = state.getCurrentChoices();

		// Is a default invisible choice the ONLY choice?
		// var invisibleChoices = allChoices.Where (c =>
		// c.choicePoint.isInvisibleDefault).ToList();
		ArrayList<Choice> invisibleChoices = new ArrayList<Choice>();
		for (Choice c : allChoices) {
			if (c.getchoicePoint().isInvisibleDefault()) {
				invisibleChoices.add(c);
			}
		}

		if (invisibleChoices.size() == 0 || allChoices.size() > invisibleChoices.size())
			return false;

		Choice choice = invisibleChoices.get(0);

		choosePath(choice.getchoicePoint().getChoiceTarget().getPath());

		return true;
	}

	int turnsSinceForContainer(Container container) throws Exception {
		if (!container.getTurnIndexShouldBeCounted()) {
			error("TURNS_SINCE() for target (" + container.getName() + " - on " + container.getDebugMetadata()
					+ ") unknown. The story may need to be compiled with countAllVisits flag (-c).");
		}

		String containerPathStr = container.getPath().toString();
		Integer index = state.getTurnIndices().get(containerPathStr);
		if (index != null) {
			return state.getCurrentTurnIndex() - index;
		} else {
			return -1;
		}
	}

	/**
	 * Remove a binding for a named EXTERNAL ink function.
	 */
	public void unbindExternalFunction(String funcName) throws Exception {
		Assert(externals.containsKey(funcName), "Function '" + funcName + "' has not been bound.");
		externals.remove(funcName);
	}

	/**
	 * Check that all EXTERNAL ink functions have a valid bound C# function.
	 * Note that this is automatically called on the first call to Continue().
	 */
	public void validateExternalBindings() throws Exception {
		validateExternalBindings(mainContentContainer);
		hasValidatedExternals = true;
	}

	void validateExternalBindings(Container c) throws Exception {
		for (RTObject innerContent : c.getContent()) {
			validateExternalBindings(innerContent);
		}
		for (INamedContent innerKeyValue : c.getNamedContent().values()) {
			validateExternalBindings(innerKeyValue instanceof RTObject ? (RTObject) innerKeyValue : null);
		}
	}

	void validateExternalBindings(RTObject o) throws Exception {
		Container container = o instanceof Container ? (Container) o : null;

		if (container != null) {
			validateExternalBindings(container);
			return;
		}

		Divert divert = o instanceof Divert ? (Divert) o : null;

		if (divert != null && divert.isExternal()) {
			String name = divert.getTargetPathString();

			if (!externals.containsKey(name)) {

				INamedContent fallbackFunction = mainContentContainer().getNamedContent().get(name);

				String message = null;

				if (!allowExternalFunctionFallbacks)
					message = "Missing function binding for external '" + name + "' (ink fallbacks disabled)";
				else if (fallbackFunction == null) {
					message = "Missing function binding for external '" + name
							+ "', and no fallback ink function found.";
				}

				if (message != null) {
					String errorPreamble = "ERROR: ";
					if (divert.getDebugMetadata() != null) {
						errorPreamble += String.format("'%s' line %d: ", divert.getDebugMetadata().fileName,
								divert.getDebugMetadata().startLineNumber);
					}

					throw new StoryException(errorPreamble + message);
				}
			}
		}
	}

	void visitChangedContainersDueToDivert() {
		RTObject previousContentObject = state.getPreviousContentObject();
		RTObject newContentObject = state.getCurrentContentObject();

		if (newContentObject == null)
			return;

		// First, find the previously open set of containers
		HashSet<Container> prevContainerSet = new HashSet<Container>();
		if (previousContentObject != null) {

			Container prevAncestor = null;

			if (previousContentObject instanceof Container) {
				prevAncestor = (Container) previousContentObject;
			} else if (previousContentObject.getParent() instanceof Container) {
				prevAncestor = (Container) previousContentObject.getParent();
			}

			while (prevAncestor != null) {
				prevContainerSet.add(prevAncestor);
				prevAncestor = prevAncestor.getParent() instanceof Container ? (Container) prevAncestor.getParent()
						: null;
			}
		}

		// If the new Object is a container itself, it will be visited
		// automatically at the next actual
		// content step. However, we need to walk up the new ancestry to see if
		// there are more new containers
		RTObject currentChildOfContainer = newContentObject;
		Container currentContainerAncestor = currentChildOfContainer.getParent() instanceof Container
				? (Container) currentChildOfContainer.getParent() : null;

		while (currentContainerAncestor != null && !prevContainerSet.contains(currentContainerAncestor)) {

			// Check whether this ancestor container is being entered at the
			// start,
			// by checking whether the child Object is the first.
			boolean enteringAtStart = currentContainerAncestor.getContent().size() > 0
					&& currentChildOfContainer == currentContainerAncestor.getContent().get(0);

			// Mark a visit to this container
			visitContainer(currentContainerAncestor, enteringAtStart);

			currentChildOfContainer = currentContainerAncestor;
			currentContainerAncestor = currentContainerAncestor.getParent() instanceof Container
					? (Container) currentContainerAncestor.getParent() : null;

		}
	}

	// Mark a container as having been visited
	void visitContainer(Container container, boolean atStart) {
		if (!container.getCountingAtStartOnly() || atStart) {
			if (container.getVisitsShouldBeCounted())
				incrementVisitCountForContainer(container);

			if (container.getTurnIndexShouldBeCounted())
				recordTurnIndexVisitToContainer(container);
		}
	}

	int visitCountForContainer(Container container) throws Exception {
		if (!container.getVisitsShouldBeCounted()) {
			error("Read count for target (" + container.getName() + " - on " + container.getDebugMetadata()
					+ ") unknown. The story may need to be compiled with countAllVisits flag (-c).");
			return 0;
		}

		String containerPathStr = container.getPath().toString();
		Integer count = state.getVisitCounts().get(containerPathStr);
		return count == null ? 0 : count;
	}

	public boolean allowExternalFunctionFallbacks() {
		return allowExternalFunctionFallbacks;
	}

	public void setAllowExternalFunctionFallbacks(boolean allowExternalFunctionFallbacks) {
		this.allowExternalFunctionFallbacks = allowExternalFunctionFallbacks;
	}

	/**
	 * Evaluates a function defined in ink.
	 * 
	 * @param functionName
	 *            The name of the function as declared in ink.
	 * @param arguments
	 *            The arguments that the ink function takes, if any. Note that
	 *            we don't (can't) do any validation on the number of arguments
	 *            right now, so make sure you get it right!
	 * @return The return value as returned from the ink function with `~ return
	 *         myValue`, or null if nothing is returned.
	 * @throws Exception
	 */
	public Object evaluateFunction(String functionName, Object[] arguments) throws Exception {
		return evaluateFunction(functionName, null, arguments);
	}

	/**
	 * Checks if a function exists.
	 * 
	 * @returns True if the function exists, else false.
	 * @param functionName
	 *            The name of the function as declared in ink.
	 */
	public boolean hasFunction(String functionName) {
		try {
			return contentAtPath(new Path(functionName)) instanceof Container;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Evaluates a function defined in ink, and gathers the possibly multi-line
	 * text as generated by the function.
	 * 
	 * @param arguments
	 *            The arguments that the ink function takes, if any. Note that
	 *            we don't (can't) do any validation on the number of arguments
	 *            right now, so make sure you get it right!
	 * @param functionName
	 *            The name of the function as declared in ink.
	 * @param textOutput
	 *            This text output is any text written as normal content within
	 *            the function, as opposed to the return value, as returned with
	 *            `~ return`.
	 * @return The return value as returned from the ink function with `~ return
	 *         myValue`, or null if nothing is returned.
	 * @throws Exception
	 */
	public Object evaluateFunction(String functionName, StringBuffer textOutput, Object[] arguments) throws Exception {
		Container funcContainer = null;

		if (functionName == null) {
			throw new Exception("Function is null");
		} else if (functionName.trim().isEmpty()) {
			throw new Exception("Function is empty or white space.");
		}

		try {
			RTObject contentAtPath = contentAtPath(new Path(functionName));
			if (contentAtPath instanceof Container)
				funcContainer = (Container) contentAtPath;
		} catch (StoryException e) {
			if (e.getMessage().contains("not found"))
				throw new Exception("Function doesn't exist: '" + functionName + "'");
			else
				throw e;
		}

		// We'll start a new callstack, so keep hold of the original,
		// as well as the evaluation stack so we know if the function
		// returned something
		CallStack originalCallstack = state.getCallStack();
		int originalEvaluationStackHeight = state.getEvaluationStack().size();

		// Create a new base call stack element.
		// By making it point at element 0 of the base, when NextContent is
		// called, it'll actually step past the entire content of the game (!)
		// and straight onto the Done. Bit of a hack :-/ We don't really have
		// a better way of creating a temporary context that ends correctly.
		state.setCallStack(new CallStack(mainContentContainer));
		state.getCallStack().currentElement().currentContainer = mainContentContainer;
		state.getCallStack().currentElement().currentContentIndex = 0;

		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (!(arguments[i] instanceof Integer || arguments[i] instanceof Float
						|| arguments[i] instanceof String)) {
					throw new IllegalArgumentException(
							"ink arguments when calling EvaluateFunction must be int, float or string");
				}

				state.getEvaluationStack().add(Value.create(arguments[i]));
			}
		}

		// Jump into the function!
		state.getCallStack().push(PushPopType.Function);
		state.setCurrentContentObject(funcContainer);

		// Evaluate the function, and collect the string output
		while (canContinue()) {
			String text = Continue();

			if (textOutput != null)
				textOutput.append(text);
		}

		// Restore original stack
		state.setCallStack(originalCallstack);

		// Do we have a returned value?
		// Potentially pop multiple values off the stack, in case we need
		// to clean up after ourselves (e.g. caller of EvaluateFunction may
		// have passed too many arguments, and we currently have no way to check
		// for that)
		RTObject returnedObj = null;
		while (state.getEvaluationStack().size() > originalEvaluationStackHeight) {
			RTObject poppedObj = state.popEvaluationStack();
			if (returnedObj == null)
				returnedObj = poppedObj;
		}

		if (returnedObj != null) {
			if (returnedObj instanceof Void)
				return null;

			// Some kind of value, if not void
			Value<?> returnVal = (Value<?>) returnedObj;

			// DivertTargets get returned as the string of components
			// (rather than a Path, which isn't public)
			if (returnVal.getValueType() == ValueType.DivertTarget) {
				return returnVal.getValueObject().toString();
			}

			// Other types can just have their exact object type:
			// int, float, string. VariablePointers get returned as strings.
			return returnVal.getValueObject();

		}

		return null;
	}
}
