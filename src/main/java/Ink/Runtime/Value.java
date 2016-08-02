package Ink.Runtime;

public abstract class Value<T> extends AbstractValue {
	public T value;

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public RTObject getvalueObject() {
		return (RTObject) value;
	}

	public Value(T val) {
		value = val;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
