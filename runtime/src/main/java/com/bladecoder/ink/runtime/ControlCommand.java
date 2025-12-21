package com.bladecoder.ink.runtime;

public class ControlCommand extends RTObject {
    public enum CommandType {
        NotSet,
        EvalStart,
        EvalOutput,
        EvalEnd,
        Duplicate,
        PopEvaluatedValue,
        PopFunction,
        PopTunnel,
        BeginString,
        EndString,
        NoOp,
        ChoiceCount,
        Turns,
        TurnsSince,
        ReadCount,
        Random,
        SeedRandom,
        VisitIndex,
        SequenceShuffleIndex,
        StartThread,
        Done,
        End,
        ListFromInt,
        ListRange,
        ListRandom,
        BeginTag,
        EndTag
    }

    private CommandType commandType = CommandType.NotSet;

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType value) {
        commandType = value;
    }

    public ControlCommand(CommandType commandType) {
        this.setCommandType(commandType);
    }

    // Require default constructor for serialisation
    public ControlCommand() {
        this(CommandType.NotSet);
    }

    @Override
    public RTObject copy() {
        return new ControlCommand(getCommandType());
    }

    @Override
    public String toString() {
        return getCommandType().toString();
    }
}
