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
package com.codename1.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Decides whether an edge may be made. Extend it for rules of your own and
/// call `super.canConnect` to keep these.
///
/// By default an edge is refused when it connects a node to itself, repeats
/// an edge that exists, leaves an input port or enters an output port, joins
/// two ports whose types are both set and differ, or takes a port past its
/// `GraphPort.getMaxConnections`. Cycles are allowed unless
/// `setAllowCycles(false)` is called.
public class ConnectionValidator {
    private boolean allowCycles = true;
    private boolean allowSelfLoops;

    /// True, the default, when an edge may close a cycle.
    public boolean isAllowCycles() {
        return allowCycles;
    }

    /// Refuses edges that would close a cycle when false.
    public void setAllowCycles(boolean allowCycles) {
        this.allowCycles = allowCycles;
    }

    /// False, the default, when a node may not connect to itself.
    public boolean isAllowSelfLoops() {
        return allowSelfLoops;
    }

    /// Allows a node to connect to itself.
    public void setAllowSelfLoops(boolean allowSelfLoops) {
        this.allowSelfLoops = allowSelfLoops;
    }

    /// True when the edge may be made.
    public boolean canConnect(GraphModel model, GraphNode source, GraphPort sourcePort,
                              GraphNode target, GraphPort targetPort) {
        if (source == null || target == null) {
            return false;
        }
        if (source == target && !allowSelfLoops) { // NOPMD CompareObjectsWithEquals - the same object is meant
            return false;
        }
        if (sourcePort != null && sourcePort.isInput()) {
            return false;
        }
        if (targetPort != null && !targetPort.isInput()) {
            return false;
        }
        if (sourcePort != null && targetPort != null && sourcePort.getType() != null
                && targetPort.getType() != null && !sourcePort.getType().equals(targetPort.getType())) {
            return false;
        }
        List<GraphEdge> edges = model.getEdges();
        int n = edges.size();
        int fromCount = 0;
        int toCount = 0;
        for (int i = 0; i < n; i++) {
            GraphEdge e = edges.get(i);
            boolean fromHere = source.getId().equals(e.getSourceId()) && same(portId(sourcePort), e.getSourcePortId());
            boolean toThere = target.getId().equals(e.getTargetId()) && same(portId(targetPort), e.getTargetPortId());
            if (fromHere && toThere) {
                return false;
            }
            if (fromHere && sourcePort != null) {
                fromCount++;
            }
            if (toThere && targetPort != null) {
                toCount++;
            }
        }
        if (sourcePort != null && sourcePort.getMaxConnections() >= 0 && fromCount >= sourcePort.getMaxConnections()) {
            return false;
        }
        if (targetPort != null && targetPort.getMaxConnections() >= 0 && toCount >= targetPort.getMaxConnections()) {
            return false;
        }
        return allowCycles || !reaches(edges, target.getId(), source.getId());
    }

    private static String portId(GraphPort p) {
        return p == null ? null : p.getId();
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /// True when a directed path leads from `from` to `to`.
    static boolean reaches(List<GraphEdge> edges, String from, String to) {
        if (from.equals(to)) {
            return true;
        }
        Set<String> seen = new HashSet<String>();
        java.util.ArrayList<String> stack = new java.util.ArrayList<String>();
        stack.add(from);
        seen.add(from);
        int n = edges.size();
        while (!stack.isEmpty()) {
            String at = stack.remove(stack.size() - 1);
            for (int i = 0; i < n; i++) {
                GraphEdge e = edges.get(i);
                if (at.equals(e.getSourceId())) {
                    String next = e.getTargetId();
                    if (next.equals(to)) {
                        return true;
                    }
                    if (seen.add(next)) {
                        stack.add(next);
                    }
                }
            }
        }
        return false;
    }
}
