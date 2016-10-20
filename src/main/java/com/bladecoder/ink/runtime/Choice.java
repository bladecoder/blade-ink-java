package com.bladecoder.ink.runtime;

/**
 * A generated Choice from the story. A single ChoicePoint in the Story could
 * potentially generate different Choices dynamically dependent on state, so
 * they're separated.
 */
public class Choice extends RTObject {
	private ChoicePoint choicePoint;

	/**
	 * The original index into currentChoices list on the Story when this Choice
	 * was generated, for convenience.
	 */
	private int index = 0;

	// Only used temporarily for loading/saving from JSON
	String originalChoicePath;

	int originalThreadIndex = 0;

	/**
	 * The main text to presented to the player for this Choice.
	 */
	private String text;

	private CallStack.Thread threadAtGeneration;

	public Choice() throws Exception {
	}

	public Choice(ChoicePoint choice) throws Exception {
		this.setChoicePoint(choice);
	}

	public ChoicePoint getchoicePoint() {
		return choicePoint;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * Get the path to the original choice point - where was this choice defined
	 * in the story? A dot separated path into the story data.
	 */
	public String getSourcePath() {
		return choicePoint.getPath().getComponentsString();
	}

	/**
	 * The target path that the Story should be diverted to if this Choice is
	 * chosen.
	 */
	public String getPathStringOnChoice() throws Exception {
		return getchoicePoint().getPathStringOnChoice();
	}

	public String getText() {
		return text;
	}

	public CallStack.Thread getThreadAtGeneration() {
		return threadAtGeneration;
	}

	public void setChoicePoint(ChoicePoint value) {
		choicePoint = value;
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
