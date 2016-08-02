package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import com.bladecoder.ink.runtime.CallStack.Element;

/// <summary>
/// A Story is the core class that represents a complete Ink narrative, and
/// manages the evaluation and state of it.
/// </summary>
public class Story extends RTObject {
	/// <summary>
	/// The current version of the ink story file format.
	/// </summary>
	public static final int inkVersionCurrent = 12;

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

	/// <summary>
	/// The minimum legacy version of ink that can be loaded by the current
	/// version of the code.
	/// </summary>
	public static final int inkVersionMinimumCompatible = 12;

	/// <summary>
	/// The list of Choice Objects available at the current point in
	/// the Story. This list will be populated as the Story is stepped
	/// through with the Continue() method. Once canContinue becomes
	/// false, this list will be fully populated, and is usually
	/// (but not always) on the final Continue() step.
	/// </summary>
	public List<Choice> getCurrentChoices() {

		// Don't include invisible choices for external usage.
		List choices = new ArrayList<Choice>();
		for (Choice c : _state.currentChoices) {
			if (!c.getchoicePoint().getisInvisibleDefault()) {
				c.setindex(choices.size());
				choices.add(c);
			}
		}
		return choices;
	}

	/// <summary>
	/// The latest line of text to be generated from a Continue() call.
	/// </summary>
	public String getCurrentText() {
		return _state.currentText();
	}

	/// <summary>
	/// Any errors generated during evaluation of the Story.
	/// </summary>
	public List<String> getCurrentErrors() {
		return _state.currentErrors;
	}

	/// <summary>
	/// Whether the currentErrors list contains any errors.
	/// </summary>
	public boolean hasError() {
		return _state.hasError();
	}

	/// <summary>
	/// The VariablesState Object contains all the global variables in the
	/// story.
	/// However, note that there's more to the state of a Story than just the
	/// global variables. This is a convenience accessor to the full state
	/// Object.
	/// </summary>
	public VariablesState getVariablesState() {
		return _state.variablesState;
	}

	/// <summary>
	/// The entire current state of the story including (but not limited to):
	///
	/// * Global variables
	/// * Temporary variables
	/// * Read/visit and turn counts
	/// * The callstack and evaluation stacks
	/// * The current threads
	///
	/// </summary>
	public StoryState getState() {
		return _state;
	}

	// Warning: When creating a Story using this constructor, you need to
	// call ResetState on it before use. Intended for compiler use only.
	// For normal use, use the constructor that takes a json string.
	Story(Container contentContainer) {
		_mainContentContainer = contentContainer;
		_externals = new HashMap<String, ExternalFunction>();
	}

	/// <summary>
	/// Construct a Story Object using a JSON String compiled through inklecate.
	/// </summary>
	public Story(String jsonString) throws Exception {
		this((Container) null);
		HashMap<String, Object> rootObject = SimpleJson.textToHashMap(jsonString);

		Object versionObj = rootObject.get("inkVersion");
		if (versionObj == null)
			throw new Exception("ink version number not found. Are you sure it's a valid .ink.json file?");

		int formatFromFile = (int) versionObj;
		if (formatFromFile > inkVersionCurrent) {
			throw new Exception("Version of ink used to build story was newer than the current verison of the engine");
		} else if (formatFromFile < inkVersionMinimumCompatible) {
			throw new Exception(
					"Version of ink used to build story is too old to be loaded by this verison of the engine");
		} else if (formatFromFile != inkVersionCurrent) {
			System.out.println(
					"WARNING: Version of ink used to build story doesn't match current version of engine. Non-critical, but recommend synchronising.");
		}

		Object rootToken = rootObject.get("root");
		if (rootToken == null)
			throw new Exception("Root node for ink not found. Are you sure it's a valid .ink.json file?");

		_mainContentContainer = (Container) Json.jTokenToRuntimeRTObject(rootToken);

		ResetState();
	}

	/// <summary>
	/// The Story itself in JSON representation.
	/// </summary>
	public String ToJsonString() throws Exception {
		List<Object> rootContainerJsonList = (List<Object>) Json.runtimeRTObjectToJToken(_mainContentContainer);

		HashMap<String, Object> rootObject = new HashMap<String, Object>();
		rootObject.put("inkVersion", inkVersionCurrent);
		rootObject.put("root", rootContainerJsonList);

		return SimpleJson.HashMapToText(rootObject);
	}

	/// <summary>
	/// Reset the Story back to its initial state as it was when it was
	/// first constructed.
	/// </summary>
	public void ResetState() throws Exception {
		_state = new StoryState(this);

		// TODO
		// _state.variablesState.variableChangedEvent +=
		// VariableStateDidChangeEvent;

		ResetGlobals();
	}

	/// <summary>
	/// Reset the runtime error list within the state.
	/// </summary>
	public void ResetErrors() {
		_state.ResetErrors();
	}

	/// <summary>
	/// Unwinds the callstack. Useful to reset the Story's evaluation
	/// without actually changing any meaningful state, for example if
	/// you want to exit a section of story prematurely and tell it to
	/// go elsewhere with a call to ChoosePathString(...).
	/// Doing so without calling ResetCallstack() could cause unexpected
	/// issues if, for example, the Story was in a tunnel already.
	/// </summary>
	public void ResetCallstack() throws Exception {
		_state.ForceEndFlow();
	}

	void ResetGlobals() throws Exception {
		if (_mainContentContainer.getnamedContent().containsKey("global decl")) {
			Path originalPath = getState().getcurrentPath();

			ChoosePathString("global decl");

			// Continue, but without validating external bindings,
			// since we may be doing this reset at initialisation time.
			ContinueInternal();

			getState().setcurrentPath(originalPath);
		}
	}

	/// <summary>
	/// Continue the story for one line of content, if possible.
	/// If you're not sure if there's more content available, for example if you
	/// want to check whether you're at a choice point or at the end of the
	/// story,
	/// you should call <c>canContinue</c> before calling this function.
	/// </summary>
	/// <returns>The line of text content.</returns>
	public String Continue() throws StoryException, Exception {
		// TODO: Should we leave this to the client, since it could be
		// slow to iterate through all the content an extra time?
		if (!_hasValidatedExternals)
			ValidateExternalBindings();

		return ContinueInternal();
	}

