package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.bladecoder.ink.runtime.CallStack.Thread;

/**
 * All story state information is included in the StoryState class, including
 * global variables, read counts, the pointer to the current point in the story,
 * the call stack (for tunnels, functions, etc), and a few other smaller bits
 * and pieces. You can save the current state using the json serialisation
 * functions ToJson and LoadJson.
 */
public class StoryState {
	/**
	 * The current version of the state save file JSON-based format.
	 */
	public static final int kInkSaveStateVersion = 5;
	public static final int kMinCompatibleLoadVersion = 4;

	// REMEMBER! REMEMBER! REMEMBER!
	// When adding state, update the Copy method and serialisation
	// REMEMBER! REMEMBER! REMEMBER!
	private List<RTObject> outputStream;
	private CallStack callStack;
	private List<Choice> currentChoices;
	private List<String> currentErrors;
	private int currentTurnIndex;
	private boolean didSafeExit;
	private RTObject divertedTargetObject;
	private List<RTObject> evaluationStack;
	private Story story;
	private int storySeed;
	private int previousRandom;
	private HashMap<String, Integer> turnIndices;
	private VariablesState variablesState;
	private HashMap<String, Integer> visitCounts;
	private String currentText;

	// Temporary state only, during externally called function evaluation
	private boolean isExternalFunctionEvaluation;
	private CallStack originalCallstack;
	private int originalEvaluationStackHeight;

	private boolean outputStreamTextDirty = true;
	private boolean outputStreamTagsDirty = true;
	private List<String> currentTags;

	StoryState(Story story) {
		this.story = story;

		outputStream = new ArrayList<RTObject>();
		outputStreamDirty();

		evaluationStack = new ArrayList<RTObject>();

		callStack = new CallStack(story.getRootContentContainer());
		variablesState = new VariablesState(callStack, story.getSets());

		visitCounts = new HashMap<String, Integer>();
		turnIndices = new HashMap<String, Integer>();
		currentTurnIndex = -1;

		// Seed the shuffle random numbers
		long timeSeed = System.currentTimeMillis();

		storySeed = new Random(timeSeed).nextInt() % 100;
		previousRandom = 0;

		currentChoices = new ArrayList<Choice>();

		goToStart();
	}

	void addError(String message) {
		// TODO: Could just add to output?
		if (currentErrors == null) {
			currentErrors = new ArrayList<String>();
		}

		currentErrors.add(message);
	}

	// Warning: Any RTObject content referenced within the StoryState will
	// be re-referenced rather than cloned. This is generally okay though since
	// RTObjects are treated as immutable after they've been set up.
	// (e.g. we don't edit a Runtime.Text after it's been created an added.)
	// I wonder if there's a sensible way to enforce that..??
	StoryState copy() {
		StoryState copy = new StoryState(story);

		copy.getOutputStream().addAll(outputStream);
		outputStreamDirty();
		copy.currentChoices.addAll(currentChoices);

		if (hasError()) {
			copy.currentErrors = new ArrayList<String>();
			copy.currentErrors.addAll(currentErrors);
		}

		copy.callStack = new CallStack(callStack);

		copy.variablesState = new VariablesState (copy.callStack, story.getSets());
		copy.variablesState.copyFrom (variablesState);

		copy.evaluationStack.addAll(evaluationStack);

		if (getDivertedTargetObject() != null)
			copy.setDivertedTargetObject(divertedTargetObject);

		copy.setPreviousContentObject(getPreviousContentObject());

		copy.visitCounts = new HashMap<String, Integer>(visitCounts);
		copy.turnIndices = new HashMap<String, Integer>(turnIndices);
		copy.currentTurnIndex = currentTurnIndex;
		copy.storySeed = storySeed;
		copy.previousRandom = previousRandom;

		copy.setDidSafeExit(didSafeExit);

		return copy;
	}

	Container currentContainer() {
		return callStack.currentElement().currentContainer;
	}

	int currentGlueIndex() {
		for (int i = outputStream.size() - 1; i >= 0; i--) {
			RTObject c = outputStream.get(i);
			Glue glue = c instanceof Glue ? (Glue) c : null;
			if (glue != null)
				return i;
			else if (c instanceof ControlCommand) // e.g. BeginString
				break;
		}
		return -1;
	}

