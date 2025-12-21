package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.Container;

public interface IWeavePoint {
    int getIndentationDepth();

    Container getRuntimeContainer();

    java.util.List<ParsedObject> getContent();

    String getName();

    Identifier getIdentifier();
}
