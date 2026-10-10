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

/// The paths that joined or left the selection of a tree, and the lead
/// path before and after.
public class TreeSelectionEvent extends EventObject {

    protected TreePath[] paths;
    protected boolean[] areNew;
    protected TreePath oldLeadSelectionPath;
    protected TreePath newLeadSelectionPath;

    public TreeSelectionEvent(Object source, TreePath[] paths, boolean[] areNew, TreePath oldLeadSelectionPath,
            TreePath newLeadSelectionPath) {
        super(source);
        this.paths = paths;
        this.areNew = areNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    public TreeSelectionEvent(Object source, TreePath path, boolean isNew, TreePath oldLeadSelectionPath,
            TreePath newLeadSelectionPath) {
        super(source);
        this.paths = new TreePath[]{path};
        this.areNew = new boolean[]{isNew};
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    public TreePath[] getPaths() {
        TreePath[] copy = new TreePath[paths.length];
        System.arraycopy(paths, 0, copy, 0, paths.length);
        return copy;
    }

    /// The first path that changed.
    public TreePath getPath() {
        return paths[0];
    }

    /// Whether the first path was added to the selection.
    public boolean isAddedPath() {
        return areNew[0];
    }

    public boolean isAddedPath(TreePath path) {
        for (int i = paths.length - 1; i >= 0; i--) {
            if (paths[i].equals(path)) {
                return areNew[i];
            }
        }
        throw new IllegalArgumentException("path is not a path identified by the TreeSelectionEvent");
    }

    public boolean isAddedPath(int index) {
        if (paths == null || index < 0 || index >= paths.length) {
            throw new IllegalArgumentException("index is beyond range of added paths identified by TreeSelectionEvent");
        }
        return areNew[index];
    }

    public TreePath getOldLeadSelectionPath() {
        return oldLeadSelectionPath;
    }

    public TreePath getNewLeadSelectionPath() {
        return newLeadSelectionPath;
    }

    /// The same event with another source.
    public Object cloneWithSource(Object newSource) {
        return new TreeSelectionEvent(newSource, paths, areNew, oldLeadSelectionPath, newLeadSelectionPath);
    }
}
