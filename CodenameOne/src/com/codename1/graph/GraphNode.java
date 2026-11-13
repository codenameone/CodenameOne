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

/// A box in a graph: a position and a size in world units, a label, and
/// optionally ports and a group.
///
/// World units are pixels at zoom 1. The position is the top left corner and
/// may be negative: a graph has no origin it must stay to the right of.
public class GraphNode {
    private final String id;
    private String label;
    private String description;
    private double x;
    private double y;
    private double width;
    private double height;
    private String groupId;
    private String styleKey;
    private boolean pinned;
    private Object userData;
    private final List<GraphPort> inputs = new ArrayList<GraphPort>();
    private final List<GraphPort> outputs = new ArrayList<GraphPort>();

    /// A node of the default size, 160 by 64, at the origin.
    public GraphNode(String id, String label) {
        this(id, label, 0, 0, 160, 64);
    }

    /// A node at a position.
    public GraphNode(String id, String label, double x, double y, double width, double height) {
        this.id = id;
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /// The id, unique within the model.
    public String getId() {
        return id;
    }

    /// The title painted on the node.
    public String getLabel() {
        return label;
    }

    /// Sets the title.
    public void setLabel(String label) {
        this.label = label;
    }

    /// A second line painted under the title, or null.
    public String getDescription() {
        return description;
    }

    /// Sets the second line.
    public void setDescription(String description) {
        this.description = description;
    }

    /// The left edge in world units.
    public double getX() {
        return x;
    }

    /// The top edge in world units.
    public double getY() {
        return y;
    }

    /// The width in world units.
    public double getWidth() {
        return width;
    }

    /// The height in world units.
    public double getHeight() {
        return height;
    }

    /// The horizontal centre.
    public double getCenterX() {
        return x + width / 2;
    }

    /// The vertical centre.
    public double getCenterY() {
        return y + height / 2;
    }

    /// Moves the node. A model shown by a component is told through
    /// `GraphModel.moveNode`, which calls this and notifies the listeners.
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /// Resizes the node.
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /// The id of the `GraphGroup` the node is drawn inside, or null.
    public String getGroupId() {
        return groupId;
    }

    /// Puts the node in a group, or takes it out with null.
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /// A key a renderer may map to a colour or a UIID, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public void setStyleKey(String styleKey) {
        this.styleKey = styleKey;
    }

    /// True when automatic layouts must leave the node where it is.
    public boolean isPinned() {
        return pinned;
    }

    /// Pins the node in place for automatic layouts.
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    /// Anything the application keeps with the node.
    public Object getUserData() {
        return userData;
    }

    /// Sets the application's data.
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /// Adds a port, as an input or an output by its direction.
    ///
    /// #### Returns
    ///
    /// this node, so calls can be chained
    public GraphNode addPort(GraphPort port) {
        port.node = this;
        if (port.isInput()) {
            inputs.add(port);
        } else {
            outputs.add(port);
        }
        return this;
    }

    /// Adds an input port.
    public GraphNode addInput(String id, String label, String type) {
        return addPort(new GraphPort(id, GraphPort.INPUT, label, type));
    }

    /// Adds an output port.
    public GraphNode addOutput(String id, String label, String type) {
        return addPort(new GraphPort(id, GraphPort.OUTPUT, label, type));
    }

    /// The input ports, in order. The list is the node's own; do not change it.
    public List<GraphPort> getInputs() {
        return inputs;
    }

    /// The output ports, in order. The list is the node's own; do not change it.
    public List<GraphPort> getOutputs() {
        return outputs;
    }

    /// True when the node has any port.
    public boolean hasPorts() {
        return !inputs.isEmpty() || !outputs.isEmpty();
    }

    /// The port with this id, or null.
    public GraphPort getPort(String portId) {
        if (portId == null) {
            return null;
        }
        for (GraphPort p : inputs) {
            if (portId.equals(p.getId())) {
                return p;
            }
        }
        for (GraphPort p : outputs) {
            if (portId.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /// True when the world point lies inside the node.
    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
