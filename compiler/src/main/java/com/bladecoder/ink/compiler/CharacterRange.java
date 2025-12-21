package com.bladecoder.ink.compiler;

import java.util.Collection;
import java.util.HashSet;

public final class CharacterRange {
    public static CharacterRange define(char start, char end) {
        return new CharacterRange(start, end, null);
    }

    public static CharacterRange define(char start, char end, Collection<Character> excludes) {
        return new CharacterRange(start, end, excludes);
    }

    public CharacterSet toCharacterSet() {
        if (_correspondingCharSet.isEmpty()) {
            for (char c = _start; c <= _end; c++) {
                if (!_excludes.contains(c)) {
                    _correspondingCharSet.add(c);
                }
            }
        }
        return _correspondingCharSet;
    }

    public char getStart() {
        return _start;
    }

    public char getEnd() {
        return _end;
    }

    private CharacterRange(char start, char end, Collection<Character> excludes) {
        _start = start;
        _end = end;
        _excludes = excludes == null ? new HashSet<>() : new HashSet<>(excludes);
    }

    private final char _start;
    private final char _end;
    private final Collection<Character> _excludes;
    private final CharacterSet _correspondingCharSet = new CharacterSet();
}
