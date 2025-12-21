package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import java.util.ArrayList;
import java.util.List;

public class Conditional extends ParsedObject {
    private Expression initialCondition;
    private List<ConditionalSingleBranch> branches;
    private ControlCommand reJoinTarget;

    public Conditional(Expression condition, List<ConditionalSingleBranch> branches) {
        this.initialCondition = condition;
        if (this.initialCondition != null) {
            addContent(condition);
        }

        this.branches = branches;
        if (this.branches != null) {
            addContent(new ArrayList<ParsedObject>(this.branches));
        }
    }

    public Expression getInitialCondition() {
        return initialCondition;
    }

    public List<ConditionalSingleBranch> getBranches() {
        return branches;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Container container = new Container();

        if (initialCondition != null) {
            RuntimeUtils.addContent(container, initialCondition.getRuntimeObject());
        }

        for (ConditionalSingleBranch branch : branches) {
            Container branchContainer = (Container) branch.getRuntimeObject();
            RuntimeUtils.addContent(container, branchContainer);
        }

        if (initialCondition != null
                && branches.get(0).getOwnExpression() != null
                && !branches.get(branches.size() - 1).isElse) {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.PopEvaluatedValue);
        }

        reJoinTarget = new ControlCommand(ControlCommand.CommandType.NoOp);
        RuntimeUtils.addContent(container, reJoinTarget);

        return container;
    }

    @Override
    public void resolveReferences(Story context) {
        for (ConditionalSingleBranch branch : branches) {
            branch.getReturnDivert().setTargetPath(reJoinTarget.getPath());
        }

        super.resolveReferences(context);
    }
}
