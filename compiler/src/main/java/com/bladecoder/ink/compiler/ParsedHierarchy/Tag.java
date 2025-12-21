package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.ControlCommand;

public class Tag extends ParsedObject {
    public boolean isStart;
    public boolean inChoice;

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        if (isStart) {
            return new ControlCommand(ControlCommand.CommandType.BeginTag);
        }
        return new ControlCommand(ControlCommand.CommandType.EndTag);
    }

    @Override
    public String toString() {
        return isStart ? "#StartTag" : "#EndTag";
    }
}
