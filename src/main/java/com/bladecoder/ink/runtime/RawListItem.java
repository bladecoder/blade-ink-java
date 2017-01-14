package com.bladecoder.ink.runtime;

class RawListItem {
	private String originName;
	private String itemName;

	public String getOriginName() {
		return originName;
	}

	public String getItemName() {
		return itemName;
	}

	public RawListItem(String originName, String itemName) {
		this.originName = originName;
		this.itemName = itemName;
	}
	
	public static RawListItem getNull() {
		return new RawListItem (null, null);
	}

	public boolean isNull() {
		return originName == null && itemName == null;
	}

	@Override
	public String toString() {
		return (originName != null ? originName : "?") + "." + itemName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RawListItem) {
			RawListItem otherItem = (RawListItem) obj;
			return otherItem.itemName == itemName && otherItem.originName == originName;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int originCode = 0;
		int itemCode = itemName.hashCode();
		if (originName != null)
			originCode = originName.hashCode();

		return originCode + itemCode;
	}
}
