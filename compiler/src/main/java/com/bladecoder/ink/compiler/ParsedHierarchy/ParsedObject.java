package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.DebugMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ParsedObject {
    private DebugMetadata debugMetadata;
    private boolean alreadyHadError;
    private boolean alreadyHadWarning;
    private com.bladecoder.ink.runtime.RTObject runtimeObject;

    public DebugMetadata getDebugMetadata() {
        if (debugMetadata == null && parent != null) {
            return parent.getDebugMetadata();
        }

        return debugMetadata;
    }

    public void setDebugMetadata(DebugMetadata value) {
        debugMetadata = value;
    }

    public boolean hasOwnDebugMetadata() {
        return debugMetadata != null;
    }

    public String getTypeName() {
        return getClass().getSimpleName();
    }

    public ParsedObject parent;
    protected List<ParsedObject> content;

    public Story getStory() {
        ParsedObject ancestor = this;
        while (ancestor.parent != null) {
            ancestor = ancestor.parent;
        }
        return (Story) ancestor;
    }

    public com.bladecoder.ink.runtime.RTObject getRuntimeObject() {
        if (runtimeObject == null) {
            runtimeObject = generateRuntimeObject();
            if (runtimeObject != null) {
                runtimeObject.setDebugMetadata(getDebugMetadata());
            }
        }
        return runtimeObject;
    }

    public void setRuntimeObject(com.bladecoder.ink.runtime.RTObject value) {
        runtimeObject = value;
    }

    public com.bladecoder.ink.runtime.Path getRuntimePath() {
        return getRuntimeObject().getPath();
    }

    public Container getContainerForCounting() {
        return (Container) getRuntimeObject();
    }

    public Path pathRelativeTo(ParsedObject otherObj) {
        List<ParsedObject> ownAncestry = getAncestry();
        List<ParsedObject> otherAncestry = otherObj.getAncestry();

        ParsedObject highestCommonAncestor = null;
        int minLength = Math.min(ownAncestry.size(), otherAncestry.size());
        for (int i = 0; i < minLength; ++i) {
            ParsedObject a1 = ownAncestry.get(i);
            ParsedObject a2 = otherAncestry.get(i);
            if (a1 == a2) {
                highestCommonAncestor = a1;
            } else {
                break;
            }
        }

        FlowBase commonFlowAncestor =
                highestCommonAncestor instanceof FlowBase ? (FlowBase) highestCommonAncestor : null;
        if (commonFlowAncestor == null && highestCommonAncestor != null) {
            commonFlowAncestor = highestCommonAncestor.closestFlowBase();
        }

        List<Identifier> pathComponents = new ArrayList<>();
        boolean hasWeavePoint = false;
        FlowLevel baseFlow = FlowLevel.WeavePoint;

        ParsedObject ancestor = this;
        while (ancestor != null && ancestor != commonFlowAncestor && !(ancestor instanceof Story)) {
            if (!hasWeavePoint) {
                IWeavePoint weavePointAncestor = ancestor instanceof IWeavePoint ? (IWeavePoint) ancestor : null;
                if (weavePointAncestor != null && weavePointAncestor.getIdentifier() != null) {
                    pathComponents.add(weavePointAncestor.getIdentifier());
                    hasWeavePoint = true;
                    ancestor = ancestor.parent;
                    continue;
                }
            }

            if (ancestor instanceof FlowBase) {
                FlowBase flowAncestor = (FlowBase) ancestor;
                pathComponents.add(flowAncestor.getIdentifier());
                baseFlow = flowAncestor.getFlowLevel();
            }

            ancestor = ancestor.parent;
        }

        Collections.reverse(pathComponents);

        if (!pathComponents.isEmpty()) {
            return new Path(baseFlow, pathComponents);
        }

        return null;
    }

    public List<ParsedObject> getAncestry() {
        List<ParsedObject> result = new ArrayList<>();

        ParsedObject ancestor = parent;
        while (ancestor != null) {
            result.add(ancestor);
            ancestor = ancestor.parent;
        }

        Collections.reverse(result);

        return result;
    }

    public String getDescriptionOfScope() {
        List<String> locationNames = new ArrayList<>();

        ParsedObject ancestor = this;
        while (ancestor != null) {
            if (ancestor instanceof FlowBase) {
                FlowBase ancestorFlow = (FlowBase) ancestor;
                if (ancestorFlow.getIdentifier() != null) {
                    locationNames.add("'" + ancestorFlow.getIdentifier() + "'");
                }
            }
            ancestor = ancestor.parent;
        }

        StringBuilder scopeSB = new StringBuilder();
        if (!locationNames.isEmpty()) {
            scopeSB.append(String.join(", ", locationNames));
            scopeSB.append(" and ");
        }

        scopeSB.append("at top scope");

        return scopeSB.toString();
    }

    public <T extends ParsedObject> T addContent(T subContent) {
        if (content == null) {
            content = new ArrayList<>();
        }

        if (subContent != null) {
            subContent.parent = this;
            content.add(subContent);
        }

        return subContent;
    }

    public <T extends ParsedObject> void addContent(List<T> listContent) {
        for (T obj : listContent) {
            addContent(obj);
        }
    }

    public <T extends ParsedObject> T insertContent(int index, T subContent) {
        if (content == null) {
            content = new ArrayList<>();
        }

        subContent.parent = this;
        content.add(index, subContent);

        return subContent;
    }

    public interface FindQueryFunc<T> {
        boolean matches(T obj);
    }

    public <T> T find(Class<T> type, FindQueryFunc<T> queryFunc) {
        if (type.isInstance(this)) {
            T tObj = type.cast(this);
            if (queryFunc == null || queryFunc.matches(tObj)) {
                return tObj;
            }
        }

        if (content == null) {
            return null;
        }

        for (ParsedObject obj : content) {
            T nestedResult = obj.find(type, queryFunc);
            if (nestedResult != null) {
                return nestedResult;
            }
        }

        return null;
    }

    public <T> T find(Class<T> type) {
        return find(type, null);
    }

    public <T> List<T> findAll(Class<T> type, FindQueryFunc<T> queryFunc) {
        List<T> found = new ArrayList<>();
        findAll(type, queryFunc, found);
        return found;
    }

    public <T> List<T> findAll(Class<T> type) {
        return findAll(type, null);
    }

    private <T> void findAll(Class<T> type, FindQueryFunc<T> queryFunc, List<T> foundSoFar) {
        if (type.isInstance(this)) {
            T tObj = type.cast(this);
            if (queryFunc == null || queryFunc.matches(tObj)) {
                foundSoFar.add(tObj);
            }
        }

        if (content == null) {
            return;
        }

        for (ParsedObject obj : content) {
            obj.findAll(type, queryFunc, foundSoFar);
        }
    }

    public abstract com.bladecoder.ink.runtime.RTObject generateRuntimeObject();

    public void resolveReferences(Story context) {
        if (content != null) {
            for (ParsedObject obj : content) {
                obj.resolveReferences(context);
            }
        }
    }

    public FlowBase closestFlowBase() {
        ParsedObject ancestor = parent;
        while (ancestor != null) {
            if (ancestor instanceof FlowBase) {
                return (FlowBase) ancestor;
            }
            ancestor = ancestor.parent;
        }

        return null;
    }

    public void error(String message, ParsedObject source, boolean isWarning) {
        if (source == null) {
            source = this;
        }

        if (source.alreadyHadError && !isWarning) {
            return;
        }
        if (source.alreadyHadWarning && isWarning) {
            return;
        }

        if (parent != null) {
            parent.error(message, source, isWarning);
        } else {
            throw new RuntimeException("No parent object to send error to: " + message);
        }

        if (isWarning) {
            source.alreadyHadWarning = true;
        } else {
            source.alreadyHadError = true;
        }
    }

    public void error(String message) {
        error(message, null, false);
    }

    public void warning(String message, ParsedObject source) {
        error(message, source, true);
    }

    public void warning(String message) {
        error(message, null, true);
    }

    public List<ParsedObject> getContent() {
        return content;
    }
}
