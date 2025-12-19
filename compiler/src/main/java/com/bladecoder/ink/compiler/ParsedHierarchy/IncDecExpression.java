package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.NativeFunctionCall;
import com.bladecoder.ink.runtime.VariableAssignment;
import com.bladecoder.ink.runtime.VariableReference;

public class IncDecExpression extends Expression {
    public Identifier varIdentifier;
    public boolean isInc;
    public Expression expression;
    private VariableAssignment runtimeAssignment;

    public IncDecExpression(Identifier varIdentifier, boolean isInc) {
        this.varIdentifier = varIdentifier;
        this.isInc = isInc;
    }

    public IncDecExpression(Identifier varIdentifier, Expression expression, boolean isInc) {
        this(varIdentifier, isInc);
        this.expression = expression;
        addContent(expression);
    }

    @Override
    public void generateIntoContainer(Container container) {
        RuntimeUtils.addContent(container, new VariableReference(varIdentifier != null ? varIdentifier.name : null));

        if (expression != null) {
            expression.generateIntoContainer(container);
        } else {
            RuntimeUtils.addContent(container, new com.bladecoder.ink.runtime.IntValue(1));
        }

        RuntimeUtils.addContent(container, NativeFunctionCall.callWithName(isInc ? "+" : "-"));

        try {
            runtimeAssignment = new VariableAssignment(varIdentifier != null ? varIdentifier.name : null, false);
            RuntimeUtils.addContent(container, runtimeAssignment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        FlowBase.VariableResolveResult varResolveResult =
                context.resolveVariableWithName(varIdentifier != null ? varIdentifier.name : null, this);
        if (!varResolveResult.found) {
            error("variable for " + incrementDecrementWord() + " could not be found: '" + varIdentifier
                    + "' after searching: " + getDescriptionOfScope());
        }

        runtimeAssignment.setIsGlobal(varResolveResult.isGlobal);

        if (!(parent instanceof Weave) && !(parent instanceof FlowBase) && !(parent instanceof ContentList)) {
            error("Can't use " + incrementDecrementWord() + " as sub-expression");
        }
    }

    private String incrementDecrementWord() {
        return isInc ? "increment" : "decrement";
    }

    @Override
    public String toString() {
        if (expression != null) {
            return varIdentifier + (isInc ? " += " : " -= ") + expression;
        }
        return varIdentifier + (isInc ? "++" : "--");
    }
}
