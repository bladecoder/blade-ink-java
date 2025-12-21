package com.bladecoder.ink.compiler;

import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.RTObject;

public final class RuntimeUtils {
    private RuntimeUtils() {}

    public static void addContent(Container container, RTObject obj) {
        try {
            container.addContent(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addContent(Container container, ControlCommand.CommandType commandType) {
        addContent(container, new ControlCommand(commandType));
    }
}
