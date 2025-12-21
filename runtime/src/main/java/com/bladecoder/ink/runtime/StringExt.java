package com.bladecoder.ink.runtime;

import java.util.List;

public class StringExt {
    public static <T> String join(String separator, List<T> RTObjects) throws Exception {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for (T o : RTObjects) {
            if (!isFirst) sb.append(separator);

            sb.append(o.toString());
            isFirst = false;
        }
        return sb.toString();
    }
}
