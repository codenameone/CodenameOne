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
package com.codename1.art;

import java.util.ArrayList;
import java.util.List;

/// An artwork: a canvas size and a tree of nodes.
///
/// Read one with `ArtFormats.read`, or build one in code. Render it with `ArtRenderer`,
/// show it with `ArtView`, edit it with `ArtModel` and `ArtEditor`.
public final class ArtDocument {
    private double width;
    private double height;
    private GroupNode root = new GroupNode();
    private String title;
    private final List<ImportNote> notes = new ArrayList<ImportNote>();

    /// Creates an empty document.
    ///
    /// #### Parameters
    ///
    /// - `width`: canvas width
    /// - `height`: canvas height
    public ArtDocument(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /// Canvas width.
    public double getWidth() {
        return width;
    }

    /// Canvas height.
    public double getHeight() {
        return height;
    }

    /// Resizes the canvas.
    ///
    /// #### Parameters
    ///
    /// - `width`: new width
    /// - `height`: new height
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /// The root group.
    public GroupNode getRoot() {
        return root;
    }

    /// Replaces the root.
    ///
    /// #### Parameters
    ///
    /// - `root`: the new root
    public void setRoot(GroupNode root) {
        this.root = root == null ? new GroupNode() : root;
    }

    /// The title, or null.
    public String getTitle() {
        return title;
    }

    /// Sets the title.
    ///
    /// #### Parameters
    ///
    /// - `title`: the title
    public void setTitle(String title) {
        this.title = title;
    }

    /// What the reader that produced this document could not represent exactly.
    public List<ImportNote> getNotes() {
        return notes;
    }

    /// A deep copy.
    public ArtDocument copy() {
        ArtDocument d = new ArtDocument(width, height);
        d.root = (GroupNode) root.copy();
        d.title = title;
        d.notes.addAll(notes);
        return d;
    }

    /// Finds a node by id anywhere in the tree.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    public ArtNode findById(String id) {
        return find(root, id);
    }

    private static ArtNode find(ArtNode n, String id) {
        if (id.equals(n.getId())) {
            return n;
        }
        // Mask content is part of the document too, and editable through ArtModel.
        if (n.getMask() != null) {
            ArtNode m = find(n.getMask(), id);
            if (m != null) {
                return m;
            }
        }
        if (n instanceof GroupNode) {
            List<ArtNode> c = ((GroupNode) n).getChildren();
            for (int i = 0; i < c.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                ArtNode r = find(c.get(i), id);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /// The group that holds `node`, or null when it is the root or not in this document.
    ///
    /// #### Parameters
    ///
    /// - `node`: the node
    public GroupNode parentOf(ArtNode node) {
        return parent(root, node);
    }

    private static GroupNode parent(GroupNode g, ArtNode node) {
        List<ArtNode> c = g.getChildren();
        for (ArtNode n : c) {
            if (n == node) { // NOPMD CompareObjectsWithEquals - node identity
                return g;
            }
            if (n instanceof GroupNode) {
                GroupNode r = parent((GroupNode) n, node);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /// The transform from `node`'s space to document space, or null when it is not here.
    ///
    /// #### Parameters
    ///
    /// - `node`: the node
    public Matrix toDocument(ArtNode node) {
        if (node == root) { // NOPMD CompareObjectsWithEquals - node identity
            return root.getTransform();
        }
        GroupNode p = parentOf(node);
        if (p == null) {
            return null;
        }
        Matrix pm = toDocument(p);
        return pm == null ? null : pm.multiply(node.getTransform());
    }
}
