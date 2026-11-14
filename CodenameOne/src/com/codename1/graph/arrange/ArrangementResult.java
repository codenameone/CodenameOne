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
package com.codename1.graph.arrange;

import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;

import java.util.HashMap;
import java.util.Map;

/// Where an arrangement wants every node, and the bends it wants in the edges.
public class ArrangementResult {
    private final Map<String, double[]> positions = new HashMap<String, double[]>();
    private final Map<String, double[]> routes = new HashMap<String, double[]>();

    /// Records a node's top left corner.
    public void setPosition(String nodeId, double x, double y) {
        positions.put(nodeId, new double[]{x, y});
    }

    /// A node's top left corner as `{x, y}`, or null when the arrangement left it.
    public double[] getPosition(String nodeId) {
        return positions.get(nodeId);
    }

    /// Records bend points for an edge, as x, y pairs.
    public void setRoute(String edgeId, double[] route) {
        routes.put(edgeId, route);
    }

    /// An edge's bend points, or null for a straight edge.
    public double[] getRoute(String edgeId) {
        return routes.get(edgeId);
    }

    /// Moves every node at once through `GraphModel.moveNode` and sets every
    /// edge's route. Edges the arrangement did not route lose any route they had.
    public void apply(GraphModel model) {
        for (GraphNode n : model.getNodes()) {
            double[] p = positions.get(n.getId());
            if (p != null && (p[0] != n.getX() || p[1] != n.getY())) {
                model.moveNode(n, p[0], p[1]);
            }
        }
        for (GraphEdge e : model.getEdges()) {
            e.setRoute(routes.get(e.getId()));
        }
    }
}
