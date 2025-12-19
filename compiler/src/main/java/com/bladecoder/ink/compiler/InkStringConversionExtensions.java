package com.bladecoder.ink.compiler;

import java.util.List;

public final class InkStringConversionExtensions {
    private InkStringConversionExtensions() {}

    public static <T> String[] toStringsArray(List<T> list) {
        int count = list.size();
        String[] strings = new String[count];

        for (int i = 0; i < count; i++) {
            strings[i] = list.get(i).toString();
        }

        return strings;
    }
}
