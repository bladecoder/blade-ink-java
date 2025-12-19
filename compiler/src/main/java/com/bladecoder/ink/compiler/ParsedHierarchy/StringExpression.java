package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.ControlCommand;
import java.util.List;

public class StringExpression extends Expression {
    public boolean isSingleString() {
        if (content == null || content.size() != 1) {
            return false;
        }

        return content.get(0) instanceof Text;
    }

    public StringExpression(List<ParsedObject> content) {
        addContent(content);
    }

    @Override
    public void generateIntoContainer(Container container) {
        RuntimeUtils.addContent(container, ControlCommand.CommandType.BeginString);

        for (ParsedObject c : content) {
            RuntimeUtils.addContent(container, c.getRuntimeObject());
        }

        RuntimeUtils.addContent(container, ControlCommand.CommandType.EndString);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ParsedObject c : content) {
            sb.append(c.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        StringExpression otherStr = obj instanceof StringExpression ? (StringExpression) obj : null;
        if (otherStr == null) {
            return false;
        }

        if (!isSingleString() || !otherStr.isSingleString()) {
            return false;
        }

        return toString().equals(otherStr.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
