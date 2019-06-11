package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Encompasses all the global variables in an ink Story, and allows binding of a
 * VariableChanged event so that that game code can be notified whenever the
 * global variables change.
 */
public class VariablesState implements Iterable<String> {

	public static interface VariableChanged {
		void variableStateDidChangeEvent(String variableName, RTObject newValue) throws Exception;
	}

	private boolean batchObservingVariableChanges;

	// Used for accessing temporary variables
	private CallStack callStack;

	private HashSet<String> changedVariablesForBatchObs;

	private HashMap<String, RTObject> globalVariables;
	private HashMap<String, RTObject> defaultGlobalVariables;

	private VariableChanged variableChangedEvent;

	private ListDefinitionsOrigin listDefsOrigin;

	private StatePatch patch;

	VariablesState(CallStack callStack, ListDefinitionsOrigin listDefsOrigin) {
		globalVariables = new HashMap<>();
		this.callStack = callStack;

		this.listDefsOrigin = listDefsOrigin;
	}

	CallStack getCallStack() {
		return callStack;
	}

	void setCallStack(CallStack callStack) {
		this.callStack = callStack;
	}

	public void assign(VariableAssignment varAss, RTObject value) throws Exception {
		String name = varAss.getVariableName();
		int contextIndex = -1;
		// Are we assigning to a global variable?
		boolean setGlobal = false;
		if (varAss.isNewDeclaration()) {
			setGlobal = varAss.isGlobal();
		} else {
			setGlobal = globalVariableExistsWithName(name);
		}

		// Constructing new variable pointer reference
		if (varAss.isNewDeclaration()) {
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
					name = existingPointer.getVariableName();
					contextIndex = existingPointer.getContextIndex();
					setGlobal = (contextIndex == 0);
				}

			} while (existingPointer != null);
		}
		if (setGlobal) {
			setGlobal(name, value);
		} else {
			callStack.setTemporaryVariable(name, value, varAss.isNewDeclaration(), contextIndex);
		}
	}

	ListDefinitionsOrigin getLists() {
		return listDefsOrigin;
	}

	void applyPatch() {
		for (Entry<String, RTObject> namedVar : getPatch().getGlobals().entrySet()) {
			globalVariables.put(namedVar.getKey(), namedVar.getValue());
		}

		if (changedVariablesForBatchObs != null) {
			for (String name : getPatch().getChangedVariables())
				changedVariablesForBatchObs.add(name);
		}

		setPatch(null);
	}

	void setJsonToken(HashMap<String, Object> jToken) throws Exception {
		globalVariables.clear();

		for (Entry<String, RTObject> varVal : defaultGlobalVariables.entrySet()) {
			Object loadedToken = jToken.get(varVal.getKey());

			if (loadedToken != null) {
				globalVariables.put(varVal.getKey(), Json.jTokenToRuntimeObject(loadedToken));
			} else {
				globalVariables.put(varVal.getKey(), varVal.getValue());
			}
		}
	}

	/// <summary>
	/// When saving out JSON state, we can skip saving global values that
	/// remain equal to the initial values that were declared in ink.
	/// This makes the save file (potentially) much smaller assuming that
	/// at least a portion of the globals haven't changed. However, it
	/// can also take marginally longer to save in the case that the
	/// majority HAVE changed, since it has to compare all globals.
	/// It may also be useful to turn this off for testing worst case
	/// save timing.
	/// </summary>
	public static boolean dontSaveDefaultValues = true;

	void writeJson(SimpleJson.Writer writer) throws Exception {
		writer.writeObjectStart();
		for (Entry<String, RTObject> keyVal : globalVariables.entrySet()) {
			String name = keyVal.getKey();
			RTObject val = keyVal.getValue();

			if (dontSaveDefaultValues) {
				// Don't write out values that are the same as the default global values
				RTObject defaultVal = defaultGlobalVariables.get(name);
				if (defaultVal != null) {
					if (runtimeObjectsEqual(val, defaultVal))
						continue;
				}
			}

			writer.writePropertyStart(name);
			Json.writeRuntimeObject(writer, val);
			writer.writePropertyEnd();
		}
		writer.writeObjectEnd();
	}

	boolean runtimeObjectsEqual(RTObject obj1, RTObject obj2) throws Exception {
		if (obj1.getClass() != obj2.getClass())
			return false;

		// Other Value type (using proper Equals: list, string, divert path)
		if (obj1 instanceof Value && obj2 instanceof Value) {
			Value<?> val1 = (Value<?>) obj1;
			Value<?> val2 = (Value<?>) obj2;

			return val1.getValueObject().equals(val2.getValueObject());
		}

		throw new Exception("FastRoughDefinitelyEquals: Unsupported runtime object type: " + obj1.getClass());
	}

	RTObject tryGetDefaultVariableValue(String name) {
		RTObject val = defaultGlobalVariables.get(name);

		return val;
	}

	public Object get(String variableName) {
		RTObject varContents = (getPatch() != null ? getPatch().getGlobal(variableName) : null);

		if (varContents != null)
			return ((Value<?>) varContents).getValueObject();

		// Search main dictionary first.
		// If it's not found, it might be because the story content has changed,
		// and the original default value hasn't be instantiated.
		// Should really warn somehow, but it's difficult to see how...!
		if ((varContents = globalVariables.get(variableName)) != null) {
			return ((Value<?>) varContents).getValue();
		} else if ((varContents = defaultGlobalVariables.get(variableName)) != null) {
			return ((Value<?>) varContents).getValue();
		} else
			return null;
	}

	public boolean getbatchObservingVariableChanges() {
		return batchObservingVariableChanges;
	}

	// Make copy of the variable pointer so we're not using the value direct
	// from
	// the runtime. Temporary must be local to the current scope.
	// 0 if named variable is global
	// 1+ if named variable is a temporary in a particular call stack element
	int getContextIndexOfVariableNamed(String varName) {
		if (globalVariableExistsWithName(varName))
			return 0;

		return callStack.getCurrentElementIndex();
	}

	RTObject getRawVariableWithName(String name, int contextIndex) throws Exception {
		RTObject varValue = null;
		// 0 context = global
		if (contextIndex == 0 || contextIndex == -1) {
			varValue = globalVariables.get(name);
			if (varValue != null) {
				return varValue;
			}

			ListValue listItemValue = listDefsOrigin.findSingleItemListWithName(name);
			if (listItemValue != null)
				return listItemValue;
		}

		// Temporary
		varValue = callStack.getTemporaryVariableWithName(name, contextIndex);

		return varValue;
	}

	void snapshotDefaultGlobals() {
		defaultGlobalVariables = new HashMap<>(globalVariables);
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

	/**
	 * Enumerator to allow iteration over all global variables by name.
	 */
	@Override
	public Iterator<String> iterator() {
		return globalVariables.keySet().iterator();
	}

	// Given a variable pointer with just the name of the target known, resolve
	// to a variable
	// pointer that more specifically points to the exact instance: whether it's
	// global,
	// or the exact position of a temporary on the callstack.
	VariablePointerValue resolveVariablePointer(VariablePointerValue varPointer) throws Exception {
		int contextIndex = varPointer.getContextIndex();
		if (contextIndex == -1)
			contextIndex = getContextIndexOfVariableNamed(varPointer.getVariableName());

		RTObject valueOfVariablePointedTo = getRawVariableWithName(varPointer.getVariableName(), contextIndex);
		// Extra layer of indirection:
		// When accessing a pointer to a pointer (e.g. when calling nested or
		// recursive functions that take a variable references, ensure we don't
		// create
		// a chain of indirection by just returning the final target.
		VariablePointerValue doubleRedirectionPointer = valueOfVariablePointedTo instanceof VariablePointerValue
				? (VariablePointerValue) valueOfVariablePointedTo
				: (VariablePointerValue) null;
		if (doubleRedirectionPointer != null) {
			return doubleRedirectionPointer;
		} else {
			return new VariablePointerValue(varPointer.getVariableName(), contextIndex);
		}
	}

	public void set(String variableName, Object value) throws Exception {

		// This is the main
		if (!defaultGlobalVariables.containsKey(variableName)) {
			throw new StoryException(
					"Cannot assign to a variable (" + variableName + ") that hasn't been declared in the story");
		}

		AbstractValue val = AbstractValue.create(value);
		if (val == null) {
			if (value == null) {
				throw new StoryException("Cannot pass null to VariableState");
			} else {
				throw new StoryException("Invalid value passed to VariableState: " + value.toString());
			}
		}

		setGlobal(variableName, val);
	}

	public void setbatchObservingVariableChanges(boolean value) throws Exception {
		batchObservingVariableChanges = value;
		if (value) {
			changedVariablesForBatchObs = new HashSet<>();
		} else {
			if (changedVariablesForBatchObs != null) {
				for (String variableName : changedVariablesForBatchObs) {
					RTObject currentValue = globalVariables.get(variableName);
					getVariableChangedEvent().variableStateDidChangeEvent(variableName, currentValue);
				}
			}

			changedVariablesForBatchObs = null;
		}
	}

	void retainListOriginsForAssignment(RTObject oldValue, RTObject newValue) {
		ListValue oldList = null;

		if (oldValue instanceof ListValue)
			oldList = (ListValue) oldValue;

		ListValue newList = null;

		if (newValue instanceof ListValue)
			newList = (ListValue) newValue;

		if (oldList != null && newList != null && newList.value.size() == 0)
			newList.value.setInitialOriginNames(oldList.value.getOriginNames());
	}

	void setGlobal(String variableName, RTObject value) throws Exception {
		RTObject oldValue = globalVariables.get(variableName);

		ListValue.retainListOriginsForAssignment(oldValue, value);

		globalVariables.put(variableName, value);

		if (getVariableChangedEvent() != null && !value.equals(oldValue)) {

			if (getbatchObservingVariableChanges()) {
				changedVariablesForBatchObs.add(variableName);
			} else {
				getVariableChangedEvent().variableStateDidChangeEvent(variableName, value);
			}
		}

	}

	public void setjsonToken(HashMap<String, Object> value) throws Exception {
		globalVariables = Json.jObjectToHashMapRuntimeObjs(value);
	}

	public RTObject valueAtVariablePointer(VariablePointerValue pointer) throws Exception {
		return getVariableWithName(pointer.getVariableName(), pointer.getContextIndex());
	}

	public VariableChanged getVariableChangedEvent() {
		return variableChangedEvent;
	}

	public void setVariableChangedEvent(VariableChanged variableChangedEvent) {
		this.variableChangedEvent = variableChangedEvent;
	}

	boolean globalVariableExistsWithName(String name) {
		return globalVariables.containsKey(name)
				|| (defaultGlobalVariables != null && defaultGlobalVariables.containsKey(name));
	}

	public StatePatch getPatch() {
		return patch;
	}

	public void setPatch(StatePatch patch) {
		this.patch = patch;
	}
}
