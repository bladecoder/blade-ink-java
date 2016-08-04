package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.print.DocFlavor.STRING;

class CallStack {
	static class Element {
		public Container currentContainer;
		public int currentContentIndex;

		public boolean inExpressionEvaluation;
		public HashMap<String, RTObject> temporaryVariables;
		public PushPopType type;

		public RTObject currentRTObject;

		public RTObject getCurrentRTObject() throws Exception {
			if (currentContainer != null && currentContentIndex < currentContainer.getContent().size()) {
				return currentContainer.getContent().get(currentContentIndex);
			}

			return null;
		}

		public void setcurrentRTObject(RTObject currentObj) throws Exception {
			if (currentObj == null) {
				currentContainer = null;
				currentContentIndex = 0;
				return;
			}

			currentContainer = 
					currentObj.getParent() instanceof Container?(Container)currentObj.getParent():null;
			
			
			if (currentContainer != null)
				currentContentIndex = currentContainer.getContent().indexOf(currentObj);

			// Two reasons why the above operation might not work:
			// - currentObj is already the root container
			// - currentObj is a named container rather than being an RTObject
			// at an index
			if (currentContainer == null || currentContentIndex == -1) {
				currentContainer = 
						currentObj instanceof Container?(Container)currentObj:null;
				currentContentIndex = 0;
			}
		}

		public Element(PushPopType type, Container container, int contentIndex, boolean inExpressionEvaluation) {
			this.currentContainer = container;
			this.currentContentIndex = contentIndex;
			this.inExpressionEvaluation = inExpressionEvaluation;
			this.temporaryVariables = new HashMap<String, RTObject>();
			this.type = type;
		}

		public Element(PushPopType type, Container container, int contentIndex) {
			this(type, container, contentIndex, false);
		}

		public Element Copy() {
			Element copy = new Element(this.type, this.currentContainer, this.currentContentIndex,
					this.inExpressionEvaluation);
			copy.temporaryVariables = this.temporaryVariables;
			return copy;
		}
	}

	static class Thread {
		public List<Element> callstack;
		public int threadIndex;
		public RTObject previousContentRTObject;

		public Thread() {
			callstack = new ArrayList<Element>();
		}

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
					RTObject contentAtPath = storyContext.ContentAtPath(new Path(currentContainerPathStr));
					currentContainer = 
							contentAtPath instanceof Container?(Container)contentAtPath:null;

					contentIndex = (int) jElementObj.get("idx");
				}

				boolean inExpressionEvaluation = (boolean) jElementObj.get("exp");

				Element el = new Element(pushPopType, currentContainer, contentIndex, inExpressionEvaluation);

				HashMap<String, Object> jObjTemps = (HashMap<String, Object>) jElementObj.get("temp");
				el.temporaryVariables = Json.jRTObjectToHashMapRuntimeObjs(jObjTemps);

