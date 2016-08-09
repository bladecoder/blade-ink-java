package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Path {
	private final static String PARENT_ID = "^";

	private List<Component> components;
	private boolean isRelative;

	public Path() {
		setComponents(new ArrayList<Component>());
	}

	public Path(Component head, Path tail) {
		this();

		getComponents().add(head);
		getComponents().addAll(tail.getComponents());
	}

	public Path(Collection<Component> components) {
		this(components, false);
	}
	
	public Path(Collection<Component> components, boolean relative) {
		this();
		getComponents().addAll(components);
		
		this.isRelative = relative;
	}

	public Path(String componentsString) {
		this();
		setComponentsString(componentsString);
	}

	public List<Component> getComponents() {
		return components;
	}

	private void setComponents(List<Component> value) {
		components = value;
	}

	public boolean isRelative() {
		return isRelative;
	}

	private void setRelative(boolean value) {
		isRelative = value;
	}

	public Component getHead() {
		if (getComponents().size() > 0) {
			return getComponents().get(0);
		} else {
			return null;
		}
	}

	public Path getTail() {
		if (getComponents().size() >= 2) {
			List<Component> tailComps = getComponents().subList(1, getComponents().size());

			return new Path(tailComps);
		} else {
			return Path.getSelf();
		}
	}

	public int getLength() {
		return getComponents().size();
	}

	public Component getLastComponent() {
		if (getComponents().size() > 0) {
			return getComponents().get(getComponents().size() - 1);
		} else {
			return null;
		}
	}

	public boolean containsNamedComponent() {
		for (Component comp : getComponents()) {
			if (!comp.isIndex()) {
				return true;
			}

		}
		return false;
	}

	public static Path getSelf() {
		Path path = new Path();
		path.setRelative(true);
		return path;
	}

	public Path pathByAppendingPath(Path pathToAppend) {
		Path p = new Path();
		int upwardMoves = 0;
		for (int i = 0; i < pathToAppend.getComponents().size(); ++i) {
			if (pathToAppend.getComponents().get(i).isParent()) {
				upwardMoves++;
			} else {
				break;
			}
		}
		for (int i = 0; i < this.getComponents().size() - upwardMoves; ++i) {
			p.getComponents().add(this.getComponents().get(i));
		}
		for (int i = upwardMoves; i < pathToAppend.getComponents().size(); ++i) {
			p.getComponents().add(pathToAppend.getComponents().get(i));
		}
		return p;
	}

	public String getComponentsString() {
		// String compsStr = String.join(".", getcomponents().toArray());

		StringBuilder sb = new StringBuilder();

		if (getComponents().size() > 0) {

			sb.append(getComponents().get(0));

			for (int i = 1; i < getComponents().size(); i++) {
				sb.append('.');
				sb.append(getComponents().get(i));
			}
		}

		String compsStr = sb.toString();

		if (isRelative())
			return "." + compsStr;
		else
			return compsStr;
	}

	private void setComponentsString(String value) {
		getComponents().clear();
		String componentsStr = value;

		// Empty path, empty components
		// (path is to root, like "/" in file system)
		if (componentsStr == null || componentsStr.isEmpty())
			return;

		// When components start with ".", it indicates a relative path, e.g.
		// .^.^.hello.5
		// is equivalent to file system style path:
		// ../../hello/5
		if (componentsStr.charAt(0) == '.') {
			setRelative(true);
			componentsStr = componentsStr.substring(1);
		} else {
			setRelative(false);
		}

		String[] componentStrings = componentsStr.split("\\.");

		for (String str : componentStrings) {
			int index = 0;

			try {
				index = Integer.parseInt(str);
				getComponents().add(new Component(index));
			} catch (NumberFormatException e) {
				getComponents().add(new Component(str));
			}
		}
	}

	@Override
	public String toString() {
		return getComponentsString();
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj instanceof Path ? (Path) obj : (Path) null);
	}

	public boolean equals(Path otherPath) {
		if (otherPath == null)
			return false;

		if (otherPath.getComponents().size() != this.getComponents().size())
			return false;

		if (otherPath.isRelative() != this.isRelative())
			return false;

		// return
		// otherPath.getcomponents().SequenceEqual(this.getcomponents());
		for (int i = 0; i < otherPath.getComponents().size(); i++) {
			if (!otherPath.getComponents().get(i).equals(getComponents().get(i)))
				return false;
		}

		return true;

	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	// Immutable Component
	public static class Component {
		private int index;
		private String name;

		public Component(int index) {
			// Debug.Assert(index >= 0);
			this.setIndex(index);
			this.setName(null);
		}

		public Component(String name) {
			// Debug.Assert(name != null && name.Length > 0);
			this.setName(name);
			this.setIndex(-1);
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int value) {
			index = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String value) {
			name = value;
		}

		public boolean isIndex() {
			return getIndex() >= 0;
		}

		public boolean isParent() {
			return Path.PARENT_ID.equals(getName());
		}

		public static Component toParent() {
			return new Component(PARENT_ID);
		}

		@Override
		public String toString() {
			if (isIndex()) {
				return Integer.toString(getIndex());
			} else {
				return getName();
			}
		}

		@Override
		public boolean equals(Object obj) {

			return equals(obj instanceof Component ? (Component) obj : (Component) null);

		}

		public boolean equals(Component otherComp) {

			if (otherComp != null && otherComp.isIndex() == this.isIndex()) {
				if (isIndex()) {
					return getIndex() == otherComp.getIndex();
				} else {
					return getName().equals(otherComp.getName());
				}
			}

			return false;
		}

		@Override
		public int hashCode() {
			if (isIndex())
				return getIndex();
			else
				return getName().hashCode();

		}

	}

}