package com.bladecoder.ink.runtime;

import java.util.List;

/**
 * A generated Choice from the story. A single ChoicePoint in the Story could
 * potentially generate different Choices dynamically dependent on state, so
 * they're separated.
 */
public class Choice extends RTObject {
    Path targetPath;
    boolean isInvisibleDefault;

    List<String> tags;

    /**
     * The original index into currentChoices list on the Story when this Choice
     * was generated, for convenience.
     */
    private int index = 0;

    int originalThreadIndex = 0;

    /**
     * The main text to presented to the player for this Choice.
     */
    private String text;

    private CallStack.Thread threadAtGeneration;

    String sourcePath;

    public Choice() {}

    public int getIndex() {
        return index;
    }

    /**
     * The target path that the Story should be diverted to if this Choice is
     * chosen.
     */
    public String getPathStringOnChoice() throws Exception {
        return targetPath.toString();
    }

    public void setPathStringOnChoice(String value) throws Exception {
        targetPath = new Path(value);
    }

    public String getText() {
        return text;
    }

    public List<String> getTags() {
        return tags;
    }
    ;

    public CallStack.Thread getThreadAtGeneration() {
        return threadAtGeneration;
    }

    public void setIndex(int value) {
        index = value;
    }

    public void setText(String value) {
        text = value;
    }

    public void setThreadAtGeneration(CallStack.Thread value) {
        threadAtGeneration = value;
    }
}
