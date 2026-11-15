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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The in-memory model and the rules an edge has to pass.
class GraphModelTest {
    private static GraphNode editorNode(String id) {
        GraphNode n = new GraphNode(id, id, 0, 0, 160, 90);
        n.addInput("in", "In", "number");
        n.addInput("text", "Text", "string");
        n.addOutput("out", "Out", "number");
        return n;
    }

    @Test
    void nodesEdgesAndEventsKeepTheIndexes() {
        DefaultGraphModel m = new DefaultGraphModel();
        final int[] events = new int[1];
        m.addChangeListener(new GraphChangeListener() {
            @Override
            public void graphChanged(GraphModel model) {
                events[0]++;
            }
        });
        GraphNode a = m.addNode(new GraphNode("a", "A"));
        GraphNode b = m.addNode(new GraphNode("b", "B"));
        GraphEdge e = m.addEdge("a", "b");
        assertSame(a, m.getNode("a"));
        assertSame(e, m.getEdge(e.getId()));
        assertEquals(3, events[0]);

        assertTrue(m.moveNode(b, 10, -20));
        assertEquals(-20, b.getY(), 0);
        assertTrue(m.removeNode(a));
        assertNull(m.getNode("a"));
        assertTrue(m.getEdges().isEmpty(), "the node's edges go with it");
        assertNull(m.getEdge(e.getId()));
        assertEquals(5, events[0]);
    }

    @Test
    void replacingANodeKeepsOneCopy() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A"));
        GraphNode again = m.addNode(new GraphNode("a", "A2"));
        assertEquals(1, m.getNodes().size());
        assertSame(again, m.getNode("a"));
    }

    @Test
    void generatedEdgeIdsSkipTakenOnes() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A"));
        m.addNode(new GraphNode("b", "B"));
        m.addNode(new GraphNode("c", "C"));
        m.addEdge(new GraphEdge("e1", "a", "b"));
        GraphEdge e = m.connect(m.getNode("b"), null, m.getNode("c"), null);
        assertNotNull(e);
        assertFalse("e1".equals(e.getId()));
    }

    @Test
    void portsDecideWhatMayConnect() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode a = m.addNode(editorNode("a"));
        GraphNode b = m.addNode(editorNode("b"));
        GraphPort out = a.getPort("out");
        assertSame(a, out.getNode());
        assertNull(m.connect(a, out, b, b.getPort("text")), "number into string");
        assertNull(m.connect(a, a.getPort("in"), b, b.getPort("in")), "out of an input");
        assertNull(m.connect(a, out, b, b.getPort("out")), "into an output");
        assertNotNull(m.connect(a, out, b, b.getPort("in")));
        assertNull(m.connect(a, out, b, b.getPort("in")), "the same edge twice");
    }

    @Test
    void maxConnectionsCapsAPort() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode a = m.addNode(editorNode("a"));
        GraphNode b = m.addNode(editorNode("b"));
        GraphNode c = m.addNode(editorNode("c"));
        b.getPort("in").setMaxConnections(1);
        assertNotNull(m.connect(a, a.getPort("out"), b, b.getPort("in")));
        assertNull(m.connect(c, c.getPort("out"), b, b.getPort("in")));
    }

    @Test
    void cyclesAndSelfLoopsAreRefusedOnRequest() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode a = m.addNode(new GraphNode("a", "A"));
        GraphNode b = m.addNode(new GraphNode("b", "B"));
        GraphNode c = m.addNode(new GraphNode("c", "C"));
        assertNull(m.connect(a, null, a, null), "no self loops by default");
        m.getValidator().setAllowSelfLoops(true);
        assertNotNull(m.connect(a, null, a, null));

        m.getValidator().setAllowCycles(false);
        assertNotNull(m.connect(a, null, b, null));
        assertNotNull(m.connect(b, null, c, null));
        assertNull(m.connect(c, null, a, null), "c to a closes a cycle");
        m.getValidator().setAllowCycles(true);
        assertNotNull(m.connect(c, null, a, null));
    }

    @Test
    void anAbstractModelRefusesEveryEdit() {
        final DefaultGraphModel data = new DefaultGraphModel();
        GraphNode a = data.addNode(new GraphNode("a", "A"));
        AbstractGraphModel ro = new AbstractGraphModel() {
            @Override
            public java.util.List<GraphNode> getNodes() {
                return data.getNodes();
            }

            @Override
            public java.util.List<GraphEdge> getEdges() {
                return data.getEdges();
            }

            @Override
            public java.util.List<GraphGroup> getGroups() {
                return data.getGroups();
            }
        };
        assertSame(a, ro.getNode("a"));
        assertFalse(ro.moveNode(a, 1, 1));
        assertNull(ro.connect(a, null, a, null));
        assertFalse(ro.removeNode(a));
        assertFalse(ro.renameNode(a, "x"));
    }
}
