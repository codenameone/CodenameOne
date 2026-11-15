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

/// A tree on rings: the root at the centre and each level on a circle around
/// it, every subtree in a wedge as wide as its share of the leaves. Suits a
/// mind map, or the neighbourhood of one node in a large network.
///
/// The tree is found the way `TreeArrangement` finds it. Rings grow apart as far
/// as the outermost needs to fit its nodes without overlapping.
public class RadialArrangement implements GraphArrangement {
    private double ringGap = 140;
    private String rootId;

    /// The least distance between rings in world units.
    public double getRingGap() {
        return ringGap;
    }

    /// Sets the least distance between rings.
    public void setRingGap(double ringGap) {
        this.ringGap = ringGap;
    }

    /// The id of the node at the centre, or null for the first node no edge
    /// enters.
    public String getRootId() {
        return rootId;
    }

    /// Sets the node at the centre.
    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    @Override
    public ArrangementResult arrange(GraphModel model) {
        Forest f = new Forest(model, rootId);
        int n = f.graph.size();
        ArrangementResult r = new ArrangementResult();
        if (n == 0) {
            return r;
        }
        // several roots hang off an invisible centre
        int[] leaves = new int[n];
        int[] order = postOrder(f);
        double maxSize = 0;
        for (int oi = 0; oi < order.length; oi++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            int v = order[oi];
            int[] kids = f.children[v];
            if (kids.length == 0) {
                leaves[v] = 1;
            } else {
                for (int i = 0; i < kids.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                    leaves[v] += leaves[kids[i]];
                }
            }
            GraphNode node = f.graph.nodes[v];
            maxSize = Math.max(maxSize, Math.max(node.getWidth(), node.getHeight()));
        }
        int totalLeaves = 0;
        for (int i = 0; i < f.roots.length; i++) {
            totalLeaves += leaves[f.roots[i]];
        }
        boolean single = f.roots.length == 1;
        int depthShift = single ? 0 : 1;
        double[] a0 = new double[n];
        double[] a1 = new double[n];
        double at = 0;
        for (int i = 0; i < f.roots.length; i++) {
            int root = f.roots[i];
            double span = 2 * Math.PI * leaves[root] / totalLeaves;
            a0[root] = at;
            a1[root] = at + span;
            at += span;
        }
        // first pass: give out the wedges, and find the ring gap at which every
        // node fits the arc of its own wedge; wedges never overlap, so neither do nodes
        double gap = Math.max(ringGap, maxSize * 1.5);
        for (int oi = order.length - 1; oi >= 0; oi--) {
            int v = order[oi];
            int[] kids = f.children[v];
            double cur = a0[v];
            for (int i = 0; i < kids.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                int c = kids[i];
                double span = (a1[v] - a0[v]) * leaves[c] / leaves[v];
                a0[c] = cur;
                a1[c] = cur + span;
                cur += span;
            }
            int ring = f.depth[v] + depthShift;
            double wedge = a1[v] - a0[v];
            if (ring > 0 && wedge < Math.PI) {
                GraphNode node = f.graph.nodes[v];
                double diag = Math.sqrt(node.getWidth() * node.getWidth() + node.getHeight() * node.getHeight());
                // the chord of the wedge at radius R is 2 R sin(wedge / 2)
                gap = Math.max(gap, diag * 1.1 / (2 * Math.sin(wedge / 2)) / ring);
            }
        }
        for (int oi = order.length - 1; oi >= 0; oi--) {
            int v = order[oi];
            int[] kids = f.children[v];
            double cur = a0[v];
            for (int i = 0; i < kids.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                int c = kids[i];
                double span = (a1[v] - a0[v]) * leaves[c] / leaves[v];
                a0[c] = cur;
                a1[c] = cur + span;
                cur += span;
            }
            GraphNode node = f.graph.nodes[v];
            int ring = f.depth[v] + depthShift;
            double cx;
            double cy;
            if (ring == 0) {
                cx = 0;
                cy = 0;
            } else {
                double a = (a0[v] + a1[v]) / 2 - Math.PI / 2;
                cx = Math.cos(a) * ring * gap;
                cy = Math.sin(a) * ring * gap;
            }
            r.setPosition(node.getId(), cx - node.getWidth() / 2, cy - node.getHeight() / 2);
        }
        return r;
    }

    private static int[] postOrder(Forest f) {
        int n = f.graph.size();
        int[] out = new int[n];
        int k = 0;
        int[] stack = new int[n];
        int[] next = new int[n];
        for (int ri = 0; ri < f.roots.length; ri++) {
            int top = 0;
            stack[0] = f.roots[ri];
            next[0] = 0;
            while (top >= 0) {
                int v = stack[top];
                if (next[top] < f.children[v].length) {
                    int c = f.children[v][next[top]++];
                    top++;
                    stack[top] = c;
                    next[top] = 0;
                } else {
                    out[k++] = v;
                    top--;
                }
            }
        }
        return out;
    }
}
