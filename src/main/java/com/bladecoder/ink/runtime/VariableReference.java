package com.bladecoder.ink.runtime;

public class VariableReference extends RTObject {
	// Normal named variable
	private String name = new String();

	// Variable reference is actually a path for a visit (read) count
	private Path pathForCount;

	// Require default constructor for serialisation
	public VariableReference() {
	}

	public VariableReference(String name) {
		this.setName(name);
	}

	public Container getContainerForCount() throws Exception {
		return this.resolvePath(getPathForCount()) instanceof Container
				? (Container) this.resolvePath(getPathForCount()) : (Container) null;
	}

	public String getName() {
		return name;
	}

	public Path getPathForCount() {
		return pathForCount;
	}

	public String getPathStringForCount() throws Exception {
		if (getPathForCount() == null)
			return null;

		return compactPathString(getPathForCount());
	}

	public void setName(String value) {
		name = value;
	}

	public void setPathForCount(Path value) {
		pathForCount = value;
	}

	public void setPathStringForCount(String value) throws Exception {
		if (value == null)
			setPathForCount(null);
		else
			setPathForCount(new Path(value));
	}

	@Override
	public String toString() {
		try {
			if (getName() != null) {
				return String.format("var({0})", getName());
			} else {
				String pathStr = getPathStringForCount();
				return String.format("read_count({0})", pathStr);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
