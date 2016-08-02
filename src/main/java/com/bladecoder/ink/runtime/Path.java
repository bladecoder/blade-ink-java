package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Path { // extends IEquatable<Path> {
	static String parentId = "^";

	private List<Component> components;
	private boolean isRelative;

	public Path() throws Exception {
		setcomponents(new ArrayList<Component>());
	}

	public Path(Component head, Path tail) throws Exception {
		this();

		getcomponents().add(head);
		getcomponents().addAll(tail.getcomponents());
	}

	public Path(Collection<Component> components) throws Exception {
		this();
		this.getcomponents().addAll(components);
	}

	public Path(String componentsString) throws Exception {
		this();
		this.setcomponentsString(componentsString);
	}

	public List<Component> getcomponents() {
		return components;
	}

	public void setcomponents(List<Component> value) {
		components = value;
	}

	public boolean getisRelative() {
		return isRelative;
	}

	public void setisRelative(boolean value) {
		isRelative = value;
	}

	public Component gethead() throws Exception {
		if (getcomponents().size() > 0) {
			return getcomponents().get(0);
		} else {
			return null;
		}
	}

	public Path gettail() throws Exception {
		if (getcomponents().size() >= 2) {
			List<Component> tailComps = getcomponents().subList(1, getcomponents().size() - 1);

			return new Path(tailComps);
		} else {
			return Path.getself();
		}
	}

	public int getlength() throws Exception {
		return getcomponents().size();
	}

	public Component getlastComponent() throws Exception {
		if (getcomponents().size() > 0) {
			return getcomponents().get(getcomponents().size() - 1);
		} else {
			return null;
		}
	}

	public boolean getcontainsNamedComponent() throws Exception {
		for (Component comp : getcomponents()) {
			if (!comp.getisIndex()) {
				return true;
			}

		}
		return false;
	}

	public static Path getself() throws Exception {
		Path path = new Path();
		path.setisRelative(true);
		return path;
	}

	public Path pathByAppendingPath(Path pathToAppend) throws Exception {
		Path p = new Path();
		int upwardMoves = 0;
		for (int i = 0; i < pathToAppend.getcomponents().size(); ++i) {
			if (pathToAppend.getcomponents().get(i).getisParent()) {
				upwardMoves++;
			} else {
				break;
			}
		}
		for (int i = 0; i < this.getcomponents().size() - upwardMoves; ++i) {
			p.getcomponents().add(this.getcomponents().get(i));
		}
		for (int i = upwardMoves; i < pathToAppend.getcomponents().size(); ++i) {
			p.getcomponents().add(pathToAppend.getcomponents().get(i));
		}
		return p;
	}

	public String getcomponentsString() throws Exception {
		// String compsStr = String.join(".", getcomponents().toArray());

		StringBuilder sb = new StringBuilder();

		sb.append(getcomponents().get(0));

		for (int i = 1; i < getcomponents().size(); i++) {
			sb.append('.');
			sb.append(getcomponents().get(i));
		}

		String compsStr = sb.toString();

		if (getisRelative())
			return "." + compsStr;
		else
			return compsStr;
	}

	public void setcomponentsString(String value) throws Exception {
		getcomponents().clear();
		String componentsStr = value;
		// When components start with ".", it indicates a relative path, e.g.
		// .^.^.hello.5
		// is equivalent to file system style path:
		// ../../hello/5
		if (componentsStr.charAt(0) == '.') {
			setisRelative(true);
			componentsStr = componentsStr.substring(1);
		}

		String[] componentStrings = componentsStr.split(".");

		for (String str : componentStrings) {
			int index = 0;

			try {
				index = Integer.parseInt(str);
				getcomponents().add(new Component(index));
			} catch (NumberFormatException e) {
				getcomponents().add(new Component(str));
			}
		}
	}

	public String toString() {
		try {
			return getcomponentsString();
		} catch (RuntimeException __dummyCatchVar4) {
			throw __dummyCatchVar4;
		} catch (Exception __dummyCatchVar4) {
			throw new RuntimeException(__dummyCatchVar4);
		}

	}

	public boolean equals(Object obj) {
		try {
			return equals(obj instanceof Path ? (Path) obj : (Path) null);
		} catch (RuntimeException __dummyCatchVar5) {
			throw __dummyCatchVar5;
		} catch (Exception __dummyCatchVar5) {
			throw new RuntimeException(__dummyCatchVar5);
		}

	}

	public boolean equals(Path otherPath) {
		try {
			if (otherPath == null)
				return false;

			if (otherPath.getcomponents().size() != this.getcomponents().size())
				return false;

			if (otherPath.getisRelative() != this.getisRelative())
				return false;
			
			//return otherPath.getcomponents().SequenceEqual(this.getcomponents());
			for(int i = 0; i < otherPath.getcomponents().size(); i++) {
				if(!otherPath.getcomponents().get(i).equals(getcomponents().get(i)))
					 return false;
			}
			
		} catch (RuntimeException __dummyCatchVar6) {
			throw __dummyCatchVar6;
		} catch (Exception __dummyCatchVar6) {
			throw new RuntimeException(__dummyCatchVar6);
		}
		
		return true;

	}

	public int hashCode() {
		try {
			return this.toString().hashCode();
		} catch (RuntimeException __dummyCatchVar7) {
			throw __dummyCatchVar7;
		} catch (Exception __dummyCatchVar7) {
			throw new RuntimeException(__dummyCatchVar7);
		}

	}

	// Immutable Component
	public static class Component { // extends
									// IEquatable<Ink.Runtime.Path.Component> {
		private int index;
		private String name;

		public Component(int index) throws Exception {
			// Debug.Assert(index >= 0);
			this.setindex(index);
			this.setname(null);
		}

		public Component(String name) throws Exception {
			// Debug.Assert(name != null && name.Length > 0);
			this.setname(name);
			this.setindex(-1);
		}

		public int getindex() {
			return index;
		}

		public void setindex(int value) {
			index = value;
		}

		public String getname() {
			return name;
		}

		public void setname(String value) {
			name = value;
		}

		public boolean getisIndex() throws Exception {
			return getindex() >= 0;
		}

		public boolean getisParent() throws Exception {
			return Path.parentId.equals(getname());
		}

		public static Component toParent() throws Exception {
			return new Component(parentId);
		}

		public String toString() {
			try {
				if (getisIndex()) {
					return Integer.toString(getindex());
				} else {
					return getname();
				}
			} catch (RuntimeException __dummyCatchVar0) {
				throw __dummyCatchVar0;
			} catch (Exception __dummyCatchVar0) {
				throw new RuntimeException(__dummyCatchVar0);
			}

		}

		public boolean equals(Object obj) {
			try {

				return equals(obj instanceof com.bladecoder.ink.runtime.Path.Component ? (com.bladecoder.ink.runtime.Path.Component) obj
						: (com.bladecoder.ink.runtime.Path.Component) null);
			} catch (RuntimeException __dummyCatchVar1) {
				throw __dummyCatchVar1;
			} catch (Exception __dummyCatchVar1) {
				throw new RuntimeException(__dummyCatchVar1);
			}

		}

		public boolean equals(Component otherComp) {
			try {
				if (otherComp != null && otherComp.getisIndex() == this.getisIndex()) {
					if (getisIndex()) {
						return getindex() == otherComp.getindex();
					} else {
						return getname().equals(otherComp.getname());
					}
				}

				return false;
			} catch (RuntimeException __dummyCatchVar2) {
				throw __dummyCatchVar2;
			} catch (Exception __dummyCatchVar2) {
				throw new RuntimeException(__dummyCatchVar2);
			}

		}

		public int hashCode() {
			try {
				if (getisIndex())
					return this.getindex();
				else
					return this.getname().hashCode();
			} catch (RuntimeException __dummyCatchVar3) {
				throw __dummyCatchVar3;
			} catch (Exception __dummyCatchVar3) {
				throw new RuntimeException(__dummyCatchVar3);
			}

		}

	}

}