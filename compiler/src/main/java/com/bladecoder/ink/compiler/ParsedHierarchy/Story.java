package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.Error.ErrorHandler;
import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.ListDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Story extends FlowBase {
    public Map<String, Expression> constants;
    public Map<String, ExternalDeclaration> externals;

    public boolean countAllVisits = false;

    private ErrorHandler errorHandler;
    private boolean hadError;
    private boolean hadWarning;

    private HashSet<Container> dontFlattenContainers = new HashSet<>();
    private Map<String, com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition> listDefs;

    public Story(List<ParsedObject> topLevelObjects, boolean isInclude) {
        super(null, topLevelObjects, null, false, isInclude);
    }

    public Story() {
        super(null, null, null, false, false);
    }

    @Override
    public FlowLevel getFlowLevel() {
        return FlowLevel.Story;
    }

    @Override
    protected void preProcessTopLevelObjects(List<ParsedObject> topLevelContent) {
        List<FlowBase> flowsFromOtherFiles = new ArrayList<>();

        int i = 0;
        while (i < topLevelContent.size()) {
            ParsedObject obj = topLevelContent.get(i);
            if (obj instanceof IncludedFile) {
                IncludedFile file = (IncludedFile) obj;

                topLevelContent.remove(i);

                if (file.includedStory != null) {
                    List<ParsedObject> nonFlowContent = new ArrayList<>();
                    Story subStory = file.includedStory;

                    if (subStory.content != null) {
                        for (ParsedObject subStoryObj : subStory.content) {
                            if (subStoryObj instanceof FlowBase) {
                                flowsFromOtherFiles.add((FlowBase) subStoryObj);
                            } else {
                                nonFlowContent.add(subStoryObj);
                            }
                        }

                        nonFlowContent.add(new Text("\n"));

                        topLevelContent.addAll(i, nonFlowContent);

                        i += nonFlowContent.size();
                    }
                }

                continue;
            }

            i++;
        }

        topLevelContent.addAll(flowsFromOtherFiles);
    }

    public com.bladecoder.ink.runtime.Story exportRuntime(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

        constants = new HashMap<>();
        for (ConstantDeclaration constDecl : findAll(ConstantDeclaration.class)) {
            String constName = constDecl.getConstantName();
            Expression existingDefinition = constants.get(constName);
            if (existingDefinition != null) {
                if (!existingDefinition.equals(constDecl.expression)) {
                    String errorMsg = String.format(
                            "CONST '%s' has been redefined with a different value. Multiple definitions of the same CONST are valid so long as they contain the same value. Initial definition was on %s.",
                            constName, existingDefinition.getDebugMetadata());
                    error(errorMsg, constDecl, false);
                }
            }

            constants.put(constName, constDecl.expression);
        }

        listDefs = new HashMap<>();
        for (com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition listDef :
                findAll(com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition.class)) {
            listDefs.put(listDef.identifier != null ? listDef.identifier.name : null, listDef);
        }

        externals = new HashMap<>();

        resolveWeavePointNaming();

        Container rootContainer = (Container) getRuntimeObject();

        Container variableInitialisation = new Container();
        RuntimeUtils.addContent(variableInitialisation, ControlCommand.CommandType.EvalStart);

        List<ListDefinition> runtimeLists = new ArrayList<>();
        for (Map.Entry<String, VariableAssignment> nameDeclPair : variableDeclarations.entrySet()) {
            String varName = nameDeclPair.getKey();
            VariableAssignment varDecl = nameDeclPair.getValue();
            if (varDecl.isGlobalDeclaration) {
                if (varDecl.listDefinition != null) {
                    listDefs.put(varName, varDecl.listDefinition);
                    RuntimeUtils.addContent(variableInitialisation, varDecl.listDefinition.getRuntimeObject());
                    runtimeLists.add(varDecl.listDefinition.getRuntimeListDefinition());
                } else {
                    varDecl.expression.generateIntoContainer(variableInitialisation);
                }

                try {
                    com.bladecoder.ink.runtime.VariableAssignment runtimeVarAss =
                            new com.bladecoder.ink.runtime.VariableAssignment(varName, true);
                    runtimeVarAss.setIsGlobal(true);
                    RuntimeUtils.addContent(variableInitialisation, runtimeVarAss);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        RuntimeUtils.addContent(variableInitialisation, ControlCommand.CommandType.EvalEnd);
        RuntimeUtils.addContent(variableInitialisation, ControlCommand.CommandType.End);

        if (!variableDeclarations.isEmpty()) {
            variableInitialisation.setName("global decl");
            rootContainer.addToNamedContentOnly(variableInitialisation);
        }

        RuntimeUtils.addContent(rootContainer, ControlCommand.CommandType.Done);

        com.bladecoder.ink.runtime.Story runtimeStory =
                new com.bladecoder.ink.runtime.Story(rootContainer, runtimeLists);

        if (hadError) {
            return null;
        }

        flattenContainersIn(rootContainer);

        resolveReferences(this);

        if (hadError) {
            return null;
        }

        try {
            runtimeStory.resetState();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return runtimeStory;
    }

    public com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition resolveList(String listName) {
        com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition list = listDefs.get(listName);
        if (list == null) {
            return null;
        }
        return list;
    }

    public ListElementDefinition resolveListItem(String listName, String itemName, ParsedObject source) {
        com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition listDef = null;

        if (listName != null) {
            listDef = listDefs.get(listName);
            if (listDef == null) {
                return null;
            }

            return listDef.itemNamed(itemName);
        }

        ListElementDefinition foundItem = null;
        com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition originalFoundList = null;

        for (Map.Entry<String, com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition> namedList :
                listDefs.entrySet()) {
            com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition listToSearch = namedList.getValue();
            ListElementDefinition itemInThisList = listToSearch.itemNamed(itemName);
            if (itemInThisList != null) {
                if (foundItem != null) {
                    error(
                            "Ambiguous item name '" + itemName + "' found in multiple sets, including "
                                    + originalFoundList.identifier + " and " + listToSearch.identifier,
                            source,
                            false);
                } else {
                    foundItem = itemInThisList;
                    originalFoundList = listToSearch;
                }
            }
        }

        return foundItem;
    }

    private void flattenContainersIn(Container container) {
        HashSet<Container> innerContainers = new HashSet<>();

        for (com.bladecoder.ink.runtime.RTObject c : container.getContent()) {
            Container innerContainer = c instanceof Container ? (Container) c : null;
            if (innerContainer != null) {
                innerContainers.add(innerContainer);
            }
        }

        if (container.getNamedContent() != null) {
            for (com.bladecoder.ink.runtime.INamedContent namedContent :
                    container.getNamedContent().values()) {
                Container namedInnerContainer = namedContent instanceof Container ? (Container) namedContent : null;
                if (namedInnerContainer != null) {
                    innerContainers.add(namedInnerContainer);
                }
            }
        }

        for (Container innerContainer : innerContainers) {
            tryFlattenContainer(innerContainer);
            flattenContainersIn(innerContainer);
        }
    }

    private void tryFlattenContainer(Container container) {
        if (!container.getNamedContent().isEmpty()
                || container.hasValidName()
                || dontFlattenContainers.contains(container)) {
            return;
        }

        Container parentContainer = container.getParent();
        if (parentContainer != null) {
            int contentIdx = parentContainer.getContent().indexOf(container);
            parentContainer.getContent().remove(contentIdx);

            com.bladecoder.ink.runtime.DebugMetadata dm = container.getOwnDebugMetadata();

            for (com.bladecoder.ink.runtime.RTObject innerContent : container.getContent()) {
                innerContent.setParent(null);
                if (dm != null && innerContent.getOwnDebugMetadata() == null) {
                    innerContent.setDebugMetadata(dm);
                }
                try {
                    parentContainer.insertContent(innerContent, contentIdx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                contentIdx++;
            }
        }
    }

    @Override
    public void error(String message, ParsedObject source, boolean isWarning) {
        ErrorType errorType = isWarning ? ErrorType.Warning : ErrorType.Error;

        StringBuilder sb = new StringBuilder();
        if (source instanceof AuthorWarning) {
            sb.append("TODO: ");
            errorType = ErrorType.Author;
        } else if (isWarning) {
            sb.append("WARNING: ");
        } else {
            sb.append("ERROR: ");
        }

        if (source != null && source.getDebugMetadata() != null && source.getDebugMetadata().startLineNumber >= 1) {
            if (source.getDebugMetadata().fileName != null) {
                sb.append("'").append(source.getDebugMetadata().fileName).append("' ");
            }

            sb.append("line ").append(source.getDebugMetadata().startLineNumber).append(": ");
        }

        sb.append(message);

        String fullMessage = sb.toString();

        if (errorHandler != null) {
            hadError = errorType == ErrorType.Error;
            hadWarning = errorType == ErrorType.Warning;
            errorHandler.error(fullMessage, errorType);
        } else {
            throw new RuntimeException(fullMessage);
        }
    }

    public void resetError() {
        hadError = false;
        hadWarning = false;
    }

    public boolean hadError() {
        return hadError;
    }

    public boolean hadWarning() {
        return hadWarning;
    }

    public boolean isExternal(String namedFuncTarget) {
        return externals.containsKey(namedFuncTarget);
    }

    public void addExternal(ExternalDeclaration decl) {
        if (externals.containsKey(decl.getName())) {
            error("Duplicate EXTERNAL definition of '" + decl.getName() + "'", decl, false);
        } else {
            externals.put(decl.getName(), decl);
        }
    }

    public void dontFlattenContainer(Container container) {
        dontFlattenContainers.add(container);
    }

    private void nameConflictError(ParsedObject obj, String name, ParsedObject existingObj, String typeNameToPrint) {
        obj.error(typeNameToPrint + " '" + name + "': name has already been used for a "
                + existingObj.getTypeName().toLowerCase() + " on " + existingObj.getDebugMetadata());
    }

    public static boolean isReservedKeyword(String name) {
        if (name == null) {
            return false;
        }
        switch (name) {
            case "true":
            case "false":
            case "not":
            case "return":
            case "else":
            case "VAR":
            case "CONST":
            case "temp":
            case "LIST":
            case "function":
                return true;
            default:
                return false;
        }
    }

    public enum SymbolType {
        Knot,
        List,
        ListItem,
        Var,
        SubFlowAndWeave,
        Arg,
        Temp
    }

    public void checkForNamingCollisions(
            ParsedObject obj, Identifier identifier, SymbolType symbolType, String typeNameOverride) {
        String typeNameToPrint = typeNameOverride != null ? typeNameOverride : obj.getTypeName();
        if (isReservedKeyword(identifier != null ? identifier.name : null)) {
            obj.error("'" + (identifier != null ? identifier.name : null) + "' cannot be used for the name of a "
                    + typeNameToPrint.toLowerCase() + " because it's a reserved keyword");
            return;
        }

        if (FunctionCall.isBuiltIn(identifier != null ? identifier.name : null)) {
            obj.error("'" + (identifier != null ? identifier.name : null) + "' cannot be used for the name of a "
                    + typeNameToPrint.toLowerCase() + " because it's a built in function");
            return;
        }

        FlowBase knotOrFunction =
                (FlowBase) contentWithNameAtLevel(identifier != null ? identifier.name : null, FlowLevel.Knot, false);
        if (knotOrFunction != null && (knotOrFunction != obj || symbolType == SymbolType.Arg)) {
            nameConflictError(obj, identifier != null ? identifier.name : null, knotOrFunction, typeNameToPrint);
            return;
        }

        if (symbolType.ordinal() < SymbolType.List.ordinal()) {
            return;
        }

        for (Map.Entry<String, com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition> namedListDef :
                listDefs.entrySet()) {
            String listDefName = namedListDef.getKey();
            com.bladecoder.ink.compiler.ParsedHierarchy.ListDefinition listDef = namedListDef.getValue();
            if (identifier != null
                    && identifier.name.equals(listDefName)
                    && obj != listDef
                    && listDef.variableAssignment != obj) {
                nameConflictError(obj, identifier.name, listDef, typeNameToPrint);
            }

            if (!(obj instanceof ListElementDefinition)) {
                for (ListElementDefinition item : listDef.itemDefinitions) {
                    if (identifier != null && identifier.name.equals(item.getName())) {
                        nameConflictError(obj, identifier.name, item, typeNameToPrint);
                    }
                }
            }
        }

        if (symbolType.ordinal() <= SymbolType.Var.ordinal()) {
            return;
        }

        VariableAssignment varDecl = variableDeclarations.get(identifier != null ? identifier.name : null);
        if (varDecl != null && varDecl != obj && varDecl.isGlobalDeclaration && varDecl.listDefinition == null) {
            nameConflictError(obj, identifier.name, varDecl, typeNameToPrint);
        }

        if (symbolType.ordinal() < SymbolType.SubFlowAndWeave.ordinal()) {
            return;
        }

        Path path = new Path(identifier);
        ParsedObject targetContent = path.resolveFromContext(obj);
        if (targetContent != null && targetContent != obj) {
            nameConflictError(obj, identifier.name, targetContent, typeNameToPrint);
            return;
        }

        if (symbolType.ordinal() < SymbolType.Arg.ordinal()) {
            return;
        }

        if (symbolType != SymbolType.Arg) {
            FlowBase flow = obj instanceof FlowBase ? (FlowBase) obj : obj.closestFlowBase();
            if (flow != null && flow.hasParameters()) {
                for (FlowBase.Argument arg : flow.getArguments()) {
                    if (arg.identifier != null && identifier != null && identifier.name.equals(arg.identifier.name)) {
                        obj.error(typeNameToPrint + " '" + identifier.name
                                + "': Name has already been used for a argument to " + flow.getIdentifier() + " on "
                                + flow.getDebugMetadata());
                        return;
                    }
                }
            }
        }
    }
}
