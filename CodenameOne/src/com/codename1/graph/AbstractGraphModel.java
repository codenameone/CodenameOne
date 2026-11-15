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

/// The listener list, the lookups and the refusals every read-only model
/// shares.
///
/// A subclass supplies `getNodes`, `getEdges` and `getGroups`. Every edit is
/// refused until the matching method is overridden.
public abstract class AbstractGraphModel implements GraphModel {
    private final List<GraphChangeListener> listeners = new ArrayList<GraphChangeListener>();

    @Override
    public GraphNode getNode(String id) {
        if (id == null) {
            return null;
        }
        List<GraphNode> nodes = getNodes();
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            GraphNode node = nodes.get(i);
            if (id.equals(node.getId())) {
                return node;
            }
        }
        return null;
    }

    @Override
    public GraphGroup getGroup(String id) {
        if (id == null) {
            return null;
        }
        List<GraphGroup> groups = getGroups();
        int n = groups.size();
        for (int i = 0; i < n; i++) {
            GraphGroup g = groups.get(i);
            if (id.equals(g.getId())) {
                return g;
            }
        }
        return null;
    }

    @Override
    public boolean moveNode(GraphNode node, double x, double y) {
        return false;
    }

    @Override
    public GraphEdge connect(GraphNode source, GraphPort sourcePort, GraphNode target, GraphPort targetPort) {
        return null;
    }

    @Override
    public boolean removeEdge(GraphEdge edge) {
        return false;
    }

    @Override
    public boolean removeNode(GraphNode node) {
        return false;
    }

    @Override
    public boolean renameNode(GraphNode node, String label) {
        return false;
    }

    @Override
    public boolean setCollapsed(GraphGroup group, boolean collapsed) {
        return false;
    }

    @Override
    public void addChangeListener(GraphChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeChangeListener(GraphChangeListener l) {
        listeners.remove(l);
    }

    /// Tells every listener the model changed.
    protected void fireChanged() {
        int n = listeners.size();
        for (int i = 0; i < n; i++) {
            listeners.get(i).graphChanged(this);
        }
    }
}
