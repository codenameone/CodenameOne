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

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionListener;

/// The selected paths of a tree.
public interface TreeSelectionModel {

    int SINGLE_TREE_SELECTION = 1;
    int CONTIGUOUS_TREE_SELECTION = 2;
    int DISCONTIGUOUS_TREE_SELECTION = 4;

    void setSelectionMode(int mode);

    int getSelectionMode();

    void setSelectionPath(TreePath path);

    void setSelectionPaths(TreePath[] paths);

    void addSelectionPath(TreePath path);

    void addSelectionPaths(TreePath[] paths);

    void removeSelectionPath(TreePath path);

    void removeSelectionPaths(TreePath[] paths);

    TreePath getSelectionPath();

    TreePath[] getSelectionPaths();

    int getSelectionCount();

    boolean isPathSelected(TreePath path);

    boolean isSelectionEmpty();

    void clearSelection();

    void setRowMapper(RowMapper newMapper);

    RowMapper getRowMapper();

    int[] getSelectionRows();

    int getMinSelectionRow();

    int getMaxSelectionRow();

    boolean isRowSelected(int row);

    void resetRowSelection();

    int getLeadSelectionRow();

    TreePath getLeadSelectionPath();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void addTreeSelectionListener(TreeSelectionListener x);

    void removeTreeSelectionListener(TreeSelectionListener x);
}
