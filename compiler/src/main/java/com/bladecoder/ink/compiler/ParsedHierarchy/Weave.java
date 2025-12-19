package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.Divert;
import com.bladecoder.ink.runtime.RTObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Used by the FlowBase when constructing the weave flow from a flat list of content objects.
public class Weave extends ParsedObject {
    public Container rootContainer;
    private Container currentContainer;

    public int baseIndentIndex;

    public List<IWeavePoint> looseEnds;

    public List<GatherPointToResolve> gatherPointsToResolve;

    public static class GatherPointToResolve {
        public Divert divert;
        public RTObject targetRuntimeObj;
    }

    public ParsedObject getLastParsedSignificantObject() {
        if (content == null || content.isEmpty()) {
            return null;
        }

        ParsedObject lastObject = null;
        for (int i = content.size() - 1; i >= 0; --i) {
            lastObject = content.get(i);

            Text lastText = lastObject instanceof Text ? (Text) lastObject : null;
            if (lastText != null && "\n".equals(lastText.getText())) {
                continue;
            }

            if (isGlobalDeclaration(lastObject)) {
                continue;
            }

            break;
        }

        Weave lastWeave = lastObject instanceof Weave ? (Weave) lastObject : null;
        if (lastWeave != null) {
            lastObject = lastWeave.getLastParsedSignificantObject();
        }

        return lastObject;
    }

    public Weave(List<ParsedObject> cont, int indentIndex) {
        if (indentIndex == -1) {
            baseIndentIndex = determineBaseIndentationFromContent(cont);
        } else {
            baseIndentIndex = indentIndex;
        }

        addContent(cont);

        constructWeaveHierarchyFromIndentation();
    }

    public Weave(List<ParsedObject> cont) {
        this(cont, -1);
    }

    public void resolveWeavePointNaming() {
        List<IWeavePoint> namedWeavePoints = findAll(
                IWeavePoint.class, w -> w.getName() != null && !w.getName().isEmpty());

        namedWeavePointsByName = new HashMap<>();

        for (IWeavePoint weavePoint : namedWeavePoints) {
            IWeavePoint existingWeavePoint = namedWeavePointsByName.get(weavePoint.getName());
            if (existingWeavePoint != null) {
                String typeName = existingWeavePoint instanceof Gather ? "gather" : "choice";
                ParsedObject existingObj = (ParsedObject) existingWeavePoint;

                error(
                        "A " + typeName + " with the same label name '" + weavePoint.getName()
                                + "' already exists in this context on line "
                                + existingObj.getDebugMetadata().startLineNumber,
                        (ParsedObject) weavePoint,
                        false);
            }

            namedWeavePointsByName.put(weavePoint.getName(), weavePoint);
        }
    }

    private void constructWeaveHierarchyFromIndentation() {
        if (content == null) {
            return;
        }

        int contentIdx = 0;
        while (contentIdx < content.size()) {
            ParsedObject obj = content.get(contentIdx);

            if (obj instanceof IWeavePoint) {
                IWeavePoint weavePoint = (IWeavePoint) obj;
                int weaveIndentIdx = weavePoint.getIndentationDepth() - 1;

                if (weaveIndentIdx > baseIndentIndex) {
                    int innerWeaveStartIdx = contentIdx;
                    while (contentIdx < content.size()) {
                        IWeavePoint innerWeaveObj = content.get(contentIdx) instanceof IWeavePoint
                                ? (IWeavePoint) content.get(contentIdx)
                                : null;
                        if (innerWeaveObj != null) {
                            int innerIndentIdx = innerWeaveObj.getIndentationDepth() - 1;
                            if (innerIndentIdx <= baseIndentIndex) {
                                break;
                            }
                        }

                        contentIdx++;
                    }

                    int weaveContentCount = contentIdx - innerWeaveStartIdx;

                    List<ParsedObject> weaveContent = new ArrayList<>(
                            content.subList(innerWeaveStartIdx, innerWeaveStartIdx + weaveContentCount));
                    content.subList(innerWeaveStartIdx, innerWeaveStartIdx + weaveContentCount)
                            .clear();

                    Weave weave = new Weave(weaveContent, weaveIndentIdx);
                    insertContent(innerWeaveStartIdx, weave);

                    contentIdx = innerWeaveStartIdx;
                }
            }

            contentIdx++;
        }
    }

    public int determineBaseIndentationFromContent(List<ParsedObject> contentList) {
        for (ParsedObject obj : contentList) {
            if (obj instanceof IWeavePoint) {
                return ((IWeavePoint) obj).getIndentationDepth() - 1;
            }
        }

        return 0;
    }