	String getCurrentText() {
		if (outputStreamTextDirty) {
			StringBuilder sb = new StringBuilder();

			for (RTObject outputObj : outputStream) {
				StringValue textContent = null;
				if (outputObj instanceof StringValue)
					textContent = (StringValue) outputObj;

				if (textContent != null) {
					sb.append(textContent.value);
				}
			}

			currentText = sb.toString();

			outputStreamTextDirty = false;
		}

		return currentText;
	}

	void forceEnd() throws Exception {

		while (callStack.canPopThread())
			callStack.popThread();

		while (callStack.canPop())
			callStack.pop();

		currentChoices.clear();

		setCurrentContentObject(null);
		setPreviousContentObject(null);

		setDidSafeExit(true);
	}

	RTObject getCurrentContentObject() {
		return callStack.currentElement().getCurrentRTObject();
	}

	Path getCurrentPath() {

		if (getCurrentContentObject() == null)
			return null;

		return getCurrentContentObject().getPath();
	}

	List<String> getCurrentTags() {
		if (outputStreamTagsDirty) {
			currentTags = new ArrayList<String>();

			for (RTObject outputObj : outputStream) {
				Tag tag = null;
				if (outputObj instanceof Tag)
					tag = (Tag) outputObj;

				if (tag != null) {
					currentTags.add(tag.getText());
				}
			}
			outputStreamTagsDirty = false;
		}

		return currentTags;
	}

	boolean getInExpressionEvaluation() {
		return callStack.currentElement().inExpressionEvaluation;
	}

	/**
	 * Object representation of full JSON state. Usually you should use LoadJson
	 * and ToJson since they serialise directly to String for you. But it may be
	 * useful to get the object representation so that you can integrate it into
	 * your own serialisation system.
	 */
	public HashMap<String, Object> getJsonToken() throws Exception {

		HashMap<String, Object> obj = new HashMap<String, Object>();

		HashMap<String, Object> choiceThreads = null;
		for (Choice c : currentChoices) {
			c.originalChoicePath = c.getchoicePoint().getPath().getComponentsString();
			c.originalThreadIndex = c.getThreadAtGeneration().threadIndex;

			if (callStack.getThreadWithIndex(c.originalThreadIndex) == null) {
				if (choiceThreads == null)
					choiceThreads = new HashMap<String, Object>();

				choiceThreads.put(Integer.toString(c.originalThreadIndex), c.getThreadAtGeneration().jsonToken());
			}
		}
		if (choiceThreads != null)
			obj.put("choiceThreads", choiceThreads);

		obj.put("callstackThreads", callStack.getJsonToken());
		obj.put("variablesState", variablesState.getjsonToken());

		obj.put("evalStack", Json.listToJArray(evaluationStack));

		obj.put("outputStream", Json.listToJArray(outputStream));

		obj.put("currentChoices", Json.listToJArray(currentChoices));

		if (getDivertedTargetObject() != null)
			obj.put("currentDivertTarget", getDivertedTargetObject().getPath().getComponentsString());

		obj.put("visitCounts", Json.intHashMapToJObject(visitCounts));
		obj.put("turnIndices", Json.intHashMapToJObject(turnIndices));
		obj.put("turnIdx", currentTurnIndex);
		obj.put("storySeed", storySeed);
		obj.put("previousRandom", previousRandom);

		obj.put("inkSaveVersion", kInkSaveStateVersion);

		// Not using this right now, but could do in future.
		obj.put("inkFormatVersion", Story.inkVersionCurrent);

		return obj;
	}

	RTObject getPreviousContentObject() {
		return callStack.getcurrentThread().previousContentRTObject;
	}

	public HashMap<String, Integer> getVisitCounts() {
		return visitCounts;
	}

	void goToStart() {
		callStack.currentElement().currentContainer = story.mainContentContainer();
		callStack.currentElement().currentContentIndex = 0;
	}

	boolean hasError() {
		return currentErrors != null && currentErrors.size() > 0;
	}

