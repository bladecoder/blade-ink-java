//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.Path;
import com.bladecoder.ink.runtime.RTObject;

public class VariableReference  extends RTObject 
{
    // Normal named variable
    private String __name = new String();
    public String getname() {
        return __name;
    }

    public void setname(String value) {
        __name = value;
    }

    // Variable reference is actually a path for a visit (read) count
    private Path __pathForCount;
    public Path getpathForCount() {
        return __pathForCount;
    }

    public void setpathForCount(Path value) {
        __pathForCount = value;
    }

    public Container getcontainerForCount() throws Exception {
        return this.resolvePath(getpathForCount()) instanceof Container ? (Container)this.resolvePath(getpathForCount()) : (Container)null;
    }

    public String getpathStringForCount() throws Exception {
        if (getpathForCount() == null)
            return null;
         
        return compactPathString(getpathForCount());
    }

    public void setpathStringForCount(String value) throws Exception {
        if (value == null)
            setpathForCount(null);
        else
            setpathForCount(new Path(value)); 
    }

    public VariableReference(String name) throws Exception {
        this.setname(name);
    }

    // Require default constructor for serialisation
    public VariableReference() throws Exception {
    }

    public String toString() {
        try
        {
            if (getname() != null)
            {
                return String.format("var({0})", getname());
            }
            else
            {
                String pathStr = getpathStringForCount();
                return String.format("read_count({0})", pathStr);
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