    @Override
    public RTObject generateRuntimeObject() {
        rootContainer = currentContainer = new Container();
        looseEnds = new ArrayList<>();

        gatherPointsToResolve = new ArrayList<>();

        if (content != null) {
            for (ParsedObject obj : content) {
                if (obj instanceof IWeavePoint) {
                    addRuntimeForWeavePoint((IWeavePoint) obj);
                } else {
                    if (obj instanceof Weave) {
                        Weave weave = (Weave) obj;
                        addRuntimeForNestedWeave(weave);
                        if (weave.gatherPointsToResolve != null) {
                            gatherPointsToResolve.addAll(weave.gatherPointsToResolve);
                        }
                    } else {
                        addGeneralRuntimeContent(obj.getRuntimeObject());
                    }
                }
            }
        }

        passLooseEndsToAncestors();

        return rootContainer;
    }

    private void addRuntimeForGather(Gather gather) {
        boolean autoEnter = !hasSeenChoiceInSection;
        hasSeenChoiceInSection = false;

        Container gatherContainer = gather.getRuntimeContainer();

        if (gather.getName() == null) {
            gatherContainer.setName("g-" + unnamedGatherCount);
            unnamedGatherCount++;
        }

        if (autoEnter) {
            RuntimeUtils.addContent(currentContainer, gatherContainer);
        } else {
            rootContainer.addToNamedContentOnly(gatherContainer);
        }

        for (IWeavePoint looseEndWeavePoint : looseEnds) {
            ParsedObject looseEnd = (ParsedObject) looseEndWeavePoint;

            if (looseEnd instanceof Gather) {
                Gather prevGather = (Gather) looseEnd;
                if (prevGather.getIndentationDepth() == gather.getIndentationDepth()) {
                    continue;
                }
            }

            Divert divert;

            if (looseEnd instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert) {
                divert = (Divert) looseEnd.getRuntimeObject();
            } else {
                divert = new Divert();
                RuntimeUtils.addContent(looseEndWeavePoint.getRuntimeContainer(), divert);
            }

            GatherPointToResolve gatherPoint = new GatherPointToResolve();
            gatherPoint.divert = divert;
            gatherPoint.targetRuntimeObj = gatherContainer;
            gatherPointsToResolve.add(gatherPoint);
        }
        looseEnds.clear();

        currentContainer = gatherContainer;
    }

    private void addRuntimeForWeavePoint(IWeavePoint weavePoint) {
        if (weavePoint instanceof Gather) {
            addRuntimeForGather((Gather) weavePoint);
        } else if (weavePoint instanceof Choice) {
            if (previousWeavePoint instanceof Gather) {
                looseEnds.remove(previousWeavePoint);
            }

            Choice choice = (Choice) weavePoint;
            RuntimeUtils.addContent(currentContainer, choice.getRuntimeObject());

            choice.getInnerContentContainer().setName("c-" + choiceCount);
            currentContainer.addToNamedContentOnly(choice.getInnerContentContainer());
            choiceCount++;

            hasSeenChoiceInSection = true;
        }

        addContentToPreviousWeavePoint = false;
        if (weavePointHasLooseEnd(weavePoint)) {
            looseEnds.add(weavePoint);

            Choice looseChoice = weavePoint instanceof Choice ? (Choice) weavePoint : null;
            if (looseChoice != null) {
                addContentToPreviousWeavePoint = true;
            }
        }
        previousWeavePoint = weavePoint;
    }

    public void addRuntimeForNestedWeave(Weave nestedResult) {
        RTObject nestedRuntime = nestedResult.getRuntimeObject();
        addGeneralRuntimeContent(nestedRuntime);

        if (previousWeavePoint != null) {
            looseEnds.remove(previousWeavePoint);
            addContentToPreviousWeavePoint = false;
        }
    }

    private void addGeneralRuntimeContent(RTObject contentObj) {
        if (contentObj == null) {
            return;
        }

        if (addContentToPreviousWeavePoint) {
            RuntimeUtils.addContent(previousWeavePoint.getRuntimeContainer(), contentObj);
        } else {
            RuntimeUtils.addContent(currentContainer, contentObj);
        }
    }

