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
package com.codename1.fxcompat.runtime;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

/// The pluggable style sheet engine. The scene graph calls the installed
/// instance at every point where the styles of a node may have changed; the
/// engine answers by calling [StyleTarget#cn1ApplyStyle(String, Object)] on
/// the nodes concerned. The default engine does nothing.
///
/// #### When the scene graph calls
///
/// - [#restyle(Node)] for a node that entered a scene. The whole subtree
///   that entered is reported, one call per node, a parent before its
///   children.
/// - [#restyle(Node)] for one node whose id, style class list, inline
///   `style` or set of pseudo-class states changed. Only that node is
///   reported: an engine with descendant or child selectors that depend on
///   it walks the subtree itself, for which [#restyleTree(Node)] is the
///   helper.
/// - [#stylesheetsChanged(Scene)] and [#stylesheetsChanged(Parent)] when
///   the `getStylesheets()` list of a scene or a parent changed.
/// - [#restyle(Node)] is also what `Node.applyCss()` calls.
///
/// A node that is not in a scene is never reported. Every call arrives on
/// the JavaFX application thread, and never from inside another call for
/// the same node: a property the engine sets does not trigger a restyle.
///
/// #### What the engine reads
///
/// `Node.getTypeSelector()`, `getId()`, `getStyleClass()`, `getStyle()`,
/// `getPseudoClassStates()`, `getStyleableParent()`, and the style sheets
/// of `Node.getScene()` and of each ancestor `Parent`.
public abstract class StyleEngine {

    private static final StyleEngine NONE = new StyleEngine() {
        @Override
        public void restyle(Node node) {
        }

        @Override
        public void stylesheetsChanged(Scene scene) {
        }

        @Override
        public void stylesheetsChanged(Parent parent) {
        }
    };

    private static StyleEngine instance = NONE;

    /// Returns the installed engine; never `null`.
    public static StyleEngine getInstance() {
        return instance;
    }

    /// Installs an engine; `null` returns to the one that does nothing.
    public static void setInstance(StyleEngine engine) {
        instance = engine == null ? NONE : engine;
    }

    /// Computes and applies the styles of one node.
    public abstract void restyle(Node node);

    /// Called when the style sheet list of a scene changed. Restyles every
    /// node of the scene unless overridden.
    public void stylesheetsChanged(Scene scene) {
        if (scene != null && scene.getRoot() != null) {
            restyleTree(scene.getRoot());
        }
    }

    /// Called when the style sheet list of a parent changed. Restyles the
    /// parent's subtree unless overridden.
    public void stylesheetsChanged(Parent parent) {
        if (parent != null && parent.getScene() != null) {
            restyleTree(parent);
        }
    }

    /// Calls [#restyle(Node)] for a node and everything below it, a parent
    /// before its children.
    public final void restyleTree(Node node) {
        restyle(node);
        if (node instanceof Parent) {
            ObservableList<Node> children = ((Parent) node).getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                restyleTree(children.get(i));
            }
        }
    }
}
