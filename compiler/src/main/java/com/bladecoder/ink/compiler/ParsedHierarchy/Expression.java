package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;

public abstract class Expression extends ParsedObject {
    private boolean outputWhenComplete;
    private Container prototypeRuntimeConstantExpression;

    public boolean isOutputWhenComplete() {
        return outputWhenComplete;
    }

    public void setOutputWhenComplete(boolean value) {
        outputWhenComplete = value;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Container container = new Container();

        RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);

        generateIntoContainer(container);

        if (outputWhenComplete) {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalOutput);
        }

        RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);

        return container;
    }

    public void generateConstantIntoContainer(Container container) {
        if (prototypeRuntimeConstantExpression == null) {
            prototypeRuntimeConstantExpression = new Container();
            generateIntoContainer(prototypeRuntimeConstantExpression);
        }

        for (com.bladecoder.ink.runtime.RTObject runtimeObj : prototypeRuntimeConstantExpression.getContent()) {
            try {
                RuntimeUtils.addContent(container, runtimeObj.copy());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public abstract void generateIntoContainer(Container container);
}
