package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.InkStringConversionExtensions;
import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import java.util.ArrayList;
import java.util.List;

public class ContentList extends ParsedObject {
    private boolean dontFlatten;

    public Container getRuntimeContainer() {
        return (Container) getRuntimeObject();
    }

    public ContentList(List<ParsedObject> objects) {
        if (objects != null) {
            addContent(objects);
        }
    }

    public ContentList(ParsedObject... objects) {
        if (objects != null) {
            List<ParsedObject> objList = new ArrayList<>();
            for (ParsedObject obj : objects) {
                objList.add(obj);
            }
            addContent(objList);
        }
    }

    public ContentList() {}

    public boolean isDontFlatten() {
        return dontFlatten;
    }

    public void setDontFlatten(boolean value) {
        dontFlatten = value;
    }

    public void trimTrailingWhitespace() {
        if (content == null) {
            return;
        }
        for (int i = content.size() - 1; i >= 0; --i) {
            Text text = content.get(i) instanceof Text ? (Text) content.get(i) : null;
            if (text == null) {
                break;
            }

            text.setText(text.getText().replaceAll("[ \t]+$", ""));
            if (text.getText().isEmpty()) {
                content.remove(i);
            } else {
                break;
            }
        }
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        Container container = new Container();
        if (content != null) {
            for (ParsedObject obj : content) {
                com.bladecoder.ink.runtime.RTObject contentObjRuntime = obj.getRuntimeObject();
                if (contentObjRuntime != null) {
                    RuntimeUtils.addContent(container, contentObjRuntime);
                }
            }
        }

        if (dontFlatten) {
            getStory().dontFlattenContainer(container);
        }

        return container;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContentList(");
        if (content != null) {
            sb.append(String.join(", ", InkStringConversionExtensions.toStringsArray(content)));
        }
        sb.append(")");
        return sb.toString();
    }
}
