package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Confusingly from a C# point of view, a LIST in ink is actually
// modelled using a C# Dictionary!
@SuppressWarnings("serial")
public class RawList extends HashMap<String, Integer> {
	// Story has to set this so that the value knows its origin,
	// necessary for certain operations (e.g. interacting with ints)
	public ListDefinition singleOriginList;

	public RawList() {
		TEMP_DebugAssertNames();
	}

	public RawList(Entry<String, Integer> singleElement) {
		put(singleElement.getKey(), singleElement.getValue());
		TEMP_DebugAssertNames();
	}

	public RawList(HashMap<String, Integer> otherList) {
		super(otherList);
		TEMP_DebugAssertNames();
	}

	public RawList union(RawList otherList) {
		RawList union = new RawList(this);
		for (String key : otherList.keySet())
			union.put(key, otherList.get(key));

		return union;
	}

	public RawList without(RawList listToRemove) {
		RawList result = new RawList(this);
		for (String kv : listToRemove.keySet())
			result.remove(kv);

		return result;
	}

	public RawList intersect(RawList otherList) {
		RawList intersection = new RawList();
		for (Entry<String, Integer> kv : this.entrySet()) {
			if (otherList.containsKey(kv.getKey()))
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

	public boolean contains(RawList otherList) {
		for (Entry<String, Integer> kv : otherList.entrySet()) {
			if (!this.containsKey(kv.getKey()))
				return false;
		}

		return true;
	}

	public boolean greaterThan(RawList otherList) {
		if (size() == 0)
			return false;
		if (otherList.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() > otherList.getMaxItem().getValue();
	}

	public boolean greaterThanOrEquals(RawList otherList) {
		if (size() == 0)
			return false;
		if (otherList.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() >= otherList.getMinItem().getValue()
				&& getMaxItem().getValue() >= otherList.getMaxItem().getValue();
	}

	public boolean lessThan(RawList otherList) {
		if (otherList.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() < otherList.getMinItem().getValue();
	}

	public boolean lessThanOrEquals(RawList otherList) {
		if (otherList.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() <= otherList.getMaxItem().getValue()
				&& getMinItem().getValue() <= otherList.getMinItem().getValue();
	}

	public RawList maxAsList() {
		if (size() > 0)
			return new RawList(getMaxItem());
		else
			return new RawList();
	}

	public RawList minAsList() {
		if (size() > 0)
			return new RawList(getMinItem());
		else
			return new RawList();
	}

	// Runtime sets may reference items from different origin sets
	public String getSingleOriginListName() {
		String name = null;

		for (Entry<String, Integer> fullNamedItem : entrySet()) {
			String listName = fullNamedItem.getKey().split(".")[0];

			// First name - take it as the assumed single origin name
			if (name == null)
				name = listName;

			// A different one than one we've already had? No longer
			// single origin.
			else if (name != listName)
				return null;
		}

		if ("UNKNOWN".equals(name))
			return null;

		return name;
	}

	public RawList getInverse() {

		RawList rawList = new RawList();

		if (singleOriginList != null) {
			for (Entry<String, Integer> nameValue : singleOriginList.getItems().entrySet()) {
				String fullName = singleOriginList.getName() + "." + nameValue.getKey();

				if (!containsKey(fullName))
					rawList.put(fullName, nameValue.getValue());
			}
		}

		return rawList;

	}

	public RawList getAll() {

		RawList list = new RawList();

		if (singleOriginList != null) {
			for (Entry<String, Integer> kv : singleOriginList.getItems().entrySet())
				list.put(singleOriginList.getName() + "." + kv.getKey(), kv.getValue());
		}

		return list;
	}

	@Override
	public boolean equals(Object other) {
		RawList otherRawList = null;

		if (other instanceof RawList)
			otherRawList = (RawList) other;

		if (otherRawList == null)
			return false;
		if (otherRawList.size() != size())
			return false;

		for (String key : keySet()) {
			if (!otherRawList.containsKey(key))
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

	private void TEMP_DebugAssertNames() {
		for (Entry<String, Integer> kv : entrySet()) {
			if (!kv.getKey().contains(".") && "UNKNOWN".equals(kv.getKey()))
				throw new RuntimeException("Not a full item name");
		}
	}

	@Override
	public String toString() {
		List<String> ordered = new ArrayList<String>(keySet());

		Collections.sort(ordered, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return get(o1) - get(o2);
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
