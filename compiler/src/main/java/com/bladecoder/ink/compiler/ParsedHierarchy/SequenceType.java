package com.bladecoder.ink.compiler.ParsedHierarchy;

public final class SequenceType {
    public static final int Stopping = 1;
    public static final int Cycle = 2;
    public static final int Shuffle = 4;
    public static final int Once = 8;

    private SequenceType() {}
}
