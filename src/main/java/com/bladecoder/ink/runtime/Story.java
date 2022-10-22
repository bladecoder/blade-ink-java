package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.SimpleJson.InnerWriter;
import com.bladecoder.ink.runtime.SimpleJson.Writer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Stack;

/**
 * A Story is the core class that represents a complete Ink narrative, and
 * manages the evaluation and state of it.
 */
public class Story extends RTObject implements VariablesState.VariableChanged {
    /**
     * General purpose delegate definition for bound EXTERNAL function definitions
     * from ink. Note that this version isn't necessary if you have a function with
     * three arguments or less.
     *
     * @param <R> the result type
     * @see ExternalFunction0
     * @see ExternalFunction1
     * @see ExternalFunction2
     * @see ExternalFunction3
     */
    public interface ExternalFunction<R> {
        R call(Object... args) throws Exception;
    }

    /**
     * EXTERNAL function delegate with zero arguments.
     *
     * @param <R> the result type
     */
    public abstract static class ExternalFunction0<R> implements ExternalFunction<R> {
        @Override
        public final R call(Object... args) throws Exception {
            if (args.length != 0) {
                throw new IllegalArgumentException("Expecting 0 arguments.");
            }
            return call();
        }

        protected abstract R call() throws Exception;
    }

    /**
     * EXTERNAL function delegate with one argument.
     *
     * @param <T> the argument type
     * @param <R> the result type
     */
    public abstract static class ExternalFunction1<T, R> implements ExternalFunction<R> {
        @Override
        public final R call(Object... args) throws Exception {
            if (args.length != 1) {
                throw new IllegalArgumentException("Expecting 1 argument.");
            }
            return call(coerceArg(args[0]));
        }

        protected abstract R call(T t) throws Exception;

        @SuppressWarnings("unchecked")
        protected T coerceArg(Object arg) throws Exception {
            return (T) arg;
        }
    }

    /**
     * EXTERNAL function delegate with two arguments.
     *
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <R>  the result type
     */
    public abstract static class ExternalFunction2<T1, T2, R> implements ExternalFunction<R> {
        @Override
        public final R call(Object... args) throws Exception {
            if (args.length != 2) {
                throw new IllegalArgumentException("Expecting 2 arguments.");
            }
            return call(coerceArg0(args[0]), coerceArg1(args[1]));
        }

        protected abstract R call(T1 t1, T2 t2) throws Exception;

        @SuppressWarnings("unchecked")
        protected T1 coerceArg0(Object arg) throws Exception {
            return (T1) arg;
        }

        @SuppressWarnings("unchecked")
        protected T2 coerceArg1(Object arg) throws Exception {
            return (T2) arg;
        }
    }

    /**
     * EXTERNAL function delegate with three arguments.
     *
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <R>  the result type
     */
    public abstract static class ExternalFunction3<T1, T2, T3, R> implements ExternalFunction<R> {
        @Override
        public final R call(Object... args) throws Exception {
            if (args.length != 3) {
                throw new IllegalArgumentException("Expecting 3 arguments.");
            }
            return call(coerceArg0(args[0]), coerceArg1(args[1]), coerceArg2(args[2]));
        }

        protected abstract R call(T1 t1, T2 t2, T3 t3) throws Exception;

        @SuppressWarnings("unchecked")
        protected T1 coerceArg0(Object arg) throws Exception {
            return (T1) arg;
        }

        @SuppressWarnings("unchecked")
        protected T2 coerceArg1(Object arg) throws Exception {
            return (T2) arg;
        }

        @SuppressWarnings("unchecked")
        protected T3 coerceArg2(Object arg) throws Exception {
            return (T3) arg;
        }
    }

    class ExternalFunctionDef {
        public ExternalFunction<?> function;
        public boolean lookaheadSafe;
    }

    // Version numbers are for engine itself and story file, rather
    // than the story state save format
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
    public static final int inkVersionCurrent = 21;

    /**
     * The minimum legacy version of ink that can be loaded by the current version
     * of the code.
     */
    public static final int inkVersionMinimumCompatible = 18;

    private Container mainContentContainer;
    private ListDefinitionsOrigin listDefinitions;

    /**
     * An ink file can provide a fallback functions for when when an EXTERNAL has
     * been left unbound by the client, and the fallback function will be called
     * instead. Useful when testing a story in playmode, when it's not possible to
     * write a client-side C# external function, but you don't want it to fail to
     * run.
     */
    private boolean allowExternalFunctionFallbacks;

    private HashMap<String, ExternalFunctionDef> externals;

    private boolean hasValidatedExternals;

    private StoryState state;

    private Container temporaryEvaluationContainer;

    private HashMap<String, List<VariableObserver>> variableObservers;

    private List<Container> prevContainers = new ArrayList<>();

    private Profiler profiler;

    private boolean asyncContinueActive;
    private StoryState stateSnapshotAtLastNewline = null;

    private int recursiveContinueCount = 0;

    private boolean asyncSaving;

    private boolean sawLookaheadUnsafeFunctionAfterNewline = false;

    public Error.ErrorHandler onError = null;

    // Warning: When creating a Story using this constructor, you need to
    // call ResetState on it before use. Intended for compiler use only.
    // For normal use, use the constructor that takes a json string.
    public Story(Container contentContainer, List<ListDefinition> lists) {
        mainContentContainer = contentContainer;

        if (lists != null) {
            listDefinitions = new ListDefinitionsOrigin(lists);
        }

        externals = new HashMap<>();
    }

    public Story(Container contentContainer) {
        this(contentContainer, null);
    }

    /**
     * Construct a Story Object using a JSON String compiled through inklecate.
     */
    public Story(String jsonString) throws Exception {
        this((Container) null);
        HashMap<String, Object> rootObject = SimpleJson.textToDictionary(jsonString);

        Object versionObj = rootObject.get("inkVersion");
        if (versionObj == null)
            throw new Exception("ink version number not found. Are you sure it's a valid .ink.json file?");

        int formatFromFile = versionObj instanceof String ? Integer.parseInt((String) versionObj) : (int) versionObj;

        if (formatFromFile > inkVersionCurrent) {
            throw new Exception("Version of ink used to build story was newer than the current version of the engine");
        } else if (formatFromFile < inkVersionMinimumCompatible) {
            throw new Exception(
                    "Version of ink used to build story is too old to be loaded by this version of the engine");
        } else if (formatFromFile != inkVersionCurrent) {
            System.out.println(
                    "WARNING: Version of ink used to build story doesn't match current version of engine. " +
                            "Non-critical, but recommend synchronising.");
        }

        Object rootToken = rootObject.get("root");
        if (rootToken == null)
            throw new Exception("Root node for ink not found. Are you sure it's a valid .ink.json file?");

        Object listDefsObj = rootObject.get("listDefs");
        if (listDefsObj != null) {
            listDefinitions = Json.jTokenToListDefinitions(listDefsObj);
        }

        RTObject runtimeObject = Json.jTokenToRuntimeObject(rootToken);
        mainContentContainer = runtimeObject instanceof Container ? (Container) runtimeObject : null;

        resetState();
    }

    void addError(String message) throws Exception {
        addError(message, false, false);
    }

    void warning(String message) throws Exception {
        addError(message, true, false);
    }

    void addError(String message, boolean isWarning, boolean useEndLineNumber) throws Exception {
        DebugMetadata dm = currentDebugMetadata();

        String errorTypeStr = isWarning ? "WARNING" : "ERROR";

        if (dm != null) {
            int lineNum = useEndLineNumber ? dm.endLineNumber : dm.startLineNumber;
            message = String.format("RUNTIME %s: '%s' line %d: %s", errorTypeStr, dm.fileName, lineNum, message);
        } else if (!state.getCurrentPointer().isNull()) {
            message = String.format("RUNTIME %s: (%s): %s", errorTypeStr,
                    state.getCurrentPointer().getPath().toString(), message);
        } else {
            message = "RUNTIME " + errorTypeStr + ": " + message;
        }

        state.addError(message, isWarning);

        // In a broken state don't need to know about any other errors.
        if (!isWarning)
            state.forceEnd();
    }

    /**
     * Start recording ink profiling information during calls to Continue on Story.
     * Return a Profiler instance that you can request a report from when you're
     * finished.
     *
     * @throws Exception
     */
    public Profiler startProfiling() throws Exception {
        ifAsyncWeCant("start profiling");
        profiler = new Profiler();

        return profiler;
    }

    /**
     * Stop recording ink profiling information during calls to Continue on Story.
     * To generate a report from the profiler, call
     */
    public void endProfiling() {
        profiler = null;
    }

    void Assert(boolean condition, Object... formatParams) throws Exception {
        Assert(condition, null, formatParams);
    }

