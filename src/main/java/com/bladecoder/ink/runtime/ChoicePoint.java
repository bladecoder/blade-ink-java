package com.bladecoder.ink.runtime;

/**
 * The ChoicePoint represents the point within the Story where a Choice instance
 * gets generated. The distinction is made because the text of the Choice can be
 * dynamically generated.
 */
public class ChoicePoint extends RTObject {
	private boolean hasChoiceOnlyContent;

	private boolean hasStartContent;

	private boolean isInvisibleDefault;

	private boolean onceOnly;

	private boolean hasCondition;

	private Path pathOnChoice;

	public ChoicePoint() throws Exception {
		this(true);
	}

	public ChoicePoint(boolean onceOnly) {
		this.setOnceOnly(onceOnly);
	}

	public Container getChoiceTarget() throws Exception {
		RTObject resolvePath = resolvePath(pathOnChoice);

		return resolvePath instanceof Container ? (Container) resolvePath : (Container) null;
	}

	public int getFlags() {
		int flags = 0;
		if (hasCondition())
			flags |= 1;

		if (hasStartContent())
			flags |= 2;

		if (hasChoiceOnlyContent())
			flags |= 4;

		if (isInvisibleDefault())
			flags |= 8;

		if (isOnceOnly())
			flags |= 16;

		return flags;
	}

	public boolean hasChoiceOnlyContent() {
		return hasChoiceOnlyContent;
	}

	public boolean hasCondition() {
		return hasCondition;
	}

	public boolean hasStartContent() {
		return hasStartContent;
	}

	public boolean isInvisibleDefault() {
		return isInvisibleDefault;
	}

	public boolean isOnceOnly() {
		return onceOnly;
	}

	public Path getPathOnChoice() throws Exception {
		// Resolve any relative paths to global ones as we come across them
		if (pathOnChoice != null && pathOnChoice.isRelative()) {
			Container choiceTargetObj = getChoiceTarget();
			if (choiceTargetObj != null) {
				pathOnChoice = choiceTargetObj.getPath();
			}
		}
		return pathOnChoice;
	}

	public String getPathStringOnChoice() throws Exception {
		return compactPathString(getPathOnChoice());
	}

	public void setFlags(int value) {
		setHasCondition((value & 1) > 0);
		setHasStartContent((value & 2) > 0);
		setHasChoiceOnlyContent((value & 4) > 0);
		setIsInvisibleDefault((value & 8) > 0);
		setOnceOnly((value & 16) > 0);
	}

	public void setHasChoiceOnlyContent(boolean value) {
		hasChoiceOnlyContent = value;
	}

	public void setHasCondition(boolean value) {
		hasCondition = value;
	}

	public void setHasStartContent(boolean value) {
		hasStartContent = value;
	}

	public void setIsInvisibleDefault(boolean value) {
		isInvisibleDefault = value;
	}

	public void setOnceOnly(boolean value) {
		onceOnly = value;
	}

	public void setPathOnChoice(Path value) {
		pathOnChoice = value;
	}

	public void setPathStringOnChoice(String value) {
		setPathOnChoice(new Path(value));
	}

	@Override
	public String toString() {
		try {
			Integer targetLineNum = debugLineNumberOfPath(getPathOnChoice());

			String targetString = getPathOnChoice().toString();

			if (targetLineNum != null) {
				targetString = " line " + targetLineNum;
			}

			return "Choice: -> " + targetString;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
