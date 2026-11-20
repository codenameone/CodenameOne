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
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.graph.GraphPort;
import com.codename1.graph.GraphShapes;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.graph.DefaultGraphEdgeRenderer;
import com.codename1.ui.graph.DefaultGraphNodeRenderer;
import com.codename1.ui.graph.GraphPalette;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.graph.PortEditorFactory;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A node editor: a palette of templates beside the graph, typed ports with
/// a colour per type, wires in the colour of what they carry, and the value
/// of each unconnected input shown by its editor.
public class NodeEditorThemeScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        // A watch face has no room for a palette and a graph side by side.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=NodeEditorTheme status=SKIPPED reason=node-editor-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "NodeEditorTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        DefaultGraphModel model = new DefaultGraphModel();
        GraphNode price = model.addNode(new GraphNode("price", "Price", 0, 0, 130, 60)
                .addOutput("out", "Value", "number"));
        GraphNode tint = model.addNode(new GraphNode("tint", "Tint", 0, 110, 130, 60)
                .addOutput("out", "Colour", "color"));
        GraphNode mul = model.addNode(new GraphNode("mul", "Multiply", 200, 0, 150, 90)
                .addInput("a", "A", "number")
                .addInput("b", "B", "number")
                .addOutput("out", "Result", "number"));
        GraphNode paint = model.addNode(new GraphNode("paint", "Material", 420, 40, 150, 110)
                .addInput("amount", "Amount", "number")
                .addInput("color", "Colour", "color")
                .addInput("gloss", "Gloss", "number"));
        mul.getPort("b").setValue(Double.valueOf(1.2));
        paint.getPort("gloss").setValue(Double.valueOf(0.5));
        model.connect(price, price.getPort("out"), mul, mul.getPort("a"));
        model.connect(mul, mul.getPort("out"), paint, paint.getPort("amount"));
        model.connect(tint, tint.getPort("out"), paint, paint.getPort("color"));

        NodeGraph graph = new NodeGraph(model);
        DefaultGraphNodeRenderer nodes = new DefaultGraphNodeRenderer();
        nodes.setAccent("number", 0x1a73e8);
        nodes.setAccent("color", 0xe8710a);
        graph.setNodeRenderer(nodes);
        DefaultGraphEdgeRenderer edges = new DefaultGraphEdgeRenderer();
        edges.setColorByPortType(true);
        graph.setEdgeRenderer(edges);
        // a label, not a text field: a field's caret would blink into the capture
        graph.setPortEditorFactory(new PortEditorFactory() {
            @Override
            public Component createEditor(NodeGraph g, GraphNode node, GraphPort port) {
                return port.getValue() == null ? null : new Label(String.valueOf(port.getValue()));
            }
        });
        graph.select(mul);

        GraphPalette palette = new GraphPalette(graph);
        palette.add(new GraphNodeTemplate("Step").setShape(GraphShapes.RECT));
        palette.add(new GraphNodeTemplate("Decision").setShape(GraphShapes.DECISION).setSize(110, 70));
        palette.add(new GraphNodeTemplate("Data").setShape(GraphShapes.DATABASE));
        palette.add(new GraphNodeTemplate("Multiply")
                .addInput("a", "A", "number")
                .addInput("b", "B", "number")
                .addOutput("out", "Result", "number"));

        form.add(BorderLayout.WEST, palette);
        form.add(BorderLayout.CENTER, graph);
        graph.fitToView();
    }
}
