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

/// A place on a `GraphNode` where edges attach, as in a node editor.
///
/// A node with no ports is connected node to node, as in a system diagram.
/// Ports are ordered on their side of the node in the order they were added.
public class GraphPort {
    /// The port takes incoming edges and sits on the leading side of the node.
    public static final int INPUT = 0;

    /// The port starts outgoing edges and sits on the trailing side.
    public static final int OUTPUT = 1;

    private final String id;
    private final int direction;
    private String label;
    private String type;
    private int maxConnections = -1;
    GraphNode node;

    /// A port.
    ///
    /// #### Parameters
    ///
    /// - `id`: unique within its node
    /// - `direction`: `INPUT` or `OUTPUT`
    /// - `label`: painted beside the port, may be null
    /// - `type`: compared by `ConnectionValidator`; null accepts any type
    public GraphPort(String id, int direction, String label, String type) {
        this.id = id;
        this.direction = direction;
        this.label = label;
        this.type = type;
    }

    /// The id, unique within the node.
    public String getId() {
        return id;
    }

    /// `INPUT` or `OUTPUT`.
    public int getDirection() {
        return direction;
    }

    /// True for an `INPUT` port.
    public boolean isInput() {
        return direction == INPUT;
    }

    /// The label, or null.
    public String getLabel() {
        return label;
    }

    /// Sets the label.
    public void setLabel(String label) {
        this.label = label;
    }

    /// The data type edges must agree on, or null for any.
    public String getType() {
        return type;
    }

    /// Sets the data type.
    public void setType(String type) {
        this.type = type;
    }

    /// How many edges may attach, or -1 for any number.
    public int getMaxConnections() {
        return maxConnections;
    }

    /// Limits the edges that may attach; -1 for any number.
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /// The node the port belongs to, or null before it is added to one.
    public GraphNode getNode() {
        return node;
    }
}
