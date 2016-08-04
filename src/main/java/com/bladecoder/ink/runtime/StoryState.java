package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.bladecoder.ink.runtime.CallStack.Thread;

/// <summary>
/// All story state information is included in the StoryState class,
/// including global variables, read counts, the pointer to the current
/// point in the story, the call stack (for tunnels, functions, etc),
/// and a few other smaller bits and pieces. You can save the current
/// state using the json serialisation functions ToJson and LoadJson.
/// </summary>
public class StoryState {
	/// <summary>
	/// The current version of the state save file JSON-based format.
	/// </summary>
	public static final int kInkSaveStateVersion = 4;
	public static final int kMinCompatibleLoadVersion = 4;

	/// <summary>
	/// Exports the current state to json format, in order to save the game.
	/// </summary>
	/// <returns>The save state in json format.</returns>
	public String ToJson() throws Exception {
		return SimpleJson.HashMapToText(getjsonToken());
	}

	/// <summary>
	/// Loads a previously saved state in JSON format.
	/// </summary>
	/// <param name="json">The JSON String to load.</param>
	public void LoadJson(String json) throws Exception {
		setjsonToken(SimpleJson.textToHashMap(json));
	}

	/// <summary>
	/// Gets the visit/read count of a particular Container at the given path.
	/// For a knot or stitch, that path String will be in the form:
	///
	/// knot
	/// knot.stitch
	///
	/// </summary>
	/// <returns>The number of times the specific knot or stitch has
	/// been enountered by the ink engine.</returns>
	/// <param name="pathString">The dot-separated path String of
	/// the specific knot or stitch.</param>
	public int VisitCountAtPathString(String pathString) {
		Integer visitCountOut = visitCounts.get(pathString);

		if (visitCountOut != null)
			return visitCountOut;

		return 0;
	}

	// REMEMBER! REMEMBER! REMEMBER!
	// When adding state, update the Copy method, and serialisation.
	// REMEMBER! REMEMBER! REMEMBER!

	List<RTObject> outputStream() {
		return _outputStream;
	}

	List<Choice> currentChoices;

	List<String> currentErrors;

	VariablesState variablesState;

	CallStack callStack;

	List<RTObject> evaluationStack;

	RTObject divertedTargetObject;

	private HashMap<String, Integer> visitCounts;
	HashMap<String, Integer> turnIndices;
	int currentTurnIndex;
	int storySeed;
	boolean didSafeExit;

	Story story;
	
	public HashMap<String, Integer> getVisitCounts() {
		return visitCounts;
	}

	Path getcurrentPath() throws Exception {

		if (getcurrentContentObject() == null)
			return null;

		return getcurrentContentObject().getPath();
	}

	void setcurrentPath(Path value) throws Exception {
		if (value != null)
			setcurrentContentObject(story.ContentAtPath(value));
		else
			setcurrentContentObject(null);
	}

	RTObject getcurrentContentObject() throws Exception {
		return callStack.currentElement().getCurrentRTObject();
	}

	void setcurrentContentObject(RTObject value) throws Exception {
		callStack.currentElement().setcurrentRTObject(value);
	}

	Container currentContainer() {
		return callStack.currentElement().currentContainer;
	}

	RTObject getpreviousContentObject() {
		return callStack.getcurrentThread().previousContentRTObject;
	}

	void setpreviousContentObject(RTObject value) {
		callStack.getcurrentThread().previousContentRTObject = value;
	}

	boolean hasError() {
		return currentErrors != null && currentErrors.size() > 0;
	}

	String currentText() {
		StringBuilder sb = new StringBuilder();

		for (RTObject outputObj : _outputStream) {
			StringValue textContent = outputObj instanceof StringValue ? (StringValue) outputObj : null;

			if (textContent != null) {
				sb.append(textContent.value);
			}
		}

		return sb.toString();
	}

	boolean getinExpressionEvaluation() {
		return callStack.currentElement().inExpressionEvaluation;
	}

