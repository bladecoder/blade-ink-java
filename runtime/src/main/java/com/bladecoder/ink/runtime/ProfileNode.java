package com.bladecoder.ink.runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Node used in the hierarchical tree of timings used by the Profiler. Each node
 * corresponds to a single line viewable in a UI-based representation.
 *
 * @author rgarcia
 */
public class ProfileNode {
    private HashMap<String, ProfileNode> nodes;
    private double selfMillisecs;
    private double totalMillisecs;
    private int selfSampleCount;
    private int totalSampleCount;

    private String key;

    /**
     * Horribly hacky field only used by ink unity integration, but saves
     * constructing an entire data structure that mirrors the one in here purely to
     * store the state of whether each node in the UI has been opened or not.
     */
    public boolean openInUI;

    /**
     * Whether this node contains any sub-nodes - i.e. does it call anything else
     * that has been recorded?
     *
     * @return true if has children; otherwise, false.
     */
    public boolean hasChildren() {
        return nodes != null && nodes.size() > 0;
    }

    /**
     * The key for the node corresponds to the printable name of the callstack
     * element.
     */
    public String getKey() {
        return key;
    }

    ProfileNode() {}

    ProfileNode(String key) {
        this.key = key;
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

        if (stackIdx + 1 < stack.length) addSampleToNode(stack, stackIdx + 1, duration);
    }

    void addSampleToNode(String[] stack, int stackIdx, double duration) {
        String nodeKey = stack[stackIdx];
        if (nodes == null) nodes = new HashMap<>();

        ProfileNode node = nodes.get(nodeKey);

        if (node == null) {
            node = new ProfileNode(nodeKey);
            nodes.put(nodeKey, node);
        }

        node.addSample(stack, stackIdx, duration);
    }

    /**
     * Returns a sorted enumerable of the nodes in descending order of how long they
     * took to run.
     */
    public Iterable<Entry<String, ProfileNode>> getDescendingOrderedNodes() {
        if (nodes == null) return null;

        List<Entry<String, ProfileNode>> averageStepTimes = new LinkedList<>(nodes.entrySet());

        Collections.sort(averageStepTimes, new Comparator<Entry<String, ProfileNode>>() {
            @Override
            public int compare(Entry<String, ProfileNode> o1, Entry<String, ProfileNode> o2) {
                return (int) (o1.getValue().totalMillisecs - o2.getValue().totalMillisecs);
            }
        });

        return averageStepTimes;
    }

    void printHierarchy(StringBuilder sb, int indent) {
        pad(sb, indent);

        sb.append(key);
        sb.append(": ");
        sb.append(getOwnReport());
        sb.append('\n');

        if (nodes == null) return;

        for (Entry<String, ProfileNode> keyNode : getDescendingOrderedNodes()) {
            keyNode.getValue().printHierarchy(sb, indent + 1);
        }
    }

    /**
     * Generates a string giving timing information for this single node, including
     * total milliseconds spent on the piece of ink, the time spent within itself
     * (v.s. spent in children), as well as the number of samples (instruction
     * steps) recorded for both too.
     *
     * @return The own report.
     */
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
        for (int i = 0; i < spaces; i++) sb.append("   ");
    }

    /**
     * Total number of milliseconds this node has been active for.
     */
    public int getTotalMillisecs() {
        return (int) totalMillisecs;
    }

    /**
     * String is a report of the sub-tree from this node, but without any of the
     * header information that's prepended by the Profiler in its Report() method.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        printHierarchy(sb, 0);
        return sb.toString();
    }
}