    void Assert(boolean condition, String message, Object... formatParams) throws Exception {
        if (!condition) {
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
     * Binds a Java function to an ink EXTERNAL function.
     *
     * @param funcName      EXTERNAL ink function name to bind to.
     * @param func          The Java function to bind.
     * @param lookaheadSafe The ink engine often evaluates further than you might
     *                      expect beyond the current line just in case it sees glue
     *                      that will cause the two lines to become one. In this
     *                      case it's possible that a function can appear to be
     *                      called twice instead of just once, and earlier than you
     *                      expect. If it's safe for your function to be called in
     *                      this way (since the result and side effect of the
     *                      function will not change), then you can pass 'true'.
     *                      Usually, you want to pass 'false', especially if you
     *                      want some action to be performed in game code when this
     *                      function is called.
     */
    public void bindExternalFunction(String funcName, ExternalFunction<?> func, boolean lookaheadSafe)
            throws Exception {
        ifAsyncWeCant("bind an external function");
        Assert(!externals.containsKey(funcName), "Function '" + funcName + "' has already been bound.");
        ExternalFunctionDef externalFunctionDef = new ExternalFunctionDef();
        externalFunctionDef.function = func;
        externalFunctionDef.lookaheadSafe = lookaheadSafe;

        externals.put(funcName, externalFunctionDef);
    }

    public void bindExternalFunction(String funcName, ExternalFunction<?> func) throws Exception {
        bindExternalFunction(funcName, func, true);
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
            return (T) (intVal == 0 ? Boolean.FALSE : Boolean.TRUE);
        }

        if (value instanceof Boolean && type == Integer.class) {
            boolean val = (Boolean) value;
            return (T) (val ? (Integer) 1 : (Integer) 0);
        }

        if (type == String.class) {
            return (T) value.toString();
        }

        Assert(false, "Failed to cast " + value.getClass().getCanonicalName() + " to " + type.getCanonicalName());

        return null;
    }

    /**
     * Get any global tags associated with the story. These are defined as hash tags
     * defined at the very top of the story.
     *
     * @throws Exception
     */
    public List<String> getGlobalTags() throws Exception {
        return tagsAtStartOfFlowContainerWithPathString("");
    }

    /**
     * Gets any tags associated with a particular knot or knot.stitch. These are
     * defined as hash tags defined at the very top of a knot or stitch.
     *
     * @param path The path of the knot or stitch, in the form "knot" or
     *             "knot.stitch".
     * @throws Exception
     */
    public List<String> tagsForContentAtPath(String path) throws Exception {
        return tagsAtStartOfFlowContainerWithPathString(path);
    }

    List<String> tagsAtStartOfFlowContainerWithPathString(String pathString) throws Exception {
        Path path = new Path(pathString);

        // Expected to be global story, knot or stitch
        Container flowContainer = contentAtPath(path).getContainer();

        while (true) {
            RTObject firstContent = flowContainer.getContent().get(0);
            if (firstContent instanceof Container)
                flowContainer = (Container) firstContent;
            else
                break;
        }

        // Any initial tag objects count as the "main tags" associated with that
        // story/knot/stitch
        boolean inTag = false;
        List<String> tags = null;
        for (RTObject c : flowContainer.getContent()) {

            if (c instanceof ControlCommand) {
                ControlCommand command = (ControlCommand) c;

                if (command.getCommandType() == ControlCommand.CommandType.BeginTag) {
                    inTag = true;
                } else if (command.getCommandType() == ControlCommand.CommandType.EndTag) {
                    inTag = false;
                }
            } else if (inTag) {
                if (c instanceof StringValue) {
                    StringValue str = (StringValue) c;
                    if (tags == null) tags = new ArrayList<>();
                    tags.add(str.value);
                } else {
                    error("Tag contained non-text content. Only plain text is allowed when using globalTags or " +
                            "TagsAtContentPath. If you want to evaluate dynamic content, you need to use story" +
                            ".Continue().");
                }
            }

            // Any other content - we're done
            // We only recognise initial text-only tags
            else {
                break;
            }
        }

        return tags;
    }

    /**
     * Useful when debugging a (very short) story, to visualise the state of the
     * story. Add this call as a watch and open the extended text. A left-arrow mark
     * will denote the current point of the story. It's only recommended that this
     * is used on very short debug stories, since it can end up generate a large
     * quantity of text otherwise.
     */
    public String buildStringOfHierarchy() {
        StringBuilder sb = new StringBuilder();

        getMainContentContainer().buildStringOfHierarchy(sb, 0, state.getCurrentPointer().resolve());

        return sb.toString();
    }

    public ExternalFunction<?> getExternalFunction(String functionName) {
        ExternalFunctionDef externalFunctionDef = externals.get(functionName);

        if (externalFunctionDef != null) {
            return externalFunctionDef.function;
        }

        return null;
    }

    void callExternalFunction(String funcName, int numberOfArguments) throws Exception {
        ExternalFunctionDef funcDef;
        Container fallbackFunctionContainer = null;

        funcDef = externals.get(funcName);

        // Should this function break glue? Abort run if we've already seen a newline.
        // Set a bool to tell it to restore the snapshot at the end of this instruction.
        if (funcDef != null && !funcDef.lookaheadSafe && stateSnapshotAtLastNewline != null) {
            sawLookaheadUnsafeFunctionAfterNewline = true;
            return;
        }

        // Try to use fallback function?
        if (funcDef == null) {
            if (allowExternalFunctionFallbacks) {

                fallbackFunctionContainer = knotContainerWithName(funcName);

                Assert(fallbackFunctionContainer != null, "Trying to call EXTERNAL function '" + funcName
                        + "' which has not been bound, and fallback ink function could not be found.");

                // Divert direct into fallback function and we're done
                state.getCallStack().push(PushPopType.Function, 0, state.getOutputStream().size());
                state.setDivertedPointer(Pointer.startOf(fallbackFunctionContainer));
                return;

            } else {
                Assert(false, "Trying to call EXTERNAL function '" + funcName
                        + "' which has not been bound (and ink fallbacks disabled).");
            }
        }

        // Pop arguments
        ArrayList<Object> arguments = new ArrayList<>();
        for (int i = 0; i < numberOfArguments; ++i) {
            Value<?> poppedObj = (Value<?>) state.popEvaluationStack();
            Object valueObj = poppedObj.getValueObject();
            arguments.add(valueObj);
        }

        // Reverse arguments from the order they were popped,
        // so they're the right way round again.
        Collections.reverse(arguments);

        // Run the function!
        Object funcResult = funcDef.function.call(arguments.toArray());

        // Convert return value (if any) to the a type that the ink engine can use
        RTObject returnObj;
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
     * Check whether more content is available if you were to call Continue() - i.e.
     * are we mid story rather than at a choice point or at the end.
     *
     * @return true if it's possible to call Continue()
     */
    public boolean canContinue() {
        return state.canContinue();
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

        choosePath(choiceToChoose.targetPath);
    }

    void choosePath(Path p) throws Exception {
        choosePath(p, true);
    }

    void choosePath(Path p, boolean incrementingTurnIndex) throws Exception {
        state.setChosenPath(p, incrementingTurnIndex);

        // Take a note of newly visited containers for read counts etc
        visitChangedContainersDueToDivert();
    }

    /**
     * Change the current position of the story to the given path. From here you can
     * call Continue() to evaluate the next line.
     * <p>
     * The path String is a dot-separated path as used ly by the engine. These
     * examples should work:
     * <p>
     * myKnot myKnot.myStitch
     * <p>
     * Note however that this won't necessarily work:
     * <p>
     * myKnot.myStitch.myLabelledChoice
     * <p>
     * ...because of the way that content is nested within a weave structure.
     * <p>
     * By default this will reset the callstack beforehand, which means that any
     * tunnels, threads or functions you were in at the time of calling will be
     * discarded. This is different from the behaviour of ChooseChoiceIndex, which
     * will always keep the callstack, since the choices are known to come from the
     * correct state, and known their source thread.
     * <p>
     * You have the option of passing false to the resetCallstack parameter if you
     * don't want this behaviour, and will leave any active threads, tunnels or
     * function calls in-tact.
     * <p>
     * This is potentially dangerous! If you're in the middle of a tunnel, it'll
     * redirect only the inner-most tunnel, meaning that when you tunnel-return
     * using '-&gt;-&gt;-&gt;', it'll return to where you were before. This may be
     * what you want though. However, if you're in the middle of a function,
     * ChoosePathString will throw an exception.
     *
     * @param path           A dot-separted path string, as specified above.
     * @param resetCallstack Whether to reset the callstack first (see summary
     *                       description).
     * @param arguments      Optional set of arguments to pass, if path is to a knot
     *                       that takes them.
     */
    public void choosePathString(String path, boolean resetCallstack, Object[] arguments) throws Exception {
        ifAsyncWeCant("call ChoosePathString right now");

        if (resetCallstack) {
            resetCallstack();
        } else {
            // ChoosePathString is potentially dangerous since you can call it when the
            // stack is
            // pretty much in any state. Let's catch one of the worst offenders.
            if (state.getCallStack().getCurrentElement().type == PushPopType.Function) {
                String funcDetail = "";
                Container container = state.getCallStack().getCurrentElement().currentPointer.container;
                if (container != null) {
                    funcDetail = "(" + container.getPath().toString() + ") ";
                }
                throw new Exception("Story was running a function " + funcDetail + "when you called ChoosePathString("
                        + path + ") - this is almost certainly not not what you want! Full stack trace: \n"
                        + state.getCallStack().getCallStackTrace());
            }
        }

        state.passArgumentsToEvaluationStack(arguments);
        choosePath(new Path(path));
    }

    public void choosePathString(String path) throws Exception {
        choosePathString(path, true, null);
    }

    public void choosePathString(String path, boolean resetCallstack) throws Exception {
        choosePathString(path, resetCallstack, null);
    }

    void ifAsyncWeCant(String activityStr) throws Exception {
        if (asyncContinueActive)
            throw new Exception("Can't " + activityStr
                    + ". Story is in the middle of a ContinueAsync(). Make more ContinueAsync() calls or a single " +
                    "Continue() call beforehand.");
    }

    SearchResult contentAtPath(Path path) throws Exception {
        return getMainContentContainer().contentAtPath(path);
    }

    Container knotContainerWithName(String name) {

        INamedContent namedContainer = mainContentContainer.getNamedContent().get(name);

        if (namedContainer != null)
            return namedContainer instanceof Container ? (Container) namedContainer : null;
        else
            return null;
    }

    /**
     * The current flow name if using multi-flow functionality - see SwitchFlow
     */
    public String getCurrentFlowName() {
        return state.getCurrentFlowName();
    }

    /**
     * Is the default flow currently active? By definition, will also return true if not using multi-flow
     * functionality - see SwitchFlow
     */
    public boolean currentFlowIsDefaultFlow() {
        return state.currentFlowIsDefaultFlow();
    }

    /**
     * Names of currently alive flows (not including the default flow)
     */
    public List<String> aliveFlowNames() {
        return state.aliveFlowNames();
    }

    public void switchFlow(String flowName) throws Exception {
        ifAsyncWeCant("switch flow");

        if (asyncSaving)
            throw new Exception("Story is already in background saving mode, can't switch flow to " + flowName);

        state.switchFlowInternal(flowName);
    }

    public void removeFlow(String flowName) throws Exception {
        state.removeFlowInternal(flowName);
    }

    public void switchToDefaultFlow() throws Exception {
        state.switchToDefaultFlowInternal();
    }

    /**
     * Continue the story for one line of content, if possible. If you're not sure
     * if there's more content available, for example if you want to check whether
     * you're at a choice point or at the end of the story, you should call
     * canContinue before calling this function.
     *
     * @return The line of text content.
     */
    public String Continue() throws StoryException, Exception {
        continueAsync(0);
        return getCurrentText();
    }

    /**
     * If ContinueAsync was called (with milliseconds limit &gt; 0) then this
     * property will return false if the ink evaluation isn't yet finished, and you
     * need to call it again in order for the Continue to fully complete.
     */
    public boolean asyncContinueComplete() {
        return !asyncContinueActive;
    }

    /**
     * An "asnychronous" version of Continue that only partially evaluates the ink,
     * with a budget of a certain time limit. It will exit ink evaluation early if
     * the evaluation isn't complete within the time limit, with the
     * asyncContinueComplete property being false. This is useful if ink evaluation
     * takes a long time, and you want to distribute it over multiple game frames
     * for smoother animation. If you pass a limit of zero, then it will fully
     * evaluate the ink in the same way as calling Continue (and in fact, this
     * exactly what Continue does internally).
     */
    public void continueAsync(float millisecsLimitAsync) throws Exception {
        if (!hasValidatedExternals)
            validateExternalBindings();

        continueInternal(millisecsLimitAsync);
    }

    void continueInternal() throws Exception {
        continueInternal(0);
    }

    void continueInternal(float millisecsLimitAsync) throws Exception {
        if (profiler != null)
            profiler.preContinue();

        boolean isAsyncTimeLimited = millisecsLimitAsync > 0;

        recursiveContinueCount++;

        // Doing either:
        // - full run through non-async (so not active and don't want to be)
        // - Starting async run-through
        if (!asyncContinueActive) {
            asyncContinueActive = isAsyncTimeLimited;
            if (!canContinue()) {
                throw new Exception("Can't continue - should check canContinue before calling Continue");
            }

            state.setDidSafeExit(false);

            state.resetOutput();

            // It's possible for ink to call game to call ink to call game etc
            // In this case, we only want to batch observe variable changes
            // for the outermost call.
            if (recursiveContinueCount == 1)
                state.getVariablesState().setbatchObservingVariableChanges(true);
        }

        // Start timing
        Stopwatch durationStopwatch = new Stopwatch();
        durationStopwatch.start();

        boolean outputStreamEndsInNewline = false;
        sawLookaheadUnsafeFunctionAfterNewline = false;
        do {

            try {
                outputStreamEndsInNewline = continueSingleStep();
            } catch (StoryException e) {
                addError(e.getMessage(), false, e.useEndLineNumber);
                break;
            }

            if (outputStreamEndsInNewline)
                break;

            // Run out of async time?
            if (asyncContinueActive && durationStopwatch.getElapsedMilliseconds() > millisecsLimitAsync) {
                break;
            }

        } while (canContinue());

        durationStopwatch.stop();

        // 4 outcomes:
        // - got newline (so finished this line of text)
        // - can't continue (e.g. choices or ending)
        // - ran out of time during evaluation
        // - error
        //
        // Successfully finished evaluation in time (or in error)
        if (outputStreamEndsInNewline || !canContinue()) {
            // Need to rewind, due to evaluating further than we should?
            if (stateSnapshotAtLastNewline != null) {
                restoreStateSnapshot();
            }

            // Finished a section of content / reached a choice point?
            if (!canContinue()) {
                if (state.getCallStack().canPopThread())
                    addError("Thread available to pop, threads should always be flat by the end of evaluation?");

                if (state.getGeneratedChoices().size() == 0 && !state.isDidSafeExit()
                        && temporaryEvaluationContainer == null) {
                    if (state.getCallStack().canPop(PushPopType.Tunnel))
                        addError("unexpectedly reached end of content. Do you need a '->->' to return from a tunnel?");
                    else if (state.getCallStack().canPop(PushPopType.Function))
                        addError("unexpectedly reached end of content. Do you need a '~ return'?");
                    else if (!state.getCallStack().canPop())
                        addError("ran out of content. Do you need a '-> DONE' or '-> END'?");
                    else
                        addError("unexpectedly reached end of content for unknown reason. Please debug compiler!");
                }
            }
            state.setDidSafeExit(false);
            sawLookaheadUnsafeFunctionAfterNewline = false;

            if (recursiveContinueCount == 1)
                state.getVariablesState().setbatchObservingVariableChanges(false);
            asyncContinueActive = false;
        }

        recursiveContinueCount--;

        if (profiler != null)
            profiler.postContinue();

        // Report any errors that occured during evaluation.
        // This may either have been StoryExceptions that were thrown
        // and caught during evaluation, or directly added with AddError.
        if (state.hasError() || state.hasWarning()) {
            if (onError != null) {
                if (state.hasError()) {
                    for (String err : state.getCurrentErrors()) {
                        onError.error(err, ErrorType.Error);
                    }
                }
                if (state.hasWarning()) {
                    for (String err : state.getCurrentWarnings()) {
                        onError.error(err, ErrorType.Warning);
                    }
                }

                resetErrors();
            }
            // Throw an exception since there's no error handler
            else {
                StringBuilder sb = new StringBuilder();
                sb.append("Ink had ");
                if (state.hasError()) {
                    sb.append(state.getCurrentErrors().size());
                    sb.append(state.getCurrentErrors().size() == 1 ? " error" : " errors");
                    if (state.hasWarning())
                        sb.append(" and ");
                }
                if (state.hasWarning()) {
                    sb.append(state.getCurrentWarnings().size());
                    sb.append(state.getCurrentWarnings().size() == 1 ? " warning" : " warnings");
                }
                sb.append(
                        ". It is strongly suggested that you assign an error handler to story.onError. The first " +
                                "issue was: ");
                sb.append(state.hasError() ? state.getCurrentErrors().get(0) : state.getCurrentWarnings().get(0));

                // If you get this exception, please assign an error handler to your story.
                // If you're using Unity, you can do something like this when you create
                // your story:
                //
                // var story = new Ink.Runtime.Story(jsonTxt);
                // story.onError = (errorMessage, errorType) => {
                // if( errorType == ErrorType.Warning )
                // Debug.LogWarning(errorMessage);
                // else
                // Debug.LogError(errorMessage);
                // };
                //
                //
                throw new StoryException(sb.toString());
            }
        }
    }

    boolean continueSingleStep() throws Exception {
        if (profiler != null)
            profiler.preStep();

        // Run main step function (walks through content)
        step();

        if (profiler != null)
            profiler.postStep();

        // Run out of content and we have a default invisible choice that we can follow?
        if (!canContinue() && !state.getCallStack().elementIsEvaluateFromGame()) {

            tryFollowDefaultInvisibleChoice();
        }

        if (profiler != null)
            profiler.preSnapshot();

        // Don't save/rewind during string evaluation, which is e.g. used for choices
        if (!state.inStringEvaluation()) {

            // We previously found a newline, but were we just double checking that
            // it wouldn't immediately be removed by glue?
            if (stateSnapshotAtLastNewline != null) {

                // Has proper text or a tag been added? Then we know that the newline
                // that was previously added is definitely the end of the line.
                OutputStateChange change = calculateNewlineOutputStateChange(
                        stateSnapshotAtLastNewline.getCurrentText(), state.getCurrentText(),
                        stateSnapshotAtLastNewline.getCurrentTags().size(), state.getCurrentTags().size());

                // The last time we saw a newline, it was definitely the end of the line, so we
                // want to rewind to that point.
                if (change == OutputStateChange.ExtendedBeyondNewline || sawLookaheadUnsafeFunctionAfterNewline) {
                    restoreStateSnapshot();

                    // Hit a newline for sure, we're done
                    return true;
                }

                // Newline that previously existed is no longer valid - e.g.
                // glue was encounted that caused it to be removed.
                else if (change == OutputStateChange.NewlineRemoved) {
                    stateSnapshotAtLastNewline = null;
                    discardSnapshot();
                }

            }

            // Current content ends in a newline - approaching end of our evaluation
            if (state.outputStreamEndsInNewline()) {

                // If we can continue evaluation for a bit:
                // Create a snapshot in case we need to rewind.
                // We're going to continue stepping in case we see glue or some
                // non-text content such as choices.
                if (canContinue()) {

                    // Don't bother to record the state beyond the current newline.
                    // e.g.:
                    // Hello world\n // record state at the end of here
                    // ~ complexCalculation() // don't actually need this unless it generates text
                    if (stateSnapshotAtLastNewline == null)
                        stateSnapshot();
                }

                // Can't continue, so we're about to exit - make sure we
                // don't have an old state hanging around.
                else {
                    discardSnapshot();
                }

            }

        }

        if (profiler != null)
            profiler.postSnapshot();

        // outputStreamEndsInNewline = false
        return false;

    }

    /**
     * Continue the story until the next choice point or until it runs out of
     * content. This is as opposed to the Continue() method which only evaluates one
     * line of output at a time.
     *
     * @return The resulting text evaluated by the ink engine, concatenated
     * together.
     */
    public String continueMaximally() throws StoryException, Exception {
        ifAsyncWeCant("ContinueMaximally");

        StringBuilder sb = new StringBuilder();

        while (canContinue()) {
            sb.append(Continue());
        }

        return sb.toString();
    }

    DebugMetadata currentDebugMetadata() {
        DebugMetadata dm;

        // Try to get from the current path first
        final Pointer pointer = new Pointer(state.getCurrentPointer());
        if (!pointer.isNull()) {
            dm = pointer.resolve().getDebugMetadata();
            if (dm != null) {
                return dm;
            }
        }

        // Move up callstack if possible
        for (int i = state.getCallStack().getElements().size() - 1; i >= 0; --i) {
            pointer.assign(state.getCallStack().getElements().get(i).currentPointer);
            if (!pointer.isNull() && pointer.resolve() != null) {
                dm = pointer.resolve().getDebugMetadata();
                if (dm != null) {
                    return dm;
                }
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
            state.popCallstack();
        }

        int endStackHeight = state.getEvaluationStack().size();
        if (endStackHeight > evalStackHeight) {
            return state.popEvaluationStack();
        } else {
            return null;
        }

    }

    /**
     * The list of Choice Objects available at the current point in the Story. This
     * list will be populated as the Story is stepped through with the Continue()
     * method. Once canContinue becomes false, this list will be populated, and is
     * usually (but not always) on the final Continue() step.
     */
    public List<Choice> getCurrentChoices() {

        // Don't include invisible choices for external usage.
        List<Choice> choices = new ArrayList<>();
        for (Choice c : state.getCurrentChoices()) {
            if (!c.isInvisibleDefault) {
                c.setIndex(choices.size());
                choices.add(c);
            }
        }

        return choices;
    }

    /**
     * Gets a list of tags as defined with '#' in source that were seen during the
     * latest Continue() call.
     *
     * @throws Exception
     */
    public List<String> getCurrentTags() throws Exception {
        ifAsyncWeCant("call currentTags since it's a work in progress");
        return state.getCurrentTags();
    }

    /**
     * Any warnings generated during evaluation of the Story.
     */
    public List<String> getCurrentWarnings() {
        return state.getCurrentWarnings();
    }

    /**
     * Any errors generated during evaluation of the Story.
     */
    public List<String> getCurrentErrors() {
        return state.getCurrentErrors();
    }

    /**
     * The latest line of text to be generated from a Continue() call.
     *
     * @throws Exception
     */
    public String getCurrentText() throws Exception {
        ifAsyncWeCant("call currentText since it's a work in progress");
        return state.getCurrentText();
    }

    /**
     * The entire current state of the story including (but not limited to):
     * <p>
     * * Global variables * Temporary variables * Read/visit and turn counts * The
     * callstack and evaluation stacks * The current threads
     */
    public StoryState getState() {
        return state;
    }

    /**
     * The VariablesState Object contains all the global variables in the story.
     * However, note that there's more to the state of a Story than just the global
     * variables. This is a convenience accessor to the full state Object.
     */
    public VariablesState getVariablesState() {
        return state.getVariablesState();
    }

    public ListDefinitionsOrigin getListDefinitions() {
        return listDefinitions;
    }

    /**
     * Whether the currentErrors list contains any errors. THIS MAY BE REMOVED - you
     * should be setting an error handler directly using Story.onError.
     */
    public boolean hasError() {
        return state.hasError();
    }

    /**
     * Whether the currentWarnings list contains any warnings.
     */
    public boolean hasWarning() {
        return state.hasWarning();
    }

    boolean incrementContentPointer() {
        boolean successfulIncrement = true;

        Pointer pointer = new Pointer(state.getCallStack().getCurrentElement().currentPointer);
        pointer.index++;

        // Each time we step off the end, we fall out to the next container, all
        // the
        // while we're in indexed rather than named content
        while (pointer.index >= pointer.container.getContent().size()) {

            successfulIncrement = false;

            Container nextAncestor = pointer.container.getParent() instanceof Container
                    ? (Container) pointer.container.getParent()
                    : null;

            if (nextAncestor == null) {
                break;
            }

            int indexInAncestor = nextAncestor.getContent().indexOf(pointer.container);
            if (indexInAncestor == -1) {
                break;
            }

            pointer = new Pointer(nextAncestor, indexInAncestor);

            // Increment to next content in outer container
            pointer.index++;

            successfulIncrement = true;
        }

        if (!successfulIncrement)
            pointer.assign(Pointer.Null);

        state.getCallStack().getCurrentElement().currentPointer.assign(pointer);

        return successfulIncrement;
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
                        + ") as a conditional value. Did you intend a function call 'likeThis()' or a read count " +
                        "check 'likeThis'? (no arrows)");
                return false;
            }

            return val.isTruthy();
        }
        return truthy;
    }

