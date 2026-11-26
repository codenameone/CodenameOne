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
package com.codename1.presentation;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphShapes;
import com.codename1.graph.arrange.ArrangementResult;
import com.codename1.graph.arrange.GraphArrangement;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.graph.arrange.RadialArrangement;
import com.codename1.graph.arrange.TreeArrangement;
import com.codename1.wordprocessing.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A diagram on a slide -- a process, a hierarchy, a cycle -- kept as what it means: nodes,
/// the edges between them and how they are arranged. Edit the nodes and the diagram lays
/// itself out again, the way an office suite's SmartArt does.
///
/// The arrangement is computed by `com.codename1.graph.arrange`, the same algorithms a
/// `com.codename1.ui.graph.NodeGraph` uses, and node shapes are named as in `GraphShapes`.
/// `toGroup` expands the diagram into ordinary shapes and connectors, which is how it is
/// painted and how it is written to formats without diagrams (PPTX, ODP); only `.cn1slides`
/// keeps the diagram itself.
public final class DiagramElement extends SlideElement {
    /// Left to right steps (layered arrangement).
    public static final String PROCESS = "process";

    /// A top-down tree from the first node (tree arrangement).
    public static final String HIERARCHY = "hierarchy";

    /// Nodes around a circle, each joined to the next.
    public static final String CYCLE = "cycle";

    /// Rings around the first node (radial arrangement).
    public static final String RADIAL = "radial";

    /// A node of a diagram.
    public static final class Node {
        /// The id, unique in the diagram.
        public final String id;

        /// The label.
        public final String label;

        /// A `GraphShapes` name.
        public final String shape;

        /// The fill as 0xRRGGBB, or -1 for the diagram's color.
        public final int color;

        /// A node.
        ///
        /// #### Parameters
        ///
        /// - `id`: the id
        ///
        /// - `label`: the label
        ///
        /// - `shape`: a `GraphShapes` name, or null for a rounded box
        ///
        /// - `color`: 0xRRGGBB, or -1
        public Node(String id, String label, String shape, int color) {
            if (id == null || id.length() == 0) {
                throw new IllegalArgumentException("A node needs an id");
            }
            this.id = id;
            this.label = label == null ? "" : label;
            this.shape = shape == null ? GraphShapes.ROUNDED : shape;
            this.color = color < 0 ? -1 : color & 0xffffff;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) {
                return false;
            }
            Node n = (Node) o;
            return id.equals(n.id) && label.equals(n.label) && shape.equals(n.shape)
                    && color == n.color;
        }

