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
package com.codename1.ui.graph;

import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;

/// A `GraphListener` that refuses nothing and ignores everything; override
/// what you need.
public class GraphAdapter implements GraphListener {
    @Override
    public void selectionChanged(NodeGraph graph) {
    }

    @Override
    public void nodeOpened(NodeGraph graph, GraphNode node) {
    }

    @Override
    public boolean nodeMoving(NodeGraph graph, GraphNode node, double x, double y) {
        return true;
    }

    @Override
    public void nodeMoved(NodeGraph graph, GraphNode node) {
    }

    @Override
    public boolean edgeCreating(NodeGraph graph, GraphNode source, GraphPort sourcePort, GraphNode target,
                                GraphPort targetPort) {
        return true;
    }

    @Override
    public void edgeCreated(NodeGraph graph, GraphEdge edge) {
    }

    @Override
    public boolean removing(NodeGraph graph, GraphNode node, GraphEdge edge) {
        return true;
    }

    @Override
    public void groupToggled(NodeGraph graph, GraphGroup group) {
    }
}
