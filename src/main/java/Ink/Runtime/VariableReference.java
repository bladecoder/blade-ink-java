//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:34
//

package Ink.Runtime;

import Ink.Runtime.Container;
import Ink.Runtime.Object;
import Ink.Runtime.Path;

public class VariableReference  extends Object 
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
        return this.ResolvePath(getpathForCount()) instanceof Container ? (Container)this.ResolvePath(getpathForCount()) : (Container)null;
    }

    public String getpathStringForCount() throws Exception {
        if (getpathForCount() == null)
            return null;
         
        return CompactPathString(getpathForCount());
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
                return String.Format("var({0})", getname());
            }
            else
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ pathStr = getpathStringForCount();
                return String.Format("read_count({0})", pathStr);
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


