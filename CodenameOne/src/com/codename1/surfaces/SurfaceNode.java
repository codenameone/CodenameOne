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
package com.codename1.surfaces;

import java.util.LinkedHashMap;
import java.util.Map;

/// The base class of all surface layout nodes. A surface descriptor is a small tree of nodes
/// (columns, rows, boxes, text, images, progress indicators) that every platform can render
/// natively while the app process is not running -- so a node is pure data: there are no
/// listeners, no theme lookups and no live `Image` references in the serialized form.
///
/// All nodes share the styling in this class: padding, background color, corner radius, alignment
/// within the parent, flexible weight, an optional fixed size and an optional action. An action is
/// a plain string id (plus optional parameters) delivered to the handler registered with
/// `Surfaces.setActionHandler(...)` when the user taps the node -- see `SurfaceActionEvent`.
///
/// All dimensions are in display-independent pixels (dips).
public abstract class SurfaceNode {
    static final int MAX_DEPTH = 8;

    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int paddingLeft;
    private SurfaceColor background;
    private int cornerRadius;
    private SurfaceAlignment alignment;
    private int weight;
    private int widthDips;
    private int heightDips;
    private String actionId;
    private Map<String, Object> actionParams;

    /// Sets the same padding on all four sides.
    ///
    /// #### Parameters
    ///
    /// - `all`: padding in dips
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setPadding(int all) {
        return setPadding(all, all, all, all);
    }

    /// Sets the padding of each side individually.
    ///
    /// #### Parameters
    ///
    /// - `top`: top padding in dips
    /// - `right`: right padding in dips
    /// - `bottom`: bottom padding in dips
    /// - `left`: left padding in dips
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        return this;
    }

    /// Sets the background color of this node.
    ///
    /// #### Parameters
    ///
    /// - `color`: the background color
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setBackground(SurfaceColor color) {
        this.background = color;
        return this;
    }

    /// Sets the corner radius applied to the node's background. May render square on Android
    /// versions below 12.
    ///
    /// #### Parameters
    ///
    /// - `radius`: the corner radius in dips
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }

    /// Sets this node's alignment within its parent. In a `SurfaceBox` all nine positions apply;
    /// in rows and columns only the cross-axis component is used.
    ///
    /// #### Parameters
    ///
    /// - `alignment`: the alignment
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setAlignment(SurfaceAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /// Sets the flexible-space weight of this node within a row or column. Nodes with a weight
    /// share the leftover space of the parent proportionally; a weight of 0 (the default) sizes
    /// the node to its natural size.
    ///
    /// #### Parameters
    ///
    /// - `weight`: the relative weight, 0 for natural sizing
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setWeight(int weight) {
        this.weight = weight;
        return this;
    }

    /// Sets a fixed size for this node. A value of 0 (the default) keeps the natural size of the
    /// respective axis.
    ///
    /// #### Parameters
    ///
    /// - `widthDips`: fixed width in dips, 0 for natural width
    /// - `heightDips`: fixed height in dips, 0 for natural height
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setSize(int widthDips, int heightDips) {
        this.widthDips = widthDips;
        this.heightDips = heightDips;
        return this;
    }

    /// Assigns a tap action to this node. Tapping the node opens (or foregrounds) the app and
    /// delivers the action id to the handler registered with `Surfaces.setActionHandler(...)`.
    /// Note that small iOS home-screen widgets only honor the action of the root node.
    ///
    /// #### Parameters
    ///
    /// - `actionId`: the app-defined action identifier
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setAction(String actionId) {
        return setAction(actionId, null);
    }

    /// Assigns a tap action with parameters to this node. The parameter map may contain `String`,
    /// `Number` and `Boolean` values and is delivered verbatim with the `SurfaceActionEvent`.
    ///
    /// #### Parameters
    ///
    /// - `actionId`: the app-defined action identifier
    /// - `params`: parameters delivered with the action, may be null
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceNode setAction(String actionId, Map<String, Object> params) {
        this.actionId = actionId;
        this.actionParams = params;
        return this;
    }

    /// Returns the top padding in dips.
    public int getPaddingTop() {
        return paddingTop;
    }

    /// Returns the right padding in dips.
    public int getPaddingRight() {
        return paddingRight;
    }

    /// Returns the bottom padding in dips.
    public int getPaddingBottom() {
        return paddingBottom;
    }

    /// Returns the left padding in dips.
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /// Returns the background color, or null.
    public SurfaceColor getBackground() {
        return background;
    }

    /// Returns the corner radius in dips.
    public int getCornerRadius() {
        return cornerRadius;
    }

    /// Returns the alignment within the parent, or null for the platform default.
    public SurfaceAlignment getAlignment() {
        return alignment;
    }

    /// Returns the flexible-space weight, 0 when naturally sized.
    public int getWeight() {
        return weight;
    }

    /// Returns the fixed width in dips, 0 when naturally sized.
    public int getWidthDips() {
        return widthDips;
    }

    /// Returns the fixed height in dips, 0 when naturally sized.
    public int getHeightDips() {
        return heightDips;
    }

    /// Returns the tap action id, or null.
    public String getActionId() {
        return actionId;
    }

    /// Returns the tap action parameters, or null.
    public Map<String, Object> getActionParams() {
        return actionParams;
    }

    /// Returns the wire-format type tag of this node (`col`, `row`, `box`, `text`, `dyn`, `img`,
    /// `prog`, `spacer`).
    abstract String getType();

    /// Adds the node-specific keys to the serialized form. Image nodes register their PNG bytes
    /// in `images`; containers serialize their children at `depth + 1`.
    abstract void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth);

    /// Serializes this node (and its subtree) to the map structure emitted as JSON.
    final Map<String, Object> toMap(Map<String, byte[]> images, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Surface descriptors are limited to " + MAX_DEPTH
                    + " nesting levels so they stay within platform widget budgets");
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("t", getType());
        if (paddingTop != 0 || paddingRight != 0 || paddingBottom != 0 || paddingLeft != 0) {
            java.util.List<Object> pad = new java.util.ArrayList<Object>(4);
            pad.add(Integer.valueOf(paddingTop));
            pad.add(Integer.valueOf(paddingRight));
            pad.add(Integer.valueOf(paddingBottom));
            pad.add(Integer.valueOf(paddingLeft));
            out.put("pad", pad);
        }
        if (background != null) {
            out.put("bg", SurfaceSerializer.colorMap(background));
        }
        if (cornerRadius != 0) {
            out.put("corner", Integer.valueOf(cornerRadius));
        }
        if (alignment != null) {
            out.put("align", alignment.getJsonName());
        }
        if (weight != 0) {
            out.put("weight", Integer.valueOf(weight));
        }
        if (widthDips != 0) {
            out.put("w", Integer.valueOf(widthDips));
        }
        if (heightDips != 0) {
            out.put("h", Integer.valueOf(heightDips));
        }
        if (actionId != null) {
            Map<String, Object> action = new LinkedHashMap<String, Object>();
            action.put("id", actionId);
            if (actionParams != null && !actionParams.isEmpty()) {
                action.put("p", SurfaceSerializer.sortedCopy(actionParams));
            }
            out.put("action", action);
        }
        serializeContent(out, images, depth);
        return out;
    }
}
