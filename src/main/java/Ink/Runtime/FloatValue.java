//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.IntValue;
import Ink.Runtime.StringValue;
import Ink.Runtime.Value;
import Ink.Runtime.ValueType;

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
