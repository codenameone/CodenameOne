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
package com.codename1.graph;

/// A labeled box drawn around the nodes whose group id names it, such as the
/// services of one subsystem. A collapsed group is drawn as a single node and
/// the edges of its members end on it.
public class GraphGroup {
    /// A box drawn around its members wherever they are, the default.
    public static final int KIND_GROUP = 0;

    /// A swimlane: a band with bounds of its own and a title along one side.
    /// Nodes are in the lane they were dropped in.
    public static final int KIND_LANE = 1;

    private final String id;
    private String label;
    private boolean collapsed;
    private String styleKey;
    private int kind;
    private boolean horizontal = true;
    private double x;
    private double y;
    private double width;
    private double height;

    /// A group.
    public GraphGroup(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /// The id members name in `GraphNode.setGroupId`.
    public String getId() {
        return id;
    }

    /// The title painted on the group.
    public String getLabel() {
        return label;
    }

    /// Sets the title.
    public void setLabel(String label) {
        this.label = label;
    }

    /// True when the group is drawn as one node.
    public boolean isCollapsed() {
        return collapsed;
    }

    /// Collapses or expands the group. A model shown by a component is told
    /// through `GraphModel.setCollapsed`. A lane does not collapse: its band
    /// is where its members are placed, and the flag is ignored for it.
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /// A key a renderer may map to a colour, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public void setStyleKey(String styleKey) {
        this.styleKey = styleKey;
    }

    /// `KIND_GROUP` or `KIND_LANE`.
    public int getKind() {
        return kind;
    }

    /// Sets the kind.
    public void setKind(int kind) {
        this.kind = kind;
    }

    /// True when this is a swimlane.
    public boolean isLane() {
        return kind == KIND_LANE;
    }

    /// Makes a swimlane with these bounds in world units.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id members name in `GraphNode.setGroupId`
    /// - `label`: the title
    /// - `horizontal`: true for a band that runs left to right with its title
    ///   on the leading side, false for a column with its title on top
    public static GraphGroup lane(String id, String label, boolean horizontal,
                                  double x, double y, double width, double height) {
        GraphGroup g = new GraphGroup(id, label);
        g.kind = KIND_LANE;
        g.horizontal = horizontal;
        g.setBounds(x, y, width, height);
        return g;
    }

    /// True, the default, when a lane runs left to right.
    public boolean isHorizontal() {
        return horizontal;
    }

    /// Sets the direction a lane runs in.
    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    /// Sets a lane's bounds in world units. A model shown by a component is
    /// told through `GraphModel.resizeGroup`. An ordinary group has no bounds
    /// of its own and ignores them.
    public void setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /// A lane's left edge.
    public double getX() {
        return x;
    }

    /// A lane's top edge.
    public double getY() {
        return y;
    }

    /// A lane's width.
    public double getWidth() {
        return width;
    }

    /// A lane's height.
    public double getHeight() {
        return height;
    }

    /// True when this is a lane and the world point lies inside it.
    public boolean contains(double px, double py) {
        return kind == KIND_LANE && px >= x && py >= y && px < x + width && py < y + height;
    }
}
