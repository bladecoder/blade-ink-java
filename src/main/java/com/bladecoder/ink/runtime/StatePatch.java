package com.bladecoder.ink.runtime;

import java.util.HashMap;
import java.util.HashSet;

class StatePatch {
	private HashMap<String, RTObject> globals;
	private HashSet<String> changedVariables = new HashSet<>();
	private HashMap<Container, Integer> visitCounts = new HashMap<>();
	private HashMap<Container, Integer> turnIndices = new HashMap<>();

	public StatePatch(StatePatch toCopy) {
		if (toCopy != null) {
			globals = new HashMap<>(toCopy.globals);
			changedVariables = new HashSet<>(toCopy.changedVariables);
			visitCounts = new HashMap<>(toCopy.visitCounts);
			turnIndices = new HashMap<>(toCopy.turnIndices);
		} else {
			globals = new HashMap<>();
			changedVariables = new HashSet<>();
			visitCounts = new HashMap<>();
			turnIndices = new HashMap<>();
		}
	}

	public RTObject getGlobal(String name) {
		return globals.get(name);
	}

	public void setGlobal(String name, RTObject value) {
		globals.put(name, value);
	}

	public void addChangedVariable(String name) {
		changedVariables.add(name);
	}

	public Integer getVisitCount(Container container) {
		return visitCounts.get(container);
	}

	public void setVisitCount(Container container, int count) {
		visitCounts.put(container, count);
	}

	public void setTurnIndex(Container container, int index) {
		turnIndices.put(container, index);
	}

	public Integer getTurnIndex(Container container) {
		return turnIndices.get(container);
	}

	public HashMap<String, RTObject> getGlobals() {
		return globals;
	}

	public HashSet<String> getChangedVariables() {
		return changedVariables;
	}

	public HashMap<Container, Integer> getVisitCounts() {
		return visitCounts;
	}

	public HashMap<Container, Integer> getTurnIndices() {
		return turnIndices;
	}
}
