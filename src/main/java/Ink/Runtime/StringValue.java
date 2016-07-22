//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;
import CS2JNet.System.StringSupport;
import Ink.Runtime.FloatValue;
import Ink.Runtime.IntValue;
import Ink.Runtime.Value;
import Ink.Runtime.ValueType;

public class StringValue  extends Value<String> 
{
    public ValueType getvalueType() throws Exception {
        return ValueType.String;
    }

    public boolean getisTruthy() throws Exception {
        return getvalue().Length > 0;
    }

    private boolean __isNewline = new boolean();
    public boolean getisNewline() {
        return __isNewline;
    }

    public void setisNewline(boolean value) {
        __isNewline = value;
    }

    private boolean __isInlineWhitespace = new boolean();
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
        setisNewline(StringSupport.equals(getvalue(), "\n"));
        setisInlineWhitespace(true);
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ c : getvalue())
        {
            if (c != ' ' && c != '\t')
            {
                setisInlineWhitespace(false);
                break;
            }
             
        }
    }

    public StringValue() throws Exception {
        this("");
    }

    public Value cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
        {
            return this;
        }
         
        if (newType == ValueType.Int)
        {
            int parsedInt = new int();
            RefSupport<int> refVar___0 = new RefSupport<int>();
            boolean boolVar___0 = int.TryParse(getvalue(), refVar___0);
            parsedInt = refVar___0.getValue();
            if (boolVar___0)
            {
                return new IntValue(parsedInt);
            }
            else
            {
                return null;
            } 
        }
         
        if (newType == ValueType.Float)
        {
            float parsedFloat = new float();
            RefSupport<float> refVar___1 = new RefSupport<float>();
            boolean boolVar___1 = float.TryParse(getvalue(), refVar___1);
            parsedFloat = refVar___1.getValue();
            if (boolVar___1)
            {
                return new FloatValue(parsedFloat);
            }
            else
            {
                return null;
            } 
        }
         
        throw new System.Exception("Unexpected type cast of Value to new ValueType");
    }

}


