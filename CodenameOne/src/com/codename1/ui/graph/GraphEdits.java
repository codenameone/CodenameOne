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
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;
import com.codename1.util.UndoableEdit;

import java.util.List;

/// The steps a `NodeGraph` records in an `UndoHistory`. Each replays through
/// the model, the way the user's own edit went, so a listening view repaints.
final class GraphEdits { // NOPMD MissingStaticMethodInNonInstantiatableClass - a holder of the edit classes
    private GraphEdits() {
    }

    /// A node went from one place to another.
    static final class Move implements UndoableEdit {
        private final GraphModel model;
        private final GraphNode node;
        private final double fromX;
        private final double fromY;
        private final double toX;
        private final double toY;

        Move(GraphModel model, GraphNode node, double fromX, double fromY, double toX, double toY) {
            this.model = model;
            this.node = node;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @Override
        public void undo() {
            model.moveNode(node, fromX, fromY);
        }

        @Override
        public void redo() {
            model.moveNode(node, toX, toY);
        }

        @Override
        public String getName() {
            return "Move";
        }
    }

    /// Edges lost the routes an arrangement gave them when a node moved.
    static final class Routes implements UndoableEdit {
        private final GraphModel model;
        private final List<GraphEdge> edges;
        private final List<double[]> before;
        private final List<double[]> after;

        Routes(GraphModel model, List<GraphEdge> edges, List<double[]> before, List<double[]> after) {
            this.model = model;
            this.edges = edges;
            this.before = before;
            this.after = after;
        }

        private void apply(List<double[]> routes) {
            int n = edges.size();
            for (int i = 0; i < n; i++) {
                edges.get(i).setRoute(routes == null ? null : routes.get(i));
            }
            // a route is not an edit of the model; moving a node nowhere tells
            // its listeners to repaint
            List<GraphNode> nodes = model.getNodes();
            if (!nodes.isEmpty()) {
                GraphNode first = nodes.get(0);
                model.moveNode(first, first.getX(), first.getY());
            }
        }

        @Override
        public void undo() {
            apply(before);
        }

        @Override
        public void redo() {
            apply(after);
        }

        @Override
        public String getName() {
            return "Move";
        }
    }

    /// A node took a new size, and with a leading or top corner a new place.
    static final class Resize implements UndoableEdit {
        private final GraphModel model;
        private final GraphNode node;
        private final double[] from;
        private final double[] to;

        Resize(GraphModel model, GraphNode node, double[] from, double[] to) {
            this.model = model;
            this.node = node;
            this.from = from;
            this.to = to;
        }

        private void apply(double[] b) {
            model.moveNode(node, b[0], b[1]);
            model.resizeNode(node, b[2], b[3]);
        }

        @Override
        public void undo() {
            apply(from);
        }

        @Override
        public void redo() {
            apply(to);
        }

        @Override
        public String getName() {
            return "Resize";
        }
    }

    /// Nodes and edges were added, or with `removal` taken out.
    static final class Presence implements UndoableEdit {
        private final GraphModel model;
        private final List<GraphNode> nodes;
        private final List<GraphEdge> edges;
        private final boolean removal;
        private final String name;

        Presence(GraphModel model, List<GraphNode> nodes, List<GraphEdge> edges, boolean removal, String name) {
            this.model = model;
            this.nodes = nodes;
            this.edges = edges;
            this.removal = removal;
            this.name = name;
        }

        private void put() {
            int n = nodes.size();
            for (int i = 0; i < n; i++) {
                model.insertNode(nodes.get(i));
            }
            n = edges.size();
            for (int i = 0; i < n; i++) {
                model.insertEdge(edges.get(i));
            }
        }

        private void take() {
            for (int i = edges.size() - 1; i >= 0; i--) {
                model.removeEdge(edges.get(i));
            }
            for (int i = nodes.size() - 1; i >= 0; i--) {
                model.removeNode(nodes.get(i));
            }
        }

        @Override
        public void undo() {
            if (removal) {
                put();
            } else {
                take();
            }
        }