	String ContinueInternal() throws StoryException, Exception {
		if (!canContinue()) {
			throw new StoryException("Can't continue - should check canContinue before calling Continue");
		}

		_state.ResetOutput();

		_state.didSafeExit = false;
		// TODO
		// _state.variablesState.batchObservingVariableChanges = true;

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
				Step();

				// Run out of content and we have a default invisible choice
				// that we can follow?
				if (!canContinue()) {
					TryFollowDefaultInvisibleChoice();
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

								RestoreStateSnapshot(stateAtLastNewline);
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
							stateAtLastNewline = StateSnapshot();
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
				RestoreStateSnapshot(stateAtLastNewline);
			}

			// Finished a section of content / reached a choice point?
			if (!canContinue()) {

				if (getState().callStack.canPopThread()) {
					Error("Thread available to pop, threads should always be flat by the end of evaluation?");
				}

				if (getCurrentChoices().size() == 0 && !getState().didSafeExit) {
					if (getState().callStack.CanPop(PushPopType.Tunnel)) {
						Error("unexpectedly reached end of content. Do you need a '->->' to return from a tunnel?");
					} else if (getState().callStack.CanPop(PushPopType.Function)) {
						Error("unexpectedly reached end of content. Do you need a '~ return'?");
					} else if (!getState().callStack.canPop()) {
						Error("ran out of content. Do you need a '-> DONE' or '-> END'?");
					} else {
						Error("unexpectedly reached end of content for unknown reason. Please debug compiler!");
					}
				}

			}

		} catch (StoryException e) {
			AddError(e.getMessage(), e.useEndLineNumber);
		} finally {

			getState().didSafeExit = false;

			_state.variablesState.setbatchObservingVariableChanges(false);
		}

