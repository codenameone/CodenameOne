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

/// A flow chart in two swimlanes: every kind of outline an order passes
/// through, a dashed edge, an edge with no arrow and a selected decision, so
/// the shapes, the lanes and the line kinds show up in one capture per
/// appearance.
public class FlowChartThemeScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        // A watch face has no room for a diagram of this size.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=FlowChartTheme status=SKIPPED reason=flow-chart-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "FlowChartTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    private static GraphNode node(DefaultGraphModel model, String id, String label, String shape,
            double x, double y, String lane) {
        GraphNode n = new GraphNode(id, label, x, y, 110, 54).setShape(shape);
        n.setGroupId(lane);
        return model.addNode(n);
    }

    @Override
    protected void populate(Form form, String suffix) {
        DefaultGraphModel model = new DefaultGraphModel();
        model.addGroup(GraphGroup.lane("shop", "Shop", true, 0, 0, 620, 150));
        model.addGroup(GraphGroup.lane("warehouse", "Warehouse", true, 0, 150, 620, 150));
        node(model, "start", "Order placed", GraphShapes.TERMINATOR, 50, 48, "shop");
        node(model, "form", "Order form", GraphShapes.DOCUMENT, 200, 48, "shop");
        node(model, "paid", "Paid?", GraphShapes.DECISION, 350, 40, "shop").setSize(110, 70);
        node(model, "cancel", "Cancelled", GraphShapes.TERMINATOR, 500, 48, "shop");
        node(model, "stock", "Stock", GraphShapes.DATABASE, 50, 198, "warehouse");
        node(model, "pick", "Pick items", GraphShapes.SUBPROCESS, 200, 198, "warehouse");
        node(model, "label", "Print label", GraphShapes.DATA, 350, 198, "warehouse");
        node(model, "ship", "Ship", GraphShapes.PREPARATION, 500, 198, "warehouse");
        model.addEdge("start", "form");
        model.addEdge("form", "paid");
        model.addEdge("paid", "cancel").setLabel("no");
        model.addEdge("paid", "pick").setLabel("yes");
        GraphEdge reads = model.addEdge("stock", "pick");
        reads.setLineStyle(GraphEdge.LINE_DASHED);
        reads.setTargetArrow(GraphEdge.ARROW_OPEN);
        model.addEdge("pick", "label");
        model.addEdge("label", "ship").setSourceArrow(GraphEdge.ARROW_CIRCLE);

        NodeGraph graph = new NodeGraph(model);
        graph.setGridVisible(false);
        graph.setEdgeStyle(NodeGraph.EDGE_ORTHOGONAL);
        graph.select(model.getNode("paid"));
        form.add(BorderLayout.CENTER, graph);
        graph.fitToView();
    }
}