	boolean inStringEvaluation() {
		for (int i = outputStream.size() - 1; i >= 0; i--) {
			ControlCommand cmd = outputStream.get(i) instanceof ControlCommand ? (ControlCommand) outputStream.get(i)
					: null;

			if (cmd != null && cmd.getcommandType() == ControlCommand.CommandType.BeginString) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Loads a previously saved state in JSON format.
	 * 
	 * @param json
	 *            The JSON String to load.
	 */
	public void loadJson(String json) throws Exception {
		setJsonToken(SimpleJson.textToHashMap(json));
	}

	List<Choice> getCurrentChoices() {
		if (canContinue())
			return new ArrayList<Choice>();

		return currentChoices;
	}

	List<Choice> getGeneratedChoices() {
		return currentChoices;
	}

	boolean canContinue() {
		return getCurrentContentObject() != null && !hasError();
	}

	List<String> getCurrentErrors() {
		return currentErrors;
	}

	List<RTObject> getOutputStream() {
		return outputStream;
	}

	CallStack getCallStack() {
		return callStack;
	}

	VariablesState getVariablesState() {
		return variablesState;
	}

	List<RTObject> getEvaluationStack() {
		return evaluationStack;
	}

	int getStorySeed() {
		return storySeed;
	}

	void setStorySeed(int s) {
		storySeed = s;
	}

	int getPreviousRandom() {
		return previousRandom;
	}

	void setPreviousRandom(int i) {
		previousRandom = i;
	}

	HashMap<String, Integer> getTurnIndices() {
		return turnIndices;
	}

	int getCurrentTurnIndex() {
		return currentTurnIndex;
	}

	boolean outputStreamContainsContent() {
		for (RTObject content : outputStream) {
			if (content instanceof StringValue)
				return true;
		}
		return false;
	}

	Glue matchRightGlueForLeftGlue(Glue leftGlue) {
		if (!leftGlue.isLeft())
			return null;

		for (int i = outputStream.size() - 1; i >= 0; i--) {
			RTObject c = outputStream.get(i);
			Glue g = null;

			if (c instanceof Glue)
				g = (Glue) c;

			if (g != null && g.isRight() && g.getParent() == leftGlue.getParent()) {
				return g;
			} else if (c instanceof ControlCommand) // e.g. BeginString
				break;
		}
		return null;
	}

	boolean outputStreamEndsInNewline() {
		if (outputStream.size() > 0) {

			for (int i = outputStream.size() - 1; i >= 0; i--) {
				RTObject obj = outputStream.get(i);
				if (obj instanceof ControlCommand) // e.g. BeginString
					break;
				StringValue text = outputStream.get(i) instanceof StringValue ? (StringValue) outputStream.get(i)
						: null;

				if (text != null) {
					if (text.isNewline())
						return true;
					else if (text.isNonWhitespace())
						break;
				}
			}
		}

		return false;
	}

	RTObject peekEvaluationStack() {
		return evaluationStack.get(evaluationStack.size() - 1);
	}

	RTObject popEvaluationStack() {
		RTObject obj = evaluationStack.get(evaluationStack.size() - 1);
		evaluationStack.remove(evaluationStack.size() - 1);
		return obj;
	}

	List<RTObject> popEvaluationStack(int numberOfObjects) throws Exception {
		if (numberOfObjects > evaluationStack.size()) {
			throw new Exception("trying to pop too many objects");
		}

		List<RTObject> popped = new ArrayList<RTObject>(
				evaluationStack.subList(evaluationStack.size() - numberOfObjects, evaluationStack.size()));
		evaluationStack.subList(evaluationStack.size() - numberOfObjects, evaluationStack.size()).clear();

		return popped;
	}

	void pushEvaluationStack(RTObject obj) {
		evaluationStack.add(obj);
	}

	// Push to output stream, but split out newlines in text for consistency
	// in dealing with them later.
	void pushToOutputStream(RTObject obj) {
		StringValue text = obj instanceof StringValue ? (StringValue) obj : null;

		if (text != null) {
			List<StringValue> listText = trySplittingHeadTailWhitespace(text);
			if (listText != null) {
				for (StringValue textObj : listText) {
					pushToOutputStreamIndividual(textObj);
				}
				return;
			}
		}

		pushToOutputStreamIndividual(obj);
	}

	void pushToOutputStreamIndividual(RTObject obj) {
		Glue glue = obj instanceof Glue ? (Glue) obj : null;
		StringValue text = obj instanceof StringValue ? (StringValue) obj : null;

		boolean includeInOutput = true;

		if (glue != null) {

			// Found matching left-glue for right-glue? Close it.

			Glue matchingRightGlue = null;
			if (glue.isLeft())
				matchingRightGlue = matchRightGlueForLeftGlue(glue);

			// Left/Right glue is auto-generated for inline expressions
			// where we want to absorb newlines but only in a certain direction.
			// "Bi" glue is written by the user in their ink with <>
			if (glue.isLeft() || glue.isBi()) {
				trimNewlinesFromOutputStream(matchingRightGlue);
			}

			// New right-glue
			includeInOutput = glue.isBi() || glue.isRight();
		} else if (text != null) {

			if (currentGlueIndex() != -1) {

				// Absorb any new newlines if there's existing glue
				// in the output stream.
				// Also trim any extra whitespace (spaces/tabs) if so.
				if (text.isNewline()) {
					trimFromExistingGlue();
					includeInOutput = false;
				}

				// Able to completely reset when
				else if (text.isNonWhitespace()) {
					removeExistingGlue();
				}
			} else if (text.isNewline()) {
				if (outputStreamEndsInNewline() || !outputStreamContainsContent())
					includeInOutput = false;
			}
		}

		if (includeInOutput) {
			outputStream.add(obj);
		}

		outputStreamDirty();
	}

	// Only called when non-whitespace is appended
	void removeExistingGlue() {
		for (int i = outputStream.size() - 1; i >= 0; i--) {
			RTObject c = outputStream.get(i);
			if (c instanceof Glue) {
				outputStream.remove(i);
			} else if (c instanceof ControlCommand) { // e.g.
														// BeginString
				break;
			}
		}

		outputStreamDirty();
	}

	void outputStreamDirty() {
		outputStreamTextDirty = true;
		outputStreamTagsDirty = true;
	}

	void resetErrors() {
		currentErrors = null;
	}

	void resetOutput() {
		outputStream.clear();
		outputStreamDirty();
	}

	// Don't make public since the method need to be wrapped in Story for visit
	// counting
	void setChosenPath(Path path) throws Exception {
		// Changing direction, assume we need to clear current set of choices
		currentChoices.clear();

		setCurrentPath(path);

		currentTurnIndex++;
	}

	void startExternalFunctionEvaluation(Container funcContainer, Object[] arguments) throws Exception {
		// We'll start a new callstack, so keep hold of the original,
		// as well as the evaluation stack so we know if the function
		// returned something
		originalCallstack = callStack;
		originalEvaluationStackHeight = evaluationStack.size();

		// Create a new base call stack element.
		callStack = new CallStack(funcContainer);
		callStack.currentElement().type = PushPopType.Function;

		// By setting ourselves in external function evaluation mode,
		// we're saying it's okay to end the flow without a Done or End,
		// but with a ~ return instead.
		isExternalFunctionEvaluation = true;

		// Pass arguments onto the evaluation stack
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				if (!(arguments[i] instanceof Integer || arguments[i] instanceof Float
						|| arguments[i] instanceof String)) {
					throw new Exception("ink arguments when calling EvaluateFunction must be int, float or string");
				}

				evaluationStack.add(Value.create(arguments[i]));
			}
		}
	}

