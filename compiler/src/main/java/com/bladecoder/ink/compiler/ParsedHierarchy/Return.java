package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.Void;

public class Return extends ParsedObject {
    public Expression returnedExpression;

    public Return(Expression returnedExpression) {
        if (returnedExpression != null) {
            this.returnedExpression = addContent(returnedExpression);
        }
    }

    public Return() {
        this(null);
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Container container = new Container();

        if (returnedExpression != null) {
            RuntimeUtils.addContent(container, returnedExpression.getRuntimeObject());
        } else {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);
            RuntimeUtils.addContent(container, new Void());
            RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);
        }

        RuntimeUtils.addContent(container, ControlCommand.CommandType.PopFunction);

        return container;
    }
}
