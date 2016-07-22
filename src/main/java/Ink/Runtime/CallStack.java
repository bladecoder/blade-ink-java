package Ink.Runtime;

class CallStack {
//	class Element {
//		public Container currentContainer;
//		public int currentContentIndex;
//
//		public boolean inExpressionEvaluation;
//		public HashMap<string, Runtime.RTObject> temporaryVariables;
//		public PushPopType type;
//
//		public Runtime.RTObject currentRTObject
//		{
//            get {
//                if (currentContainer && currentContentIndex < currentContainer.content.Count) {
//                    return currentContainer.content [currentContentIndex];
//                }
//
//                return null;
//            }
//            set {
//                var currentObj = value;
//                if (currentObj == null) {
//                    currentContainer = null;
//                    currentContentIndex = 0;
//                    return;
//                }
//
//                currentContainer = currentObj.parent as Container;
//                if (currentContainer != null)
//                    currentContentIndex = currentContainer.content.IndexOf (currentObj);
//
//                // Two reasons why the above operation might not work:
//                //  - currentObj is already the root container
//                //  - currentObj is a named container rather than being an RTObject at an index
//                if (currentContainer == null || currentContentIndex == -1) {
//                    currentContainer = currentObj as Container;
//                    currentContentIndex = 0;
//                }
//            }
//        }
//
//		public Element(PushPopType type, Container container, int contentIndex, boolean inExpressionEvaluation = false) {
//            this.currentContainer = container;
//            this.currentContentIndex = contentIndex;
//            this.inExpressionEvaluation = inExpressionEvaluation;
//            this.temporaryVariables = new HashMap<string, RTObject>();
//            this.type = type;
//        }
//
//		public Element Copy() {
//			var copy = new Element(this.type, this.currentContainer, this.currentContentIndex,
//					this.inExpressionEvaluation);
//			copy.temporaryVariables = this.temporaryVariables;
//			return copy;
//		}
//	}
//
//	internal
//
	class Thread {
//		public List<Element> callstack;
//		public int threadIndex;
//		public Runtime.RTObject previousContentRTObject;
//
//		public Thread() {
//			callstack = new List<Element>();
//		}
//
//		public Thread(HashMap<string, RTObject> jThreadObj, Story storyContext) : this() {
//            threadIndex = (int) jThreadObj ["threadIndex"];
//
//			List<RTObject> jThreadCallstack = (List<RTObject>) jThreadObj ["callstack"];
//
//		foreach (RTObject jElTok in jThreadCallstack) {
//
//				var jElementObj = (HashMap<string, RTObject>)jElTok;
//
//                PushPopType pushPopType = (PushPopType)(int)jElementObj ["type"];
//
//				Container currentContainer = null;
//				int contentIndex = 0;
//
//				string currentContainerPathStr = null;
//				RTObject currentContainerPathStrToken;
//				if (jElementObj.TryGetValue ("cPath", out currentContainerPathStrToken)) {
//					currentContainerPathStr = currentContainerPathStrToken.ToString ();
//					currentContainer = storyContext.ContentAtPath (new Path(currentContainerPathStr)) as Container;
//                    contentIndex = (int) jElementObj ["idx"];
//				}
//
//                boolean inExpressionEvaluation = (boolean)jElementObj ["exp"];
//
//				var el = new Element (pushPopType, currentContainer, contentIndex, inExpressionEvaluation);
//
//				var jObjTemps = (HashMap<string, RTObject>) jElementObj ["temp"];
//				el.temporaryVariables = Json.JRTObjectToHashMapRuntimeObjs (jObjTemps);
//
//				callstack.Add (el);
//			}
//
//			RTObject prevContentObjPath;
//			if( jThreadObj.TryGetValue("previousContentRTObject", out prevContentObjPath) ) {
//				var prevPath = new Path((string)prevContentObjPath);
//                previousContentRTObject = storyContext.ContentAtPath(prevPath);
//            }
//		}
//
//		public Thread Copy() {
//            var copy = new Thread ();
//            copy.threadIndex = threadIndex;
//            foreach(var e in callstack) {
//                copy.callstack.Add(e.Copy());
//            }
//            copy.previousContentRTObject = previousContentRTObject;
//            return copy;
//        }
//
//		public HashMap<string, RTObject> jsonToken
//		{
//			get {
//				var threadJObj = new HashMap<string, RTObject> ();
//
//				var jThreadCallstack = new List<RTObject> ();
//				foreach (CallStack.Element el in callstack) {
//					var jObj = new HashMap<string, RTObject> ();
//					if (el.currentContainer) {
//						jObj ["cPath"] = el.currentContainer.path.componentsString;
//						jObj ["idx"] = el.currentContentIndex;
//					}
//					jObj ["exp"] = el.inExpressionEvaluation;
//					jObj ["type"] = (int) el.type;
//					jObj ["temp"] = Json.HashMapRuntimeObjsToJRTObject (el.temporaryVariables);
//					jThreadCallstack.Add (jObj);
//				}
//
//				threadJObj ["callstack"] = jThreadCallstack;
//				threadJObj ["threadIndex"] = threadIndex;
//
//                if (previousContentRTObject != null)
//                    threadJObj ["previousContentRTObject"] = previousContentRTObject.path.ToString();
//
//				return threadJObj;
//			}
//		}
	}
//
//	public List<Element> elements
//	{
//        get {
//            return callStack;
//        }
//    }
//
//	public Element currentElement
//	{ 
//        get { 
//            return callStack [callStack.Count - 1];
//        } 
//    }
//
//	public int currentElementIndex
//	{
//        get {
//            return callStack.Count - 1;
//        }
//    }
//
//	public Thread currentThread
//	{
//        get {
//            return _threads [_threads.Count - 1];
//        }
//        set {
//            Debug.Assert (_threads.Count == 1, "Shouldn't be directly setting the current thread when we have a stack of them");
//            _threads.Clear ();
//            _threads.Add (value);
//        }
//    }
//
//	public boolean canPop
//	{
//        get {
//            return callStack.Count > 1;
//        }
//    }
//
//	public CallStack(Container rootContentContainer) {
//		_threads = new List<Thread>();
//		_threads.Add(new Thread());
//
//		_threads[0].callstack.Add(new Element(PushPopType.Tunnel, rootContentContainer, 0));
//	}
//
//	public CallStack(CallStack toCopy)
//    {
//        _threads = new List<Thread> ();
//        foreach (var otherThread in toCopy._threads) {
//            _threads.Add (otherThread.Copy ());
//        }
//    }
//
//	// Unfortunately it's not possible to implement jsonToken since
//	// the setter needs to take a Story as a context in order to
//	// look up RTObjects from paths for currentContainer within elements.
//	public void SetJsonToken(HashMap<string, RTObject> jRTObject, Story storyContext)
//    {
//        _threads.Clear ();
//
//        var jThreads = (List<RTObject>) jRTObject ["threads"];
//
//        foreach (RTObject jThreadTok in jThreads) {
//            var jThreadObj = (HashMap<string, RTObject>)jThreadTok;
//            var thread = new Thread (jThreadObj, storyContext);
//            _threads.Add (thread);
//        }
//
//        _threadCounter = (int)jRTObject ["threadCounter"];
//    }
//
//	// See above for why we can't implement jsonToken
//	public HashMap<string, RTObject> GetJsonToken() {
//
//        var jRTObject = new HashMap<string, RTObject> ();
//
//        var jThreads = new List<RTObject> ();
//        foreach (CallStack.Thread thread in _threads) {
//			jThreads.Add (thread.jsonToken);
//        }
//
//        jRTObject ["threads"] = jThreads;
//        jRTObject ["threadCounter"] = _threadCounter;
//
//        return jRTObject;
//    }
//
//	public void PushThread() {
//		var newThread = currentThread.Copy();
//		newThread.threadIndex = _threadCounter;
//		_threadCounter++;
//		_threads.Add(newThread);
//	}
//
//	public void PopThread() {
//		if (canPopThread) {
//			_threads.Remove(currentThread);
//		} else {
//			Debug.Fail("Can't pop thread");
//		}
//	}
//
//	public boolean canPopThread
//	{
//        get {
//            return _threads.Count > 1;
//        }
//    }
//
//	public void Push(PushPopType type)
//    {
//        // When pushing to callstack, maintain the current content path, but jump out of expressions by default
//        callStack.Add (new Element(type, currentElement.currentContainer, currentElement.currentContentIndex, inExpressionEvaluation: false));
//    }
//
//	public boolean CanPop(PushPopType? type = null) {
//
//        if (!canPop)
//            return false;
//        
//        if (type == null)
//            return true;
//        
//        return currentElement.type == type;
//    }
//
//	public void Pop(PushPopType? type = null)
//    {
//        if (CanPop (type)) {
//            callStack.RemoveAt (callStack.Count - 1);
//            return;
//        } else {
//            Debug.Fail ("Mismatched push/pop in Callstack");
//        }
//    }
//
//	// Get variable value, dereferencing a variable pointer if necessary
//	public Runtime.RTObject GetTemporaryVariableWithName(string name, int contextIndex = -1)
//    {
//        if (contextIndex == -1)
//            contextIndex = currentElementIndex+1;
//        
//        Runtime.RTObject varValue = null;
//
//        var contextElement = callStack [contextIndex-1];
//
//        if (contextElement.temporaryVariables.TryGetValue (name, out varValue)) {
//            return varValue;
//        } else {
//            return null;
//        }
//    }
//
//	public void SetTemporaryVariable(string name, Runtime.RTObject value, boolean declareNew, int contextIndex = -1)
//    {
//        if (contextIndex == -1)
//            contextIndex = currentElementIndex+1;
//
//        var contextElement = callStack [contextIndex-1];
//        
//        if (!declareNew && !contextElement.temporaryVariables.ContainsKey(name)) {
//            throw new StoryException ("Could not find temporary variable to set: " + name);
//        }
//
//        contextElement.temporaryVariables [name] = value;
//    }
//
//	// Find the most appropriate context for this variable.
//	// Are we referencing a temporary or global variable?
//	// Note that the compiler will have warned us about possible conflicts,
//	// so anything that happens here should be safe!
//	public int ContextForVariableNamed(string name) {
//		// Current temporary context?
//		// (Shouldn't attempt to access contexts higher in the callstack.)
//		if (currentElement.temporaryVariables.ContainsKey(name)) {
//			return currentElementIndex + 1;
//		}
//
//		// Global
//		else {
//			return 0;
//		}
//	}
//
//	public Thread ThreadWithIndex(int index)
//    {
//        return _threads.Find (t => t.threadIndex == index);
//    }
//
//	private List<Element> callStack
//	{
//        get {
//            return currentThread.callstack;
//        }
//    }
//
//	private List<Thread> _threads;
//	public int _threadCounter;
}
