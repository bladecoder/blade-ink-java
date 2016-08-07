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
			return String.format("line {0} of {1}", startLineNumber, fileName);
		} else {
			return "line " + startLineNumber;
		}
	}

}
