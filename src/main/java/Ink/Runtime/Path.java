//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;
import CS2JNet.System.StringSupport;
import Ink.Runtime.Path;
import Ink.Runtime.StringExt;

public class Path  extends IEquatable<Path> 
{
    static String parentId = "^";
    // Immutable Component
    public static class Component  extends IEquatable<Ink.Runtime.Path.Component> 
    {
        private int __index = new int();
        public int getindex() {
            return __index;
        }

        public void setindex(int value) {
            __index = value;
        }

        private String __name = new String();
        public String getname() {
            return __name;
        }

        public void setname(String value) {
            __name = value;
        }

        public boolean getisIndex() throws Exception {
            return getindex() >= 0;
        }

        public boolean getisParent() throws Exception {
            return StringSupport.equals(getname(), Path.parentId);
        }

        public Component(int index) throws Exception {
            Debug.Assert(index >= 0);
            this.setindex(index);
            this.setname(null);
        }

        public Component(String name) throws Exception {
            Debug.Assert(name != null && name.Length > 0);
            this.setname(name);
            this.setindex(-1);
        }

        public static Ink.Runtime.Path.Component toParent() throws Exception {
            return new Ink.Runtime.Path.Component(parentId);
        }

        public String toString() {
            try
            {
                if (getisIndex())
                {
                    return getindex().ToString();
                }
                else
                {
                    return getname();
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

        public boolean equals(Object obj) {
            try
            {
                return equals(obj instanceof Ink.Runtime.Path.Component ? (Ink.Runtime.Path.Component)obj : (Ink.Runtime.Path.Component)null);
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

        public boolean equals(Ink.Runtime.Path.Component otherComp) {
            try
            {
                if (otherComp != null && otherComp.getisIndex() == this.getisIndex())
                {
                    if (getisIndex())
                    {
                        return getindex() == otherComp.getindex();
                    }
                    else
                    {
                        return StringSupport.equals(getname(), otherComp.getname());
                    } 
                }
                 
                return false;
            }
            catch (RuntimeException __dummyCatchVar2)
            {
                throw __dummyCatchVar2;
            }
            catch (Exception __dummyCatchVar2)
            {
                throw new RuntimeException(__dummyCatchVar2);
            }
        
        }

        public int hashCode() {
            try
            {
                if (getisIndex())
                    return this.getindex();
                else
                    return this.getname().GetHashCode(); 
            }
            catch (RuntimeException __dummyCatchVar3)
            {
                throw __dummyCatchVar3;
            }
            catch (Exception __dummyCatchVar3)
            {
                throw new RuntimeException(__dummyCatchVar3);
            }
        
        }
    
    }

    private List<Ink.Runtime.Path.Component> __components = new List<Ink.Runtime.Path.Component>();
    public List<Ink.Runtime.Path.Component> getcomponents() {
        return __components;
    }

    public void setcomponents(List<Ink.Runtime.Path.Component> value) {
        __components = value;
    }

    private boolean __isRelative = new boolean();
    public boolean getisRelative() {
        return __isRelative;
    }

    public void setisRelative(boolean value) {
        __isRelative = value;
    }

    public Ink.Runtime.Path.Component gethead() throws Exception {
        if (getcomponents().Count > 0)
        {
            return getcomponents().First();
        }
        else
        {
            return null;
        } 
    }

    public Path gettail() throws Exception {
        if (getcomponents().Count >= 2)
        {
            List<Ink.Runtime.Path.Component> tailComps = getcomponents().GetRange(1, getcomponents().Count - 1);
            return new Path(tailComps);
        }
        else
        {
            return Path.getself();
        } 
    }

    public int getlength() throws Exception {
        return getcomponents().Count;
    }

    public Ink.Runtime.Path.Component getlastComponent() throws Exception {
        if (getcomponents().Count > 0)
        {
            return getcomponents().Last();
        }
        else
        {
            return null;
        } 
    }

    public boolean getcontainsNamedComponent() throws Exception {
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ comp : getcomponents())
        {
            if (!comp.isIndex)
            {
                return true;
            }
             
        }
        return false;
    }

    public Path() throws Exception {
        setcomponents(new List<Ink.Runtime.Path.Component>());
    }

    public Path(Ink.Runtime.Path.Component head, Path tail) throws Exception {
        this();
        getcomponents().Add(head);
        getcomponents().AddRange(tail.getcomponents());
    }

    public Path(IEnumerable<Ink.Runtime.Path.Component> components) throws Exception {
        this();
        this.getcomponents().AddRange(components);
    }

    public Path(String componentsString) throws Exception {
        this();
        this.setcomponentsString(componentsString);
    }

    public static Path getself() throws Exception {
        Path path = new Path();
        path.setisRelative(true);
        return path;
    }

    public Path pathByAppendingPath(Path pathToAppend) throws Exception {
        Path p = new Path();
        int upwardMoves = 0;
        for (int i = 0;i < pathToAppend.getcomponents().Count;++i)
        {
            if (pathToAppend.getcomponents()[i].isParent)
            {
                upwardMoves++;
            }
            else
            {
                break;
            } 
        }
        for (int i = 0;i < this.getcomponents().Count - upwardMoves;++i)
        {
            p.getcomponents().Add(this.getcomponents()[i]);
        }
        for (int i = upwardMoves;i < pathToAppend.getcomponents().Count;++i)
        {
            p.getcomponents().Add(pathToAppend.getcomponents()[i]);
        }
        return p;
    }

    public String getcomponentsString() throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ compsStr = StringExt.join(".",getcomponents());
        if (getisRelative())
            return "." + compsStr;
        else
            return compsStr; 
    }

    public void setcomponentsString(String value) throws Exception {
        getcomponents().Clear();
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ componentsStr = value;
        // When components start with ".", it indicates a relative path, e.g.
        //   .^.^.hello.5
        // is equivalent to file system style path:
        //  ../../hello/5
        if (componentsStr[0] == '.')
        {
            setisRelative(true);
            componentsStr = componentsStr.Substring(1);
        }
         
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ componentStrings = componentsStr.Split('.');
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ str : componentStrings)
        {
            int index = new int();
            RefSupport<int> refVar___0 = new RefSupport<int>();
            boolean boolVar___0 = int.TryParse(str, refVar___0);
            index = refVar___0.getValue();
            if (boolVar___0)
            {
                getcomponents().Add(new Ink.Runtime.Path.Component(index));
            }
            else
            {
                getcomponents().Add(new Ink.Runtime.Path.Component(str));
            } 
        }
    }

    public String toString() {
        try
        {
            return getcomponentsString();
        }
        catch (RuntimeException __dummyCatchVar4)
        {
            throw __dummyCatchVar4;
        }
        catch (Exception __dummyCatchVar4)
        {
            throw new RuntimeException(__dummyCatchVar4);
        }
    
    }

    public boolean equals(Object obj) {
        try
        {
            return equals(obj instanceof Path ? (Path)obj : (Path)null);
        }
        catch (RuntimeException __dummyCatchVar5)
        {
            throw __dummyCatchVar5;
        }
        catch (Exception __dummyCatchVar5)
        {
            throw new RuntimeException(__dummyCatchVar5);
        }
    
    }

    public boolean equals(Path otherPath) {
        try
        {
            if (otherPath == null)
                return false;
             
            if (otherPath.getcomponents().Count != this.getcomponents().Count)
                return false;
             
            if (otherPath.getisRelative() != this.getisRelative())
                return false;
             
            return otherPath.getcomponents().SequenceEqual(this.getcomponents());
        }
        catch (RuntimeException __dummyCatchVar6)
        {
            throw __dummyCatchVar6;
        }
        catch (Exception __dummyCatchVar6)
        {
            throw new RuntimeException(__dummyCatchVar6);
        }
    
    }

    public int hashCode() {
        try
        {
            return this.toString().GetHashCode();
        }
        catch (RuntimeException __dummyCatchVar7)
        {
            throw __dummyCatchVar7;
        }
        catch (Exception __dummyCatchVar7)
        {
            throw new RuntimeException(__dummyCatchVar7);
        }
    
    }

}


// TODO: Better way to make a hash code!