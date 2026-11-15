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

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every arrangement on the same graphs: nothing overlaps, every node gets a
/// place, and each keeps the promise its name makes.
class GraphArrangementTest {
    /// A random tree of `n` nodes of mixed widths with `extra` more edges.
    private static DefaultGraphModel graph(int n, int extra, long seed) {
        Random r = new Random(seed);
        DefaultGraphModel m = new DefaultGraphModel();
        for (int i = 0; i < n; i++) {
            m.addNode(new GraphNode("n" + i, "N" + i, 0, 0, 100 + r.nextInt(80), 40 + r.nextInt(30)));
        }
        for (int i = 1; i < n; i++) {
            m.addEdge("n" + r.nextInt(i), "n" + i);
        }
        for (int i = 0; i < extra; i++) {
            m.addEdge("n" + r.nextInt(n), "n" + r.nextInt(n));
        }
        return m;
    }

    private static void assertNoOverlap(DefaultGraphModel m, ArrangementResult r) {
        List<GraphNode> ns = m.getNodes();
        for (int i = 0; i < ns.size(); i++) {
            GraphNode a = ns.get(i);
            double[] p = r.getPosition(a.getId());
            assertNotNull(p, a.getId() + " was placed");
            for (int j = i + 1; j < ns.size(); j++) {
                GraphNode b = ns.get(j);
                double[] q = r.getPosition(b.getId());
                boolean overlap = p[0] < q[0] + b.getWidth() && q[0] < p[0] + a.getWidth()
                        && p[1] < q[1] + b.getHeight() && q[1] < p[1] + a.getHeight();
                assertFalse(overlap, a.getId() + " overlaps " + b.getId());
            }
        }
    }

    @Test
    void noLayoutOverlapsNodes() {
        GraphArrangement[] arrangements = {new LayeredArrangement(), new TreeArrangement(), new RadialArrangement(), new ForceArrangement()};
        for (long seed = 1; seed <= 4; seed++) {
            DefaultGraphModel m = graph(80, 20, seed);
            for (GraphArrangement l : arrangements) {
                assertNoOverlap(m, l.arrange(m));
            }
        }
        LayeredArrangement lr = new LayeredArrangement();
        lr.setDirection(LayeredArrangement.LEFT_TO_RIGHT);
        TreeArrangement tr = new TreeArrangement();
        tr.setDirection(TreeArrangement.LEFT_TO_RIGHT);
        DefaultGraphModel m = graph(60, 10, 9);
        assertNoOverlap(m, lr.arrange(m));
        assertNoOverlap(m, tr.arrange(m));
    }

    @Test
    void layoutsAreDeterministic() {
        DefaultGraphModel m = graph(50, 15, 3);
        GraphArrangement[] arrangements = {new LayeredArrangement(), new TreeArrangement(), new RadialArrangement(), new ForceArrangement()};
        for (GraphArrangement l : arrangements) {
            ArrangementResult a = l.arrange(m);
            ArrangementResult b = l.arrange(m);
            for (GraphNode n : m.getNodes()) {
                assertEquals(a.getPosition(n.getId())[0], b.getPosition(n.getId())[0], 0, l.getClass().getName());
                assertEquals(a.getPosition(n.getId())[1], b.getPosition(n.getId())[1], 0, l.getClass().getName());
            }
        }
    }

    @Test
    void layeredEdgesPointDownAndLongOnesBend() {
        DefaultGraphModel m = new DefaultGraphModel();
        for (String id : new String[]{"a", "b", "c", "d"}) {
            m.addNode(new GraphNode(id, id));
        }
        m.addEdge("a", "b");
        m.addEdge("b", "c");
        m.addEdge("c", "d");
        GraphEdge skip = m.addEdge("a", "d");
        GraphEdge back = m.addEdge("d", "b");
        ArrangementResult r = new LayeredArrangement().arrange(m);
        double ya = r.getPosition("a")[1];
        double yb = r.getPosition("b")[1];
        double yc = r.getPosition("c")[1];
        double yd = r.getPosition("d")[1];
        assertTrue(ya < yb && yb < yc && yc < yd, "one layer each, top to bottom");
        double[] route = r.getRoute(skip.getId());
        assertNotNull(route, "a to d crosses two layers");
        assertEquals(4, route.length);
        assertNotNull(r.getRoute(back.getId()), "the edge that closed the cycle is routed too");
    }