	void setinExpressionEvaluation(boolean value) {
		callStack.currentElement().inExpressionEvaluation = value;
	}

	StoryState(Story story) throws Exception {
		this.story = story;

		_outputStream = new ArrayList<RTObject>();

		evaluationStack = new ArrayList<RTObject>();

		callStack = new CallStack(story.getRootContentContainer());
		variablesState = new VariablesState(callStack);

		visitCounts = new HashMap<String, Integer>();
		turnIndices = new HashMap<String, Integer>();
		currentTurnIndex = -1;

		// Seed the shuffle random numbers
		long timeSeed = System.currentTimeMillis();

		storySeed = new Random(timeSeed).nextInt() % 100;

		currentChoices = new ArrayList<Choice>();

		GoToStart();
	}

	void GoToStart() {
		callStack.currentElement().currentContainer = story.mainContentContainer();
		callStack.currentElement().currentContentIndex = 0;
	}

	// Warning: Any RTObject content referenced within the StoryState will
	// be re-referenced rather than cloned. This is generally okay though since
	// RTObjects are treated as immutable after they've been set up.
	// (e.g. we don't edit a Runtime.Text after it's been created an added.)
	// I wonder if there's a sensible way to enforce that..??
	StoryState	Copy() throws Exception {
		StoryState copy = new StoryState(story);

		copy.outputStream().addAll(_outputStream);
		copy.currentChoices.addAll(currentChoices);

		if (hasError()) {
			copy.currentErrors = new ArrayList<String>();
			copy.currentErrors.addAll(currentErrors);
		}

		copy.callStack = new CallStack(callStack);

		copy._currentRightGlue = _currentRightGlue;

		copy.variablesState = new VariablesState(copy.callStack);
		copy.variablesState.copyFrom(variablesState);

		copy.evaluationStack.addAll(evaluationStack);

		if (divertedTargetObject != null)
			copy.divertedTargetObject = divertedTargetObject;

		copy.setpreviousContentObject(getpreviousContentObject());

		copy.visitCounts = new HashMap<String, Integer>(visitCounts);
		copy.turnIndices = new HashMap<String, Integer>(turnIndices);
		copy.currentTurnIndex = currentTurnIndex;
		copy.storySeed = storySeed;

		copy.didSafeExit = didSafeExit;

		return copy;
	}

	/// <summary>
	/// Object representation of full JSON state. Usually you should use
	/// LoadJson and ToJson since they serialise directly to String for you.
	/// But, if your game uses Json.Net itself, it may be useful to get
	/// the JToken so that you can integrate it into your own save format.
	/// </summary>
	public HashMap<String, Object> getjsonToken() throws Exception {

		HashMap<String, Object> obj = new HashMap<String, Object>();

		HashMap<String, Object> choiceThreads = null;
		for (Choice c : currentChoices) {
			c.originalChoicePath = c.getchoicePoint().getPath().getComponentsString();
			c.originalThreadIndex = c.getthreadAtGeneration().threadIndex;

			if (callStack.ThreadWithIndex(c.originalThreadIndex) == null) {
				if (choiceThreads == null)
					choiceThreads = new HashMap<String, Object>();

				choiceThreads.put(Integer.toString(c.originalThreadIndex), c.getthreadAtGeneration().jsonToken());
			}
		}
		if (choiceThreads != null)
			obj.put("choiceThreads", choiceThreads);

		obj.put("callstackThreads", callStack.GetJsonToken());
		obj.put("variablesState", variablesState.getjsonToken());

		obj.put("evalStack", Json.listToJArray(evaluationStack));

		obj.put("outputStream", Json.listToJArray(_outputStream));

		obj.put("currentChoices", Json.listToJArray(currentChoices));

		if (_currentRightGlue != null) {
			int rightGluePos = _outputStream.indexOf(_currentRightGlue);
			if (rightGluePos != -1) {
				obj.put("currRightGlue", _outputStream.indexOf(_currentRightGlue));
			}
		}

		if (divertedTargetObject != null)
			obj.put("currentDivertTarget", divertedTargetObject.getPath().getComponentsString());

		obj.put("visitCounts", Json.intHashMapToJRTObject(visitCounts));
		obj.put("turnIndices", Json.intHashMapToJRTObject(turnIndices));
		obj.put("turnIdx", currentTurnIndex);
		obj.put("storySeed", storySeed);

		obj.put("inkSaveVersion", kInkSaveStateVersion);

		// Not using this right now, but could do in future.
		obj.put("inkFormatVersion", Story.inkVersionCurrent);

		return obj;
	}

