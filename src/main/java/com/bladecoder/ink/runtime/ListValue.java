package com.bladecoder.ink.runtime;

import java.util.Map.Entry;

class ListValue extends Value<RawList> {

	public ListValue(RawList list) {
		super(list);
	}

	public ListValue() {
		super(new RawList());
	}

	public ListValue(String singleItemName, int singleValue) {
		super(new RawList());
		value.put(singleItemName, singleValue);
	}

	@Override
	public ValueType getValueType() {
		return ValueType.List;
	}

	// Truthy if it contains any non-zero items
	@Override
	public boolean isTruthy() {
		for (Integer kv : value.values()) {
			if (kv != 0)
				return true;
		}

		return false;
	}

	@Override
	public AbstractValue cast(ValueType newType) {
		if (newType == ValueType.Int) {
			Entry<String, Integer> max = value.getMaxItem();
			if (max.getKey() == null)
				return new IntValue(0);
			else
				return new IntValue(max.getValue());
		}

		else if (newType == ValueType.Float) {
			Entry<String, Integer> max = value.getMaxItem();
			if (max.getKey() == null)
				return new FloatValue(0.0f);
			else
				return new FloatValue((float) max.getValue());
		}

		else if (newType == ValueType.String) {
			Entry<String, Integer> max = value.getMaxItem();
			if (max.getKey() == null)
				return new StringValue("");
			else {
				String[] nameParts = max.getKey().split(".");
				String name = nameParts[nameParts.length - 1];
				return new StringValue(name);
			}
		}

		if (newType == getValueType())
			return this;

		throw new RuntimeException("Unexpected type cast of Value to new ValueType");
	}

}
