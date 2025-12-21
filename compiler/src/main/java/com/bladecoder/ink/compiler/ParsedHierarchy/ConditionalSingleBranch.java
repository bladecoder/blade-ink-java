package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.Divert;
import com.bladecoder.ink.runtime.NativeFunctionCall;
import com.bladecoder.ink.runtime.StringValue;
import java.util.List;

public class ConditionalSingleBranch extends ParsedObject {
    public boolean isTrueBranch;
    private Expression ownExpression;
    public boolean matchingEquality;
    public boolean isElse;
    public boolean isInline;

    public Divert returnDivert;

    public ConditionalSingleBranch(List<ParsedObject> content) {
        if (content != null) {
            innerWeave = new Weave(content);
            addContent(innerWeave);
        }
    }

    public Expression getOwnExpression() {
        return ownExpression;
    }

    public Divert getReturnDivert() {
        return returnDivert;
    }

    public void setOwnExpression(Expression value) {
        ownExpression = value;
        if (ownExpression != null) {
            addContent(ownExpression);
        }
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        if (innerWeave != null && innerWeave.getContent() != null) {
            for (ParsedObject obj : innerWeave.getContent()) {
                Text text = obj instanceof Text ? (Text) obj : null;
                if (text != null) {
                    if (text.getText().startsWith("else:")) {
                        warning(
                                "Saw the text 'else:' which is being treated as content. Did you mean '- else:'?",
                                text);
                    }
                }
            }
        }

        Container container = new Container();

        boolean duplicatesStackValue = matchingEquality && !isElse;
        if (duplicatesStackValue) {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.Duplicate);
        }

        conditionalDivert = new Divert();
        conditionalDivert.setConditional(!isElse);

        if (!isTrueBranch && !isElse) {
            boolean needsEval = ownExpression != null;
            if (needsEval) {
                RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);
            }

            if (ownExpression != null) {
                ownExpression.generateIntoContainer(container);
            }

            if (matchingEquality) {
                RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("=="));
            }

            if (needsEval) {
                RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);
            }
        }

        RuntimeUtils.addContent(container, conditionalDivert);

        contentContainer = generateRuntimeForContent();
        contentContainer.setName("b");

        if (!isInline) {
            try {
                contentContainer.insertContent(new StringValue("\n"), 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (duplicatesStackValue || (isElse && matchingEquality)) {
            try {
                contentContainer.insertContent(new ControlCommand(ControlCommand.CommandType.PopEvaluatedValue), 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        container.addToNamedContentOnly(contentContainer);

        returnDivert = new Divert();
        RuntimeUtils.addContent(contentContainer, returnDivert);

        return container;
    }

    public Container generateRuntimeForContent() {
        if (innerWeave == null) {
            return new Container();
        }
        innerWeave.getRuntimeObject();
        return innerWeave.rootContainer;
    }

    @Override
    public void resolveReferences(Story context) {
        conditionalDivert.setTargetPath(contentContainer.getPath());

        super.resolveReferences(context);
    }

    private Container contentContainer;
    private Divert conditionalDivert;
    private Weave innerWeave;
}
