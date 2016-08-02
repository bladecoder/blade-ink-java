package Ink.Runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import Ink.Runtime.Path.Component;

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
		addContent(value);
	}

	public HashMap<String, INamedContent> getnamedContent() {
		return __namedContent;
	}

	public void setnamedContent(HashMap<String, INamedContent> value) {
		__namedContent = value;
	}

	public HashMap<String, RTObject> getnamedOnlyContent() throws Exception {

		HashMap<String, RTObject> namedOnlyContent = new HashMap<String, RTObject>();

		for (Entry<String, INamedContent> kvPair : getnamedContent().entrySet()) {
			namedOnlyContent.put(kvPair.getKey(), (RTObject) kvPair.getValue());
		}

		for (RTObject c : getcontent()) {
			INamedContent named = c instanceof INamedContent ? (INamedContent) c : (INamedContent) null;
			if (named != null && named.gethasValidName()) {
				namedOnlyContent.remove(named.getname());
			}

		}

		if (namedOnlyContent.size() == 0)
			namedOnlyContent = null;

		return namedOnlyContent;
	}

	public void setnamedOnlyContent(HashMap<String, RTObject> value) throws Exception {
		HashMap<String, RTObject> existingNamedOnly = getnamedOnlyContent();
		if (existingNamedOnly != null) {
			for (Entry<String, RTObject> kvPair : existingNamedOnly.entrySet()) {
				getnamedContent().remove(kvPair.getKey());
			}
		}

		if (value == null)
			return;

		for (Entry<String, RTObject> kvPair : value.entrySet()) {
			INamedContent named = kvPair.getValue() instanceof INamedContent ? (INamedContent) kvPair.getValue()
					: (INamedContent) null;
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

	public static final int COUNTFLAGS_VISITS = 1;
	public static final int COUNTFLAGS_TURNS = 2;
	public static final int COUNTFLAGS_COUNTSTARTONLY = 4;

	public int getcountFlags() throws Exception {
		int flags = 0;

		if (getvisitsShouldBeCounted())
			flags |= COUNTFLAGS_VISITS;

		if (getturnIndexShouldBeCounted())
			flags |= COUNTFLAGS_TURNS;

		if (getcountingAtStartOnly())
			flags |= COUNTFLAGS_COUNTSTARTONLY;

		// If we're only storing CountStartOnly, it serves no purpose,
		// since it's dependent on the other two to be used at all.
		// (e.g. for setting the fact that *if* a gather or choice's
		// content is counted, then is should only be counter at the start)
		// So this is just an optimisation for storage.
		if (flags == COUNTFLAGS_COUNTSTARTONLY) {
			flags = 0;
		}

		return flags;
	}

	public void setcountFlags(int value) throws Exception {
		int flag = value;

		if ((flag & COUNTFLAGS_VISITS) > 0)
			setvisitsShouldBeCounted(true);

		if ((flag & COUNTFLAGS_TURNS) > 0)
			setturnIndexShouldBeCounted(true);

		if ((flag & COUNTFLAGS_COUNTSTARTONLY) > 0)
			setcountingAtStartOnly(true);

	}

	public boolean gethasValidName() throws Exception {
		return getname() != null && getname().length() > 0;
	}

	public Path getpathToFirstLeafContent() throws Exception {
		if (_pathToFirstLeafContent == null)
			_pathToFirstLeafContent = path.pathByAppendingPath(getinternalPathToFirstLeafContent());

		return _pathToFirstLeafContent;
	}

	Path _pathToFirstLeafContent;

	Path getinternalPathToFirstLeafContent() throws Exception {
		Path path = new Path();
		Container container = this;
		while (container != null) {
			if (container.getcontent().size() > 0) {
				path.getcomponents().add(new Ink.Runtime.Path.Component(0));
				container = container.getcontent().get(0) instanceof Container
						? (Container) container.getcontent().get(0) : (Container) null;
			}

		}
		return path;
	}

	public void addContent(RTObject contentObj) throws Exception {
		getcontent().add(contentObj);

		if (contentObj.getparent() != null) {
			throw new Exception("content is already in " + contentObj.getparent());
		}

		contentObj.setparent(this);

		tryAddNamedContent(contentObj);
	}

	public void addContent(List<RTObject> contentList) throws Exception {
		for (RTObject c : contentList) {
			addContent(c);
		}
	}

	public void insertContent(RTObject contentObj, int index) throws Exception {
		getcontent().add(index, contentObj);
		if (contentObj.getparent() != null) {
			throw new Exception("content is already in " + contentObj.getparent());
		}

		contentObj.setparent(this);
		tryAddNamedContent(contentObj);
	}

	public void tryAddNamedContent(RTObject contentObj) throws Exception {
		INamedContent namedContentObj = contentObj instanceof INamedContent ? (INamedContent) contentObj
				: (INamedContent) null;
		if (namedContentObj != null && namedContentObj.gethasValidName()) {
			addToNamedContentOnly(namedContentObj);
		}

	}

	public void addToNamedContentOnly(INamedContent namedContentObj) throws Exception {
		// Debug.Assert(namedContentObj instanceof RTObject, "Can only add
		// Runtime.RTObjects to a Runtime.Container");
		RTObject runtimeObj = (RTObject) namedContentObj;

		runtimeObj.setparent(this);

		getnamedContent().put(namedContentObj.getname(), namedContentObj);
	}

	public void addContentsOfContainer(Container otherContainer) throws Exception {
		getcontent().addAll(otherContainer.getcontent());

		for (RTObject obj : otherContainer.getcontent()) {
			obj.setparent(this);

			tryAddNamedContent(obj);
		}
	}

	protected RTObject contentWithPathComponent(Path.Component component) throws Exception {
	
		if (component.getisIndex()) {
			if (component.getindex() >= 0 && component.getindex() < getcontent().size()) {
				return getcontent().get(component.getindex());
			} else {
				return null;
			}
		} else if (component.getisParent()) { 
			// When path is out of range, quietly return nil
			// (useful as we step/increment forwards through content)
			return this.getparent();
		} else {
			INamedContent foundContent = getnamedContent().get(component.getname());
			
			if (foundContent != null) {
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
			Component comp = path.getcomponents().get(i);
			if (currentContainer == null)
				throw new Exception("Path continued, but previous RTObject wasn't a container: " + currentObj);

			currentObj = currentContainer.contentWithPathComponent(comp);
			currentContainer = currentObj instanceof Container ? (Container) currentObj : (Container) null;
		}
		return currentObj;
	}

	private final static int spacesPerIndent = 4;

	private void appendIndentation(StringBuilder sb, int indentation) {
		for (int i = 0; i < spacesPerIndent * indentation; ++i) {
			sb.append(" ");
		}
	}

	public void buildStringOfHierarchy(StringBuilder sb, int indentation, RTObject pointedObj) throws Exception {

		appendIndentation(sb, indentation);
		
		sb.append("[");
		if (this.gethasValidName()) {
			sb.append(" ({");
			sb.append(this.getname());
			sb.append("})");
		}

		if (this == pointedObj) {
			sb.append("  <---");
		}

		sb.append("\n");
		indentation++;
		for (int i = 0; i < getcontent().size(); ++i) {
			RTObject obj = getcontent().get(i);

			if (obj instanceof Container) {
				Container container = (Container) obj;
				container.buildStringOfHierarchy(sb, indentation, pointedObj);
			} else {
				appendIndentation(sb, indentation);
				if (obj instanceof StringValue) {
					sb.append("\"");
					sb.append(obj.toString().replaceAll("\n", "\\n"));
					sb.append("\"");
				} else {
					sb.append(obj.toString());
				}
			}
			if (i != getcontent().size() - 1) {
				sb.append(",");
			}

			if (!(obj instanceof Container) && obj == pointedObj) {
				sb.append("  <---");
			}

			sb.append("\n");
		}

		HashMap<String, INamedContent> onlyNamed = new HashMap<String, INamedContent>();

		for (Entry<String, INamedContent> objKV : getnamedContent().entrySet()) {
			if (getcontent().contains((RTObject) objKV.getValue())) {
				continue;
			} else {
				onlyNamed.put(objKV.getKey(), objKV.getValue());
			}
		}
		if (onlyNamed.size() > 0) {
			appendIndentation(sb, indentation);
			
			sb.append("-- named: --\n");
			
			for (Entry<String, INamedContent>  objKV : onlyNamed.entrySet()) {
				//Debug.Assert(objKV.Value instanceof Container, "Can only print out named Containers");
				Container container = (Container) objKV.getValue();
				container.buildStringOfHierarchy(sb, indentation, pointedObj);
				sb.append("\n");
			}
		}

		indentation--;
		appendIndentation(sb, indentation);
		sb.append("]");
	}

	public String buildStringOfHierarchy() throws Exception {
		StringBuilder sb = new StringBuilder();
		buildStringOfHierarchy(sb, 0, null);
		return sb.toString();
	}

}
