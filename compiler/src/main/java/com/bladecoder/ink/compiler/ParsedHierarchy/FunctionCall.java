package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.InkStringConversionExtensions;
import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.InkList;
import com.bladecoder.ink.runtime.ListValue;
import com.bladecoder.ink.runtime.NativeFunctionCall;
import com.bladecoder.ink.runtime.StringValue;
import java.util.List;

public class FunctionCall extends Expression {
    private final Divert proxyDivert;
    private DivertTarget divertTargetToCount;
    private VariableReference variableReferenceToCount;

    public boolean shouldPopReturnedValue;

    public FunctionCall(Identifier functionName, List<Expression> arguments) {
        proxyDivert = new Divert(new Path(functionName), arguments);
        proxyDivert.isFunctionCall = true;
        addContent(proxyDivert);
    }

    public String getName() {
        return proxyDivert.target.getFirstComponent();
    }

    public Divert getProxyDivert() {
        return proxyDivert;
    }

    public List<Expression> getArguments() {
        return proxyDivert.arguments;
    }

    public com.bladecoder.ink.runtime.Divert getRuntimeDivert() {
        return proxyDivert.runtimeDivert;
    }

    public boolean isChoiceCount() {
        return "CHOICE_COUNT".equals(getName());
    }

    public boolean isTurns() {
        return "TURNS".equals(getName());
    }

    public boolean isTurnsSince() {
        return "TURNS_SINCE".equals(getName());
    }

    public boolean isRandom() {
        return "RANDOM".equals(getName());
    }

    public boolean isSeedRandom() {
        return "SEED_RANDOM".equals(getName());
    }

    public boolean isListRange() {
        return "LIST_RANGE".equals(getName());
    }

    public boolean isListRandom() {
        return "LIST_RANDOM".equals(getName());
    }

    public boolean isReadCount() {
        return "READ_COUNT".equals(getName());
    }

    @Override
    public void generateIntoContainer(Container container) {
        ListDefinition foundList = getStory().resolveList(getName());

        boolean usingProxyDivert = false;

        if (isChoiceCount()) {
            if (getArguments().size() > 0) {
                error("The CHOICE_COUNT() function shouldn't take any arguments");
            }

            RuntimeUtils.addContent(container, ControlCommand.CommandType.ChoiceCount);

        } else if (isTurns()) {
            if (getArguments().size() > 0) {
                error("The TURNS() function shouldn't take any arguments");
            }

            RuntimeUtils.addContent(container, ControlCommand.CommandType.Turns);

        } else if (isTurnsSince() || isReadCount()) {
            DivertTarget divertTarget = getArguments().get(0) instanceof DivertTarget
                    ? (DivertTarget) getArguments().get(0)
                    : null;
            VariableReference variableDivertTarget = getArguments().get(0) instanceof VariableReference
                    ? (VariableReference) getArguments().get(0)
                    : null;

            if (getArguments().size() != 1 || (divertTarget == null && variableDivertTarget == null)) {
                error(
                        "The " + getName()
                                + "() function should take one argument: a divert target to the target knot, stitch, gather or choice you want to check. e.g. TURNS_SINCE(-> myKnot)");
                return;
            }

            if (divertTarget != null) {
                divertTargetToCount = divertTarget;
                addContent(divertTargetToCount);
                divertTargetToCount.generateIntoContainer(container);
            } else {
                variableReferenceToCount = variableDivertTarget;
                addContent(variableReferenceToCount);
                variableReferenceToCount.generateIntoContainer(container);
            }

            if (isTurnsSince()) {
                RuntimeUtils.addContent(container, ControlCommand.CommandType.TurnsSince);
            } else {
                RuntimeUtils.addContent(container, ControlCommand.CommandType.ReadCount);
            }

        } else if (isRandom()) {
            if (getArguments().size() != 2) {
                error("RANDOM should take 2 parameters: a minimum and a maximum integer");
            }

            for (int arg = 0; arg < getArguments().size(); arg++) {
                if (getArguments().get(arg) instanceof Number) {
                    Number num = (Number) getArguments().get(arg);
                    if (!(num.value instanceof Integer)) {
                        String paramName = arg == 0 ? "minimum" : "maximum";
                        error("RANDOM's " + paramName + " parameter should be an integer");
                    }
                }

                getArguments().get(arg).generateIntoContainer(container);
            }

            RuntimeUtils.addContent(container, ControlCommand.CommandType.Random);

        } else if (isSeedRandom()) {
            if (getArguments().size() != 1) {
                error("SEED_RANDOM should take 1 parameter - an integer seed");
            }

            Number num = getArguments().get(0) instanceof Number
                    ? (Number) getArguments().get(0)
                    : null;
            if (num != null && !(num.value instanceof Integer)) {
                error("SEED_RANDOM's parameter should be an integer seed");
            }

            getArguments().get(0).generateIntoContainer(container);

            RuntimeUtils.addContent(container, ControlCommand.CommandType.SeedRandom);

        } else if (isListRange()) {
            if (getArguments().size() != 3) {
                error("LIST_RANGE should take 3 parameters - a list, a min and a max");
            }

            for (Expression arg : getArguments()) {
                arg.generateIntoContainer(container);
            }

            RuntimeUtils.addContent(container, ControlCommand.CommandType.ListRange);

        } else if (isListRandom()) {
            if (getArguments().size() != 1) {
                error("LIST_RANDOM should take 1 parameter - a list");
            }

            getArguments().get(0).generateIntoContainer(container);

            RuntimeUtils.addContent(container, ControlCommand.CommandType.ListRandom);

        } else if (NativeFunctionCall.callExistsWithName(getName())) {
            NativeFunctionCall nativeCall = NativeFunctionCall.callWithName(getName());

            if (nativeCall.getNumberOfParameters() != getArguments().size()) {
                String msg = getName() + " should take " + nativeCall.getNumberOfParameters() + " parameter";
                if (nativeCall.getNumberOfParameters() > 1) {
                    msg += "s";
                }
                error(msg);
            }

            for (Expression arg : getArguments()) {
                arg.generateIntoContainer(container);
            }

            RuntimeUtils.addContent(container, NativeFunctionCall.callWithName(getName()));

        } else if (foundList != null) {
            if (getArguments().size() > 1) {
                error(
                        "Can currently only construct a list from one integer (or an empty list from a given list definition)");
            }

            if (getArguments().size() == 1) {
                RuntimeUtils.addContent(container, new StringValue(getName()));
                getArguments().get(0).generateIntoContainer(container);
                RuntimeUtils.addContent(container, ControlCommand.CommandType.ListFromInt);
            } else {
                InkList list = new InkList();
                list.setInitialOriginName(getName());
                RuntimeUtils.addContent(container, new ListValue(list));
            }

        } else {
            RuntimeUtils.addContent(container, proxyDivert.getRuntimeObject());
            usingProxyDivert = true;
        }

        if (!usingProxyDivert && content != null) {
            content.remove(proxyDivert);
        }

        if (shouldPopReturnedValue) {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.PopEvaluatedValue);
        }
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (content == null || !content.contains(proxyDivert)) {
            if (getArguments() != null) {
                for (Expression arg : getArguments()) {
                    arg.resolveReferences(context);
                }
            }
        }

