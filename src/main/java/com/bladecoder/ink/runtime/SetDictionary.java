package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.Map;

//Helper class purely to make it less unweildly to type Dictionary<string, int> all the time.
@SuppressWarnings("serial")
public class SetDictionary extends HashMap<String, Integer> {
	public SetDictionary() {

	}

	public SetDictionary(Entry<String, Integer> singleElement) {
		put(singleElement.getKey(), singleElement.getValue());
	}

	public SetDictionary(HashMap<String, Integer> otherDict) {
		super(otherDict);
	}

	public SetDictionary union(SetDictionary otherDict) {
		SetDictionary union = new SetDictionary(this);
		for (String key : otherDict.keySet())
			union.put(key, otherDict.get(key));

		return union;
	}

	public SetDictionary without(SetDictionary setToRemove) {
		SetDictionary result = new SetDictionary(this);
		for (String kv : setToRemove.keySet())
			result.remove(kv);

		return result;
	}

	public SetDictionary intersect(SetDictionary otherDict) {
		SetDictionary intersection = new SetDictionary();
		for (Entry<String, Integer> kv : this.entrySet()) {
			if (otherDict.containsKey(kv.getKey()))
				intersection.put(kv.getKey(), kv.getValue());
		}
		return intersection;
	}

	public Entry<String, Integer> getMaxItem() {
		CustomEntry max = new CustomEntry(null, 0);

		for (Entry<String, Integer> kv : this.entrySet()) {
			if (max.getKey() == null || kv.getValue() > max.getValue()) {
				max.set(kv);
			}
		}

		return max;
	}

	public Entry<String, Integer> getMinItem() {
		CustomEntry min = new CustomEntry(null, 0);

		for (Entry<String, Integer> kv : this.entrySet()) {
			if (min.getKey() == null || kv.getValue() < min.getValue())
				min.set(kv);
		}

		return min;
	}

	public boolean contains(SetDictionary otherSet) {
		for (Entry<String, Integer> kv : otherSet.entrySet()) {
			if (!this.containsKey(kv.getKey()))
				return false;
		}

		return true;
	}

	public boolean greaterThan(SetDictionary otherSet) {
		if (size() == 0)
			return false;
		if (otherSet.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() > otherSet.getMaxItem().getValue();
	}
	
	public boolean greaterThanOrEquals(SetDictionary otherSet) {
		if (size() == 0)
			return false;
		if (otherSet.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() >= otherSet.getMinItem().getValue() &&
				getMaxItem().getValue() >= otherSet.getMaxItem().getValue();
	}
	
	public boolean lessThan(SetDictionary otherSet) {
		if (otherSet.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() < otherSet.getMinItem().getValue();
	}
	
	public boolean lessThanOrEquals(SetDictionary otherSet) {
		if (otherSet.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() <= otherSet.getMaxItem().getValue() &&
				getMinItem().getValue() <= otherSet.getMinItem().getValue();
	}

	public SetDictionary maxAsSet() {
		if (size() > 0)
			return new SetDictionary(getMaxItem());
		else
			return new SetDictionary();
	}

	public SetDictionary minAsSet() {
		if (size() > 0)
			return new SetDictionary(getMinItem());
		else
			return new SetDictionary();
	}

	@Override
	public boolean equals(Object other) {
		SetDictionary otherSetValue = null;

		if (other instanceof SetDictionary)
			otherSetValue = (SetDictionary) other;

		if (otherSetValue == null)
			return false;
		if (otherSetValue.size() != size())
			return false;

		for (String key : keySet()) {
			if (!otherSetValue.containsKey(key))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int ownHash = 0;

		for (String key : keySet())
			ownHash += key.hashCode();

		return ownHash;
	}

	public class CustomEntry implements Map.Entry<String, Integer> {

		private String key;
		private Integer value;

		CustomEntry(String key, Integer value) {
			set(key, value);
		}

		public void set(String key, Integer value) {
			this.key = key;
			this.value = value;
		}

		public void set(Map.Entry<String, Integer> e) {
			key = e.getKey();
			value = e.getValue();
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Integer getValue() {
			return value;
		}

		@Override
		public Integer setValue(Integer value) {
			Integer old = this.value;
			this.value = value;

			return old;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}
}
