package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import java.util.ArrayList;
import java.util.List;

public class VariableReference extends Expression {
    private final String name;
    private Identifier singleIdentifier;

    public List<Identifier> pathIdentifiers;
    public List<String> path;

    public boolean isConstantReference;
    public boolean isListItemReference;

    private com.bladecoder.ink.runtime.VariableReference runtimeVarRef;

    public VariableReference(List<Identifier> pathIdentifiers) {
        this.pathIdentifiers = pathIdentifiers;
        this.path = new ArrayList<>();
        for (Identifier id : pathIdentifiers) {
            this.path.add(id != null ? id.name : null);
        }
        this.name = String.join(".", this.path);
    }

    public String getName() {
        return name;
    }

    public Identifier getIdentifier() {
        if (pathIdentifiers == null || pathIdentifiers.isEmpty()) {
            return null;
        }

        if (singleIdentifier == null) {
            String joinedName = String.join(".", path);
            Identifier first = pathIdentifiers.get(0);
            com.bladecoder.ink.runtime.DebugMetadata debugMetadata = first != null ? first.debugMetadata : null;
            for (Identifier id : pathIdentifiers) {
                if (id != null && id.debugMetadata != null && debugMetadata != null) {
                    debugMetadata = debugMetadata.merge(id.debugMetadata);
                } else if (debugMetadata == null && id != null) {
                    debugMetadata = id.debugMetadata;
                }
            }
            singleIdentifier = new Identifier();
            singleIdentifier.name = joinedName;
            singleIdentifier.debugMetadata = debugMetadata;
        }

        return singleIdentifier;
    }

    public com.bladecoder.ink.runtime.VariableReference getRuntimeVarRef() {
        return runtimeVarRef;
    }

    @Override
    public void generateIntoContainer(com.bladecoder.ink.runtime.Container container) {
        Expression constantValue = getStory().constants.get(name);

        if (constantValue != null) {
            constantValue.generateConstantIntoContainer(container);
            isConstantReference = true;
            return;
        }

        runtimeVarRef = new com.bladecoder.ink.runtime.VariableReference(name);

        if (path.size() == 1 || path.size() == 2) {
            String listItemName;
            String listName = null;

            if (path.size() == 1) {
                listItemName = path.get(0);
            } else {
                listName = path.get(0);
                listItemName = path.get(1);
            }

            ListElementDefinition listItem = getStory().resolveListItem(listName, listItemName, this);
            if (listItem != null) {
                isListItemReference = true;
            }
        }

        RuntimeUtils.addContent(container, runtimeVarRef);
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (isConstantReference || isListItemReference) {
            return;
        }

        Path parsedPath = new Path(pathIdentifiers);
        ParsedObject targetForCount = parsedPath.resolveFromContext(this);
        if (targetForCount != null) {
            targetForCount.getContainerForCounting().setVisitsShouldBeCounted(true);

            if (runtimeVarRef == null) {
                return;
            }

            runtimeVarRef.setPathForCount(targetForCount.getRuntimePath());
            runtimeVarRef.setName(null);

            FlowBase targetFlow = targetForCount instanceof FlowBase ? (FlowBase) targetForCount : null;
            if (targetFlow != null && targetFlow.isFunction()) {
                if (parent instanceof Weave || parent instanceof ContentList || parent instanceof FlowBase) {
                    warning(
                            "'" + targetFlow.getIdentifier()
                                    + "' being used as read count rather than being called as function. Perhaps you intended to write "
                                    + targetFlow.getName() + "()",
                            this);
                }
            }

            return;
        }

        if (path.size() > 1) {
            String errorMsg = "Could not find target for read count: " + parsedPath;
            if (path.size() <= 2) {
                errorMsg += ", or couldn't find list item with the name " + String.join(",", path);
            }
            error(errorMsg);
            return;
        }

        if (!context.resolveVariableWithName(name, this).found) {
            error("Unresolved variable: " + toString(), this, false);
        }
    }

    @Override
    public String toString() {
        return String.join(".", path);
    }
}
