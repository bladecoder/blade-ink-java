package com.bladecoder.ink.compiler.ParsedHierarchy;

public class TunnelOnwards extends ParsedObject {
    public Divert divertAfter;
    private com.bladecoder.ink.runtime.Divert runtimeDivert;
    private com.bladecoder.ink.runtime.DivertTargetValue runtimeDivertTargetValue;

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        com.bladecoder.ink.runtime.Container container = new com.bladecoder.ink.runtime.Container();

        com.bladecoder.ink.compiler.RuntimeUtils.addContent(
                container, com.bladecoder.ink.runtime.ControlCommand.CommandType.EvalStart);

        if (divertAfter != null) {
            divertAfter.generateRuntimeObject();
            runtimeDivert = divertAfter.runtimeDivert;
            runtimeDivertTargetValue = new com.bladecoder.ink.runtime.DivertTargetValue();
            com.bladecoder.ink.compiler.RuntimeUtils.addContent(container, runtimeDivertTargetValue);
        } else {
            com.bladecoder.ink.compiler.RuntimeUtils.addContent(container, new com.bladecoder.ink.runtime.Void());
        }

        com.bladecoder.ink.compiler.RuntimeUtils.addContent(
                container, com.bladecoder.ink.runtime.ControlCommand.CommandType.EvalEnd);
        com.bladecoder.ink.compiler.RuntimeUtils.addContent(
                container, com.bladecoder.ink.runtime.ControlCommand.CommandType.PopTunnel);
        return container;
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (divertAfter == null) {
            return;
        }

        divertAfter.resolveReferences(context);

        if (divertAfter.isDone() || divertAfter.isEnd()) {
            error("Can't use -> DONE or -> END as tunnel onwards targets", this, false);
            return;
        }

        if (runtimeDivert != null && runtimeDivert.hasVariableTarget()) {
            error(
                    "Since '" + divertAfter.target.getDotSeparatedComponents()
                            + "' is a variable, it shouldn't be preceded by '->' here.",
                    this,
                    false);
            return;
        }

        if (runtimeDivertTargetValue != null && runtimeDivert != null) {
            try {
                runtimeDivertTargetValue.setTargetPath(runtimeDivert.getTargetPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
