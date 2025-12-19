package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.PushPopType;
import com.bladecoder.ink.runtime.VariablePointerValue;
import java.util.ArrayList;
import java.util.List;

public class Divert extends ParsedObject {
    public Path target;
    public ParsedObject targetContent;
    public List<Expression> arguments;
    public com.bladecoder.ink.runtime.Divert runtimeDivert;
    public boolean isFunctionCall;
    public boolean isEmpty;
    public boolean isTunnel;
    public boolean isThread;

    public Divert(Path target, List<Expression> arguments) {
        this.target = target;
        this.arguments = arguments;

        if (arguments != null) {
            List<ParsedObject> argObjects = new ArrayList<>();
            argObjects.addAll(arguments);
            addContent(argObjects);
        }
    }

    public Divert(Path target) {
        this(target, null);
    }

    public Divert(ParsedObject targetContent) {
        this.targetContent = targetContent;
    }

    public boolean isEnd() {
        return target != null && "END".equals(target.getDotSeparatedComponents());
    }

    public boolean isDone() {
        return target != null && "DONE".equals(target.getDotSeparatedComponents());
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        if (isEnd()) {
            return new com.bladecoder.ink.runtime.ControlCommand(ControlCommand.CommandType.End);
        }
        if (isDone()) {
            return new com.bladecoder.ink.runtime.ControlCommand(ControlCommand.CommandType.Done);
        }

        runtimeDivert = new com.bladecoder.ink.runtime.Divert();

        resolveTargetContent();

        checkArgumentValidity();

        boolean requiresArgCodeGen = arguments != null && !arguments.isEmpty();
        if (requiresArgCodeGen || isFunctionCall || isTunnel || isThread) {
            Container container = new Container();

            if (requiresArgCodeGen) {
                if (!isFunctionCall) {
                    RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);
                }

                List<FlowBase.Argument> targetArguments = null;
                if (targetContent != null && targetContent instanceof FlowBase) {
                    targetArguments = ((FlowBase) targetContent).getArguments();
                }

                for (int i = 0; i < arguments.size(); ++i) {
                    Expression argToPass = arguments.get(i);
                    FlowBase.Argument argExpected = null;
                    if (targetArguments != null && i < targetArguments.size()) {
                        argExpected = targetArguments.get(i);
                    }

                    if (argExpected != null && argExpected.isByReference) {
                        VariableReference varRef =
                                argToPass instanceof VariableReference ? (VariableReference) argToPass : null;
                        if (varRef == null) {
                            error("Expected variable name to pass by reference to 'ref " + argExpected.identifier
                                    + "' but saw " + argToPass);
                            break;
                        }

                        Path targetPath = new Path(varRef.pathIdentifiers);
                        ParsedObject targetForCount = targetPath.resolveFromContext(this);
                        if (targetForCount != null) {
                            error("can't pass a read count by reference. '" + targetPath.getDotSeparatedComponents()
                                    + "' is a knot/stitch/label, but '" + target.getDotSeparatedComponents()
                                    + "' requires the name of a VAR to be passed.");
                            break;
                        }

                        VariablePointerValue varPointer = new VariablePointerValue(varRef.getName());
                        RuntimeUtils.addContent(container, varPointer);
                    } else {
                        argToPass.generateIntoContainer(container);
                    }
                }

                if (!isFunctionCall) {
                    RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);
                }
            }

            if (isThread) {
                RuntimeUtils.addContent(container, ControlCommand.CommandType.StartThread);
            } else if (isFunctionCall || isTunnel) {
                runtimeDivert.setPushesToStack(true);
                runtimeDivert.setStackPushType(isFunctionCall ? PushPopType.Function : PushPopType.Tunnel);
            }

            RuntimeUtils.addContent(container, runtimeDivert);

