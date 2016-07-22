//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.DivertTargetValue;
import Ink.Runtime.FloatValue;
import Ink.Runtime.IntValue;
import Ink.Runtime.RTObject;
import Ink.Runtime.Path;
import Ink.Runtime.StringValue;
import Ink.Runtime.Value;
import Ink.Runtime.ValueType;

public abstract class Value  extends RTObject 
{
    public abstract ValueType getvalueType() throws Exception ;

    public abstract boolean getisTruthy() throws Exception ;

    public abstract Value cast(ValueType newType) throws Exception ;

    public abstract RTObject getvalueRTObject() throws Exception ;

    public static Value create(RTObject val) throws Exception {
        // Implicitly lose precision from any doubles we get passed in
        if (val instanceof double)
        {
            double doub = (Double)val;
            val = (float)doub;
        }
         
        // Implicitly convert bools into ints
        if (val instanceof boolean)
        {
            boolean b = (Boolean)val;
            val = (int)(b ? 1 : 0);
        }
         
        if (val instanceof int)
        {
            return new IntValue((Integer)val);
        }
        else if (val instanceof long)
        {
            return new IntValue((int)(Long)val);
        }
        else if (val instanceof float)
        {
            return new FloatValue((Float)val);
        }
        else if (val instanceof double)
        {
            return new FloatValue((float)(Double)val);
        }
        else if (val instanceof String)
        {
            return new StringValue((String)val);
        }
        else if (val instanceof Path)
        {
            return new DivertTargetValue((Path)val);
        }
              
        return null;
    }

    public RTObject copy() throws Exception {
        return create(getvalueRTObject());
    }

}


