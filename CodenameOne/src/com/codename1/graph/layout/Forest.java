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

import com.codename1.graph.GraphModel;

/// A spanning forest of a graph: the tree and radial layouts place nodes by
/// it and draw the edges it leaves out wherever their ends land.
final class Forest {
    final Graphs graph;
    final int[] parent;
    final int[][] children;
    final int[] depth;
    final int[] roots;

    /// Builds the forest breadth first from `rootId` when given and present,
    /// then from every node no edge enters, then from whatever is left, so
    /// cycles and unreachable parts still get a place.
    Forest(GraphModel model, String rootId) {
        graph = new Graphs(model);
        int n = graph.size();
        parent = new int[n];
        depth = new int[n];
        int[] childCount = new int[n];
        boolean[] seen = new boolean[n];
        int[] queue = new int[n];
        int[] rootTmp = new int[n];
        int rootCount = 0;
        for (int i = 0; i < n; i++) {
            parent[i] = -1;
        }
        int[] starts = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (rootId != null && rootId.equals(graph.nodes[i].getId())) {
                starts[k++] = i;
            }
        }
        for (int i = 0; i < n; i++) {
            if (graph.in[i].length == 0 && (k == 0 || starts[0] != i)) {
                starts[k++] = i;
            }
        }
        for (int i = 0; i < n; i++) {
            if (graph.in[i].length != 0 && (rootId == null || !rootId.equals(graph.nodes[i].getId()))) {
                starts[k++] = i;
            }
        }
        for (int si = 0; si < k; si++) {
            int s = starts[si];
            if (seen[s]) {
                continue;
            }
            seen[s] = true;
            rootTmp[rootCount++] = s;
            int head = 0;
            int tail = 0;
            queue[tail++] = s;
            while (head < tail) {
                int v = queue[head++];
                int[] out = graph.out[v];
                for (int j = 0; j < out.length; j++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
                    int t = out[j];
                    if (!seen[t]) {
                        seen[t] = true;
                        parent[t] = v;
                        depth[t] = depth[v] + 1;
                        childCount[v]++;
                        queue[tail++] = t;
                    }
                }
            }
        }
        children = new int[n][];
        for (int i = 0; i < n; i++) {
            children[i] = new int[childCount[i]];
            childCount[i] = 0;
        }
        // children in model order keeps the picture stable as nodes are added
        for (int i = 0; i < n; i++) {
            if (parent[i] >= 0) {
                children[parent[i]][childCount[parent[i]]++] = i;
            }
        }
        roots = new int[rootCount];
        System.arraycopy(rootTmp, 0, roots, 0, rootCount);
    }

    int maxDepth() {
        int m = 0;
        for (int i = 0; i < depth.length; i++) { // NOPMD ForLoopCanBeForeach - indexed, like the rest of the pass
            m = Math.max(m, depth[i]);
        }
        return m;
    }
}
