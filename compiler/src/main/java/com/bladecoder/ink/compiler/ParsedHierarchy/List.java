package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.compiler.RuntimeUtils;
import com.bladecoder.ink.runtime.Container;
import com.bladecoder.ink.runtime.InkList;
import com.bladecoder.ink.runtime.InkListItem;
import com.bladecoder.ink.runtime.ListValue;

public class List extends Expression {
    public java.util.List<Identifier> itemIdentifierList;

    public List(java.util.List<Identifier> itemIdentifierList) {
        this.itemIdentifierList = itemIdentifierList;
    }

    @Override
    public void generateIntoContainer(Container container) {
        InkList runtimeRawList = new InkList();

        if (itemIdentifierList != null) {
            for (Identifier itemIdentifier : itemIdentifierList) {
                String[] nameParts = itemIdentifier != null ? itemIdentifier.name.split("\\.") : new String[0];

                String listName = null;
                String listItemName = null;
                if (nameParts.length > 1) {
                    listName = nameParts[0];
                    listItemName = nameParts[1];
                } else if (nameParts.length == 1) {
                    listItemName = nameParts[0];
                }

                ListElementDefinition listItem = getStory().resolveListItem(listName, listItemName, this);
                if (listItem == null) {
                    if (listName == null) {
                        error("Could not find list definition that contains item '" + itemIdentifier + "'");
                    } else {
                        error("Could not find list item " + itemIdentifier);
                    }
                } else {
                    if (listName == null) {
                        listName = ((ListDefinition) listItem.parent).identifier != null
                                ? ((ListDefinition) listItem.parent).identifier.name
                                : null;
                    }
                    InkListItem item = new InkListItem(listName, listItem.getName());

                    if (runtimeRawList.containsKey(item)) {
                        warning("Duplicate of item '" + itemIdentifier + "' in list.");
                    } else {
                        runtimeRawList.put(item, listItem.seriesValue);
                    }
                }
            }
        }

        RuntimeUtils.addContent(container, new ListValue(runtimeRawList));
    }
}
