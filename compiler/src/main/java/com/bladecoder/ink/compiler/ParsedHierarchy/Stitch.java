package com.bladecoder.ink.compiler.ParsedHierarchy;

import java.util.List;

public class Stitch extends FlowBase {
    @Override
    public FlowLevel getFlowLevel() {
        return FlowLevel.Stitch;
    }

    public Stitch(Identifier name, List<ParsedObject> topLevelObjects, List<Argument> arguments, boolean isFunction) {
        super(name, topLevelObjects, arguments, isFunction, false);
    }
}
