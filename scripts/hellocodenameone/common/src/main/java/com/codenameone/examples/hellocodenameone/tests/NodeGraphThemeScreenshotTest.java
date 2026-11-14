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
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A service map with a group, a labeled edge, typed ports and a selection,
/// so every themed NodeGraph UIID shows up in one capture per appearance.
public class NodeGraphThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        // A watch face has no room for a diagram of this size.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=NodeGraphTheme status=SKIPPED reason=node-graph-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "NodeGraphTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        DefaultGraphModel model = new DefaultGraphModel();
        model.addNode(new GraphNode("web", "Web app").addOutput("out", "Requests", "http"));
        model.addNode(new GraphNode("api", "API gateway")
                .addInput("in", "Requests", "http")
                .addOutput("out", "Calls", "rpc"));
        model.addNode(new GraphNode("orders", "Orders").addInput("in", "Calls", "rpc"));
        model.addNode(new GraphNode("billing", "Billing").addInput("in", "Calls", "rpc"));
        model.addNode(new GraphNode("db", "Database"));
        model.addGroup(new GraphGroup("backend", "Backend"));
        model.getNode("orders").setGroupId("backend");
        model.getNode("billing").setGroupId("backend");
        model.addEdge("web", "api");
        model.addEdge("api", "orders");
        model.addEdge("api", "billing");
        model.addEdge("orders", "db").setLabel("writes");
        model.addEdge("billing", "db").setLabel("reads");

        NodeGraph graph = new NodeGraph(model);
        graph.setGridVisible(true);
        graph.setEdgeStyle(NodeGraph.EDGE_CURVED);
        graph.arrange(new LayeredArrangement(), false);
        graph.select(model.getNode("api"));
        form.add(BorderLayout.CENTER, graph);
    }
}
