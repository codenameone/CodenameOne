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

import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;

/// An organic arrangement for networks: edges pull like springs and every node
/// pushes every other away, until the graph settles.
///
/// This is Fruchterman and Reingold's method with a cooling schedule. Repulsion
/// is summed through a Barnes-Hut quadtree, so a step costs n log n rather than
/// n squared, and a graph of thousands of nodes settles in well under a second.
///
/// `arrange` runs every iteration at once. A view that animates the graph
/// settling, or keeps it live while the user drags a node, calls `start` and
/// then `step` once a frame instead. Starting positions come from the model, so
/// a graph that was laid out before moves only as much as its changes need;
/// nodes that are all at one point are first spread on a circle. The run is
/// deterministic: the same graph gives the same picture.
public class ForceArrangement implements GraphArrangement {
    private double idealLength = 160;
    private int iterations = 300;
    private double theta = 0.8;

    private Graphs graph;
    private double[] px;
    private double[] py;
    private double[] dx;
    private double[] dy;
    private double temperature;
    private int remaining;

    /// The length an edge settles at, in world units, between node centres.
    public double getIdealLength() {
        return idealLength;
    }

    /// Sets the edge length.
    public void setIdealLength(double idealLength) {
        this.idealLength = Math.max(10, idealLength);
    }

    /// How many steps `arrange` runs, 300 by default.
    public int getIterations() {
        return iterations;
    }

    /// Sets the number of steps.
    public void setIterations(int iterations) {
        this.iterations = Math.max(1, iterations);
    }

    @Override
    public ArrangementResult arrange(GraphModel model) {
        start(model);
        boolean more = true;
        while (more) {
            more = step();
        }
        removeOverlaps();
        return result();
    }

    /// Prepares a stepwise run from the model's current positions.
    public void start(GraphModel model) {
        graph = new Graphs(model);
        int n = graph.size();
        px = new double[n];
        py = new double[n];
        dx = new double[n];
        dy = new double[n];
        boolean allSame = true;
        for (int i = 0; i < n; i++) {
            GraphNode node = graph.nodes[i];
            px[i] = node.getCenterX();
            py[i] = node.getCenterY();
            if (i > 0 && (px[i] != px[0] || py[i] != py[0])) {
                allSame = false;
            }
        }
        if (allSame && n > 1) {
            seed(graph, n);
        }
        temperature = idealLength * Math.max(1, Math.sqrt(n)) / 4;
        remaining = iterations;
    }

