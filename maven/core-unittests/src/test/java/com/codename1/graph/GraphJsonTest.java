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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A graph written as JSON reads back the same.
class GraphJsonTest {
    private static DefaultGraphModel sample() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode a = new GraphNode("a", "Start \"here\"", 10, 20, 120, 50).setShape(GraphShapes.TERMINATOR);
        a.setDescription("first\nline");
        a.setStyleKey("ok");
        a.addOutput("out", "Out", "num");
        GraphNode b = new GraphNode("b", "Sum", 200.5, 20, 140, 90);
        b.addInput("in", "In", "num");
        b.addInput("s", "Text", "str");
        b.getPort("in").setValue(Integer.valueOf(7));
        b.getPort("s").setValue("x");
        b.setGroupId("lane");
        b.setUserData("payload");
        m.addNode(a);
        m.addNode(b);
        GraphEdge e = m.addEdge(new GraphEdge("e1", "a", "out", "b", "in"));
        e.setLabel("feeds");
        e.setLineStyle(GraphEdge.LINE_DASHED);
        e.setSourceArrow(GraphEdge.ARROW_CIRCLE);
        e.setTargetArrow(GraphEdge.ARROW_OPEN);
        e.setRoute(new double[]{1, 2, 3.5, 4});
        m.addGroup(GraphGroup.lane("lane", "Lane", true, 0, 0, 500, 200));
        GraphGroup g = m.addGroup(new GraphGroup("g", "Group"));
        g.setCollapsed(true);
        return m;
    }

    @Test
    void aGraphRoundTrips() throws IOException {
        String json = GraphJson.toJson(sample());
        DefaultGraphModel m = GraphJson.fromJson(json);
        assertEquals(2, m.getNodes().size());
        GraphNode a = m.getNode("a");
        assertEquals("Start \"here\"", a.getLabel());
        assertEquals("first\nline", a.getDescription());
        assertEquals(GraphShapes.TERMINATOR, a.getShape());
        assertEquals("ok", a.getStyleKey());
        assertEquals(10, a.getX(), 0);
        assertEquals(50, a.getHeight(), 0);
        assertEquals("num", a.getPort("out").getType());
        assertFalse(a.getPort("out").isInput());
        GraphNode b = m.getNode("b");
        assertEquals(200.5, b.getX(), 0);
        assertEquals("lane", b.getGroupId());
        assertEquals(7, ((Number) b.getPort("in").getValue()).intValue());
        assertEquals("x", b.getPort("s").getValue());
        // user data needs a codec
        assertNull(b.getUserData());
        GraphEdge e = m.getEdge("e1");
        assertNotNull(e);
        assertEquals("out", e.getSourcePortId());
        assertEquals("in", e.getTargetPortId());
        assertEquals("feeds", e.getLabel());
        assertEquals(GraphEdge.LINE_DASHED, e.getLineStyle());
        assertEquals(GraphEdge.ARROW_CIRCLE, e.getSourceArrow());
        assertEquals(GraphEdge.ARROW_OPEN, e.getTargetArrow());
        assertEquals(4, e.getRoute().length);
        assertEquals(3.5, e.getRoute()[2], 0);
        assertEquals(2, m.getGroups().size());
        GraphGroup lane = m.getGroup("lane");
        assertTrue(lane.isLane());
        assertTrue(lane.isHorizontal());
        assertEquals(500, lane.getWidth(), 0);
        assertTrue(m.getGroup("g").isCollapsed());
        assertFalse(m.getGroup("g").isLane());
        // and what was read writes the same text
        assertEquals(json, GraphJson.toJson(m));
    }

    @Test
    void aDefaultEdgeKeepsFollowingItsDirection() throws IOException {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A"));
        m.addNode(new GraphNode("b", "B"));
        GraphEdge e = m.addEdge("a", "b");
        e.setDirected(false);
        GraphEdge back = GraphJson.fromJson(GraphJson.toJson(m)).getEdges().get(0);
        assertFalse(back.isDirected());
        assertFalse(back.isTargetArrowSet());
        assertEquals(GraphEdge.ARROW_NONE, back.getTargetArrow());
    }

    @Test
    void aSubsetKeepsOnlyTheEdgesInsideIt() throws IOException {
        DefaultGraphModel m = sample();
        m.addNode(new GraphNode("c", "C"));
        m.addEdge("b", "c");
        List<GraphNode> some = new ArrayList<GraphNode>();
        some.add(m.getNode("a"));
        some.add(m.getNode("b"));
        DefaultGraphModel part = GraphJson.fromJson(GraphJson.toJson(m, some, null));
        assertEquals(2, part.getNodes().size());
        assertEquals(1, part.getEdges().size());
        assertEquals(0, part.getGroups().size());
    }

    @Test
    void aCodecCarriesUserData() throws IOException {
        GraphJson.UserDataCodec codec = new GraphJson.UserDataCodec() {
            @Override
            public Object encode(Object userData) {
                return "<" + userData + ">";
            }

            @Override
            public Object decode(Object json) {
                String s = (String) json;
                return s.substring(1, s.length() - 1);
            }
        };
        DefaultGraphModel m = sample();
        DefaultGraphModel back = new DefaultGraphModel();
        GraphJson.read(GraphJson.toJson(m, null, codec), back, codec);
        assertEquals("payload", back.getNode("b").getUserData());
    }

    @Test
    void textThatIsNotAGraphIsRefused() {
        assertThrows(IOException.class, () -> GraphJson.fromJson("[1, 2]"));
        assertThrows(IOException.class, () -> GraphJson.fromJson("not json at all {"));
    }

    @Test
    void aDocumentThatCannotBeAddedWholeAddsNothing() throws IOException {
        final DefaultGraphModel m = sample();
        final String json = GraphJson.toJson(m);
        // every id is taken: nothing of it goes in
        assertThrows(IOException.class, () -> GraphJson.read(json, m, null));
        assertEquals(2, m.getNodes().size());
        assertEquals(1, m.getEdges().size());
        assertEquals(2, m.getGroups().size());
        // a node twice in one document, and an edge to a node that is nowhere
        final DefaultGraphModel empty = new DefaultGraphModel();
        assertThrows(IOException.class, () -> GraphJson.read(
                "{\"nodes\":[{\"id\":\"a\"},{\"id\":\"a\"}]}", empty, null));
        assertThrows(IOException.class, () -> GraphJson.read(
                "{\"nodes\":[{\"id\":\"a\"}],\"edges\":[{\"id\":\"e\",\"from\":\"a\",\"to\":\"zz\"}]}",
                empty, null));
        assertEquals(0, empty.getNodes().size());
        // an edge may name a node the model already has
        empty.addNode(new GraphNode("zz", "Z"));
        GraphJson.read("{\"nodes\":[{\"id\":\"a\"}],\"edges\":[{\"id\":\"e\",\"from\":\"a\",\"to\":\"zz\"}]}",
                empty, null);
        assertEquals(1, empty.getEdges().size());
    }

    @Test
    void aPortValueKeepsItsKindThroughJson() throws IOException {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode n = m.addNode(new GraphNode("n", "N")
                .addInput("on", "On", "bool").addInput("count", "Count", "num").addInput("ratio", "Ratio", "num"));
        n.getPort("on").setValue(Boolean.TRUE);
        n.getPort("count").setValue(Integer.valueOf(3));
        n.getPort("ratio").setValue(Double.valueOf(0.5));
        GraphNode back = GraphJson.fromJson(GraphJson.toJson(m)).getNode("n");
        assertEquals(Boolean.TRUE, back.getPort("on").getValue());
        assertEquals(Long.valueOf(3), back.getPort("count").getValue());
        assertEquals(Double.valueOf(0.5), back.getPort("ratio").getValue());
    }
}
