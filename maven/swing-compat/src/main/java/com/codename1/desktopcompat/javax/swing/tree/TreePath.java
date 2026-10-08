/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.desktopcompat.javax.swing.tree;

/// The nodes from the root of a tree down to one node. Two paths are
/// equal when their nodes are, one by one, so a path identifies a node
/// whatever object was used to name it.
public class TreePath {

    private TreePath parentPath;
    private Object lastPathComponent;

    public TreePath(Object[] path) {
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("path in TreePath must be non null and not empty.");
        }
        lastPathComponent = path[path.length - 1];
        if (lastPathComponent == null) {
            throw new IllegalArgumentException("Last path component must be non-null");
        }
        if (path.length > 1) {
            parentPath = new TreePath(path, path.length - 1);
        }
    }

    public TreePath(Object lastPathComponent) {
        if (lastPathComponent == null) {
            throw new IllegalArgumentException("path in TreePath must be non null.");
        }
        this.lastPathComponent = lastPathComponent;
        parentPath = null;
    }

    protected TreePath(TreePath parent, Object lastPathComponent) {
        if (lastPathComponent == null) {
            throw new IllegalArgumentException("path in TreePath must be non null.");
        }
        parentPath = parent;
        this.lastPathComponent = lastPathComponent;
    }

    protected TreePath(Object[] path, int length) {
        lastPathComponent = path[length - 1];
        if (lastPathComponent == null) {
            throw new IllegalArgumentException("Path elements must be non-null");
        }
        if (length > 1) {
            parentPath = new TreePath(path, length - 1);
        }
    }

    protected TreePath() {
    }

    public Object[] getPath() {
        int i = getPathCount();
        Object[] result = new Object[i--];
        for (TreePath path = this; path != null; path = path.getParentPath()) {
            result[i--] = path.getLastPathComponent();
        }
        return result;
    }

    public Object getLastPathComponent() {
        return lastPathComponent;
    }

    public int getPathCount() {
        int result = 0;
        for (TreePath path = this; path != null; path = path.getParentPath()) {
            result++;
        }
        return result;
    }

    public Object getPathComponent(int index) {
        int count = getPathCount();
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("Index " + index + " is out of the specified range");
        }
        TreePath path = this;
        for (int i = count - 1; i != index; i--) {
            path = path.getParentPath();
        }
        return path.getLastPathComponent();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TreePath)) {
            return false;
        }
        TreePath a = this;
        TreePath b = (TreePath) o;
        if (a.getPathCount() != b.getPathCount()) {
            return false;
        }
        while (a != null && b != null) {
            if (a == b) {
                return true;
            }
            Object x = a.getLastPathComponent();
            Object y = b.getLastPathComponent();
            if (x == null ? y != null : !x.equals(y)) {
                return false;
            }
            a = a.getParentPath();
            b = b.getParentPath();
        }
        return a == null && b == null;
    }

    @Override
    public int hashCode() {
        return lastPathComponent == null ? 0 : lastPathComponent.hashCode();
    }

    /// Whether `aTreePath` is this path or one that continues it.
    public boolean isDescendant(TreePath aTreePath) {
        if (aTreePath == this) {
            return true;
        }
        if (aTreePath == null) {
            return false;
        }
        int count = getPathCount();
        int other = aTreePath.getPathCount();
        if (other < count) {
            return false;
        }
        TreePath p = aTreePath;
        while (other-- > count) {
            p = p.getParentPath();
        }
        return equals(p);
    }

    public TreePath pathByAddingChild(Object child) {
        if (child == null) {
            throw new NullPointerException("Null child not allowed");
        }
        return new TreePath(this, child);
    }

    public TreePath getParentPath() {
        return parentPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Object[] all = getPath();
        for (int i = 0; i < all.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(all[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
