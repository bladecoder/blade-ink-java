package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.DivertTargetValue;

public class DivertTarget extends Expression {
    public Divert divert;

    private DivertTargetValue runtimeDivertTargetValue;
    private com.bladecoder.ink.runtime.Divert runtimeDivert;

    public DivertTarget(Divert divert) {
        this.divert = addContent(divert);
    }

    @Override
    public void generateIntoContainer(Container container) {
        divert.generateRuntimeObject();

        runtimeDivert = divert.runtimeDivert;
        runtimeDivertTargetValue = new DivertTargetValue();

        RuntimeUtils.addContent(container, runtimeDivertTargetValue);
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (divert.isDone() || divert.isEnd()) {
            error("Can't use -> DONE or -> END as variable divert targets", this, false);
            return;
        }

        ParsedObject usageContext = this;
        while (usageContext != null && usageContext instanceof Expression) {
            boolean badUsage = false;
            boolean foundUsage = false;

            ParsedObject usageParent = usageContext.parent;
            if (usageParent instanceof BinaryExpression) {
                BinaryExpression binaryExprParent = (BinaryExpression) usageParent;
                if (!binaryExprParent.opName.equals("==") && !binaryExprParent.opName.equals("!=")) {
                    badUsage = true;
                } else {
                    if (!(binaryExprParent.leftExpression instanceof DivertTarget
                            || binaryExprParent.leftExpression instanceof VariableReference)) {
                        badUsage = true;
                    }
                    if (!(binaryExprParent.rightExpression instanceof DivertTarget
                            || binaryExprParent.rightExpression instanceof VariableReference)) {
                        badUsage = true;
                    }
                }
                foundUsage = true;
            } else if (usageParent instanceof FunctionCall) {
                FunctionCall funcCall = (FunctionCall) usageParent;
                if (!funcCall.isTurnsSince() && !funcCall.isReadCount()) {
                    badUsage = true;
                }
                foundUsage = true;
            } else if (usageParent instanceof Expression) {
                badUsage = true;
                foundUsage = true;
            } else if (usageParent instanceof MultipleConditionExpression) {
                badUsage = true;
                foundUsage = true;
            } else if (usageParent instanceof Choice && ((Choice) usageParent).getCondition() == usageContext) {
                badUsage = true;
                foundUsage = true;
            } else if (usageParent instanceof Conditional || usageParent instanceof ConditionalSingleBranch) {
                badUsage = true;
                foundUsage = true;
            }

            if (badUsage) {
                error(
                        "Can't use a divert target like that. Did you intend to call '" + divert.target
                                + "' as a function: likeThis(), or check the read count: likeThis, with no arrows?",
                        this,
                        false);
            }

            if (foundUsage) {
                break;
            }

            usageContext = usageParent;
        }

        if (runtimeDivert.hasVariableTarget()) {
            error(
                    "Since '" + divert.target.getDotSeparatedComponents()
                            + "' is a variable, it shouldn't be preceded by '->' here.",
                    this,
                    false);
        }

        try {
            runtimeDivertTargetValue.setTargetPath(runtimeDivert.getTargetPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ParsedObject targetContent = divert.targetContent;
        if (targetContent != null) {
            com.bladecoder.ink.runtime.Container target = targetContent.getContainerForCounting();
            if (target != null) {
                FunctionCall parentFunc = parent instanceof FunctionCall ? (FunctionCall) parent : null;
                if (parentFunc != null && parentFunc.isTurnsSince()) {
                    target.setTurnIndexShouldBeCounted(true);
                } else {
                    target.setVisitsShouldBeCounted(true);
                    target.setTurnIndexShouldBeCounted(true);
                }
            }

            FlowBase targetFlow = targetContent instanceof FlowBase ? (FlowBase) targetContent : null;
            if (targetFlow != null && targetFlow.getArguments() != null) {
                for (FlowBase.Argument arg : targetFlow.getArguments()) {
                    if (arg.isByReference) {
                        error(
                                "Can't store a divert target to a knot or function that has by-reference arguments ('"
                                        + targetFlow.getIdentifier() + "' has 'ref " + arg.identifier + "').",
                                this,
                                false);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        DivertTarget otherDivTarget = obj instanceof DivertTarget ? (DivertTarget) obj : null;
        if (otherDivTarget == null) {
            return false;
        }

        String targetStr = divert.target.getDotSeparatedComponents();
        String otherTargetStr = otherDivTarget.divert.target.getDotSeparatedComponents();

        return targetStr.equals(otherTargetStr);
    }

    @Override
    public int hashCode() {
        String targetStr = divert.target.getDotSeparatedComponents();
        return targetStr.hashCode();
    }
}
