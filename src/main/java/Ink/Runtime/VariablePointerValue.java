//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.Value;
import Ink.Runtime.ValueType;
import Ink.Runtime.VariablePointerValue;

// TODO: Think: Erm, I get that this contains a string, but should
// we really derive from Value<string>? That seems a bit misleading to me.
public class VariablePointerValue  extends Value<String> 
{
    public String getvariableName() throws Exception {
        return this.getvalue();
    }

    public void setvariableName(String value) throws Exception {
        this.setvalue(value);
    }

    public ValueType getvalueType() throws Exception {
        return ValueType.VariablePointer;
    }

    public boolean getisTruthy() throws Exception {
        throw new System.Exception("Shouldn't be checking the truthiness of a variable pointer");
    }

    // Where the variable is located
    // -1 = default, unknown, yet to be determined
    // 0  = in global scope
    // 1+ = callstack element index + 1 (so that the first doesn't conflict with special global scope)
    private int __contextIndex = new int();
    public int getcontextIndex() {
        return __contextIndex;
    }

    public void setcontextIndex(int value) {
        __contextIndex = value;
    }

    public VariablePointerValue(String variableName, int contextIndex) throws Exception {
        super(variableName);
        this.setcontextIndex(contextIndex);
    }

    public VariablePointerValue() throws Exception {
        this(null);
    }

    public Value cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
            return this;
         
        throw new System.Exception("Unexpected type cast of Value to new ValueType");
    }

    public String toString() {
        try
        {
            return "VariablePointerValue(" + getvariableName() + ")";
        }
        catch (RuntimeException __dummyCatchVar2)
        {
            throw __dummyCatchVar2;
        }
        catch (Exception __dummyCatchVar2)
        {
            throw new RuntimeException(__dummyCatchVar2);
        }
    
    }

    public RTObject copy() throws Exception {
        return new VariablePointerValue(getvariableName(),getcontextIndex());
    }

}


