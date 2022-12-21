package com.bladecoder.ink.runtime;

class BoolValue extends Value<Boolean> {
    public BoolValue() {
        this(false);
    }

    public BoolValue(boolean boolVal) {
        super(boolVal);
    }

    @Override
    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getValueType()) {
            return this;
        }

        if (newType == ValueType.Int) {
            return new IntValue(value ? 1 : 0);
        }

        if (newType == ValueType.Float) {
            return new FloatValue(value ? 1f : 0f);
        }

        if (newType == ValueType.String) {
            return new StringValue(value.toString());
        }

        throw BadCastException(newType);
    }

    @Override
    public boolean isTruthy() {
        return value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.Bool;
    }
}