	void setjsonToken(HashMap<String, Object> value) throws StoryException, Exception {

		HashMap<String, Object> jObject = value;

		Object jSaveVersion = jObject.get("inkSaveVersion");

		if (jSaveVersion == null) {
			throw new StoryException("ink save format incorrect, can't load.");
		} else if ((int) jSaveVersion < kMinCompatibleLoadVersion) {
			throw new StoryException("Ink save format isn't compatible with the current version (saw '" + jSaveVersion
					+ "', but minimum is " + kMinCompatibleLoadVersion + "), so can't load.");
		}

		callStack.SetJsonToken((HashMap<String, Object>) jObject.get("callstackThreads"), story);
		variablesState.setjsonToken((HashMap<String, Object>) jObject.get("variablesState"));

		evaluationStack = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("evalStack"));

		_outputStream = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("outputStream"));

		currentChoices = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("currentChoices"));

		Object propValue = jObject.get("currRightGlue");
		if (propValue != null) {
			int gluePos = (int) propValue;
			if (gluePos >= 0) {
				_currentRightGlue = (Glue) _outputStream.get(gluePos);
			}
		}

		Object currentDivertTargetPath = jObject.get("currentDivertTarget");
		if (currentDivertTargetPath != null) {
			Path divertPath = new Path(currentDivertTargetPath.toString());
			divertedTargetObject = story.ContentAtPath(divertPath);
		}

		visitCounts = Json.jRTObjectToIntHashMap((HashMap<String, Object>) jObject.get("visitCounts"));
		turnIndices = Json.jRTObjectToIntHashMap((HashMap<String, Object>) jObject.get("turnIndices"));
		currentTurnIndex = (int) jObject.get("turnIdx");
		storySeed = (int) jObject.get("storySeed");

		Object jChoiceThreadsObj = jObject.get("choiceThreads");
		HashMap<String, Object> jChoiceThreads = (HashMap<String, Object>) jChoiceThreadsObj;

		for (Choice c : currentChoices) {
			c.setchoicePoint((ChoicePoint) story.ContentAtPath(new Path(c.originalChoicePath)));

			Thread foundActiveThread = callStack.ThreadWithIndex(c.originalThreadIndex);
			if (foundActiveThread != null) {
				c.setthreadAtGeneration(foundActiveThread);
			} else {
				HashMap<String, Object> jSavedChoiceThread = (HashMap<String, Object>) jChoiceThreads
						.get(Integer.toString(c.originalThreadIndex));
				c.setthreadAtGeneration(new CallStack.Thread(jSavedChoiceThread, story));
			}
		}

	}

	void ResetErrors() {
		currentErrors = null;
	}

	void ResetOutput() {
		_outputStream.clear();
	}

	// Push to output stream, but split out newlines in text for consistency
	// in dealing with them later.

	void PushToOutputStream(RTObject obj) throws Exception {
		StringValue text = obj instanceof StringValue ? (StringValue) obj : null;

		if (text != null) {
			List<StringValue> listText = TrySplittingHeadTailWhitespace(text);
			if (listText != null) {
				for (StringValue textObj : listText) {
					PushToOutputStreamIndividual(textObj);
				}
				return;
			}
		}

		PushToOutputStreamIndividual(obj);
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
	List<StringValue> TrySplittingHeadTailWhitespace(StringValue single) throws Exception {
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

	void PushToOutputStreamIndividual(RTObject obj) throws Exception {
		Glue glue = obj instanceof Glue ? (Glue) obj : null;
		StringValue text = obj instanceof StringValue ? (StringValue) obj : null;

		boolean includeInOutput = true;

		if (glue != null) {

			// Found matching left-glue for right-glue? Close it.
			boolean foundMatchingLeftGlue = glue.getisLeft() && _currentRightGlue != null
					&& glue.getParent() == _currentRightGlue.getParent();
			if (foundMatchingLeftGlue) {
				_currentRightGlue = null;
			}

			// Left/Right glue is auto-generated for inline expressions
			// where we want to absorb newlines but only in a certain direction.
			// "Bi" glue is written by the user in their ink with <>
			if (glue.getisLeft() || glue.getisBi()) {
				TrimNewlinesFromOutputStream(foundMatchingLeftGlue);
			}

			// New right-glue
			boolean isNewRightGlue = glue.getisRight() && _currentRightGlue == null;
			if (isNewRightGlue) {
				_currentRightGlue = glue;
			}

			includeInOutput = glue.getisBi() || isNewRightGlue;
		}

		else if (text != null) {

			if (currentGlueIndex() != -1) {

				// Absorb any new newlines if there's existing glue
				// in the output stream.
				// Also trim any extra whitespace (spaces/tabs) if so.
				if (text.getisNewline()) {
					TrimFromExistingGlue();
					includeInOutput = false;
				}

				// Able to completely reset when
				else if (text.getisNonWhitespace()) {
					RemoveExistingGlue();
					_currentRightGlue = null;
				}
			} else if (text.getisNewline()) {
				if (outputStreamEndsInNewline() || !outputStreamContainsContent())
					includeInOutput = false;
			}
		}

		if (includeInOutput) {
			_outputStream.add(obj);
		}
	}

	void TrimNewlinesFromOutputStream(boolean stopAndRemoveRightGlue) throws Exception {
		int removeWhitespaceFrom = -1;
		int rightGluePos = -1;
		boolean foundNonWhitespace = false;

		// Work back from the end, and try to find the point where
		// we need to start removing content. There are two ways:
		// - Start from the matching right-glue (because we just saw a
		// left-glue)
		// - Simply work backwards to find the first newline in a String of
		// whitespace
		int i = _outputStream.size() - 1;
		while (i >= 0) {
			RTObject obj = _outputStream.get(i);
			ControlCommand cmd = obj instanceof ControlCommand ? (ControlCommand) obj : null;
			StringValue txt = obj instanceof StringValue ? (StringValue) obj : null;
			Glue glue = obj instanceof Glue ? (Glue) obj : null;

			if (cmd != null || (txt != null && txt.getisNonWhitespace())) {
				foundNonWhitespace = true;

				if (!stopAndRemoveRightGlue)
					break;
			} else if (stopAndRemoveRightGlue && glue != null && glue.getisRight()) {
				rightGluePos = i;
				break;
			} else if (txt != null && txt.getisNewline() && !foundNonWhitespace) {
				removeWhitespaceFrom = i;
			}
			i--;
		}

		// Remove the whitespace
		if (removeWhitespaceFrom >= 0) {
			i = removeWhitespaceFrom;
			while (i < _outputStream.size()) {
				StringValue text = _outputStream.get(i) instanceof StringValue ? (StringValue) _outputStream.get(i)
						: null;
				if (text != null) {
					_outputStream.remove(i);
				} else {
					i++;
				}
			}
		}

		// Remove the glue (it will come before the whitespace,
		// so index is still valid)
		if (stopAndRemoveRightGlue && rightGluePos > -1)
			_outputStream.remove(rightGluePos);
	}

	void TrimFromExistingGlue() throws Exception {
		int i = currentGlueIndex();
		while (i < _outputStream.size()) {
			StringValue txt = _outputStream.get(i) instanceof StringValue ? (StringValue) _outputStream.get(i) : null;

			if (txt != null && !txt.getisNonWhitespace())
				_outputStream.remove(i);
			else
				i++;
		}
	}

	// Only called when non-whitespace is appended
	void RemoveExistingGlue() {
		for (int i = _outputStream.size() - 1; i >= 0; i--) {
			RTObject c = _outputStream.get(i);
			if (c instanceof Glue) {
				_outputStream.remove(i);
			} else if (c instanceof ControlCommand) { // e.g.
														// BeginString
				break;
			}
		}
	}

	int currentGlueIndex() {
		for (int i = _outputStream.size() - 1; i >= 0; i--) {
			RTObject c = _outputStream.get(i);
			Glue glue = c instanceof Glue ? (Glue) c : null;
			if (glue != null)
				return i;
			else if (c instanceof ControlCommand) // e.g. BeginString
				break;
		}
		return -1;
	}

	boolean outputStreamEndsInNewline() throws Exception {
		if (_outputStream.size() > 0) {

			for (int i = _outputStream.size() - 1; i >= 0; i--) {
				RTObject obj = _outputStream.get(i);
				if (obj instanceof ControlCommand) // e.g. BeginString
					break;
				StringValue text = _outputStream.get(i) instanceof StringValue ? (StringValue) _outputStream.get(i)
						: null;

				if (text != null) {
					if (text.getisNewline())
						return true;
					else if (text.getisNonWhitespace())
						break;
				}
			}
		}

		return false;
	}

	boolean outputStreamContainsContent() {
		for (RTObject content : _outputStream) {
			if (content instanceof StringValue)
				return true;
		}
		return false;
	}

	boolean inStringEvaluation() {
		for (int i = _outputStream.size() - 1; i >= 0; i--) {
			ControlCommand cmd = _outputStream.get(i) instanceof ControlCommand ? (ControlCommand) _outputStream.get(i)
					: null;

			if (cmd != null && cmd.getcommandType() == ControlCommand.CommandType.BeginString) {
				return true;
			}
		}

		return false;
	}

	void PushEvaluationStack(RTObject obj) {
		evaluationStack.add(obj);
	}

	RTObject PopEvaluationStack() {
		RTObject obj = evaluationStack.get(evaluationStack.size() - 1);
		evaluationStack.remove(evaluationStack.size() - 1);
		return obj;
	}

	RTObject PeekEvaluationStack() {
		return evaluationStack.get(evaluationStack.size() - 1);
	}

	List<RTObject>

			PopEvaluationStack(int numberOfObjects) throws Exception {
		if (numberOfObjects > evaluationStack.size()) {
			throw new Exception("trying to pop too many objects");
		}

		List<RTObject> popped = new ArrayList<RTObject>(
				evaluationStack.subList(evaluationStack.size() - numberOfObjects, evaluationStack.size()));
		evaluationStack.subList(evaluationStack.size() - numberOfObjects, numberOfObjects).clear();
		return popped;
	}

	void ForceEndFlow() throws Exception {
		setcurrentContentObject(null);

		while (callStack.canPopThread())
			callStack.PopThread();

		while (callStack.canPop())
			callStack.Pop();

		currentChoices.clear();

		didSafeExit = true;
	}

	// Don't make public since the method need to be wrapped in Story for visit
	// counting

	void SetChosenPath(Path path) throws Exception {
		// Changing direction, assume we need to clear current set of choices
		currentChoices.clear();

		setcurrentPath(path);

		currentTurnIndex++;
	}

	void AddError(String message) {
		// TODO: Could just add to output?
		if (currentErrors == null) {
			currentErrors = new ArrayList<String>();
		}

		currentErrors.add(message);
	}

	// REMEMBER! REMEMBER! REMEMBER!
	// When adding state, update the Copy method and serialisation
	// REMEMBER! REMEMBER! REMEMBER!

	private List<RTObject> _outputStream;
	private Glue _currentRightGlue;
}