	boolean tryExitExternalFunctionEvaluation() {
		if (isExternalFunctionEvaluation && callStack.getElements().size() == 1
				&& callStack.currentElement().type == PushPopType.Function) {
			setCurrentContentObject(null);
			didSafeExit = true;
			return true;
		}

		return false;
	}

	Object completeExternalFunctionEvaluation() {

		// Do we have a returned value?
		// Potentially pop multiple values off the stack, in case we need
		// to clean up after ourselves (e.g. caller of EvaluateFunction may
		// have passed too many arguments, and we currently have no way to check
		// for that)
		RTObject returnedObj = null;
		while (evaluationStack.size() > originalEvaluationStackHeight) {
			RTObject poppedObj = popEvaluationStack();
			if (returnedObj == null)
				returnedObj = poppedObj;
		}

		// Restore our own state
		callStack = originalCallstack;
		originalCallstack = null;
		originalEvaluationStackHeight = 0;

		// What did we get back?
		if (returnedObj != null) {
			if (returnedObj instanceof Void)
				return null;

			// Some kind of value, if not void
			Value<?> returnVal = null;

			if (returnedObj instanceof Value)
				returnVal = (Value<?>) returnedObj;

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

	void setCurrentContentObject(RTObject value) {
		callStack.currentElement().setcurrentRTObject(value);
	}

	void setCurrentPath(Path value) throws Exception {
		if (value != null)
			setCurrentContentObject(story.contentAtPath(value));
		else
			setCurrentContentObject(null);
	}

	void setInExpressionEvaluation(boolean value) {
		callStack.currentElement().inExpressionEvaluation = value;
	}

	@SuppressWarnings("unchecked")
	void setJsonToken(HashMap<String, Object> value) throws StoryException, Exception {

		HashMap<String, Object> jObject = value;

		Object jSaveVersion = jObject.get("inkSaveVersion");

		if (jSaveVersion == null) {
			throw new StoryException("ink save format incorrect, can't load.");
		} else if ((int) jSaveVersion < kMinCompatibleLoadVersion) {
			throw new StoryException("Ink save format isn't compatible with the current version (saw '" + jSaveVersion
					+ "', but minimum is " + kMinCompatibleLoadVersion + "), so can't load.");
		}

		callStack.setJsonToken((HashMap<String, Object>) jObject.get("callstackThreads"), story);
		variablesState.setjsonToken((HashMap<String, Object>) jObject.get("variablesState"));

		evaluationStack = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("evalStack"));

		outputStream = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("outputStream"));
		outputStreamDirty();

		currentChoices = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("currentChoices"));

