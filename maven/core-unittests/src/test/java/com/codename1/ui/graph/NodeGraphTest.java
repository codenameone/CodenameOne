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
package com.codename1.ui.graph;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The node graph through its surface: what taps and drags do to the model.
/// Every test shows the graph at zoom 1 with world 0, 0 at the top left, so
/// world units are pixels of the view.
class NodeGraphTest extends UITestBase {
    private NodeGraph show(DefaultGraphModel model) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        NodeGraph g = new NodeGraph(model);
        g.setGridVisible(false);
        form.add(BorderLayout.CENTER, g);
        form.revalidate();
        g.setZoom(1f);
        g.setView(0, 0);
        return g;
    }

    private static void drag(NodeGraph g, int fromX, int fromY, int toX, int toY) {
        Component c = g.canvas();
        int ax = c.getAbsoluteX();
        int ay = c.getAbsoluteY();
        c.pointerPressed(ax + fromX, ay + fromY);
        c.pointerDragged(ax + (fromX + toX) / 2, ay + (fromY + toY) / 2);
        c.pointerDragged(ax + toX, ay + toY);
        c.pointerReleased(ax + toX, ay + toY);
    }

    private static void tap(NodeGraph g, int x, int y) {
        Component c = g.canvas();
        c.pointerPressed(c.getAbsoluteX() + x, c.getAbsoluteY() + y);
        c.pointerReleased(c.getAbsoluteX() + x, c.getAbsoluteY() + y);
    }

    private static DefaultGraphModel twoBoxes() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A", 20, 20, 100, 50));
        m.addNode(new GraphNode("b", "B", 260, 20, 100, 50));
        return m;
    }

    @FormTest
    void tapSelectsAndTheBackgroundClears() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        assertEquals(1f, g.getZoom(), 0.0001);
        tap(g, 60, 40);
        assertEquals(1, g.getSelectedNodes().size());
        assertSame(m.getNode("a"), g.getSelectedNodes().get(0));
        tap(g, 200, 300);
        assertTrue(g.getSelectedNodes().isEmpty());
    }

    @FormTest
    void draggingANodeMovesItThroughTheModel() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        drag(g, 60, 40, 60, 140);
        assertEquals(120, m.getNode("a").getY(), 0.5);
        assertEquals(20, m.getNode("a").getX(), 0.5);
    }

    @FormTest
    void aRefusedMoveGoesBack() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.addGraphListener(new GraphAdapter() {
            @Override
            public boolean nodeMoving(NodeGraph graph, GraphNode node, double x, double y) {
                return false;
            }
        });
        drag(g, 60, 40, 60, 140);
        assertEquals(20, m.getNode("a").getY(), 0);
    }

    @FormTest
    void snapRoundsTheDrop() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.setSnap(25);
        // 20 + 7 rounds to 25, and 20 + 93 to 125
        drag(g, 60, 40, 67, 133);
        assertEquals(25, m.getNode("a").getX(), 0);
        assertEquals(125, m.getNode("a").getY(), 0);
    }

    @FormTest
    void theHandleOfASelectedNodeConnectsIt() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        tap(g, 60, 40);
        // the handle sits on the middle of the trailing edge
        drag(g, 120, 45, 300, 45);
        assertEquals(1, m.getEdges().size());
        GraphEdge e = m.getEdges().get(0);
        assertEquals("a", e.getSourceId());
        assertEquals("b", e.getTargetId());
    }

    @FormTest
    void portsConnectOnlyMatchingTypesAndAnInputCanBePickedUp() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode a = m.addNode(new GraphNode("a", "A", 20, 20, 140, 90));
        a.addOutput("out", "Out", "number");
        GraphNode b = m.addNode(new GraphNode("b", "B", 300, 20, 140, 90));
        b.addInput("text", "Text", "string");
        b.addInput("in", "In", "number");
        NodeGraph g = show(m);
        int ox = (int) g.portX(a, a.getPort("out"));
        int oy = (int) g.portY(a, a.getPort("out"));
        GraphPort text = b.getPort("text");
        int tx = (int) g.portX(b, text);
        int ty = (int) g.portY(b, text);
        drag(g, ox, oy, tx, ty);
        assertEquals(1, m.getEdges().size(), "dropped near the string input, the number input is taken");
        GraphEdge e = m.getEdges().get(0);
        assertEquals("in", e.getTargetPortId());

        // picking the edge up off its input and dropping it on nothing removes it
        int ix = (int) g.portX(b, b.getPort("in"));
        int iy = (int) g.portY(b, b.getPort("in"));
        drag(g, ix, iy, 250, 300);
        assertTrue(m.getEdges().isEmpty());
    }

    @FormTest
    void aRefusedConnectionMakesNoEdge() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.addGraphListener(new GraphAdapter() {
            @Override
            public boolean edgeCreating(NodeGraph graph, GraphNode s, GraphPort sp, GraphNode t, GraphPort tp) {
                return false;
            }
        });
        tap(g, 60, 40);
        drag(g, 120, 45, 300, 45);
        assertTrue(m.getEdges().isEmpty());
    }

    @FormTest
    void tappingAnEdgeSelectsItAndDeleteRemovesIt() {
        DefaultGraphModel m = twoBoxes();
        GraphEdge e = m.addEdge("a", "b");
        NodeGraph g = show(m);
        assertSame(e, g.edgeAt(190, 45));
        tap(g, 190, 45);
        assertSame(e, g.getSelectedEdge());
        g.deleteSelection();
        assertTrue(m.getEdges().isEmpty());
    }

    @FormTest
    void aGroupTitleTapCollapsesItAndHidesItsMembers() {
        DefaultGraphModel m = twoBoxes();
        m.addGroup(new GraphGroup("g", "Services"));
        m.getNode("a").setGroupId("g");
        m.getNode("b").setGroupId("g");
        NodeGraph g = show(m);
        g.setView(-100, -100);
        double[] r = g.getGroupRect(m.getGroup("g"));
        tap(g, g.worldToScreenX(r[0] + 40), g.worldToScreenY(r[1] + 10));
        assertTrue(m.getGroup("g").isCollapsed());
        assertFalse(g.isNodeVisible(m.getNode("a")));
        assertNull(g.nodeAt(g.worldToScreenX(320), g.worldToScreenY(40)), "b is hidden in the group");
    }

    @FormTest
    void panningMovesTheViewNotTheNodes() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        drag(g, 200, 300, 150, 250);
        assertEquals(50, g.getViewX(), 0.5);
        assertEquals(50, g.getViewY(), 0.5);
        assertEquals(20, m.getNode("a").getX(), 0);
    }

    @FormTest
    void zoomKeepsThePointUnderTheFinger() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        double wx = g.screenToWorldX(100);
        double wy = g.screenToWorldY(80);
        assertTrue(g.zoomAt(2f, 100, 80));
        assertEquals(wx, g.screenToWorldX(100), 0.001);
        assertEquals(wy, g.screenToWorldY(80), 0.001);
    }

    @FormTest
    void aLayoutWithoutAnimationPlacesNodesAndFitShowsThemAll() {
        DefaultGraphModel m = twoBoxes();
        m.addNode(new GraphNode("c", "C"));
        m.addEdge("a", "b");
        m.addEdge("b", "c");
        NodeGraph g = show(m);
        g.arrange(new LayeredArrangement(), false);
        assertTrue(m.getNode("a").getY() < m.getNode("b").getY());
        g.fitToView();
        for (GraphNode n : m.getNodes()) {
            assertTrue(g.worldToScreenX(n.getX()) >= 0);
            assertTrue(g.worldToScreenY(n.getY() + n.getHeight()) <= g.canvas().getHeight());
        }
        Image img = g.toImage(200, 150);
        assertEquals(200, img.getWidth());
    }

    @FormTest
    void aFactoryComponentSitsOverItsNode() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.setNodeComponentFactory(new NodeComponentFactory() {
            @Override
            public Component createComponent(NodeGraph graph, GraphNode node) {
                return "a".equals(node.getId()) ? new Label("slider") : null;
            }
        });
        Component c = g.getNodeComponent(m.getNode("a"));
        assertNotNull(c);
        assertNotNull(c.getParent());
        assertTrue(c.getX() >= 20 && c.getY() > 20);
        assertNull(g.getNodeComponent(m.getNode("b")));
        g.setZoom(0.3f);
        assertNull(c.getParent(), "hidden below the component zoom");
    }
}
