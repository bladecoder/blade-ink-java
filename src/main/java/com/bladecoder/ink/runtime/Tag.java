package com.bladecoder.ink.runtime;

public class Tag extends RTObject {
    private String text;

    public String getText() {
        return text;
    }

    public Tag(String tagText) {
        this.text = tagText;
    }

    public String toString() {
        return "# " + text;
    }
}
