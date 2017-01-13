package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

class ListValue extends Value<RawList> {

	public Set singleOriginSet;

	public ListValue(RawList val) {
		super(val);
		TEMP_DebugAssertNames();
	}

	public ListValue() {
		super(new RawList());
		TEMP_DebugAssertNames();
	}

	public ListValue(String singleItemName, int singleValue) {
		super(new RawList());
		value.put(singleItemName, singleValue);
		TEMP_DebugAssertNames();
	}

	@Override
	public ValueType getValueType() {
		return ValueType.Set;
	}

	// Runtime sets may reference items from different origin sets
	public String getSingleOriginSetName() {
		String name = null;

		for (Entry<String, Integer> fullNamedItem : getValue().entrySet()) {
			String setName = fullNamedItem.getKey().split(".")[0];

			// First name - take it as the assumed single origin name
			if (name == null)
				name = setName;

			// A different one than one we've already had? No longer
			// single origin.
			else if (name != setName)
				return null;
		}

		if ("UNKNOWN".equals(name))
			return null;

		return name;
	}

	public ListValue getInverse() {
		if (singleOriginSet == null)
			return null;

		RawList rawList = new RawList();

		for (Entry<String, Integer> nameValue : singleOriginSet.getItems().entrySet()) {
			String fullName = singleOriginSet.getName() + "." + nameValue.getKey();

			if (!value.containsKey(fullName))
				rawList.put(fullName, nameValue.getValue());
		}

		return new ListValue(rawList);

	}

	public ListValue getAll() {
		if (singleOriginSet == null)
			return null;

		RawList dict = new RawList();

		for (Entry<String, Integer> kv : singleOriginSet.getItems().entrySet())
			dict.put(singleOriginSet.getName() + "." + kv.getKey(), kv.getValue());

		return new ListValue(dict);
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

	public Entry<String, Integer> maxItem() {
		return value.getMaxItem();
	}

	@Override
	public AbstractValue cast(ValueType newType) {
		if (newType == ValueType.Int) {
			Entry<String, Integer> max = maxItem();
			if (max.getKey() == null)
				return new IntValue(0);
			else
				return new IntValue(max.getValue());
		}

		else if (newType == ValueType.Float) {
			Entry<String, Integer> max = maxItem();
			if (max.getKey() == null)
				return new FloatValue(0.0f);
			else
				return new FloatValue((float) max.getValue());
		}

		else if (newType == ValueType.String) {
			Entry<String, Integer> max = maxItem();
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

	void TEMP_DebugAssertNames() {
		for (Entry<String, Integer> kv : value.entrySet()) {
			if (!kv.getKey().contains(".") && "UNKNOWN".equals(kv.getKey()))
				throw new RuntimeException("Not a full item name");
		}
	}

	@Override
	public String toString() {
		List<String> ordered = new ArrayList<String>(value.keySet());

		Collections.sort(ordered, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return value.get(o1) - value.get(o2);
			}
		});

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < ordered.size(); i++) {
			if (i > 0)
				sb.append(", ");

			String fullItemPath = ordered.get(i);
			String[] nameParts = fullItemPath.split(".");
			String itemName = nameParts[nameParts.length - 1];

			sb.append(itemName);
		}

		return sb.toString();
	}

}
