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

/// What a `NodeGraph` tells an application about. Extend `GraphAdapter` to
/// implement only what is needed.
///
/// Every edit is offered first to the listeners, any of which may refuse
/// it, and then to the model, which may refuse it too.
public interface GraphListener {
    /// The selection changed.
    void selectionChanged(NodeGraph graph);

    /// A node was double tapped.
    void nodeOpened(NodeGraph graph, GraphNode node);

    /// A node was dragged and is about to be moved to a world position.
    ///
    /// #### Returns
    ///
    /// false to refuse, which puts it back
    boolean nodeMoving(NodeGraph graph, GraphNode node, double x, double y);

    /// A node was moved by the user.
    void nodeMoved(NodeGraph graph, GraphNode node);

    /// An edge was dragged from a node or port to another and is about to be
    /// made. Either port may be null.
    ///
    /// #### Returns
    ///
    /// false to refuse
    boolean edgeCreating(NodeGraph graph, GraphNode source, GraphPort sourcePort, GraphNode target, GraphPort targetPort);

    /// An edge was made by the user.
    void edgeCreated(NodeGraph graph, GraphEdge edge);

    /// A node or an edge is about to be removed by `NodeGraph.deleteSelection`.
    /// Exactly one of the two is not null.
    ///
    /// #### Returns
    ///
    /// false to refuse
    boolean removing(NodeGraph graph, GraphNode node, GraphEdge edge);

    /// A group was collapsed or expanded by a tap on its title.
    void groupToggled(NodeGraph graph, GraphGroup group);
}
