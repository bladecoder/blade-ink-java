package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.NativeFunctionCall;

public class BinaryExpression extends Expression {
    public Expression leftExpression;
    public Expression rightExpression;
    public String opName;

    public BinaryExpression(Expression left, Expression right, String opName) {
        leftExpression = addContent(left);
        rightExpression = addContent(right);
        this.opName = opName;
    }

    @Override
    public void generateIntoContainer(Container container) {
        leftExpression.generateIntoContainer(container);
        rightExpression.generateIntoContainer(container);

        opName = nativeNameForOp(opName);

        RuntimeUtils.addContent(container, NativeFunctionCall.callWithName(opName));
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (nativeNameForOp(opName).equals("?")) {
            UnaryExpression leftUnary =
                    leftExpression instanceof UnaryExpression ? (UnaryExpression) leftExpression : null;
            if (leftUnary != null && (leftUnary.op.equals("not") || leftUnary.op.equals("!"))) {
                error(
                        "Using 'not' or '!' here negates '" + leftUnary.innerExpression
                                + "' rather than the result of the '?' or 'has' operator. You need to add parentheses around the (A ? B) expression.");
            }
        }
    }

    private String nativeNameForOp(String opName) {
        if (opName.equals("and")) {
            return "&&";
        }
        if (opName.equals("or")) {
            return "||";
        }
        if (opName.equals("mod")) {
            return "%";
        }
        if (opName.equals("has")) {
            return "?";
        }
        if (opName.equals("hasnt")) {
            return "!?";
        }

        return opName;
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", leftExpression, opName, rightExpression);
    }
}
