//
// Translated by CS2J (http://www.cs2j.com): 22/07/2016 12:24:33
//

package Ink.Runtime;

import Ink.Runtime.ControlCommand;
import Ink.Runtime.RTObject;

public class ControlCommand  extends RTObject 
{
    public enum CommandType
    {
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
        TurnsSince,
        VisitIndex,
        SequenceShuffleIndex,
        StartThread,
        Done,
        End,
        //----
        TOTAL_VALUES
    }
    private CommandType __commandType = CommandType.NotSet;
    public CommandType getcommandType() {
        return __commandType;
    }

    public void setcommandType(CommandType value) {
        __commandType = value;
    }

    public ControlCommand(CommandType commandType) throws Exception {
        this.setcommandType(commandType);
    }

    // Require default constructor for serialisation
    public ControlCommand() throws Exception {
        this(CommandType.NotSet);
    }

    public RTObject copy() throws Exception {
        return new ControlCommand(getcommandType());
    }

    // The following static factory methods are to make generating these RTObjects
    // slightly more succinct. Without these, the code gets pretty massive! e.g.
    //
    //     var c = new Runtime.ControlCommand(Runtime.ControlCommand.CommandType.EvalStart)
    //
    // as opposed to
    //
    //     var c = Runtime.ControlCommand.EvalStart()
    public static ControlCommand evalStart() throws Exception {
        return new ControlCommand(CommandType.EvalStart);
    }

    public static ControlCommand evalOutput() throws Exception {
        return new ControlCommand(CommandType.EvalOutput);
    }

    public static ControlCommand evalEnd() throws Exception {
        return new ControlCommand(CommandType.EvalEnd);
    }

    public static ControlCommand duplicate() throws Exception {
        return new ControlCommand(CommandType.Duplicate);
    }

    public static ControlCommand popEvaluatedValue() throws Exception {
        return new ControlCommand(CommandType.PopEvaluatedValue);
    }

    public static ControlCommand popFunction() throws Exception {
        return new ControlCommand(CommandType.PopFunction);
    }

    public static ControlCommand popTunnel() throws Exception {
        return new ControlCommand(CommandType.PopTunnel);
    }

    public static ControlCommand beginString() throws Exception {
        return new ControlCommand(CommandType.BeginString);
    }

    public static ControlCommand endString() throws Exception {
        return new ControlCommand(CommandType.EndString);
    }

    public static ControlCommand noOp() throws Exception {
        return new ControlCommand(CommandType.NoOp);
    }

    public static ControlCommand choiceCount() throws Exception {
        return new ControlCommand(CommandType.ChoiceCount);
    }

    public static ControlCommand turnsSince() throws Exception {
        return new ControlCommand(CommandType.TurnsSince);
    }

    public static ControlCommand visitIndex() throws Exception {
        return new ControlCommand(CommandType.VisitIndex);
    }

    public static ControlCommand sequenceShuffleIndex() throws Exception {
        return new ControlCommand(CommandType.SequenceShuffleIndex);
    }

    public static ControlCommand startThread() throws Exception {
        return new ControlCommand(CommandType.StartThread);
    }

    public static ControlCommand done() throws Exception {
        return new ControlCommand(CommandType.Done);
    }

    public static ControlCommand end() throws Exception {
        return new ControlCommand(CommandType.End);
    }

    public String toString() {
        try
        {
            return getcommandType().toString();
        }
        catch (RuntimeException __dummyCatchVar0)
        {
            throw __dummyCatchVar0;
        }
        catch (Exception __dummyCatchVar0)
        {
            throw new RuntimeException(__dummyCatchVar0);
        }
    
    }

}


