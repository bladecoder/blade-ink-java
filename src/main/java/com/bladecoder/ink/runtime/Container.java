package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.Path.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Container extends RTObject implements INamedContent {
    private String name;

    private final List<RTObject> content;
    private HashMap<String, INamedContent> namedContent;

    private boolean visitsShouldBeCounted;
    private boolean turnIndexShouldBeCounted;
    private boolean countingAtStartOnly;

    public Container() {
        content = new ArrayList<>();
        setNamedContent(new HashMap<>());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public List<RTObject> getContent() {
        return content;
    }

    public HashMap<String, INamedContent> getNamedContent() {
        return namedContent;
    }

    public void setNamedContent(HashMap<String, INamedContent> value) {
        namedContent = value;
    }

    public HashMap<String, RTObject> getNamedOnlyContent() {

        HashMap<String, RTObject> namedOnlyContentDict = new HashMap<String, RTObject>();

        for (Entry<String, INamedContent> kvPair : getNamedContent().entrySet()) {
            namedOnlyContentDict.put(kvPair.getKey(), (RTObject) kvPair.getValue());
        }

        for (RTObject c : getContent()) {
            INamedContent named = c instanceof INamedContent ? (INamedContent) c : null;
            if (named != null && named.hasValidName()) {
                namedOnlyContentDict.remove(named.getName());
            }
        }

        return namedOnlyContentDict;
    }

    public void setNamedOnlyContent(HashMap<String, RTObject> value) {
        HashMap<String, RTObject> existingNamedOnly = getNamedOnlyContent();
        for (Entry<String, RTObject> kvPair : existingNamedOnly.entrySet()) {
            getNamedContent().remove(kvPair.getKey());
        }

        for (Entry<String, RTObject> kvPair : value.entrySet()) {
            INamedContent named = kvPair.getValue() instanceof INamedContent
                    ? (INamedContent) kvPair.getValue()
                    : (INamedContent) null;
            if (named != null) addToNamedContentOnly(named);
        }
    }

    public boolean getVisitsShouldBeCounted() {
        return visitsShouldBeCounted;
    }

    public void setVisitsShouldBeCounted(boolean value) {
        visitsShouldBeCounted = value;
    }

    public boolean getTurnIndexShouldBeCounted() {
        return turnIndexShouldBeCounted;
    }

    public void setTurnIndexShouldBeCounted(boolean value) {
        turnIndexShouldBeCounted = value;
    }

    public boolean getCountingAtStartOnly() {
        return countingAtStartOnly;
    }

    public void setCountingAtStartOnly(boolean value) {
        countingAtStartOnly = value;
    }

    public static final int COUNTFLAGS_VISITS = 1;
    public static final int COUNTFLAGS_TURNS = 2;
    public static final int COUNTFLAGS_COUNTSTARTONLY = 4;

    public int getCountFlags() {
        int flags = 0;

        if (getVisitsShouldBeCounted()) flags |= COUNTFLAGS_VISITS;

        if (getTurnIndexShouldBeCounted()) flags |= COUNTFLAGS_TURNS;

        if (getCountingAtStartOnly()) flags |= COUNTFLAGS_COUNTSTARTONLY;

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

    public void setCountFlags(int value) {

        if ((value & COUNTFLAGS_VISITS) > 0) setVisitsShouldBeCounted(true);

        if ((value & COUNTFLAGS_TURNS) > 0) setTurnIndexShouldBeCounted(true);

        if ((value & COUNTFLAGS_COUNTSTARTONLY) > 0) setCountingAtStartOnly(true);
    }

    @Override
    public boolean hasValidName() {
        return getName() != null && !getName().isEmpty();
    }

    public void addContents(List<RTObject> contentList) throws Exception {
        for (RTObject c : contentList) {
            addContent(c);
        }
    }

    private void addContent(RTObject contentObj) throws Exception {
        getContent().add(contentObj);

        if (contentObj.getParent() != null) {
            throw new Exception("content is already in " + contentObj.getParent());
        }

        contentObj.setParent(this);

        tryAddNamedContent(contentObj);
    }

    private void tryAddNamedContent(RTObject contentObj) {
        INamedContent namedContentObj = contentObj instanceof INamedContent ? (INamedContent) contentObj : null;
        if (namedContentObj != null && namedContentObj.hasValidName()) {
            addToNamedContentOnly(namedContentObj);
        }
    }

    public void addToNamedContentOnly(INamedContent namedContentObj) {
        // Debug.Assert(namedContentObj instanceof RTObject, "Can only add
        // Runtime.RTObjects to a Runtime.Container");
        RTObject runtimeObj = (RTObject) namedContentObj;

        runtimeObj.setParent(this);

        getNamedContent().put(namedContentObj.getName(), namedContentObj);
    }

    private RTObject contentWithPathComponent(Path.Component component) {

        if (component.isIndex()) {
            if (component.getIndex() >= 0 && component.getIndex() < getContent().size()) {
                return getContent().get(component.getIndex());
            } else {
                return null;
            }
        } else if (component.isParent()) {
            // When path is out of range, quietly return nil
            // (useful as we step/increment forwards through content)
            return this.getParent();
        } else {
            INamedContent foundContent = getNamedContent().get(component.getName());

            if (foundContent != null) {
                return (RTObject) foundContent;
            } else {
                return null;
            }
        }
    }

    public SearchResult contentAtPath(Path path) {
        return contentAtPath(path, 0, -1);
    }

    public SearchResult contentAtPath(Path path, int partialPathStart, int partialPathLength) {
        if (partialPathLength == -1) partialPathLength = path.getLength();

        SearchResult result = new SearchResult();
        result.approximate = false;

        Container currentContainer = this;
        RTObject currentObj = this;

        for (int i = partialPathStart; i < partialPathLength; ++i) {
            Component comp = path.getComponent(i);
            // Path component was wrong type
            if (currentContainer == null) {
                result.approximate = true;
                break;
            }

            RTObject foundObj = currentContainer.contentWithPathComponent(comp);

            // Couldn't resolve entire path?
            if (foundObj == null) {
                result.approximate = true;
                break;
            }

            // Are we about to loop into another container?
            // Is the object a container as expected? It might
            // no longer be if the content has shuffled around, so what
            // was originally a container no longer is.
            Container nextContainer = foundObj instanceof Container ? (Container) foundObj : null;
            if (i < partialPathLength - 1 && nextContainer == null) {
                result.approximate = true;
                break;
            }

            currentObj = foundObj;
            currentContainer = nextContainer;
        }

        result.obj = currentObj;

        return result;
    }

    private static final int spacesPerIndent = 4;

    private void appendIndentation(StringBuilder sb, int indentation) {
        for (int i = 0; i < spacesPerIndent * indentation; ++i) {
            sb.append(" ");
        }
    }

    public void buildStringOfHierarchy(StringBuilder sb, int indentation, RTObject pointedObj) {

        appendIndentation(sb, indentation);

        sb.append("[");
        if (this.hasValidName()) {
            sb.append(" ({");
            sb.append(this.getName());
            sb.append("})");
        }

        if (this == pointedObj) {
            sb.append("  <---");
        }

        sb.append("\n");
        indentation++;
        for (int i = 0; i < getContent().size(); ++i) {
            RTObject obj = getContent().get(i);

            if (obj instanceof Container) {
                Container container = (Container) obj;
                container.buildStringOfHierarchy(sb, indentation, pointedObj);
            } else {
                appendIndentation(sb, indentation);
                if (obj instanceof StringValue) {
                    sb.append("\"");
                    sb.append(obj.toString().replace("\n", "\\n"));
                    sb.append("\"");
                } else {
                    sb.append(obj.toString());
                }
            }
            if (i != getContent().size() - 1) {
                sb.append(",");
            }

            if (!(obj instanceof Container) && obj == pointedObj) {
                sb.append("  <---");
            }

            sb.append("\n");
        }

        HashMap<String, INamedContent> onlyNamed = new HashMap<>();

        for (Entry<String, INamedContent> objKV : getNamedContent().entrySet()) {
            if (!getContent().contains(objKV.getValue())) {
                onlyNamed.put(objKV.getKey(), objKV.getValue());
            }
        }

        if (!onlyNamed.isEmpty()) {
            appendIndentation(sb, indentation);

            sb.append("-- named: --\n");

            for (Entry<String, INamedContent> objKV : onlyNamed.entrySet()) {
                // Debug.Assert(objKV.Value instanceof Container, "Can only
                // print out named Containers");
                Container container = (Container) objKV.getValue();
                container.buildStringOfHierarchy(sb, indentation, pointedObj);
                sb.append("\n");
            }
        }

        indentation--;
        appendIndentation(sb, indentation);
        sb.append("]");
    }

    public String buildStringOfHierarchy() {
        StringBuilder sb = new StringBuilder();
        buildStringOfHierarchy(sb, 0, null);
        return sb.toString();
    }
}
