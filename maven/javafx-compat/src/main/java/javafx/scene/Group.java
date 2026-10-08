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
package javafx.scene;

import java.util.Collection;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;

/// A parent that adds nothing of its own: its bounds are the union of its
/// children's, and it does not position them. Transforms and opacity set
/// on a group apply to all of its children.
public class Group extends Parent {

    private final BooleanProperty autoSizeChildren = new FxBoolean(this, "autoSizeChildren", true, Dirty.LAYOUT);

    /// Creates an empty group.
    public Group() {
    }

    /// Creates a group with children.
    public Group(Node... children) {
        cn1Children().addAll(children);
    }

    /// Creates a group with children.
    public Group(Collection<Node> children) {
        cn1Children().addAll(children);
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /// Returns whether resizable children get their preferred size at
    /// each layout.
    public final boolean isAutoSizeChildren() {
        return autoSizeChildren.get();
    }

    /// Sets whether resizable children get their preferred size at each
    /// layout.
    public final void setAutoSizeChildren(boolean value) {
        autoSizeChildren.set(value);
    }

    /// Whether resizable children get their preferred size at each layout.
    public final BooleanProperty autoSizeChildrenProperty() {
        return autoSizeChildren;
    }

    @Override
    protected void layoutChildren() {
        if (isAutoSizeChildren()) {
            super.layoutChildren();
        }
    }

    @Override
    public double prefWidth(double height) {
        if (isAutoSizeChildren()) {
            layout();
        }
        double w = getLayoutBounds().getWidth();
        return Double.isNaN(w) || w < 0 ? 0 : w;
    }

    @Override
    public double prefHeight(double width) {
        if (isAutoSizeChildren()) {
            layout();
        }
        double h = getLayoutBounds().getHeight();
        return Double.isNaN(h) || h < 0 ? 0 : h;
    }

    @Override
    public double minWidth(double height) {
        return prefWidth(height);
    }

    @Override
    public double minHeight(double width) {
        return prefHeight(width);
    }
}
