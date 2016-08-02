package Ink.Runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encompasses all the global variables in an ink Story, and allows binding of a
 * VariableChanged event so that that game code can be notified whenever the
 * global variables change.
 */
public class VariablesState implements Iterable<String> {

	// TODO
	// public static class __MultiVariableChanged implements VariableChanged
	// {
	// public void invoke(String variableName, RTObject newValue) throws
	// Exception {
	// IList<VariableChanged> copy = new IList<VariableChanged>(), members =
	// this.getInvocationList();
	// synchronized (members)
	// {
	// copy = new LinkedList<VariableChanged>(members);
	// }
	// for (RTObject __dummyForeachVar0 : copy)
	// {
	// VariableChanged d = (VariableChanged)__dummyForeachVar0;
	// d.invoke(variableName, newValue);
	// }
	// }
	//
	// private System.Collections.Generic.IList<VariableChanged> _invocationList
	// = new ArrayList<VariableChanged>();
	// public static VariableChanged combine(VariableChanged a, VariableChanged
	// b) throws Exception {
	// if (a == null)
	// return b;
	//
	// if (b == null)
	// return a;
	//
	// __MultiVariableChanged ret = new __MultiVariableChanged();
	// ret._invocationList = a.getInvocationList();
	// // Finished observing variables in a batch - now send
	// // notifications for changed variables all in one go.
	// ret._invocationList.addAll(b.getInvocationList());
	// return ret;
	// }
	//
	// public static VariableChanged remove(VariableChanged a, VariableChanged
	// b) throws Exception {
	// if (a == null || b == null)
	// return a;
	//
	// /**
	// * Get or set the value of a named global ink variable.
	// * The types available are the standard ink types. Certain
	// * types will be implicitly casted when setting.
	// * For example, doubles to floats, longs to ints, and bools
	// * to ints.
	// */
	// System.Collections.Generic.IList<VariableChanged> aInvList =
	// a.getInvocationList();
	// System.Collections.Generic.IList<VariableChanged> newInvList =
	// ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
	// if (aInvList == newInvList)
	// {
	// return a;
	// }
	// else
	// {
	// __MultiVariableChanged ret = new __MultiVariableChanged();
	// ret._invocationList = newInvList;
	// return ret;
	// }
	// }
	//
	// public System.Collections.Generic.IList<VariableChanged>
	// getInvocationList() throws Exception {
	// return _invocationList;
	// }
	//
	// }
	//
	// public static interface VariableChanged
	// {
	// void invoke(String variableName, RTObject newValue) throws Exception ;
	//
	// System.Collections.Generic.IList<VariableChanged> getInvocationList()
	// throws Exception ;
	//
	// }
	//
	// public VariableChanged variableChangedEvent;

	public boolean getbatchObservingVariableChanges() throws Exception {
		return _batchObservingVariableChanges;
	}

	public void setbatchObservingVariableChanges(boolean value) throws Exception {
		_batchObservingVariableChanges = value;
		if (value) {
			_changedVariables = new HashSet<String>();
		} else {
			if (_changedVariables != null) {
				for (String variableName : _changedVariables) {
					Object currentValue = _globalVariables.get(variableName);
					// TODO
					// variableChangedEvent(variableName, currentValue);
				}
			}

			_changedVariables = null;
		}
	}

	boolean _batchObservingVariableChanges;

	public Object get(String variableName) throws Exception {
		RTObject varContents = _globalVariables.get(variableName);
		if (varContents != null) {
			return ((Value<?>) varContents).getValue();
		} else
			return null;
	}

	public void set(String variableName, Object value) throws Exception {
		AbstractValue val = Value.create(value);
		if (val == null) {
			if (value == null) {
				throw new StoryException("Cannot pass null to VariableState");
			} else {
				throw new StoryException("Invalid value passed to VariableState: " + value.toString());
			}
		}

		setGlobal(variableName, val);
	}

	/**
	 * Enumerator to allow iteration over all global variables by name.
	 */
	public Iterator<String> iterator() {
		return _globalVariables.keySet().iterator();
	}

	public VariablesState(CallStack callStack) throws Exception {
		_globalVariables = new HashMap<String, RTObject>();
		_callStack = callStack;
	}

	public void copyFrom(VariablesState varState) throws Exception {
		_globalVariables = new HashMap<String, RTObject>(varState._globalVariables);
		// TODO
		// variableChangedEvent = varState.variableChangedEvent;

		if (varState.getbatchObservingVariableChanges() != getbatchObservingVariableChanges()) {
			if (varState.getbatchObservingVariableChanges()) {
				_batchObservingVariableChanges = true;
				_changedVariables = new HashSet<String>(varState._changedVariables);
			} else {
				_batchObservingVariableChanges = false;
				_changedVariables = null;
			}
		}

	}

	public HashMap<String, Object> getjsonToken() throws Exception {
		return Json.HashMapRuntimeObjsToJRTObject(_globalVariables);
	}

	public void setjsonToken(HashMap<String, Object> value) throws Exception {
		_globalVariables = Json.jRTObjectToHashMapRuntimeObjs(value);
	}

	public RTObject getVariableWithName(String name) throws Exception {
		return getVariableWithName(name, -1);
	}

