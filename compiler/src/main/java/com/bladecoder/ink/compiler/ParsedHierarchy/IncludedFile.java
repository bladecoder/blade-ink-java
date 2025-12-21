package com.bladecoder.ink.compiler.ParsedHierarchy;

public class IncludedFile extends ParsedObject {
    public final Story includedStory;

    public IncludedFile(Story includedStory) {
        this.includedStory = includedStory;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        return null;
    }
}
