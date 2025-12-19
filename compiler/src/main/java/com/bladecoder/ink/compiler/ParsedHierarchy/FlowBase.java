package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.DebugMetadata;
import com.bladecoder.ink.runtime.Divert;
import com.bladecoder.ink.runtime.INamedContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Base class for Knots and Stitches
public abstract class FlowBase extends ParsedObject implements INamedContent {
    public static class Argument {
        public Identifier identifier;
        public boolean isByReference;
        public boolean isDivertTarget;
    }

    protected Identifier identifier;
    protected List<Argument> arguments;
    protected boolean isFunction;
    protected Map<String, VariableAssignment> variableDeclarations;

    private Weave rootWeave;
    private Map<String, FlowBase> subFlowsByName;
    private Divert startingSubFlowDivert;
    private com.bladecoder.ink.runtime.RTObject startingSubFlowRuntime;
    private FlowBase firstChildFlow;

    public abstract FlowLevel getFlowLevel();

    @Override
    public String getName() {
        return identifier != null ? identifier.name : null;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public boolean hasParameters() {
        return arguments != null && !arguments.isEmpty();
    }

    protected FlowBase(
            Identifier name,
            List<ParsedObject> topLevelObjects,
            List<Argument> arguments,
            boolean isFunction,
            boolean isIncludedStory) {
        this.identifier = name;

        if (topLevelObjects == null) {
            topLevelObjects = new ArrayList<>();
        }

        preProcessTopLevelObjects(topLevelObjects);

        topLevelObjects = splitWeaveAndSubFlowContent(topLevelObjects, this instanceof Story && !isIncludedStory);

        addContent(topLevelObjects);

        this.arguments = arguments;
        this.isFunction = isFunction;
        this.variableDeclarations = new HashMap<>();
    }

    protected FlowBase() {
        this(null, null, null, false, false);
    }

    private List<ParsedObject> splitWeaveAndSubFlowContent(List<ParsedObject> contentObjs, boolean isRootStory) {
        List<ParsedObject> weaveObjs = new ArrayList<>();
        List<ParsedObject> subFlowObjs = new ArrayList<>();

        subFlowsByName = new HashMap<>();

        for (ParsedObject obj : contentObjs) {
            FlowBase subFlow = obj instanceof FlowBase ? (FlowBase) obj : null;
            if (subFlow != null) {
                if (firstChildFlow == null) {
                    firstChildFlow = subFlow;
                }

                subFlowObjs.add(obj);
                subFlowsByName.put(subFlow.getIdentifier() != null ? subFlow.getIdentifier().name : null, subFlow);
            } else {
                weaveObjs.add(obj);
            }
        }

        if (isRootStory) {
            weaveObjs.add(new Gather(null, 1));
            weaveObjs.add(new com.bladecoder.ink.compiler.ParsedHierarchy.Divert(new Path(Identifier.Done)));
        }

        List<ParsedObject> finalContent = new ArrayList<>();

        if (!weaveObjs.isEmpty()) {
            rootWeave = new Weave(weaveObjs, 0);
            finalContent.add(rootWeave);
        }

        if (!subFlowObjs.isEmpty()) {
            finalContent.addAll(subFlowObjs);
        }

        return finalContent;
    }

    protected void preProcessTopLevelObjects(List<ParsedObject> topLevelObjects) {}

    public static class VariableResolveResult {
        public boolean found;
        public boolean isGlobal;
        public boolean isArgument;
        public boolean isTemporary;
        public FlowBase ownerFlow;
    }

    public VariableResolveResult resolveVariableWithName(String varName, ParsedObject fromNode) {
        VariableResolveResult result = new VariableResolveResult();

        FlowBase ownerFlow = fromNode == null ? this : fromNode.closestFlowBase();

        if (ownerFlow.arguments != null) {
            for (Argument arg : ownerFlow.arguments) {
                if (arg.identifier != null && arg.identifier.name.equals(varName)) {
                    result.found = true;
                    result.isArgument = true;
                    result.ownerFlow = ownerFlow;
                    return result;
                }
            }
        }

        Story story = getStory();
        if (ownerFlow != story && ownerFlow.variableDeclarations.containsKey(varName)) {
            result.found = true;
            result.ownerFlow = ownerFlow;
            result.isTemporary = true;
            return result;
        }

        if (story.variableDeclarations.containsKey(varName)) {
            result.found = true;
            result.ownerFlow = story;
            result.isGlobal = true;
            return result;
        }

        result.found = false;
        return result;
    }

    public void tryAddNewVariableDeclaration(VariableAssignment varDecl) {
        String varName = varDecl.getVariableName();
        if (variableDeclarations.containsKey(varName)) {
            String prevDeclError = "";
            DebugMetadata debugMetadata = variableDeclarations.get(varName).getDebugMetadata();
            if (debugMetadata != null) {
                prevDeclError = " (" + variableDeclarations.get(varName).getDebugMetadata() + ")";
            }
            error(
                    "found declaration variable '" + varName + "' that was already declared" + prevDeclError,
                    varDecl,
                    false);
            return;
        }

        variableDeclarations.put(varDecl.getVariableName(), varDecl);
    }

    public void resolveWeavePointNaming() {
        if (rootWeave != null) {
            rootWeave.resolveWeavePointNaming();
        }

        if (subFlowsByName != null) {
            for (FlowBase subFlow : subFlowsByName.values()) {
                subFlow.resolveWeavePointNaming();
            }
        }
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Return foundReturn = null;
        if (isFunction) {
            checkForDisallowedFunctionFlowControl();
        } else if (getFlowLevel() == FlowLevel.Knot || getFlowLevel() == FlowLevel.Stitch) {
            foundReturn = find(Return.class);
            if (foundReturn != null) {
                error(
                        "Return statements can only be used in knots that are declared as functions: == function "
                                + this.identifier + " ==",
                        foundReturn,
                        false);
            }
        }

        Container container = new Container();
        container.setName(identifier != null ? identifier.name : null);

        if (getStory().countAllVisits) {
            container.setVisitsShouldBeCounted(true);
        }

        generateArgumentVariableAssignments(container);

        int contentIdx = 0;
        while (content != null && contentIdx < content.size()) {
            ParsedObject obj = content.get(contentIdx);

            if (obj instanceof FlowBase) {
                FlowBase childFlow = (FlowBase) obj;

                com.bladecoder.ink.runtime.RTObject childFlowRuntime = childFlow.getRuntimeObject();

                if (contentIdx == 0 && !childFlow.hasParameters() && this.getFlowLevel() == FlowLevel.Knot) {
                    startingSubFlowDivert = new Divert();
                    RuntimeUtils.addContent(container, startingSubFlowDivert);
                    startingSubFlowRuntime = childFlowRuntime;
                }

                INamedContent namedChild = (INamedContent) childFlowRuntime;
                INamedContent existingChild = container.getNamedContent().get(namedChild.getName());
                if (existingChild != null) {
                    String errorMsg = String.format(
                            "%s already contains flow named '%s' (at %s)",
                            this.getClass().getSimpleName(),
                            namedChild.getName(),
                            ((com.bladecoder.ink.runtime.RTObject) existingChild).getDebugMetadata());

                    error(errorMsg, childFlow, false);
                }

                container.addToNamedContentOnly(namedChild);
            } else {
                RuntimeUtils.addContent(container, obj.getRuntimeObject());
            }

            contentIdx++;
        }

        if (getFlowLevel() != FlowLevel.Story && !this.isFunction && rootWeave != null && foundReturn == null) {
            rootWeave.validateTermination(this::warningInTermination);
        }

        return container;
    }

    private void generateArgumentVariableAssignments(Container container) {
        if (this.arguments == null || this.arguments.isEmpty()) {
            return;
        }

        for (int i = arguments.size() - 1; i >= 0; --i) {
            String paramName = arguments.get(i).identifier != null ? arguments.get(i).identifier.name : null;

            try {
                com.bladecoder.ink.runtime.VariableAssignment assign =
                        new com.bladecoder.ink.runtime.VariableAssignment(paramName, true);
                RuntimeUtils.addContent(container, assign);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ParsedObject contentWithNameAtLevel(String name, FlowLevel level, boolean deepSearch) {
        if (level == getFlowLevel() || level == null) {
            if (identifier != null && name.equals(identifier.name)) {
                return this;
            }
        }

        if (level == FlowLevel.WeavePoint || level == null) {
            ParsedObject weavePointResult = null;

            if (rootWeave != null) {
                weavePointResult = (ParsedObject) rootWeave.weavePointNamed(name);
                if (weavePointResult != null) {
                    return weavePointResult;
                }
            }

            if (level == FlowLevel.WeavePoint) {
                return deepSearch ? deepSearchForAnyLevelContent(name) : null;
            }
        }

        if (level != null && level.ordinal() < getFlowLevel().ordinal()) {
            return null;
        }

        FlowBase subFlow = subFlowsByName.get(name);
        if (subFlow != null) {
            if (level == null || level == subFlow.getFlowLevel()) {
                return subFlow;
            }
        }

        return deepSearch ? deepSearchForAnyLevelContent(name) : null;
    }

    private ParsedObject deepSearchForAnyLevelContent(String name) {
        ParsedObject weaveResultSelf = contentWithNameAtLevel(name, FlowLevel.WeavePoint, false);
        if (weaveResultSelf != null) {
            return weaveResultSelf;
        }

        for (FlowBase subFlow : subFlowsByName.values()) {
            ParsedObject deepResult = subFlow.contentWithNameAtLevel(name, null, true);
            if (deepResult != null) {
                return deepResult;
            }
        }

        return null;
    }

    @Override
    public void resolveReferences(Story context) {
        if (startingSubFlowDivert != null) {
            startingSubFlowDivert.setTargetPath(startingSubFlowRuntime.getPath());
        }

        super.resolveReferences(context);

        if (arguments != null) {
            for (Argument arg : arguments) {
                context.checkForNamingCollisions(this, arg.identifier, Story.SymbolType.Arg, "argument");
            }

            for (int i = 0; i < arguments.size(); i++) {
                for (int j = i + 1; j < arguments.size(); j++) {
                    if (arguments.get(i).identifier != null
                            && arguments.get(j).identifier != null
                            && arguments.get(i).identifier.name.equals(arguments.get(j).identifier.name)) {
                        error("Multiple arguments with the same name: '" + arguments.get(i).identifier + "'");
                    }
                }
            }
        }

        if (getFlowLevel() != FlowLevel.Story) {
            Story.SymbolType symbolType =
                    getFlowLevel() == FlowLevel.Knot ? Story.SymbolType.Knot : Story.SymbolType.SubFlowAndWeave;
            context.checkForNamingCollisions(this, identifier, symbolType, null);
        }
    }

    private void checkForDisallowedFunctionFlowControl() {
        if (!(this instanceof Knot)) {
            error(
                    "Functions cannot be stitches - i.e. they should be defined as '== function myFunc ==' rather than public to another knot.");
        }

        for (FlowBase subFlow : subFlowsByName.values()) {
            String name = subFlow.getIdentifier() != null ? subFlow.getIdentifier().name : null;
            error(
                    "Functions may not contain stitches, but saw '" + name + "' within the function '" + this.identifier
                            + "'",
                    subFlow,
                    false);
        }

        List<com.bladecoder.ink.compiler.ParsedHierarchy.Divert> allDiverts =
                rootWeave.findAll(com.bladecoder.ink.compiler.ParsedHierarchy.Divert.class);
        for (com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert : allDiverts) {
            if (!divert.isFunctionCall && !(divert.parent instanceof DivertTarget)) {
                error("Functions may not contain diverts, but saw '" + divert + "'", divert, false);
            }
        }

        List<Choice> allChoices = rootWeave.findAll(Choice.class);
        for (Choice choice : allChoices) {
            error("Functions may not contain choices, but saw '" + choice + "'", choice, false);
        }
    }

    @Override
    public boolean hasValidName() {
        return getName() != null && !getName().isEmpty();
    }

    private void warningInTermination(ParsedObject terminatingObject) {
        String message =
                "Apparent loose end exists where the flow runs out. Do you need a '-> DONE' statement, choice or divert?";
        if (terminatingObject.parent == rootWeave && firstChildFlow != null) {
            message = message + " Note that if you intend to enter '" + firstChildFlow.identifier
                    + "' next, you need to divert to it explicitly.";
        }

        com.bladecoder.ink.compiler.ParsedHierarchy.Divert terminatingDivert =
                terminatingObject instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert
                        ? (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) terminatingObject
                        : null;
        if (terminatingDivert != null && terminatingDivert.isTunnel) {
            message = message + " When final tunnel to '" + terminatingDivert.target
                    + " ->' returns it won't have anywhere to go.";
        }

        warning(message, terminatingObject);
    }

    public Map<String, FlowBase> getSubFlowsByName() {
        return subFlowsByName;
    }

    @Override
    public String getTypeName() {
        if (isFunction) {
            return "Function";
        }
        return getFlowLevel().toString();
    }

    @Override
    public String toString() {
        return getTypeName() + " '" + identifier + "'";
    }
}
