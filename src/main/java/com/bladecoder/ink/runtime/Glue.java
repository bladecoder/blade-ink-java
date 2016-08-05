package com.bladecoder.ink.runtime;

public class Glue extends RTObject {
	private GlueType __glueType = GlueType.Bidirectional;

	public GlueType getglueType() {
		return __glueType;
	}

	public void setglueType(GlueType value) {
		__glueType = value;
	}

	public boolean getisLeft() {
		return getglueType() == GlueType.Left;
	}

	public boolean getisBi() {
		return getglueType() == GlueType.Bidirectional;
	}

	public boolean getisRight() {
		return getglueType() == GlueType.Right;
	}

	public Glue(GlueType type) {
		setglueType(type);
	}

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
