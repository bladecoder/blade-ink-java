//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.RTObject;

// The value to be assigned is popped off the evaluation stack, so no need to keep it here
public class VariableAssignment  extends RTObject 
{
    private String __variableName = new String();
    public String getvariableName() {
        return __variableName;
    }

    public void setvariableName(String value) {
        __variableName = value;
    }

    private boolean __isNewDeclaration;
    
    public boolean getisNewDeclaration() {
        return __isNewDeclaration;
    }

    public void setisNewDeclaration(boolean value) {
        __isNewDeclaration = value;
    }

    private boolean __isGlobal;
    
    public boolean getisGlobal() {
        return __isGlobal;
    }

    public void setisGlobal(boolean value) {
        __isGlobal = value;
    }

    public VariableAssignment(String variableName, boolean isNewDeclaration) throws Exception {
        this.setvariableName(variableName);
        this.setisNewDeclaration(isNewDeclaration);
    }

    // Require default constructor for serialisation
    public VariableAssignment() throws Exception {
        this(null, false);
    }

    public String toString() {
        try
        {
            return "VarAssign to " + getvariableName();
        }
        catch (RuntimeException __dummyCatchVar0)
        {
            throw __dummyCatchVar0;
        }
        catch (Exception __dummyCatchVar0)
        {
            throw new RuntimeException(__dummyCatchVar0);
        }
    
    }

}


