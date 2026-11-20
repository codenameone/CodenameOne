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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphShapes;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// Every built-in outline, every line style and every kind of arrow, in
/// three vertical lanes, with a selected node showing its resize handles.
/// What `FlowChartTheme` leaves out of a chart that has to read as one.
public class FlowChartShapesScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        // A watch face has no room for a table of eleven outlines.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=FlowChartShapes status=SKIPPED reason=flow-chart-shapes-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "FlowChartShapes";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    private static final String[] SHAPES = {
        GraphShapes.RECT, GraphShapes.ROUNDED, GraphShapes.TERMINATOR, GraphShapes.DECISION,
        GraphShapes.DATA, GraphShapes.DOCUMENT, GraphShapes.PREPARATION, GraphShapes.DATABASE,
        GraphShapes.SUBPROCESS, GraphShapes.ELLIPSE, GraphShapes.NOTE,
    };

    private static final int[] ARROWS = {
        GraphEdge.ARROW_FILLED, GraphEdge.ARROW_OPEN, GraphEdge.ARROW_DIAMOND, GraphEdge.ARROW_CIRCLE,
        GraphEdge.ARROW_NONE,
    };

    private static final int[] LINES = {GraphEdge.LINE_SOLID, GraphEdge.LINE_DASHED, GraphEdge.LINE_DOTTED};

    @Override
    protected void populate(Form form, String suffix) {
        DefaultGraphModel model = new DefaultGraphModel();
        String[] lanes = {"one", "two", "three"};
        for (int l = 0; l < lanes.length; l++) {
            model.addGroup(GraphGroup.lane(lanes[l], "Lane " + (l + 1), false, l * 170, 0, 170, 420));
        }
        // four rows of three, the last row one short
        for (int i = 0; i < SHAPES.length; i++) {
            int col = i % 3;
            int row = i / 3;
            GraphNode n = new GraphNode("s" + i, SHAPES[i], col * 170 + 30, 50 + row * 95, 110, 54)
                    .setShape(SHAPES[i]);
            n.setGroupId(lanes[col]);
            model.addNode(n);
        }
        // down each lane, and across the rows: each edge another line and arrow
        int k = 0;
        for (int i = 0; i + 3 < SHAPES.length; i++) {
            GraphEdge e = model.addEdge("s" + i, "s" + (i + 3));
            e.setLineStyle(LINES[k % LINES.length]);
            e.setTargetArrow(ARROWS[k % ARROWS.length]);
            k++;
        }
        for (int i = 0; i + 1 < SHAPES.length; i += 3) {
            GraphEdge e = model.addEdge("s" + i, "s" + (i + 1));
            e.setLineStyle(LINES[k % LINES.length]);
            e.setSourceArrow(ARROWS[(k + 2) % ARROWS.length]);
            e.setTargetArrow(ARROWS[k % ARROWS.length]);
            k++;
        }

        NodeGraph graph = new NodeGraph(model);
        graph.setGridVisible(false);
        graph.setResizable(true);
        graph.select(model.getNode("s4"));
        form.add(BorderLayout.CENTER, graph);
        graph.fitToView();
    }
}
