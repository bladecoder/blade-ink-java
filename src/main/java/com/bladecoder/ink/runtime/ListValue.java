package com.bladecoder.ink.runtime;

import java.util.Map.Entry;

class ListValue extends Value<RawList> {

	public ListValue(RawList list) {
		super(list);
	}

	public ListValue() {
		super(new RawList());
	}

	public ListValue(RawListItem singleItem, int singleValue) {
		super(new RawList());
		value.put(singleItem, singleValue);
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
			Entry<RawListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new IntValue(0);
			else
				return new IntValue(max.getValue());
		}

		else if (newType == ValueType.Float) {
			Entry<RawListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new FloatValue(0.0f);
			else
				return new FloatValue((float) max.getValue());
		}

		else if (newType == ValueType.String) {
			Entry<RawListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new StringValue("");
			else {
				return new StringValue(max.getKey().toString());
			}
		}

		if (newType == getValueType())
			return this;

		throw new RuntimeException("Unexpected type cast of Value to new ValueType");
	}

}
