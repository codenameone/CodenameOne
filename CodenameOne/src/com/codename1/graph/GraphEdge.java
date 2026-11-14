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

/// A connection from one node to another, optionally from a port to a port.
public class GraphEdge {
    private final String id;
    private final String sourceId;
    private final String sourcePortId;
    private final String targetId;
    private final String targetPortId;
    private String label;
    private boolean directed = true;
    private String styleKey;
    private Object userData;
    private double[] route;

    /// An edge between two nodes.
    public GraphEdge(String id, String sourceId, String targetId) {
        this(id, sourceId, null, targetId, null);
    }

    /// An edge between two ports. Either port id may be null to attach to
    /// the node itself.
    public GraphEdge(String id, String sourceId, String sourcePortId, String targetId, String targetPortId) {
        this.id = id;
        this.sourceId = sourceId;
        this.sourcePortId = sourcePortId;
        this.targetId = targetId;
        this.targetPortId = targetPortId;
    }

    /// The id, unique within the model.
    public String getId() {
        return id;
    }

    /// The id of the node the edge leaves.
    public String getSourceId() {
        return sourceId;
    }

    /// The id of the output port the edge leaves, or null.
    public String getSourcePortId() {
        return sourcePortId;
    }

    /// The id of the node the edge enters.
    public String getTargetId() {
        return targetId;
    }

    /// The id of the input port the edge enters, or null.
    public String getTargetPortId() {
        return targetPortId;
    }

    /// The text painted at the middle of the edge, or null.
    public String getLabel() {
        return label;
    }

    /// Sets the label.
    public void setLabel(String label) {
        this.label = label;
    }

    /// True, the default, when the edge is painted with an arrowhead.
    public boolean isDirected() {
        return directed;
    }

    /// Paints the edge with or without an arrowhead.
    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    /// A key a renderer may map to a colour or a dash, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public void setStyleKey(String styleKey) {
        this.styleKey = styleKey;
    }

    /// Anything the application keeps with the edge.
    public Object getUserData() {
        return userData;
    }

    /// Sets the application's data.
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /// The bend points an arrangement routed the edge through, as x, y pairs in
    /// world units, or null for a direct line between its ends.
    public double[] getRoute() {
        return route;
    }

    /// Sets the bend points; null removes them.
    public void setRoute(double[] route) {
        this.route = route;
    }

    /// True when the edge touches the node with this id.
    public boolean touches(String nodeId) {
        return nodeId != null && (nodeId.equals(sourceId) || nodeId.equals(targetId));
    }
}
