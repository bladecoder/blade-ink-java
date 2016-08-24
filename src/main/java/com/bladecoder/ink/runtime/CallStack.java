package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class CallStack {
	static class Element {
		public Container currentContainer;
		public int currentContentIndex;

		public RTObject currentRTObject;
		public boolean inExpressionEvaluation;
		public HashMap<String, RTObject> temporaryVariables;

		public PushPopType type;

		public Element(PushPopType type, Container container, int contentIndex) {
			this(type, container, contentIndex, false);
		}

		public Element(PushPopType type, Container container, int contentIndex, boolean inExpressionEvaluation) {
			this.currentContainer = container;
			this.currentContentIndex = contentIndex;
			this.inExpressionEvaluation = inExpressionEvaluation;
			this.temporaryVariables = new HashMap<String, RTObject>();
			this.type = type;
		}

		public Element copy() {
			Element copy = new Element(this.type, this.currentContainer, this.currentContentIndex,
					this.inExpressionEvaluation);
			copy.temporaryVariables = new HashMap<String,RTObject>(this.temporaryVariables);
			return copy;
		}

		public RTObject getCurrentRTObject() {
			if (currentContainer != null && currentContentIndex < currentContainer.getContent().size()) {
				return currentContainer.getContent().get(currentContentIndex);
			}

			return null;
		}

		public void setcurrentRTObject(RTObject currentObj) {
			if (currentObj == null) {
				currentContainer = null;
				currentContentIndex = 0;
				return;
			}

			currentContainer = currentObj.getParent() instanceof Container ? (Container) currentObj.getParent() : null;

			if (currentContainer != null)
				currentContentIndex = currentContainer.getContent().indexOf(currentObj);

			// Two reasons why the above operation might not work:
			// - currentObj is already the root container
			// - currentObj is a named container rather than being an RTObject
			// at an index
			if (currentContainer == null || currentContentIndex == -1) {
				currentContainer = currentObj instanceof Container ? (Container) currentObj : null;
				currentContentIndex = 0;
			}
		}
	}

	static class Thread {
		public List<Element> callstack;
		public RTObject previousContentRTObject;
		public int threadIndex;

		public Thread() {
			callstack = new ArrayList<Element>();
		}

		@SuppressWarnings("unchecked")
		public Thread(HashMap<String, Object> jThreadObj, Story storyContext) throws Exception {
			this();
			threadIndex = (int) jThreadObj.get("threadIndex");

			List<Object> jThreadCallstack = (List<Object>) jThreadObj.get("callstack");

			for (Object jElTok : jThreadCallstack) {

				HashMap<String, Object> jElementObj = (HashMap<String, Object>) jElTok;

				PushPopType pushPopType = PushPopType.values()[(Integer) jElementObj.get("type")];

				Container currentContainer = null;
				int contentIndex = 0;

				String currentContainerPathStr = null;
				Object currentContainerPathStrToken = jElementObj.get("cPath");
				if (currentContainerPathStrToken != null) {
					currentContainerPathStr = currentContainerPathStrToken.toString();
					RTObject contentAtPath = storyContext.contentAtPath(new Path(currentContainerPathStr));
					currentContainer = contentAtPath instanceof Container ? (Container) contentAtPath : null;

					contentIndex = (int) jElementObj.get("idx");
				}

				boolean inExpressionEvaluation = (boolean) jElementObj.get("exp");

				Element el = new Element(pushPopType, currentContainer, contentIndex, inExpressionEvaluation);

				HashMap<String, Object> jObjTemps = (HashMap<String, Object>) jElementObj.get("temp");
				el.temporaryVariables = Json.jObjectToHashMapRuntimeObjs(jObjTemps);

				callstack.add(el);
			}

			Object prevContentObjPath = jThreadObj.get("previousContentRTObject");
			if (prevContentObjPath != null) {
				Path prevPath = new Path((String) prevContentObjPath);
				previousContentRTObject = storyContext.contentAtPath(prevPath);
			}
		}

		public Thread copy() {
			Thread copy = new Thread();
			copy.threadIndex = threadIndex;
			for (Element e : callstack) {
				copy.callstack.add(e.copy());
			}
			copy.previousContentRTObject = previousContentRTObject;
			return copy;
		}

		public HashMap<String, Object> jsonToken() throws Exception {
			HashMap<String, Object> threadJObj = new HashMap<String, Object>();

			List<Object> jThreadCallstack = new ArrayList<Object>();
			for (CallStack.Element el : callstack) {
				HashMap<String, Object> jObj = new HashMap<String, Object>();
				if (el.currentContainer != null) {
					jObj.put("cPath", el.currentContainer.getPath().getComponentsString());
					jObj.put("idx", el.currentContentIndex);
				}
				jObj.put("exp", el.inExpressionEvaluation);
				jObj.put("type", el.type.ordinal());
				jObj.put("temp", Json.hashMapRuntimeObjsToJObject(el.temporaryVariables));
				jThreadCallstack.add(jObj);
			}

			threadJObj.put("callstack", jThreadCallstack);
			threadJObj.put("threadIndex", threadIndex);

			if (previousContentRTObject != null)
				threadJObj.put("previousContentRTObject", previousContentRTObject.getPath().toString());

			return threadJObj;
		}
	}

	private int threadCounter;

	private List<Thread> threads;

	public CallStack(CallStack toCopy) {
		threads = new ArrayList<Thread>();
		for (Thread otherThread : toCopy.threads) {
			threads.add(otherThread.copy());
		}
	}

	public CallStack(Container rootContentContainer) {
		threads = new ArrayList<Thread>();
		threads.add(new Thread());

		threads.get(0).callstack.add(new Element(PushPopType.Tunnel, rootContentContainer, 0));
	}

	public boolean canPop() {
		return getCallStack().size() > 1;
	}

	public boolean canPop(PushPopType type) {

		if (!canPop())
			return false;

		if (type == null)
			return true;

		return currentElement().type == type;
	}

	public boolean canPopThread() {
		return threads.size() > 1;
	}

	// Find the most appropriate context for this variable.
	// Are we referencing a temporary or global variable?
	// Note that the compiler will have warned us about possible conflicts,
	// so anything that happens here should be safe!
	public int contextForVariableNamed(String name) {
		// Current temporary context?
		// (Shouldn't attempt to access contexts higher in the callstack.)
		if (currentElement().temporaryVariables.containsKey(name)) {
			return currentElementIndex() + 1;
		}

		// Global
		else {
			return 0;
		}
	}

	public Element currentElement() {
		return getCallStack().get(getCallStack().size() - 1);
	}

	public int currentElementIndex() {
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

	// See above for why we can't implement jsonToken
	public HashMap<String, Object> getJsonToken() throws Exception {

		HashMap<String, Object> jRTObject = new HashMap<String, Object>();

		ArrayList<Object> jThreads = new ArrayList<Object>();
		for (CallStack.Thread thread : threads) {
			jThreads.add(thread.jsonToken());
		}

		jRTObject.put("threads", jThreads);
		jRTObject.put("threadCounter", threadCounter);

		return jRTObject;
	}

	public RTObject getTemporaryVariableWithName(String name) {
		return getTemporaryVariableWithName(name, -1);
	}

	// Get variable value, dereferencing a variable pointer if necessary
	public RTObject getTemporaryVariableWithName(String name, int contextIndex) {
		if (contextIndex == -1)
			contextIndex = currentElementIndex() + 1;

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
		// When pushing to callstack, maintain the current content path, but
		// jump
		// out of expressions by default
		getCallStack()
				.add(new Element(type, currentElement().currentContainer, currentElement().currentContentIndex, false));
	}

	public void pushThread() {
		Thread newThread = getcurrentThread().copy();
		newThread.threadIndex = threadCounter;
		threadCounter++;
		threads.add(newThread);
	}

	public void setCurrentThread(Thread value) {
		// Debug.Assert (_threads.Count == 1, "Shouldn't be directly setting the
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
	}

	public void setTemporaryVariable(String name, RTObject value, boolean declareNew) {
		setTemporaryVariable(name, value, declareNew);
	}

	public void setTemporaryVariable(String name, RTObject value, boolean declareNew, int contextIndex)
			throws StoryException, Exception {
		if (contextIndex == -1)
			contextIndex = currentElementIndex() + 1;

		Element contextElement = getCallStack().get(contextIndex - 1);

		if (!declareNew && !contextElement.temporaryVariables.containsKey(name)) {
			throw new StoryException("Could not find temporary variable to set: " + name);
		}

		contextElement.temporaryVariables.put(name, value);
	}
	public Thread getThreadWithIndex(int index) {
		// return _threads.Find (t => t.threadIndex == index);

		for (Thread t : threads)
			if (t.threadIndex == index)
				return t;

		return null;
	}
}
