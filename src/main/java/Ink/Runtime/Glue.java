//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import Ink.Runtime.GlueType;
import Ink.Runtime.RTObject;

public class Glue  extends RTObject 
{
    private GlueType __glueType = GlueType.Bidirectional;
    public GlueType getglueType() {
        return __glueType;
    }

    public void setglueType(GlueType value) {
        __glueType = value;
    }

    public boolean getisLeft() throws Exception {
        return getglueType() == GlueType.Left;
    }

    public boolean getisBi() throws Exception {
        return getglueType() == GlueType.Bidirectional;
    }

    public boolean getisRight() throws Exception {
        return getglueType() == GlueType.Right;
    }

    public Glue(GlueType type) throws Exception {
        setglueType(type);
    }

    public String toString() {
        try
        {
            switch(getglueType())
            {
                case Bidirectional: 
                    return "BidirGlue";
                case Left: 
                    return "LeftGlue";
                case Right: 
                    return "RightGlue";
            
            }
            return "UnexpectedGlueType";
        }
        catch (RuntimeException __dummyCatchVar0)
        {
            throw __dummyCatchVar0;
        }
        catch (Exception __dummyCatchVar0)
        {
            throw new RuntimeException(__dummyCatchVar0);
        }
    
    }

}


