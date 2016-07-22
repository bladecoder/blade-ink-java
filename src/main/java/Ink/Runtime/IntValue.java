//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.FloatValue;
import Ink.Runtime.StringValue;
import Ink.Runtime.Value;
import Ink.Runtime.ValueType;

public class IntValue  extends Value<int> 
{
    public ValueType getvalueType() throws Exception {
        return ValueType.Int;
    }

    public boolean getisTruthy() throws Exception {
        return getvalue() != 0;
    }

    public IntValue(int intVal) throws Exception {
        super(intVal);
    }

    public IntValue() throws Exception {
        this(0);
    }

    public Value cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
        {
            return this;
        }
         
        if (newType == ValueType.Float)
        {
            return new FloatValue((float)this.getvalue());
        }
         
        if (newType == ValueType.String)
        {
            return new StringValue("" + this.getvalue());
        }
         
        throw new System.Exception("Unexpected type cast of Value to new ValueType");
    }

}


