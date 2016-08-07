package com.bladecoder.ink.runtime;

public class FloatValue extends Value<Float> {
	public FloatValue() {
		this(0.0f);
	}

	public FloatValue(float val) {
		super(val);
	}

	@Override
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == getValueType()) {
			return this;
		}

		if (newType == ValueType.Int) {
			return new IntValue(this.getValue().intValue());
		}

		if (newType == ValueType.String) {
			return new StringValue(this.getValue().toString());
		}

		throw new Exception("Unexpected type cast of Value to new ValueType");
	}

	@Override
	public boolean isTruthy() {
		return getValue() != 0.0f;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.Float;
	}

}