            return container;
        }

        return runtimeDivert;
    }

    public String pathAsVariableName() {
        return target.getFirstComponent();
    }

    private void resolveTargetContent() {
        if (isEmpty || isEnd()) {
            return;
        }

        if (targetContent == null) {
            String variableTargetName = pathAsVariableName();
            if (variableTargetName != null) {
                FlowBase flowBaseScope = closestFlowBase();
                FlowBase.VariableResolveResult resolveResult =
                        flowBaseScope.resolveVariableWithName(variableTargetName, this);
                if (resolveResult.found) {
                    if (resolveResult.isArgument) {
                        for (FlowBase.Argument arg : resolveResult.ownerFlow.getArguments()) {
                            if (arg.identifier != null && variableTargetName.equals(arg.identifier.name)) {
                                if (!arg.isDivertTarget) {
                                    error(
                                            "Since '" + arg.identifier + "' is used as a variable divert target (on "
                                                    + getDebugMetadata() + "), it should be marked as: -> "
                                                    + arg.identifier,
                                            resolveResult.ownerFlow,
                                            false);
                                }
                                break;
                            }
                        }
                    }

                    runtimeDivert.setVariableDivertName(variableTargetName);
                    return;
                }
            }

            targetContent = target.resolveFromContext(this);
        }
    }

    @Override
    public void resolveReferences(Story context) {
        if (isEmpty || isEnd() || isDone()) {
            return;
        }

        if (targetContent != null) {
            runtimeDivert.setTargetPath(targetContent.getRuntimePath());
        }

        super.resolveReferences(context);

        FlowBase targetFlow = targetContent instanceof FlowBase ? (FlowBase) targetContent : null;
        if (targetFlow != null) {
            if (!targetFlow.isFunction() && isFunctionCall) {
                error(targetFlow.getIdentifier()
                        + " hasn't been marked as a function, but it's being called as one. Do you need to delcare the knot as '== function "
                        + targetFlow.getIdentifier() + " =='?");
            } else if (targetFlow.isFunction() && !isFunctionCall && !(parent instanceof DivertTarget)) {
                error(targetFlow.getIdentifier()
                        + " can't be diverted to. It can only be called as a function since it's been marked as such: '"
                        + targetFlow.getIdentifier() + "(...)'");
            }
        }

        boolean targetWasFound = targetContent != null;
        boolean isBuiltIn = false;
        boolean isExternal = false;

        if (target.getNumberOfComponents() == 1) {
            isBuiltIn = FunctionCall.isBuiltIn(target.getFirstComponent());
            isExternal = context.isExternal(target.getFirstComponent());

            if (isBuiltIn || isExternal) {
                if (!isFunctionCall) {
                    error(target.getFirstComponent() + " must be called as a function: ~ " + target.getFirstComponent()
                            + "()");
                }
                if (isExternal) {
                    runtimeDivert.setExternal(true);
                    if (arguments != null) {
                        runtimeDivert.setExternalArgs(arguments.size());
                    }
                    runtimeDivert.setPushesToStack(false);
                    runtimeDivert.setTargetPath(new com.bladecoder.ink.runtime.Path(target.getFirstComponent()));
                    checkExternalArgumentValidity(context);
                }
                return;
            }
        }

        if (runtimeDivert.getVariableDivertName() != null) {
            return;
        }

        if (!targetWasFound && !isBuiltIn && !isExternal) {
            error("target not found: '" + target + "'");
        }
    }

    private void checkArgumentValidity() {
        if (isEmpty) {
            return;
        }

        int numArgs = 0;
        if (arguments != null && !arguments.isEmpty()) {
            numArgs = arguments.size();
        }

        if (targetContent == null) {
            return;
        }

        FlowBase targetFlow = targetContent instanceof FlowBase ? (FlowBase) targetContent : null;

        if (numArgs == 0 && (targetFlow == null || !targetFlow.hasParameters())) {
            return;
        }

        if (targetFlow == null && numArgs > 0) {
            error("target needs to be a knot or stitch in order to pass arguments");
            return;
        }

        if (targetFlow.getArguments() == null && numArgs > 0) {
            error("target (" + targetFlow.getName() + ") doesn't take parameters");
            return;
        }

        if (parent instanceof DivertTarget) {
            if (numArgs > 0) {
                error("can't store arguments in a divert target variable");
            }
            return;
        }

        int paramCount = targetFlow.getArguments().size();
        if (paramCount != numArgs) {
            String butClause;
            if (numArgs == 0) {
                butClause = "but there weren't any passed to it";
            } else if (numArgs < paramCount) {
                butClause = "but only got " + numArgs;
            } else {
                butClause = "but got " + numArgs;
            }
            error("to '" + targetFlow.getIdentifier() + "' requires " + paramCount + " arguments, " + butClause);
            return;
        }

        for (int i = 0; i < paramCount; ++i) {
            FlowBase.Argument flowArg = targetFlow.getArguments().get(i);
            Expression divArgExpr = arguments.get(i);

            if (flowArg.isDivertTarget) {
                VariableReference varRef =
                        divArgExpr instanceof VariableReference ? (VariableReference) divArgExpr : null;
                if (!(divArgExpr instanceof DivertTarget) && varRef == null) {
                    error(
                            "Target '" + targetFlow.getIdentifier()
                                    + "' expects a divert target for the parameter named -> " + flowArg.identifier
                                    + " but saw "
                                    + divArgExpr,
                            divArgExpr,
                            false);
                } else if (varRef != null) {
                    Path knotCountPath = new Path(varRef.pathIdentifiers);
                    ParsedObject targetForCount = knotCountPath.resolveFromContext(varRef);
                    if (targetForCount != null) {
                        error("Passing read count of '" + knotCountPath.getDotSeparatedComponents()
                                + "' instead of a divert target. You probably meant '" + knotCountPath + "'");
                    }
                }
            }
        }

        if (targetFlow == null) {
            error("Can't call as a function or with arguments unless it's a knot or stitch");
        }
    }

    private void checkExternalArgumentValidity(Story context) {
        String externalName = target.getFirstComponent();
        ExternalDeclaration external = context.externals.get(externalName);

        int externalArgCount = external.argumentNames.size();
        int ownArgCount = arguments != null ? arguments.size() : 0;

        if (ownArgCount != externalArgCount) {
            error("incorrect number of arguments sent to external function '" + externalName + "'. Expected "
                    + externalArgCount + " but got " + ownArgCount);
        }
    }

    @Override
    public void error(String message, ParsedObject source, boolean isWarning) {
        if (source != this && source != null) {
            super.error(message, source, isWarning);
            return;
        }

        if (isFunctionCall) {
            super.error("Function call " + message, source, isWarning);
        } else {
            super.error("Divert " + message, source, isWarning);
        }
    }

    @Override
    public String toString() {
        if (target != null) {
            return target.toString();
        }
        return "-> <empty divert>";
    }
}
