package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The InkList is the underlying type that's used to store an instance of a list
 * in ink. It's not used for the *definition* of the list, but for a list value
 * that's stored in a variable. Somewhat confusingly, it's backed by a C#
 * Dictionary, and has nothing to do with a C# List!
 */
@SuppressWarnings("serial")
public class InkList extends HashMap<InkListItem, Integer> {
    // Story has to set this so that the value knows its origin,
    // necessary for certain operations (e.g. interacting with ints).
    // Only the story has access to the full set of lists, so that
    // the origin can be resolved from the originListName.
    private List<ListDefinition> origins;

    // Origin name needs to be serialised when content is empty,
    // assuming a name is availble, for list definitions with variable
    // that is currently empty.
    private List<String> originNames;

    /**
     * Create a new empty ink list.
     */
    public InkList() {}

    public InkList(Map.Entry<InkListItem, Integer> singleElement) {
        put(singleElement.getKey(), singleElement.getValue());
    }

    /**
     * Create a new empty ink list that's intended to hold items from a particular
     * origin list definition. The origin Story is needed in order to be able to
     * look up that definition.
     */
    public InkList(String singleOriginListName, Story originStory) throws Exception {
        setInitialOriginName(singleOriginListName);

        ListDefinition def = originStory.getListDefinitions().getListDefinition(singleOriginListName);

        if (def != null) {
            origins = new ArrayList<>();
            origins.add(def);
        } else
            throw new Exception(
                    "InkList origin could not be found in story when constructing new list: " + singleOriginListName);
    }

    /**
     * Create a new ink list that contains the same contents as another list.
     */
    public InkList(InkList otherList) {
        super(otherList);

        if (otherList.originNames != null) this.originNames = new ArrayList<>(otherList.originNames);

        if (otherList.origins != null) {
            origins = new ArrayList<>(otherList.origins);
        }
    }

    /**
     * Converts a string to an ink list and returns for use in the story.
     */
    public static InkList fromString(String myListItem, Story originStory) throws Exception {
        if (myListItem == null || myListItem.isEmpty()) return new InkList();

        ListValue listValue = originStory.getListDefinitions().findSingleItemListWithName(myListItem);
        if (listValue != null) return new InkList(listValue.value);
        else
            throw new Exception("Could not find the InkListItem from the string '" + myListItem
                    + "' to create an InkList because it doesn't exist in the original list definition in ink.");
    }

    ListDefinition getOriginOfMaxItem() {
        if (origins == null) return null;

        String maxOriginName = getMaxItem().getKey().getOriginName();
        for (ListDefinition origin : origins) {
            if (origin.getName().equals(maxOriginName)) return origin;
        }

        return null;
    }

    public void setOrigins(List<ListDefinition> origins) {
        this.origins = origins;
    }

    public List<ListDefinition> getOrigins() {
        return origins;
    }

    public List<String> getOriginNames() {
        if (this.size() > 0) {
            if (originNames == null && this.size() > 0) originNames = new ArrayList<>();
            else originNames.clear();

            for (InkListItem itemAndValue : keySet()) originNames.add(itemAndValue.getOriginName());
        }

        return originNames;
    }

    public void setInitialOriginName(String initialOriginName) {
        originNames = new ArrayList<>();
        originNames.add(initialOriginName);
    }

    public void setInitialOriginNames(List<String> initialOriginNames) {
        if (initialOriginNames == null) originNames = null;
        else {
            originNames = new ArrayList<>();
            originNames.addAll(initialOriginNames);
        }
    }

    /**
     * Returns a new list that is the combination of the current list and one that's
     * passed in. Equivalent to calling (list1 + list2) in ink.
     */
    public InkList union(InkList otherList) {
        InkList union = new InkList(this);
        for (InkListItem key : otherList.keySet()) union.put(key, otherList.get(key));

        return union;
    }

    /**
     * Returns a new list that's the same as the current one, except with the given
     * items removed that are in the passed in list. Equivalent to calling (list1 -
     * list2) in ink.
     *
     * @param listToRemove List to remove.
     */
    public InkList without(InkList listToRemove) {
        InkList result = new InkList(this);
        for (InkListItem kv : listToRemove.keySet()) result.remove(kv);

        return result;
    }

