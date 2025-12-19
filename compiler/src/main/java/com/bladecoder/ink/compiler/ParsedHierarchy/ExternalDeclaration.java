package com.bladecoder.ink.compiler.ParsedHierarchy;

import java.util.List;

public class ExternalDeclaration extends ParsedObject implements INamedContent {
    public Identifier identifier;
    public List<String> argumentNames;

    public ExternalDeclaration(Identifier identifier, List<String> argumentNames) {
        this.identifier = identifier;
        this.argumentNames = argumentNames;
    }

    @Override
    public String getName() {
        return identifier != null ? identifier.name : null;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        getStory().addExternal(this);
        return null;
    }
}
