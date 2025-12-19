package com.bladecoder.ink.compiler.ParsedHierarchy;

public class AuthorWarning extends ParsedObject {
    public String warningMessage;

    public AuthorWarning(String message) {
        warningMessage = message;
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        warning(warningMessage);
        return null;
    }
}
