package com.bladecoder.ink.runtime;

class DivertTargetValue extends Value<Path> {
    public DivertTargetValue() {
        super(null);
    }

    public DivertTargetValue(Path targetPath) {
        super(targetPath);
    }

    @Override
    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getValueType()) return this;

        throw BadCastException(newType);
    }

    @Override
    public boolean isTruthy() throws Exception {
        throw new Exception("Shouldn't be checking the truthiness of a divert target");
    }

    public Path getTargetPath() {
        return this.getValue();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DivertTarget;
    }

    public void setTargetPath(Path value) {
        this.setValue(value);
    }

    @Override
    public String toString() {
        return "DivertTargetValue(" + getTargetPath() + ")";
    }
}
