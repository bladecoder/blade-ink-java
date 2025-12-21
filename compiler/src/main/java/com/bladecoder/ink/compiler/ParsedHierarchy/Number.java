package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.BoolValue;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.FloatValue;
import com.bladecoder.ink.runtime.IntValue;

public class Number extends Expression {
    public java.lang.Object value;

    public Number(java.lang.Object value) {
        if (value instanceof Integer || value instanceof Float || value instanceof Boolean) {
            this.value = value;
        } else {
            throw new RuntimeException("Unexpected object type in Number");
        }
    }

    @Override
    public void generateIntoContainer(Container container) {
        if (value instanceof Integer) {
            RuntimeUtils.addContent(container, new IntValue((Integer) value));
        } else if (value instanceof Float) {
            RuntimeUtils.addContent(container, new FloatValue((Float) value));
        } else if (value instanceof Boolean) {
            RuntimeUtils.addContent(container, new BoolValue((Boolean) value));
        }
    }

    @Override
    public String toString() {
        if (value instanceof Float) {
            return Float.toString((Float) value);
        }
        return value.toString();
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        Number otherNum = obj instanceof Number ? (Number) obj : null;
        if (otherNum == null) {
            return false;
        }

        return value.equals(otherNum.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
