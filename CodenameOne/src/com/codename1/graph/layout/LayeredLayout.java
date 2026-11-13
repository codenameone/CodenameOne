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
package com.codename1.graph.layout;

import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A layout for flows: every edge points the same way, from one layer to the
/// next, with as few crossings as it can find.
///
/// This is the method of Sugiyama, Tagawa and Toda in four passes. Cycles are
/// broken by reversing the edges a depth first search finds going back;
/// nodes are put on layers by their longest path from a source; an edge that
/// spans several layers gets a hidden point on each so it bends around the
/// nodes between; the order inside each layer is improved by sweeping down
/// and up, moving each node to the median of its neighbours; and each node is
/// then pulled towards its neighbours without overlapping the next one.
///
/// The edges get routes through their hidden points. Pinned nodes are placed
/// like any other: a layered picture has no room for a node that stays put.
public class LayeredLayout implements GraphLayout {
    /// Layers run down the screen and edges point down.
    public static final int TOP_TO_BOTTOM = 0;

    /// Layers run across the screen and edges point right.
    public static final int LEFT_TO_RIGHT = 1;

    private int direction = TOP_TO_BOTTOM;
    private double layerGap = 80;
    private double nodeGap = 40;
    private int sweeps = 12;

    /// `TOP_TO_BOTTOM`, the default, or `LEFT_TO_RIGHT`.
    public int getDirection() {
        return direction;
    }

    /// Sets the direction edges point.
    public void setDirection(int direction) {
        this.direction = direction;
    }

    /// The space between layers in world units.
    public double getLayerGap() {
        return layerGap;
    }

    /// Sets the space between layers.
    public void setLayerGap(double layerGap) {
        this.layerGap = layerGap;
    }

    /// The space between nodes on one layer in world units.
    public double getNodeGap() {
        return nodeGap;
    }

    /// Sets the space between nodes on one layer.
    public void setNodeGap(double nodeGap) {
        this.nodeGap = nodeGap;
    }

