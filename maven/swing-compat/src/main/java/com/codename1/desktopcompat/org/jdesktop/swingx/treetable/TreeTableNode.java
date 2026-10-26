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
package com.codename1.desktopcompat.org.jdesktop.swingx.treetable;

import com.codename1.desktopcompat.javax.swing.tree.TreeNode;
import java.util.Enumeration;

/// A tree node with a value for each of a number of columns and a user
/// object.
public interface TreeTableNode extends TreeNode {

    @Override
    Enumeration<? extends TreeTableNode> children();

    /// The node's value in a column.
    Object getValueAt(int column);

    @Override
    TreeTableNode getChildAt(int childIndex);

    /// How many columns this node has a value for, which may be fewer
    /// than its model has.
    int getColumnCount();

    @Override
    TreeTableNode getParent();

    boolean isEditable(int column);

    void setValueAt(Object aValue, int column);

    Object getUserObject();

    void setUserObject(Object userObject);
}
