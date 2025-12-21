package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.Divert;
import com.bladecoder.ink.runtime.DivertTargetValue;
import com.bladecoder.ink.runtime.RTObject;
import com.bladecoder.ink.runtime.VariableAssignment;

public class Choice extends ParsedObject implements IWeavePoint, INamedContent {
    public ContentList startContent;
    public ContentList choiceOnlyContent;
    public ContentList innerContent;

    public Identifier identifier;

    private Expression condition;

    public boolean onceOnly;
    public boolean isInvisibleDefault;

    public int indentationDepth;
    public boolean hasWeaveStyleInlineBrackets;

    private com.bladecoder.ink.runtime.ChoicePoint runtimeChoice;
    private Container innerContentContainer;
    private Container outerContainer;
    private Container startContentRuntimeContainer;
    private Divert divertToStartContentOuter;
    private Divert divertToStartContentInner;
    private Container r1Label;
    private Container r2Label;
    private DivertTargetValue returnToR1;
    private DivertTargetValue returnToR2;

    public Choice(ContentList startContent, ContentList choiceOnlyContent, ContentList innerContent) {
        this.startContent = startContent;
        this.choiceOnlyContent = choiceOnlyContent;
        this.innerContent = innerContent;
        this.indentationDepth = 1;

        if (startContent != null) {
            addContent(this.startContent);
        }

        if (choiceOnlyContent != null) {
            addContent(this.choiceOnlyContent);
        }

        if (innerContent != null) {
            addContent(this.innerContent);
        }

        this.onceOnly = true;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression value) {
        condition = value;
        if (condition != null) {
            addContent(condition);
        }
    }

    @Override
    public String getName() {
        return identifier != null ? identifier.name : null;
    }

    @Override
    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public int getIndentationDepth() {
        return indentationDepth;
    }

    @Override
    public Container getRuntimeContainer() {
        return innerContentContainer;
    }

    @Override
    public Container getContainerForCounting() {
        if (innerContentContainer != null) {
            return innerContentContainer;
        }
        return super.getContainerForCounting();
    }

    @Override
    public com.bladecoder.ink.runtime.Path getRuntimePath() {
        if (innerContentContainer != null) {
            return innerContentContainer.getPath();
        }
        return super.getRuntimePath();
    }

    public Container getInnerContentContainer() {
        return innerContentContainer;
    }

    @Override
    public RTObject generateRuntimeObject() {
        outerContainer = new Container();

        runtimeChoice = new com.bladecoder.ink.runtime.ChoicePoint(onceOnly);
        runtimeChoice.setIsInvisibleDefault(isInvisibleDefault);

        if (startContent != null || choiceOnlyContent != null || condition != null) {
            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.EvalStart);
        }

        if (startContent != null) {
            returnToR1 = new DivertTargetValue();
            RuntimeUtils.addContent(outerContainer, returnToR1);
            try {
                VariableAssignment varAssign = new VariableAssignment("$r", true);
                RuntimeUtils.addContent(outerContainer, varAssign);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.BeginString);

            divertToStartContentOuter = new Divert();
            RuntimeUtils.addContent(outerContainer, divertToStartContentOuter);

            startContentRuntimeContainer = (Container) startContent.getRuntimeObject();
            startContentRuntimeContainer.setName("s");

            Divert varDivert = new Divert();
            varDivert.setVariableDivertName("$r");
            RuntimeUtils.addContent(startContentRuntimeContainer, varDivert);

            outerContainer.addToNamedContentOnly(startContentRuntimeContainer);

            r1Label = new Container();
            r1Label.setName("$r1");
            RuntimeUtils.addContent(outerContainer, r1Label);

            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.EndString);

            runtimeChoice.setHasStartContent(true);
        }

        if (choiceOnlyContent != null) {
            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.BeginString);

            Container choiceOnlyRuntimeContent = (Container) choiceOnlyContent.getRuntimeObject();
            outerContainer.addContentsOfContainer(choiceOnlyRuntimeContent);

            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.EndString);

            runtimeChoice.setHasChoiceOnlyContent(true);
        }

        if (condition != null) {
            condition.generateIntoContainer(outerContainer);
            runtimeChoice.setHasCondition(true);
        }

        if (startContent != null || choiceOnlyContent != null || condition != null) {
            RuntimeUtils.addContent(outerContainer, ControlCommand.CommandType.EvalEnd);
        }

        RuntimeUtils.addContent(outerContainer, runtimeChoice);

        innerContentContainer = new Container();

        if (startContent != null) {
            returnToR2 = new DivertTargetValue();
            RuntimeUtils.addContent(innerContentContainer, ControlCommand.CommandType.EvalStart);
            RuntimeUtils.addContent(innerContentContainer, returnToR2);
            RuntimeUtils.addContent(innerContentContainer, ControlCommand.CommandType.EvalEnd);
            try {
                VariableAssignment varAssign = new VariableAssignment("$r", true);
                RuntimeUtils.addContent(innerContentContainer, varAssign);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            divertToStartContentInner = new Divert();
            RuntimeUtils.addContent(innerContentContainer, divertToStartContentInner);

            r2Label = new Container();
            r2Label.setName("$r2");
            RuntimeUtils.addContent(innerContentContainer, r2Label);
        }

        if (innerContent != null) {
            Container innerChoiceOnlyContent = (Container) innerContent.getRuntimeObject();
            innerContentContainer.addContentsOfContainer(innerChoiceOnlyContent);
        }

        if (getStory().countAllVisits) {
            innerContentContainer.setVisitsShouldBeCounted(true);
        }

        innerContentContainer.setCountingAtStartOnly(true);

        return outerContainer;
    }

    @Override
    public void resolveReferences(Story context) {
        if (innerContentContainer != null) {
            runtimeChoice.setPathOnChoice(innerContentContainer.getPath());

            if (onceOnly) {
                innerContentContainer.setVisitsShouldBeCounted(true);
            }
        }

        if (returnToR1 != null) {
            returnToR1.setTargetPath(r1Label.getPath());
        }

        if (returnToR2 != null) {
            returnToR2.setTargetPath(r2Label.getPath());
        }

        if (divertToStartContentOuter != null) {
            divertToStartContentOuter.setTargetPath(startContentRuntimeContainer.getPath());
        }

        if (divertToStartContentInner != null) {
            divertToStartContentInner.setTargetPath(startContentRuntimeContainer.getPath());
        }

        super.resolveReferences(context);

        if (identifier != null && identifier.name.length() > 0) {
            context.checkForNamingCollisions(this, identifier, Story.SymbolType.SubFlowAndWeave, null);
        }
    }

    @Override
    public String toString() {
        if (choiceOnlyContent != null) {
            return String.format("* %s[%s]...", startContent, choiceOnlyContent);
        }
        return String.format("* %s...", startContent);
    }
}
