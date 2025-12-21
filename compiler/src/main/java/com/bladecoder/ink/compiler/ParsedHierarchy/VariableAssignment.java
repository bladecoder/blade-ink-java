package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;

public class VariableAssignment extends ParsedObject {
    public Identifier variableIdentifier;
    public Expression expression;
    public ListDefinition listDefinition;

    public boolean isGlobalDeclaration;
    public boolean isNewTemporaryDeclaration;

    private com.bladecoder.ink.runtime.VariableAssignment runtimeAssignment;

    public VariableAssignment(Identifier identifier, Expression assignedExpression) {
        this.variableIdentifier = identifier;

        if (assignedExpression != null) {
            this.expression = addContent(assignedExpression);
        }
    }

    public VariableAssignment(Identifier identifier, ListDefinition listDef) {
        this.variableIdentifier = identifier;

        if (listDef != null) {
            this.listDefinition = addContent(listDef);
            this.listDefinition.variableAssignment = this;
        }

        isGlobalDeclaration = true;
    }

    public String getVariableName() {
        return variableIdentifier.name;
    }

    public boolean isDeclaration() {
        return isGlobalDeclaration || isNewTemporaryDeclaration;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        FlowBase newDeclScope = null;
        if (isGlobalDeclaration) {
            newDeclScope = getStory();
        } else if (isNewTemporaryDeclaration) {
            newDeclScope = closestFlowBase();
        }

        if (newDeclScope != null) {
            newDeclScope.tryAddNewVariableDeclaration(this);
        }

        if (isGlobalDeclaration) {
            return null;
        }

        Container container = new Container();

        if (expression != null) {
            RuntimeUtils.addContent(container, expression.getRuntimeObject());
        } else if (listDefinition != null) {
            RuntimeUtils.addContent(container, listDefinition.getRuntimeObject());
        }

        try {
            runtimeAssignment =
                    new com.bladecoder.ink.runtime.VariableAssignment(getVariableName(), isNewTemporaryDeclaration);
            RuntimeUtils.addContent(container, runtimeAssignment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return container;
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (isDeclaration() && listDefinition == null) {
            context.checkForNamingCollisions(
                    this, variableIdentifier, isGlobalDeclaration ? Story.SymbolType.Var : Story.SymbolType.Temp, null);
        }

        if (isGlobalDeclaration) {
            VariableReference variableReference =
                    expression instanceof VariableReference ? (VariableReference) expression : null;
            if (variableReference != null
                    && !variableReference.isConstantReference
                    && !variableReference.isListItemReference) {
                error(
                        "global variable assignments cannot refer to other variables, only literal values, constants and list items");
            }
        }

        if (!isNewTemporaryDeclaration) {
            FlowBase.VariableResolveResult resolvedVarAssignment =
                    context.resolveVariableWithName(getVariableName(), this);
            if (!resolvedVarAssignment.found) {
                if (getStory().constants.containsKey(getVariableName())) {
                    error(
                            "Can't re-assign to a constant (do you need to use VAR when declaring '" + getVariableName()
                                    + "'?)",
                            this,
                            false);
                } else {
                    error("Variable could not be found to assign to: '" + getVariableName() + "'", this, false);
                }
            }

            if (runtimeAssignment != null) {
                runtimeAssignment.setIsGlobal(resolvedVarAssignment.isGlobal);
            }
        }
    }

    @Override
    public String getTypeName() {
        if (isNewTemporaryDeclaration) {
            return "temp";
        }
        if (isGlobalDeclaration) {
            return "VAR";
        }
        return "variable assignment";
    }
}
