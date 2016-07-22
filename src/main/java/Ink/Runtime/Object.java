//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;
import Ink.Runtime.Container;
import Ink.Runtime.DebugMetadata;
import Ink.Runtime.INamedContent;
import Ink.Runtime.Object;
import Ink.Runtime.Path;

/**
* Base class for all ink runtime content.
*/
/* TODO: abstract */
public class Object   
{
    /**
    * Runtime.Objects can be included in the main Story as a hierarchy.
    * Usually parents are Container objects. (TODO: Always?)
    * The parent.
    */
    private Object __parent;
    public Object getparent() {
        return __parent;
    }

    public void setparent(Object value) {
        __parent = value;
    }

    public DebugMetadata getdebugMetadata() throws Exception {
        if (_debugMetadata == null)
        {
            if (getparent())
            {
                return getparent().debugMetadata;
            }
             
        }
         
        return _debugMetadata;
    }

    public void setdebugMetadata(DebugMetadata value) throws Exception {
        _debugMetadata = value;
    }

    // TODO: Come up with some clever solution for not having
    // to have debug metadata on the object itself, perhaps
    // for serialisation purposes at least.
    DebugMetadata _debugMetadata;
    public int? debugLineNumberOfPath(Path path) throws Exception {
        if (path == null)
            return null;
         
        // Try to get a line number from debug metadata
        Container root = this.getrootContentContainer();
        if (root)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ targetContent = root.ContentAtPath(path);
            if (targetContent)
            {
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ dm = targetContent.debugMetadata;
                if (dm != null)
                {
                    return dm.startLineNumber;
                }
                 
            }
             
        }
         
        return null;
    }

    public Path getpath() throws Exception {
        if (_path == null)
        {
            if (getparent() == null)
            {
                _path = new Path();
            }
            else
            {
                // Maintain a Stack so that the order of the components
                // is reversed when they're added to the Path.
                // We're iterating up the hierarchy from the leaves/children to the root.
                /* [UNSUPPORTED] 'var' as type is unsupported "var" */ comps = new Stack<Ink.Runtime.Path.Component>();
                Object child = this;
                Container container = child.getparent() instanceof Container ? (Container)child.getparent() : (Container)null;
                while (container)
                {
                    INamedContent namedChild = child instanceof INamedContent ? (INamedContent)child : (INamedContent)null;
                    if (namedChild != null && namedChild.gethasValidName())
                    {
                        comps.Push(new Ink.Runtime.Path.Component(namedChild.getname()));
                    }
                    else
                    {
                        comps.Push(new Ink.Runtime.Path.Component(container.getcontent().IndexOf(child)));
                    } 
                    child = container;
                    container = container.parent instanceof Container ? (Container)container.parent : (Container)null;
                }
                _path = new Path(comps);
            } 
        }
         
        return _path;
    }

    Path _path;
    public Object resolvePath(Path path) throws Exception {
        if (path.getisRelative())
        {
            Container nearestContainer = this instanceof Container ? (Container)this : (Container)null;
            if (!nearestContainer)
            {
                Debug.Assert(this.getparent() != null, "Can't resolve relative path because we don't have a parent");
                nearestContainer = this.getparent() instanceof Container ? (Container)this.getparent() : (Container)null;
                Debug.Assert(nearestContainer != null, "Expected parent to be a container");
                Debug.Assert(path.getcomponents()[0].isParent);
                path = path.gettail();
            }
             
            return nearestContainer.ContentAtPath(path);
        }
        else
        {
            return this.getrootContentContainer().ContentAtPath(path);
        } 
    }

    public Path convertPathToRelative(Path globalPath) throws Exception {
        // 1. Find last shared ancestor
        // 2. Drill up using ".." style (actually represented as "^")
        // 3. Re-build downward chain from common ancestor
        Path ownPath = this.getpath();
        int minPathLength = Math.Min(globalPath.getcomponents().Count, ownPath.getcomponents().Count);
        int lastSharedPathCompIndex = -1;
        for (int i = 0;i < minPathLength;++i)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ ownComp = ownPath.getcomponents()[i];
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ otherComp = globalPath.getcomponents()[i];
            if (ownComp.Equals(otherComp))
            {
                lastSharedPathCompIndex = i;
            }
            else
            {
                break;
            } 
        }
        // No shared path components, so just use global path
        if (lastSharedPathCompIndex == -1)
            return globalPath;
         
        int numUpwardsMoves = (ownPath.getcomponents().Count - 1) - lastSharedPathCompIndex;
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ newPathComps = new List<Ink.Runtime.Path.Component>();
        for (int up = 0;up < numUpwardsMoves;++up)
            newPathComps.Add(Ink.Runtime.Path.Component.toParent());
        for (int down = lastSharedPathCompIndex + 1;down < globalPath.getcomponents().Count;++down)
            newPathComps.Add(globalPath.getcomponents()[down]);
        Path relativePath = new Path(newPathComps);
        relativePath.setisRelative(true);
        return relativePath;
    }

    // Find most compact representation for a path, whether relative or global
    public String compactPathString(Path otherPath) throws Exception {
        String globalPathStr = null;
        String relativePathStr = null;
        if (otherPath.getisRelative())
        {
            relativePathStr = otherPath.getcomponentsString();
            globalPathStr = this.getpath().pathByAppendingPath(otherPath).getcomponentsString();
        }
        else
        {
            Path relativePath = convertPathToRelative(otherPath);
            relativePathStr = relativePath.getcomponentsString();
            globalPathStr = otherPath.getcomponentsString();
        } 
        if (relativePathStr.Length < globalPathStr.Length)
            return relativePathStr;
        else
            return globalPathStr; 
    }

    public Container getrootContentContainer() throws Exception {
        Object ancestor = this;
        while (ancestor.getparent())
        {
            ancestor = ancestor.getparent();
        }
        return ancestor instanceof Container ? (Container)ancestor : (Container)null;
    }

    public Object() throws Exception {
    }

    public Object copy() throws Exception {
        throw new System.NotImplementedException(GetType().Name + " doesn't support copying");
    }

    public <T extends Object>void setChild(RefSupport<T> obj, T value) throws Exception {
        if (obj.getValue())
            obj.getValue().parent = null;
         
        obj.setValue(value);
        if (obj.getValue())
            obj.getValue().parent = this;
         
    }

    /**
    * Allow implicit conversion to bool so you don't have to do:
    * if( myObj != null ) ...
    */
    public static boolean __cast(Object obj) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ isNull = Object.ReferenceEquals(obj, null);
        return !isNull;
    }

    /**
    * Required for implicit bool comparison
    */

    /**
    * Required for implicit bool comparison
    */

    /**
    * Required for implicit bool comparison
    */
    public boolean equals(Object obj) {
        try
        {
            return Object.ReferenceEquals(obj, this);
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

    /**
    * Required for implicit bool comparison
    */
    public int hashCode() {
        try
        {
            return super.GetHashCode();
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


