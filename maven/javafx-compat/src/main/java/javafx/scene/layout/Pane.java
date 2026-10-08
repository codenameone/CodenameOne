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
package javafx.scene.layout;

import javafx.collections.ObservableList;
import javafx.scene.Node;

/// A region whose children the application positions itself: layout only
/// gives each resizable child its preferred size and leaves it at its
/// `layoutX`, `layoutY`. The base of the layout panes.
public class Pane extends Region {

    /// Creates an empty pane.
    public Pane() {
    }

    /// Creates a pane with children.
    public Pane(Node... children) {
        cn1Children().addAll(children);
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /// Stores a layout constraint on a node, or removes it with `null`.
    static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            if (node.hasProperties()) {
                node.getProperties().remove(key);
            }
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }

    /// Returns a layout constraint of a node, or `null`.
    static Object getConstraint(Node node, Object key) {
        return node.hasProperties() ? node.getProperties().get(key) : null;
    }
}
