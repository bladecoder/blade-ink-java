package com.bladecoder.ink.runtime;

/**
 * Internal structure used to point to a particular / current point in the
 * story. Where Path is a set of components that make content fully addressable,
 * this is a reference to the current container, and the index of the current
 * piece of content within that container. This scheme makes it as fast and
 * efficient as possible to increment the pointer (move the story forwards) in a
 * way that's as native to the internal engine as possible.
 * 
 * @author rgarcia
 *
 */
class Pointer {
	public Container container;
	public int index;

	public Pointer() {
		
	}
	
	public Pointer(Pointer p) {
		assign(p);
	}
	
	public Pointer(Container container, int index) {
		this.container = container;
		this.index = index;
	}
	
	public void assign(Pointer p) {
		container = p.container;
		index = p.index;
	}

	public RTObject resolve() {
		return index >= 0 ? container.getContent().get(index) : container;
	}

	public boolean isNull() {
		return container == null;
	}

	public Path getPath() {
		if (isNull())
			return null;

		if (index >= 0)
			return container.getPath().pathByAppendingComponent(new Path.Component(index));
		else
			return container.getPath();
	}

	@Override
	public String toString() {
		if (container == null)
			return "Ink Pointer (null)";

		return "Ink Pointer -> " + container.getPath().toString() + " -- index " + index;
	}

	public static Pointer startOf(Container container) {
		return new Pointer(container, 0);
	}

	public static final Pointer Null = new Pointer(null, -1);
}