    /**
     * Returns a new list that is the intersection of the current list with another
     * list that's passed in - i.e. a list of the items that are shared between the
     * two other lists. Equivalent to calling (list1 ^ list2) in ink.
     */
    public InkList intersect(InkList otherList) {
        InkList intersection = new InkList();

        for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
            if (otherList.containsKey(kv.getKey())) intersection.put(kv.getKey(), kv.getValue());
        }

        return intersection;
    }

    /**
     * Fast test for the existence of any intersection between the current list and another
     */
    public boolean hasIntersection(InkList otherList) {
        for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
            if (otherList.containsKey(kv.getKey())) return true;
        }
        return false;
    }

    /**
     * Get the maximum item in the list, equivalent to calling LIST_MAX(list) in
     * ink.
     */
    public Map.Entry<InkListItem, Integer> getMaxItem() {
        CustomEntry max = new CustomEntry(null, 0);

        for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
            if (max.getKey() == null || kv.getValue() > max.getValue()) {
                max.set(kv);
            }
        }

        return max;
    }

    /**
     * Get the minimum item in the list, equivalent to calling LIST_MIN(list) in
     * ink.
     */
    public Map.Entry<InkListItem, Integer> getMinItem() {
        CustomEntry min = new CustomEntry(null, 0);

        for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
            if (min.getKey() == null || kv.getValue() < min.getValue()) min.set(kv);
        }

        return min;
    }

    /**
     * Returns true if the current list contains all the items that are in the list
     * that is passed in. Equivalent to calling (list1 ? list2) in ink.
     *
     * @param otherList Other list.
     */
    public boolean contains(InkList otherList) {
        if (otherList.size() == 0 || this.size() == 0) return false;

        for (Map.Entry<InkListItem, Integer> kv : otherList.entrySet()) {
            if (!this.containsKey(kv.getKey())) return false;
        }

        return true;
    }

    /**
     * Returns true if the current list contains an item matching the given name.
     */
    public boolean contains(String listItemName) {
        for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
            if (Objects.equals(kv.getKey().getItemName(), listItemName)) return true;
        }
        return false;
    }

    /**
     * Returns true if all the item values in the current list are greater than all
     * the item values in the passed in list. Equivalent to calling (list1 &gt;
     * list2) in ink.
     */
    public boolean greaterThan(InkList otherList) {
        if (size() == 0) return false;
        if (otherList.size() == 0) return true;

        // All greater
        return getMinItem().getValue() > otherList.getMaxItem().getValue();
    }

    /**
     * Returns true if the item values in the current list overlap or are all
     * greater than the item values in the passed in list. None of the item values
     * in the current list must fall below the item values in the passed in list.
     * Equivalent to (list1 &gt;= list2) in ink, or LIST_MIN(list1) &gt;=
     * LIST_MIN(list2) &amp;&amp; LIST_MAX(list1) &gt;= LIST_MAX(list2).
     */
    public boolean greaterThanOrEquals(InkList otherList) {
        if (size() == 0) return false;
        if (otherList.size() == 0) return true;

        // All greater
        return getMinItem().getValue() >= otherList.getMinItem().getValue()
                && getMaxItem().getValue() >= otherList.getMaxItem().getValue();
    }

    /**
     * Returns true if all the item values in the current list are less than all the
     * item values in the passed in list. Equivalent to calling (list1 &lt; list2)
     * in ink.
     */
    public boolean lessThan(InkList otherList) {
        if (otherList.size() == 0) return false;
        if (size() == 0) return true;

        return getMaxItem().getValue() < otherList.getMinItem().getValue();
    }

    /**
     * Returns true if the item values in the current list overlap or are all less
     * than the item values in the passed in list. None of the item values in the
     * current list must go above the item values in the passed in list. Equivalent
     * to (list1 &lt;= list2) in ink, or LIST_MAX(list1) &lt;= LIST_MAX(list2)
     * &amp;&amp; LIST_MIN(list1) &lt;= LIST_MIN(list2).
     */
    public boolean lessThanOrEquals(InkList otherList) {
        if (otherList.size() == 0) return false;
        if (size() == 0) return true;

        return getMaxItem().getValue() <= otherList.getMaxItem().getValue()
                && getMinItem().getValue() <= otherList.getMinItem().getValue();
    }

    InkList maxAsList() {
        if (size() > 0) return new InkList(getMaxItem());
        else return new InkList();
    }

    InkList minAsList() {
        if (size() > 0) return new InkList(getMinItem());
        else return new InkList();
    }

    /**
     * Returns a sublist with the elements given the minimum and maxmimum bounds.
     * The bounds can either be ints which are indices into the entire (sorted)
     * list, or they can be InkLists themselves. These are intended to be
     * single-item lists so you can specify the upper and lower bounds. If you pass
     * in multi-item lists, it'll use the minimum and maximum items in those lists
     * respectively. WARNING: Calling this method requires a full sort of all the
     * elements in the list.
     *
     * @throws Exception
     * @throws StoryException
     */
    public InkList listWithSubRange(Object minBound, Object maxBound) throws StoryException, Exception {
        if (this.size() == 0) return new InkList();

        List<Entry<InkListItem, Integer>> ordered = getOrderedItems();
        int minValue = 0;
        int maxValue = Integer.MAX_VALUE;

        if (minBound instanceof Integer) {
            minValue = (int) minBound;
        } else {
            if (minBound instanceof InkList && ((InkList) minBound).size() > 0)
                minValue = ((InkList) minBound).getMinItem().getValue();
        }

        if (maxBound instanceof Integer) maxValue = (int) maxBound;
        else {
            if (maxBound instanceof InkList && ((InkList) maxBound).size() > 0)
                maxValue = ((InkList) maxBound).getMaxItem().getValue();
        }

        InkList subList = new InkList();
        subList.setInitialOriginNames(originNames);

        for (Entry<InkListItem, Integer> item : ordered) {
            if (item.getValue() >= minValue && item.getValue() <= maxValue) {
                subList.put(item.getKey(), item.getValue());
            }
        }

        return subList;
    }

    // Runtime sets may reference items from different origin sets
    public String getSingleOriginListName() {
        String name = null;

        for (Map.Entry<InkListItem, Integer> itemAndValue : entrySet()) {
            String originName = itemAndValue.getKey().getOriginName();

            // First name - take it as the assumed single origin name
            if (name == null) name = originName;

            // A different one than one we've already had? No longer
            // single origin.
            else if (name != originName) return null;
        }

        return name;
    }

    /**
     * If you have an InkList that's known to have one single item, this is a convenient way to get it.
     */
    public InkListItem getSingleItem() {
        for (Map.Entry<InkListItem, Integer> item : this.entrySet()) {
            return item.getKey();
        }

        return null;
    }

    /**
     * The inverse of the list, equivalent to calling LIST_INVERSE(list) in ink
     */
    public InkList getInverse() {

        InkList rawList = new InkList();

        if (origins != null) {
            for (ListDefinition origin : origins) {
                for (Map.Entry<InkListItem, Integer> itemAndValue :
                        origin.getItems().entrySet()) {

                    if (!this.containsKey(itemAndValue.getKey()))
                        rawList.put(itemAndValue.getKey(), itemAndValue.getValue());
                }
            }
        }

        return rawList;
    }

    /**
     * The list of all items from the original list definition, equivalent to
     * calling LIST_ALL(list) in ink.
     */
    public InkList getAll() {

        InkList list = new InkList();

        if (origins != null) {
            for (ListDefinition origin : origins) {
                for (Map.Entry<InkListItem, Integer> kv : origin.getItems().entrySet()) {
                    list.put(kv.getKey(), kv.getValue());
                }
            }
        }

        return list;
    }

    /**
     * Adds the given item to the ink list. Note that the item must come from a list
     * definition that is already "known" to this list, so that the item's value can
     * be looked up.
     * By "known", we mean that it already has items in it from that
     * source, or it did at one point - it can't be a completely fresh empty list,
     * or a list that only contains items from a different list definition.
     *
     * @throws Exception
     */
    public void addItem(InkListItem item) throws Exception {
        if (item.getOriginName() == null) {
            addItem(item.getItemName());
            return;
        }

        for (ListDefinition origin : origins) {
            if (origin.getName().equals(item.getOriginName())) {
                Integer intVal = origin.getValueForItem(item);

                if (intVal != null) {
                    this.put(item, intVal);
                    return;
                } else {
                    throw new Exception("Could not add the item " + item
                            + " to this list because it doesn't exist in the original list definition in ink.");
                }
            }
        }

        throw new Exception(
                "Failed to add item to list because the item was from a new list definition that wasn't previously "
                        + "known to this list. Only items from previously known lists can be used, so that the int "
                        + "value"
                        + " can be found.");
    }

    /**
     * Adds the given item to the ink list, attempting to find the origin list
     * definition that it belongs to. The item must therefore come from a list
     * definition that is already "known" to this list, so that the item's value can
     * be looked up. By "known", we mean that it already has items in it from that
     * source, or it did at one point - it can't be a completely fresh empty list,
     * or a list that only contains items from a different list definition.
     * You can also provide the Story object, so in the case of an unknown element, it can be created fresh.
     *
     * @throws Exception
     */
    public void addItem(String itemName, Story storyObject) throws Exception {
        ListDefinition foundListDef = null;

        if (origins != null) {
            for (ListDefinition origin : origins) {
                if (origin.containsItemWithName(itemName)) {
                    if (foundListDef != null) {
                        throw new Exception("Could not add the item " + itemName
                                + " to this list because it could come from either " + origin.getName() + " or "
                                + foundListDef.getName());
                    } else {
                        foundListDef = origin;
                    }
                }
            }
        }

        if (foundListDef == null) {
            if (storyObject == null) {
                throw new Exception("Could not add the item " + itemName
                        + " to this list because it isn't known to any list definitions previously associated with this "
                        + "list.");
            } else {
                Entry<InkListItem, Integer> newItem =
                        fromString(itemName, storyObject).getOrderedItems().get(0);
                this.put(newItem.getKey(), newItem.getValue());
            }
        } else {
            InkListItem item = new InkListItem(foundListDef.getName(), itemName);
            Integer itemVal = foundListDef.getValueForItem(item);
            this.put(item, itemVal != null ? itemVal : 0);
        }
    }

    public void addItem(String itemName) throws Exception {
        addItem(itemName, null);
    }

    /**
     * Returns true if this ink list contains an item with the given short name
     * (ignoring the original list where it was defined).
     */
    public boolean containsItemNamed(String itemName) {
        for (Map.Entry<InkListItem, Integer> itemWithValue : this.entrySet()) {
            if (itemWithValue.getKey().getItemName().equals(itemName)) return true;
        }
        return false;
    }

    /**
     * Returns true if the passed object is also an ink list that contains the same
     * items as the current list, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        InkList otherRawList = null;

        if (other instanceof InkList) otherRawList = (InkList) other;

        if (otherRawList == null) return false;
        if (otherRawList.size() != size()) return false;

        for (InkListItem key : keySet()) {
            if (!otherRawList.containsKey(key)) return false;
        }

        return true;
    }

    /**
     * Return the hashcode for this object, used for comparisons and inserting into
     * dictionaries.
     */
    @Override
    public int hashCode() {
        int ownHash = 0;

        for (InkListItem key : keySet()) ownHash += key.hashCode();

        return ownHash;
    }

    List<Entry<InkListItem, Integer>> getOrderedItems() {
        List<Entry<InkListItem, Integer>> ordered = new ArrayList<>(entrySet());

        Collections.sort(ordered, new Comparator<Entry<InkListItem, Integer>>() {
            @Override
            public int compare(Entry<InkListItem, Integer> o1, Entry<InkListItem, Integer> o2) {
                if (o1.getValue() == o2.getValue()) {
                    return o1.getKey().getOriginName().compareTo(o2.getKey().getOriginName());
                } else {
                    return o1.getValue() - o2.getValue();
                }
            }
        });

        return ordered;
    }

    /**
     * Returns a string in the form "a, b, c" with the names of the items in the
     * list, without the origin list definition names. Equivalent to writing {list}
     * in ink.
     */
    @Override
    public String toString() {
        List<Entry<InkListItem, Integer>> ordered = getOrderedItems();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) sb.append(", ");

            InkListItem item = ordered.get(i).getKey();

            sb.append(item.getItemName());
        }

        return sb.toString();
    }

    public static class CustomEntry implements Map.Entry<InkListItem, Integer> {

        private InkListItem key;
        private Integer value;

        CustomEntry(InkListItem key, Integer value) {
            set(key, value);
        }

        public void set(InkListItem key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public void set(Map.Entry<InkListItem, Integer> e) {
            key = e.getKey();
            value = e.getValue();
        }

        @Override
        public InkListItem getKey() {
            return key;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public Integer setValue(Integer value) {
            Integer old = this.value;
            this.value = value;

            return old;
        }

        public void setKey(InkListItem key) {
            this.key = key;
        }
    }
}
