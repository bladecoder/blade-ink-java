package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.Map.Entry;

public class ListDefinition {
	private String name;
	private HashMap<RawListItem, Integer> items;

	// The main representation should be simple item names rather than a
	// RawListItem,
	// since we mainly want to access items based on their simple name, since
	// that's
	// how they'll be most commonly requested from ink.
	private HashMap<String, Integer> itemNameToValues;

	public ListDefinition(String name, HashMap<String, Integer> items) {
		this.name = name;
		this.itemNameToValues = items;
	}

	public ListValue listRange(int min, int max) {
		RawList rawList = new RawList();
		for (Entry<String, Integer> nameAndValue : itemNameToValues.entrySet()) {
			if (nameAndValue.getValue() >= min && nameAndValue.getValue() <= max) {
				RawListItem item = new RawListItem(name, nameAndValue.getKey());

				rawList.put(item, nameAndValue.getValue());
			}
		}

		return new ListValue(rawList);
	}

	public HashMap<RawListItem, Integer> getItems() {
		if (items == null) {
			HashMap<RawListItem, Integer> items = new HashMap<RawListItem, Integer>();
			for (Entry<String, Integer> itemNameAndValue : itemNameToValues.entrySet()) {
				RawListItem item = new RawListItem(name, itemNameAndValue.getKey());
				items.put(item, itemNameAndValue.getValue());
			}
		}

		return items;
	}

	public String getName() {
		return name;
	}

	public int getValueForItem(RawListItem item) {
		Integer v = itemNameToValues.get(item.getItemName());

		if (v != null)
			return v;

		return 0;
	}

	public Integer tryGetValueForItem(String itemName) {
		return itemNameToValues.get(itemName);
	}

	public boolean containsItem(RawListItem itemName) {
		return itemNameToValues.containsKey(itemName);
	}

	public RawListItem getItemWithValue(int val) {
		RawListItem item = null;

		for (Entry<String, Integer> namedItem : itemNameToValues.entrySet()) {
			if (namedItem.getValue() == val) {
				return new RawListItem(name, namedItem.getKey());
			}
		}

		return item;
	}
}