				callstack.add(el);
			}

			Object prevContentObjPath = jThreadObj.get("previousContentRTObject");
			if (prevContentObjPath != null) {
				Path prevPath = new Path((String) prevContentObjPath);
				previousContentRTObject = storyContext.ContentAtPath(prevPath);
			}
		}

		public Thread Copy() {
			Thread copy = new Thread();
			copy.threadIndex = threadIndex;
			for (Element e : callstack) {
				copy.callstack.add(e.Copy());
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
				jObj.put("temp", Json.HashMapRuntimeObjsToJRTObject(el.temporaryVariables));
				jThreadCallstack.add(jObj);
			}

			threadJObj.put("callstack", jThreadCallstack);
			threadJObj.put("threadIndex", threadIndex);

			if (previousContentRTObject != null)
				threadJObj.put("previousContentRTObject", previousContentRTObject.getPath().toString());

			return threadJObj;
		}
	}

	//
	public List<Element> getElements() {
		return getCallStack();
	}

	public Element currentElement() {
		return getCallStack().get(getCallStack().size() - 1);
	}

	public int currentElementIndex() {
		return getCallStack().size() - 1;
	}

	public Thread getcurrentThread() {
		return _threads.get(_threads.size() - 1);
	}

	public void setCurrentThread(Thread value) {
		// Debug.Assert (_threads.Count == 1, "Shouldn't be directly setting the
		// current thread when we have a stack of them");
		_threads.clear();
		_threads.add(value);
	}

	public boolean canPop() {
		return getCallStack().size() > 1;
	}

	public CallStack(Container rootContentContainer) {
		_threads = new ArrayList<Thread>();
		_threads.add(new Thread());

		_threads.get(0).callstack.add(new Element(PushPopType.Tunnel, rootContentContainer, 0));
	}

	public CallStack(CallStack toCopy) {
		_threads = new ArrayList<Thread>();
		for (Thread otherThread : toCopy._threads) {
			_threads.add(otherThread.Copy());
		}
	}

	// Unfortunately it's not possible to implement jsonToken since
	// the setter needs to take a Story as a context in order to
	// look up RTObjects from paths for currentContainer within elements.
	public void SetJsonToken(HashMap<String, Object> jRTObject, Story storyContext) throws Exception {
		_threads.clear();

		List<Object> jThreads = (List<Object>) jRTObject.get("threads");

		for (Object jThreadTok : jThreads) {
			HashMap<String, Object> jThreadObj = (HashMap<String, Object>) jThreadTok;
			Thread thread = new Thread(jThreadObj, storyContext);
			_threads.add(thread);
		}

		_threadCounter = (int) jRTObject.get("threadCounter");
	}

	// See above for why we can't implement jsonToken
	public HashMap<String, Object> GetJsonToken() throws Exception {

		HashMap<String, Object> jRTObject = new HashMap<String, Object>();

		ArrayList<Object> jThreads = new ArrayList<Object>();
		for (CallStack.Thread thread : _threads) {
			jThreads.add(thread.jsonToken());
		}

		jRTObject.put("threads", jThreads);
		jRTObject.put("threadCounter", _threadCounter);

		return jRTObject;
	}

	public void PushThread() {
		Thread newThread = getcurrentThread().Copy();
		newThread.threadIndex = _threadCounter;
		_threadCounter++;
		_threads.add(newThread);
	}

	public void PopThread() {
		if (canPopThread()) {
			_threads.remove(getcurrentThread());
		} else {
			// Debug.Fail("Can't pop thread");
		}
	}

	public boolean canPopThread() {
		return _threads.size() > 1;
	}

	public void Push(PushPopType type) {
		// When pushing to callstack, maintain the current content path, but
		// jump
		// out of expressions by default
		getCallStack()
				.add(new Element(type, currentElement().currentContainer, currentElement().currentContentIndex, false));
	}

	public boolean CanPop(PushPopType type) {

		if (!canPop())
			return false;

		if (type == null)
			return true;

		return currentElement().type == type;
	}
	
	public void Pop() {
		Pop(null);
	}

	public void Pop(PushPopType type) {
		if (CanPop(type)) {
			getCallStack().remove(getCallStack().size() - 1);
			return;
		} else {
			// Debug.Fail ("Mismatched push/pop in Callstack");
		}
	}

	// Get variable value, dereferencing a variable pointer if necessary
	public RTObject GetTemporaryVariableWithName(String name, int contextIndex) {
		if (contextIndex == -1)
			contextIndex = currentElementIndex() + 1;

		Element contextElement = getCallStack().get(contextIndex - 1);
		RTObject varValue = contextElement.temporaryVariables.get(name);

		return varValue;
	}

	public RTObject GetTemporaryVariableWithName(String name) {
		return GetTemporaryVariableWithName(name, -1);
	}

	public void SetTemporaryVariable(String name, RTObject value, boolean declareNew) {
		SetTemporaryVariable(name, value, declareNew);
	}

	public void SetTemporaryVariable(String name, RTObject value, boolean declareNew, int contextIndex) throws StoryException, Exception {
		if (contextIndex == -1)
			contextIndex = currentElementIndex() + 1;

		Element contextElement = getCallStack().get(contextIndex - 1);

		if (!declareNew && !contextElement.temporaryVariables.containsKey(name)) {
			throw new StoryException("Could not find temporary variable to set: " + name);
		}

		contextElement.temporaryVariables.put(name, value);
	}

	// Find the most appropriate context for this variable.
	// Are we referencing a temporary or global variable?
	// Note that the compiler will have warned us about possible conflicts,
	// so anything that happens here should be safe!
	public int ContextForVariableNamed(String name) {
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

	public Thread ThreadWithIndex(int index) {
		// return _threads.Find (t => t.threadIndex == index);

		for (Thread t : _threads)
			if (t.threadIndex == index)
				return t;

		return null;
	}

	private List<Element> getCallStack() {
		return getcurrentThread().callstack;
	}

	private List<Thread> _threads;
	public int _threadCounter;
}
