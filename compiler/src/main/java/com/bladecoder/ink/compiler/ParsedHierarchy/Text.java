package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.StringValue;

public class Text extends ParsedObject {
    private String text;

    public Text(String str) {
        text = str;
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = value;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        return new StringValue(text);
    }

    @Override
    public String toString() {
        return text;
    }
}
