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
package com.codename1.desktopcompat.javax.swing.event;

import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import java.util.EventObject;

/// What changed in a tree model: the path to a parent node and, unless
/// everything below it changed, the children concerned with the indexes
/// they have (or had, for a removal) in that parent.
public class TreeModelEvent extends EventObject {

    protected TreePath path;
    protected int[] childIndices;
    protected Object[] children;

    public TreeModelEvent(Object source, Object[] path, int[] childIndices, Object[] children) {
        this(source, path == null ? null : new TreePath(path), childIndices, children);
    }

    public TreeModelEvent(Object source, TreePath path, int[] childIndices, Object[] children) {
        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    public TreeModelEvent(Object source, Object[] path) {
        this(source, path == null ? null : new TreePath(path));
    }

    public TreeModelEvent(Object source, TreePath path) {
        super(source);
        this.path = path;
        this.childIndices = new int[0];
    }

    public TreePath getTreePath() {
        return path;
    }

    public Object[] getPath() {
        return path == null ? null : path.getPath();
    }

    public Object[] getChildren() {
        if (children == null) {
            return null;
        }
        Object[] copy = new Object[children.length];
        System.arraycopy(children, 0, copy, 0, children.length);
        return copy;
    }

    public int[] getChildIndices() {
        if (childIndices == null) {
            return null;
        }
        int[] copy = new int[childIndices.length];
        System.arraycopy(childIndices, 0, copy, 0, childIndices.length);
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(' ').append(hashCode());
        if (path != null) {
            sb.append(" path ").append(path);
        }
        if (childIndices != null) {
            sb.append(" indices [ ");
            for (int i = 0; i < childIndices.length; i++) {
                sb.append(childIndices[i]).append(' ');
            }
            sb.append(']');
        }
        if (children != null) {
            sb.append(" children [ ");
            for (int i = 0; i < children.length; i++) {
                sb.append(children[i]).append(' ');
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
