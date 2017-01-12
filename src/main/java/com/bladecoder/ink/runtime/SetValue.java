package com.bladecoder.ink.runtime;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class SetValue extends Value<HashMap<String, Integer>> {

	public SetValue(HashMap<String, Integer> val) {
		super(val);
	}
	

	public SetValue() {
		super(new HashMap<String, Integer>());
	}

	public SetValue(String singleItemName, int singleValue) {
		super(new HashMap<String, Integer>());
		value.put(singleItemName, singleValue);
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
			else
				return new StringValue(max.getKey());
		}

		if (newType == getValueType())
			return this;

		throw new Exception("Unexpected type cast of Value to new ValueType");
	}

	@Override
	 public String toString () {
		List<String> ordered = new ArrayList<String>(value.keySet());
		
		ordered.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return value.get(o1) - value.get(o2);
			}
		});
		
	 
		StringBuilder sb = new StringBuilder ();
	     sb.append ("(");
	     for (int i = 0; i < ordered.size(); i++) {
	         if (i > 0)
	             sb.append (", ");
	         
	         sb.append (ordered.get(i));
	         sb.append (": ");
	         sb.append (value.get(ordered.get(i)));
	     }
	     
	     sb.append (")");
	 
	     return sb.toString ();
	 }

}
