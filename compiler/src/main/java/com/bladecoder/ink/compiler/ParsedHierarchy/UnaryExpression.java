package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.NativeFunctionCall;

public class UnaryExpression extends Expression {
    public Expression innerExpression;
    public String op;

    public static Expression withInner(Expression inner, String op) {
        Number innerNumber = inner instanceof Number ? (Number) inner : null;
        if (innerNumber != null) {
            java.lang.Object innerValue = innerNumber.value;

            if (op.equals("-")) {
                if (innerValue instanceof Integer) {
                    return new Number(-((Integer) innerValue));
                } else if (innerValue instanceof Float) {
                    return new Number(-((Float) innerValue));
                }
            } else if (op.equals("!") || op.equals("not")) {
                if (innerValue instanceof Integer) {
                    return new Number(((Integer) innerValue) == 0);
                } else if (innerValue instanceof Float) {
                    return new Number(((Float) innerValue) == 0.0f);
                } else if (innerValue instanceof Boolean) {
                    return new Number(!((Boolean) innerValue));
                }
            }

            throw new RuntimeException("Unexpected operation or number type");
        }

        return new UnaryExpression(inner, op);
    }

    public UnaryExpression(Expression inner, String op) {
        this.innerExpression = addContent(inner);
        this.op = op;
    }

    @Override
    public void generateIntoContainer(Container container) {
        innerExpression.generateIntoContainer(container);
        RuntimeUtils.addContent(container, NativeFunctionCall.callWithName(nativeNameForOp()));
    }

    @Override
    public String toString() {
        return nativeNameForOp() + innerExpression;
    }

    private String nativeNameForOp() {
        if (op.equals("-")) {
            return "_";
        }
        if (op.equals("not")) {
            return "!";
        }
        return op;
    }
}
