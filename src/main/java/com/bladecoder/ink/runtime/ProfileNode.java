package com.bladecoder.ink.runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class ProfileNode {
	private HashMap<String, ProfileNode> nodes;
	private double selfMillisecs;
	private double totalMillisecs;
	private int selfSampleCount;
	private int totalSampleCount;

	private String key;

	public boolean openInUI;

	public boolean hasChildren() {
		return nodes != null && nodes.size() > 0;
	}

	ProfileNode() {
		
	}
	
	ProfileNode(String key) {
		this.key = key;
	}
	
	String getKey() {
		return key;
	}

	void addSample(String[] stack, double duration) {
		addSample(stack, -1, duration);
	}

	void addSample(String[] stack, int stackIdx, double duration) {

		totalSampleCount++;
		totalMillisecs += duration;

		if (stackIdx == stack.length - 1) {
			selfSampleCount++;
			selfMillisecs += duration;
		}

		if (stackIdx + 1 < stack.length)
			addSampleToNode(stack, stackIdx + 1, duration);
	}

	void addSampleToNode(String[] stack, int stackIdx, double duration)
	{
		String nodeKey = stack[stackIdx];
		if( nodes == null ) nodes = new HashMap<String, ProfileNode>();

		ProfileNode node = nodes.get(nodeKey);
		
		if(node == null ) {
			node = new ProfileNode(nodeKey);
			nodes.put(nodeKey, node);
		}

		node.addSample(stack, stackIdx, duration);
	}

	public Iterable<Entry<String, ProfileNode>> getDescendingOrderedNodes() {
			if( nodes == null ) return null;
			
			List<Entry<String, ProfileNode>> averageStepTimes = new LinkedList<Entry<String, ProfileNode>>(nodes.entrySet());
			
			Collections.sort(averageStepTimes, new Comparator<Entry<String, ProfileNode>>() {
				public int compare(Entry<String, ProfileNode> o1, Entry<String, ProfileNode> o2) {
					return (int) (o1.getValue().totalMillisecs - o2.getValue().totalMillisecs);
				}
			});
			
			
			return averageStepTimes;
	}

	void printHierarchy(StringBuilder sb, int indent)
	{
		pad(sb, indent);

		sb.append(key);
		sb.append(": ");
		sb.append(getOwnReport());
		sb.append('\n');

		if( nodes == null ) return;

		for(Entry<String, ProfileNode> keyNode : getDescendingOrderedNodes()) {
			keyNode.getValue().printHierarchy(sb, indent+1);
		}
	}

	public String getOwnReport() {
		StringBuilder sb = new StringBuilder();
		sb.append("total ");
		sb.append(Profiler.formatMillisecs(totalMillisecs));
		sb.append(", self ");
		sb.append(Profiler.formatMillisecs(selfMillisecs));
		sb.append(" (");
		sb.append(selfSampleCount);
		sb.append(" self samples, ");
		sb.append(totalSampleCount);
		sb.append(" total)");
		
		return sb.toString();
	}

	void pad(StringBuilder sb, int spaces) {
		for (int i = 0; i < spaces; i++)
			sb.append("   ");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		printHierarchy(sb, 0);
		return sb.toString();
	}
}
