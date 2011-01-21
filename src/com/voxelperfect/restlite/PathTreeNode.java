package com.voxelperfect.restlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathTreeNode<T> {

	public static boolean isNameAPathParam(String name) {

		return (name.startsWith("{") && name.endsWith("}"));
	}

	public static String nameToPathParam(String name) {

		if (isNameAPathParam(name)) {
			name = name.substring(1, name.length() - 1);
		}
		return name;
	}

	public static class DataRef<T> {
		List<PathTreeNode<T>> nodePath;
		T data;

		public DataRef(List<PathTreeNode<T>> nodePath) {
			this.nodePath = nodePath;
			this.data = nodePath.get(nodePath.size() - 1).getData();
		}

		public List<PathTreeNode<T>> getNodePath() {
			return nodePath;
		}

		public T getData() {
			return data;
		}

		public Map<String, String> getPathParams() {

			HashMap<String, String> pathParams = new HashMap<String, String>();

			if (nodePath != null) {
				for (PathTreeNode<T> node : nodePath) {
					if (isNameAPathParam(node.name)) {
						pathParams.put(nameToPathParam(node.name), node.value);
					}
				}
			}

			return pathParams;
		}
	}

	private String name;
	private String value;
	private T data;
	private List<PathTreeNode<T>> children;

	public PathTreeNode(String name) {
		this.name = name;
		children = new ArrayList<PathTreeNode<T>>();
	}

	public PathTreeNode(String name, T data) {
		this(name);
		this.data = data;
	}

	public PathTreeNode(PathTreeNode<T> source) {
		this.name = source.name;
		this.data = source.data;
		this.children = source.children;
	}

	public PathTreeNode<T> createPathParamNode(String value) {

		PathTreeNode<T> param = new PathTreeNode<T>(this);
		param.value = value;
		return param;
	}

	public List<PathTreeNode<T>> getChildren() {
		return this.children;
	}

	public int getNumberOfChildren() {
		return getChildren().size();
	}

	public boolean hasChildren() {
		return (getNumberOfChildren() > 0);
	}

	public void setChildren(List<PathTreeNode<T>> children) {
		this.children = children;
	}

	public void addChild(PathTreeNode<T> child) {
		children.add(child);
	}

	public void addChildAt(int index, PathTreeNode<T> child)
			throws IndexOutOfBoundsException {
		children.add(index, child);
	}

	public void removeChildren() {
		this.children = new ArrayList<PathTreeNode<T>>();
	}

	public void removeChildAt(int index) throws IndexOutOfBoundsException {
		children.remove(index);
	}

	public PathTreeNode<T> getChildAt(int index)
			throws IndexOutOfBoundsException {
		return children.get(index);
	}

	public T getData() {
		return this.data;
	}

	public T getData(String path) {
		List<PathTreeNode<T>> nodePath = getPathNodes(path, false);
		if (nodePath != null) {
			return nodePath.get(nodePath.size() - 1).getData();
		} else {
			throw new IllegalArgumentException(path);
		}
	}

	public DataRef<T> getDataRef(String path) {
		List<PathTreeNode<T>> nodePath = getPathNodes(path, false);
		if (nodePath != null) {
			return new DataRef<T>(nodePath);
		} else {
			throw new IllegalArgumentException(path);
		}
	}

	public void setData(T data) {
		this.data = data;
	}

	public void setData(String path, T data) {

		List<PathTreeNode<T>> nodes = getPathNodes(path, true);
		if (nodes != null) {
			nodes.get(nodes.size() - 1).setData(data);
		} else {
			throw new IllegalArgumentException(path);
		}
	}

	public List<PathTreeNode<T>> getPathNodes(String path) {

		return getPathNodes(path, false);
	}

	public List<PathTreeNode<T>> getPathNodes(String path, boolean create) {

		ArrayList<PathTreeNode<T>> nodes = new ArrayList<PathTreeNode<T>>();

		String[] names = path.split("/");
		if (!name.equals(names[0])) {
			return null;
		}

		PathTreeNode<T> node = this;
		PathTreeNode<T> foundChild;
		String pathName;
		PathTreeNode<T> newChild;

		int count = names.length;
		for (int n = 1; n < count; n++) {
			pathName = names[n];
			foundChild = null;
			for (PathTreeNode<T> child : node.children) {
				if (isNameAPathParam(child.name)) {
					child = child.createPathParamNode(pathName);
				} else if (!child.name.equals(pathName)) {
					child = null;
				}

				if (child != null) {
					nodes.add(child);
					foundChild = child;

					node = child;
					break;
				}
			}

			if (foundChild == null) {
				if (create) {
					newChild = new PathTreeNode<T>(pathName);
					node.addChild(newChild);
					nodes.add(newChild);

					node = newChild;
				} else {
					return null;
				}
			}
		}

		return nodes;
	}

	public String toStringVerbose() {
		String stringRepresentation = getData().toString() + ":[";

		for (PathTreeNode<T> node : getChildren()) {
			stringRepresentation += node.getData().toString() + ", ";
		}

		// Pattern.DOTALL causes ^ and $ to match. Otherwise it won't. It's
		// retarded.
		Pattern pattern = Pattern.compile(", $", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(stringRepresentation);

		stringRepresentation = matcher.replaceFirst("");
		stringRepresentation += "]";

		return stringRepresentation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
