package com.bladecoder.ink.compiler.ParsedHierarchy;

public class Wrap<T extends com.bladecoder.ink.runtime.RTObject> extends ParsedObject {
    private final T objToWrap;

    public Wrap(T objToWrap) {
        this.objToWrap = objToWrap;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        return objToWrap;
    }

    public static class Glue extends Wrap<com.bladecoder.ink.runtime.Glue> {
        public Glue(com.bladecoder.ink.runtime.Glue glue) {
            super(glue);
        }
    }

    public static class LegacyTag extends Wrap<com.bladecoder.ink.runtime.Tag> {
        public LegacyTag(com.bladecoder.ink.runtime.Tag tag) {
            super(tag);
        }
    }
}
