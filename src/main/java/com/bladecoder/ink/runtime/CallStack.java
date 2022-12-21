package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.SimpleJson.InnerWriter;
import com.bladecoder.ink.runtime.SimpleJson.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CallStack {
    public static class Element {
        public final Pointer currentPointer = new Pointer();

        public boolean inExpressionEvaluation;
        public HashMap<String, RTObject> temporaryVariables;

        public PushPopType type;

        // When this callstack element is actually a function evaluation called from the
        // game,
        // we need to keep track of the size of the evaluation stack when it was called
        // so that we know whether there was any return value.
        public int evaluationStackHeightWhenPushed;

        // When functions are called, we trim whitespace from the start and end of what
        // they generate, so we make sure know where the function's start and end are.
        public int functionStartInOuputStream;

        public Element(PushPopType type, Pointer pointer) {
            this(type, pointer, false);
        }

        public Element(PushPopType type, Pointer pointer, boolean inExpressionEvaluation) {
            this.currentPointer.assign(pointer);

            this.inExpressionEvaluation = inExpressionEvaluation;
            this.temporaryVariables = new HashMap<>();
            this.type = type;
        }

        public Element copy() {
            Element copy = new Element(this.type, currentPointer, this.inExpressionEvaluation);
            copy.temporaryVariables = new HashMap<>(this.temporaryVariables);
            copy.evaluationStackHeightWhenPushed = evaluationStackHeightWhenPushed;
            copy.functionStartInOuputStream = functionStartInOuputStream;
            return copy;
        }
    }

    public static class Thread {
        public List<Element> callstack;
        public final Pointer previousPointer = new Pointer();
        public int threadIndex;

        public Thread() {
            callstack = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        public Thread(HashMap<String, Object> jThreadObj, Story storyContext) throws Exception {
            this();
            threadIndex = (int) jThreadObj.get("threadIndex");

            List<Object> jThreadCallstack = (List<Object>) jThreadObj.get("callstack");

            for (Object jElTok : jThreadCallstack) {

                HashMap<String, Object> jElementObj = (HashMap<String, Object>) jElTok;

                PushPopType pushPopType = PushPopType.values()[(Integer) jElementObj.get("type")];

                final Pointer pointer = new Pointer(Pointer.Null);

                String currentContainerPathStr = null;
                Object currentContainerPathStrToken = jElementObj.get("cPath");
                if (currentContainerPathStrToken != null) {
                    currentContainerPathStr = currentContainerPathStrToken.toString();
                    final SearchResult threadPointerResult =
                            storyContext.contentAtPath(new Path(currentContainerPathStr));
                    pointer.container = threadPointerResult.getContainer();
                    pointer.index = (int) jElementObj.get("idx");

                    if (threadPointerResult.obj == null)
                        throw new Exception("When loading state, internal story location couldn't be found: "
                                + currentContainerPathStr
                                + ". Has the story changed since this save data was created?");
                    else if (threadPointerResult.approximate)
                        storyContext.warning("When loading state, exact internal story location couldn't be found: '"
                                + currentContainerPathStr + "', so it was approximated to '"
                                + pointer.container.getPath().toString()
                                + "' to recover. Has the story changed since this save data was created?");
                }

                boolean inExpressionEvaluation = (boolean) jElementObj.get("exp");

                Element el = new Element(pushPopType, pointer, inExpressionEvaluation);

                Object temps = jElementObj.get("temp");
                if (temps != null) {
                    el.temporaryVariables = Json.jObjectToHashMapRuntimeObjs((HashMap<String, Object>) temps);
                } else {
                    el.temporaryVariables.clear();
                }

                callstack.add(el);
            }

            Object prevContentObjPath = jThreadObj.get("previousContentObject");
            if (prevContentObjPath != null) {
                Path prevPath = new Path((String) prevContentObjPath);
                previousPointer.assign(storyContext.pointerAtPath(prevPath));
            }
        }

        public Thread copy() {
            Thread copy = new Thread();
            copy.threadIndex = threadIndex;
            for (Element e : callstack) {
                copy.callstack.add(e.copy());
            }
            copy.previousPointer.assign(previousPointer);
            return copy;
        }

        public void writeJson(SimpleJson.Writer writer) throws Exception {
            writer.writeObjectStart();

            // callstack
            writer.writePropertyStart("callstack");
            writer.writeArrayStart();
            for (CallStack.Element el : callstack) {
                writer.writeObjectStart();
                if (!el.currentPointer.isNull()) {
                    writer.writeProperty(
                            "cPath", el.currentPointer.container.getPath().getComponentsString());
                    writer.writeProperty("idx", el.currentPointer.index);
                }

                writer.writeProperty("exp", el.inExpressionEvaluation);
                writer.writeProperty("type", el.type.ordinal());

                if (el.temporaryVariables.size() > 0) {
                    writer.writePropertyStart("temp");
                    Json.writeDictionaryRuntimeObjs(writer, el.temporaryVariables);
                    writer.writePropertyEnd();
                }

                writer.writeObjectEnd();
            }
            writer.writeArrayEnd();
            writer.writePropertyEnd();

            // threadIndex
            writer.writeProperty("threadIndex", threadIndex);

            if (!previousPointer.isNull()) {
                writer.writeProperty(
                        "previousContentObject",
                        previousPointer.resolve().getPath().toString());
            }

            writer.writeObjectEnd();
        }
    }

    private int threadCounter;
    private final Pointer startOfRoot = new Pointer();

    private List<Thread> threads;

    public CallStack(CallStack toCopy) {
        threads = new ArrayList<>();
        for (Thread otherThread : toCopy.threads) {
            threads.add(otherThread.copy());
        }

        threadCounter = toCopy.threadCounter;
        startOfRoot.assign(toCopy.startOfRoot);
    }

    public CallStack(Story storyContext) {
        startOfRoot.assign(Pointer.startOf(storyContext.getRootContentContainer()));

        reset();
    }

    public void reset() {
        threads = new ArrayList<>();
        threads.add(new Thread());

        threads.get(0).callstack.add(new Element(PushPopType.Tunnel, startOfRoot));
    }

    public boolean canPop() {
        return getCallStack().size() > 1;
    }

    public boolean canPop(PushPopType type) {

        if (!canPop()) return false;

        if (type == null) return true;

        return getCurrentElement().type == type;
    }

    public boolean canPopThread() {
        return threads.size() > 1 && !elementIsEvaluateFromGame();
    }

    public boolean elementIsEvaluateFromGame() {
        return getCurrentElement().type == PushPopType.FunctionEvaluationFromGame;
    }

    // Find the most appropriate context for this variable.
    // Are we referencing a temporary or global variable?
    // Note that the compiler will have warned us about possible conflicts,
    // so anything that happens here should be safe!
    public int contextForVariableNamed(String name) {
        // Current temporary context?
        // (Shouldn't attempt to access contexts higher in the callstack.)
        if (getCurrentElement().temporaryVariables.containsKey(name)) {
            return getCurrentElementIndex() + 1;
        }

        // Global
        else {
            return 0;
        }
    }

    public int getDepth() {
        return getElements().size();
    }

    public Element getCurrentElement() {
        Thread thread = threads.get(threads.size() - 1);
        List<Element> cs = thread.callstack;
        return cs.get(cs.size() - 1);
    }

    public int getCurrentElementIndex() {
        return getCallStack().size() - 1;
    }

    private List<Element> getCallStack() {
        return getcurrentThread().callstack;
    }

    public Thread getcurrentThread() {
        return threads.get(threads.size() - 1);
    }

    //
    public List<Element> getElements() {
        return getCallStack();
    }

    public void writeJson(SimpleJson.Writer w) throws Exception {
        w.writeObject(new InnerWriter() {

            @Override
            public void write(Writer writer) throws Exception {
                writer.writePropertyStart("threads");
                writer.writeArrayStart();

                for (CallStack.Thread thread : threads) {
                    thread.writeJson(writer);
                }
                writer.writeArrayEnd();
                writer.writePropertyEnd();

                writer.writePropertyStart("threadCounter");
                writer.write(threadCounter);
                writer.writePropertyEnd();
            }
        });
    }

    public RTObject getTemporaryVariableWithName(String name) {
        return getTemporaryVariableWithName(name, -1);
    }

    // Get variable value, dereferencing a variable pointer if necessary
    public RTObject getTemporaryVariableWithName(String name, int contextIndex) {
        if (contextIndex == -1) contextIndex = getCurrentElementIndex() + 1;

        Element contextElement = getCallStack().get(contextIndex - 1);
        RTObject varValue = contextElement.temporaryVariables.get(name);

        return varValue;
    }

    public void pop() throws Exception {
        pop(null);
    }

    public void pop(PushPopType type) throws Exception {
        if (canPop(type)) {
            getCallStack().remove(getCallStack().size() - 1);
            return;
        } else {
            throw new Exception("Mismatched push/pop in Callstack");
        }
    }

    public void popThread() throws Exception {
        if (canPopThread()) {
            threads.remove(getcurrentThread());
        } else {
            throw new Exception("Can't pop thread");
        }
    }

    public void push(PushPopType type) {
        push(type, 0, 0);
    }

    public void push(PushPopType type, int externalEvaluationStackHeight) {
        push(type, externalEvaluationStackHeight, 0);
    }

    public void push(PushPopType type, int externalEvaluationStackHeight, int outputStreamLengthWithPushed) {
        // When pushing to callstack, maintain the current content path, but
        // jump
        // out of expressions by default
        Element element = new Element(type, getCurrentElement().currentPointer, false);

        element.evaluationStackHeightWhenPushed = externalEvaluationStackHeight;
        element.functionStartInOuputStream = outputStreamLengthWithPushed;

        getCallStack().add(element);
    }

    public void pushThread() {
        Thread newThread = getcurrentThread().copy();
        threadCounter++;
        newThread.threadIndex = threadCounter;
        threads.add(newThread);
    }

    public void setCurrentThread(Thread value) {
        // Debug.Assert (threads.Count == 1, "Shouldn't be directly setting the
        // current thread when we have a stack of them");
        threads.clear();
        threads.add(value);
    }

    // Unfortunately it's not possible to implement jsonToken since
    // the setter needs to take a Story as a context in order to
    // look up RTObjects from paths for currentContainer within elements.
    @SuppressWarnings("unchecked")
    public void setJsonToken(HashMap<String, Object> jRTObject, Story storyContext) throws Exception {
        threads.clear();

        List<Object> jThreads = (List<Object>) jRTObject.get("threads");

        for (Object jThreadTok : jThreads) {
            HashMap<String, Object> jThreadObj = (HashMap<String, Object>) jThreadTok;
            Thread thread = new Thread(jThreadObj, storyContext);
            threads.add(thread);
        }

        threadCounter = (int) jRTObject.get("threadCounter");
        startOfRoot.assign(Pointer.startOf(storyContext.getRootContentContainer()));
    }

    public Thread forkThread() {
        Thread forkedThread = getcurrentThread().copy();
        threadCounter++;
        forkedThread.threadIndex = threadCounter;
        return forkedThread;
    }

    public void setTemporaryVariable(String name, RTObject value, boolean declareNew) {
        setTemporaryVariable(name, value, declareNew);
    }

    public void setTemporaryVariable(String name, RTObject value, boolean declareNew, int contextIndex)
            throws Exception {
        if (contextIndex == -1) contextIndex = getCurrentElementIndex() + 1;

        Element contextElement = getCallStack().get(contextIndex - 1);

        if (!declareNew && !contextElement.temporaryVariables.containsKey(name)) {
            throw new Exception("Could not find temporary variable to set: " + name);
        }

        RTObject oldValue = contextElement.temporaryVariables.get(name);

        if (oldValue != null) ListValue.retainListOriginsForAssignment(oldValue, value);

        contextElement.temporaryVariables.put(name, value);
    }

    public Thread getThreadWithIndex(int index) {
        // return threads.Find (t => t.threadIndex == index);

        for (Thread t : threads) if (t.threadIndex == index) return t;

        return null;
    }

    String getCallStackTrace() {
        StringBuilder sb = new StringBuilder();

        for (int t = 0; t < threads.size(); t++) {

            Thread thread = threads.get(t);
            boolean isCurrent = (t == threads.size() - 1);
            sb.append(String.format(
                    "=== THREAD %d/%d %s===\n", (t + 1), threads.size(), (isCurrent ? "(current) " : "")));

            for (int i = 0; i < thread.callstack.size(); i++) {

                if (thread.callstack.get(i).type == PushPopType.Function) sb.append("  [FUNCTION] ");
                else sb.append("  [TUNNEL] ");

                final Pointer pointer = new Pointer();
                pointer.assign(thread.callstack.get(i).currentPointer);
                if (!pointer.isNull()) {
                    sb.append("<SOMEWHERE IN ");
                    sb.append(pointer.container.getPath().toString());
                    sb.append(">\n");
                }
            }
        }

        return sb.toString();
    }
}
