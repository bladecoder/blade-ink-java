package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.FloatValue;
import com.bladecoder.ink.runtime.IntValue;
import com.bladecoder.ink.runtime.Value;
import com.bladecoder.ink.runtime.ValueType;

public class StringValue extends Value<String> {
	@Override
	public ValueType getvalueType() throws Exception {
		return ValueType.String;
	}

	@Override
	public boolean getisTruthy() throws Exception {
		return getValue().length() > 0;
	}

	private boolean __isNewline;

	public boolean getisNewline() {
		return __isNewline;
	}

	public void setisNewline(boolean value) {
		__isNewline = value;
	}

	private boolean __isInlineWhitespace;

	public boolean getisInlineWhitespace() {
		return __isInlineWhitespace;
	}

	public void setisInlineWhitespace(boolean value) {
		__isInlineWhitespace = value;
	}

	public boolean getisNonWhitespace() throws Exception {
		return !getisNewline() && !getisInlineWhitespace();
	}

	public StringValue(String str) throws Exception {
		super(str);
		// Classify whitespace status
		setisNewline("\n".equals(getValue()));

		setisInlineWhitespace(true);
		for (char c : getValue().toCharArray()) {
			if (c != ' ' && c != '\t') {
				setisInlineWhitespace(false);
				break;
			}

		}
	}

	public StringValue() throws Exception {
		this("");
	}

	@Override
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == getvalueType()) {
			return this;
		}

		if (newType == ValueType.Int) {
			try {
				int parsedInt = Integer.parseInt(getValue());

				return new IntValue(parsedInt);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		if (newType == ValueType.Float) {
			try {
				float parsedFloat = Float.parseFloat(getValue());

				return new FloatValue(parsedFloat);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		throw new Exception("Unexpected type cast of Value to new ValueType");
	}

}
