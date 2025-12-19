package com.bladecoder.ink.runtime;

// TODO: Think: Erm, I get that this contains a string, but should
// we really derive from Value<string>? That seems a bit misleading to me.
class VariablePointerValue extends Value<String> {
    // Where the variable is located
    // -1 = default, unknown, yet to be determined
    // 0 = in global scope
    // 1+ = callstack element index + 1 (so that the first doesn't conflict with
    // special global scope)
    private int contextIndex;

    public VariablePointerValue() {
        this(null);
    }

    public VariablePointerValue(String variableName) {
        this(variableName, -1);
    }

    public VariablePointerValue(String variableName, int contextIndex) {
        super(variableName);
        this.setContextIndex(contextIndex);
    }

    @Override
    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getValueType()) return this;

        throw BadCastException(newType);
    }

    @Override
    RTObject copy() {
        return new VariablePointerValue(getVariableName(), getContextIndex());
    }

    public int getContextIndex() {
        return contextIndex;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.VariablePointer;
    }

    public String getVariableName() {
        return this.getValue();
    }

    @Override
    public boolean isTruthy() throws Exception {
        throw new Exception("Shouldn't be checking the truthiness of a variable pointer");
    }

    public void setContextIndex(int value) {
        contextIndex = value;
    }

    public void setvariableName(String value) {
        this.setValue(value);
    }

    @Override
    public String toString() {
        return "VariablePointerValue(" + getVariableName() + ")";
    }
}