        if (divertTargetToCount != null) {
            Divert divert = divertTargetToCount.divert;
            boolean attemptingTurnCountOfVariableTarget = divert.runtimeDivert.getVariableDivertName() != null;

            if (attemptingTurnCountOfVariableTarget) {
                error(
                        "When getting the TURNS_SINCE() of a variable target, remove the '->' - i.e. it should just be TURNS_SINCE("
                                + divert.runtimeDivert.getVariableDivertName() + ")");
                return;
            }

            ParsedObject targetObject = divert.targetContent;
            if (targetObject == null) {
                if (!attemptingTurnCountOfVariableTarget) {
                    error("Failed to find target for TURNS_SINCE: '" + divert.target + "'");
                }
            } else {
                targetObject.getContainerForCounting().setTurnIndexShouldBeCounted(true);
            }
        } else if (variableReferenceToCount != null) {
            com.bladecoder.ink.runtime.VariableReference runtimeVarRef = variableReferenceToCount.getRuntimeVarRef();
            if (runtimeVarRef != null && runtimeVarRef.getPathForCount() != null) {
                error("Should be " + getName() + "(-> " + variableReferenceToCount.getName()
                        + "). Usage without the '->' only makes sense for variable targets.");
            }
        }
    }

    public static boolean isBuiltIn(String name) {
        if (NativeFunctionCall.callExistsWithName(name)) {
            return true;
        }

        return "CHOICE_COUNT".equals(name)
                || "TURNS_SINCE".equals(name)
                || "TURNS".equals(name)
                || "RANDOM".equals(name)
                || "SEED_RANDOM".equals(name)
                || "LIST_VALUE".equals(name)
                || "LIST_RANDOM".equals(name)
                || "READ_COUNT".equals(name);
    }

    @Override
    public String toString() {
        String strArgs = String.join(", ", InkStringConversionExtensions.toStringsArray(getArguments()));
        return String.format("%s(%s)", getName(), strArgs);
    }
}
