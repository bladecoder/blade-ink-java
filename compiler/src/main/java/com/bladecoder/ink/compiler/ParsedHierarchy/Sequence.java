package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import com.bladecoder.ink.runtime.Divert;
import com.bladecoder.ink.runtime.IntValue;
import com.bladecoder.ink.runtime.NativeFunctionCall;
import com.bladecoder.ink.runtime.RTObject;
import java.util.ArrayList;
import java.util.List;

public class Sequence extends ParsedObject {
    public List<ParsedObject> sequenceElements;
    public int sequenceType;

    public Sequence(List<ContentList> elementContentLists, int sequenceType) {
        this.sequenceType = sequenceType;
        this.sequenceElements = new ArrayList<>();

        for (ContentList elementContentList : elementContentLists) {
            List<ParsedObject> contentObjs = elementContentList.getContent();

            ParsedObject seqElObject;

            if (contentObjs == null || contentObjs.isEmpty()) {
                seqElObject = elementContentList;
            } else if (containsWeavePoint(contentObjs)) {
                seqElObject = new Weave(contentObjs);
            } else {
                seqElObject = elementContentList;
            }

            sequenceElements.add(seqElObject);
            addContent(seqElObject);
        }
    }

    private boolean containsWeavePoint(List<ParsedObject> contentObjs) {
        for (ParsedObject obj : contentObjs) {
            if (obj instanceof IWeavePoint || obj instanceof Weave) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RTObject generateRuntimeObject() {
        Container container = new Container();
        container.setVisitsShouldBeCounted(true);
        container.setCountingAtStartOnly(true);

        sequenceDivertsToResolve = new ArrayList<>();

        RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);
        RuntimeUtils.addContent(container, ControlCommand.CommandType.VisitIndex);

        boolean once = (sequenceType & SequenceType.Once) > 0;
        boolean cycle = (sequenceType & SequenceType.Cycle) > 0;
        boolean stopping = (sequenceType & SequenceType.Stopping) > 0;
        boolean shuffle = (sequenceType & SequenceType.Shuffle) > 0;

        int seqBranchCount = sequenceElements.size();
        if (once) {
            seqBranchCount++;
        }

        if (stopping || once) {
            RuntimeUtils.addContent(container, new IntValue(seqBranchCount - 1));
            RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("MIN"));
        } else if (cycle) {
            RuntimeUtils.addContent(container, new IntValue(sequenceElements.size()));
            RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("%"));
        }

        if (shuffle) {
            ControlCommand postShuffleNoOp = new ControlCommand(ControlCommand.CommandType.NoOp);

            if (once || stopping) {
                int lastIdx = stopping ? sequenceElements.size() - 1 : sequenceElements.size();
                RuntimeUtils.addContent(container, new ControlCommand(ControlCommand.CommandType.Duplicate));
                RuntimeUtils.addContent(container, new IntValue(lastIdx));
                RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("=="));

                Divert skipShuffleDivert = new Divert();
                skipShuffleDivert.setConditional(true);
                RuntimeUtils.addContent(container, skipShuffleDivert);

                addDivertToResolve(skipShuffleDivert, postShuffleNoOp);
            }

            int elementCountToShuffle = sequenceElements.size();
            if (stopping) {
                elementCountToShuffle--;
            }

            RuntimeUtils.addContent(container, new IntValue(elementCountToShuffle));
            RuntimeUtils.addContent(container, ControlCommand.CommandType.SequenceShuffleIndex);
            if (once || stopping) {
                RuntimeUtils.addContent(container, postShuffleNoOp);
            }
        }

        RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);

        ControlCommand postSequenceNoOp = new ControlCommand(ControlCommand.CommandType.NoOp);

        for (int elIndex = 0; elIndex < seqBranchCount; elIndex++) {
            RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalStart);
            RuntimeUtils.addContent(container, ControlCommand.CommandType.Duplicate);
            RuntimeUtils.addContent(container, new IntValue(elIndex));
            RuntimeUtils.addContent(container, NativeFunctionCall.callWithName("=="));
            RuntimeUtils.addContent(container, ControlCommand.CommandType.EvalEnd);

            Divert sequenceDivert = new Divert();
            sequenceDivert.setConditional(true);
            RuntimeUtils.addContent(container, sequenceDivert);

            Container contentContainerForSequenceBranch;
            if (elIndex < sequenceElements.size()) {
                ParsedObject el = sequenceElements.get(elIndex);
                contentContainerForSequenceBranch = (Container) el.getRuntimeObject();
            } else {
                contentContainerForSequenceBranch = new Container();
            }

            contentContainerForSequenceBranch.setName("s" + elIndex);
            try {
                contentContainerForSequenceBranch.insertContent(
                        new ControlCommand(ControlCommand.CommandType.PopEvaluatedValue), 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Divert seqBranchCompleteDivert = new Divert();
            RuntimeUtils.addContent(contentContainerForSequenceBranch, seqBranchCompleteDivert);
            container.addToNamedContentOnly(contentContainerForSequenceBranch);

            addDivertToResolve(sequenceDivert, contentContainerForSequenceBranch);
            addDivertToResolve(seqBranchCompleteDivert, postSequenceNoOp);
        }

        RuntimeUtils.addContent(container, postSequenceNoOp);

        return container;
    }

    private void addDivertToResolve(Divert divert, RTObject targetContent) {
        sequenceDivertsToResolve.add(new SequenceDivertToResolve(divert, targetContent));
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        for (SequenceDivertToResolve toResolve : sequenceDivertsToResolve) {
            toResolve.divert.setTargetPath(toResolve.targetContent.getPath());
        }
    }

    private static class SequenceDivertToResolve {
        public Divert divert;
        public RTObject targetContent;

        public SequenceDivertToResolve(Divert divert, RTObject targetContent) {
            this.divert = divert;
            this.targetContent = targetContent;
        }
    }

    private List<SequenceDivertToResolve> sequenceDivertsToResolve;
}
