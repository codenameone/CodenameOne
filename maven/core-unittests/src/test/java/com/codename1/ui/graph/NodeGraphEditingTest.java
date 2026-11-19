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
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.graph.GraphPort;
import com.codename1.graph.GraphShapes;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.UndoHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What makes a node graph a diagram editor: shapes, resizing, lanes, the
/// palette, the clipboard and undo. As in `NodeGraphTest`, the graph is at
/// zoom 1 with world 0, 0 at the top left.
class NodeGraphEditingTest extends UITestBase {
    private UndoHistory history;

    private NodeGraph show(DefaultGraphModel model) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        NodeGraph g = new NodeGraph(model);
        g.setGridVisible(false);
        history = new UndoHistory();
        g.setUndoHistory(history);
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
        m.addEdge("a", "b");
        return m;
    }

    @FormTest
    void aShapeIsHitByItsOutlineNotItsBounds() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("d", "Sure?", 20, 20, 120, 80).setShape(GraphShapes.DECISION));
        NodeGraph g = show(m);
        // inside the bounds, outside the diamond
        tap(g, 26, 24);
        assertTrue(g.getSelectedNodes().isEmpty());
        tap(g, 80, 60);
        assertEquals(1, g.getSelectedNodes().size());
    }

    @FormTest
    void anEdgeEndsOnTheOutline() {
        double[] rect = {0, 0, 100, 60};
        double[] xs = {75};
        double[] ys = {0};
        assertFalse(GraphShapeRegistry.contains(GraphShapes.DECISION, 74, 2, 0, 0, 100, 60));
        GraphShapeRegistry.pullToOutline(GraphShapes.DECISION, rect, xs, ys, 0);
        // the point moved along the line to the centre, onto the diamond's edge
        assertTrue(ys[0] > 5, "y " + ys[0]);
        assertEquals((75 - xs[0]) / 25, ys[0] / 30, 0.01);
        assertEquals(1.0, Math.abs(xs[0] - 50) / 50 + Math.abs(ys[0] - 30) / 30, 0.02);
        // a shape with no outline is left on its bounds
        xs[0] = 75;
        ys[0] = 0;
        GraphShapeRegistry.pullToOutline("no such shape", rect, xs, ys, 0);
        assertEquals(0, ys[0], 0);
    }

    @FormTest
    void everyBuiltInShapePaints() {
        DefaultGraphModel m = new DefaultGraphModel();
        String[] shapes = {GraphShapes.RECT, GraphShapes.ROUNDED, GraphShapes.TERMINATOR, GraphShapes.DECISION,
            GraphShapes.DATA, GraphShapes.DOCUMENT, GraphShapes.PREPARATION, GraphShapes.DATABASE,
            GraphShapes.SUBPROCESS, GraphShapes.ELLIPSE, GraphShapes.NOTE};
        for (int i = 0; i < shapes.length; i++) {
            assertNotNull(GraphShapeRegistry.get(shapes[i]), shapes[i]);
            m.addNode(new GraphNode("n" + i, shapes[i], 10 + (i % 4) * 90, 10 + (i / 4) * 70, 80, 50)
                    .setShape(shapes[i]));
            // the middle of every outline is inside it
            assertTrue(GraphShapeRegistry.contains(shapes[i], 40, 25, 0, 0, 80, 50), shapes[i]);
        }
        GraphEdge e = m.addEdge("n0", "n3");
        e.setLineStyle(GraphEdge.LINE_DASHED);
        e.setSourceArrow(GraphEdge.ARROW_CIRCLE);
        e.setTargetArrow(GraphEdge.ARROW_OPEN);
        GraphEdge e2 = m.addEdge("n1", "n6");
        e2.setLineStyle(GraphEdge.LINE_DOTTED);
        e2.setTargetArrow(GraphEdge.ARROW_DIAMOND);
        NodeGraph g = show(m);
        Image img = g.toImage(400, 300);
        assertNotNull(img);
        assertEquals(400, img.getWidth());
    }

    @FormTest
    void aCornerResizesTheSelectedNodeAndUndoPutsItBack() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.setResizable(true);
        tap(g, 60, 40);
        drag(g, 120, 70, 160, 100);
        GraphNode a = m.getNode("a");
        assertEquals(140, a.getWidth(), 0.5);
        assertEquals(80, a.getHeight(), 0.5);
        assertEquals(20, a.getX(), 0.5);
        // the top leading corner moves the node too, and stops at a least size
        drag(g, 20, 20, 400, 400);
        assertEquals(40, a.getWidth(), 0.5);
        assertEquals(24, a.getHeight(), 0.5);
        assertEquals(120, a.getX(), 0.5);
        assertTrue(g.undo());
        assertEquals(140, a.getWidth(), 0.5);
        assertEquals(20, a.getX(), 0.5);
        assertTrue(g.undo());
        assertEquals(100, a.getWidth(), 0.5);
        assertEquals(50, a.getHeight(), 0.5);
        assertTrue(g.redo());
        assertEquals(140, a.getWidth(), 0.5);
    }

    @FormTest
    void aListenerRefusesAResize() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        g.setResizable(true);
        final int[] done = new int[1];
        g.addGraphListener(new GraphAdapter() {
            @Override
            public boolean nodeResizing(NodeGraph graph, GraphNode node, double width, double height) {
                return width < 130;
            }

            @Override
            public void nodeResized(NodeGraph graph, GraphNode node) {
                done[0]++;
            }
        });
        tap(g, 60, 40);
        drag(g, 120, 70, 160, 100);
        assertEquals(100, m.getNode("a").getWidth(), 0);
        assertEquals(0, done[0]);
        assertFalse(history.canUndo());
        drag(g, 120, 70, 140, 70);
        assertEquals(120, m.getNode("a").getWidth(), 0.5);
        assertEquals(1, done[0]);
    }

    @FormTest
    void aNodeDroppedInALaneJoinsItAndTheLaneMovesItsMembers() {
        DefaultGraphModel m = twoBoxes();
        GraphGroup lane = m.addGroup(GraphGroup.lane("ops", "Operations", true, 0, 120, 400, 140));
        NodeGraph g = show(m);
        GraphNode a = m.getNode("a");
        assertNull(a.getGroupId());
        drag(g, 60, 40, 100, 190);
        assertEquals("ops", a.getGroupId());
        assertSame(lane, g.laneAt(a.getCenterX(), a.getCenterY()));
        double ax = a.getX();
        double ay = a.getY();
        // the title band of a horizontal lane is its leading strip
        drag(g, 10, 180, 10, 230);
        assertEquals(170, lane.getY(), 0.5);
        assertEquals(ay + 50, a.getY(), 0.5);
        assertEquals(ax, a.getX(), 0.5);
        assertEquals("Move", history.getUndoName());
        assertTrue(g.undo());
        assertEquals(120, lane.getY(), 0.5);
        assertEquals(ay, a.getY(), 0.5);
        assertTrue(g.undo());
        assertNull(a.getGroupId());
        assertEquals(20, a.getY(), 0.5);
        // a lane never collapses
        tap(g, 10, 180);
        assertFalse(lane.isCollapsed());
    }

    @FormTest
    void aLaneTheModelKeepsInPlaceKeepsItsMembers() {
        DefaultGraphModel m = new DefaultGraphModel() {
            @Override
            public boolean resizeGroup(GraphGroup group, double x, double y, double width, double height) {
                return false;
            }
        };
        GraphGroup lane = m.addGroup(GraphGroup.lane("ops", "Operations", true, 0, 120, 400, 140));
        GraphNode a = new GraphNode("a", "A", 60, 160, 100, 50);
        a.setGroupId("ops");
        m.addNode(a);
        NodeGraph g = show(m);
        drag(g, 10, 180, 10, 230);
        assertEquals(120, lane.getY(), 0.5);
        assertEquals(160, a.getY(), 0.5);
        assertEquals("ops", a.getGroupId());
        assertFalse(history.canUndo());
    }

    @FormTest
    void aLaneEdgeResizesIt() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphGroup lane = m.addGroup(GraphGroup.lane("l", "Lane", false, 20, 20, 200, 200));
        NodeGraph g = show(m);
        drag(g, 220, 120, 300, 120);
        assertEquals(280, lane.getWidth(), 0.5);
        drag(g, 120, 220, 120, 100);
        assertEquals(80, lane.getHeight(), 0.5);
        assertTrue(g.undo());
        assertEquals(200, lane.getHeight(), 0.5);
        assertNotNull(g.toImage(400, 300));
    }

    @FormTest
    void aTemplateMakesANodeWhereItIsPut() {
        DefaultGraphModel m = new DefaultGraphModel();
        NodeGraph g = show(m);
        GraphNodeTemplate t = new GraphNodeTemplate("Decision").setShape(GraphShapes.DECISION).setSize(100, 60);
        final int[] made = new int[1];
        final boolean[] allow = {true};
        g.addGraphListener(new GraphAdapter() {
            @Override
            public boolean nodeCreating(NodeGraph graph, GraphNode node) {
                return allow[0];
            }

            @Override
            public void nodeCreated(NodeGraph graph, GraphNode node) {
                made[0]++;
            }
        });
        GraphNode n = g.createNode(t, 200, 150);
        assertNotNull(n);
        assertEquals(GraphShapes.DECISION, n.getShape());
        assertEquals(150, n.getX(), 0.5);
        assertEquals(120, n.getY(), 0.5);
        assertSame(n, g.getSelectedNodes().get(0));
        assertEquals(1, made[0]);
        // armed, the next tap on the background makes one
        g.setPendingTemplate(t);
        tap(g, 60, 300);
        assertEquals(2, m.getNodes().size());
        assertNull(g.getPendingTemplate());
        tap(g, 300, 300);
        assertEquals(2, m.getNodes().size());
        allow[0] = false;
        assertNull(g.createNode(t, 10, 10));
        assertEquals(2, m.getNodes().size());
        assertTrue(g.undo());
        assertTrue(g.undo());
        assertEquals(0, m.getNodes().size());
        assertTrue(g.redo());
        assertSame(n, m.getNode(n.getId()));
    }

    @FormTest
    void aPaletteItemIsDraggedOntoTheGraph() {
        DefaultGraphModel m = new DefaultGraphModel();
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        NodeGraph g = new NodeGraph(m);
        GraphPalette palette = new GraphPalette(g);
        GraphNodeTemplate step = new GraphNodeTemplate("Step").setShape(GraphShapes.RECT);
        GraphNodeTemplate end = new GraphNodeTemplate("End").setShape(GraphShapes.TERMINATOR);
        palette.add(step).add(end);
        form.add(BorderLayout.CENTER, g);
        form.add(BorderLayout.WEST, palette);
        form.revalidate();
        g.setZoom(1f);
        g.setView(0, 0);
        assertTrue(palette.getWidth() > 0);
        int px = palette.getAbsoluteX() + palette.getWidth() / 2;
        int py = palette.getAbsoluteY() + palette.getPreferredH() * 3 / 4;
        Component c = g.canvas();
        int tx = c.getAbsoluteX() + 200;
        int ty = c.getAbsoluteY() + 150;
        palette.pointerPressed(px, py);
        palette.pointerDragged((px + tx) / 2, (py + ty) / 2);
        palette.pointerDragged(tx, ty);
        palette.pointerReleased(tx, ty);
        assertEquals(1, m.getNodes().size());
        GraphNode n = m.getNodes().get(0);
        assertEquals(GraphShapes.TERMINATOR, n.getShape());
        assertEquals(200, n.getCenterX(), 1);
        assertEquals(150, n.getCenterY(), 1);
        // a tap arms the item, a second lets go of it
        int sy = palette.getAbsoluteY() + palette.getPreferredH() / 4;
        palette.pointerPressed(px, sy);
        palette.pointerReleased(px, sy);
        assertSame(step, g.getPendingTemplate());
        palette.pointerPressed(px, sy);
        palette.pointerReleased(px, sy);
        assertNull(g.getPendingTemplate());
        // let go outside the graph, nothing is made
        palette.pointerPressed(px, sy);
        palette.pointerDragged(px + 40, sy + 40);
        palette.pointerReleased(px, sy + 2);
        assertEquals(1, m.getNodes().size());
    }

    @FormTest
    void copyAndPasteBringTheEdgesAmongTheNodes() {
        DefaultGraphModel m = twoBoxes();
        m.addNode(new GraphNode("c", "C", 20, 200, 100, 50));
        m.addEdge("a", "c");
        NodeGraph g = show(m);
        g.select(m.getNode("a"));
        g.toggleSelection(m.getNode("b"));
        assertTrue(g.copySelection());
        assertTrue(g.paste());
        assertEquals(5, m.getNodes().size());
        // a to b came along; a to c did not, c was not copied
        assertEquals(3, m.getEdges().size());
        assertEquals(2, g.getSelectedNodes().size());
        GraphNode copy = g.getSelectedNodes().get(0);
        assertEquals("A", copy.getLabel());
        assertEquals(44, copy.getX(), 0.5);
        assertTrue(g.paste());
        assertEquals(68, g.getSelectedNodes().get(0).getX(), 0.5);
        assertEquals(7, m.getNodes().size());
        assertEquals("Paste", history.getUndoName());
        assertTrue(g.undo());
        assertTrue(g.undo());
        assertEquals(3, m.getNodes().size());
        assertEquals(2, m.getEdges().size());
        assertTrue(g.redo());
        assertEquals(5, m.getNodes().size());
        assertEquals(3, m.getEdges().size());
    }

    @FormTest
    void duplicateAndCut() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        assertFalse(g.duplicateSelection());
        g.select(m.getNode("a"));
        assertTrue(g.duplicateSelection());
        assertEquals(3, m.getNodes().size());
        assertEquals(1, m.getEdges().size());
        GraphNode copy = g.getSelectedNodes().get(0);
        assertFalse("a".equals(copy.getId()));
        assertTrue(g.cutSelection());
        assertEquals(2, m.getNodes().size());
        assertTrue(g.paste());
        assertEquals(3, m.getNodes().size());
    }

    @FormTest
    void deleteIsUndoneWithItsEdges() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        GraphNode a = m.getNode("a");
        GraphEdge e = m.getEdges().get(0);
        tap(g, 60, 40);
        g.canvas().keyPressed(127);
        assertEquals(1, m.getNodes().size());
        assertEquals(0, m.getEdges().size());
        assertEquals("Delete", history.getUndoName());
        assertTrue(g.undo());
        assertSame(a, m.getNode("a"));
        assertEquals(1, m.getEdges().size());
        assertSame(e, m.getEdges().get(0));
        assertTrue(g.redo());
        assertEquals(0, m.getEdges().size());
    }

    @FormTest
    void aMoveAConnectionAndAnArrangementAreEachOneStep() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("a", "A", 20, 20, 100, 50));
        m.addNode(new GraphNode("b", "B", 260, 20, 100, 50));
        NodeGraph g = show(m);
        g.select(m.getNode("a"));
        g.toggleSelection(m.getNode("b"));
        drag(g, 60, 40, 60, 140);
        assertEquals(1, history.getUndoCount());
        assertEquals(120, m.getNode("b").getY(), 0.5);
        g.select(m.getNode("a"));
        drag(g, 120, 145, 300, 145);
        assertEquals(1, m.getEdges().size());
        assertEquals(2, history.getUndoCount());
        assertEquals("Connect", history.getUndoName());
        g.arrange(new LayeredArrangement(), false);
        assertEquals("Arrange", history.getUndoName());
        double arrangedX = m.getNode("b").getX();
        assertTrue(g.undo());
        assertEquals(260, m.getNode("b").getX(), 0.5);
        assertTrue(g.undo());
        assertEquals(0, m.getEdges().size());
        assertTrue(g.undo());
        assertEquals(20, m.getNode("a").getY(), 0.5);
        assertEquals(20, m.getNode("b").getY(), 0.5);
        assertFalse(g.undo());
        assertTrue(g.redo());
        assertTrue(g.redo());
        assertTrue(g.redo());
        assertEquals(arrangedX, m.getNode("b").getX(), 0.5);
        assertEquals(1, m.getEdges().size());
    }

    @FormTest
    void anUnconnectedInputShowsItsEditor() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode src = new GraphNode("s", "Source", 20, 20, 140, 80);
        src.addOutput("out", "Out", "num");
        GraphNode n = new GraphNode("n", "Add", 260, 20, 160, 100);
        n.addInput("x", "X", "num");
        n.addInput("y", "Y", "num");
        m.addNode(src);
        m.addNode(n);
        final NodeGraph g = show(m);
        g.setPortEditorFactory(new PortEditorFactory() {
            @Override
            public Component createEditor(NodeGraph graph, GraphNode node, GraphPort port) {
                return new Label(String.valueOf(port.getValue()));
            }
        });
        GraphPort x = n.getPort("x");
        Component editor = g.getPortEditor(n, x);
        assertNotNull(editor);
        assertNotNull(editor.getParent());
        assertTrue(editor.getAbsoluteX() >= g.canvas().getAbsoluteX() + 260);
        assertTrue(g.setPortValue(x, Integer.valueOf(3)));
        assertEquals(Integer.valueOf(3), x.getValue());
        assertTrue(g.undo());
        assertNull(x.getValue());
        // connected, the port takes its value from the wire
        m.connect(src, src.getPort("out"), n, x);
        g.setView(0, 0);
        assertNull(editor.getParent());
        assertNotNull(g.getPortEditor(n, n.getPort("y")).getParent());
    }

    @FormTest
    void undoingADeletePutsANodeBackUnderTheOnesThatWereOverIt() {
        DefaultGraphModel m = twoBoxes();
        m.addNode(new GraphNode("c", "C", 400, 300, 100, 60));
        NodeGraph g = show(m);
        GraphNode a = m.getNode("a");
        assertSame(a, m.getNodes().get(0));
        g.select(a);
        g.deleteSelection();
        assertEquals(2, m.getNodes().size());
        assertTrue(g.undo());
        assertSame(a, m.getNodes().get(0));
        assertEquals(3, m.getNodes().size());
    }

    @FormTest
    void anotherModelEndsWhatWasUnderWay() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        GraphNode a = m.getNode("a");
        g.select(a);
        g.duplicateSelection();
        assertTrue(history.canUndo());
        g.startLabelEdit(a);
        DefaultGraphModel other = twoBoxes();
        String label = other.getNode("a").getLabel();
        int nodes = m.getNodes().size();
        g.setModel(other);
        // the steps were the old model's, and so was the label being typed
        assertFalse(history.canUndo());
        assertFalse(g.undo());
        assertEquals(nodes, m.getNodes().size());
        assertEquals(label, other.getNode("a").getLabel());
        assertEquals(2, other.getNodes().size());
    }

    @FormTest
    void aRefusedMoveKeepsTheRoutesAndLeavesNoStep() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        GraphEdge e = m.getEdges().get(0);
        double[] route = {1, 2, 3, 4};
        e.setRoute(route);
        g.addGraphListener(new GraphAdapter() {
            @Override
            public boolean nodeMoving(NodeGraph graph, GraphNode node, double x, double y) {
                return false;
            }
        });
        GraphNode a = m.getNode("a");
        double x = a.getX();
        drag(g, 60, 40, 160, 140);
        assertEquals(x, a.getX(), 0);
        assertSame(route, e.getRoute());
        assertFalse(history.canUndo());
    }

    @FormTest
    void aListenerThatThrowsLeavesNoStepOpen() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        final boolean[] thrown = new boolean[1];
        g.addGraphListener(new GraphAdapter() {
            @Override
            public void nodeMoved(NodeGraph graph, GraphNode node) {
                if (!thrown[0]) {
                    thrown[0] = true;
                    throw new IllegalStateException("listener failure");
                }
            }
        });
        try {
            drag(g, 60, 40, 160, 140);
        } catch (IllegalStateException expected) {
            // the listener's own failure reaches the caller
        }
        assertTrue(thrown[0]);
        assertFalse(history.isCompoundOpen());
        GraphNode b = m.getNode("b");
        double bx = b.getX();
        drag(g, 300, 40, 300, 140);
        assertTrue(b.getY() > 20);
        assertTrue(g.undo());
        assertEquals(bx, b.getX(), 0);
        assertEquals(20, b.getY(), 0);
    }

    @FormTest
    void undoWhileANodeIsHeldPutsItBackFirst() {
        DefaultGraphModel m = twoBoxes();
        NodeGraph g = show(m);
        GraphNode a = m.getNode("a");
        GraphNode b = m.getNode("b");
        m.moveNode(b, 260, 200);
        drag(g, 300, 220, 300, 260);
        assertTrue(history.canUndo());
        Component c = g.canvas();
        int ax = c.getAbsoluteX();
        int ay = c.getAbsoluteY();
        c.pointerPressed(ax + 60, ay + 40);
        c.pointerDragged(ax + 90, ay + 70);
        c.pointerDragged(ax + 160, ay + 140);
        g.undo();
        c.pointerReleased(ax + 160, ay + 140);
        // what was held went back, and the release recorded nothing from it
        assertEquals(20, a.getX(), 0);
        assertEquals(20, a.getY(), 0);
    }

    @FormTest
    void aFactoryWithNoEditorIsAskedAgainOnlyWhenTheValueChanges() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphNode n = m.addNode(new GraphNode("n", "N", 20, 20, 160, 80).addInput("in", "In", "num"));
        NodeGraph g = show(m);
        final int[] asked = new int[1];
        g.setPortEditorFactory(new PortEditorFactory() {
            @Override
            public Component createEditor(NodeGraph graph, GraphNode node, GraphPort port) {
                asked[0]++;
                return port.getValue() == null ? null : new Label(String.valueOf(port.getValue()));
            }
        });
        int first = asked[0];
        assertTrue(first >= 1);
        for (int i = 0; i < 5; i++) {
            m.moveNode(n, 20 + i, 20);
            g.getParent().revalidate();
        }
        assertEquals(first, asked[0]);
        g.setPortValue(n.getPort("in"), Integer.valueOf(4));
        g.getParent().revalidate();
        assertNotNull(g.getPortEditor(n, n.getPort("in")));
    }
}