    @Override
    public LayoutResult layout(GraphModel model) {
        Graphs g = new Graphs(model);
        int n = g.size();
        LayoutResult result = new LayoutResult();
        if (n == 0) {
            return result;
        }
        boolean[][] reversed = breakCycles(g);
        // forward successor lists after reversal
        List<List<Integer>> succ = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) {
            succ.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < g.out[i].length; j++) {
                int t = g.out[i][j];
                if (reversed[i][j]) {
                    succ.get(t).add(Integer.valueOf(i));
                } else {
                    succ.get(i).add(Integer.valueOf(t));
                }
            }
        }
        int[] layer = longestPath(n, succ);

        // vertices: real nodes then hidden points; chains record each edge's points
        List<Integer> vLayer = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            vLayer.add(Integer.valueOf(layer[i]));
        }
        List<List<Integer>> vSucc = new ArrayList<List<Integer>>();
        List<List<Integer>> vPred = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++) {
            vSucc.add(new ArrayList<Integer>());
            vPred.add(new ArrayList<Integer>());
        }
        Map<Long, int[]> chains = new HashMap<Long, int[]>();
        for (int i = 0; i < n; i++) {
            List<Integer> s = succ.get(i);
            for (int j = 0; j < s.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                int t = s.get(j).intValue();
                int span = layer[t] - layer[i];
                int prev = i;
                int[] chain = new int[Math.max(0, span - 1)];
                for (int l = 1; l < span; l++) {
                    int v = vLayer.size();
                    vLayer.add(Integer.valueOf(layer[i] + l));
                    vSucc.add(new ArrayList<Integer>());
                    vPred.add(new ArrayList<Integer>());
                    vSucc.get(prev).add(Integer.valueOf(v));
                    vPred.get(v).add(Integer.valueOf(prev));
                    chain[l - 1] = v;
                    prev = v;
                }
                vSucc.get(prev).add(Integer.valueOf(t));
                vPred.get(t).add(Integer.valueOf(prev));
                chains.put(key(i, t), chain);
            }
        }
        int vn = vLayer.size();
        int layers = 0;
        for (int v = 0; v < vn; v++) {
            layers = Math.max(layers, vLayer.get(v).intValue() + 1);
        }
        int[][] order = initialOrder(vn, layers, vLayer, vSucc, vPred);
        int[] pos = new int[vn];
        reduceCrossings(order, pos, vSucc, vPred);

        boolean tb = direction == TOP_TO_BOTTOM;
        double[] breadth = new double[vn];
        double[] depth = new double[layers];
        for (int v = 0; v < vn; v++) {
            if (v < n) {
                GraphNode node = g.nodes[v];
                breadth[v] = tb ? node.getWidth() : node.getHeight();
                double d = tb ? node.getHeight() : node.getWidth();
                int l = vLayer.get(v).intValue();
                depth[l] = Math.max(depth[l], d);
            } else {
                breadth[v] = 0;
            }
        }
        double[] center = assignCoordinates(order, breadth, vSucc, vPred);
        double[] layerStart = new double[layers];
        double at = 0;
        for (int l = 0; l < layers; l++) {
            layerStart[l] = at;
            at += depth[l] + layerGap;
        }
        for (int i = 0; i < n; i++) {
            GraphNode node = g.nodes[i];
            int l = layer[i];
            double c = center[i];
            double along = layerStart[l] + (depth[l] - (tb ? node.getHeight() : node.getWidth())) / 2;
            if (tb) {
                result.setPosition(node.getId(), c - node.getWidth() / 2, along);
            } else {
                result.setPosition(node.getId(), along, c - node.getHeight() / 2);
            }
        }
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < n; i++) {
            index.put(g.nodes[i].getId(), Integer.valueOf(i));
        }
        for (GraphEdge e : model.getEdges()) {
            Integer s = index.get(e.getSourceId());
            Integer t = index.get(e.getTargetId());
            if (s == null || t == null) {
                continue;
            }
            int[] chain = chains.get(key(s.intValue(), t.intValue()));
            boolean back = false;
            if (chain == null) {
                chain = chains.get(key(t.intValue(), s.intValue()));
                back = true;
            }
            if (chain == null || chain.length == 0) {
                continue;
            }
            double[] route = new double[chain.length * 2];
            for (int c = 0; c < chain.length; c++) {
                int v = chain[back ? chain.length - 1 - c : c];
                int l = vLayer.get(v).intValue();
                double along = layerStart[l] + depth[l] / 2;
                route[c * 2] = tb ? center[v] : along;
                route[c * 2 + 1] = tb ? along : center[v];
            }
            result.setRoute(e.getId(), route);
        }
        return result;
    }

    private static Long key(int a, int b) {
        return Long.valueOf(((long) a << 32) | (b & 0xffffffffL));
    }

    /// Marks the edges an iterative depth first search meets going back to a
    /// node still on its stack.
    private static boolean[][] breakCycles(Graphs g) {
        int n = g.size();
        boolean[][] rev = new boolean[n][];
        for (int i = 0; i < n; i++) {
            rev[i] = new boolean[g.out[i].length];
        }
        int[] state = new int[n];
        int[] stack = new int[n];
        int[] edgeAt = new int[n];
        // start from sources first so the natural direction wins
        int[] starts = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (g.in[i].length == 0) {
                starts[k++] = i;
            }
        }
        for (int i = 0; i < n; i++) {
            if (g.in[i].length != 0) {
                starts[k++] = i;
            }
        }
        for (int si = 0; si < n; si++) {
            int s = starts[si];
            if (state[s] != 0) {
                continue;
            }
            int top = 0;
            stack[0] = s;
            edgeAt[0] = 0;
            state[s] = 1;
            while (top >= 0) {
                int v = stack[top];
                if (edgeAt[top] < g.out[v].length) {
                    int j = edgeAt[top]++;
                    int t = g.out[v][j];
                    if (state[t] == 1) {
                        rev[v][j] = true;
                    } else if (state[t] == 0) {
                        state[t] = 1;
                        top++;
                        stack[top] = t;
                        edgeAt[top] = 0;
                    }
                } else {
                    state[v] = 2;
                    top--;
                }
            }
        }
        return rev;
    }

    private static int[] longestPath(int n, List<List<Integer>> succ) {
        int[] indeg = new int[n];
        for (int i = 0; i < n; i++) {
            for (Integer t : succ.get(i)) {
                indeg[t.intValue()]++;
            }
        }
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) {
                queue[tail++] = i;
            }
        }
        int[] layer = new int[n];
        while (head < tail) {
            int v = queue[head++];
            for (Integer t : succ.get(v)) {
                int ti = t.intValue();
                layer[ti] = Math.max(layer[ti], layer[v] + 1);
                indeg[ti]--;
                if (indeg[ti] == 0) {
                    queue[tail++] = ti;
                }
            }
        }
        return layer;
    }

    /// Orders each layer by a breadth first walk, which keeps the children of
    /// one node together before any sweep runs.
    private static int[][] initialOrder(int vn, int layers, List<Integer> vLayer, List<List<Integer>> vSucc,
                                        List<List<Integer>> vPred) {
        int[] count = new int[layers];
        int[][] order = new int[layers][];
        for (int v = 0; v < vn; v++) {
            count[vLayer.get(v).intValue()]++;
        }
        for (int l = 0; l < layers; l++) {
            order[l] = new int[count[l]];
            count[l] = 0;
        }
        boolean[] seen = new boolean[vn];
        int[] queue = new int[vn];
        for (int s = 0; s < vn; s++) {
            if (seen[s] || !vPred.get(s).isEmpty()) {
                continue;
            }
            int head = 0;
            int tail = 0;
            queue[tail++] = s;
            seen[s] = true;
            while (head < tail) {
                int v = queue[head++];
                int l = vLayer.get(v).intValue();
                order[l][count[l]++] = v;
                for (Integer t : vSucc.get(v)) {
                    if (!seen[t.intValue()]) {
                        seen[t.intValue()] = true;
                        queue[tail++] = t.intValue();
                    }
                }
            }
        }
        for (int v = 0; v < vn; v++) {
            if (!seen[v]) {
                int l = vLayer.get(v).intValue();
                order[l][count[l]++] = v;
            }
        }
        return order;
    }

    private void reduceCrossings(int[][] order, int[] pos, List<List<Integer>> vSucc, List<List<Integer>> vPred) {
        updatePos(order, pos);
        int best = crossings(order, pos, vSucc);
        int[][] bestOrder = copy(order);
        for (int s = 0; s < sweeps && best > 0; s++) {
            boolean down = s % 2 == 0;
            if (down) {
                for (int l = 1; l < order.length; l++) {
                    sortByMedian(order[l], pos, vPred);
                    updatePos(order, pos);
                }
            } else {
                for (int l = order.length - 2; l >= 0; l--) {
                    sortByMedian(order[l], pos, vSucc);
                    updatePos(order, pos);
                }
            }
            int c = crossings(order, pos, vSucc);
            if (c < best) {
                best = c;
                bestOrder = copy(order);
            }
        }
        for (int l = 0; l < order.length; l++) {
            order[l] = bestOrder[l];
        }
        updatePos(order, pos);
    }

    private static int[][] copy(int[][] a) {
        int[][] c = new int[a.length][];
        for (int i = 0; i < a.length; i++) {
            c[i] = new int[a[i].length];
            System.arraycopy(a[i], 0, c[i], 0, a[i].length);
        }
        return c;
    }

    private static void updatePos(int[][] order, int[] pos) {
        for (int l = 0; l < order.length; l++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            for (int i = 0; i < order[l].length; i++) {
                pos[order[l][i]] = i;
            }
        }
    }

    /// A stable insertion sort of one layer by the median position of each
    /// vertex's neighbours on the fixed layer; a vertex with none keeps its
    /// place.
    private static void sortByMedian(int[] row, int[] pos, List<List<Integer>> adj) {
        int len = row.length;
        double[] m = new double[len];
        for (int i = 0; i < len; i++) {
            List<Integer> nb = adj.get(row[i]);
            if (nb.isEmpty()) {
                m[i] = i;
                continue;
            }
            int[] p = new int[nb.size()];
            for (int j = 0; j < p.length; j++) {
                p[j] = pos[nb.get(j).intValue()];
            }
            java.util.Arrays.sort(p);
            int h = p.length / 2;
            m[i] = p.length % 2 == 1 ? p[h] : (p[h - 1] + p[h]) / 2.0;
        }
        for (int i = 1; i < len; i++) {
            int v = row[i];
            double mv = m[i];
            int j = i - 1;
            while (j >= 0 && m[j] > mv) {
                row[j + 1] = row[j];
                m[j + 1] = m[j];
                j--;
            }
            row[j + 1] = v;
            m[j + 1] = mv;
        }
    }

    /// Counts crossings between consecutive layers by comparing every pair of
    /// edges, which is quadratic per layer pair but trivial at diagram sizes.
    static int crossings(int[][] order, int[] pos, List<List<Integer>> vSucc) {
        int total = 0;
        for (int l = 0; l + 1 < order.length; l++) {
            List<int[]> edges = new ArrayList<int[]>();
            for (int i = 0; i < order[l].length; i++) {
                int v = order[l][i];
                for (Integer t : vSucc.get(v)) {
                    edges.add(new int[]{pos[v], pos[t.intValue()]});
                }
            }
            int m = edges.size();
            for (int a = 0; a < m; a++) {
                int[] ea = edges.get(a);
                for (int b = a + 1; b < m; b++) {
                    int[] eb = edges.get(b);
                    if ((ea[0] - eb[0]) * (ea[1] - eb[1]) < 0) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /// Places vertices on their layer axis: packed in order first, then a few
    /// rounds pulling each towards the mean of its neighbours, each round
    /// followed by a sweep that pushes overlapping vertices apart.
    private double[] assignCoordinates(int[][] order, double[] breadth, List<List<Integer>> vSucc,
                                       List<List<Integer>> vPred) {
        int vn = breadth.length;
        double[] c = new double[vn];
        for (int l = 0; l < order.length; l++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            double at = 0;
            for (int i = 0; i < order[l].length; i++) {
                int v = order[l][i];
                c[v] = at + breadth[v] / 2;
                at += breadth[v] + gapFor(breadth[v]);
            }
        }
        for (int round = 0; round < 8; round++) {
            boolean down = round % 2 == 0;
            for (int li = 0; li < order.length; li++) {
                int l = down ? li : order.length - 1 - li;
                int[] row = order[l];
                double[] want = new double[row.length];
                for (int i = 0; i < row.length; i++) {
                    int v = row[i];
                    List<Integer> nb = down ? vPred.get(v) : vSucc.get(v);
                    if (nb.isEmpty()) {
                        want[i] = c[v];
                    } else {
                        double sum = 0;
                        for (Integer u : nb) {
                            sum += c[u.intValue()];
                        }
                        want[i] = sum / nb.size();
                    }
                }
                // push apart left to right, then right to left, and average
                double[] left = new double[row.length];
                double[] right = new double[row.length];
                for (int i = 0; i < row.length; i++) {
                    left[i] = want[i];
                    if (i > 0) {
                        double min = left[i - 1] + (breadth[row[i - 1]] + breadth[row[i]]) / 2 + gapFor(breadth[row[i]]);
                        if (left[i] < min) {
                            left[i] = min;
                        }
                    }
                }
                for (int i = row.length - 1; i >= 0; i--) {
                    right[i] = want[i];
                    if (i < row.length - 1) {
                        double max = right[i + 1] - (breadth[row[i + 1]] + breadth[row[i]]) / 2 - gapFor(breadth[row[i]]);
                        if (right[i] > max) {
                            right[i] = max;
                        }
                    }
                }
                for (int i = 0; i < row.length; i++) {
                    c[row[i]] = (left[i] + right[i]) / 2;
                }
                // averaging can leave overlaps; one more left to right pass removes them
                for (int i = 1; i < row.length; i++) {
                    double min = c[row[i - 1]] + (breadth[row[i - 1]] + breadth[row[i]]) / 2 + gapFor(breadth[row[i]]);
                    if (c[row[i]] < min) {
                        c[row[i]] = min;
                    }
                }
            }
        }
        double minC = Double.MAX_VALUE;
        for (int v = 0; v < vn; v++) {
            minC = Math.min(minC, c[v] - breadth[v] / 2);
        }
        for (int v = 0; v < vn; v++) {
            c[v] -= minC;
        }
        return c;
    }

    private double gapFor(double breadth) {
        // hidden points need far less room than nodes
        return breadth == 0 ? nodeGap / 4 : nodeGap;
    }
}