		return getCurrentText();
	}

	/// <summary>
	/// Check whether more content is available if you were to call
	/// <c>Continue()</c> - i.e.
	/// are we mid story rather than at a choice point or at the end.
	/// </summary>
	/// <value><c>true</c> if it's possible to call <c>Continue()</c>.</value>
	public boolean canContinue() throws Exception {
		return _state.getcurrentContentObject() != null && !_state.hasError();
	}

	/// <summary>
	/// Continue the story until the next choice point or until it runs out of
	/// content.
	/// This is as opposed to the Continue() method which only evaluates one
	/// line of
	/// output at a time.
	/// </summary>
	/// <returns>The resulting text evaluated by the ink engine, concatenated
	/// together.</returns>
	public String ContinueMaximally() throws StoryException, Exception {
		StringBuilder sb = new StringBuilder();

		while (canContinue()) {
			sb.append(Continue());
		}

		return sb.toString();
	}

	RTObject ContentAtPath(Path path) throws Exception {
		return mainContentContainer().contentAtPath(path);
	}

	StoryState StateSnapshot() throws Exception {
		return _state.Copy();
	}

	void RestoreStateSnapshot(StoryState state) {
		_state = state;
	}

	void Step() throws Exception {
		boolean shouldAddToStream = true;

		// Get current content
		RTObject currentContentObj = _state.getcurrentContentObject();
		if (currentContentObj == null) {
			return;
		}

		// Step directly to the first element of content in a container (if
		// necessary)
		Container currentContainer = (Container) currentContentObj;
		while (currentContainer != null) {

			// Mark container as being entered
			VisitContainer(currentContainer, true);

			// No content? the most we can do is step past it
			if (currentContainer._content.size() == 0)
				break;

			currentContentObj = currentContainer._content.get(0);
			_state.callStack.currentElement().currentContentIndex = 0;
			_state.callStack.currentElement().currentContainer = currentContainer;

			currentContainer = (Container) currentContentObj;
		}
		currentContainer = _state.callStack.currentElement().currentContainer;

		// Is the current content Object:
		// - Normal content
		// - Or a logic/flow statement - if so, do it
		// Stop flow if we hit a stack pop when we're unable to pop (e.g.
		// return/done statement in knot
		// that was diverted to rather than called as a function)
		boolean isLogicOrFlowControl = PerformLogicAndFlowControl(currentContentObj);

		// Has flow been forced to end by flow control above?
		if (_state.getcurrentContentObject() == null) {
			return;
		}

		if (isLogicOrFlowControl) {
			shouldAddToStream = false;
		}

		// Choice with condition?
		ChoicePoint choicePoint = (ChoicePoint) currentContentObj;
		if (choicePoint != null) {
			Choice choice = ProcessChoice(choicePoint);
			if (choice != null) {
				_state.currentChoices.add(choice);
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
			VariablePointerValue varPointer = (VariablePointerValue) currentContentObj;
			if (varPointer != null && varPointer.getcontextIndex() == -1) {

				// Create new Object so we're not overwriting the story's own
				// data
				int contextIdx = _state.callStack.ContextForVariableNamed(varPointer.getvariableName());
				currentContentObj = new VariablePointerValue(varPointer.getvariableName(), contextIdx);
			}

			// Expression evaluation content
			if (_state.getinExpressionEvaluation()) {
				_state.PushEvaluationStack(currentContentObj);
			}
			// Output stream content (i.e. not expression evaluation)
			else {
				_state.PushToOutputStream(currentContentObj);
			}
		}

		// Increment the content pointer, following diverts if necessary
		NextContent();

		// Starting a thread should be done after the increment to the content
		// pointer,
		// so that when returning from the thread, it returns to the content
		// after this instruction.
		ControlCommand controlCmd = (ControlCommand) currentContentObj;
		if (controlCmd != null && controlCmd.getcommandType() == ControlCommand.CommandType.StartThread) {
			_state.callStack.PushThread();
		}
	}

	// Mark a container as having been visited
	void VisitContainer(Container container, boolean atStart) {
		if (!container.getcountingAtStartOnly() || atStart) {
			if (container.getvisitsShouldBeCounted())
				IncrementVisitCountForContainer(container);

			if (container.getturnIndexShouldBeCounted())
				RecordTurnIndexVisitToContainer(container);
		}
	}

	void VisitChangedContainersDueToDivert() throws Exception {
		RTObject previousContentObject = _state.getpreviousContentObject();
		RTObject newContentObject = _state.getcurrentContentObject();

		if (newContentObject == null)
			return;

		// First, find the previously open set of containers
		HashSet<Container> prevContainerSet = new HashSet<Container>();
		if (previousContentObject != null) {
			Container prevAncestor = (previousContentObject instanceof Container ? (Container) previousContentObject
					: (Container) previousContentObject.getparent());
			while (prevAncestor != null) {
				prevContainerSet.add(prevAncestor);
				prevAncestor = (Container) prevAncestor.getparent();
			}
		}

		// If the new Object is a container itself, it will be visited
		// automatically at the next actual
		// content step. However, we need to walk up the new ancestry to see if
		// there are more new containers
		RTObject currentChildOfContainer = newContentObject;
		Container currentContainerAncestor = (Container) currentChildOfContainer.getparent();
		while (currentContainerAncestor != null && !prevContainerSet.contains(currentContainerAncestor)) {

			// Check whether this ancestor container is being entered at the
			// start,
			// by checking whether the child Object is the first.
			boolean enteringAtStart = currentContainerAncestor._content.size() > 0
					&& currentChildOfContainer == currentContainerAncestor._content.get(0);

			// Mark a visit to this container
			VisitContainer(currentContainerAncestor, enteringAtStart);

			currentChildOfContainer = currentContainerAncestor;
			currentContainerAncestor = (Container) currentContainerAncestor.getparent();
		}
	}

	Choice ProcessChoice(ChoicePoint choicePoint) throws Exception {
		boolean showChoice = true;

		// Don't create choice if choice point doesn't pass conditional
		if (choicePoint.gethasCondition()) {
			RTObject conditionValue = _state.PopEvaluationStack();
			if (!IsTruthy(conditionValue)) {
				showChoice = false;
			}
		}

		String startText = "";
		String choiceOnlyText = "";

		if (choicePoint.gethasChoiceOnlyContent()) {
			StringValue choiceOnlyStrVal = (StringValue) _state.PopEvaluationStack();
			choiceOnlyText = choiceOnlyStrVal.value;
		}

		if (choicePoint.gethasStartContent()) {
			StringValue startStrVal = (StringValue) _state.PopEvaluationStack();
			startText = startStrVal.value;
		}

		// Don't create choice if player has already read this content
		if (choicePoint.getonceOnly()) {
			int visitCount = VisitCountForContainer(choicePoint.getchoiceTarget());
			if (visitCount > 0) {
				showChoice = false;
			}
		}

		Choice choice = new Choice(choicePoint);
		choice.setthreadAtGeneration(_state.callStack.getcurrentThread().Copy());

		// We go through the full process of creating the choice above so
		// that we consume the content for it, since otherwise it'll
		// be shown on the output stream.
		if (!showChoice) {
			return null;
		}

		// Set final text for the choice
		choice.settext(startText + choiceOnlyText);

		return choice;
	}

	// Does the expression result represented by this Object evaluate to true?
	// e.g. is it a Number that's not equal to 1?
	boolean IsTruthy(RTObject obj) throws Exception {
		boolean truthy = false;
		if (obj instanceof Value) {
			Value val = (Value) obj;

			if (val instanceof DivertTargetValue) {
				DivertTargetValue divTarget = (DivertTargetValue) val;
				Error("Shouldn't use a divert target (to " + divTarget.gettargetPath()
						+ ") as a conditional value. Did you intend a function call 'likeThis()' or a read count check 'likeThis'? (no arrows)");
				return false;
			}

			return val.getisTruthy();
		}
		return truthy;
	}

	/// <summary>
	/// Checks whether contentObj is a control or flow Object rather than a
	/// piece of content,
	/// and performs the required command if necessary.
	/// </summary>
	/// <returns><c>true</c> if Object was logic or flow control, <c>false</c>
	/// if it's normal content.</returns>
	/// <param name="contentObj">Content Object.</param>
	boolean PerformLogicAndFlowControl(RTObject contentObj) throws Exception {
		if (contentObj == null) {
			return false;
		}

		// Divert
		if (contentObj instanceof Divert) {

			Divert currentDivert = (Divert) contentObj;

			if (currentDivert.getisConditional()) {
				RTObject conditionValue = _state.PopEvaluationStack();

				// False conditional? Cancel divert
				if (!IsTruthy(conditionValue))
					return true;
			}

			if (currentDivert.gethasVariableTarget()) {
				String varName = currentDivert.getvariableDivertName();

				RTObject varContents = _state.variablesState.getVariableWithName(varName);

				if (!(varContents instanceof DivertTargetValue)) {

					IntValue intContent = (IntValue) varContents;

					String errorMessage = "Tried to divert to a target from a variable, but the variable (" + varName
							+ ") didn't contain a divert target, it ";
					if (intContent != null && intContent.value == 0) {
						errorMessage += "was empty/null (the value 0).";
					} else {
						errorMessage += "contained '" + varContents + "'.";
					}

					Error(errorMessage);
				}

				DivertTargetValue target = (DivertTargetValue) varContents;
				_state.divertedTargetObject = ContentAtPath(target.gettargetPath());

			} else if (currentDivert.getisExternal()) {
				CallExternalFunction(currentDivert.gettargetPathString(), currentDivert.getexternalArgs());
				return true;
			} else {
				_state.divertedTargetObject = currentDivert.gettargetContent();
			}

			if (currentDivert.getpushesToStack()) {
				_state.callStack.Push(currentDivert.stackPushType);
			}

			if (_state.divertedTargetObject == null && !currentDivert.getisExternal()) {

				// Human readable name available - runtime divert is part of a
				// hard-written divert that to missing content
				if (currentDivert != null && currentDivert.debugMetadata.sourceName != null) {
					Error("Divert target doesn't exist: " + currentDivert.debugMetadata.sourceName);
				} else {
					Error("Divert resolution failed: " + currentDivert);
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
				Assert(_state.getinExpressionEvaluation() == false, "Already in expression evaluation?");
				_state.setinExpressionEvaluation(true);
				break;

			case EvalEnd:
				Assert(_state.getinExpressionEvaluation() == true, "Not in expression evaluation mode");
				_state.setinExpressionEvaluation(false);
				break;

			case EvalOutput:

				// If the expression turned out to be empty, there may not be
				// anything on the stack
				if (_state.evaluationStack.size() > 0) {

					RTObject output = _state.PopEvaluationStack();

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

						_state.PushToOutputStream(text);
					}

				}
				break;

			case NoOp:
				break;

			case Duplicate:
				_state.PushEvaluationStack(_state.PeekEvaluationStack());
				break;

			case PopEvaluatedValue:
				_state.PopEvaluationStack();
				break;

			case PopFunction:
			case PopTunnel:

				PushPopType popType = evalCommand.getcommandType() == ControlCommand.CommandType.PopFunction
						? PushPopType.Function : PushPopType.Tunnel;

				if (_state.callStack.currentElement().type != popType || !_state.callStack.canPop()) {

					HashMap<PushPopType, String> names = new HashMap<PushPopType, String>();
					names.put(PushPopType.Function, "function return statement (~ return)");
					names.put(PushPopType.Tunnel, "tunnel onwards statement (->->)");

					String expected = names.get(_state.callStack.currentElement().type);
					if (!_state.callStack.canPop()) {
						expected = "end of flow (-> END or choice)";
					}

					String errorMsg = String.format("Found {0}, when expected {1}", names.get(popType), expected);

					Error(errorMsg);
				}

				else {
					_state.callStack.Pop();
				}
				break;

			case BeginString:
				_state.PushToOutputStream(evalCommand);

				Assert(_state.getinExpressionEvaluation() == true,
						"Expected to be in an expression when evaluating a string");
				_state.setinExpressionEvaluation(false);
				break;

			case EndString:

				// Since we're iterating backward through the content,
				// build a stack so that when we build the string,
				// it's in the right order
				Stack<RTObject> contentStackForString = new Stack<RTObject>();

				int outputCountConsumed = 0;
				for (int i = _state.outputStream().size() - 1; i >= 0; --i) {
					RTObject obj = _state.outputStream().get(i);

					outputCountConsumed++;

					ControlCommand command = (ControlCommand) obj;
					if (command != null && command.getcommandType() == ControlCommand.CommandType.BeginString) {
						break;
					}

					if (obj instanceof StringValue)
						contentStackForString.push(obj);
				}

				// Consume the content that was produced for this string
				_state.outputStream().subList(_state.outputStream().size() - outputCountConsumed, outputCountConsumed)
						.clear();

				// Build String out of the content we collected
				StringBuilder sb = new StringBuilder();
				for (RTObject c : contentStackForString) {
					sb.append(c.toString());
				}

				// Return to expression evaluation (from content mode)
				_state.setinExpressionEvaluation(true);
				_state.PushEvaluationStack(new StringValue(sb.toString()));
				break;

			case ChoiceCount:
				choiceCount = getCurrentChoices().size();
				_state.PushEvaluationStack(new IntValue(choiceCount));
				break;

			case TurnsSince:
				RTObject target = _state.PopEvaluationStack();
				if (!(target instanceof DivertTargetValue)) {
					String extraNote = "";
					if (target instanceof IntValue)
						extraNote = ". Did you accidentally pass a read count ('knot_name') instead of a target ('-> knot_name')?";
					Error("TURNS_SINCE expected a divert target (knot, stitch, label name), but saw " + target
							+ extraNote);
					break;
				}

				DivertTargetValue divertTarget = (DivertTargetValue) target;
				Container container = (Container) ContentAtPath(divertTarget.gettargetPath());
				int turnCount = TurnsSinceForContainer(container);
				_state.PushEvaluationStack(new IntValue(turnCount));
				break;

			case VisitIndex:
				int count = VisitCountForContainer(_state.currentContainer()) - 1; // index
																					// not
																					// count
				_state.PushEvaluationStack(new IntValue(count));
				break;

			case SequenceShuffleIndex:
				int shuffleIndex = NextSequenceShuffleIndex();
				_state.PushEvaluationStack(new IntValue(shuffleIndex));
				break;

			case StartThread:
				// Handled in main step function
				break;

			case Done:

				// We may exist in the context of the initial
				// act of creating the thread, or in the context of
				// evaluating the content.
				if (_state.callStack.canPopThread()) {
					_state.callStack.PopThread();
				}

				// In normal flow - allow safe exit without warning
				else {
					_state.didSafeExit = true;
				}

				break;

			// Force flow to end completely
			case End:
				_state.ForceEndFlow();
				break;

			default:
				Error("unhandled ControlCommand: " + evalCommand);
				break;
			}

			return true;
		}

		// Variable assignment
		else if (contentObj instanceof VariableAssignment) {
			VariableAssignment varAss = (VariableAssignment) contentObj;
			RTObject assignedVal = _state.PopEvaluationStack();

			// When in temporary evaluation, don't create new variables purely
			// within
			// the temporary context, but attempt to create them globally
			// var prioritiseHigherInCallStack = _temporaryEvaluationContainer
			// != null;

			_state.variablesState.assign(varAss, assignedVal);

			return true;
		}

		// Variable reference
		else if (contentObj instanceof VariableReference) {
			VariableReference varRef = (VariableReference) contentObj;
			RTObject foundValue = null;

			// Explicit read count value
			if (varRef.getpathForCount() != null) {

				Container container = varRef.getcontainerForCount();
				int count = VisitCountForContainer(container);
				foundValue = new IntValue(count);
			}

			// Normal variable reference
			else {

				foundValue = _state.variablesState.getVariableWithName(varRef.getname());

				if (foundValue == null) {
					Error("Uninitialised variable: " + varRef.getname());
					foundValue = new IntValue(0);
				}
			}

			_state.evaluationStack.add(foundValue);

			return true;
		}

		// Native function call
		else if (contentObj instanceof NativeFunctionCall) {
			NativeFunctionCall func = (NativeFunctionCall) contentObj;
			List<RTObject> funcParams = _state.PopEvaluationStack(func._numberOfParameters);
			// TODO
			// var result = func.Call(funcParams);
			// _state.evaluationStack.add(result);
			return true;
		}

		// No control content, must be ordinary content
		return false;
	}

	/// <summary>
	/// Change the current position of the story to the given path.
	/// From here you can call Continue() to evaluate the next line.
	/// The path String is a dot-separated path as used ly by the
	/// engine.
	/// These examples should work:
	///
	/// myKnot
	/// myKnot.myStitch
	///
	/// Note however that this won't necessarily work:
	///
	/// myKnot.myStitch.myLabelledChoice
	///
	/// ...because of the way that content is nested within a weave structure.
	///
	/// </summary>
	/// <param name="path">A dot-separted path string, as specified
	/// above.</param>
	public void ChoosePathString(String path) throws Exception {
		ChoosePath(new Path(path));
	}

	void ChoosePath(Path path) throws Exception {
		_state.SetChosenPath(path);

		// Take a note of newly visited containers for read counts etc
		VisitChangedContainersDueToDivert();
	}

	/// <summary>
	/// Chooses the Choice from the currentChoices list with the given
	/// index. Internally, this sets the current content path to that
	/// pointed to by the Choice, ready to continue story evaluation.
	/// </summary>
	public void ChooseChoiceIndex(int choiceIdx) throws Exception {
		List<Choice> choices = getCurrentChoices();
		Assert(choiceIdx >= 0 && choiceIdx < choices.size(), "choice out of range");

		// Replace callstack with the one from the thread at the choosing point,
		// so that we can jump into the right place in the flow.
		// This is important in case the flow was forked by a new thread, which
		// can create multiple leading edges for the story, each of
		// which has its own context.
		Choice choiceToChoose = choices.get(choiceIdx);
		_state.callStack.setCurrentThread(choiceToChoose.getthreadAtGeneration());

		ChoosePath(choiceToChoose.getchoicePoint().getchoiceTarget().path);
	}

	// Evaluate a "hot compiled" piece of ink content, as used by the REPL-like
	// CommandLinePlayer.
	RTObject EvaluateExpression(Container exprContainer) throws StoryException, Exception {
		int startCallStackHeight = _state.callStack.getElements().size();

		_state.callStack.Push(PushPopType.Tunnel);

		_temporaryEvaluationContainer = exprContainer;

		_state.GoToStart();

		int evalStackHeight = _state.evaluationStack.size();

		Continue();

		_temporaryEvaluationContainer = null;

		// Should have fallen off the end of the Container, which should
		// have auto-popped, but just in case we didn't for some reason,
		// manually pop to restore the state (including currentPath).
		if (_state.callStack.getElements().size() > startCallStackHeight) {
			_state.callStack.Pop();
		}

		int endStackHeight = _state.evaluationStack.size();
		if (endStackHeight > evalStackHeight) {
			return _state.PopEvaluationStack();
		} else {
			return null;
		}

	}

	/// <summary>
	/// An ink file can provide a fallback functions for when when an EXTERNAL
	/// has been left
	/// unbound by the client, and the fallback function will be called instead.
	/// Useful when
	/// testing a story in playmode, when it's not possible to write a
	/// client-side C# external
	/// function, but you don't want it to fail to run.
	/// </summary>
	public boolean allowExternalFunctionFallbacks;

	void CallExternalFunction(String funcName, int numberOfArguments) throws Exception {
		Container fallbackFunctionContainer = null;

		ExternalFunction func = _externals.get(funcName);

		// Try to use fallback function?
		if (func == null) {
			if (allowExternalFunctionFallbacks) {
				fallbackFunctionContainer = (Container) ContentAtPath(new Path(funcName));
				Assert(fallbackFunctionContainer != null, "Trying to call EXTERNAL function '" + funcName
						+ "' which has not been bound, and fallback ink function could not be found.");

				// Divert direct into fallback function and we're done
				_state.callStack.Push(PushPopType.Function);
				_state.divertedTargetObject = fallbackFunctionContainer;
				return;

			} else {
				Assert(false, "Trying to call EXTERNAL function '" + funcName
						+ "' which has not been bound (and ink fallbacks disabled).");
			}
		}

		// Pop arguments
		ArrayList<Object> arguments = new ArrayList<Object>();
		for (int i = 0; i < numberOfArguments; ++i) {
			Value<?> poppedObj = (Value<?>) _state.PopEvaluationStack();
			RTObject valueObj = poppedObj.getValueRTObject();
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
			returnObj = Value.create(funcResult);
			 Assert (returnObj != null, "Could not create ink value from returned Object of type " + funcResult.getClass().getCanonicalName());
		} else {
			returnObj = new Void();
		}

		_state.PushEvaluationStack(returnObj);
	}

	/// <summary>
	/// General purpose delegate definition for bound EXTERNAL function
	/// definitions
	/// from ink. Note that this version isn't necessary if you have a function
	/// with three arguments or less - see the overloads of
	/// BindExternalFunction.
	/// </summary>
	public interface ExternalFunction {
		Object call(Object[] args);
	}

	/// <summary>
	/// Most general form of function binding that returns an Object
	/// and takes an array of Object parameters.
	/// The only way to bind a function with more than 3 arguments.
	/// </summary>
	/// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	/// <param name="func">The C# function to bind.</param>
	public void BindExternalFunctionGeneral(String funcName, ExternalFunction func) throws Exception {
		Assert(!_externals.containsKey(funcName), "Function '" + funcName + "' has already been bound.");
		_externals.put(funcName, func);
	}
	// TODO
	// Object TryCoerce<T>(Object value)
	// {
	// if (value == null)
	// return null;
	//
	// if (value.GetType () == typeof(T))
	// return (T) value;
	//
	// if (value is float && typeof(T) == typeof(int)) {
	// int intVal = (int)Math.Round ((float)value);
	// return intVal;
	// }
	//
	// if (value is int && typeof(T) == typeof(float)) {
	// float floatVal = (float)(int)value;
	// return floatVal;
	// }
	//
	// if (value is int && typeof(T) == typeof(bool)) {
	// int intVal = (int)value;
	// return intVal == 0 ? false : true;
	// }
	//
	// if (typeof(T) == typeof(string)) {
	// return value.toString ();
	// }
	//
	// Assert (false, "Failed to cast " + value.GetType ().Name + " to " +
	// typeof(T).Name);
	//
	// return null;
	// }
	//
	// // Convenience overloads for standard functions and actions of various
	// // arities
	// // Is there a better way of doing this?!
	//
	// /// <summary>
	// /// Bind a C# function to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="func">The C# function to bind.</param>
	// public void BindExternalFunction(String funcName, Func<Object> func)
	// {
	// Assert(func != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 0, "External function expected no arguments");
	// return func();
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# Action to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="act">The C# action to bind.</param>
	// public void BindExternalFunction(String funcName, Action act)
	// {
	// Assert(act != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 0, "External function expected no arguments");
	// act();
	// return null;
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# function to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="func">The C# function to bind.</param>
	// public void BindExternalFunction<T>(String funcName, Func<T,Object>func)
	// {
	// Assert(func != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 1, "External function expected one argument");
	// return func( (T)TryCoerce<T>(args[0]) );
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# action to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="act">The C# action to bind.</param>
	// public void BindExternalFunction<T>(String funcName, Action<T>act)
	// {
	// Assert(act != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 1, "External function expected one argument");
	// act( (T)TryCoerce<T>(args[0]) );
	// return null;
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# function to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="func">The C# function to bind.</param>
	// public void BindExternalFunction<T1,T2>(
	// String funcName, Func<T1,T2,Object>func)
	// {
	// Assert(func != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 2, "External function expected two arguments");
	// return func(
	// (T1)TryCoerce<T1>(args[0]),
	// (T2)TryCoerce<T2>(args[1])
	// );
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# action to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="act">The C# action to bind.</param>
	// public void BindExternalFunction<T1,T2>(
	// String funcName, Action<T1,T2>act)
	// {
	// Assert(act != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 2, "External function expected two arguments");
	// act(
	// (T1)TryCoerce<T1>(args[0]),
	// (T2)TryCoerce<T2>(args[1])
	// );
	// return null;
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# function to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="func">The C# function to bind.</param>
	// public void BindExternalFunction<T1,T2,T3>(
	// String funcName, Func<T1,T2,T3,Object>func)
	// {
	// Assert(func != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 3, "External function expected two arguments");
	// return func(
	// (T1)TryCoerce<T1>(args[0]),
	// (T2)TryCoerce<T2>(args[1]),
	// (T3)TryCoerce<T3>(args[2])
	// );
	// });
	// }
	//
	// /// <summary>
	// /// Bind a C# action to an ink EXTERNAL function declaration.
	// /// </summary>
	// /// <param name="funcName">EXTERNAL ink function name to bind to.</param>
	// /// <param name="act">The C# action to bind.</param>
	// public void BindExternalFunction<T1,T2,T3>(
	// String funcName, Action<T1,T2,T3>act)
	// {
	// Assert(act != null, "Can't bind a null function");
	//
	// BindExternalFunctionGeneral (funcName, (Object[] args) => {
	// Assert(args.Length == 3, "External function expected two arguments");
	// act(
	// (T1)TryCoerce<T1>(args[0]),
	// (T2)TryCoerce<T2>(args[1]),
	// (T3)TryCoerce<T3>(args[2])
	// );
	// return null;
	// });
	// }

	/// <summary>
	/// Remove a binding for a named EXTERNAL ink function.
	/// </summary>
	public void UnbindExternalFunction(String funcName) throws Exception {
		Assert(_externals.containsKey(funcName), "Function '" + funcName + "' has not been bound.");
		_externals.remove(funcName);
	}

	/// <summary>
	/// Check that all EXTERNAL ink functions have a valid bound C# function.
	/// Note that this is automatically called on the first call to Continue().
	/// </summary>
	public void ValidateExternalBindings() throws Exception {
		ValidateExternalBindings(_mainContentContainer);
		_hasValidatedExternals = true;
	}

	void ValidateExternalBindings(Container c) throws Exception {
		for (RTObject innerContent : c._content) {
			ValidateExternalBindings(innerContent);
		}
		for (INamedContent innerKeyValue : c.getnamedContent().values()) {
			ValidateExternalBindings((RTObject) innerKeyValue);
		}
	}

	void ValidateExternalBindings(RTObject o) throws Exception {
		Container container = (Container) o;
		if (container != null) {
			ValidateExternalBindings(container);
			return;
		}

		Divert divert = (Divert) o;
		if (divert != null && divert.getisExternal()) {
			String name = divert.gettargetPathString();

			if (!_externals.containsKey(name)) {

				INamedContent fallbackFunction = mainContentContainer().getnamedContent().get(name);

				if (!allowExternalFunctionFallbacks)
					Error("Missing function binding for external '" + name + "' (ink fallbacks disabled)");
				else if (fallbackFunction == null) {
					Error("Missing function binding for external '" + name + "', and no fallback ink function found.");
				}
			}
		}
	}

	/// <summary>
	/// Delegate definition for variable observation - see ObserveVariable.
	/// </summary>
	// TODO
	// public delegate void VariableObserver(String variableName, Object
	// newValue);
	
	public interface VariableObserver {
		Object call(String variableName, Object newValue);
	}

	/// <summary>
	/// When the named global variable changes it's value, the observer will be
	/// called to notify it of the change. Note that if the value changes
	/// multiple
	/// times within the ink, the observer will only be called once, at the end
	/// of the ink's evaluation. If, during the evaluation, it changes and then
	/// changes back again to its original value, it will still be called.
	/// Note that the observer will also be fired if the value of the variable
	/// is changed externally to the ink, by directly setting a value in
	/// story.variablesState.
	/// </summary>
	/// <param name="variableName">The name of the global variable to
	/// observe.</param>
	/// <param name="observer">A delegate function to call when the variable
	/// changes.</param>
	// TODO
	// public void ObserveVariable(String variableName, VariableObserver
	// observer) {
	// if (_variableObservers == null)
	// _variableObservers = new HashMap<String, VariableObserver>();
	//
	// if (_variableObservers.ContainsKey(variableName)) {
	// _variableObservers[variableName] += observer;
	// } else {
	// _variableObservers[variableName] = observer;
	// }
	// }

	/// <summary>
	/// Convenience function to allow multiple variables to be observed with the
	/// same
	/// observer delegate function. See the singular ObserveVariable for
	/// details.
	/// The observer will get one call for every variable that has changed.
	/// </summary>
	/// <param name="variableNames">The set of variables to observe.</param>
	/// <param name="observer">The delegate function to call when any of the
	/// named variables change.</param>

	// TODO
	// public void ObserveVariables(IList<String> variableNames,
	// VariableObserver observer)
	// {
	// foreach (var varName in variableNames) {
	// ObserveVariable (varName, observer);
	// }
	// }

	/// <summary>
	/// Removes the variable observer, to stop getting variable change
	/// notifications.
	/// If you pass a specific variable name, it will stop observing that
	/// particular one. If you
	/// pass null (or leave it blank, since it's optional), then the observer
	/// will be removed
	/// from all variables that it's subscribed to.
	/// </summary>
	/// <param name="observer">The observer to stop observing.</param>
	/// <param name="specificVariableName">(Optional) Specific variable name to
	/// stop observing.</param>
	// public void RemoveVariableObserver(VariableObserver observer, String
	// specificVariableName = null)
	// {
	// if (_variableObservers == null)
	// return;
	//
	// // Remove observer for this specific variable
	// if (specificVariableName != null) {
	// if (_variableObservers.ContainsKey (specificVariableName)) {
	// _variableObservers [specificVariableName] -= observer;
	// }
	// }
	//
	// // Remove observer for all variables
	// else {
	//
	// foreach (var keyValue in _variableObservers) {
	// var varName = keyValue.Key;
	// _variableObservers [varName] -= observer;
	// }
	// }
	//
	// }
	//
	// void VariableStateDidChangeEvent(String variableName, RTObject
	// newValueObj)
	// {
	// if (_variableObservers == null)
	// return;
	//
	// VariableObserver observers = null;
	// if (_variableObservers.TryGetValue (variableName, out observers)) {
	//
	// if (!(newValueObj is Value)) {
	// throw new System.Exception ("Tried to get the value of a variable that
	// isn't a standard type");
	// }
	// var val = newValueObj as Value;
	//
	// observers (variableName, val.valueObject);
	// }
	// }

	/// <summary>
	/// Useful when debugging a (very short) story, to visualise the state of
	/// the
	/// story. Add this call as a watch and open the extended text. A left-arrow
	/// mark
	/// will denote the current point of the story.
	/// It's only recommended that this is used on very short debug stories,
	/// since
	/// it can end up generate a large quantity of text otherwise.
	/// </summary>
	public String BuildStringOfHierarchy() throws Exception {
		StringBuilder sb = new StringBuilder();

		_mainContentContainer.buildStringOfHierarchy(sb, 0, _state.getcurrentContentObject());

		return sb.toString();
	}

	private void NextContent() throws Exception {
		// Setting previousContentObject is critical for
		// VisitChangedContainersDueToDivert
		_state.setpreviousContentObject(_state.getcurrentContentObject());

		// Divert step?
		if (_state.divertedTargetObject != null) {

			_state.setcurrentContentObject(_state.divertedTargetObject);
			_state.divertedTargetObject = null;

			// Internally uses state.previousContentObject and
			// state.currentContentObject
			VisitChangedContainersDueToDivert();

			// Diverted location has valid content?
			if (_state.getcurrentContentObject() != null) {
				return;
			}

			// Otherwise, if diverted location doesn't have valid content,
			// drop down and attempt to increment.
			// This can happen if the diverted path is intentionally jumping
			// to the end of a container - e.g. a Conditional that's re-joining
		}

		boolean successfulPointerIncrement = IncrementContentPointer();

		// Ran out of content? Try to auto-exit from a function,
		// or finish evaluating the content of a thread
		if (!successfulPointerIncrement) {

			boolean didPop = false;

			if (_state.callStack.CanPop(PushPopType.Function)) {

				// Pop from the call stack
				_state.callStack.Pop(PushPopType.Function);

				// This pop was due to dropping off the end of a function that
				// didn't return anything,
				// so in this case, we make sure that the evaluator has
				// something to chomp on if it needs it
				if (_state.getinExpressionEvaluation()) {
					_state.PushEvaluationStack(new Void());
				}

				didPop = true;
			}

			else if (_state.callStack.canPopThread()) {
				_state.callStack.PopThread();

				didPop = true;
			}

			// Step past the point where we last called out
			if (didPop && _state.getcurrentContentObject() != null) {
				NextContent();
			}
		}
	}

	boolean IncrementContentPointer() {
		boolean successfulIncrement = true;

		Element currEl = _state.callStack.currentElement();
		currEl.currentContentIndex++;

		// Each time we step off the end, we fall out to the next container, all
		// the
		// while we're in indexed rather than named content
		while (currEl.currentContentIndex >= currEl.currentContainer._content.size()) {

			successfulIncrement = false;

			Container nextAncestor = (Container) currEl.currentContainer.getparent();
			if (nextAncestor == null) {
				break;
			}

			int indexInAncestor = nextAncestor._content.indexOf(currEl.currentContainer);
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

	boolean TryFollowDefaultInvisibleChoice() throws Exception {
		List<Choice> allChoices = _state.currentChoices;

		// Is a default invisible choice the ONLY choice?
		// var invisibleChoices = allChoices.Where (c =>
		// c.choicePoint.isInvisibleDefault).ToList();
		ArrayList<Choice> invisibleChoices = new ArrayList<Choice>();
		for (Choice c : allChoices) {
			if (c.getchoicePoint().getisInvisibleDefault()) {
				invisibleChoices.add(c);
			}
		}

		if (invisibleChoices.size() == 0 || allChoices.size() > invisibleChoices.size())
			return false;

		Choice choice = invisibleChoices.get(0);

		ChoosePath(choice.getchoicePoint().getchoiceTarget().path);

		return true;
	}

	int VisitCountForContainer(Container container) throws Exception {
		if (!container.getvisitsShouldBeCounted()) {
			Error("Read count for target (" + container.getname() + " - on " + container.debugMetadata
					+ ") unknown. The story may need to be compiled with countAllVisits flag (-c).");
			return 0;
		}

		int count = 0;
		String containerPathStr = container.path.toString();
		count = _state.visitCounts.get(containerPathStr);
		return count;
	}

	void IncrementVisitCountForContainer(Container container) {
		int count = 0;
		String containerPathStr = container.path.toString();
		count = _state.visitCounts.get(containerPathStr);
		count++;
		_state.visitCounts.put(containerPathStr, count);
	}

	void RecordTurnIndexVisitToContainer(Container container) {
		String containerPathStr = container.path.toString();
		_state.turnIndices.put(containerPathStr, _state.currentTurnIndex);
	}

	int TurnsSinceForContainer(Container container) throws Exception {
		if (!container.getturnIndexShouldBeCounted()) {
			Error("TURNS_SINCE() for target (" + container.getname() + " - on " + container.debugMetadata
					+ ") unknown. The story may need to be compiled with countAllVisits flag (-c).");
		}

		String containerPathStr = container.path.toString();
		Integer index = _state.turnIndices.get(containerPathStr);
		if (index != null) {
			return _state.currentTurnIndex - index;
		} else {
			return -1;
		}
	}

	// Note that this is O(n), since it re-evaluates the shuffle indices
	// from a consistent seed each time.
	// TODO: Is this the best algorithm it can be?
	int NextSequenceShuffleIndex() throws Exception {
		IntValue numElementsIntVal = (IntValue) _state.PopEvaluationStack();

		if (numElementsIntVal == null) {
			Error("expected number of elements in sequence for shuffle index");
			return 0;
		}

		Container seqContainer = _state.currentContainer();

		int numElements = numElementsIntVal.value;

		IntValue seqCountVal = (IntValue) _state.PopEvaluationStack();
		Integer seqCount = seqCountVal.value;
		int loopIndex = seqCount / numElements;
		int iterationIndex = seqCount % numElements;

		// Generate the same shuffle based on:
		// - The hash of this container, to make sure it's consistent
		// each time the runtime returns to the sequence
		// - How many times the runtime has looped around this full shuffle
		String seqPathStr = seqContainer.path.toString();
		int sequenceHash = 0;
		for (char c : seqPathStr.toCharArray()) {
			sequenceHash += c;
		}

		int randomSeed = sequenceHash + loopIndex + _state.storySeed;

		Random random = new Random(randomSeed);

		ArrayList<Integer> unpickedIndices = new ArrayList<Integer>();
		for (int i = 0; i < numElements; ++i) {
			unpickedIndices.add(i);
		}

		for (int i = 0; i <= iterationIndex; ++i) {
			int chosen = random.nextInt() % unpickedIndices.size();
			int chosenIndex = unpickedIndices.get(chosen);
			unpickedIndices.remove(chosen);

			if (i == iterationIndex) {
				return chosenIndex;
			}
		}

		throw new Exception("Should never reach here");
	}

	// Throw an exception that gets caught and causes AddError to be called,
	// then exits the flow.
	void Error(String message, boolean useEndLineNumber) throws Exception {
		StoryException e = new StoryException(message);
		e.useEndLineNumber = useEndLineNumber;
		throw e;
	}

	void Error(String message) throws Exception
    {
		Error(message, false);
    }

	void AddError(String message, boolean useEndLineNumber) throws Exception {
		DebugMetadata dm = currentDebugMetadata();

		if (dm != null) {
			int lineNum = useEndLineNumber ? dm.endLineNumber : dm.startLineNumber;
			message = String.format("RUNTIME ERROR: '{0}' line {1}: {2}", dm.fileName, lineNum, message);
		} else {
			message = "RUNTIME ERROR: " + message;
		}

		_state.AddError(message);

		// In a broken state don't need to know about any other errors.
		_state.ForceEndFlow();
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

	DebugMetadata currentDebugMetadata() throws Exception {
		DebugMetadata dm;

		// Try to get from the current path first
		RTObject currentContent = _state.getcurrentContentObject();
		if (currentContent != null) {
			dm = currentContent.debugMetadata;
			if (dm != null) {
				return dm;
			}
		}

		// Move up callstack if possible
		for (int i = _state.callStack.getElements().size() - 1; i >= 0; --i) {
			RTObject currentObj = _state.callStack.getElements().get(i).currentRTObject;
			if (currentObj != null && currentObj.debugMetadata != null) {
				return currentObj.debugMetadata;
			}
		}

		// Current/previous path may not be valid if we've just had an error,
		// or if we've simply run out of content.
		// As a last resort, try to grab something from the output stream
		for (int i = _state.outputStream().size() - 1; i >= 0; --i) {
			RTObject outputObj = _state.outputStream().get(i);
			dm = outputObj.debugMetadata;
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

	Container mainContentContainer() {
		if (_temporaryEvaluationContainer != null) {
			return _temporaryEvaluationContainer;
		} else {
			return _mainContentContainer;
		}
	}

	private Container _mainContentContainer;

	HashMap<String, ExternalFunction> _externals;
	HashMap<String, VariableObserver> _variableObservers;
	boolean _hasValidatedExternals;

	Container _temporaryEvaluationContainer;

	StoryState _state;
}
