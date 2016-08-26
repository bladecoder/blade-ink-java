package com.bladecoder.ink.runtime;

public abstract class Value<T> extends AbstractValue {
	public T value;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public Object getValueObject() {
		return (Object) value;
	}

	public Value(T val) {
		value = val;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
