package com.bladecoder.ink.compiler.ParsedHierarchy;

import com.bladecoder.ink.runtime.InkList;
import com.bladecoder.ink.runtime.InkListItem;
import com.bladecoder.ink.runtime.ListValue;
import java.util.HashMap;
import java.util.List;

public class ListDefinition extends ParsedObject {
    public Identifier identifier;
    public List<ListElementDefinition> itemDefinitions;

    public VariableAssignment variableAssignment;

    private HashMap<String, ListElementDefinition> elementsByName;

    public ListDefinition(List<ListElementDefinition> elements) {
        itemDefinitions = elements;

        int currentValue = 1;
        for (ListElementDefinition e : itemDefinitions) {
            if (e.explicitValue != null) {
                currentValue = e.explicitValue;
            }

            e.seriesValue = currentValue;

            currentValue++;
        }

        addContent(elements);
    }

    public com.bladecoder.ink.runtime.ListDefinition getRuntimeListDefinition() {
        HashMap<String, Integer> allItems = new HashMap<>();
        for (ListElementDefinition e : itemDefinitions) {
            if (!allItems.containsKey(e.getName())) {
                allItems.put(e.getName(), e.seriesValue);
            } else {
                error("List '" + identifier + "' contains duplicate items called '" + e.getName() + "'");
            }
        }

        return new com.bladecoder.ink.runtime.ListDefinition(identifier != null ? identifier.name : null, allItems);
    }

    public ListElementDefinition itemNamed(String itemName) {
        if (elementsByName == null) {
            elementsByName = new HashMap<>();
            for (ListElementDefinition el : itemDefinitions) {
                elementsByName.put(el.getName(), el);
            }
        }

        return elementsByName.get(itemName);
    }

    @Override
    public com.bladecoder.ink.runtime.RTObject generateRuntimeObject() {
        InkList initialValues = new InkList();
        for (ListElementDefinition itemDef : itemDefinitions) {
            if (itemDef.inInitialList) {
                InkListItem item = new InkListItem(identifier != null ? identifier.name : null, itemDef.getName());
                initialValues.put(item, itemDef.seriesValue);
            }
        }

        initialValues.setInitialOriginName(identifier != null ? identifier.name : null);

        return new ListValue(initialValues);
    }

    @Override
    public void resolveReferences(Story context) {
        super.resolveReferences(context);

        context.checkForNamingCollisions(this, identifier, Story.SymbolType.List, null);
    }

    @Override
    public String getTypeName() {
        return "List definition";
    }
}
