package com.bladecoder.ink.compiler;

import java.util.Collection;
import java.util.HashSet;

public class CharacterSet extends HashSet<Character> {
    public static CharacterSet fromRange(char start, char end) {
        return new CharacterSet().addRange(start, end);
    }

    public CharacterSet() {}

    public CharacterSet(String str) {
        addCharacters(str);
    }

    public CharacterSet(CharacterSet charSetToCopy) {
        addCharacters(charSetToCopy);
    }

    public CharacterSet addRange(char start, char end) {
        for (char c = start; c <= end; ++c) {
            add(c);
        }
        return this;
    }

    public CharacterSet addCharacters(Collection<Character> chars) {
        for (char c : chars) {
            add(c);
        }
        return this;
    }

    public CharacterSet addCharacters(String chars) {
        for (char c : chars.toCharArray()) {
            add(c);
        }
        return this;
    }
}
