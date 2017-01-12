package com.bladecoder.ink.runtime;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map.Entry;

public class SetValue extends Value<HashMap<String, Integer>> {

	public SetValue(HashMap<String, Integer> val) {
		super(val);
	}

	@Override
	public ValueType getValueType() {
		return ValueType.Set;
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

	Entry<String, Integer> maxItem() {
		Entry<String, Integer> max = new AbstractMap.SimpleEntry<String, Integer>((String) null, Integer.MIN_VALUE);

		for (Entry<String, Integer> kv : value.entrySet()) {
			if (kv.getValue() > max.getValue())
				max = kv;
		}

		return max;
	}

	@Override
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == ValueType.Int) {
			Entry<String, Integer> max = maxItem();
			if (max.getKey() == null)
				return new IntValue(0);
			else
				return new IntValue(max.getValue());
		}

		else if (newType == ValueType.Float) {
			Entry<String, Integer>  max = maxItem();
			if (max.getKey() == null)
				return new FloatValue(0.0f);
			else
				return new FloatValue((float) max.getValue());
		}

		else if (newType == ValueType.String) {
			Entry<String, Integer>  max = maxItem();
			if (max.getKey() == null)
				return new StringValue("");
			else
				return new StringValue(max.getKey());
		}

		if (newType == getValueType())
			return this;

		throw new Exception("Unexpected type cast of Value to new ValueType");
	}

	public void setValue() {
		value = new HashMap<String, Integer>();
	}

	@Override
	public void setValue(HashMap<String, Integer> dict) {
		value = new HashMap<String, Integer>(dict);
	}

}
