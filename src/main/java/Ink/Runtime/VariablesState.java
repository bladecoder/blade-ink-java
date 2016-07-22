//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;
import CS2JNet.JavaSupport.util.ListSupport;
import Ink.Runtime.CallStack;
import Ink.Runtime.Json;
import Ink.Runtime.RTObject;
import Ink.Runtime.StoryException;
import Ink.Runtime.Value;
import Ink.Runtime.VariableAssignment;
import Ink.Runtime.VariablePointerValue;
import Ink.Runtime.VariablesState;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
* Encompasses all the global variables in an ink Story, and
* allows binding of a VariableChanged event so that that game
* code can be notified whenever the global variables change.
*/
public class VariablesState  extends IEnumerable<String> 
{
    public static class __MultiVariableChanged   implements VariableChanged
    {
        public void invoke(String variableName, RTObject newValue) throws Exception {
            IList<VariableChanged> copy = new IList<VariableChanged>(), members = this.getInvocationList();
            synchronized (members)
            {
                copy = new LinkedList<VariableChanged>(members);
            }
            for (RTObject __dummyForeachVar0 : copy)
            {
                VariableChanged d = (VariableChanged)__dummyForeachVar0;
                d.invoke(variableName, newValue);
            }
        }

        private System.Collections.Generic.IList<VariableChanged> _invocationList = new ArrayList<VariableChanged>();
        public static VariableChanged combine(VariableChanged a, VariableChanged b) throws Exception {
            if (a == null)
                return b;
             
            if (b == null)
                return a;
             
            __MultiVariableChanged ret = new __MultiVariableChanged();
            ret._invocationList = a.getInvocationList();
            // Finished observing variables in a batch - now send
            // notifications for changed variables all in one go.
            ret._invocationList.addAll(b.getInvocationList());
            return ret;
        }

        public static VariableChanged remove(VariableChanged a, VariableChanged b) throws Exception {
            if (a == null || b == null)
                return a;
             
            /**
            * Get or set the value of a named global ink variable.
            * The types available are the standard ink types. Certain
            * types will be implicitly casted when setting.
            * For example, doubles to floats, longs to ints, and bools
            * to ints.
            */
            System.Collections.Generic.IList<VariableChanged> aInvList = a.getInvocationList();
            System.Collections.Generic.IList<VariableChanged> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
            if (aInvList == newInvList)
            {
                return a;
            }
            else
            {
                __MultiVariableChanged ret = new __MultiVariableChanged();
                ret._invocationList = newInvList;
                return ret;
            } 
        }

        public System.Collections.Generic.IList<VariableChanged> getInvocationList() throws Exception {
            return _invocationList;
        }
    
    }

    public static interface VariableChanged   
    {
        void invoke(String variableName, RTObject newValue) throws Exception ;

        System.Collections.Generic.IList<VariableChanged> getInvocationList() throws Exception ;
    
    }

    public VariableChanged variableChangedEvent;
    public boolean getbatchObservingVariableChanges() throws Exception {
        return _batchObservingVariableChanges;
    }

