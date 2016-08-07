package com.bladecoder.ink.runtime;

public class Glue extends RTObject {
	private GlueType glueType = GlueType.Bidirectional;

	public Glue(GlueType type) {
		setglueType(type);
	}

	public GlueType getglueType() {
		return glueType;
	}

	public boolean isBi() {
		return getglueType() == GlueType.Bidirectional;
	}

	public boolean isLeft() {
		return getglueType() == GlueType.Left;
	}

	public boolean isRight() {
		return getglueType() == GlueType.Right;
	}

	public void setglueType(GlueType value) {
		glueType = value;
	}

	@Override
	public String toString() {
		switch (getglueType()) {
		case Bidirectional:
			return "BidirGlue";
		case Left:
			return "LeftGlue";
		case Right:
			return "RightGlue";

		}

		return "UnexpectedGlueType";

	}

}
