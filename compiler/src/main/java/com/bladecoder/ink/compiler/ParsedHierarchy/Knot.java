package com.bladecoder.ink.compiler.ParsedHierarchy;

import java.util.List;

public class Knot extends FlowBase {
    @Override
    public FlowLevel getFlowLevel() {
        return FlowLevel.Knot;
    }

    public Knot(Identifier name, List<ParsedObject> topLevelObjects, List<Argument> arguments, boolean isFunction) {
        super(name, topLevelObjects, arguments, isFunction, false);
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        Story parentStory = getStory();

        for (FlowBase stitch : getSubFlowsByName().values()) {
            String stitchName = stitch.getIdentifier() != null ? stitch.getIdentifier().name : null;

            ParsedObject knotWithStitchName = parentStory.contentWithNameAtLevel(stitchName, FlowLevel.Knot, false);
            if (knotWithStitchName != null) {
                String errorMsg = String.format(
                        "Stitch '%s' has the same name as a knot (on %s)",
                        stitch.getIdentifier(), knotWithStitchName.getDebugMetadata());
                error(errorMsg, stitch, false);
            }
        }
    }
}