		Object currentDivertTargetPath = jObject.get("currentDivertTarget");
		if (currentDivertTargetPath != null) {
			Path divertPath = new Path(currentDivertTargetPath.toString());
			setDivertedTargetObject(story.contentAtPath(divertPath));
		}

		visitCounts = Json.jObjectToIntHashMap((HashMap<String, Object>) jObject.get("visitCounts"));
		turnIndices = Json.jObjectToIntHashMap((HashMap<String, Object>) jObject.get("turnIndices"));
		currentTurnIndex = (int) jObject.get("turnIdx");
		storySeed = (int) jObject.get("storySeed");
		previousRandom = (int) jObject.get("previousRandom");

		Object jChoiceThreadsObj = jObject.get("choiceThreads");
		HashMap<String, Object> jChoiceThreads = (HashMap<String, Object>) jChoiceThreadsObj;

		for (Choice c : currentChoices) {
			c.setChoicePoint((ChoicePoint) story.contentAtPath(new Path(c.originalChoicePath)));

			Thread foundActiveThread = callStack.getThreadWithIndex(c.originalThreadIndex);
			if (foundActiveThread != null) {
				c.setThreadAtGeneration(foundActiveThread);
			} else {
				HashMap<String, Object> jSavedChoiceThread = (HashMap<String, Object>) jChoiceThreads
						.get(Integer.toString(c.originalThreadIndex));
				c.setThreadAtGeneration(new CallStack.Thread(jSavedChoiceThread, story));
			}
		}

	}

	void setPreviousContentObject(RTObject value) {
		callStack.getcurrentThread().previousContentRTObject = value;
	}

	/**
	 * Exports the current state to json format, in order to save the game.
	 * 
	 * @returns The save state in json format.
	 */
	public String toJson() throws Exception {
		return SimpleJson.HashMapToText(getJsonToken());
	}

	void trimFromExistingGlue() {
		int i = currentGlueIndex();
		while (i < outputStream.size()) {
			StringValue txt = outputStream.get(i) instanceof StringValue ? (StringValue) outputStream.get(i) : null;

			if (txt != null && !txt.isNonWhitespace())
				outputStream.remove(i);
			else
				i++;
		}

		outputStreamDirty();
	}

	void trimNewlinesFromOutputStream(Glue rightGlueToStopAt) {
		int removeWhitespaceFrom = -1;
		int rightGluePos = -1;
		boolean foundNonWhitespace = false;

		// Work back from the end, and try to find the point where
		// we need to start removing content. There are two ways:
		// - Start from the matching right-glue (because we just saw a
		// left-glue)
		// - Simply work backwards to find the first newline in a String of
		// whitespace
		int i = outputStream.size() - 1;
		while (i >= 0) {
			RTObject obj = outputStream.get(i);
			ControlCommand cmd = obj instanceof ControlCommand ? (ControlCommand) obj : null;
			StringValue txt = obj instanceof StringValue ? (StringValue) obj : null;
			Glue glue = obj instanceof Glue ? (Glue) obj : null;

			if (cmd != null || (txt != null && txt.isNonWhitespace())) {
				foundNonWhitespace = true;

				if (rightGlueToStopAt == null)
					break;
			} else if (rightGlueToStopAt != null && glue == rightGlueToStopAt) {
				rightGluePos = i;
				break;
			} else if (txt != null && txt.isNewline() && !foundNonWhitespace) {
				removeWhitespaceFrom = i;
			}
			i--;
		}

		// Remove the whitespace
		if (removeWhitespaceFrom >= 0) {
			i = removeWhitespaceFrom;
			while (i < outputStream.size()) {
				StringValue text = outputStream.get(i) instanceof StringValue ? (StringValue) outputStream.get(i)
						: null;
				if (text != null) {
					outputStream.remove(i);
				} else {
					i++;
				}
			}
		}

		// Remove the glue (it will come before the whitespace,
		// so index is still valid)
		// Also remove any other non-matching right glues that come after,
		// since they'll have lost their matching glues already
		if (rightGlueToStopAt != null && rightGluePos > -1) {
			i = rightGluePos;
			while (i < outputStream.size()) {
				if (outputStream.get(i) instanceof Glue && ((Glue) outputStream.get(i)).isRight()) {
					outputStream.remove(i);
				} else {
					i++;
				}
			}
		}
	}

	// At both the start and the end of the String, split out the new lines like
	// so:
	//
	// " \n \n \n the String \n is awesome \n \n "
	// ^-----------^ ^-------^
	//
	// Excess newlines are converted into single newlines, and spaces discarded.
	// Outside spaces are significant and retained. "Interior" newlines within
	// the main String are ignored, since this is for the purpose of gluing
	// only.
	//
	// - If no splitting is necessary, null is returned.
	// - A newline on its own is returned in an list for consistency.
	List<StringValue> trySplittingHeadTailWhitespace(StringValue single) {
		String str = single.value;

		int headFirstNewlineIdx = -1;
		int headLastNewlineIdx = -1;
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if (c == '\n') {
				if (headFirstNewlineIdx == -1)
					headFirstNewlineIdx = i;
				headLastNewlineIdx = i;
			} else if (c == ' ' || c == '\t')
				continue;
			else
				break;
		}

		int tailLastNewlineIdx = -1;
		int tailFirstNewlineIdx = -1;
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if (c == '\n') {
				if (tailLastNewlineIdx == -1)
					tailLastNewlineIdx = i;
				tailFirstNewlineIdx = i;
			} else if (c == ' ' || c == '\t')
				continue;
			else
				break;
		}

		// No splitting to be done?
		if (headFirstNewlineIdx == -1 && tailLastNewlineIdx == -1)
			return null;

		List<StringValue> listTexts = new ArrayList<StringValue>();
		int innerStrStart = 0;
		int innerStrEnd = str.length();

		if (headFirstNewlineIdx != -1) {
			if (headFirstNewlineIdx > 0) {
				StringValue leadingSpaces = new StringValue(str.substring(0, headFirstNewlineIdx));
				listTexts.add(leadingSpaces);
			}
			listTexts.add(new StringValue("\n"));
			innerStrStart = headLastNewlineIdx + 1;
		}

		if (tailLastNewlineIdx != -1) {
			innerStrEnd = tailFirstNewlineIdx;
		}

		if (innerStrEnd > innerStrStart) {
			String innerStrText = str.substring(innerStrStart, innerStrEnd);
			listTexts.add(new StringValue(innerStrText));
		}

		if (tailLastNewlineIdx != -1 && tailFirstNewlineIdx > headLastNewlineIdx) {
			listTexts.add(new StringValue("\n"));
			if (tailLastNewlineIdx < str.length() - 1) {
				int numSpaces = (str.length() - tailLastNewlineIdx) - 1;
				StringValue trailingSpaces = new StringValue(
						str.substring(tailLastNewlineIdx + 1, numSpaces + tailLastNewlineIdx + 1));
				listTexts.add(trailingSpaces);
			}
		}

		return listTexts;
	}

	/**
	 * Gets the visit/read count of a particular Container at the given path.
	 * For a knot or stitch, that path String will be in the form:
	 *
	 * knot knot.stitch
	 * 
	 * @return The number of times the specific knot or stitch has been
	 *         enountered by the ink engine.
	 * 
	 * @param pathString
	 *            The dot-separated path String of the specific knot or stitch.
	 *
	 */
	public int visitCountAtPathString(String pathString) {
		Integer visitCountOut = visitCounts.get(pathString);

		if (visitCountOut != null)
			return visitCountOut;

		return 0;
	}

	public RTObject getDivertedTargetObject() {
		return divertedTargetObject;
	}

	public void setDivertedTargetObject(RTObject divertedTargetObject) {
		this.divertedTargetObject = divertedTargetObject;
	}

	public boolean isDidSafeExit() {
		return didSafeExit;
	}

	public void setDidSafeExit(boolean didSafeExit) {
		this.didSafeExit = didSafeExit;
	}

	void setCallStack(CallStack cs) {
		callStack = cs;
	}
}
