package Ink.Runtime;

import java.util.ArrayList;
import java.util.Stack;

import Ink.Runtime.Path.Component;

/**
 * Base class for all ink runtime content.
 */
/* TODO: abstract */
public class RTObject {
	/**
	 * Runtime.RTObjects can be included in the main Story as a hierarchy.
	 * Usually parents are Container RTObjects. (TODO: Always?) The parent.
	 */
	private RTObject parent;

	Path path;

	public RTObject() throws Exception {
	}

	// TODO: Come up with some clever solution for not having
	// to have debug metadata on the RTObject itself, perhaps
	// for serialisation purposes at least.
	DebugMetadata debugMetadata;

	public RTObject getparent() {
		return parent;
	}

	public void setparent(RTObject value) {
		parent = value;
	}

	public DebugMetadata getdebugMetadata() throws Exception {
		if (debugMetadata == null) {
			if (getparent() != null) {
				return getparent().getdebugMetadata();
			}

		}

		return debugMetadata;
	}

	public void setdebugMetadata(DebugMetadata value) throws Exception {
		debugMetadata = value;
	}

	public Integer debugLineNumberOfPath(Path path) throws Exception {
		if (path == null)
			return null;

		// Try to get a line number from debug metadata
		Container root = this.getrootContentContainer();

		if (root != null) {
			RTObject targetContent = root.contentAtPath(path);
			if (targetContent != null) {
				DebugMetadata dm = targetContent.debugMetadata;
				if (dm != null) {
					return dm.startLineNumber;
				}

			}

		}

		return null;
	}

	public Path getpath() throws Exception {
		if (path == null) {
			if (getparent() == null) {
				path = new Path();
			} else {
				// Maintain a Stack so that the order of the components
				// is reversed when they're added to the Path.
				// We're iterating up the hierarchy from the leaves/children to
				// the root.
				Stack<Ink.Runtime.Path.Component> comps = new Stack<Ink.Runtime.Path.Component>();
				RTObject child = this;
				Container container = child.getparent() instanceof Container ? (Container) child.getparent()
						: (Container) null;
				while (container != null) {
					INamedContent namedChild = child instanceof INamedContent ? (INamedContent) child
							: (INamedContent) null;
					if (namedChild != null && namedChild.gethasValidName()) {
						comps.push(new Ink.Runtime.Path.Component(namedChild.getname()));
					} else {
						comps.push(new Ink.Runtime.Path.Component(container.getcontent().IndexOf(child)));
					}
					child = container;
					container = container.getParent() instanceof Container ? (Container) container.parent : (Container) null;
				}
				path = new Path(comps);
			}
		}

		return path;
	}

	public RTObject resolvePath(Path path) throws Exception {
		if (path.getisRelative()) {
			Container nearestContainer = this instanceof Container ? (Container) this : (Container) null;

			if (nearestContainer == null) {
				//Debug.Assert(this.getparent() != null, "Can't resolve relative path because we don't have a parent");
				nearestContainer = this.getparent() instanceof Container ? (Container) this.getparent()
						: (Container) null;
				//Debug.Assert(nearestContainer != null, "Expected parent to be a container");
				//Debug.Assert(path.getcomponents()[0].isParent);
				path = path.gettail();
			}

			return nearestContainer.contentAtPath(path);
		} else {
			return this.getrootContentContainer().contentAtPath(path);
		}
	}

	public Path convertPathToRelative(Path globalPath) throws Exception {
		// 1. Find last shared ancestor
		// 2. Drill up using ".." style (actually represented as "^")
		// 3. Re-build downward chain from common ancestor
		Path ownPath = this.getpath();
		int minPathLength = Math.min(globalPath.getcomponents().size(), ownPath.getcomponents().size());
		int lastSharedPathCompIndex = -1;
		for (int i = 0; i < minPathLength; ++i) {
			Component ownComp = ownPath.getcomponents().get(i);
			Component otherComp = globalPath.getcomponents().get(i);
			
			if (ownComp.equals(otherComp)) {
				lastSharedPathCompIndex = i;
			} else {
				break;
			}
		}
		// No shared path components, so just use global path
		if (lastSharedPathCompIndex == -1)
			return globalPath;

		int numUpwardsMoves = (ownPath.getcomponents().Count - 1) - lastSharedPathCompIndex;
		ArrayList<Component> newPathComps = new ArrayList<Ink.Runtime.Path.Component>();
		for (int up = 0; up < numUpwardsMoves; ++up)
			newPathComps.add(Ink.Runtime.Path.Component.toParent());
		for (int down = lastSharedPathCompIndex + 1; down < globalPath.getcomponents().size(); ++down)
			newPathComps.add(globalPath.getcomponents().get(down));
		Path relativePath = new Path(newPathComps);
		relativePath.setisRelative(true);
		return relativePath;
	}

	// Find most compact representation for a path, whether relative or global
	public String compactPathString(Path otherPath) throws Exception {
		String globalPathStr = null;
		String relativePathStr = null;
		if (otherPath.getisRelative()) {
			relativePathStr = otherPath.getcomponentsString();
			globalPathStr = this.getpath().pathByAppendingPath(otherPath).getcomponentsString();
		} else {
			Path relativePath = convertPathToRelative(otherPath);
			relativePathStr = relativePath.getcomponentsString();
			globalPathStr = otherPath.getcomponentsString();
		}
		if (relativePathStr.length() < globalPathStr.length())
			return relativePathStr;
		else
			return globalPathStr;
	}

	public Container getrootContentContainer() throws Exception {
		RTObject ancestor = this;
		while (ancestor.getparent() != null) {
			ancestor = ancestor.getparent();
		}
		return ancestor instanceof Container ? (Container) ancestor : (Container) null;
	}

	public RTObject copy() throws Exception {
		throw new UnsupportedOperationException(GetType().Name + " doesn't support copying");
	}

	public <T extends RTObject> void setChild(RefSupport<T> obj, T value) throws Exception {
		if (obj.getValue())
			obj.getValue().parent = null;

		obj.setValue(value);
		if (obj.getValue())
			obj.getValue().parent = this;

	}
}
