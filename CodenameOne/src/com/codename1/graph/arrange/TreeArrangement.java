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

/// An arrangement for hierarchies, such as an org chart or a file tree: every node
/// below its parent, parents centred over their children, and no subtree
/// overlapping another.
///
/// A graph that is not a tree is laid out by a spanning tree found breadth
/// first from the root (`setRootId`, or every node no edge enters); the other
/// edges are drawn straight between wherever their ends land. Several roots
/// make a forest, laid out side by side.
public class TreeArrangement implements GraphArrangement {
    /// Children below their parent.
    public static final int TOP_TO_BOTTOM = 0;

    /// Children to the right of their parent, as in a mind map or file tree.
    public static final int LEFT_TO_RIGHT = 1;

    private int direction = TOP_TO_BOTTOM;
    private double levelGap = 60;
    private double siblingGap = 24;
    private String rootId;

    /// `TOP_TO_BOTTOM`, the default, or `LEFT_TO_RIGHT`.
    public int getDirection() {
        return direction;
    }

    /// Sets the direction the tree grows.
    public void setDirection(int direction) {
        this.direction = direction;
    }

    /// The space between a parent and its children in world units.
    public double getLevelGap() {
        return levelGap;
    }

    /// Sets the space between levels.
    public void setLevelGap(double levelGap) {
        this.levelGap = levelGap;
    }

    /// The space between neighbouring subtrees in world units.
    public double getSiblingGap() {
        return siblingGap;
    }

    /// Sets the space between neighbouring subtrees.
    public void setSiblingGap(double siblingGap) {
        this.siblingGap = siblingGap;
    }

    /// The id of the root, or null to start from every node no edge enters.
    public String getRootId() {
        return rootId;
    }

    /// Sets the root.
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
        boolean tb = direction == TOP_TO_BOTTOM;
        int levels = f.maxDepth() + 1;
        double[] levelDepth = new double[levels];
        for (int i = 0; i < n; i++) {
            GraphNode node = f.graph.nodes[i];
            levelDepth[f.depth[i]] = Math.max(levelDepth[f.depth[i]], tb ? node.getHeight() : node.getWidth());
        }
        double[] levelStart = new double[levels];
        double at = 0;
        for (int l = 0; l < levels; l++) {
            levelStart[l] = at;
            at += levelDepth[l] + levelGap;
        }
        double[] center = new double[n];
        double cursor = 0;
        for (int i = 0; i < f.roots.length; i++) {
            cursor = place(f, f.roots[i], cursor, center, tb) + siblingGap * 2;
        }
        for (int i = 0; i < n; i++) {
            GraphNode node = f.graph.nodes[i];
            double along = levelStart[f.depth[i]];
            if (tb) {
                r.setPosition(node.getId(), center[i] - node.getWidth() / 2, along);
            } else {
                r.setPosition(node.getId(), along, center[i] - node.getHeight() / 2);
            }
        }
        return r;
    }

    /// Places a subtree starting at `start` on the breadth axis, iteratively
    /// so a deep chain cannot overflow the stack.
    ///
    /// #### Returns
    ///
    /// where the subtree ends
    private double place(Forest f, int root, double start, double[] center, boolean tb) {
        int n = center.length;
        double[] left = new double[n];
        double[] right = new double[n];
        int[] stack = new int[n];
        int[] next = new int[n];
        double[] cursor = new double[n];
        double[] startAt = new double[n];
        int top = 0;
        stack[0] = root;
        next[0] = 0;
        cursor[0] = start;
        startAt[0] = start;
        while (top >= 0) {
            int v = stack[top];
            int[] kids = f.children[v];
            if (next[top] < kids.length) {
                int c = kids[next[top]++];
                double childStart = cursor[top];
                top++;
                stack[top] = c;
                next[top] = 0;
                cursor[top] = childStart;
                startAt[top] = childStart;
                continue;
            }
            GraphNode node = f.graph.nodes[v];
            double b = tb ? node.getWidth() : node.getHeight();
            double subStart = startAt[top];
            double c;
            double end;
            if (kids.length == 0) {
                c = subStart + b / 2;
                end = subStart + b;
            } else {
                double first = center[kids[0]];
                double last = center[kids[kids.length - 1]];
                c = (first + last) / 2;
                double kidsRight = right[kids[kids.length - 1]];
                // a wide parent pushes its children right
                double mine = c - b / 2;
                if (mine < subStart) {
                    double shift = subStart - mine;
                    shift(f, v, shift, center, left, right);
                    c += shift;
                    kidsRight += shift;
                }
                end = Math.max(kidsRight, c + b / 2);
            }
            center[v] = c;
            left[v] = Math.min(c - b / 2, subStart);
            right[v] = end;
            top--;
            if (top >= 0) {
                cursor[top] = end + siblingGap;
            }
        }
        return right[root];
    }

    private static void shift(Forest f, int v, double by, double[] center, double[] left, double[] right) {
        int[] kids = f.children[v];
        int[] stack = new int[center.length];
        int top = -1;
        for (int i = 0; i < kids.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            stack[++top] = kids[i];
        }
        while (top >= 0) {
            int u = stack[top--];
            center[u] += by;
            left[u] += by;
            right[u] += by;
            int[] k = f.children[u];
            for (int i = 0; i < k.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                stack[++top] = k[i];
            }
        }
    }
}
