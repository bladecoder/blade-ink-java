package com.bladecoder.ink.runtime;

public class DebugMetadata {
	public int startLineNumber = 0;
	public int endLineNumber = 0;
	public String fileName = null;
	public String sourceName = null;

	public DebugMetadata() {
	}

	@Override
	public String toString() {
		if (fileName != null) {
			return String.format("line %d of %s", startLineNumber, fileName);
		} else {
			return "line " + startLineNumber;
		}
	}

}
