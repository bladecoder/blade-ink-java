package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

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
	public List<ListDefinition> origins;

	// Origin name needs to be serialised when content is empty,
	// assuming a name is availble, for list definitions with variable
	// that is currently empty.
	private List<String> originNames;

	/**
	 * Create a new empty ink list.
	 */
	public InkList() {
	}

	InkList(Map.Entry<InkListItem, Integer> singleElement) {
		put(singleElement.getKey(), singleElement.getValue());
	}

	/**
	 * Create a new ink list that contains the same contents as another list.
	 */
	public InkList(InkList otherList) {
		super(otherList);
		this.originNames = otherList.originNames;
	}

	ListDefinition getOriginOfMaxItem() {
		if (origins == null)
			return null;

		String maxOriginName = getMaxItem().getKey().getOriginName();
		for (ListDefinition origin : origins) {
			if (origin.getName().equals(maxOriginName))
				return origin;
		}

		return null;
	}

	List<String> getOriginNames() {
		if (this.size() > 0) {
			if (originNames == null && this.size() > 0)
				originNames = new ArrayList<String>();
			else
				originNames.clear();

			for (InkListItem itemAndValue : keySet())
				originNames.add(itemAndValue.getOriginName());
		}

		return originNames;
	}

	void setInitialOriginNames(List<String> initialOriginNames) {
		if (initialOriginNames == null)
			originNames = null;
		else {
			originNames = new ArrayList<String>();
			originNames.addAll(initialOriginNames);
		}
	}

	/**
	 * Returns a new list that is the combination of the current list and one
	 * that's passed in. Equivalent to calling (list1 + list2) in ink.
	 */
	public InkList union(InkList otherList) {
		InkList union = new InkList(this);
		for (InkListItem key : otherList.keySet())
			union.put(key, otherList.get(key));

		return union;
	}

	/**
	 * Returns a new list that's the same as the current one, except with the
	 * given items removed that are in the passed in list. Equivalent to calling
	 * (list1 - list2) in ink.
	 * 
	 * @param listToRemove
	 *            List to remove.
	 */

	public InkList without(InkList listToRemove) {
		InkList result = new InkList(this);
		for (InkListItem kv : listToRemove.keySet())
			result.remove(kv);

		return result;
	}

	/**
	 * Returns a new list that is the intersection of the current list with
	 * another list that's passed in - i.e. a list of the items that are shared
	 * between the two other lists. Equivalent to calling (list1 ^ list2) in
	 * ink.
	 */
	public InkList intersect(InkList otherList) {
		InkList intersection = new InkList();

		for (Map.Entry<InkListItem, Integer> kv : this.entrySet()) {
			if (otherList.containsKey(kv.getKey()))
				intersection.put(kv.getKey(), kv.getValue());
		}

		return intersection;
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
			if (min.getKey() == null || kv.getValue() < min.getValue())
				min.set(kv);
		}

		return min;
	}

	/**
	 * Returns true if the current list contains all the items that are in the
	 * list that is passed in. Equivalent to calling (list1 ? list2) in ink.
	 * 
	 * @param otherList
	 *            Other list.
	 */
	public boolean contains(InkList otherList) {
		for (Map.Entry<InkListItem, Integer> kv : otherList.entrySet()) {
			if (!this.containsKey(kv.getKey()))
				return false;
		}

		return true;
	}

	/**
	 * Returns true if all the item values in the current list are greater than
	 * all the item values in the passed in list. Equivalent to calling (list1
	 * &gt; list2) in ink.
	 */
	public boolean greaterThan(InkList otherList) {
		if (size() == 0)
			return false;
		if (otherList.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() > otherList.getMaxItem().getValue();
	}

	/**
	 * Returns true if the item values in the current list overlap or are all
	 * greater than the item values in the passed in list. None of the item
	 * values in the current list must fall below the item values in the passed
	 * in list. Equivalent to (list1 &gt;= list2) in ink, or LIST_MIN(list1)
	 * &gt;= LIST_MIN(list2) &amp;&amp; LIST_MAX(list1) &gt;= LIST_MAX(list2).
	 */
	public boolean greaterThanOrEquals(InkList otherList) {
		if (size() == 0)
			return false;
		if (otherList.size() == 0)
			return true;

		// All greater
		return getMinItem().getValue() >= otherList.getMinItem().getValue()
				&& getMaxItem().getValue() >= otherList.getMaxItem().getValue();
	}

	/**
	 * Returns true if all the item values in the current list are less than all
	 * the item values in the passed in list. Equivalent to calling (list1 &lt;
	 * list2) in ink.
	 */
	public boolean lessThan(InkList otherList) {
		if (otherList.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() < otherList.getMinItem().getValue();
	}

	/**
	 * Returns true if the item values in the current list overlap or are all
	 * less than the item values in the passed in list. None of the item values
	 * in the current list must go above the item values in the passed in list.
	 * Equivalent to (list1 &lt;= list2) in ink, or LIST_MAX(list1) &lt;=
	 * LIST_MAX(list2) &amp;&amp; LIST_MIN(list1) &lt;= LIST_MIN(list2).
	 */
	public boolean lessThanOrEquals(InkList otherList) {
		if (otherList.size() == 0)
			return false;
		if (size() == 0)
			return true;

		return getMaxItem().getValue() <= otherList.getMaxItem().getValue()
				&& getMinItem().getValue() <= otherList.getMinItem().getValue();
	}

	InkList maxAsList() {
		if (size() > 0)
			return new InkList(getMaxItem());
		else
			return new InkList();
	}

	InkList minAsList() {
		if (size() > 0)
			return new InkList(getMinItem());
		else
			return new InkList();
	}

	// Runtime sets may reference items from different origin sets
	public String getSingleOriginListName() {
		String name = null;

		for (Map.Entry<InkListItem, Integer> itemAndValue : entrySet()) {
			String originName = itemAndValue.getKey().getOriginName();

			// First name - take it as the assumed single origin name
			if (name == null)
				name = originName;

			// A different one than one we've already had? No longer
			// single origin.
			else if (name != originName)
				return null;
		}

		return name;
	}

	/**
	 * The inverse of the list, equivalent to calling LIST_INVERSE(list) in ink
	 */
	public InkList getInverse() {

		InkList rawList = new InkList();

		if (origins != null) {
			for (ListDefinition origin : origins) {
				for (Map.Entry<InkListItem, Integer> itemAndValue : origin.getItems().entrySet()) {

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
	 * Adds the given item to the ink list. Note that the item must come from a
	 * list definition that is already "known" to this list, so that the item's
	 * value can be looked up. By "known", we mean that it already has items in
	 * it from that source, or it did at one point - it can't be a completely
	 * fresh empty list, or a list that only contains items from a different
	 * list definition.
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
				"Failed to add item to list because the item was from a new list definition that wasn't previously known to this list. Only items from previously known lists can be used, so that the int value can be found.");
	}

	/**
	 * Adds the given item to the ink list, attempting to find the origin list
	 * definition that it belongs to. The item must therefore come from a list
	 * definition that is already "known" to this list, so that the item's value
	 * can be looked up. By "known", we mean that it already has items in it
	 * from that source, or it did at one point - it can't be a completely fresh
	 * empty list, or a list that only contains items from a different list
	 * definition.
	 * 
	 * @throws Exception
	 */
	public void addItem(String itemName) throws Exception {
		ListDefinition foundListDef = null;

		for (ListDefinition origin : origins) {
			if (origin.containsItemWithName(itemName)) {
				if (foundListDef != null) {
					throw new Exception(
							"Could not add the item " + itemName + " to this list because it could come from either "
									+ origin.getName() + " or " + foundListDef.getName());
				} else {
					foundListDef = origin;
				}
			}
		}

		if (foundListDef == null)
			throw new Exception("Could not add the item " + itemName
					+ " to this list because it isn't known to any list definitions previously associated with this list.");

		InkListItem item = new InkListItem(foundListDef.getName(), itemName);
		Integer itemVal = foundListDef.getValueForItem(item);
		this.put(item, itemVal != null ? itemVal : 0);
	}

	/**
	 * Returns true if this ink list contains an item with the given short name
	 * (ignoring the original list where it was defined).
	 */
	public boolean ContainsItemNamed(String itemName) {
		for (Map.Entry<InkListItem, Integer> itemWithValue : this.entrySet()) {
			if (itemWithValue.getKey().getItemName().equals(itemName))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if the passed object is also an ink list that contains the
	 * same items as the current list, false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		InkList otherRawList = null;

		if (other instanceof InkList)
			otherRawList = (InkList) other;

		if (otherRawList == null)
			return false;
		if (otherRawList.size() != size())
			return false;

		for (InkListItem key : keySet()) {
			if (!otherRawList.containsKey(key))
				return false;
		}

		return true;
	}

	/**
	 * Return the hashcode for this object, used for comparisons and inserting
	 * into dictionaries.
	 */
	@Override
	public int hashCode() {
		int ownHash = 0;

		for (InkListItem key : keySet())
			ownHash += key.hashCode();

		return ownHash;
	}

	/**
	 * Returns a string in the form "a, b, c" with the names of the items in the
	 * list, without the origin list definition names. Equivalent to writing
	 * {list} in ink.
	 */
	@Override
	public String toString() {
		List<InkListItem> ordered = new ArrayList<InkListItem>(keySet());

		Collections.sort(ordered, new Comparator<InkListItem>() {
			@Override
			public int compare(InkListItem o1, InkListItem o2) {
				return get(o1) - get(o2);
			}
		});

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < ordered.size(); i++) {
			if (i > 0)
				sb.append(", ");

			InkListItem item = ordered.get(i);

			sb.append(item.getItemName());
		}

		return sb.toString();
	}

	public class CustomEntry implements Map.Entry<InkListItem, Integer> {

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
