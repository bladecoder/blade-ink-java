package com.bladecoder.ink.compiler.ParsedHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Path {
    private FlowLevel baseTargetLevel;
    private String dotSeparatedComponents;
    public final List<Identifier> components;

    public FlowLevel getBaseTargetLevel() {
        if (isBaseLevelAmbiguous()) {
            return FlowLevel.Story;
        }
        return baseTargetLevel;
    }

    public boolean isBaseLevelAmbiguous() {
        return baseTargetLevel == null;
    }

    public String getFirstComponent() {
        if (components == null || components.isEmpty()) {
            return null;
        }

        return components.get(0).name;
    }

    public int getNumberOfComponents() {
        return components.size();
    }

    public String getDotSeparatedComponents() {
        if (dotSeparatedComponents == null) {
            dotSeparatedComponents =
                    components.stream().map(c -> c == null ? null : c.name).collect(Collectors.joining("."));
        }

        return dotSeparatedComponents;
    }

    public Path(FlowLevel baseFlowLevel, List<Identifier> components) {
        baseTargetLevel = baseFlowLevel;
        this.components = components;
    }

    public Path(List<Identifier> components) {
        baseTargetLevel = null;
        this.components = components;
    }

    public Path(Identifier ambiguousName) {
        baseTargetLevel = null;
        components = new ArrayList<>();
        components.add(ambiguousName);
    }

    @Override
    public String toString() {
        if (components == null || components.isEmpty()) {
            if (getBaseTargetLevel() == FlowLevel.WeavePoint) {
                return "-> <next gather point>";
            }
            return "<invalid Path>";
        }

        return "-> " + getDotSeparatedComponents();
    }

    public ParsedObject resolveFromContext(ParsedObject context) {
        if (components == null || components.isEmpty()) {
            return null;
        }

        ParsedObject baseTargetObject = resolveBaseTarget(context);
        if (baseTargetObject == null) {
            return null;
        }

        if (components.size() > 1) {
            return resolveTailComponents(baseTargetObject);
        }

        return baseTargetObject;
    }

    private ParsedObject resolveBaseTarget(ParsedObject originalContext) {
        String firstComp = getFirstComponent();

        ParsedObject ancestorContext = originalContext;
        while (ancestorContext != null) {
            boolean deepSearch = ancestorContext == originalContext;

            ParsedObject foundBase = tryGetChildFromContext(ancestorContext, firstComp, null, deepSearch);
            if (foundBase != null) {
                return foundBase;
            }

            ancestorContext = ancestorContext.parent;
        }

        return null;
    }

    private ParsedObject resolveTailComponents(ParsedObject rootTarget) {
        ParsedObject foundComponent = rootTarget;
        for (int i = 1; i < components.size(); ++i) {
            String compName = components.get(i).name;

            FlowLevel minimumExpectedLevel;
            FlowBase foundFlow = foundComponent instanceof FlowBase ? (FlowBase) foundComponent : null;
            if (foundFlow != null) {
                minimumExpectedLevel =
                        FlowLevel.values()[foundFlow.getFlowLevel().ordinal() + 1];
            } else {
                minimumExpectedLevel = FlowLevel.WeavePoint;
            }

            foundComponent = tryGetChildFromContext(foundComponent, compName, minimumExpectedLevel, false);
            if (foundComponent == null) {
                break;
            }
        }

        return foundComponent;
    }

    private ParsedObject tryGetChildFromContext(
            ParsedObject context, String childName, FlowLevel minimumLevel, boolean forceDeepSearch) {
        boolean ambiguousChildLevel = minimumLevel == null;

        Weave weaveContext = context instanceof Weave ? (Weave) context : null;
        if (weaveContext != null && (ambiguousChildLevel || minimumLevel == FlowLevel.WeavePoint)) {
            return (ParsedObject) weaveContext.weavePointNamed(childName);
        }

        FlowBase flowContext = context instanceof FlowBase ? (FlowBase) context : null;
        if (flowContext != null) {
            boolean shouldDeepSearch = forceDeepSearch || flowContext.getFlowLevel() == FlowLevel.Knot;
            return flowContext.contentWithNameAtLevel(childName, minimumLevel, shouldDeepSearch);
        }

        return null;
    }
}
