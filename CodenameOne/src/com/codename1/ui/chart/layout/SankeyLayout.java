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
package com.codename1.ui.chart.layout;

/// Positions the nodes and links of a sankey diagram.
///
/// Nodes go into columns by their longest path from a source, with sinks pushed to the last
/// column. Heights are proportional to the larger of a node's inflow and outflow, using one
/// scale for every column. Vertical positions are then relaxed towards the weighted centre of
/// each node's neighbours, which shortens and straightens links, and overlaps are pushed apart.
public final class SankeyLayout {
    private int[] column;
    private float[] nodeY;
    private float[] nodeH;
    private float[] nodeX;
    private float[] linkSy;
    private float[] linkTy;
    private float[] linkW;
    private int columns;

    /// Computes a layout.
    ///
    /// #### Parameters
    ///
    /// - `nodeCount`: the number of nodes
    ///
    /// - `src`: source node of each link
    ///
    /// - `dst`: target node of each link
    ///
    /// - `value`: value of each link
    ///
    /// - `x`: the area's left
    ///
    /// - `y`: the area's top
    ///
    /// - `w`: the area's width
    ///
    /// - `h`: the area's height
    ///
    /// - `nodeWidth`: the width of a node bar
    ///
    /// - `nodePad`: the vertical gap between nodes of one column
    public SankeyLayout(int nodeCount, int[] src, int[] dst, double[] value, float x, float y, float w, float h,
            float nodeWidth, float nodePad) {
        int n = nodeCount;
        int m = src.length;
        column = new int[n];
        // longest path from a source; the iteration bound also stops cycles
        for (int pass = 0; pass < n; pass++) {
            boolean changed = false;
            for (int l = 0; l < m; l++) {
                if (src[l] != dst[l] && column[dst[l]] < column[src[l]] + 1 && column[src[l]] + 1 < n) {
                    column[dst[l]] = column[src[l]] + 1;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        int maxCol = 0;
        for (int i = 0; i < n; i++) {
            maxCol = Math.max(maxCol, column[i]);
        }
        boolean[] hasOut = new boolean[n];
        for (int l = 0; l < m; l++) {
            hasOut[src[l]] = true;
        }
        for (int i = 0; i < n; i++) {
            if (!hasOut[i]) {
                column[i] = maxCol;
            }
        }
        columns = maxCol + 1;
        double[] in = new double[n];
        double[] out = new double[n];
        for (int l = 0; l < m; l++) {
            out[src[l]] += Math.max(0, value[l]);
            in[dst[l]] += Math.max(0, value[l]);
        }
        double[] nv = new double[n];
        for (int i = 0; i < n; i++) {
            nv[i] = Math.max(in[i], out[i]);
        }
        double ky = Double.MAX_VALUE;
        for (int c = 0; c < columns; c++) {
            double sum = 0;
            int count = 0;
            for (int i = 0; i < n; i++) {
                if (column[i] == c) {
                    sum += nv[i];
                    count++;
                }
            }
            if (sum > 0) {
                ky = Math.min(ky, (h - nodePad * Math.max(0, count - 1)) / sum);
            }
        }
        if (ky == Double.MAX_VALUE || ky < 0) {
            ky = 0;
        }
        nodeX = new float[n];
        nodeY = new float[n];
        nodeH = new float[n];
        float colStep = columns > 1 ? (w - nodeWidth) / (columns - 1) : 0;
        for (int c = 0; c < columns; c++) {
            float cy = y;
            for (int i = 0; i < n; i++) {
                if (column[i] == c) {
                    nodeX[i] = x + c * colStep;
                    nodeH[i] = (float) Math.max(1, nv[i] * ky);
                    nodeY[i] = cy;
                    cy += nodeH[i] + nodePad;
                }
            }
        }
        float alpha = 1;
        for (int iter = 0; iter < 8; iter++) {
            alpha *= 0.99f;
            relax(src, dst, value, n, true, alpha);
            resolve(n, y, h, nodePad);
            relax(src, dst, value, n, false, alpha);
            resolve(n, y, h, nodePad);
        }
        linkSy = new float[m];
        linkTy = new float[m];
        linkW = new float[m];
        for (int l = 0; l < m; l++) {
            linkW[l] = (float) (Math.max(0, value[l]) * ky);
        }
        // order links by the far end's position so ribbons do not cross at their ends
        float[] outOff = new float[n];
        float[] inOff = new float[n];
        Integer[] order = new Integer[m];
        for (int l = 0; l < m; l++) {
            order[l] = Integer.valueOf(l);
        }
        sortBy(order, dst);
        for (int k = 0; k < m; k++) {
            int l = order[k].intValue();
            linkSy[l] = nodeY[src[l]] + outOff[src[l]];
            outOff[src[l]] += linkW[l];
        }
        sortBy(order, src);
        for (int k = 0; k < m; k++) {
            int l = order[k].intValue();
            linkTy[l] = nodeY[dst[l]] + inOff[dst[l]];
            inOff[dst[l]] += linkW[l];
        }
    }

    /// The number of columns.
    public int getColumns() {
        return columns;
    }

    /// The column of node `i`.
    public int getColumn(int i) {
        return column[i];
    }

    /// The left of node `i`.
    public float getNodeX(int i) {
        return nodeX[i];
    }

    /// The top of node `i`.
    public float getNodeY(int i) {
        return nodeY[i];
    }

    /// The height of node `i`.
    public float getNodeHeight(int i) {
        return nodeH[i];
    }

    /// The top of link `l` where it leaves its source.
    public float getLinkSourceY(int l) {
        return linkSy[l];
    }

    /// The top of link `l` where it enters its target.
    public float getLinkTargetY(int l) {
        return linkTy[l];
    }

    /// The thickness of link `l`.
    public float getLinkWidth(int l) {
        return linkW[l];
    }

    private void sortBy(Integer[] order, int[] end) {
        for (int a = 1; a < order.length; a++) {
            for (int b = a; b > 0 && nodeY[end[order[b].intValue()]] < nodeY[end[order[b - 1].intValue()]]; b--) {
                Integer t = order[b];
                order[b] = order[b - 1];
                order[b - 1] = t;
            }
        }
    }

    private void relax(int[] src, int[] dst, double[] value, int n, boolean forward, float alpha) {
        for (int i = 0; i < n; i++) {
            double sum = 0;
            double wsum = 0;
            for (int l = 0; l < src.length; l++) {
                int me = forward ? dst[l] : src[l];
                int other = forward ? src[l] : dst[l];
                if (me != i || other == i) {
                    continue;
                }
                double v = Math.max(0, value[l]);
                sum += (nodeY[other] + nodeH[other] / 2) * v;
                wsum += v;
            }
            if (wsum > 0) {
                float target = (float) (sum / wsum) - nodeH[i] / 2;
                nodeY[i] += (target - nodeY[i]) * alpha;
            }
        }
    }

    private void resolve(int n, float y, float h, float pad) {
        for (int c = 0; c < columns; c++) {
            int count = 0;
            int[] idx = new int[n];
            for (int i = 0; i < n; i++) {
                if (column[i] == c) {
                    idx[count++] = i;
                }
            }
            for (int a = 1; a < count; a++) {
                for (int b = a; b > 0 && nodeY[idx[b]] < nodeY[idx[b - 1]]; b--) {
                    int t = idx[b];
                    idx[b] = idx[b - 1];
                    idx[b - 1] = t;
                }
            }
            float cur = y;
            for (int k = 0; k < count; k++) {
                int i = idx[k];
                if (nodeY[i] < cur) {
                    nodeY[i] = cur;
                }
                cur = nodeY[i] + nodeH[i] + pad;
            }
            float bottom = y + h;
            for (int k = count - 1; k >= 0; k--) {
                int i = idx[k];
                if (nodeY[i] + nodeH[i] > bottom) {
                    nodeY[i] = bottom - nodeH[i];
                }
                bottom = nodeY[i] - pad;
            }
        }
    }
}