    @Test
    void layeredFindsACrossingFreeOrderWhenThereIsOne() {
        // two chains whose ends were added crosswise
        DefaultGraphModel m = new DefaultGraphModel();
        for (String id : new String[]{"a", "b", "a2", "b2", "a3", "b3"}) {
            m.addNode(new GraphNode(id, id));
        }
        m.addEdge("a", "b2");
        m.addEdge("b", "a2");
        m.addEdge("b2", "a3");
        m.addEdge("a2", "b3");
        ArrangementResult r = new LayeredArrangement().arrange(m);
        boolean leftA = r.getPosition("a")[0] < r.getPosition("b")[0];
        assertEquals(leftA, r.getPosition("b2")[0] < r.getPosition("a2")[0]);
        assertEquals(leftA, r.getPosition("a3")[0] < r.getPosition("b3")[0]);
    }

    @Test
    void treeCentresParentsOverTheirChildren() {
        DefaultGraphModel m = new DefaultGraphModel();
        for (String id : new String[]{"root", "x", "y", "z"}) {
            m.addNode(new GraphNode(id, id, 0, 0, 100, 40));
        }
        m.addEdge("root", "x");
        m.addEdge("root", "y");
        m.addEdge("root", "z");
        ArrangementResult r = new TreeArrangement().arrange(m);
        double rootCentre = r.getPosition("root")[0] + 50;
        double childCentre = (r.getPosition("x")[0] + r.getPosition("z")[0]) / 2 + 50;
        assertEquals(childCentre, rootCentre, 0.001);
        assertTrue(r.getPosition("x")[1] > r.getPosition("root")[1]);
        assertNull(r.getRoute("anything"));
    }

    @Test
    void radialPutsTheRootInTheMiddle() {
        DefaultGraphModel m = graph(30, 0, 5);
        RadialArrangement l = new RadialArrangement();
        l.setRootId("n0");
        ArrangementResult r = l.arrange(m);
        GraphNode root = m.getNode("n0");
        assertEquals(0, r.getPosition("n0")[0] + root.getWidth() / 2, 0.001);
        assertEquals(0, r.getPosition("n0")[1] + root.getHeight() / 2, 0.001);
    }

    @Test
    void forceKeepsPinnedNodesAndRunsInSteps() {
        DefaultGraphModel m = graph(20, 5, 7);
        GraphNode pinned = m.getNode("n3");
        pinned.setPosition(500, 500);
        pinned.setPinned(true);
        ForceArrangement f = new ForceArrangement();
        f.setIterations(10);
        f.start(m);
        int steps = 0;
        while (f.step()) {
            steps++;
        }
        assertEquals(9, steps);
        ArrangementResult r = f.result();
        assertEquals(500, r.getPosition("n3")[0], 0);
        r.apply(m);
        assertEquals(500, pinned.getX(), 0);
    }

    @Test
    void emptyAndSingleNodeGraphsAreFine() {
        DefaultGraphModel m = new DefaultGraphModel();
        GraphArrangement[] arrangements = {new LayeredArrangement(), new TreeArrangement(), new RadialArrangement(), new ForceArrangement()};
        for (GraphArrangement l : arrangements) {
            l.arrange(m);
        }
        m.addNode(new GraphNode("only", "Only"));
        for (GraphArrangement l : arrangements) {
            assertNotNull(l.arrange(m).getPosition("only"));
        }
    }
}
