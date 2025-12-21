package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.Map.Entry;

public class ListDefinition {
    private String name;
    private HashMap<InkListItem, Integer> items;

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

    public HashMap<InkListItem, Integer> getItems() {
        if (items == null) {
            items = new HashMap<InkListItem, Integer>();
            for (Entry<String, Integer> itemNameAndValue : itemNameToValues.entrySet()) {
                InkListItem item = new InkListItem(name, itemNameAndValue.getKey());
                items.put(item, itemNameAndValue.getValue());
            }
        }

        return items;
    }

    public String getName() {
        return name;
    }

    public Integer getValueForItem(InkListItem item) {
        return itemNameToValues.get(item.getItemName());
    }

    public boolean containsItem(InkListItem item) {
        if (!item.getOriginName().equals(name)) return false;

        return itemNameToValues.containsKey(item.getItemName());
    }

    public boolean containsItemWithName(String itemName) {
        return itemNameToValues.containsKey(itemName);
    }

    public InkListItem getItemWithValue(int val) {
        InkListItem item = null;

        for (Entry<String, Integer> namedItem : itemNameToValues.entrySet()) {
            if (namedItem.getValue() == val) {
                return new InkListItem(name, namedItem.getKey());
            }
        }

        return item;
    }
}
