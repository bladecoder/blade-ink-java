package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class ListDefinitionsOrigin {
	private HashMap<String, ListDefinition> lists;

	public ListDefinitionsOrigin(List<ListDefinition> lists) {
		this.lists = new HashMap<String, ListDefinition>();

		for (ListDefinition list : lists) {
			this.lists.put(list.getName(), list);
		}
	}

	public ListDefinition getDefinition(String name) {
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
		InkListItem item = InkListItem.getNull();
		ListDefinition list = null;
		String[] nameParts = name.split("\\.");
		
		if (nameParts.length == 2) {
			item = new InkListItem(nameParts[0], nameParts[1]);
			list = getDefinition(item.getOriginName());
		} else {
			for (Entry<String, ListDefinition> namedList : lists.entrySet()) {
				ListDefinition listWithItem = namedList.getValue();
				item = new InkListItem(namedList.getKey(), name);
				
				if (listWithItem.containsItem(item)) {
					list = listWithItem;
					break;
				}
			}
		}

		// Manager to get the list that contains the given item?
		if (list != null) {
			int itemValue = list.getValueForItem(item);
			return new ListValue(item, itemValue);
		}

		return null;
	}

}
