//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package com.bladecoder.ink.runtime;


public class DebugMetadata   
{
    public int startLineNumber = 0;
    public int endLineNumber = 0;
    public String fileName = null;
    public String sourceName = null;
    public DebugMetadata() throws Exception {
    }

    public String toString() {
        try
        {
            if (fileName != null)
            {
                return String.format("line {0} of {1}", startLineNumber, fileName);
            }
            else
            {
                return "line " + startLineNumber;
            } 
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


