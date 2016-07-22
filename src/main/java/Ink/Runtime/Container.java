package Ink.Runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Container extends RTObject implements INamedContent {
	private String __name = new String();

	List<RTObject> _content = new ArrayList<RTObject>();
	private HashMap<String, INamedContent> __namedContent = new HashMap<String, INamedContent>();

	private boolean __visitsShouldBeCounted;
	private boolean __turnIndexShouldBeCounted;
	private boolean __countingAtStartOnly;
	

	public Container() throws Exception {
		_content = new ArrayList<RTObject>();
		setnamedContent(new HashMap<String, INamedContent>());
	}

	public String getname() {
		return __name;
	}

	public void setname(String value) {
		__name = value;
	}

	public List<RTObject> getcontent() throws Exception {
		return _content;
	}

	public void setcontent(List<RTObject> value) throws Exception {
		AddContent(value);
	}

	public HashMap<String, INamedContent> getnamedContent() {
		return __namedContent;
	}

	public void setnamedContent(HashMap<String, INamedContent> value) {
		__namedContent = value;
	}

	public HashMap<String, RTObject> getnamedOnlyContent() throws Exception {

		HashMap<String, RTObject> namedOnlyContent = new HashMap<String, RTObject>();
        
		for (Entry<String, INamedContent> kvPair : getnamedContent().entrySet())
        {
            namedOnlyContent.put(kvPair.getKey(),(RTObject)kvPair.getValue());
        }
        
		for (RTObject c : getcontent())
        {
            INamedContent named = c instanceof INamedContent ? (INamedContent)c : (INamedContent)null;
            if (named != null && named.gethasValidName())
            {
                namedOnlyContent.remove(named.getname());
            }
             
        }
        
		if (namedOnlyContent.size() == 0)
            namedOnlyContent = null;
         
        return namedOnlyContent;
    }

	public void setnamedOnlyContent(HashMap<String, RTObject> value) throws Exception {
		HashMap<String, RTObject> existingNamedOnly = getnamedOnlyContent();
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

	public boolean getvisitsShouldBeCounted() {
		return __visitsShouldBeCounted;
	}

	public void setvisitsShouldBeCounted(boolean value) {
		__visitsShouldBeCounted = value;
	}

	public boolean getturnIndexShouldBeCounted() {
		return __turnIndexShouldBeCounted;
	}

	public void setturnIndexShouldBeCounted(boolean value) {
		__turnIndexShouldBeCounted = value;
	}

	public boolean getcountingAtStartOnly() {
		return __countingAtStartOnly;
	}

	public void setcountingAtStartOnly(boolean value) {
		__countingAtStartOnly = value;
	}

	public enum CountFlags {
		__dummyEnum__0, Visits, Turns, __dummyEnum__1, CountStartOnly
	}

	public int getcountFlags() throws Exception {
		CountFlags flags = 0;
		
		if (getvisitsShouldBesizeCounted())
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
		if (flags == CountFlags.CountStartOnly) {
			flags = 0;
		}

		return ((Enum) flags).ordinal();
	}

	public void setcountFlags(int value) throws Exception {
		CountFlags flag = CountFlags.values()[value];
		if ((flag & CountFlags.Visits) > 0)
			setvisitsShouldBeCounted(true);

		if ((flag & CountFlags.Turns) > 0)
			setturnIndexShouldBesizeCounted(true);

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
		while (container != null) {
			if (container.getcontent().size() > 0) {
				path.getcomponents().Add(new Ink.Runtime.Path.Component(0));
				container = container.getcontent()[0] instanceof Container ? (Container) container.getcontent()[0]
						: (Container) null;
			}

		}
		return path;
	}

	public void addContent(RTObject contentObj) throws Exception {
		getcontent().Add(contentObj);
		if (contentObj.getparent()) {
			throw new System.Exception("content is already in " + contentObj.getparent());
		}

		contentObj.setparent(this);
		TryAddNamedContent(contentObj);
	}

	public void addContent(List<RTObject> contentList) throws Exception {
        for (RTObject c : contentList)
        {
            AddContent(c);
        }
    }

	public void insertContent(RTObject contentObj, int index) throws Exception {
		getcontent().Insert(index, contentObj);
		if (contentObj.getparent()) {
			throw new System.Exception("content is already in " + contentObj.getparent());
		}

		contentObj.setparent(this);
		TryAddNamedContent(contentObj);
	}

	public void tryAddNamedContent(RTObject contentObj) throws Exception {
		INamedContent namedContentObj = contentObj instanceof INamedContent ? (INamedContent) contentObj
				: (INamedContent) null;
		if (namedContentObj != null && namedContentObj.gethasValidName()) {
			addToNamedContentOnly(namedContentObj);
		}

	}

	public void addToNamedContentOnly(INamedContent namedContentObj) throws Exception {
		Debug.Assert(namedContentObj instanceof RTObject, "Can only add Runtime.RTObjects to a Runtime.Container");
		RTObject runtimeObj = (RTObject) namedContentObj;
		runtimeObj.setparent(this);
		getnamedContent()[namedContentObj.getname()] = namedContentObj;
	}

	public void addContentsOfContainer(Container otherContainer) throws Exception {
        getcontent().AddRange(otherContainer.getcontent());
        for (RTObject obj : otherContainer.getcontent())
        {
            obj.parent = this;
            TryAddNamedContent(obj);
        }
    }

	protected RTObject contentWithPathComponent(Ink.Runtime.Path.Component component) throws Exception {
		if (component.getisIndex()) {
			if (component.getindex() >= 0 && component.getindex() < getcontent().size()) {
				return getcontent()[component.getindex()];
			} else {
				return null;
			}
		} else // When path is out of range, quietly return nil
		// (useful as we step/increment forwards through content)
		if (component.getisParent()) {
			return this.parent;
		} else {
			INamedContent foundContent = null;
			RefSupport<INamedContent> refVar___0 = new RefSupport<INamedContent>();
			boolean boolVar___0 = getnamedContent().TryGetValue(component.getname(), refVar___0);
			foundContent = refVar___0.getValue();
			if (boolVar___0) {
				return (RTObject) foundContent;
			} else {
				throw new StoryException(
						"Content '" + component.getname() + "' not found at path: '" + this.path + "'");
			}
		}
	}
	
	public RTObject contentAtPath(Path path) throws Exception {
		return contentAtPath(path, -1);
	}

	public RTObject contentAtPath(Path path, int partialPathLength) throws Exception {
		if (partialPathLength == -1)
			partialPathLength = path.getcomponents().size();

		Container currentContainer = this;
		RTObject currentObj = this;
		for (int i = 0; i < partialPathLength; ++i) {
			/* [UNSUPPORTED] 'var' as type is unsupported "var" */ comp = path.getcomponents()[i];
			if (currentContainer == null)
				throw new System.Exception("Path continued, but previous RTObject wasn't a container: " + currentObj);

			currentObj = currentContainer.ContentWithPathComponent(comp);
			currentContainer = currentObj instanceof Container ? (Container) currentObj : (Container) null;
		}
		return currentObj;
	}

	private final static int spacesPerIndent = 4;
	
	private void appendIndetation(StringBuilder sb, int indentation) {
		for (int i = 0;i < spacesPerIndent * indentation;++i)
        {
            sb.Append(" ");
        }
	}
	
	public void buildStringOfHierarchy(StringBuilder sb, int indentation, RTObject pointedObj) throws Exception {
        
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
        for (int i = 0;i < getcontent().size();++i)
        {
            RTObject obj = getcontent().get(i);
            
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
            if (i != getcontent().size() - 1)
            {
                sb.Append(",");
            }
             
            if (!(obj instanceof Container) && obj == pointedObj)
            {
                sb.Append("  <---");
            }
             
            sb.AppendLine();
        }

        HashMap<String, INamedContent> onlyNamed = new HashMap<String, INamedContent>();
        
        for (RTObject objKV : getnamedContent())
        {
            if (getcontent().Contains((RTObject)objKV.Value))
            {
                continue;
            }
            else
            {
                onlyNamed.Add(objKV.Key, objKV.Value);
            } 
        }
        if (onlyNamed.size() > 0)
        {
            appendIndentation();
            sb.AppendLine("-- named: --");
            for (RTObject objKV : onlyNamed)
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
		StringBuilder sb = new StringBuilder();
		BuildStringOfHierarchy(sb, 0, null);
		return sb.ToString();
	}

}
