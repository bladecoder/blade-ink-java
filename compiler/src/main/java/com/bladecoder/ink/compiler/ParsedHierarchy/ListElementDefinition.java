package com.bladecoder.ink.compiler.ParsedHierarchy;

public class ListElementDefinition extends ParsedObject {
    public Identifier identifier;
    public Integer explicitValue;
    public int seriesValue;
    public boolean inInitialList;

    public String getName() {
        return identifier != null ? identifier.name : null;
    }

    public String getFullName() {
        ListDefinition parentList = parent instanceof ListDefinition ? (ListDefinition) parent : null;
        if (parentList == null) {
            throw new RuntimeException("Can't get full name without a parent list");
        }

        return parentList.identifier + "." + getName();
    }

    public ListElementDefinition(Identifier identifier, boolean inInitialList, Integer explicitValue) {
        this.identifier = identifier;
        this.inInitialList = inInitialList;
        this.explicitValue = explicitValue;
    }

    public ListElementDefinition(Identifier identifier, boolean inInitialList) {
        this(identifier, inInitialList, null);
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        context.checkForNamingCollisions(this, identifier, Story.SymbolType.ListItem, null);
    }

    @Override
    public String getTypeName() {
        return "List element";
    }
}
