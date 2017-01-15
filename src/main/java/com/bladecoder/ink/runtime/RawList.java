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
public class RawList extends HashMap<RawListItem, Integer> {
	// Story has to set this so that the value knows its origin,
	// necessary for certain operations (e.g. interacting with ints).
	// Only the story has access to the full set of lists, so that
	// the origin can be resolved from the originListName.
	public List<ListDefinition> origins;

	// Origin name needs to be serialised when content is empty,
	// assuming a name is availble, for list definitions with variable
	// that is currently empty.
	private List<String> originNames;

	public RawList() {
	}

	public RawList(Entry<RawListItem, Integer> singleElement) {
		put(singleElement.getKey(), singleElement.getValue());
	}

	public RawList(RawList otherList) {
		super(otherList);
		this.originNames = otherList.originNames;
	}

	public ListDefinition getOriginOfMaxItem() {
		if (origins == null)
			return null;

		String maxOriginName = getMaxItem().getKey().getOriginName();
		for (ListDefinition origin : origins) {
			if (origin.getName().equals(maxOriginName))
				return origin;
		}

		return null;
	}

	public List<String> getOriginNames() {
		if (this.size() > 0) {
			if (originNames == null && this.size() > 0)
				originNames = new ArrayList<String>();
			else
				originNames.clear();

			for (RawListItem itemAndValue : keySet())
				originNames.add(itemAndValue.getOriginName());
		}

		return originNames;
	}

	public void setInitialOriginNames(List<String> initialOriginName) {
		originNames = new ArrayList<String>();
		originNames.addAll(initialOriginName);
	}

	public RawList union(RawList otherList) {
		RawList union = new RawList(this);
		for (RawListItem key : otherList.keySet())
			union.put(key, otherList.get(key));

		return union;
	}

	public RawList without(RawList listToRemove) {
		RawList result = new RawList(this);
		for (RawListItem kv : listToRemove.keySet())
			result.remove(kv);

		return result;
	}

	public RawList intersect(RawList otherList) {
		RawList intersection = new RawList();
		for (Entry<RawListItem, Integer> kv : this.entrySet()) {
			if (otherList.containsKey(kv.getKey()))
				intersection.put(kv.getKey(), kv.getValue());
		}
		return intersection;
	}

	public Entry<RawListItem, Integer> getMaxItem() {
		CustomEntry max = new CustomEntry(null, 0);

		for (Entry<RawListItem, Integer> kv : this.entrySet()) {
			if (max.getKey() == null || kv.getValue() > max.getValue()) {
				max.set(kv);
			}
		}

		return max;
	}

	public Entry<RawListItem, Integer> getMinItem() {
		CustomEntry min = new CustomEntry(null, 0);

		for (Entry<RawListItem, Integer> kv : this.entrySet()) {
			if (min.getKey() == null || kv.getValue() < min.getValue())
				min.set(kv);
		}

		return min;
	}

	public boolean contains(RawList otherList) {
		for (Entry<RawListItem, Integer> kv : otherList.entrySet()) {
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

		for (Entry<RawListItem, Integer> itemAndValue : entrySet()) {
			String originName = itemAndValue.getKey().getOriginName();

			// First name - take it as the assumed single origin name
			if (name == null)
				name = originName;

			// A different one than one we've already had? No longer
			// single origin.
			else if (name != originName)
				return null;
		}

		return name;
	}

	public RawList getInverse() {

		RawList rawList = new RawList();

		if (origins != null) {
			for (ListDefinition origin : origins) {
				for (Entry<RawListItem, Integer> itemAndValue : origin.getItems().entrySet()) {

					if (!this.containsKey(itemAndValue))
						rawList.put(itemAndValue.getKey(), itemAndValue.getValue());
				}
			}
		}

		return rawList;

	}

	public RawList getAll() {

		RawList list = new RawList();

		if (origins != null) {
			for (ListDefinition origin : origins) {
				for (Entry<RawListItem, Integer> kv : origin.getItems().entrySet()) {
					list.put(kv.getKey(), kv.getValue());
				}
			}
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

		for (RawListItem key : keySet()) {
			if (!otherRawList.containsKey(key))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int ownHash = 0;

		for (RawListItem key : keySet())
			ownHash += key.hashCode();

		return ownHash;
	}

	@Override
	public String toString() {
		List<RawListItem> ordered = new ArrayList<RawListItem>(keySet());

		Collections.sort(ordered, new Comparator<RawListItem>() {
			@Override
			public int compare(RawListItem o1, RawListItem o2) {
				return get(o1) - get(o2);
			}
		});

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < ordered.size(); i++) {
			if (i > 0)
				sb.append(", ");

			RawListItem item = ordered.get(i);

			sb.append(item.getItemName());
		}

		return sb.toString();
	}

	public class CustomEntry implements Map.Entry<RawListItem, Integer> {

		private RawListItem key;
		private Integer value;

		CustomEntry(RawListItem key, Integer value) {
			set(key, value);
		}

		public void set(RawListItem key, Integer value) {
			this.key = key;
			this.value = value;
		}

		public void set(Map.Entry<RawListItem, Integer> e) {
			key = e.getKey();
			value = e.getValue();
		}

		@Override
		public RawListItem getKey() {
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

		public void setKey(RawListItem key) {
			this.key = key;
		}
	}
}
