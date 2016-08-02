//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.IntValue;
import com.bladecoder.ink.runtime.StringValue;
import com.bladecoder.ink.runtime.Value;
import com.bladecoder.ink.runtime.ValueType;

public class FloatValue extends Value<Float> {
	public ValueType getvalueType() throws Exception {
		return ValueType.Float;
	}

	public boolean getisTruthy() throws Exception {
		return getValue() != 0.0f;
	}

	public FloatValue(float val) throws Exception {
		super(val);
	}

	public FloatValue() throws Exception {
		this(0.0f);
	}

	@Override
	public AbstractValue cast(ValueType newType) throws Exception {
		if (newType == getvalueType()) {
			return this;
		}

		if (newType == ValueType.Int) {
			return new IntValue(this.getValue().intValue());
		}

		if (newType == ValueType.String) {
			return new StringValue(this.getValue().toString());
		}

		throw new Exception("Unexpected type cast of Value to new ValueType");
	}

}
