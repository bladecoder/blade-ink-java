package com.bladecoder.ink.runtime;

/**
 * The underlying type for a list item in ink. It stores the original list
 * definition name as well as the item name, but without the value of the item.
 * When the value is stored, it's stored in a KeyValuePair of InkListItem and
 * int.
 */
public class InkListItem {
	/**
	 * The name of the list where the item was originally defined.
	 */
	private String originName;

	/**
	 * The main name of the item as defined in ink.
	 */
	private String itemName;

	/**
	 * Create an item with the given original list definition name, and the name
	 * of this item.
	 */
	public InkListItem(String originName, String itemName) {
		this.originName = originName;
		this.itemName = itemName;
	}

	/**
	 * Create an item from a dot-separted string of the form
	 * "listDefinitionName.listItemName".
	 */
	public InkListItem(String fullName) {
		String[] nameParts = fullName.split("\\.");
		this.originName = nameParts[0];
		this.itemName = nameParts[1];
	}

	static InkListItem getNull() {
		return new InkListItem(null, null);
	}

	public String getOriginName() {
		return originName;
	}

	public String getItemName() {
		return itemName;
	}

	/**
	 * Get the full dot-separated name of the item, in the form
	 * "listDefinitionName.itemName".
	 */
	public String getFullName() {
		return (originName != null ? originName : "?") + "." + itemName;
	}

	boolean isNull() {
		return originName == null && itemName == null;
	}

	/**
	 * Get the full dot-separated name of the item, in the form
	 * "listDefinitionName.itemName". Calls fullName internally.
	 */
	@Override
	public String toString() {
		return getFullName();
	}

	/**
	 * Is this item the same as another item?
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InkListItem) {
			InkListItem otherItem = (InkListItem) obj;
			return otherItem.itemName.equals(itemName) && otherItem.originName.equals(originName);
		}

		return false;
	}

	/**
	 * Get the hashcode for an item.
	 */
	@Override
	public int hashCode() {
		int originCode = 0;
		int itemCode = itemName.hashCode();
		if (originName != null)
			originCode = originName.hashCode();

		return originCode + itemCode;
	}
}
