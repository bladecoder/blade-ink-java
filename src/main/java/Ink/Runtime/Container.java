//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import CS2JNet.JavaSupport.language.RefSupport;
import Ink.Runtime.Container;
import Ink.Runtime.INamedContent;
import Ink.Runtime.Object;
import Ink.Runtime.Path;
import Ink.Runtime.StoryException;
import Ink.Runtime.StringValue;

public class Container  extends Object implements INamedContent
{
    private String __name = new String();
    public String getname() {
        return __name;
    }

    public void setname(String value) {
        __name = value;
    }

    public List<Object> getcontent() throws Exception {
        return _content;
    }

    public void setcontent(List<Object> value) throws Exception {
        AddContent(value);
    }

    List<Object> _content = new List<Object>();
    private Dictionary<String, INamedContent> __namedContent = new Dictionary<String, INamedContent>();
    public Dictionary<String, INamedContent> getnamedContent() {
        return __namedContent;
    }

    public void setnamedContent(Dictionary<String, INamedContent> value) {
        __namedContent = value;
    }

    public Dictionary<String, Object> getnamedOnlyContent() throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ namedOnlyContent = new Dictionary<String, Object>();
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ kvPair : getnamedContent())
        {
            namedOnlyContent[kvPair.Key] = (Object)kvPair.Value;
        }
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ c : getcontent())
        {
            INamedContent named = c instanceof INamedContent ? (INamedContent)c : (INamedContent)null;
            if (named != null && named.gethasValidName())
            {
                namedOnlyContent.Remove(named.getname());
            }
             
        }
        if (namedOnlyContent.Count == 0)
            namedOnlyContent = null;
         
        return namedOnlyContent;
    }

    public void setnamedOnlyContent(Dictionary<String, Object> value) throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ existingNamedOnly = getnamedOnlyContent();
        if (existingNamedOnly != null)
        {
            for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ kvPair : existingNamedOnly)
            {
                getnamedContent().Remove(kvPair.Key);
            }
        }
         
        if (value == null)
            return ;
         
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ kvPair : value)
        {
            INamedContent named = kvPair.Value instanceof INamedContent ? (INamedContent)kvPair.Value : (INamedContent)null;
            if (named != null)
                addToNamedContentOnly(named);
             
        }
    }

    private boolean __visitsShouldBeCounted = new boolean();
    public boolean getvisitsShouldBeCounted() {
        return __visitsShouldBeCounted;
    }

    public void setvisitsShouldBeCounted(boolean value) {
        __visitsShouldBeCounted = value;
    }

    private boolean __turnIndexShouldBeCounted = new boolean();
    public boolean getturnIndexShouldBeCounted() {
        return __turnIndexShouldBeCounted;
    }

    public void setturnIndexShouldBeCounted(boolean value) {
        __turnIndexShouldBeCounted = value;
    }

    private boolean __countingAtStartOnly = new boolean();
    public boolean getcountingAtStartOnly() {
        return __countingAtStartOnly;
    }

    public void setcountingAtStartOnly(boolean value) {
        __countingAtStartOnly = value;
    }

    public enum CountFlags
    {
        __dummyEnum__0,
        Visits,
        Turns,
        __dummyEnum__1,
        CountStartOnly
    }
    public int getcountFlags() throws Exception {
        CountFlags flags = 0;
        if (getvisitsShouldBeCounted())
            flags |= CountFlags.Visits;
         
        if (getturnIndexShouldBeCounted())
            flags |= CountFlags.Turns;
         
        if (getcountingAtStartOnly())
            flags |= CountFlags.CountStartOnly;
         
        // If we're only storing CountStartOnly, it serves no purpose,
        // since it's dependent on the other two to be used at all.
        // (e.g. for setting the fact that *if* a gather or choice's
        // content is counted, then is should only be counter at the start)
        // So this is just an optimisation for storage.
        if (flags == CountFlags.CountStartOnly)
        {
            flags = 0;
        }
         
        return ((Enum)flags).ordinal();
    }

    public void setcountFlags(int value) throws Exception {
        CountFlags flag = CountFlags.values()[value];
        if ((flag & CountFlags.Visits) > 0)
            setvisitsShouldBeCounted(true);
         
        if ((flag & CountFlags.Turns) > 0)
            setturnIndexShouldBeCounted(true);
         
        if ((flag & CountFlags.CountStartOnly) > 0)
            setcountingAtStartOnly(true);
         
    }

    public boolean gethasValidName() throws Exception {
        return getname() != null && getname().Length > 0;
    }

    public Path getpathToFirstLeafContent() throws Exception {
        if (_pathToFirstLeafContent == null)
            _pathToFirstLeafContent = path.PathByAppendingPath(getinternalPathToFirstLeafContent());
         
        return _pathToFirstLeafContent;
    }

    Path _pathToFirstLeafContent;
    Path getinternalPathToFirstLeafContent() throws Exception {
        Path path = new Path();
        Container container = this;
        while (container != null)
        {
            if (container.getcontent().Count > 0)
            {
                path.getcomponents().Add(new Ink.Runtime.Path.Component(0));
                container = container.getcontent()[0] instanceof Container ? (Container)container.getcontent()[0] : (Container)null;
            }
             
        }
        return path;
    }

    public Container() throws Exception {
        _content = new List<Object>();
        setnamedContent(new Dictionary<String, INamedContent>());
    }

    public void addContent(Object contentObj) throws Exception {
        getcontent().Add(contentObj);
        if (contentObj.getparent())
        {
            throw new System.Exception("content is already in " + contentObj.getparent());
        }
         
        contentObj.setparent(this);
        TryAddNamedContent(contentObj);
    }

    public void addContent(IList<Object> contentList) throws Exception {
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ c : contentList)
        {
            AddContent(c);
        }
    }

    public void insertContent(Object contentObj, int index) throws Exception {
        getcontent().Insert(index, contentObj);
        if (contentObj.getparent())
        {
            throw new System.Exception("content is already in " + contentObj.getparent());
        }
         
        contentObj.setparent(this);
        TryAddNamedContent(contentObj);
    }

    public void tryAddNamedContent(Object contentObj) throws Exception {
        INamedContent namedContentObj = contentObj instanceof INamedContent ? (INamedContent)contentObj : (INamedContent)null;
        if (namedContentObj != null && namedContentObj.gethasValidName())
        {
            addToNamedContentOnly(namedContentObj);
        }
         
    }

    public void addToNamedContentOnly(INamedContent namedContentObj) throws Exception {
        Debug.Assert(namedContentObj instanceof Object, "Can only add Runtime.Objects to a Runtime.Container");
        Object runtimeObj = (Object)namedContentObj;
        runtimeObj.setparent(this);
        getnamedContent()[namedContentObj.getname()] = namedContentObj;
    }

    public void addContentsOfContainer(Container otherContainer) throws Exception {
        getcontent().AddRange(otherContainer.getcontent());
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ obj : otherContainer.getcontent())
        {
            obj.parent = this;
            TryAddNamedContent(obj);
        }
    }

    protected Object contentWithPathComponent(Ink.Runtime.Path.Component component) throws Exception {
        if (component.getisIndex())
        {
            if (component.getindex() >= 0 && component.getindex() < getcontent().Count)
            {
                return getcontent()[component.getindex()];
            }
            else
            {
                return null;
            } 
        }
        else // When path is out of range, quietly return nil
        // (useful as we step/increment forwards through content)
        if (component.getisParent())
        {
            return this.parent;
        }
        else
        {
            INamedContent foundContent = null;
            RefSupport<INamedContent> refVar___0 = new RefSupport<INamedContent>();
            boolean boolVar___0 = getnamedContent().TryGetValue(component.getname(), refVar___0);
            foundContent = refVar___0.getValue();
            if (boolVar___0)
            {
                return (Object)foundContent;
            }
            else
            {
                throw new StoryException("Content '" + component.getname() + "' not found at path: '" + this.path + "'");
            } 
        }  
    }

    public Object contentAtPath(Path path, int partialPathLength) throws Exception {
        if (partialPathLength == -1)
            partialPathLength = path.getcomponents().Count;
         
        Container currentContainer = this;
        Object currentObj = this;
        for (int i = 0;i < partialPathLength;++i)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ comp = path.getcomponents()[i];
            if (currentContainer == null)
                throw new System.Exception("Path continued, but previous object wasn't a container: " + currentObj);
             
            currentObj = currentContainer.ContentWithPathComponent(comp);
            currentContainer = currentObj instanceof Container ? (Container)currentObj : (Container)null;
        }
        return currentObj;
    }

    public void buildStringOfHierarchy(StringBuilder sb, int indentation, Object pointedObj) throws Exception {
        Action appendIndentation = /* [UNSUPPORTED] to translate lambda expressions we need an explicit delegate type, try adding a cast "() => {
            ;
            for (int i = 0;i < spacesPerIndent * indentation;++i)
            {
                sb.Append(" ");
            }
        }" */;
        appendIndentation();
        sb.Append("[");
        if (this.gethasValidName())
        {
            sb.AppendFormat(" ({0})", this.getname());
        }
         
        if (this == pointedObj)
        {
            sb.Append("  <---");
        }
         
        sb.AppendLine();
        indentation++;
        for (int i = 0;i < getcontent().Count;++i)
        {
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ obj = getcontent()[i];
            if (obj instanceof Container)
            {
                Container container = (Container)obj;
                container.BuildStringOfHierarchy(sb, indentation, pointedObj);
            }
            else
            {
                appendIndentation();
                if (obj instanceof StringValue)
                {
                    sb.Append("\"");
                    sb.Append(obj.ToString().Replace("\n", "\\n"));
                    sb.Append("\"");
                }
                else
                {
                    sb.Append(obj.ToString());
                } 
            } 
            if (i != getcontent().Count - 1)
            {
                sb.Append(",");
            }
             
            if (!(obj instanceof Container) && obj == pointedObj)
            {
                sb.Append("  <---");
            }
             
            sb.AppendLine();
        }
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ onlyNamed = new Dictionary<String, INamedContent>();
        for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ objKV : getnamedContent())
        {
            if (getcontent().Contains((Object)objKV.Value))
            {
                continue;
            }
            else
            {
                onlyNamed.Add(objKV.Key, objKV.Value);
            } 
        }
        if (onlyNamed.Count > 0)
        {
            appendIndentation();
            sb.AppendLine("-- named: --");
            for (/* [UNSUPPORTED] 'var' as type is unsupported "var" */ objKV : onlyNamed)
            {
                Debug.Assert(objKV.Value instanceof Container, "Can only print out named Containers");
                Container container = (Container)objKV.Value;
                container.BuildStringOfHierarchy(sb, indentation, pointedObj);
                sb.AppendLine();
            }
        }
         
        indentation--;
        appendIndentation();
        sb.Append("]");
    }

    public String buildStringOfHierarchy() throws Exception {
        /* [UNSUPPORTED] 'var' as type is unsupported "var" */ sb = new StringBuilder();
        BuildStringOfHierarchy(sb, 0, null);
        return sb.ToString();
    }

}


