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
import java.util.List;
import java.util.Map;

/// The graph as index arrays, which every arrangement walks many times.
final class Graphs {
    final GraphNode[] nodes;
    /// Successor indexes per node, deduplicated, self loops dropped.
    final int[][] out;
    /// Predecessor indexes per node.
    final int[][] in;

    Graphs(GraphModel model) {
        List<GraphNode> list = model.getNodes();
        int n = list.size();
        nodes = new GraphNode[n];
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < n; i++) {
            nodes[i] = list.get(i);
            index.put(nodes[i].getId(), Integer.valueOf(i));
        }
        int[] outCount = new int[n];
        int[] inCount = new int[n];
        List<GraphEdge> edges = model.getEdges();
        int m = edges.size();
        int[] from = new int[m];
        int[] to = new int[m];
        int k = 0;
        for (int i = 0; i < m; i++) {
            GraphEdge e = edges.get(i);
            Integer s = index.get(e.getSourceId());
            Integer t = index.get(e.getTargetId());
            if (s == null || t == null || s.intValue() == t.intValue()) {
                continue;
            }
            from[k] = s.intValue();
            to[k] = t.intValue();
            k++;
        }
        boolean[] dup = new boolean[k];
        Map<Long, Boolean> seen = new HashMap<Long, Boolean>();
        for (int i = 0; i < k; i++) {
            Long key = Long.valueOf(((long) from[i] << 32) | (to[i] & 0xffffffffL));
            if (seen.put(key, Boolean.TRUE) != null) {
                dup[i] = true;
            } else {
                outCount[from[i]]++;
                inCount[to[i]]++;
            }
        }
        out = new int[n][];
        in = new int[n][];
        for (int i = 0; i < n; i++) {
            out[i] = new int[outCount[i]];
            in[i] = new int[inCount[i]];
            outCount[i] = 0;
            inCount[i] = 0;
        }
        for (int i = 0; i < k; i++) {
            if (!dup[i]) {
                out[from[i]][outCount[from[i]]++] = to[i];
                in[to[i]][inCount[to[i]]++] = from[i];
            }
        }
    }

    int size() {
        return nodes.length;
    }
}
