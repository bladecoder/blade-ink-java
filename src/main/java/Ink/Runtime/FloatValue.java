//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.IntValue;
import Ink.Runtime.StringValue;
import Ink.Runtime.Value;
import Ink.Runtime.ValueType;

public class FloatValue  extends Value<float> 
{
    public ValueType getvalueType() throws Exception {
        return ValueType.Float;
    }

    public boolean getisTruthy() throws Exception {
        return getvalue() != 0.0f;
    }

    public FloatValue(float val) throws Exception {
        super(val);
    }

    public FloatValue() throws Exception {
        this(0.0f);
    }

    public Value cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
        {
            return this;
        }
         
        if (newType == ValueType.Int)
        {
            return new IntValue((int)this.getvalue());
        }
         
        if (newType == ValueType.String)
        {
            return new StringValue("" + this.getvalue());
        }
         
        throw new System.Exception("Unexpected type cast of Value to new ValueType");
    }

}


