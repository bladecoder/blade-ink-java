package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.NativeFunctionCall;
import java.util.ArrayList;
import java.util.List;

public class MultipleConditionExpression extends Expression {
    public List<Expression> getSubExpressions() {
        List<Expression> result = new ArrayList<>();
        if (content != null) {
            for (ParsedObject obj : content) {
                result.add((Expression) obj);
            }
        }
        return result;
    }

    public MultipleConditionExpression(List<Expression> conditionExpressions) {
        addContent(conditionExpressions);
    }

    @Override
    public void generateIntoContainer(Container container) {
        boolean isFirst = true;
        for (Expression conditionExpr : getSubExpressions()) {
            conditionExpr.generateIntoContainer(container);

            if (!isFirst) {
                RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("&&"));
            }

            isFirst = false;
        }
    }
}
