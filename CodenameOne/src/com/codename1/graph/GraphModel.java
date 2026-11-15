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

import java.util.List;

/// The nodes, edges and groups a `com.codename1.ui.graph.NodeGraph` shows.
///
/// Every edit a user makes in the view goes through one of the methods below,
/// so a model that refuses an edit (by returning false or null) keeps the
/// graph as it was. `AbstractGraphModel` refuses all of them; extend
/// `DefaultGraphModel` for an editable graph in memory.
public interface GraphModel {
    /// Every node, in painting order: later nodes are drawn over earlier ones.
    List<GraphNode> getNodes();

    /// Every edge.
    List<GraphEdge> getEdges();

    /// Every group.
    List<GraphGroup> getGroups();

    /// The node with this id, or null.
    GraphNode getNode(String id);

    /// The group with this id, or null.
    GraphGroup getGroup(String id);

    /// Moves a node to a world position.
    ///
    /// #### Returns
    ///
    /// false when the model refuses the move
    boolean moveNode(GraphNode node, double x, double y);

    /// Makes an edge from one node or port to another.
    ///
    /// #### Parameters
    ///
    /// - `source`: the node the edge leaves
    /// - `sourcePort`: its output port, or null
    /// - `target`: the node the edge enters
    /// - `targetPort`: its input port, or null
    ///
    /// #### Returns
    ///
    /// the new edge, or null when the model refuses it
    GraphEdge connect(GraphNode source, GraphPort sourcePort, GraphNode target, GraphPort targetPort);

    /// Removes an edge.
    ///
    /// #### Returns
    ///
    /// false when the model refuses
    boolean removeEdge(GraphEdge edge);

    /// Removes a node and every edge that touches it.
    ///
    /// #### Returns
    ///
    /// false when the model refuses
    boolean removeNode(GraphNode node);

    /// Gives a node a new label.
    ///
    /// #### Returns
    ///
    /// false when the model refuses
    boolean renameNode(GraphNode node, String label);

    /// Collapses or expands a group.
    ///
    /// #### Returns
    ///
    /// false when the model refuses
    boolean setCollapsed(GraphGroup group, boolean collapsed);

    /// Adds a listener told about every change.
    void addChangeListener(GraphChangeListener l);

    /// Removes a listener.
    void removeChangeListener(GraphChangeListener l);
}
