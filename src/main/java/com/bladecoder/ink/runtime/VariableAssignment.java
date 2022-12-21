package com.bladecoder.ink.runtime;

// The value to be assigned is popped off the evaluation stack, so no need to keep it here
public class VariableAssignment extends RTObject {
    private boolean isGlobal;

    private boolean isNewDeclaration;

    private String variableName = new String();

    // Require default constructor for serialisation
    public VariableAssignment() throws Exception {
        this(null, false);
    }

    public VariableAssignment(String variableName, boolean isNewDeclaration) throws Exception {
        this.setVariableName(variableName);
        this.setIsNewDeclaration(isNewDeclaration);
    }

    public String getVariableName() {
        return variableName;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean isNewDeclaration() {
        return isNewDeclaration;
    }

    public void setIsGlobal(boolean value) {
        isGlobal = value;
    }

    public void setIsNewDeclaration(boolean value) {
        isNewDeclaration = value;
    }

    public void setVariableName(String value) {
        variableName = value;
    }

    @Override
    public String toString() {
        return "VarAssign to " + getVariableName();
    }
}
