package com.bladecoder.ink.compiler.StringParser;

public class StringParserState {
    public int getLineIndex() {
        return currentElement().lineIndex;
    }

    public void setLineIndex(int value) {
        currentElement().lineIndex = value;
    }

    public int getCharacterIndex() {
        return currentElement().characterIndex;
    }

    public void setCharacterIndex(int value) {
        currentElement().characterIndex = value;
    }

    public int getCharacterInLineIndex() {
        return currentElement().characterInLineIndex;
    }

    public void setCharacterInLineIndex(int value) {
        currentElement().characterInLineIndex = value;
    }

    public long getCustomFlags() {
        return currentElement().customFlags;
    }

    public void setCustomFlags(long value) {
        currentElement().customFlags = value;
    }

    public boolean isErrorReportedAlreadyInScope() {
        return currentElement().reportedErrorInScope;
    }

    public int getStackHeight() {
        return _numElements;
    }

    public static class Element {
        public int characterIndex;
        public int characterInLineIndex;
        public int lineIndex;
        public boolean reportedErrorInScope;
        public int uniqueId;
        public long customFlags;

        public Element() {}

        public void copyFrom(Element fromElement) {
            _uniqueIdCounter++;
            this.uniqueId = _uniqueIdCounter;
            this.characterIndex = fromElement.characterIndex;
            this.characterInLineIndex = fromElement.characterInLineIndex;
            this.lineIndex = fromElement.lineIndex;
            this.customFlags = fromElement.customFlags;
            this.reportedErrorInScope = false;
        }

        public void squashFrom(Element fromElement) {
            this.characterIndex = fromElement.characterIndex;
            this.characterInLineIndex = fromElement.characterInLineIndex;
            this.lineIndex = fromElement.lineIndex;
            this.reportedErrorInScope = fromElement.reportedErrorInScope;
            this.customFlags = fromElement.customFlags;
        }

        private static int _uniqueIdCounter;
    }

    public StringParserState() {
        final int expectedMaxStackDepth = 200;
        _stack = new Element[expectedMaxStackDepth];

        for (int i = 0; i < expectedMaxStackDepth; ++i) {
            _stack[i] = new Element();
        }

        _numElements = 1;
    }

    public int push() {
        if (_numElements >= _stack.length) {
            throw new RuntimeException("Stack overflow in parser state");
        }

        Element prevElement = _stack[_numElements - 1];
        Element newElement = _stack[_numElements];
        _numElements++;

        newElement.copyFrom(prevElement);

        return newElement.uniqueId;
    }

    public void pop(int expectedRuleId) {
        if (_numElements == 1) {
            throw new RuntimeException(
                    "Attempting to remove final stack element is illegal! Mismatched Begin/Succceed/Fail?");
        }

        if (currentElement().uniqueId != expectedRuleId) {
            throw new RuntimeException("Mismatched rule IDs - do you have mismatched Begin/Succeed/Fail?");
        }

        _numElements--;
    }

    public Element peek(int expectedRuleId) {
        if (currentElement().uniqueId != expectedRuleId) {
            throw new RuntimeException("Mismatched rule IDs - do you have mismatched Begin/Succeed/Fail?");
        }

        return _stack[_numElements - 1];
    }

    public Element peekPenultimate() {
        if (_numElements >= 2) {
            return _stack[_numElements - 2];
        }
        return null;
    }

    public void squash() {
        if (_numElements < 2) {
            throw new RuntimeException(
                    "Attempting to remove final stack element is illegal! Mismatched Begin/Succceed/Fail?");
        }

        Element penultimateEl = _stack[_numElements - 2];
        Element lastEl = _stack[_numElements - 1];

        penultimateEl.squashFrom(lastEl);

        _numElements--;
    }

    public void noteErrorReported() {
        for (Element el : _stack) {
            el.reportedErrorInScope = true;
        }
    }

    protected Element currentElement() {
        return _stack[_numElements - 1];
    }

    private final Element[] _stack;
    private int _numElements;
}