    public void setbatchObservingVariableChanges(boolean value) throws Exception {
        _batchObservingVariableChanges = value;
        if (value)
        {
            _changedVariables = new HashSet<String>();
        }
        else
        {
            if (_changedVariables != null)
            {
                for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ variableName : _changedVariables)
                {
                    /* [UNSUPPORTED] 'var' as type is unsupported "var" */ currentValue = _globalVariables[variableName];
                    variableChangedEvent(variableName, currentValue);
                }
            }
             
            _changedVariables = null;
        } 
    }

    boolean _batchObservingVariableChanges = new boolean();
    public RTObject get___idx(String variableName) throws Exception {
        RTObject varContents;
        boolean boolVar___0 = _globalVariables.TryGetValue(variableName, refVar___0);
        if (boolVar___0)
        {
            RefSupport<RTObject> refVar___0 = new RefSupport<RTObject>();
            resVar___1 = (varContents instanceof Value ? (Value)varContents : (Value)null).getvalueRTObject();
            varContents = refVar___0.getValue();
            return resVar___1;
        }
        else
            return null; 
    }

    public void set___idx(String variableName, RTObject value) throws Exception {
        Value val = Value.create(value);
        if (val == null)
        {
            if (value == null)
            {
                throw new StoryException("Cannot pass null to VariableState");
            }
            else
            {
                throw new StoryException("Invalid value passed to VariableState: " + value.ToString());
            } 
        }
         
        setGlobal(variableName,val);
    }

    System.Collections.IEnumerator system___Collections___IEnumerable___GetEnumerator() throws Exception {
        return getEnumerator();
    }

    /**
    * Enumerator to allow iteration over all global variables by name.
    */
    public IEnumerator<String> getEnumerator() throws Exception {
        return _globalVariables.Keys.GetEnumerator();
    }

    public VariablesState(CallStack callStack) throws Exception {
        _globalVariables = new HashMap<String, RTObject>();
        _callStack = callStack;
    }

    public void copyFrom(VariablesState varState) throws Exception {
        _globalVariables = new HashMap<String, RTObject>(varState._globalVariables);
        variableChangedEvent = varState.variableChangedEvent;
        if (varState.getbatchObservingVariableChanges() != getbatchObservingVariableChanges())
        {
            if (varState.getbatchObservingVariableChanges())
            {
                _batchObservingVariableChanges = true;
                _changedVariables = new HashSet<String>(varState._changedVariables);
            }
            else
            {
                _batchObservingVariableChanges = false;
                _changedVariables = null;
            } 
        }
         
    }

    public HashMap<String, RTObject> getjsonToken() throws Exception {
        return Json.HashMapRuntimeObjsToJRTObject(_globalVariables);
    }

    public void setjsonToken(HashMap<String, RTObject> value) throws Exception {
        _globalVariables = Json.JRTObjectToHashMapRuntimeObjs(value);
    }

    public RTObject getVariableWithName(String name) throws Exception {
        return getVariableWithName(name,-1);
    }

    RTObject getVariableWithName(String name, int contextIndex) throws Exception {
        RTObject varValue = getRawVariableWithName(name,contextIndex);
        // Get value from pointer?
        VariablePointerValue varPointer = varValue instanceof VariablePointerValue ? (VariablePointerValue)varValue : (VariablePointerValue)null;
        if (varPointer)
        {
            varValue = valueAtVariablePointer(varPointer);
        }
         
        return varValue;
    }

    RTObject getRawVariableWithName(String name, int contextIndex) throws Exception {
        RTObject varValue = null;
        // 0 context = global
        if (contextIndex == 0 || contextIndex == -1)
        {
            boolean boolVar___2 = _globalVariables.TryGetValue(name, refVar___1);
            if (boolVar___2)
            {
                RefSupport<RTObject> refVar___1 = new RefSupport<RTObject>();
                RTObject resVar___3 = varValue;
                varValue = refVar___1.getValue();
                return resVar___3;
            }
             
        }
         
        // Temporary
        varValue = _callStack.getTemporaryVariableWithName(name,contextIndex);
        if (varValue == null)
            throw new System.Exception("RUNTIME ERROR: Variable '" + name + "' could not be found in context '" + contextIndex + "'. This shouldn't be possible so is a bug in the ink engine. Please try to construct a minimal story that reproduces the problem and report to inkle, thank you!");
         
        return varValue;
    }

    public RTObject valueAtVariablePointer(VariablePointerValue pointer) throws Exception {
        return getVariableWithName(pointer.getvariableName(),pointer.getcontextIndex());
    }

    public void assign(VariableAssignment varAss, RTObject value) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ name = varAss.getvariableName();
        int contextIndex = -1;
        // Are we assigning to a global variable?
        boolean setGlobal = false;
        if (varAss.getisNewDeclaration())
        {
            setGlobal = varAss.getisGlobal();
        }
        else
        {
            setGlobal = _globalVariables.ContainsKey(name);
        } 
        // Constructing new variable pointer reference
        if (varAss.getisNewDeclaration())
        {
            VariablePointerValue varPointer = value instanceof VariablePointerValue ? (VariablePointerValue)value : (VariablePointerValue)null;
            if (varPointer)
            {
                VariablePointerValue fullyResolvedVariablePointer = resolveVariablePointer(varPointer);
                value = fullyResolvedVariablePointer;
            }
             
        }
        else
        {
            // Assign to existing variable pointer?
            // Then assign to the variable that the pointer is pointing to by name.
            // De-reference variable reference to point to
            VariablePointerValue existingPointer = null;
            do
            {
                existingPointer = GetRawVariableWithName(name, contextIndex) instanceof VariablePointerValue ? (VariablePointerValue)GetRawVariableWithName(name, contextIndex) : (VariablePointerValue)null;
                if (existingPointer)
                {
                    name = existingPointer.getvariableName();
                    contextIndex = existingPointer.getcontextIndex();
                    setGlobal = (contextIndex == 0);
                }
                 
            }
            while (existingPointer);
        } 
        if (setGlobal)
        {
            SetGlobal(name, value);
        }
        else
        {
            _callStack.SetTemporaryVariable(name, value, varAss.getisNewDeclaration(), contextIndex);
        } 
    }

    void setGlobal(String variableName, RTObject value) throws Exception {
        RTObject oldValue = null;
        RefSupport<RTObject> refVar___2 = new RefSupport<RTObject>();
        _globalVariables.TryGetValue(variableName, refVar___2);
        oldValue = refVar___2.getValue();
        _globalVariables[variableName] = value;
        if (variableChangedEvent != null && !value.equals(oldValue))
        {
            if (getbatchObservingVariableChanges())
            {
                _changedVariables.Add(variableName);
            }
            else
            {
                variableChangedEvent(variableName, value);
            } 
        }
         
    }

    // Given a variable pointer with just the name of the target known, resolve to a variable
    // pointer that more specifically points to the exact instance: whether it's global,
    // or the exact position of a temporary on the callstack.
    VariablePointerValue resolveVariablePointer(VariablePointerValue varPointer) throws Exception {
        int contextIndex = varPointer.getcontextIndex();
        if (contextIndex == -1)
            contextIndex = getContextIndexOfVariableNamed(varPointer.getvariableName());
         
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ valueOfVariablePointedTo = getRawVariableWithName(varPointer.getvariableName(),contextIndex);
        // Extra layer of indirection:
        // When accessing a pointer to a pointer (e.g. when calling nested or
        // recursive functions that take a variable references, ensure we don't create
        // a chain of indirection by just returning the final target.
        VariablePointerValue doubleRedirectionPointer = valueOfVariablePointedTo instanceof VariablePointerValue ? (VariablePointerValue)valueOfVariablePointedTo : (VariablePointerValue)null;
        if (doubleRedirectionPointer)
        {
            return doubleRedirectionPointer;
        }
        else
        {
            return new VariablePointerValue(varPointer.getvariableName(),contextIndex);
        } 
    }

    // Make copy of the variable pointer so we're not using the value direct from
    // the runtime. Temporary must be local to the current scope.
    // 0  if named variable is global
    // 1+ if named variable is a temporary in a particular call stack element
    int getContextIndexOfVariableNamed(String varName) throws Exception {
        if (_globalVariables.ContainsKey(varName))
            return 0;
         
        return _callStack.getcurrentElementIndex();
    }

    HashMap<String, RTObject> _globalVariables = new HashMap<String, RTObject>();
    // Used for accessing temporary variables
    CallStack _callStack;
    HashSet<String> _changedVariables = new HashSet<String>();
}