    private void passLooseEndsToAncestors() {
        if (looseEnds.isEmpty()) {
            return;
        }

        Weave closestInnerWeaveAncestor = null;
        Weave closestOuterWeaveAncestor = null;

        boolean nested = false;
        for (ParsedObject ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            Weave weaveAncestor = ancestor instanceof Weave ? (Weave) ancestor : null;
            if (weaveAncestor != null) {
                if (!nested && closestInnerWeaveAncestor == null) {
                    closestInnerWeaveAncestor = weaveAncestor;
                }

                if (nested && closestOuterWeaveAncestor == null) {
                    closestOuterWeaveAncestor = weaveAncestor;
                }
            }

            if (ancestor instanceof Sequence || ancestor instanceof Conditional) {
                nested = true;
            }
        }

        if (closestInnerWeaveAncestor == null && closestOuterWeaveAncestor == null) {
            return;
        }

        for (int i = looseEnds.size() - 1; i >= 0; i--) {
            IWeavePoint looseEnd = looseEnds.get(i);

            boolean received = false;

            if (nested) {
                if (looseEnd instanceof Choice && closestInnerWeaveAncestor != null) {
                    closestInnerWeaveAncestor.receiveLooseEnd(looseEnd);
                    received = true;
                } else if (!(looseEnd instanceof Choice)) {
                    Weave receivingWeave =
                            closestInnerWeaveAncestor != null ? closestInnerWeaveAncestor : closestOuterWeaveAncestor;
                    if (receivingWeave != null) {
                        receivingWeave.receiveLooseEnd(looseEnd);
                        received = true;
                    }
                }
            } else {
                closestInnerWeaveAncestor.receiveLooseEnd(looseEnd);
                received = true;
            }

            if (received) {
                looseEnds.remove(i);
            }
        }
    }

    private void receiveLooseEnd(IWeavePoint childWeaveLooseEnd) {
        looseEnds.add(childWeaveLooseEnd);
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (looseEnds != null && !looseEnds.isEmpty()) {
            boolean isNestedWeave = false;
            for (ParsedObject ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
                if (ancestor instanceof Sequence || ancestor instanceof Conditional) {
                    isNestedWeave = true;
                    break;
                }
            }
            if (isNestedWeave) {
                validateTermination(this::badNestedTerminationHandler);
            }
        }

        for (GatherPointToResolve gatherPoint : gatherPointsToResolve) {
            gatherPoint.divert.setTargetPath(gatherPoint.targetRuntimeObj.getPath());
        }

        checkForWeavePointNamingCollisions();
    }

    public IWeavePoint weavePointNamed(String name) {
        if (namedWeavePointsByName == null) {
            return null;
        }

        return namedWeavePointsByName.get(name);
    }

    private boolean isGlobalDeclaration(ParsedObject obj) {
        VariableAssignment varAss = obj instanceof VariableAssignment ? (VariableAssignment) obj : null;
        if (varAss != null && varAss.isGlobalDeclaration && varAss.isDeclaration()) {
            return true;
        }

        ConstantDeclaration constDecl = obj instanceof ConstantDeclaration ? (ConstantDeclaration) obj : null;
        if (constDecl != null) {
            return true;
        }

        return false;
    }

    private List<ParsedObject> contentThatFollowsWeavePoint(IWeavePoint weavePoint) {
        ParsedObject obj = (ParsedObject) weavePoint;

        List<ParsedObject> result = new ArrayList<>();

        if (obj.getContent() != null) {
            for (ParsedObject contentObj : obj.getContent()) {
                if (isGlobalDeclaration(contentObj)) {
                    continue;
                }
                result.add(contentObj);
            }
        }

        Weave parentWeave = obj.parent instanceof Weave ? (Weave) obj.parent : null;
        if (parentWeave == null) {
            throw new RuntimeException("Expected weave point parent to be weave?");
        }

        int weavePointIdx = parentWeave.content.indexOf(obj);

        for (int i = weavePointIdx + 1; i < parentWeave.content.size(); i++) {
            ParsedObject laterObj = parentWeave.content.get(i);

            if (isGlobalDeclaration(laterObj)) {
                continue;
            }

            if (laterObj instanceof IWeavePoint) {
                break;
            }

            if (laterObj instanceof Weave) {
                break;
            }

            result.add(laterObj);
        }

        return result;
    }

    public interface BadTerminationHandler {
        void onBadTermination(ParsedObject terminatingObj);
    }

    public void validateTermination(BadTerminationHandler badTerminationHandler) {
        if (getLastParsedSignificantObject() instanceof AuthorWarning) {
            return;
        }

        boolean hasLooseEnds = looseEnds != null && !looseEnds.isEmpty();

        if (hasLooseEnds) {
            for (IWeavePoint looseEnd : looseEnds) {
                List<ParsedObject> looseEndFlow = contentThatFollowsWeavePoint(looseEnd);
                validateFlowOfObjectsTerminates(looseEndFlow, (ParsedObject) looseEnd, badTerminationHandler);
            }
        } else {
            if (content != null) {
                for (ParsedObject obj : content) {
                    if (obj instanceof IWeavePoint) {
                        return;
                    }
                }
            }

            validateFlowOfObjectsTerminates(content, this, badTerminationHandler);
        }
    }

