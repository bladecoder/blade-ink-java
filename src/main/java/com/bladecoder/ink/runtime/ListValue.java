package com.bladecoder.ink.runtime;

import java.util.Map.Entry;

class ListValue extends Value<InkList> {

	public ListValue(InkList list) {
		super(list);
	}

	public ListValue() {
		super(new InkList());
	}

	public ListValue(InkListItem singleItem, int singleValue) {
		super(new InkList());
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
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == ValueType.Int) {
			Entry<InkListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new IntValue(0);
			else
				return new IntValue(max.getValue());
		}

		else if (newType == ValueType.Float) {
			Entry<InkListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new FloatValue(0.0f);
			else
				return new FloatValue((float) max.getValue());
		}

		else if (newType == ValueType.String) {
			Entry<InkListItem, Integer> max = value.getMaxItem();
			if (max.getKey().isNull())
				return new StringValue("");
			else {
				return new StringValue(max.getKey().toString());
			}
		}

		if (newType == getValueType())
			return this;

		 throw BadCastException (newType);
	}

	public static void retainListOriginsForAssignment(RTObject oldValue, RTObject newValue) {
		ListValue oldList = null;

		if (oldValue instanceof ListValue)
			oldList = (ListValue) oldValue;

		ListValue newList = null;

		if (newValue instanceof ListValue)
			newList = (ListValue) newValue;

		// When assigning the emtpy list, try to retain any initial origin names
		if (oldList != null && newList != null && newList.value.size() == 0)
			newList.value.setInitialOriginNames(oldList.value.getOriginNames());
	}

}
