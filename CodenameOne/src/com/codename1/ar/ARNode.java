/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

import java.util.ArrayList;

/// A node in the small scene graph rendered at an `ARAnchor`. A node carries
/// an optional `ARModel` plus a local transform relative to its parent (or to
/// the anchor for the root node), and may hold child nodes.
///
/// Attach a root node to an anchor with `ARAnchor#setNode(ARNode)`. After
/// attachment, transform and visibility changes are forwarded to the platform
/// renderer automatically. Mutate nodes on the EDT.
public class ARNode {
    private final ARModel model;
    private float x;
    private float y;
    private float z;
    private float qx;
    private float qy;
    private float qz;
    private float qw = 1;
    private float scale = 1;
    private boolean visible = true;
    private final ArrayList<ARNode> children = new ArrayList<ARNode>();
    private ARNode parent;
    ARSession session;
    String anchorId;

    /// Creates an empty grouping node with no geometry of its own.
    public ARNode() {
        this.model = null;
    }

    /// Creates a node rendering the supplied model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the content to render at this node
    public ARNode(ARModel model) {
        this.model = model;
    }

    /// The model rendered at this node, or null for a grouping node.
    public ARModel getModel() {
        return model;
    }

    /// Sets the node position relative to its parent (or the anchor for a
    /// root node), in meters.
    public ARNode setLocalPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        changed();
        return this;
    }

    /// Sets the node rotation relative to its parent as a quaternion.
    public ARNode setLocalRotation(float qx, float qy, float qz, float qw) {
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
        changed();
        return this;
    }

    /// Sets a uniform scale factor relative to the parent. Default `1`.
    public ARNode setLocalScale(float scale) {
        this.scale = scale;
        changed();
        return this;
    }

    /// Shows or hides this node and its children. Default visible.
    public ARNode setVisible(boolean visible) {
        this.visible = visible;
        changed();
        return this;
    }

    /// The local X position in meters.
    public float getLocalX() {
        return x;
    }

    /// The local Y position in meters.
    public float getLocalY() {
        return y;
    }

    /// The local Z position in meters.
    public float getLocalZ() {
        return z;
    }

    /// The X component of the local rotation quaternion.
    public float getLocalQx() {
        return qx;
    }

    /// The Y component of the local rotation quaternion.
    public float getLocalQy() {
        return qy;
    }

    /// The Z component of the local rotation quaternion.
    public float getLocalQz() {
        return qz;
    }

    /// The W component of the local rotation quaternion.
    public float getLocalQw() {
        return qw;
    }

    /// The uniform local scale factor.
    public float getLocalScale() {
        return scale;
    }

    /// True when this node (and therefore its children) is rendered.
    public boolean isVisible() {
        return visible;
    }

    /// Adds a child node. A node may have at most one parent.
    ///
    /// #### Parameters
    ///
    /// - `child`: the node to add below this one
    public void addChild(ARNode child) {
        if (child == null) {
            throw new IllegalArgumentException("child is required");
        }
        if (child.parent != null) {
            throw new IllegalArgumentException("node already has a parent");
        }
        child.parent = this;
        children.add(child);
        changed();
    }

    /// Removes a child node. No-op when the node is not a child of this one.
    ///
    /// #### Parameters
    ///
    /// - `child`: the node to remove
    public void removeChild(ARNode child) {
        if (children.remove(child)) {
            child.parent = null;
            changed();
        }
    }

    /// The number of child nodes.
    public int getChildCount() {
        return children.size();
    }

    /// The child node at the supplied index.
    public ARNode getChildAt(int index) {
        return children.get(index);
    }

    /// The parent node, or null for a root node.
    public ARNode getParent() {
        return parent;
    }

    private void changed() {
        ARNode root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        if (root.session != null) {
            root.session.nodeChangedInternal(root.anchorId, root);
        }
    }
}