        @Override
        public void redo() {
            if (removal) {
                take();
            } else {
                put();
            }
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /// A node was given another label.
    static final class Rename implements UndoableEdit {
        private final GraphModel model;
        private final GraphNode node;
        private final String from;
        private final String to;

        Rename(GraphModel model, GraphNode node, String from, String to) {
            this.model = model;
            this.node = node;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.renameNode(node, from);
        }

        @Override
        public void redo() {
            model.renameNode(node, to);
        }

        @Override
        public String getName() {
            return "Rename";
        }
    }

    /// A group was collapsed or expanded.
    static final class Collapse implements UndoableEdit {
        private final GraphModel model;
        private final GraphGroup group;
        private final boolean to;

        Collapse(GraphModel model, GraphGroup group, boolean to) {
            this.model = model;
            this.group = group;
            this.to = to;
        }

        @Override
        public void undo() {
            model.setCollapsed(group, !to);
        }

        @Override
        public void redo() {
            model.setCollapsed(group, to);
        }

        @Override
        public String getName() {
            return to ? "Collapse" : "Expand";
        }
    }

    /// A node went into another lane or group.
    static final class Membership implements UndoableEdit {
        private final GraphModel model;
        private final GraphNode node;
        private final String from;
        private final String to;

        Membership(GraphModel model, GraphNode node, String from, String to) {
            this.model = model;
            this.node = node;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.setNodeGroup(node, from);
        }

        @Override
        public void redo() {
            model.setNodeGroup(node, to);
        }

        @Override
        public String getName() {
            return "Move";
        }
    }

    /// A lane was moved or resized.
    static final class LaneBounds implements UndoableEdit {
        private final GraphModel model;
        private final GraphGroup lane;
        private final double[] from;
        private final double[] to;

        LaneBounds(GraphModel model, GraphGroup lane, double[] from, double[] to) {
            this.model = model;
            this.lane = lane;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.resizeGroup(lane, from[0], from[1], from[2], from[3]);
        }

        @Override
        public void redo() {
            model.resizeGroup(lane, to[0], to[1], to[2], to[3]);
        }

        @Override
        public String getName() {
            return "Resize lane";
        }
    }

    /// An input port's own value changed.
    static final class PortValue implements UndoableEdit {
        private final GraphModel model;
        private final GraphPort port;
        private final Object from;
        private final Object to;

        PortValue(GraphModel model, GraphPort port, Object from, Object to) {
            this.model = model;
            this.port = port;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.setPortValue(port, from);
        }

        @Override
        public void redo() {
            model.setPortValue(port, to);
        }

        @Override
        public String getName() {
            return "Change value";
        }
    }

    /// An arrangement placed every node. Like the arrangement itself, this
    /// is not offered to the model as a user's move: nodes a model refuses to
    /// move are placed directly.
    static final class Arrange implements UndoableEdit {
        private final GraphModel model;
        private final List<GraphNode> nodes;
        private final double[] from;
        private final double[] to;
        private final List<GraphEdge> edges;
        private final List<double[]> routesFrom;
        private final List<double[]> routesTo;

        Arrange(GraphModel model, List<GraphNode> nodes, double[] from, double[] to,
                List<GraphEdge> edges, List<double[]> routesFrom, List<double[]> routesTo) {
            this.model = model;
            this.nodes = nodes;
            this.from = from;
            this.to = to;
            this.edges = edges;
            this.routesFrom = routesFrom;
            this.routesTo = routesTo;
        }

        private void apply(double[] at, List<double[]> routes) {
            int n = edges.size();
            for (int i = 0; i < n; i++) {
                edges.get(i).setRoute(routes.get(i));
            }
            n = nodes.size();
            for (int i = 0; i < n; i++) {
                GraphNode node = nodes.get(i);
                if (!model.moveNode(node, at[i * 2], at[i * 2 + 1])) {
                    node.setPosition(at[i * 2], at[i * 2 + 1]);
                }
            }
        }

        @Override
        public void undo() {
            apply(from, routesFrom);
        }

        @Override
        public void redo() {
            apply(to, routesTo);
        }

        @Override
        public String getName() {
            return "Arrange";
        }
    }
}