    /// Spreads nodes that all start at one point on a sunflower spiral, in
    /// breadth first order, so that neighbours start near each other. Placed
    /// on a circle in the model's order instead, the graph's cycles start as
    /// one ring around the circle, and the forces settle into a stretched loop
    /// they cannot untangle.
    private void seed(Graphs g, int n) {
        int[] order = new int[n];
        boolean[] seen = new boolean[n];
        int head = 0;
        int tail = 0;
        for (int s = 0; s < n; s++) {
            if (seen[s]) {
                continue;
            }
            seen[s] = true;
            order[tail++] = s;
            while (head < tail) {
                int v = order[head++];
                tail = visit(g.out[v], seen, order, tail);
                tail = visit(g.in[v], seen, order, tail);
            }
        }
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double r = idealLength * 0.5 * Math.sqrt(i + 0.5);
            double a = i * golden;
            px[order[i]] = Math.cos(a) * r;
            py[order[i]] = Math.sin(a) * r;
        }
    }

    private static int visit(int[] next, boolean[] seen, int[] order, int tail) {
        for (int j = 0; j < next.length; j++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            int t = next[j];
            if (!seen[t]) {
                seen[t] = true;
                order[tail++] = t;
            }
        }
        return tail;
    }

    /// Moves every node one step towards rest.
    ///
    /// #### Returns
    ///
    /// false once the run has finished
    public boolean step() {
        if (graph == null || remaining <= 0) {
            return false;
        }
        int n = graph.size();
        double k = idealLength;
        double k2 = k * k;
        for (int i = 0; i < n; i++) {
            dx[i] = 0;
            dy[i] = 0;
        }
        if (n > 1) {
            Quad root = buildTree(n);
            for (int i = 0; i < n; i++) {
                repel(root, i, k2);
            }
        }
        for (int i = 0; i < n; i++) {
            int[] out = graph.out[i];
            for (int j = 0; j < out.length; j++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                int t = out[j];
                double ex = px[i] - px[t];
                double ey = py[i] - py[t];
                double d = Math.sqrt(ex * ex + ey * ey);
                if (d < 0.01) {
                    d = 0.01;
                }
                double f = d / k;
                dx[i] -= ex * f;
                dy[i] -= ey * f;
                dx[t] += ex * f;
                dy[t] += ey * f;
            }
        }
        for (int i = 0; i < n; i++) {
            if (graph.nodes[i].isPinned()) {
                continue;
            }
            // a weak pull to the origin keeps disconnected parts from drifting
            dx[i] -= px[i] * 0.01;
            dy[i] -= py[i] * 0.01;
            double d = Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
            if (d > 0) {
                double lim = Math.min(d, temperature);
                px[i] += dx[i] / d * lim;
                py[i] += dy[i] / d * lim;
            }
        }
        temperature *= 0.97;
        if (temperature < 0.5) {
            temperature = 0.5;
        }
        remaining--;
        return remaining > 0;
    }

    /// Keeps the run going for more steps, for instance while a node is
    /// dragged, at a temperature that lets the graph follow.
    public void reheat(int steps) {
        remaining = Math.max(remaining, steps);
        temperature = Math.max(temperature, idealLength / 4);
    }

    /// Holds a node at a centre position for the next steps; the view calls it
    /// while the user drags the node.
    public void hold(String nodeId, double centerX, double centerY) {
        if (graph == null) {
            return;
        }
        for (int i = 0; i < graph.size(); i++) {
            if (graph.nodes[i].getId().equals(nodeId)) {
                px[i] = centerX;
                py[i] = centerY;
                return;
            }
        }
    }

    /// The positions the run has reached.
    public ArrangementResult result() {
        ArrangementResult r = new ArrangementResult();
        if (graph == null) {
            return r;
        }
        for (int i = 0; i < graph.size(); i++) {
            GraphNode node = graph.nodes[i];
            if (node.isPinned()) {
                r.setPosition(node.getId(), node.getX(), node.getY());
            } else {
                r.setPosition(node.getId(), px[i] - node.getWidth() / 2, py[i] - node.getHeight() / 2);
            }
        }
        return r;
    }

    /// Pushes apart nodes that still overlap once the forces have settled,
    /// along the axis where they overlap least. Neighbours are found through
    /// a grid of cells as large as the largest node, so a pass is linear.
    public void removeOverlaps() {
        if (graph == null) {
            return;
        }
        int n = graph.size();
        double cell = 1;
        for (int i = 0; i < n; i++) {
            cell = Math.max(cell, Math.max(graph.nodes[i].getWidth(), graph.nodes[i].getHeight()));
        }
        double pad = idealLength / 8;
        cell += pad;
        java.util.Map<Long, java.util.List<Integer>> grid = new java.util.HashMap<Long, java.util.List<Integer>>();
        for (int pass = 0; pass < 60; pass++) {
            grid.clear();
            for (int i = 0; i < n; i++) {
                Long key = cellKey((long) Math.floor(px[i] / cell), (long) Math.floor(py[i] / cell));
                java.util.List<Integer> l = grid.get(key);
                if (l == null) {
                    l = new java.util.ArrayList<Integer>();
                    grid.put(key, l);
                }
                l.add(Integer.valueOf(i));
            }
            boolean moved = false;
            for (int i = 0; i < n; i++) {
                long gx = (long) Math.floor(px[i] / cell);
                long gy = (long) Math.floor(py[i] / cell);
                for (long ox = -1; ox <= 1; ox++) {
                    for (long oy = -1; oy <= 1; oy++) {
                        java.util.List<Integer> l = grid.get(cellKey(gx + ox, gy + oy));
                        if (l == null) {
                            continue;
                        }
                        for (int li = 0; li < l.size(); li++) {
                            int j = l.get(li).intValue();
                            if (j > i && separate(graph.nodes, i, j, pad)) {
                                moved = true;
                            }
                        }
                    }
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    private static Long cellKey(long x, long y) {
        return Long.valueOf((x << 32) ^ (y & 0xffffffffL));
    }

    private boolean separate(GraphNode[] nodes, int i, int j, double pad) {
        GraphNode a = nodes[i];
        GraphNode b = nodes[j];
        double ox = (a.getWidth() + b.getWidth()) / 2 + pad - Math.abs(px[i] - px[j]);
        double oy = (a.getHeight() + b.getHeight()) / 2 + pad - Math.abs(py[i] - py[j]);
        if (ox <= 0 || oy <= 0) {
            return false;
        }
        boolean pa = a.isPinned();
        boolean pb = b.isPinned();
        if (pa && pb) {
            return false;
        }
        double wa = pa ? 0 : pb ? 1 : 0.5;
        double wb = 1 - wa;
        if (ox < oy) {
            double s = px[i] < px[j] || px[i] == px[j] && i < j ? -1 : 1;
            px[i] += s * ox * wa;
            px[j] -= s * ox * wb;
        } else {
            double s = py[i] < py[j] || py[i] == py[j] && i < j ? -1 : 1;
            py[i] += s * oy * wa;
            py[j] -= s * oy * wb;
        }
        return true;
    }

    // ---- Barnes-Hut ----

    private static final class Quad {
        double x0;
        double y0;
        double size;
        double mass;
        double cx;
        double cy;
        int body = -1;
        Quad[] kids;
    }

    private Quad newQuad(double x0, double y0, double size) {
        Quad q = new Quad();
        q.x0 = x0;
        q.y0 = y0;
        q.size = size;
        return q;
    }

    private Quad buildTree(int n) {
        double minX = px[0];
        double minY = py[0];
        double maxX = minX;
        double maxY = minY;
        for (int i = 1; i < n; i++) {
            minX = Math.min(minX, px[i]);
            minY = Math.min(minY, py[i]);
            maxX = Math.max(maxX, px[i]);
            maxY = Math.max(maxY, py[i]);
        }
        double size = Math.max(maxX - minX, maxY - minY) + 1;
        Quad root = newQuad(minX, minY, size);
        for (int i = 0; i < n; i++) {
            insert(root, i, 0);
        }
        return root;
    }

    private void insert(Quad q, int i, int depth) {
        double m = q.mass;
        q.cx = (q.cx * m + px[i]) / (m + 1);
        q.cy = (q.cy * m + py[i]) / (m + 1);
        q.mass = m + 1;
        if (m == 0) {
            q.body = i;
            return;
        }
        if (depth > 40) {
            // coincident points: keep them in one leaf
            return;
        }
        if (q.kids == null) {
            q.kids = new Quad[4];
            int old = q.body;
            q.body = -1;
            if (old >= 0) {
                place(q, old, depth);
            }
        }
        place(q, i, depth);
    }

    private void place(Quad q, int i, int depth) {
        double h = q.size / 2;
        int qx = px[i] >= q.x0 + h ? 1 : 0;
        int qy = py[i] >= q.y0 + h ? 1 : 0;
        int idx = qy * 2 + qx;
        if (q.kids[idx] == null) {
            q.kids[idx] = newQuad(q.x0 + qx * h, q.y0 + qy * h, h);
        }
        insert(q.kids[idx], i, depth + 1);
    }

    private void repel(Quad q, int i, double k2) {
        if (q == null || q.mass == 0 || q.body == i && q.kids == null) {
            return;
        }
        double ex = px[i] - q.cx;
        double ey = py[i] - q.cy;
        double d2 = ex * ex + ey * ey;
        if (q.kids == null || q.size * q.size < theta * theta * d2) {
            if (d2 < 0.0001) {
                // two nodes on one spot: push apart along a fixed direction
                ex = (i % 7) - 3 + 0.5;
                ey = (i % 5) - 2 + 0.5;
                d2 = ex * ex + ey * ey;
            }
            double f = k2 * q.mass / d2;
            dx[i] += ex * f;
            dy[i] += ey * f;
            return;
        }
        for (int j = 0; j < 4; j++) {
            repel(q.kids[j], i, k2);
        }
    }
}
