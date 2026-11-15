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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// An editable graph kept in memory.
///
/// Edits made by a user are checked by the `ConnectionValidator` before an
/// edge is made. Ids of edges the model makes itself are `"e"` followed by a
/// counter, skipping any already taken.
public class DefaultGraphModel extends AbstractGraphModel {
    private final List<GraphNode> nodes = new ArrayList<GraphNode>();
    private final List<GraphEdge> edges = new ArrayList<GraphEdge>();
    private final List<GraphGroup> groups = new ArrayList<GraphGroup>();
    private final Map<String, GraphNode> nodeIndex = new HashMap<String, GraphNode>();
    private final Map<String, GraphEdge> edgeIndex = new HashMap<String, GraphEdge>();
    private ConnectionValidator validator = new ConnectionValidator();
    private int edgeCounter;

    @Override
    public List<GraphNode> getNodes() {
        return nodes;
    }

    @Override
    public List<GraphEdge> getEdges() {
        return edges;
    }

    @Override
    public List<GraphGroup> getGroups() {
        return groups;
    }

    @Override
    public GraphNode getNode(String id) {
        return id == null ? null : nodeIndex.get(id);
    }

    /// The edge with this id, or null.
    public GraphEdge getEdge(String id) {
        return id == null ? null : edgeIndex.get(id);
    }

    /// The validator `connect` asks. Never null.
    public ConnectionValidator getValidator() {
        return validator;
    }

    /// Replaces the validator; null restores the default one.
    public void setValidator(ConnectionValidator validator) {
        this.validator = validator == null ? new ConnectionValidator() : validator;
    }

    /// Adds a node, replacing one with the same id.
    ///
    /// #### Returns
    ///
    /// the node
    public GraphNode addNode(GraphNode node) {
        GraphNode old = nodeIndex.put(node.getId(), node);
        if (old != null) {
            nodes.remove(old);
        }
        nodes.add(node);
        fireChanged();
        return node;
    }

    /// Adds an edge as it is, without asking the validator.
    ///
    /// #### Returns
    ///
    /// the edge
    public GraphEdge addEdge(GraphEdge edge) {
        GraphEdge old = edgeIndex.put(edge.getId(), edge);
        if (old != null) {
            edges.remove(old);
        }
        edges.add(edge);
        fireChanged();
        return edge;
    }

    /// Adds an edge between two nodes with a generated id.
    public GraphEdge addEdge(String sourceId, String targetId) {
        return addEdge(new GraphEdge(nextEdgeId(), sourceId, targetId));
    }

    /// Adds a group.
    public GraphGroup addGroup(GraphGroup group) {
        groups.add(group);
        fireChanged();
        return group;
    }

    /// Removes every node, edge and group.
    public void clear() {
        nodes.clear();
        edges.clear();
        groups.clear();
        nodeIndex.clear();
        edgeIndex.clear();
        fireChanged();
    }

    /// The edges that touch a node.
    public List<GraphEdge> getEdges(GraphNode node) {
        List<GraphEdge> out = new ArrayList<GraphEdge>();
        int n = edges.size();
        for (int i = 0; i < n; i++) {
            GraphEdge e = edges.get(i);
            if (e.touches(node.getId())) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public boolean moveNode(GraphNode node, double x, double y) {
        node.setPosition(x, y);
        fireChanged();
        return true;
    }

    @Override
    public GraphEdge connect(GraphNode source, GraphPort sourcePort, GraphNode target, GraphPort targetPort) {
        if (!validator.canConnect(this, source, sourcePort, target, targetPort)) {
            return null;
        }
        GraphEdge e = new GraphEdge(nextEdgeId(), source.getId(),
                sourcePort == null ? null : sourcePort.getId(),
                target.getId(), targetPort == null ? null : targetPort.getId());
        return addEdge(e);
    }

    @Override
    public boolean removeEdge(GraphEdge edge) {
        if (!edges.remove(edge)) {
            return false;
        }
        edgeIndex.remove(edge.getId());
        fireChanged();
        return true;
    }

    @Override
    public boolean removeNode(GraphNode node) {
        if (!nodes.remove(node)) {
            return false;
        }
        nodeIndex.remove(node.getId());
        for (int i = edges.size() - 1; i >= 0; i--) {
            GraphEdge e = edges.get(i);
            if (e.touches(node.getId())) {
                edges.remove(i);
                edgeIndex.remove(e.getId());
            }
        }
        fireChanged();
        return true;
    }

    @Override
    public boolean renameNode(GraphNode node, String label) {
        node.setLabel(label);
        fireChanged();
        return true;
    }

    @Override
    public boolean setCollapsed(GraphGroup group, boolean collapsed) {
        group.setCollapsed(collapsed);
        fireChanged();
        return true;
    }

    private String nextEdgeId() {
        String id;
        do {
            edgeCounter++;
            id = "e" + edgeCounter;
        } while (edgeIndex.containsKey(id));
        return id;
    }
}
