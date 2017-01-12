package com.bladecoder.ink.runtime;

import java.util.HashMap;

public class Set {
	private String name;
	private HashMap<String, Integer> namedItems;

	public String getName() {
		return name;
	}

	public Set(String name, HashMap<String, Integer> namedItems) {
		this.name = name;
		this.namedItems = namedItems;
	}

}