    /**
     * When the named global variable changes it's value, the observer will be
     * called to notify it of the change. Note that if the value changes multiple
     * times within the ink, the observer will only be called once, at the end of
     * the ink's evaluation. If, during the evaluation, it changes and then changes
     * back again to its original value, it will still be called. Note that the
     * observer will also be fired if the value of the variable is changed
     * externally to the ink, by directly setting a value in story.variablesState.
     *
     * @param variableName The name of the global variable to observe.
     * @param observer     A delegate function to call when the variable changes.
     * @throws Exception
     */
    public void observeVariable(String variableName, VariableObserver observer) throws Exception {
        ifAsyncWeCant("observe a new variable");

        if (variableObservers == null)
            variableObservers = new HashMap<>();

        if (!state.getVariablesState().globalVariableExistsWithName(variableName))
            throw new Exception(
                    "Cannot observe variable '" + variableName + "' because it wasn't declared in the ink story.");

        if (variableObservers.containsKey(variableName)) {
            variableObservers.get(variableName).add(observer);
        } else {
            List<VariableObserver> l = new ArrayList<>();
            l.add(observer);
            variableObservers.put(variableName, l);
        }
    }

    /**
     * Convenience function to allow multiple variables to be observed with the same
     * observer delegate function. See the singular ObserveVariable for details. The
     * observer will get one call for every variable that has changed.
     *
     * @param variableNames The set of variables to observe.
     * @param observer      The delegate function to call when any of the named
     *                      variables change.
     * @throws Exception
     * @throws StoryException
     */
    public void observeVariables(List<String> variableNames, VariableObserver observer)
            throws StoryException, Exception {
        for (String varName : variableNames) {
            observeVariable(varName, observer);
        }
    }

