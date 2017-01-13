package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.Map.Entry;

public class Set {
	private String name;
	private HashMap<String, Integer> namedItems;

	public Set(String name, HashMap<String, Integer> namedItems) {
		this.name = name;
		this.namedItems = namedItems;
	}
	
	public String getName() {
		return name;
	}

    public Integer getValueForItem(String itemName) {
        return namedItems.get(itemName);
    }
    
    public boolean containsItem(String itemName) {
    	return namedItems.containsKey(itemName);
    }
    
    public String getItemWithValue (int val) {
        String itemName = null;
    
        for (Entry<String, Integer> namedItem : namedItems.entrySet()) {
            if (namedItem.getValue() == val) {
                return namedItem.getKey();
            }
        }
    
        return itemName;
    }
}
