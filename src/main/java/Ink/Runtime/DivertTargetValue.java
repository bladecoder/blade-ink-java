//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.Path;
import Ink.Runtime.AbstractValue;
import Ink.Runtime.ValueType;

public class DivertTargetValue  extends Value<Path> 
{
    public Path gettargetPath() throws Exception {
        return this.getValue();
    }

    public void settargetPath(Path value) throws Exception {
        this.setValue(value);
    }

    @Override
    public ValueType getvalueType() throws Exception {
        return ValueType.DivertTarget;
    }

    @Override
    public boolean getisTruthy() throws Exception {
        throw new Exception("Shouldn't be checking the truthiness of a divert target");
    }

    public DivertTargetValue(Path targetPath) throws Exception {
        super(targetPath);
    }

    public DivertTargetValue() throws Exception {
        super(null);
    }

    public AbstractValue cast(ValueType newType) throws Exception {
        if (newType == getvalueType())
            return this;
         
        throw new Exception("Unexpected type cast of Value to new ValueType");
    }

    public String toString() {
        try
        {
            return "DivertTargetValue(" + gettargetPath() + ")";
        }
        catch (RuntimeException __dummyCatchVar1)
        {
            throw __dummyCatchVar1;
        }
        catch (Exception __dummyCatchVar1)
        {
            throw new RuntimeException(__dummyCatchVar1);
        }
    
    }

}


