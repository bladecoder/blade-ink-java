package com.bladecoder.ink.runtime;

class IntValue extends Value<Integer> {
	public IntValue() {
		this(0);
	}

	public IntValue(int intVal) {
		super(intVal);
	}

	@Override
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == getValueType()) {
			return this;
		}

		if (newType == ValueType.Float) {
			return new FloatValue(this.getValue());
		}

		if (newType == ValueType.String) {
			return new StringValue(this.getValue().toString());
		}

		 throw BadCastException (newType);
	}

	@Override
	public boolean isTruthy() {
		return getValue() != 0;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.Int;
	}

}
