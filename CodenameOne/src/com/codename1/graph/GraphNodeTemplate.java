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

import java.util.ArrayList;
import java.util.List;

/// A kind of node a user can add: what a `com.codename1.ui.graph.GraphPalette`
/// lists, and what a node editor calls a node type.
///
/// ```java
/// GraphNodeTemplate decision = new GraphNodeTemplate("Decision")
///         .setShape(GraphShapes.DECISION).setSize(140, 90);
/// GraphNodeTemplate add = new GraphNodeTemplate("Add")
///         .addInput("a", "A", "number").addInput("b", "B", "number")
///         .addOutput("sum", "Sum", "number");
/// ```
///
/// Override `create` to make nodes that carry data of your own.
public class GraphNodeTemplate {
    private final String name;
    private String label;
    private String description;
    private String shape;
    private String styleKey;
    private double width = 160;
    private double height = 64;
    private final List<GraphPort> ports = new ArrayList<GraphPort>();

    /// A template whose nodes are labeled with its name.
    public GraphNodeTemplate(String name) {
        this.name = name;
        this.label = name;
    }

    /// The name a palette shows.
    public String getName() {
        return name;
    }

    /// The label new nodes get.
    public String getLabel() {
        return label;
    }

    /// Sets the label new nodes get.
    public GraphNodeTemplate setLabel(String label) {
        this.label = label;
        return this;
    }

    /// The description new nodes get, or null.
    public String getDescription() {
        return description;
    }

    /// Sets the description new nodes get.
    public GraphNodeTemplate setDescription(String description) {
        this.description = description;
        return this;
    }

    /// The outline of new nodes, one of the `GraphShapes` names, or null.
    public String getShape() {
        return shape;
    }

    /// Sets the outline of new nodes.
    public GraphNodeTemplate setShape(String shape) {
        this.shape = shape;
        return this;
    }

    /// The style key of new nodes, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key of new nodes.
    public GraphNodeTemplate setStyleKey(String styleKey) {
        this.styleKey = styleKey;
        return this;
    }

    /// The width of new nodes in world units.
    public double getWidth() {
        return width;
    }

    /// The height of new nodes in world units.
    public double getHeight() {
        return height;
    }

    /// Sets the size of new nodes in world units.
    public GraphNodeTemplate setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /// Gives new nodes an input port.
    public GraphNodeTemplate addInput(String id, String label, String type) {
        ports.add(new GraphPort(id, GraphPort.INPUT, label, type));
        return this;
    }

    /// Gives new nodes an output port.
    public GraphNodeTemplate addOutput(String id, String label, String type) {
        ports.add(new GraphPort(id, GraphPort.OUTPUT, label, type));
        return this;
    }

    /// True when new nodes have ports.
    public boolean hasPorts() {
        return !ports.isEmpty();
    }

    /// Makes a node at the origin; the view places it.
    ///
    /// #### Parameters
    ///
    /// - `id`: an id no node of the model has
    public GraphNode create(String id) {
        GraphNode n = new GraphNode(id, label, 0, 0, width, height);
        n.setDescription(description);
        n.setShape(shape);
        n.setStyleKey(styleKey);
        int count = ports.size();
        for (int i = 0; i < count; i++) {
            GraphPort p = ports.get(i);
            GraphPort q = new GraphPort(p.getId(), p.getDirection(), p.getLabel(), p.getType());
            q.setMaxConnections(p.getMaxConnections());
            q.setValue(p.getValue());
            n.addPort(q);
        }
        return n;
    }
}
