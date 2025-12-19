package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.DebugMetadata;

public class Identifier {
    public String name;
    public DebugMetadata debugMetadata;

    @Override
    public String toString() {
        return name;
    }

    public static final Identifier Done = new Identifier();

    static {
        Done.name = "DONE";
        Done.debugMetadata = null;
    }
}