    private void badNestedTerminationHandler(ParsedObject terminatingObj) {
        Conditional conditional = null;
        for (ParsedObject ancestor = terminatingObj.parent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor instanceof Sequence || ancestor instanceof Conditional) {
                conditional = ancestor instanceof Conditional ? (Conditional) ancestor : null;
                break;
            }
        }

        String errorMsg = "Choices nested in conditionals or sequences need to explicitly divert afterwards.";

        if (conditional != null) {
            int numChoices = conditional.findAll(Choice.class).size();
            if (numChoices == 1) {
                errorMsg = "Choices with conditions should be written: '* {condition} choice'. Otherwise, "
                        + errorMsg.toLowerCase();
            }
        }

        error(errorMsg, terminatingObj, false);
    }

    private void validateFlowOfObjectsTerminates(
            List<ParsedObject> objFlow, ParsedObject defaultObj, BadTerminationHandler badTerminationHandler) {
        boolean terminated = false;
        ParsedObject terminatingObj = defaultObj;
        if (objFlow != null) {
            for (ParsedObject flowObj : objFlow) {
                com.bladecoder.ink.compiler.ParsedHierarchy.Divert divert = flowObj.find(
                        com.bladecoder.ink.compiler.ParsedHierarchy.Divert.class,
                        d -> !d.isThread && !d.isTunnel && !d.isFunctionCall && !(d.parent instanceof DivertTarget));
                if (divert != null) {
                    terminated = true;
                }

                if (flowObj.find(TunnelOnwards.class) != null) {
                    terminated = true;
                    break;
                }

                terminatingObj = flowObj;
            }
        }

        if (!terminated) {
            if (terminatingObj instanceof AuthorWarning) {
                return;
            }

            badTerminationHandler.onBadTermination(terminatingObj);
        }
    }

    private boolean weavePointHasLooseEnd(IWeavePoint weavePoint) {
        if (weavePoint.getContent() == null) {
            return true;
        }

        List<ParsedObject> weaveContent = weavePoint.getContent();
        for (int i = weaveContent.size() - 1; i >= 0; --i) {
            com.bladecoder.ink.compiler.ParsedHierarchy.Divert innerDivert =
                    weaveContent.get(i) instanceof com.bladecoder.ink.compiler.ParsedHierarchy.Divert
                            ? (com.bladecoder.ink.compiler.ParsedHierarchy.Divert) weaveContent.get(i)
                            : null;
            if (innerDivert != null) {
                boolean willReturn = innerDivert.isThread || innerDivert.isTunnel || innerDivert.isFunctionCall;
                if (!willReturn) {
                    return false;
                }
            }
        }

        return true;
    }

    private void checkForWeavePointNamingCollisions() {
        if (namedWeavePointsByName == null) {
            return;
        }

        List<FlowBase> ancestorFlows = new ArrayList<>();
        for (ParsedObject obj : getAncestry()) {
            FlowBase flow = obj instanceof FlowBase ? (FlowBase) obj : null;
            if (flow != null) {
                ancestorFlows.add(flow);
            } else {
                break;
            }
        }

        for (Map.Entry<String, IWeavePoint> namedWeavePointPair : namedWeavePointsByName.entrySet()) {
            String weavePointName = namedWeavePointPair.getKey();
            ParsedObject weavePoint = (ParsedObject) namedWeavePointPair.getValue();

            for (FlowBase flow : ancestorFlows) {
                ParsedObject otherContentWithName = flow.contentWithNameAtLevel(weavePointName, null, false);

                if (otherContentWithName != null && otherContentWithName != weavePoint) {
                    String errorMsg = String.format(
                            "%s '%s' has the same label name as a %s (on %s)",
                            weavePoint.getClass().getSimpleName(),
                            weavePointName,
                            otherContentWithName.getClass().getSimpleName(),
                            otherContentWithName.getDebugMetadata());

                    error(errorMsg, weavePoint, false);
                }
            }
        }
    }

    private IWeavePoint previousWeavePoint;
    private boolean addContentToPreviousWeavePoint;
    private boolean hasSeenChoiceInSection;
    private int unnamedGatherCount;
    private int choiceCount;

    private Map<String, IWeavePoint> namedWeavePointsByName;
}