    /**
     * Removes the variable observer, to stop getting variable change notifications.
     * If you pass a specific variable name, it will stop observing that particular
     * one. If you pass null (or leave it blank, since it's optional), then the
     * observer will be removed from all variables that it's subscribed to.
     *
     * @param observer             The observer to stop observing.
     * @param specificVariableName (Optional) Specific variable name to stop
     *                             observing.
     * @throws Exception
     */
    public void removeVariableObserver(VariableObserver observer, String specificVariableName) throws Exception {
        ifAsyncWeCant("remove a variable observer");

        if (variableObservers == null)
            return;

        // Remove observer for this specific variable
        if (specificVariableName != null) {
            if (variableObservers.containsKey(specificVariableName)) {
                variableObservers.get(specificVariableName).remove(observer);
                if (variableObservers.get(specificVariableName).size() == 0) {
                    variableObservers.remove(specificVariableName);
                }
            }
        } else {
            // Remove observer for all variables
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, List<VariableObserver>> obs : variableObservers.entrySet()) {
                obs.getValue().remove(observer);
                if (obs.getValue().size() == 0) {
                    keysToRemove.add(obs.getKey());
                }
            }

            for (String keyToRemove : keysToRemove) {
                variableObservers.remove(keyToRemove);
            }
        }
    }

    public void removeVariableObserver(VariableObserver observer) throws Exception {
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

    public Container getMainContentContainer() {
        if (temporaryEvaluationContainer != null) {
            return temporaryEvaluationContainer;
        } else {
            return mainContentContainer;
        }
    }

    String buildStringOfContainer(Container container) {
        StringBuilder sb = new StringBuilder();

        container.buildStringOfHierarchy(sb, 0, state.getCurrentPointer().resolve());

        return sb.toString();
    }

    private void nextContent() throws Exception {
        // Setting previousContentObject is critical for
        // VisitChangedContainersDueToDivert
        state.setPreviousPointer(state.getCurrentPointer());

        // Divert step?
        if (!state.getDivertedPointer().isNull()) {

            state.setCurrentPointer(state.getDivertedPointer());
            state.setDivertedPointer(Pointer.Null);

            // Internally uses state.previousContentObject and
            // state.currentContentObject
            visitChangedContainersDueToDivert();

            // Diverted location has valid content?
            if (!state.getCurrentPointer().isNull()) {
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
                state.popCallstack(PushPopType.Function);

                // This pop was due to dropping off the end of a function that
                // didn't return anything,
                // so in this case, we make sure that the evaluator has
                // something to chomp on if it needs it
                if (state.getInExpressionEvaluation()) {
                    state.pushEvaluationStack(new Void());
                }

                didPop = true;
            } else if (state.getCallStack().canPopThread()) {
                state.getCallStack().popThread();

                didPop = true;
            } else {
                state.tryExitFunctionEvaluationFromGame();
            }

            // Step past the point where we last called out
            if (didPop && !state.getCurrentPointer().isNull()) {
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

        Container seqContainer = state.getCurrentPointer().container;

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

        ArrayList<Integer> unpickedIndices = new ArrayList<>();
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
     * Checks whether contentObj is a control or flow Object rather than a piece of
     * content, and performs the required command if necessary.
     *
     * @param contentObj Content Object.
     * @return true if Object was logic or flow control, false if it's normal
     * content.
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

                if (varContents == null) {
                    error("Tried to divert using a target from a variable that could not be found (" + varName + ")");
                } else if (!(varContents instanceof DivertTargetValue)) {

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
                state.setDivertedPointer(pointerAtPath(target.getTargetPath()));

            } else if (currentDivert.isExternal()) {
                callExternalFunction(currentDivert.getTargetPathString(), currentDivert.getExternalArgs());
                return true;
            } else {
                state.setDivertedPointer(currentDivert.getTargetPointer());
            }

            if (currentDivert.getPushesToStack()) {
                state.getCallStack().push(currentDivert.getStackPushType(), 0, state.getOutputStream().size());
            }

            if (state.getDivertedPointer().isNull() && !currentDivert.isExternal()) {

                // Human readable name available - runtime divert is part of a
                // hard-written divert that to missing content
                if (currentDivert.getDebugMetadata().sourceName != null) {
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
            switch (evalCommand.getCommandType()) {

                case EvalStart:
                    Assert(!state.getInExpressionEvaluation(), "Already in expression evaluation?");
                    state.setInExpressionEvaluation(true);
                    break;

                case EvalEnd:
                    Assert(state.getInExpressionEvaluation(), "Not in expression evaluation mode");
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

                    PushPopType popType = evalCommand.getCommandType() == ControlCommand.CommandType.PopFunction
                            ? PushPopType.Function
                            : PushPopType.Tunnel;

                    // Tunnel onwards is allowed to specify an optional override
                    // divert to go to immediately after returning: ->-> target
                    DivertTargetValue overrideTunnelReturnTarget = null;
                    if (popType == PushPopType.Tunnel) {
                        RTObject popped = state.popEvaluationStack();

                        if (popped instanceof DivertTargetValue) {
                            overrideTunnelReturnTarget = (DivertTargetValue) popped;
                        }

                        if (overrideTunnelReturnTarget == null) {
                            Assert(popped instanceof Void, "Expected void if ->-> doesn't override target");
                        }
                    }

                    if (state.tryExitFunctionEvaluationFromGame()) {
                        break;
                    } else if (state.getCallStack().getCurrentElement().type != popType || !state.getCallStack()
                            .canPop()) {

                        HashMap<PushPopType, String> names = new HashMap<>();
                        names.put(PushPopType.Function, "function return statement (~ return)");
                        names.put(PushPopType.Tunnel, "tunnel onwards statement (->->)");

                        String expected = names.get(state.getCallStack().getCurrentElement().type);
                        if (!state.getCallStack().canPop()) {
                            expected = "end of flow (-> END or choice)";
                        }

                        String errorMsg = String.format("Found %s, when expected %s", names.get(popType), expected);

                        error(errorMsg);
                    } else {
                        state.popCallstack();

                        // Does tunnel onwards override by diverting to a new ->->
                        // target?
                        if (overrideTunnelReturnTarget != null)
                            state.setDivertedPointer(pointerAtPath(overrideTunnelReturnTarget.getTargetPath()));
                    }
                    break;

                case BeginString:
                    state.pushToOutputStream(evalCommand);

                    Assert(state.getInExpressionEvaluation(),
                            "Expected to be in an expression when evaluating a string");
                    state.setInExpressionEvaluation(false);
                    break;
                // Leave it to story.currentText and story.currentTags to sort out the text from the tags
                // This is mostly because we can't always rely on the existence of EndTag, and we don't want
                // to try and flatten dynamic tags to strings every time \n is pushed to output
                case BeginTag:
                    state.pushToOutputStream(evalCommand);
                    break;
                case EndTag: {

                    // EndTag has 2 modes:
                    //  - When in string evaluation (for choices)
                    //  - Normal
                    //
                    // The only way you could have an EndTag in the middle of
                    // string evaluation is if we're currently generating text for a
                    // choice, such as:
                    //
                    //   + choice # tag
                    //
                    // In the above case, the ink will be run twice:
                    //  - First, to generate the choice text. String evaluation
                    //    will be on, and the final string will be pushed to the
                    //    evaluation stack, ready to be popped to make a Choice
                    //    object.
                    //  - Second, when ink generates text after choosing the choice.
                    //    On this ocassion, it's not in string evaluation mode.
                    //
                    // On the writing side, we disallow manually putting tags within
                    // strings like this:
                    //
                    //   {"hello # world"}
                    //
                    // So we know that the tag must be being generated as part of
                    // choice content. Therefore, when the tag has been generated,
                    // we push it onto the evaluation stack in the exact same way
                    // as the string for the choice content.
                    if (state.inStringEvaluation()) {

                        Stack<StringValue> contentStackForTag = new Stack<>();
                        int outputCountConsumed = 0;

                        for (int i = state.getOutputStream().size() - 1; i >= 0; --i) {
                            RTObject obj = state.getOutputStream().get(i);

                            outputCountConsumed++;

                            if (obj instanceof ControlCommand) {
                                ControlCommand command = (ControlCommand) obj;
                                if (command.getCommandType() == ControlCommand.CommandType.BeginTag) {
                                    break;
                                } else {
                                    error("Unexpected ControlCommand while extracting tag from choice");
                                    break;
                                }
                            }

                            if (obj instanceof StringValue)
                                contentStackForTag.push((StringValue) obj);
                        }

                        // Consume the content that was produced for this string
                        state.popFromOutputStream(outputCountConsumed);

                        StringBuilder sb = new StringBuilder();
                        for (StringValue strVal : contentStackForTag) {
                            sb.append(strVal.value);
                        }

                        Tag choiceTag = new Tag(state.cleanOutputWhitespace(sb.toString()));
                        // Pushing to the evaluation stack means it gets picked up
                        // when a Choice is generated from the next Choice Point.
                        state.pushEvaluationStack(choiceTag);
                    }

                    // Otherwise! Simply push EndTag, so that in the output stream we
                    // have a structure of: [BeginTag, "the tag content", EndTag]
                    else {
                        state.pushToOutputStream(evalCommand);
                    }
                    break;
                }
                // Dynamic strings and tags are built in the same way
                case EndString: {

                    // Since we're iterating backward through the content,
                    // build a stack so that when we build the string,
                    // it's in the right order
                    Stack<RTObject> contentStackForString = new Stack<>();
                    Stack<RTObject> contentToRetain = new Stack<>();

                    int outputCountConsumed = 0;
                    for (int i = state.getOutputStream().size() - 1; i >= 0; --i) {
                        RTObject obj = state.getOutputStream().get(i);

                        outputCountConsumed++;

                        ControlCommand command = obj instanceof ControlCommand ? (ControlCommand) obj : null;

                        if (command != null && command.getCommandType() == ControlCommand.CommandType.BeginString) {
                            break;
                        }

                        if (obj instanceof Tag)
                            contentToRetain.push(obj);

                        if (obj instanceof StringValue)
                            contentStackForString.push(obj);
                    }

                    // Consume the content that was produced for this string
                    state.popFromOutputStream(outputCountConsumed);

                    // Rescue the tags that we want actually to keep on the output stack
                    // rather than consume as part of the string we're building.
                    // At the time of writing, this only applies to Tag objects generated
                    // by choices, which are pushed to the stack during string generation.
                    for (RTObject rescuedTag : contentToRetain)
                        state.pushToOutputStream(rescuedTag);

                    // Build string out of the content we collected
                    StringBuilder sb = new StringBuilder();

                    while (contentStackForString.size() > 0) {
                        RTObject c = contentStackForString.pop();

                        sb.append(c.toString());
                    }

                    // Return to expression evaluation (from content mode)
                    state.setInExpressionEvaluation(true);
                    state.pushEvaluationStack(new StringValue(sb.toString()));
                    break;
                }
                case ChoiceCount:
                    choiceCount = state.getGeneratedChoices().size();
                    state.pushEvaluationStack(new IntValue(choiceCount));
                    break;

                case Turns:
                    state.pushEvaluationStack(new IntValue(state.getCurrentTurnIndex() + 1));
                    break;

                case TurnsSince:
                case ReadCount:
                    RTObject target = state.popEvaluationStack();
                    if (!(target instanceof DivertTargetValue)) {
                        String extraNote = "";
                        if (target instanceof IntValue)
                            extraNote = ". Did you accidentally pass a read count ('knot_name') instead of a target " +
                                    "('-> knot_name')?";
                        error("TURNS_SINCE expected a divert target (knot, stitch, label name), but saw " + target
                                + extraNote);
                        break;
                    }

                    DivertTargetValue divertTarget = target instanceof DivertTargetValue ? (DivertTargetValue) target
                            : null;

                    RTObject otmp = contentAtPath(divertTarget.getTargetPath()).correctObj();
                    Container container = otmp instanceof Container ? (Container) otmp : null;

                    int eitherCount;

                    if (container != null) {
                        if (evalCommand.getCommandType() == ControlCommand.CommandType.TurnsSince)
                            eitherCount = state.turnsSinceForContainer(container);
                        else
                            eitherCount = state.visitCountForContainer(container);
                    } else {
                        if (evalCommand.getCommandType() == ControlCommand.CommandType.TurnsSince)
                            eitherCount = -1; // turn count, default to never/unknown
                        else
                            eitherCount = 0; // visit count, assume 0 to default to allowing entry

                        warning("Failed to find container for " + evalCommand.toString() + " lookup at "
                                + divertTarget.getTargetPath().toString());
                    }

                    state.pushEvaluationStack(new IntValue(eitherCount));
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

                case SeedRandom: {
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
                }
                case VisitIndex:
                    int count = state.visitCountForContainer(state.getCurrentPointer().container) - 1; // index
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
                        state.setCurrentPointer(Pointer.Null);
                    }

                    break;

                // Force flow to end completely
                case End:
                    state.forceEnd();
                    break;

                case ListFromInt: {
                    IntValue intVal = null;

                    RTObject o = state.popEvaluationStack();

                    if (o instanceof IntValue)
                        intVal = (IntValue) o;

                    StringValue listNameVal = null;

                    o = state.popEvaluationStack();

                    if (o instanceof StringValue)
                        listNameVal = (StringValue) o;

                    if (intVal == null) {
                        throw new StoryException(
                                "Passed non-integer when creating a list element from a numerical value.");
                    }

                    ListValue generatedListValue = null;

                    ListDefinition foundListDef = listDefinitions.getListDefinition(listNameVal.value);

                    if (foundListDef != null) {
                        InkListItem foundItem;

                        foundItem = foundListDef.getItemWithValue(intVal.value);

                        if (foundItem != null) {
                            generatedListValue = new ListValue(foundItem, intVal.value);
                        }
                    } else {
                        throw new StoryException("Failed to find List called " + listNameVal.value);
                    }

                    if (generatedListValue == null)
                        generatedListValue = new ListValue();

                    state.pushEvaluationStack(generatedListValue);
                    break;
                }

                case ListRange: {
                    RTObject p = state.popEvaluationStack();
                    Value<?> max = p instanceof Value ? (Value<?>) p : null;

                    p = state.popEvaluationStack();
                    Value<?> min = p instanceof Value ? (Value<?>) p : null;

                    p = state.popEvaluationStack();
                    ListValue targetList = p instanceof ListValue ? (ListValue) p : null;

                    if (targetList == null || min == null || max == null)
                        throw new StoryException("Expected List, minimum and maximum for LIST_RANGE");

                    InkList result = targetList.value.listWithSubRange(min.getValueObject(), max.getValueObject());

                    state.pushEvaluationStack(new ListValue(result));
                    break;
                }

                case ListRandom: {

                    RTObject o = state.popEvaluationStack();
                    ListValue listVal = o instanceof ListValue ? (ListValue) o : null;

                    if (listVal == null)
                        throw new StoryException("Expected list for LIST_RANDOM");

                    InkList list = listVal.value;

                    InkList newList = null;

                    // List was empty: return empty list
                    if (list.size() == 0) {
                        newList = new InkList();
                    }

                    // Non-empty source list
                    else {
                        // Generate a random index for the element to take
                        int resultSeed = state.getStorySeed() + state.getPreviousRandom();
                        Random random = new Random(resultSeed);

                        int nextRandom = random.nextInt(Integer.MAX_VALUE);
                        int listItemIndex = nextRandom % list.size();

                        // Iterate through to get the random element
                        Iterator<Entry<InkListItem, Integer>> listEnumerator = list.entrySet().iterator();

                        Entry<InkListItem, Integer> randomItem = null;

                        for (int i = 0; i <= listItemIndex; i++) {
                            randomItem = listEnumerator.next();
                        }

                        // Origin list is simply the origin of the one element
                        newList = new InkList(randomItem.getKey().getOriginName(), this);
                        newList.put(randomItem.getKey(), randomItem.getValue());

                        state.setPreviousRandom(nextRandom);
                    }

                    state.pushEvaluationStack(new ListValue(newList));
                    break;
                }

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
                int count = state.visitCountForContainer(container);
                foundValue = new IntValue(count);
            }

            // Normal variable reference
            else {

                foundValue = state.getVariablesState().getVariableWithName(varRef.getName());

                if (foundValue == null) {
                    warning("Variable not found: '" + varRef.getName()
                            + "'. Using default value of 0 (false). This can happen with temporary variables if the " +
                            "declaration hasn't yet been hit. Globals are always given a default value on load if a " +
                            "value doesn't exist in the save state.");
                    foundValue = new IntValue(0);
                }
            }

            state.pushEvaluationStack(foundValue);

            return true;
        }

        // Native function call
        else if (contentObj instanceof NativeFunctionCall) {
            NativeFunctionCall func = (NativeFunctionCall) contentObj;
            List<RTObject> funcParams = state.popEvaluationStack(func.getNumberOfParameters());

            RTObject result = func.call(funcParams);
            state.pushEvaluationStack(result);
            return true;
        }

        // No control content, must be ordinary content
        return false;
    }

    // Assumption: prevText is the snapshot where we saw a newline, and we're
    // checking whether we're really done
    // with that line. Therefore prevText will definitely end in a newline.
    //
    // We take tags into account too, so that a tag following a content line:
    // Content
    // # tag
    // ... doesn't cause the tag to be wrongly associated with the content above.
    enum OutputStateChange {
        NoChange, ExtendedBeyondNewline, NewlineRemoved
    }

    OutputStateChange calculateNewlineOutputStateChange(String prevText, String currText, int prevTagCount,
                                                        int currTagCount) {
        // Simple case: nothing's changed, and we still have a newline
        // at the end of the current content
        boolean newlineStillExists = currText.length() >= prevText.length() && prevText.length() > 0
                && currText.charAt(prevText.length() - 1) == '\n';
        if (prevTagCount == currTagCount && prevText.length() == currText.length() && newlineStillExists)
            return OutputStateChange.NoChange;

        // Old newline has been removed, it wasn't the end of the line after all
        if (!newlineStillExists) {
            return OutputStateChange.NewlineRemoved;
        }

        // Tag added - definitely the start of a new line
        if (currTagCount > prevTagCount)
            return OutputStateChange.ExtendedBeyondNewline;

        // There must be new content - check whether it's just whitespace
        for (int i = prevText.length(); i < currText.length(); i++) {
            char c = currText.charAt(i);
            if (c != ' ' && c != '\t') {
                return OutputStateChange.ExtendedBeyondNewline;
            }
        }

        // There's new text but it's just spaces and tabs, so there's still the
        // potential
        // for glue to kill the newline.
        return OutputStateChange.NoChange;
    }

    String popChoiceStringAndTags(List<String> tags) {
        StringValue choiceOnlyStrVal = (StringValue) state.popEvaluationStack();

        while (state.getEvaluationStack().size() > 0 && state.peekEvaluationStack() instanceof Tag) {
            Tag tag = (Tag) state.popEvaluationStack();
            tags.add(0, tag.getText()); // popped in reverse order
        }

        return choiceOnlyStrVal.value;
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
        List<String> tags = new ArrayList<>(0);

        if (choicePoint.hasChoiceOnlyContent()) {
            choiceOnlyText = popChoiceStringAndTags(tags);
        }

        if (choicePoint.hasStartContent()) {
            startText = popChoiceStringAndTags(tags);
        }

        // Don't create choice if player has already read this content
        if (choicePoint.isOnceOnly()) {
            int visitCount = state.visitCountForContainer(choicePoint.getChoiceTarget());
            if (visitCount > 0) {
                showChoice = false;
            }
        }

        // We go through the full process of creating the choice above so
        // that we consume the content for it, since otherwise it'll
        // be shown on the output stream.
        if (!showChoice) {
            return null;
        }

        Choice choice = new Choice();
        choice.targetPath = choicePoint.getPathOnChoice();
        choice.sourcePath = choicePoint.getPath().toString();
        choice.isInvisibleDefault = choicePoint.isInvisibleDefault();
        choice.tags = tags;

        // We need to capture the state of the callstack at the point where
        // the choice was generated, since after the generation of this choice
        // we may go on to pop out from a tunnel (possible if the choice was
        // wrapped in a conditional), or we may pop out from a thread,
        // at which point that thread is discarded.
        // Fork clones the thread, gives it a new ID, but without affecting
        // the thread stack itself.
        choice.setThreadAtGeneration(state.getCallStack().forkThread());

        // Set final text for the choice
        choice.setText((startText + choiceOnlyText).trim());

        return choice;
    }

    /**
     * Unwinds the callstack. Useful to reset the Story's evaluation without
     * actually changing any meaningful state, for example if you want to exit a
     * section of story prematurely and tell it to go elsewhere with a call to
     * ChoosePathString(...). Doing so without calling ResetCallstack() could cause
     * unexpected issues if, for example, the Story was in a tunnel already.
     */
    public void resetCallstack() throws Exception {
        ifAsyncWeCant("ResetCallstack");

        state.forceEnd();
    }

    void resetErrors() {
        state.resetErrors();
    }

    void resetGlobals() throws Exception {
        if (mainContentContainer.getNamedContent().containsKey("global decl")) {
            final Pointer originalPointer = new Pointer(state.getCurrentPointer());

            choosePath(new Path("global decl"), false);

            // Continue, but without validating external bindings,
            // since we may be doing this reset at initialisation time.
            continueInternal();

            state.setCurrentPointer(originalPointer);
        }

        state.getVariablesState().snapshotDefaultGlobals();
    }

    /**
     * Reset the Story back to its initial state as it was when it was first
     * constructed.
     */
    public void resetState() throws Exception {
        // TODO: Could make this possible
        ifAsyncWeCant("ResetState");

        state = new StoryState(this);

        state.getVariablesState().setVariableChangedEvent(this);

        resetGlobals();
    }

    Pointer pointerAtPath(Path path) throws Exception {
        if (path.getLength() == 0)
            return Pointer.Null;

        final Pointer p = new Pointer();

        int pathLengthToUse = path.getLength();
        final SearchResult result;

        if (path.getLastComponent().isIndex()) {
            pathLengthToUse = path.getLength() - 1;
            result = new SearchResult(mainContentContainer.contentAtPath(path, 0, pathLengthToUse));
            p.container = result.getContainer();
            p.index = path.getLastComponent().getIndex();
        } else {
            result = new SearchResult(mainContentContainer.contentAtPath(path));
            p.container = result.getContainer();
            p.index = -1;
        }

        if (result.obj == null || result.obj == mainContentContainer && pathLengthToUse > 0)
            error("Failed to find content at path '" + path + "', and no approximation of it was possible.");
        else if (result.approximate)
            warning("Failed to find content at path '" + path + "', so it was approximated to: '" + result.obj.getPath()
                    + "'.");

        return p;
    }

    void step() throws Exception {

        boolean shouldAddToStream = true;

        // Get current content
        final Pointer pointer = new Pointer();
        pointer.assign(state.getCurrentPointer());

        if (pointer.isNull()) {
            return;
        }

        // Step directly to the first element of content in a container (if
        // necessary)
        RTObject r = pointer.resolve();
        Container containerToEnter = r instanceof Container ? (Container) r : null;

        while (containerToEnter != null) {

            // Mark container as being entered
            visitContainer(containerToEnter, true);

            // No content? the most we can do is step past it
            if (containerToEnter.getContent().size() == 0)
                break;

            pointer.assign(Pointer.startOf(containerToEnter));

            r = pointer.resolve();
            containerToEnter = r instanceof Container ? (Container) r : null;
        }

        state.setCurrentPointer(pointer);

        if (profiler != null) {
            profiler.step(state.getCallStack());
        }

        // Is the current content Object:
        // - Normal content
        // - Or a logic/flow statement - if so, do it
        // Stop flow if we hit a stack pop when we're unable to pop (e.g.
        // return/done statement in knot
        // that was diverted to rather than called as a function)
        RTObject currentContentObj = pointer.resolve();
        boolean isLogicOrFlowControl = performLogicAndFlowControl(currentContentObj);

        // Has flow been forced to end by flow control above?
        if (state.getCurrentPointer().isNull()) {
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
                state.getGeneratedChoices().add(choice);
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
                    ? (VariablePointerValue) currentContentObj
                    : null;

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
        if (controlCmd != null && controlCmd.getCommandType() == ControlCommand.CommandType.StartThread) {
            state.getCallStack().pushThread();
        }
    }

    /**
     * The Story itself in JSON representation.
     *
     * @throws Exception
     */
    public String toJson() throws Exception {
        // return ToJsonOld();
        SimpleJson.Writer writer = new SimpleJson.Writer();
        toJson(writer);
        return writer.toString();
    }

    /**
     * The Story itself in JSON representation.
     *
     * @throws Exception
     */
    public void toJson(OutputStream stream) throws Exception {
        SimpleJson.Writer writer = new SimpleJson.Writer(stream);
        toJson(writer);
    }

    void toJson(SimpleJson.Writer writer) throws Exception {
        writer.writeObjectStart();

        writer.writeProperty("inkVersion", inkVersionCurrent);

        // Main container content
        writer.writeProperty("root", new InnerWriter() {

            @Override
            public void write(Writer w) throws Exception {
                Json.writeRuntimeContainer(w, mainContentContainer);
            }
        });

        // List definitions
        if (listDefinitions != null) {

            writer.writePropertyStart("listDefs");
            writer.writeObjectStart();

            for (ListDefinition def : listDefinitions.getLists()) {
                writer.writePropertyStart(def.getName());
                writer.writeObjectStart();

                for (Entry<InkListItem, Integer> itemToVal : def.getItems().entrySet()) {
                    InkListItem item = itemToVal.getKey();
                    int val = itemToVal.getValue();
                    writer.writeProperty(item.getItemName(), val);
                }

                writer.writeObjectEnd();
                writer.writePropertyEnd();
            }

            writer.writeObjectEnd();
            writer.writePropertyEnd();
        }

        writer.writeObjectEnd();
    }

    boolean tryFollowDefaultInvisibleChoice() throws Exception {
        List<Choice> allChoices = state.getCurrentChoices();

        // Is a default invisible choice the ONLY choice?
        // var invisibleChoices = allChoices.Where (c =>
        // c.choicePoint.isInvisibleDefault).ToList();
        ArrayList<Choice> invisibleChoices = new ArrayList<>();
        for (Choice c : allChoices) {
            if (c.isInvisibleDefault) {
                invisibleChoices.add(c);
            }
        }

        if (invisibleChoices.size() == 0 || allChoices.size() > invisibleChoices.size())
            return false;

        Choice choice = invisibleChoices.get(0);

        // Invisible choice may have been generated on a different thread,
        // in which case we need to restore it before we continue
        state.getCallStack().setCurrentThread(choice.getThreadAtGeneration());

        // If there's a chance that this state will be rolled back to before
        // the invisible choice then make sure that the choice thread is
        // left intact, and it isn't re-entered in an old state.
        if (stateSnapshotAtLastNewline != null)
            state.getCallStack().setCurrentThread(state.getCallStack().forkThread());

        choosePath(choice.targetPath, false);

        return true;
    }

    /**
     * Remove a binding for a named EXTERNAL ink function.
     */
    public void unbindExternalFunction(String funcName) throws Exception {
        ifAsyncWeCant("unbind an external a function");
        Assert(externals.containsKey(funcName), "Function '" + funcName + "' has not been bound.");
        externals.remove(funcName);
    }

    /**
     * Check that all EXTERNAL ink functions have a valid bound C# function. Note
     * that this is automatically called on the first call to Continue().
     */
    public void validateExternalBindings() throws Exception {
        HashSet<String> missingExternals = new HashSet<>();

        validateExternalBindings(mainContentContainer, missingExternals);
        hasValidatedExternals = true;
        // No problem! Validation complete
        if (missingExternals.size() == 0) {
            hasValidatedExternals = true;
        } else { // Error for all missing externals

            StringBuilder join = new StringBuilder();
            boolean first = true;
            for (String item : missingExternals) {
                if (first)
                    first = false;
                else
                    join.append(", ");

                join.append(item);
            }

            String message = String.format("ERROR: Missing function binding for external%s: '%s' %s",
                    missingExternals.size() > 1 ? "s" : "", join.toString(),
                    allowExternalFunctionFallbacks ? ", and no fallback ink function found."
                            : " (ink fallbacks disabled)");

            error(message);
        }
    }

    void validateExternalBindings(Container c, HashSet<String> missingExternals) throws Exception {
        for (RTObject innerContent : c.getContent()) {
            Container container = innerContent instanceof Container ? (Container) innerContent : null;
            if (container == null || !container.hasValidName())
                validateExternalBindings(innerContent, missingExternals);
        }

        for (INamedContent innerKeyValue : c.getNamedContent().values()) {
            validateExternalBindings(innerKeyValue instanceof RTObject ? (RTObject) innerKeyValue : (RTObject) null,
                    missingExternals);
        }
    }

    void validateExternalBindings(RTObject o, HashSet<String> missingExternals) throws Exception {
        Container container = o instanceof Container ? (Container) o : null;

        if (container != null) {
            validateExternalBindings(container, missingExternals);
            return;
        }

        Divert divert = o instanceof Divert ? (Divert) o : null;

        if (divert != null && divert.isExternal()) {
            String name = divert.getTargetPathString();

            if (!externals.containsKey(name)) {

                if (allowExternalFunctionFallbacks) {
                    boolean fallbackFound = mainContentContainer.getNamedContent().containsKey(name);
                    if (!fallbackFound) {
                        missingExternals.add(name);
                    }
                } else {
                    missingExternals.add(name);
                }
            }
        }
    }

    void visitChangedContainersDueToDivert() throws Exception {
        final Pointer previousPointer = new Pointer(state.getPreviousPointer());
        final Pointer pointer = new Pointer(state.getCurrentPointer());

        // Unless we're pointing *directly* at a piece of content, we don't do
        // counting here. Otherwise, the main stepping function will do the counting.
        if (pointer.isNull() || pointer.index == -1)
            return;

        // First, find the previously open set of containers

        prevContainers.clear();

        if (!previousPointer.isNull()) {

            Container prevAncestor = null;

            if (previousPointer.resolve() instanceof Container) {
                prevAncestor = (Container) previousPointer.resolve();
            } else if (previousPointer.container instanceof Container) {
                prevAncestor = previousPointer.container;
            }

            while (prevAncestor != null) {
                prevContainers.add(prevAncestor);
                prevAncestor = prevAncestor.getParent() instanceof Container ? (Container) prevAncestor.getParent()
                        : null;
            }
        }

        // If the new Object is a container itself, it will be visited
        // automatically at the next actual
        // content step. However, we need to walk up the new ancestry to see if
        // there are more new containers
        RTObject currentChildOfContainer = pointer.resolve();

        // Invalid pointer? May happen if attemptingto
        if (currentChildOfContainer == null)
            return;

        Container currentContainerAncestor = currentChildOfContainer.getParent() instanceof Container
                ? (Container) currentChildOfContainer.getParent()
                : null;

        boolean allChildrenEnteredAtStart = true;
        while (currentContainerAncestor != null && (!prevContainers.contains(currentContainerAncestor)
                || currentContainerAncestor.getCountingAtStartOnly())) {

            // Check whether this ancestor container is being entered at the
            // start,
            // by checking whether the child Object is the first.
            boolean enteringAtStart = currentContainerAncestor.getContent().size() > 0
                    && currentChildOfContainer == currentContainerAncestor.getContent().get(0)
                    && allChildrenEnteredAtStart;

            // Don't count it as entering at start if we're entering random somewhere within
            // a container B that happens to be nested at index 0 of container A. It only
            // counts
            // if we're diverting directly to the first leaf node.
            if (!enteringAtStart)
                allChildrenEnteredAtStart = false;

            // Mark a visit to this container
            visitContainer(currentContainerAncestor, enteringAtStart);

            currentChildOfContainer = currentContainerAncestor;
            currentContainerAncestor = currentContainerAncestor.getParent() instanceof Container
                    ? (Container) currentContainerAncestor.getParent()
                    : null;

        }
    }

    // Mark a container as having been visited
    void visitContainer(Container container, boolean atStart) throws Exception {
        if (!container.getCountingAtStartOnly() || atStart) {
            if (container.getVisitsShouldBeCounted())
                state.incrementVisitCountForContainer(container);

            if (container.getTurnIndexShouldBeCounted())
                state.recordTurnIndexVisitToContainer(container);
        }
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
     * @param functionName The name of the function as declared in ink.
     * @param arguments    The arguments that the ink function takes, if any. Note
     *                     that we don't (can't) do any validation on the number of
     *                     arguments right now, so make sure you get it right!
     * @return The return value as returned from the ink function with `~ return
     * myValue`, or null if nothing is returned.
     * @throws Exception
     */
    public Object evaluateFunction(String functionName, Object[] arguments) throws Exception {
        return evaluateFunction(functionName, null, arguments);
    }

    public Object evaluateFunction(String functionName) throws Exception {
        return evaluateFunction(functionName, null, null);
    }

    /**
     * Checks if a function exists.
     *
     * @param functionName The name of the function as declared in ink.
     * @return True if the function exists, else false.
     */
    public boolean hasFunction(String functionName) {
        try {
            return knotContainerWithName(functionName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Evaluates a function defined in ink, and gathers the possibly multi-line text
     * as generated by the function.
     *
     * @param arguments    The arguments that the ink function takes, if any. Note
     *                     that we don't (can't) do any validation on the number of
     *                     arguments right now, so make sure you get it right!
     * @param functionName The name of the function as declared in ink.
     * @param textOutput   This text output is any text written as normal content
     *                     within the function, as opposed to the return value, as
     *                     returned with `~ return`.
     * @return The return value as returned from the ink function with `~ return
     * myValue`, or null if nothing is returned.
     * @throws Exception
     */
    public Object evaluateFunction(String functionName, StringBuilder textOutput, Object[] arguments) throws Exception {
        ifAsyncWeCant("evaluate a function");

        if (functionName == null) {
            throw new Exception("Function is null");
        } else if (functionName.trim().isEmpty()) {
            throw new Exception("Function is empty or white space.");
        }

        // Get the content that we need to run
        Container funcContainer = knotContainerWithName(functionName);
        if (funcContainer == null)
            throw new Exception("Function doesn't exist: '" + functionName + "'");

        // Snapshot the output stream
        ArrayList<RTObject> outputStreamBefore = new ArrayList<>(state.getOutputStream());
        state.resetOutput();

        // State will temporarily replace the callstack in order to evaluate
        state.startFunctionEvaluationFromGame(funcContainer, arguments);

        // Evaluate the function, and collect the string output
        while (canContinue()) {
            String text = Continue();

            if (textOutput != null)
                textOutput.append(text);
        }

        // Restore the output stream in case this was called
        // during main story evaluation.
        state.resetOutput(outputStreamBefore);

        // Finish evaluation, and see whether anything was produced
        Object result = state.completeFunctionEvaluationFromGame();
        return result;
    }

    // Maximum snapshot stack:
    // - stateSnapshotDuringSave -- not retained, but returned to game code
    // - _stateSnapshotAtLastNewline (has older patch)
    // - _state (current, being patched)
    void stateSnapshot() {
        stateSnapshotAtLastNewline = state;
        state = state.copyAndStartPatching();
    }

    void restoreStateSnapshot() {
        // Patched state had temporarily hijacked our
        // VariablesState and set its own callstack on it,
        // so we need to restore that.
        // If we're in the middle of saving, we may also
        // need to give the VariablesState the old patch.
        stateSnapshotAtLastNewline.restoreAfterPatch();

        state = stateSnapshotAtLastNewline;
        stateSnapshotAtLastNewline = null;

        // If save completed while the above snapshot was
        // active, we need to apply any changes made since
        // the save was started but before the snapshot was made.
        if (!asyncSaving) {
            state.applyAnyPatch();
        }
    }

    void discardSnapshot() {
        // Normally we want to integrate the patch
        // into the main global/counts dictionaries.
        // However, if we're in the middle of async
        // saving, we simply stay in a "patching" state,
        // albeit with the newer cloned patch.
        if (!asyncSaving)
            state.applyAnyPatch();

        // No longer need the snapshot.
        stateSnapshotAtLastNewline = null;
    }

    /**
     * Advanced usage! If you have a large story, and saving state to JSON takes too
     * long for your framerate, you can temporarily freeze a copy of the state for
     * saving on a separate thread. Internally, the engine maintains a "diff patch".
     * When you've finished saving your state, call BackgroundSaveComplete() and
     * that diff patch will be applied, allowing the story to continue in its usual
     * mode.
     *
     * @return The state for background thread save.
     * @throws Exception
     */
    public StoryState copyStateForBackgroundThreadSave() throws Exception {
        ifAsyncWeCant("start saving on a background thread");
        if (asyncSaving)
            throw new Exception(
                    "Story is already in background saving mode, can't call CopyStateForBackgroundThreadSave again!");
        StoryState stateToSave = state;
        state = state.copyAndStartPatching();
        asyncSaving = true;
        return stateToSave;
    }

    /**
     * See CopyStateForBackgroundThreadSave. This method releases the "frozen" save
     * state, applying its patch that it was using internally.
     */
    public void backgroundSaveComplete() {
        // CopyStateForBackgroundThreadSave must be called outside
        // of any async ink evaluation, since otherwise you'd be saving
        // during an intermediate state.
        // However, it's possible to *complete* the save in the middle of
        // a glue-lookahead when there's a state stored in _stateSnapshotAtLastNewline.
        // This state will have its own patch that is newer than the save patch.
        // We hold off on the final apply until the glue-lookahead is finished.
        // In that case, the apply is always done, it's just that it may
        // apply the looked-ahead changes OR it may simply apply the changes
        // made during the save process to the old _stateSnapshotAtLastNewline state.
        if (stateSnapshotAtLastNewline == null) {
            state.applyAnyPatch();
        }

        asyncSaving = false;
    }
}
