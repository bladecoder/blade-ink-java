package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;

public class Gather extends ParsedObject implements IWeavePoint, INamedContent {
    public Identifier identifier;
    private final int indentationDepth;

    public Gather(Identifier identifier, int indentationDepth) {
        this.identifier = identifier;
        this.indentationDepth = indentationDepth;
    }

    @Override
    public int getIndentationDepth() {
        return indentationDepth;
    }

    @Override
    public Container getRuntimeContainer() {
        return (Container) getRuntimeObject();
    }

    @Override
    public String getName() {
        return identifier != null ? identifier.name : null;
    }

    @Override
    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Container container = new Container();
        container.setName(getName());

        if (getStory().countAllVisits) {
            container.setVisitsShouldBeCounted(true);
        }

        container.setCountingAtStartOnly(true);

        if (content != null) {
            for (ParsedObject obj : content) {
                RuntimeUtils.addContent(container, obj.getRuntimeObject());
            }
        }

        return container;
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        if (identifier != null && identifier.name.length() > 0) {
            context.checkForNamingCollisions(this, identifier, Story.SymbolType.SubFlowAndWeave, null);
        }
    }
}