        @Override
        public int hashCode() {
            return id.hashCode() * 31 + label.hashCode();
        }
    }

    /// The nodes, never null.
    public final List<Node> nodes;

    /// The edges as pairs of node ids: element 0 is from, 1 is to.
    public final List<String[]> edges;

    /// `PROCESS`, `HIERARCHY`, `CYCLE` or `RADIAL`.
    public final String arrangement;

    /// The default node fill as 0xRRGGBB.
    public final int color;

    /// The label text color as 0xRRGGBB.
    public final int textColor;

    /// A diagram.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `arrangement`: `PROCESS`, `HIERARCHY`, `CYCLE` or `RADIAL`
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `nodes`: the nodes
    ///
    /// - `edges`: pairs of node ids; for `PROCESS` and `CYCLE` null joins each node to the
    ///   next
    ///
    /// - `color`: the node fill
    ///
    /// - `textColor`: the label color
    public DiagramElement(String id, String arrangement, float x, float y, float width,
            float height, List<Node> nodes, List<String[]> edges, int color, int textColor) {
        this(id, null, arrangement, x, y, width, height, 0, nodes, edges, color, textColor);
    }

    /// A diagram with a name and rotation.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `arrangement`: the arrangement
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `nodes`: the nodes
    ///
    /// - `edges`: pairs of node ids, or null for the arrangement's default
    ///
    /// - `color`: the node fill
    ///
    /// - `textColor`: the label color
    public DiagramElement(String id, String name, String arrangement, float x, float y,
            float width, float height, float rotation, List<Node> nodes, List<String[]> edges,
            int color, int textColor) {
        super(id, name, x, y, width, height, rotation, false, false);
        this.arrangement = HIERARCHY.equals(arrangement) || CYCLE.equals(arrangement)
                || RADIAL.equals(arrangement) ? arrangement : PROCESS;
        List<Node> n = new ArrayList<Node>();
        // a set: a diagram read from a file may hold many nodes, and a list lookup per node
        // and per edge would be quadratic
        Set<String> ids = new HashSet<String>();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node != null && !ids.contains(node.id)) {
                    n.add(node);
                    ids.add(node.id);
                }
            }
        }
        this.nodes = Collections.unmodifiableList(n);
        List<String[]> e = new ArrayList<String[]>();
        if (edges == null) {
            for (int i = 0; i + 1 < n.size(); i++) {
                e.add(new String[] {n.get(i).id, n.get(i + 1).id});
            }
            if (CYCLE.equals(this.arrangement) && n.size() > 2) {
                e.add(new String[] {n.get(n.size() - 1).id, n.get(0).id});
            }
        } else {
            for (String[] pair : edges) {
                if (pair != null && pair.length >= 2 && ids.contains(pair[0])
                        && ids.contains(pair[1]) && !pair[0].equals(pair[1])) {
                    e.add(new String[] {pair[0], pair[1]});
                }
            }
        }
        this.edges = Collections.unmodifiableList(e);
        this.color = color & 0xffffff;
        this.textColor = textColor & 0xffffff;
    }

    /// A process of steps, each joined to the next.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `steps`: the labels
    ///
    /// #### Returns
    ///
    /// the diagram
    public static DiagramElement process(String id, float x, float y, float width,
            float height, String... steps) {
        List<Node> n = new ArrayList<Node>();
        for (int i = 0; i < steps.length; i++) {
            n.add(new Node("n" + (i + 1), steps[i], null, -1));
        }
        return new DiagramElement(id, PROCESS, x, y, width, height, n, null,
                Theme.DEFAULT.color(Theme.ACCENT1), 0xffffff);
    }

    /// This diagram with other nodes and edges.
    ///
    /// #### Parameters
    ///
    /// - `n`: the nodes
    ///
    /// - `e`: the edges, or null for the default
    ///
    /// #### Returns
    ///
    /// the diagram
    public DiagramElement withNodes(List<Node> n, List<String[]> e) {
        return new DiagramElement(id, name, arrangement, x, y, width, height, rotation, n, e,
                color, textColor);
    }

    /// This diagram arranged another way.
    ///
    /// #### Parameters
    ///
    /// - `value`: the arrangement
    ///
    /// #### Returns
    ///
    /// the diagram
    public DiagramElement withArrangement(String value) {
        return new DiagramElement(id, name, value, x, y, width, height, rotation, nodes, edges,
                color, textColor);
    }

    /// The diagram as plain shapes and connectors, laid out to fit the element's box. Ids are
    /// this element's id followed by the node id, or by `e` and the edge's index.
    ///
    /// #### Returns
    ///
    /// the group
    public GroupElement toGroup() {
        int count = nodes.size();
        double[][] pos = new double[count][];
        double nodeW = 120;
        double nodeH = 54;
        if (CYCLE.equals(arrangement)) {
            double r = Math.max(120, count * 40);
            for (int i = 0; i < count; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, count);
                pos[i] = new double[] {r * Math.cos(a) - nodeW / 2, r * Math.sin(a) - nodeH / 2};
            }
        } else {
            DefaultGraphModel model = new DefaultGraphModel();
            for (Node n : nodes) {
                model.addNode(new GraphNode(n.id, n.label, 0, 0, nodeW, nodeH));
            }
            for (String[] e : edges) {
                model.addEdge(e[0], e[1]);
            }
            ArrangementResult res = count == 0 ? null : arrangementFor().arrange(model);
            for (int i = 0; i < count; i++) {
                double[] p = res == null ? null : res.getPosition(nodes.get(i).id);
                pos[i] = p == null ? new double[] {i * (nodeW + 40), 0} : p;
            }
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            minX = Math.min(minX, pos[i][0]);
            minY = Math.min(minY, pos[i][1]);
            maxX = Math.max(maxX, pos[i][0] + nodeW);
            maxY = Math.max(maxY, pos[i][1] + nodeH);
        }
        List<SlideElement> out = new ArrayList<SlideElement>();
        if (count == 0) {
            out.add(new ShapeElement(id + "#empty", "rect", x, y, width, height, null,
                    new LineStyle(0x9aa3ad, 1).withDash(LineStyle.DASH), null));
            return new GroupElement(id, name, out, x, y, width, height, rotation, false, false);
        }
        double scale = Math.min(width / (maxX - minX), height / (maxY - minY));
        double offX = x + (width - (maxX - minX) * scale) / 2;
        double offY = y + (height - (maxY - minY) * scale) / 2;
        float w = (float) (nodeW * scale);
        float h = (float) (nodeH * scale);
        float[][] boxes = new float[count][];
        for (int i = 0; i < count; i++) {
            boxes[i] = new float[] {(float) (offX + (pos[i][0] - minX) * scale),
                (float) (offY + (pos[i][1] - minY) * scale), w, h};
        }
        float stroke = Math.max(1, (float) (2 * scale));
        int k = 0;
        for (String[] e : edges) {
            float[] a = boxes[index(e[0])];
            float[] b = boxes[index(e[1])];
            float[] p = edgePoint(a, b);
            float[] q = edgePoint(b, a);
            out.add(ShapeElement.line(id + "#e" + (k++), p[0], p[1], q[0], q[1],
                    new LineStyle(0x7b8794, stroke).withArrows(LineStyle.ARROW_NONE,
                            LineStyle.ARROW_TRIANGLE)));
        }
        float fontSize = Math.max(6, (float) (14 * scale));
        TextStyle style = TextStyle.DEFAULT.withFont("Sans").withSize(fontSize)
                .withColor(textColor);
        for (int i = 0; i < count; i++) {
            Node n = nodes.get(i);
            float[] r = boxes[i];
            TextFrame t = TextFrame.of(id + "#n" + n.id + "p", n.label, style, "center")
                    .withAnchor(TextFrame.MIDDLE).withAutofit(TextFrame.FIT_SHRINK);
            out.add(new ShapeElement(id + "#n" + n.id, n.label, geometryOf(n.shape), r[0], r[1], r[2],
                    r[3], 0, false, false, Fill.solid(n.color < 0 ? color : n.color), null, t,
                    null, null));
        }
        return new GroupElement(id, name, out, x, y, width, height, rotation, false, false);
    }

    private GraphArrangement arrangementFor() {
        if (HIERARCHY.equals(arrangement)) {
            TreeArrangement t = new TreeArrangement();
            t.setDirection(TreeArrangement.TOP_TO_BOTTOM);
            t.setRootId(nodes.get(0).id);
            return t;
        }
        if (RADIAL.equals(arrangement)) {
            RadialArrangement r = new RadialArrangement();
            r.setRootId(nodes.get(0).id);
            return r;
        }
        LayeredArrangement l = new LayeredArrangement();
        l.setDirection(LayeredArrangement.LEFT_TO_RIGHT);
        return l;
    }

    private int index(String nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id.equals(nodeId)) {
                return i;
            }
        }
        return 0;
    }

    /// Where the line from the centre of `a` towards the centre of `b` leaves `a`.
    private static float[] edgePoint(float[] a, float[] b) {
        float cx = a[0] + a[2] / 2;
        float cy = a[1] + a[3] / 2;
        float dx = b[0] + b[2] / 2 - cx;
        float dy = b[1] + b[3] / 2 - cy;
        if (dx == 0 && dy == 0) {
            return new float[] {cx, cy};
        }
        float tx = dx == 0 ? Float.MAX_VALUE : Math.abs(a[2] / 2 / dx);
        float ty = dy == 0 ? Float.MAX_VALUE : Math.abs(a[3] / 2 / dy);
        float t = Math.min(tx, ty);
        return new float[] {cx + dx * t, cy + dy * t};
    }

    /// The preset geometry a `GraphShapes` name is drawn as.
    ///
    /// #### Parameters
    ///
    /// - `shape`: the `GraphShapes` name
    ///
    /// #### Returns
    ///
    /// the preset geometry name
    public static String geometryOf(String shape) {
        if (GraphShapes.RECT.equals(shape)) {
            return "rect";
        }
        if (GraphShapes.ELLIPSE.equals(shape)) {
            return "ellipse";
        }
        if (GraphShapes.DECISION.equals(shape)) {
            return "diamond";
        }
        if (GraphShapes.TERMINATOR.equals(shape)) {
            return "flowChartTerminator";
        }
        if (GraphShapes.DATA.equals(shape)) {
            return "parallelogram";
        }
        if (GraphShapes.DOCUMENT.equals(shape)) {
            return "flowChartDocument";
        }
        if (GraphShapes.PREPARATION.equals(shape)) {
            return "hexagon";
        }
        if (GraphShapes.DATABASE.equals(shape)) {
            return "can";
        }
        if (GraphShapes.SUBPROCESS.equals(shape)) {
            return "flowChartPredefinedProcess";
        }
        if (GraphShapes.NOTE.equals(shape)) {
            return "foldedCorner";
        }
        return "roundRect";
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        return new DiagramElement(id, name, arrangement, nx, ny, nw, nh, rot, nodes, edges,
                color, textColor);
    }

    @Override
    public SlideElement withId(String value) {
        return new DiagramElement(value, name, arrangement, x, y, width, height, rotation,
                nodes, edges, color, textColor);
    }

    @Override
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (Node n : nodes) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(n.label);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DiagramElement)) {
            return false;
        }
        DiagramElement d = (DiagramElement) o;
        if (!sameFrame(d) || !arrangement.equals(d.arrangement) || color != d.color
                || textColor != d.textColor || !nodes.equals(d.nodes)
                || edges.size() != d.edges.size()) {
            return false;
        }
        for (int i = 0; i < edges.size(); i++) {
            if (!edges.get(i)[0].equals(d.edges.get(i)[0])
                    || !edges.get(i)[1].equals(d.edges.get(i)[1])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + nodes.hashCode() + arrangement.hashCode();
    }
}