	RTObject getVariableWithName(String name, int contextIndex) throws Exception {
		RTObject varValue = getRawVariableWithName(name, contextIndex);
		// Get value from pointer?
		VariablePointerValue varPointer = varValue instanceof VariablePointerValue ? (VariablePointerValue) varValue
				: (VariablePointerValue) null;
		if (varPointer != null) {
			varValue = valueAtVariablePointer(varPointer);
		}

		return varValue;
	}

	RTObject getRawVariableWithName(String name, int contextIndex) throws Exception {
		RTObject varValue = null;
		// 0 context = global
		if (contextIndex == 0 || contextIndex == -1) {
			varValue = _globalVariables.get(name);
			if (varValue != null) {
				return varValue;
			}

		}

		// Temporary
		varValue = _callStack.GetTemporaryVariableWithName(name, contextIndex);
		if (varValue == null)
			throw new Exception("RUNTIME ERROR: Variable '" + name + "' could not be found in context '" + contextIndex
					+ "'. This shouldn't be possible so is a bug in the ink engine. Please try to construct a minimal story that reproduces the problem and report to inkle, thank you!");

		return varValue;
	}

	public RTObject valueAtVariablePointer(VariablePointerValue pointer) throws Exception {
		return getVariableWithName(pointer.getvariableName(), pointer.getcontextIndex());
	}

	public void assign(VariableAssignment varAss, RTObject value) throws Exception {
		String name = varAss.getvariableName();
		int contextIndex = -1;
		// Are we assigning to a global variable?
		boolean setGlobal = false;
		if (varAss.getisNewDeclaration()) {
			setGlobal = varAss.getisGlobal();
		} else {
			setGlobal = _globalVariables.containsKey(name);
		}
		// Constructing new variable pointer reference
		if (varAss.getisNewDeclaration()) {
			VariablePointerValue varPointer = value instanceof VariablePointerValue ? (VariablePointerValue) value
					: (VariablePointerValue) null;
			if (varPointer != null) {
				VariablePointerValue fullyResolvedVariablePointer = resolveVariablePointer(varPointer);
				value = fullyResolvedVariablePointer;
			}

		} else {
			// Assign to existing variable pointer?
			// Then assign to the variable that the pointer is pointing to by
			// name.
			// De-reference variable reference to point to
			VariablePointerValue existingPointer = null;
			do {
				existingPointer = getRawVariableWithName(name, contextIndex) instanceof VariablePointerValue
						? (VariablePointerValue) getRawVariableWithName(name, contextIndex)
						: (VariablePointerValue) null;
				if (existingPointer != null) {
					name = existingPointer.getvariableName();
					contextIndex = existingPointer.getcontextIndex();
					setGlobal = (contextIndex == 0);
				}

			} while (existingPointer != null);
		}
		if (setGlobal) {
			setGlobal(name, value);
		} else {
			_callStack.SetTemporaryVariable(name, value, varAss.getisNewDeclaration(), contextIndex);
		}
	}

	void setGlobal(String variableName, RTObject value) throws Exception {
		RTObject oldValue = oldValue = _globalVariables.get(variableName);
		_globalVariables.put(variableName, value);

		// TODO
		// if (variableChangedEvent != null && !value.equals(oldValue)) {
		// if (getbatchObservingVariableChanges()) {
		// _changedVariables.Add(variableName);
		// } else {
		// variableChangedEvent(variableName, value);
		// }
		// }

	}

	// Given a variable pointer with just the name of the target known, resolve
	// to a variable
	// pointer that more specifically points to the exact instance: whether it's
	// global,
	// or the exact position of a temporary on the callstack.
	VariablePointerValue resolveVariablePointer(VariablePointerValue varPointer) throws Exception {
		int contextIndex = varPointer.getcontextIndex();
		if (contextIndex == -1)
			contextIndex = getContextIndexOfVariableNamed(varPointer.getvariableName());

		RTObject valueOfVariablePointedTo = getRawVariableWithName(varPointer.getvariableName(), contextIndex);
		// Extra layer of indirection:
		// When accessing a pointer to a pointer (e.g. when calling nested or
		// recursive functions that take a variable references, ensure we don't
		// create
		// a chain of indirection by just returning the final target.
		VariablePointerValue doubleRedirectionPointer = valueOfVariablePointedTo instanceof VariablePointerValue
				? (VariablePointerValue) valueOfVariablePointedTo : (VariablePointerValue) null;
		if (doubleRedirectionPointer != null) {
			return doubleRedirectionPointer;
		} else {
			return new VariablePointerValue(varPointer.getvariableName(), contextIndex);
		}
	}

	// Make copy of the variable pointer so we're not using the value direct
	// from
	// the runtime. Temporary must be local to the current scope.
	// 0 if named variable is global
	// 1+ if named variable is a temporary in a particular call stack element
	int getContextIndexOfVariableNamed(String varName) throws Exception {
		if (_globalVariables.containsKey(varName))
			return 0;

		return _callStack.currentElementIndex();
	}

	HashMap<String, RTObject> _globalVariables;
	// Used for accessing temporary variables
	CallStack _callStack;
	HashSet<String> _changedVariables;
}
