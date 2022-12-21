package com.bladecoder.ink.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Path {
    private static final String PARENT_ID = "^";

    private List<Component> components;
    private boolean isRelative = false;
    private String componentsString;

    public Path() {
        components = new ArrayList<Component>();
    }

    public Path(Component head, Path tail) {
        this();

        components.add(head);
        components.addAll(tail.components);
    }

    public Path(Collection<Component> components) {
        this(components, false);
    }

    public Path(Collection<Component> components, boolean relative) {
        this();
        this.components.addAll(components);

        this.isRelative = relative;
    }

    public Path(String componentsString) {
        this();
        setComponentsString(componentsString);
    }

    public Component getComponent(int index) {
        return components.get(index);
    }

    public boolean isRelative() {
        return isRelative;
    }

    private void setRelative(boolean value) {
        isRelative = value;
    }

    public Component getHead() {
        if (components.size() > 0) {
            return components.get(0);
        } else {
            return null;
        }
    }

    public Path getTail() {
        if (components.size() >= 2) {
            List<Component> tailComps = components.subList(1, components.size());

            return new Path(tailComps);
        } else {
            return Path.getSelf();
        }
    }

    public int getLength() {
        return components.size();
    }

    public Component getLastComponent() {
        int lastComponentIdx = components.size() - 1;
        if (lastComponentIdx >= 0) return components.get(lastComponentIdx);
        else return null;
    }

    public boolean containsNamedComponent() {
        for (Component comp : components) {
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
        for (int i = 0; i < pathToAppend.components.size(); ++i) {
            if (pathToAppend.components.get(i).isParent()) {
                upwardMoves++;
            } else {
                break;
            }
        }
        for (int i = 0; i < this.components.size() - upwardMoves; ++i) {
            p.components.add(this.components.get(i));
        }
        for (int i = upwardMoves; i < pathToAppend.components.size(); ++i) {
            p.components.add(pathToAppend.components.get(i));
        }
        return p;
    }

    public String getComponentsString() {
        if (componentsString == null) {
            StringBuilder sb = new StringBuilder();

            if (components.size() > 0) {

                sb.append(components.get(0));

                for (int i = 1; i < components.size(); i++) {
                    sb.append('.');
                    sb.append(components.get(i));
                }
            }

            componentsString = sb.toString();

            if (isRelative) componentsString = "." + componentsString;
        }

        return componentsString;
    }

    private void setComponentsString(String value) {
        components.clear();
        componentsString = value;

        // Empty path, empty components
        // (path is to root, like "/" in file system)
        if (componentsString == null || componentsString.isEmpty()) return;

        // When components start with ".", it indicates a relative path, e.g.
        // .^.^.hello.5
        // is equivalent to file system style path:
        // ../../hello/5
        if (componentsString.charAt(0) == '.') {
            setRelative(true);
            componentsString = componentsString.substring(1);
        } else {
            setRelative(false);
        }

        String[] componentStrings = componentsString.split("\\.");

        for (String str : componentStrings) {
            int index = 0;

            try {
                index = Integer.parseInt(str);
                components.add(new Component(index));
            } catch (NumberFormatException e) {
                components.add(new Component(str));
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
        if (otherPath == null) return false;

        if (otherPath.components.size() != this.components.size()) return false;

        if (otherPath.isRelative() != this.isRelative()) return false;

        // return
        // otherPath.components.SequenceEqual(this.components);
        for (int i = 0; i < otherPath.components.size(); i++) {
            if (!otherPath.components.get(i).equals(components.get(i))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public Path pathByAppendingComponent(Component c) {
        Path p = new Path();
        p.components.addAll(components);
        p.components.add(c);
        return p;
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
            if (isIndex()) return getIndex();
            else return getName().hashCode();
        }
    }
}
