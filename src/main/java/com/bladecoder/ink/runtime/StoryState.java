package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.CallStack.Element;
import com.bladecoder.ink.runtime.SimpleJson.InnerWriter;
import com.bladecoder.ink.runtime.SimpleJson.Writer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

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
    public static final int kInkSaveStateVersion = 9;
    public static final int kMinCompatibleLoadVersion = 8;
    public static final String kDefaultFlowName = "DEFAULT_FLOW";

    // REMEMBER! REMEMBER! REMEMBER!
    // When adding state, update the Copy method and serialisation
    // REMEMBER! REMEMBER! REMEMBER!

    // TODO: Consider removing currentErrors / currentWarnings altogether
    // and relying on client error handler code immediately handling StoryExceptions
    // etc
    // Or is there a specific reason we need to collect potentially multiple
    // errors before throwing/exiting?
    private List<String> currentErrors;
    private List<String> currentWarnings;
    private int currentTurnIndex;
    private boolean didSafeExit;
    private final Pointer divertedPointer = new Pointer();
    private List<RTObject> evaluationStack;
    private Story story;
    private int storySeed;
    private int previousRandom;
    private HashMap<String, Integer> turnIndices;
    private VariablesState variablesState;
    private HashMap<String, Integer> visitCounts;
    private String currentText;

    private boolean outputStreamTextDirty = true;
    private boolean outputStreamTagsDirty = true;
    private List<String> currentTags;

    private StatePatch patch;

    private HashMap<String, Flow> namedFlows;
    private Flow currentFlow;

    private List<String> aliveFlowNames;

    boolean aliveFlowNamesDirty = true;

    StoryState(Story story) {
        this.story = story;

        currentFlow = new Flow(kDefaultFlowName, story);
        outputStreamDirty();
        aliveFlowNamesDirty = true;

        evaluationStack = new ArrayList<>();

        variablesState = new VariablesState(getCallStack(), story.getListDefinitions());

        visitCounts = new HashMap<>();
        turnIndices = new HashMap<>();
        currentTurnIndex = -1;

        // Seed the shuffle random numbers
        long timeSeed = System.currentTimeMillis();

        storySeed = new Random(timeSeed).nextInt() % 100;
        previousRandom = 0;

        goToStart();
    }

    int getCallStackDepth() {
        return getCallStack().getDepth();
    }

    void addError(String message, boolean isWarning) {
        if (!isWarning) {
            if (currentErrors == null)
                currentErrors = new ArrayList<>();

            currentErrors.add(message);
        } else {
            if (currentWarnings == null)
                currentWarnings = new ArrayList<>();

            currentWarnings.add(message);
        }
    }

    // Warning: Any RTObject content referenced within the StoryState will
    // be re-referenced rather than cloned. This is generally okay though since
    // RTObjects are treated as immutable after they've been set up.
    // (e.g. we don't edit a Runtime.StringValue after it's been created an added.)
    // I wonder if there's a sensible way to enforce that..??
    StoryState copyAndStartPatching() {
        StoryState copy = new StoryState(story);

        copy.patch = new StatePatch(patch);

        // Hijack the new default flow to become a copy of our current one
        // If the patch is applied, then this new flow will replace the old one in
        // _namedFlows
        copy.currentFlow.name = currentFlow.name;
        copy.currentFlow.callStack = new CallStack(currentFlow.callStack);
        copy.currentFlow.currentChoices.addAll(currentFlow.currentChoices);
        copy.currentFlow.outputStream.addAll(currentFlow.outputStream);
        copy.outputStreamDirty();

        // The copy of the state has its own copy of the named flows dictionary,
        // except with the current flow replaced with the copy above
        // (Assuming we're in multi-flow mode at all. If we're not then
        // the above copy is simply the default flow copy and we're done)
        if (namedFlows != null) {
            copy.namedFlows = new HashMap<>();
            for (Map.Entry<String, Flow> namedFlow : namedFlows.entrySet())
                copy.namedFlows.put(namedFlow.getKey(), namedFlow.getValue());
            copy.namedFlows.put(currentFlow.name, copy.currentFlow);
            copy.aliveFlowNamesDirty = true;
        }

        if (hasError()) {
            copy.currentErrors = new ArrayList<>();
            copy.currentErrors.addAll(currentErrors);
        }

        if (hasWarning()) {
            copy.currentWarnings = new ArrayList<>();
            copy.currentWarnings.addAll(currentWarnings);
        }

        // ref copy - exactly the same variables state!
        // we're expecting not to read it only while in patch mode
        // (though the callstack will be modified)
        copy.variablesState = variablesState;
        copy.variablesState.setCallStack(copy.getCallStack());
        copy.variablesState.setPatch(copy.patch);

        copy.evaluationStack.addAll(evaluationStack);

        if (!divertedPointer.isNull())
            copy.divertedPointer.assign(divertedPointer);

        copy.setPreviousPointer(getPreviousPointer());

        // visit counts and turn indicies will be read only, not modified
        // while in patch mode
        copy.visitCounts = visitCounts;
        copy.turnIndices = turnIndices;

        copy.currentTurnIndex = currentTurnIndex;
        copy.storySeed = storySeed;
        copy.previousRandom = previousRandom;

        copy.setDidSafeExit(didSafeExit);

        return copy;
    }

    void popFromOutputStream(int count) {
        getOutputStream().subList(getOutputStream().size() - count, getOutputStream().size()).clear();

        outputStreamDirty();
    }

    String getCurrentText() {
        if (outputStreamTextDirty) {
            StringBuilder sb = new StringBuilder();

            for (RTObject outputObj : getOutputStream()) {
                StringValue textContent = null;
                if (outputObj instanceof StringValue)
                    textContent = (StringValue) outputObj;

                if (textContent != null) {
                    sb.append(textContent.value);
                }
            }

            currentText = cleanOutputWhitespace(sb.toString());

            outputStreamTextDirty = false;
        }

        return currentText;
    }

    /**
     * Cleans inline whitespace in the following way: - Removes all whitespace from
     * the start and end of line (including just before a \n) - Turns all
     * consecutive space and tab runs into single spaces (HTML style)
     */
    String cleanOutputWhitespace(String str) {
        StringBuilder sb = new StringBuilder(str.length());

        int currentWhitespaceStart = -1;
        int startOfLine = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            boolean isInlineWhitespace = c == ' ' || c == '\t';

            if (isInlineWhitespace && currentWhitespaceStart == -1)
                currentWhitespaceStart = i;

            if (!isInlineWhitespace) {
                if (c != '\n' && currentWhitespaceStart > 0 && currentWhitespaceStart != startOfLine) {
                    sb.append(' ');
                }
                currentWhitespaceStart = -1;
            }

            if (c == '\n')
                startOfLine = i + 1;

            if (!isInlineWhitespace)
                sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Ends the current ink flow, unwrapping the callstack but without affecting any
     * variables. Useful if the ink is (say) in the middle a nested tunnel, and you
     * want it to reset so that you can divert elsewhere using ChoosePathString().
     * Otherwise, after finishing the content you diverted to, it would continue
     * where it left off. Calling this is equivalent to calling -&gt; END in ink.
     */
    public void forceEnd() throws Exception {

        getCallStack().reset();

        currentFlow.currentChoices.clear();

        setCurrentPointer(Pointer.Null);
        setPreviousPointer(Pointer.Null);

        setDidSafeExit(true);
    }

    // Add the end of a function call, trim any whitespace from the end.
    // We always trim the start and end of the text that a function produces.
    // The start whitespace is discard as it is generated, and the end
    // whitespace is trimmed in one go here when we pop the function.
    void trimWhitespaceFromFunctionEnd() {
        assert (getCallStack().getCurrentElement().type == PushPopType.Function);

        int functionStartPoint = getCallStack().getCurrentElement().functionStartInOuputStream;

        // If the start point has become -1, it means that some non-whitespace
        // text has been pushed, so it's safe to go as far back as we're able.
        if (functionStartPoint == -1) {
            functionStartPoint = 0;
        }

        // Trim whitespace from END of function call
        for (int i = getOutputStream().size() - 1; i >= functionStartPoint; i--) {
            RTObject obj = getOutputStream().get(i);

            if (!(obj instanceof StringValue))
                continue;
            StringValue txt = (StringValue) obj;

            if (obj instanceof ControlCommand)
                break;

            if (txt.isNewline() || txt.isInlineWhitespace()) {
                getOutputStream().remove(i);
                outputStreamDirty();
            } else {
                break;
            }
        }
    }

    void popCallstack() throws Exception {
        popCallstack(null);
    }

    void popCallstack(PushPopType popType) throws Exception {
        // Add the end of a function call, trim any whitespace from the end.
        if (getCallStack().getCurrentElement().type == PushPopType.Function)
            trimWhitespaceFromFunctionEnd();

        getCallStack().pop(popType);
    }

    Pointer getCurrentPointer() {
        return getCallStack().getCurrentElement().currentPointer;
    }

    List<String> getCurrentTags() {
        if (outputStreamTagsDirty) {
            currentTags = new ArrayList<>();

            for (RTObject outputObj : getOutputStream()) {
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

    public String getCurrentFlowName() {
        return currentFlow.name;
    }

    public boolean currentFlowIsDefaultFlow() {
        return Objects.equals(currentFlow.name, kDefaultFlowName);
    }

    public List<String> aliveFlowNames() {
        if (aliveFlowNamesDirty) {
            aliveFlowNames = new ArrayList<>();

            if (namedFlows != null) {
                for (String flowName : namedFlows.keySet()) {
                    if (!Objects.equals(flowName, kDefaultFlowName)) {
                        aliveFlowNames.add(flowName);
                    }
                }
            }

            aliveFlowNamesDirty = false;
        }

        return aliveFlowNames;
    }

    boolean getInExpressionEvaluation() {
        return getCallStack().getCurrentElement().inExpressionEvaluation;
    }

    Pointer getPreviousPointer() {
        return getCallStack().getcurrentThread().previousPointer;
    }

    void goToStart() {
        getCallStack().getCurrentElement().currentPointer.assign(Pointer.startOf(story.getMainContentContainer()));
    }

    void switchFlowInternal(String flowName) throws Exception {
        if (flowName == null)
            throw new Exception("Must pass a non-null string to Story.SwitchFlow");

        if (namedFlows == null) {
            namedFlows = new HashMap<>();
            namedFlows.put(kDefaultFlowName, currentFlow);
        }

        if (flowName.equals(currentFlow.name)) {
            return;
        }

        Flow flow = namedFlows.get(flowName);
        if (flow == null) {
            flow = new Flow(flowName, story);
            namedFlows.put(flowName, flow);
            aliveFlowNamesDirty = true;
        }

        currentFlow = flow;
        variablesState.setCallStack(currentFlow.callStack);

        // Cause text to be regenerated from output stream if necessary
        outputStreamDirty();
    }

    void switchToDefaultFlowInternal() throws Exception {
        if (namedFlows == null)
            return;

        switchFlowInternal(kDefaultFlowName);
    }

    void removeFlowInternal(String flowName) throws Exception {
        if (flowName == null)
            throw new Exception("Must pass a non-null string to Story.DestroyFlow");
        if (flowName.equals(kDefaultFlowName))
            throw new Exception("Cannot destroy default flow");

        // If we're currently in the flow that's being removed, switch back to default
        if (currentFlow.name.equals(flowName)) {
            switchToDefaultFlowInternal();
        }

        namedFlows.remove(flowName);
        aliveFlowNamesDirty = true;
    }

    boolean hasError() {
        return currentErrors != null && currentErrors.size() > 0;
    }

    boolean inStringEvaluation() {
        for (int i = getOutputStream().size() - 1; i >= 0; i--) {
            ControlCommand cmd = getOutputStream().get(i) instanceof ControlCommand
                    ? (ControlCommand) getOutputStream().get(i)
                    : null;

            if (cmd != null && cmd.getCommandType() == ControlCommand.CommandType.BeginString) {
                return true;
            }
        }

        return false;
    }

    /**
     * Loads a previously saved state in JSON format.
     *
     * @param json The JSON String to load.
     */
    public void loadJson(String json) throws Exception {
        HashMap<String, Object> jObject = SimpleJson.textToDictionary(json);
        loadJsonObj(jObject);
    }

    List<Choice> getCurrentChoices() {
        // If we can continue generating text content rather than choices,
        // then we reflect the choice list as being empty, since choices
        // should always come at the end.
        if (canContinue())
            return new ArrayList<>();

        return currentFlow.currentChoices;
    }

    List<Choice> getGeneratedChoices() {
        return currentFlow.currentChoices;
    }

    boolean canContinue() {
        return !getCurrentPointer().isNull() && !hasError();
    }

    List<String> getCurrentErrors() {
        return currentErrors;
    }

    List<String> getCurrentWarnings() {
        return currentWarnings;
    }

    boolean hasWarning() {
        return currentWarnings != null && currentWarnings.size() > 0;
    }

    List<RTObject> getOutputStream() {
        return currentFlow.outputStream;
    }

    CallStack getCallStack() {
        return currentFlow.callStack;
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

    int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    boolean outputStreamContainsContent() {
        for (RTObject content : getOutputStream()) {
            if (content instanceof StringValue)
                return true;
        }
        return false;
    }

    boolean outputStreamEndsInNewline() {
        if (getOutputStream().size() > 0) {

            for (int i = getOutputStream().size() - 1; i >= 0; i--) {
                RTObject obj = getOutputStream().get(i);
                if (obj instanceof ControlCommand) // e.g. BeginString
                    break;
                StringValue text = getOutputStream().get(i) instanceof StringValue
                        ? (StringValue) getOutputStream().get(i)
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

        List<RTObject> popped = new ArrayList<>(
                evaluationStack.subList(evaluationStack.size() - numberOfObjects, evaluationStack.size()));
        evaluationStack.subList(evaluationStack.size() - numberOfObjects, evaluationStack.size()).clear();

        return popped;
    }

    void pushEvaluationStack(RTObject obj) {

        // Include metadata about the origin List for set values when
        // they're used, so that lower level functions can make use
        // of the origin list to get related items, or make comparisons
        // with the integer values etc.
        ListValue listValue = null;
        if (obj instanceof ListValue)
            listValue = (ListValue) obj;

        if (listValue != null) {
            // Update origin when list is has something to indicate the list
            // origin
            InkList rawList = listValue.getValue();

            if (rawList.getOriginNames() != null) {

                if (rawList.getOrigins() == null)
                    rawList.setOrigins(new ArrayList<ListDefinition>());

                rawList.getOrigins().clear();

                for (String n : rawList.getOriginNames()) {
                    ListDefinition def = story.getListDefinitions().getListDefinition(n);
                    if (!rawList.getOrigins().contains(def))
                        rawList.getOrigins().add(def);

                }
            }
        }

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
                outputStreamDirty();
                return;
            }
        }

        pushToOutputStreamIndividual(obj);
    }

    void pushToOutputStreamIndividual(RTObject obj) {
        Glue glue = obj instanceof Glue ? (Glue) obj : null;
        StringValue text = obj instanceof StringValue ? (StringValue) obj : null;

        boolean includeInOutput = true;

        // New glue, so chomp away any whitespace from the end of the stream
        if (glue != null) {
            trimNewlinesFromOutputStream();
            includeInOutput = true;
        }
        // New text: do we really want to append it, if it's whitespace?
        // Two different reasons for whitespace to be thrown away:
        // - Function start/end trimming
        // - User defined glue: <>
        // We also need to know when to stop trimming, when there's non-whitespace.
        else if (text != null) {

            // Where does the current function call begin?
            int functionTrimIndex = -1;
            Element currEl = getCallStack().getCurrentElement();
            if (currEl.type == PushPopType.Function) {
                functionTrimIndex = currEl.functionStartInOuputStream;
            }

            // Do 2 things:
            // - Find latest glue
            // - Check whether we're in the middle of string evaluation
            // If we're in string eval within the current function, we
            // don't want to trim back further than the length of the current string.
            int glueTrimIndex = -1;
            for (int i = getOutputStream().size() - 1; i >= 0; i--) {
                RTObject o = getOutputStream().get(i);
                ControlCommand c = o instanceof ControlCommand ? (ControlCommand) o : null;
                Glue g = o instanceof Glue ? (Glue) o : null;

                // Find latest glue
                if (g != null) {
                    glueTrimIndex = i;
                    break;
                }

                // Don't function-trim past the start of a string evaluation section
                else if (c != null && c.getCommandType() == ControlCommand.CommandType.BeginString) {
                    if (i >= functionTrimIndex) {
                        functionTrimIndex = -1;
                    }
                    break;
                }
            }

            // Where is the most agressive (earliest) trim point?
            int trimIndex = -1;
            if (glueTrimIndex != -1 && functionTrimIndex != -1)
                trimIndex = Math.min(functionTrimIndex, glueTrimIndex);
            else if (glueTrimIndex != -1)
                trimIndex = glueTrimIndex;
            else
                trimIndex = functionTrimIndex;

            // So, are we trimming then?
            if (trimIndex != -1) {

                // While trimming, we want to throw all newlines away,
                // whether due to glue or the start of a function
                if (text.isNewline()) {
                    includeInOutput = false;
                }

                // Able to completely reset when normal text is pushed
                else if (text.isNonWhitespace()) {

                    if (glueTrimIndex > -1)
                        removeExistingGlue();

                    // Tell all functions in callstack that we have seen proper text,
                    // so trimming whitespace at the start is done.
                    if (functionTrimIndex > -1) {
                        List<Element> callstackElements = getCallStack().getElements();
                        for (int i = callstackElements.size() - 1; i >= 0; i--) {
                            Element el = callstackElements.get(i);
                            if (el.type == PushPopType.Function) {
                                el.functionStartInOuputStream = -1;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }

            // De-duplicate newlines, and don't ever lead with a newline
            else if (text.isNewline()) {
                if (outputStreamEndsInNewline() || !outputStreamContainsContent())
                    includeInOutput = false;
            }
        }

        if (includeInOutput) {
            getOutputStream().add(obj);
            outputStreamDirty();
        }

    }

    // Only called when non-whitespace is appended
    void removeExistingGlue() {
        for (int i = getOutputStream().size() - 1; i >= 0; i--) {
            RTObject c = getOutputStream().get(i);
            if (c instanceof Glue) {
                getOutputStream().remove(i);
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

    void resetOutput(List<RTObject> objs) {
        getOutputStream().clear();
        if (objs != null)
            getOutputStream().addAll(objs);
        outputStreamDirty();
    }

    void resetOutput() {
        resetOutput(null);
    }

    // Don't make public since the method need to be wrapped in Story for visit
    // counting
    void setChosenPath(Path path, boolean incrementingTurnIndex) throws Exception {
        // Changing direction, assume we need to clear current set of choices
        currentFlow.currentChoices.clear();

        final Pointer newPointer = new Pointer(story.pointerAtPath(path));
        if (!newPointer.isNull() && newPointer.index == -1)
            newPointer.index = 0;

        setCurrentPointer(newPointer);

        if (incrementingTurnIndex)
            currentTurnIndex++;
    }

    void startFunctionEvaluationFromGame(Container funcContainer, Object[] arguments) throws Exception {
        getCallStack().push(PushPopType.FunctionEvaluationFromGame, evaluationStack.size());
        getCallStack().getCurrentElement().currentPointer.assign(Pointer.startOf(funcContainer));

        passArgumentsToEvaluationStack(arguments);
    }

    void passArgumentsToEvaluationStack(Object[] arguments) throws Exception {
        // Pass arguments onto the evaluation stack
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (!(arguments[i] instanceof Integer || arguments[i] instanceof Float || arguments[i] instanceof String
                        || arguments[i] instanceof Boolean || arguments[i] instanceof InkList)) {
                    throw new Exception(
                            "ink arguments when calling EvaluateFunction / ChoosePathStringWithParameters must be " +
                                    "int, float, string, bool or InkList. Argument was "
                                    + (arguments[i] == null ? "null" : arguments[i].getClass().getName()));

                }

                pushEvaluationStack(Value.create(arguments[i]));
            }
        }
    }

    boolean tryExitFunctionEvaluationFromGame() {
        if (getCallStack().getCurrentElement().type == PushPopType.FunctionEvaluationFromGame) {
            setCurrentPointer(Pointer.Null);
            didSafeExit = true;
            return true;
        }

        return false;
    }

    Object completeFunctionEvaluationFromGame() throws Exception {
        if (getCallStack().getCurrentElement().type != PushPopType.FunctionEvaluationFromGame) {
            throw new Exception("Expected external function evaluation to be complete. Stack trace: "
                    + getCallStack().getCallStackTrace());
        }

        int originalEvaluationStackHeight = getCallStack().getCurrentElement().evaluationStackHeightWhenPushed;

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

        // Finally, pop the external function evaluation
        getCallStack().pop(PushPopType.FunctionEvaluationFromGame);

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

    void setCurrentPointer(Pointer value) {
        getCallStack().getCurrentElement().currentPointer.assign(value);
    }

    void setInExpressionEvaluation(boolean value) {
        getCallStack().getCurrentElement().inExpressionEvaluation = value;
    }

    void setPreviousPointer(Pointer value) {
        getCallStack().getcurrentThread().previousPointer.assign(value);
    }

    /**
     * Exports the current state to json format, in order to save the game.
     *
     * @return The save state in json format.
     */
    public String toJson() throws Exception {
        SimpleJson.Writer writer = new SimpleJson.Writer();
        writeJson(writer);

        return writer.toString();
    }

    /**
     * Exports the current state to json format, in order to save the game. For this
     * overload you can pass in a custom stream, such as a FileStream.
     *
     * @throws Exception
     */
    public void toJson(OutputStream stream) throws Exception {
        SimpleJson.Writer writer = new SimpleJson.Writer(stream);
        writeJson(writer);
    }

    void trimNewlinesFromOutputStream() {
        int removeWhitespaceFrom = -1;

        // Work back from the end, and try to find the point where
        // we need to start removing content.
        // - Simply work backwards to find the first newline in a String of
        // whitespace
        // e.g. This is the content \n \n\n
        // ^---------^ whitespace to remove
        // ^--- first while loop stops here
        int i = getOutputStream().size() - 1;
        while (i >= 0) {
            RTObject obj = getOutputStream().get(i);
            ControlCommand cmd = obj instanceof ControlCommand ? (ControlCommand) obj : null;
            StringValue txt = obj instanceof StringValue ? (StringValue) obj : null;

            if (cmd != null || (txt != null && txt.isNonWhitespace())) {
                break;
            } else if (txt != null && txt.isNewline()) {
                removeWhitespaceFrom = i;
            }
            i--;
        }

        // Remove the whitespace
        if (removeWhitespaceFrom >= 0) {
            i = removeWhitespaceFrom;
            while (i < getOutputStream().size()) {
                StringValue text = getOutputStream().get(i) instanceof StringValue
                        ? (StringValue) getOutputStream().get(i)
                        : null;
                if (text != null) {
                    getOutputStream().remove(i);
                } else {
                    i++;
                }
            }
        }

        outputStreamDirty();
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
        for (int i = 0; i < str.length(); i++) {
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
        for (int i = str.length() - 1; i >= 0; i--) {
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

        List<StringValue> listTexts = new ArrayList<>();
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
     * Gets the visit/read count of a particular Container at the given path. For a
     * knot or stitch, that path String will be in the form:
     * <p>
     * knot knot.stitch
     *
     * @param pathString The dot-separated path String of the specific knot or
     *                   stitch.
     * @return The number of times the specific knot or stitch has been enountered
     * by the ink engine.
     * @throws Exception
     */
    public int visitCountAtPathString(String pathString) throws Exception {
        Integer visitCountOut;

        if (patch != null) {
            Container container = story.contentAtPath(new Path(pathString)).getContainer();
            if (container == null)
                throw new Exception("Content at path not found: " + pathString);

            visitCountOut = patch.getVisitCount(container);
            if (visitCountOut != null)
                return visitCountOut;
        }

        visitCountOut = visitCounts.get(pathString);
        if (visitCountOut != null)
            return visitCountOut;

        return 0;
    }

    int visitCountForContainer(Container container) throws Exception {
        if (!container.getVisitsShouldBeCounted()) {
            story.error("Read count for target (" + container.getName() + " - on " + container.getDebugMetadata()
                    + ") unknown.");
            return 0;
        }

        if (patch != null && patch.getVisitCount(container) != null)
            return patch.getVisitCount(container);

        String containerPathStr = container.getPath().toString();

        if (visitCounts.containsKey(containerPathStr))
            return visitCounts.get(containerPathStr);

        return 0;
    }

    void incrementVisitCountForContainer(Container container) throws Exception {
        if (patch != null) {
            int currCount = visitCountForContainer(container);
            currCount++;
            patch.setVisitCount(container, currCount);

            return;
        }

        Integer count = 0;
        String containerPathStr = container.getPath().toString();

        if (visitCounts.containsKey(containerPathStr))
            count = visitCounts.get(containerPathStr);

        count++;
        visitCounts.put(containerPathStr, count);
    }

    void recordTurnIndexVisitToContainer(Container container) {
        if (patch != null) {
            patch.setTurnIndex(container, currentTurnIndex);
            return;
        }

        String containerPathStr = container.getPath().toString();
        turnIndices.put(containerPathStr, currentTurnIndex);
    }

    int turnsSinceForContainer(Container container) throws Exception {
        if (!container.getTurnIndexShouldBeCounted()) {
            story.error("TURNS_SINCE() for target (" + container.getName() + " - on " + container.getDebugMetadata()
                    + ") unknown.");
        }

        int index = 0;

        if (patch != null && patch.getTurnIndex(container) != null) {
            index = patch.getTurnIndex(container);
            return currentTurnIndex - index;
        }

        String containerPathStr = container.getPath().toString();

        if (turnIndices.containsKey(containerPathStr)) {
            index = turnIndices.get(containerPathStr);
            return currentTurnIndex - index;
        } else {
            return -1;
        }
    }

    public Pointer getDivertedPointer() {
        return divertedPointer;
    }

    public void setDivertedPointer(Pointer p) {
        divertedPointer.assign(p);
    }

    public boolean isDidSafeExit() {
        return didSafeExit;
    }

    public void setDidSafeExit(boolean didSafeExit) {
        this.didSafeExit = didSafeExit;
    }

    void restoreAfterPatch() {
        // VariablesState was being borrowed by the patched
        // state, so restore it with our own callstack.
        // _patch will be null normally, but if you're in the
        // middle of a save, it may contain a _patch for save purpsoes.
        variablesState.setCallStack(getCallStack());
        variablesState.setPatch(patch); // usually null
    }

    void applyAnyPatch() {
        if (patch == null)
            return;

        variablesState.applyPatch();

        for (Entry<Container, Integer> pathToCount : patch.getVisitCounts().entrySet())
            applyCountChanges(pathToCount.getKey(), pathToCount.getValue(), true);

        for (Entry<Container, Integer> pathToIndex : patch.getTurnIndices().entrySet())
            applyCountChanges(pathToIndex.getKey(), pathToIndex.getValue(), false);

        patch = null;
    }

    void applyCountChanges(Container container, int newCount, boolean isVisit) {
        HashMap<String, Integer> counts = isVisit ? visitCounts : turnIndices;

        counts.put(container.getPath().toString(), newCount);
    }

    void writeJson(SimpleJson.Writer writer) throws Exception {
        writer.writeObjectStart();

        // Flows
        writer.writePropertyStart("flows");
        writer.writeObjectStart();

        // Multi-flow
        if (namedFlows != null) {
            for (Entry<String, Flow> namedFlow : namedFlows.entrySet()) {
                final Flow flow = namedFlow.getValue();

                writer.writeProperty(namedFlow.getKey(), new InnerWriter() {

                    @Override
                    public void write(Writer w) throws Exception {
                        flow.writeJson(w);
                    }
                });
            }
        }

        // Single flow
        else {
            writer.writeProperty(currentFlow.name, new InnerWriter() {
                @Override
                public void write(Writer w) throws Exception {
                    currentFlow.writeJson(w);
                }
            });
        }

        writer.writeObjectEnd();
        writer.writePropertyEnd(); // end of flows

        writer.writeProperty("currentFlowName", currentFlow.name);

        writer.writeProperty("variablesState", new InnerWriter() {
            @Override
            public void write(Writer w) throws Exception {
                variablesState.writeJson(w);
            }
        });

        writer.writeProperty("evalStack", new InnerWriter() {
            @Override
            public void write(Writer w) throws Exception {
                Json.writeListRuntimeObjs(w, evaluationStack);
            }
        });

        if (!divertedPointer.isNull())
            writer.writeProperty("currentDivertTarget", divertedPointer.getPath().getComponentsString());

        writer.writeProperty("visitCounts", new InnerWriter() {
            @Override
            public void write(Writer w) throws Exception {
                Json.writeIntDictionary(w, visitCounts);
            }
        });

        writer.writeProperty("turnIndices", new InnerWriter() {
            @Override
            public void write(Writer w) throws Exception {
                Json.writeIntDictionary(w, turnIndices);
            }
        });

        writer.writeProperty("turnIdx", currentTurnIndex);
        writer.writeProperty("storySeed", storySeed);
        writer.writeProperty("previousRandom", previousRandom);

        writer.writeProperty("inkSaveVersion", kInkSaveStateVersion);

        // Not using this right now, but could do in future.
        writer.writeProperty("inkFormatVersion", Story.inkVersionCurrent);

        writer.writeObjectEnd();
    }

    @SuppressWarnings("unchecked")
    void loadJsonObj(HashMap<String, Object> jObject) throws Exception {
        Object jSaveVersion = jObject.get("inkSaveVersion");

        if (jSaveVersion == null) {
            throw new Exception("ink save format incorrect, can't load.");
        } else if ((int) jSaveVersion < kMinCompatibleLoadVersion) {
            throw new Exception("Ink save format isn't compatible with the current version (saw '" + jSaveVersion
                    + "', but minimum is " + kMinCompatibleLoadVersion + "), so can't load.");
        }

        // Flows: Always exists in latest format (even if there's just one default)
        // but this dictionary doesn't exist in prev format
        Object flowsObj = jObject.get("flows");
        if (flowsObj != null) {
            HashMap<String, Object> flowsObjDict = (HashMap<String, Object>) flowsObj;

            // Single default flow
            if (flowsObjDict.size() == 1)
                namedFlows = null;

                // Multi-flow, need to create flows dict
            else if (namedFlows == null)
                namedFlows = new HashMap<>();

                // Multi-flow, already have a flows dict
            else
                namedFlows.clear();

            // Load up each flow (there may only be one)
            for (Entry<String, Object> namedFlowObj : flowsObjDict.entrySet()) {
                String name = namedFlowObj.getKey();
                HashMap<String, Object> flowObj = (HashMap<String, Object>) namedFlowObj.getValue();

                // Load up this flow using JSON data
                Flow flow = new Flow(name, story, flowObj);

                if (flowsObjDict.size() == 1) {
                    currentFlow = new Flow(name, story, flowObj);
                } else {
                    namedFlows.put(name, flow);
                }
            }

            if (namedFlows != null && namedFlows.size() > 1) {
                String currFlowName = (String) jObject.get("currentFlowName");
                currentFlow = namedFlows.get(currFlowName);
            }
        }

        // Old format: individually load up callstack, output stream, choices in
        // current/default flow
        else {
            namedFlows = null;
            currentFlow.name = kDefaultFlowName;
            currentFlow.callStack.setJsonToken((HashMap<String, Object>) jObject.get("callstackThreads"), story);
            currentFlow.outputStream = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("outputStream"));
            currentFlow.currentChoices = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("currentChoices"));

            Object jChoiceThreadsObj = jObject.get("choiceThreads");

            currentFlow.loadFlowChoiceThreads((HashMap<String, Object>) jChoiceThreadsObj, story);
        }

        outputStreamDirty();
        aliveFlowNamesDirty = true;

        variablesState.setJsonToken((HashMap<String, Object>) jObject.get("variablesState"));
        variablesState.setCallStack(currentFlow.callStack);

        evaluationStack = Json.jArrayToRuntimeObjList((List<Object>) jObject.get("evalStack"));

        Object currentDivertTargetPath = jObject.get("currentDivertTarget");
        if (currentDivertTargetPath != null) {
            Path divertPath = new Path(currentDivertTargetPath.toString());
            divertedPointer.assign(story.pointerAtPath(divertPath));
        }

        visitCounts = Json.jObjectToIntHashMap((HashMap<String, Object>) jObject.get("visitCounts"));
        turnIndices = Json.jObjectToIntHashMap((HashMap<String, Object>) jObject.get("turnIndices"));

        currentTurnIndex = (int) jObject.get("turnIdx");
        storySeed = (int) jObject.get("storySeed");

        // Not optional, but bug in inkjs means it's actually missing in inkjs saves
        Object previousRandomObj = jObject.get("previousRandom");
        if (previousRandomObj != null) {
            previousRandom = (int) previousRandomObj;
        } else {
            previousRandom = 0;
        }

    }
}
