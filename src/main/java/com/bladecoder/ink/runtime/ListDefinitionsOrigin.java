package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class ListDefinitionsOrigin {
	private HashMap<String, ListDefinition> lists;
	private HashMap<String, ListValue> allUnambiguousListValueCache;

	public ListDefinitionsOrigin(List<ListDefinition> lists) {
		this.lists = new HashMap<String, ListDefinition>();
		allUnambiguousListValueCache = new HashMap<String, ListValue>();

		for (ListDefinition list : lists) {
			this.lists.put(list.getName(), list);

			for (Entry<InkListItem, Integer> itemWithValue : list.getItems().entrySet()) {
				InkListItem item = itemWithValue.getKey();
				Integer val = itemWithValue.getValue();
				ListValue listValue = new ListValue(item, val);

				// May be ambiguous, but compiler should've caught that,
				// so we may be doing some replacement here, but that's okay.
				allUnambiguousListValueCache.put(item.getItemName(), listValue);
				allUnambiguousListValueCache.put(item.getFullName(), listValue);
			}
		}
	}

	public ListDefinition getListDefinition(String name) {
		return lists.get(name);
	}

	public List<ListDefinition> getLists() {
		List<ListDefinition> listOfLists = new ArrayList<ListDefinition>();
		for (ListDefinition namedList : lists.values()) {
			listOfLists.add(namedList);
		}

		return listOfLists;
	}

	ListValue findSingleItemListWithName(String name) {
		ListValue val = null;
		
		val = allUnambiguousListValueCache.get(name);
		
		return val;
	}

}
