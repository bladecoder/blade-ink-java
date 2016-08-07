package com.bladecoder.ink.runtime;

import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.RTObject;

public class ControlCommand extends RTObject {
	public enum CommandType {
		NotSet, EvalStart, EvalOutput, EvalEnd, Duplicate, PopEvaluatedValue, PopFunction, PopTunnel, BeginString, EndString, NoOp, ChoiceCount, TurnsSince, VisitIndex, SequenceShuffleIndex, StartThread, Done, End
	}

	private CommandType commandType = CommandType.NotSet;

	public CommandType getcommandType() {
		return commandType;
	}

	public void setcommandType(CommandType value) {
		commandType = value;
	}

	public ControlCommand(CommandType commandType) {
		this.setcommandType(commandType);
	}

	// Require default constructor for serialisation
	public ControlCommand() {
		this(CommandType.NotSet);
	}

	@Override
	public RTObject copy() {
		return new ControlCommand(getcommandType());
	}

	// The following static factory methods are to make generating these
	// RTObjects
	// slightly more succinct. Without these, the code gets pretty massive! e.g.
	//
	// var c = new
	// Runtime.ControlCommand(Runtime.ControlCommand.CommandType.EvalStart)
	//
	// as opposed to
	//
	// var c = Runtime.ControlCommand.EvalStart()
	public static ControlCommand evalStart() {
		return new ControlCommand(CommandType.EvalStart);
	}

	public static ControlCommand evalOutput() {
		return new ControlCommand(CommandType.EvalOutput);
	}

	public static ControlCommand evalEnd() {
		return new ControlCommand(CommandType.EvalEnd);
	}

	public static ControlCommand duplicate() {
		return new ControlCommand(CommandType.Duplicate);
	}

	public static ControlCommand popEvaluatedValue() {
		return new ControlCommand(CommandType.PopEvaluatedValue);
	}

	public static ControlCommand popFunction() {
		return new ControlCommand(CommandType.PopFunction);
	}

	public static ControlCommand popTunnel() {
		return new ControlCommand(CommandType.PopTunnel);
	}

	public static ControlCommand beginString() {
		return new ControlCommand(CommandType.BeginString);
	}

	public static ControlCommand endString() {
		return new ControlCommand(CommandType.EndString);
	}

	public static ControlCommand noOp() {
		return new ControlCommand(CommandType.NoOp);
	}

	public static ControlCommand choiceCount() {
		return new ControlCommand(CommandType.ChoiceCount);
	}

	public static ControlCommand turnsSince() {
		return new ControlCommand(CommandType.TurnsSince);
	}

	public static ControlCommand visitIndex() {
		return new ControlCommand(CommandType.VisitIndex);
	}

	public static ControlCommand sequenceShuffleIndex() {
		return new ControlCommand(CommandType.SequenceShuffleIndex);
	}

	public static ControlCommand startThread() {
		return new ControlCommand(CommandType.StartThread);
	}

	public static ControlCommand done() {
		return new ControlCommand(CommandType.Done);
	}

	public static ControlCommand end() {
		return new ControlCommand(CommandType.End);
	}

	@Override
	public String toString() {
		return getcommandType().toString();
	}

}
