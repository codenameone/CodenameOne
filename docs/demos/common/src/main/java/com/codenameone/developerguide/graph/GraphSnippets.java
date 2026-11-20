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
package com.codenameone.developerguide.graph;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphJson;
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.graph.GraphShapes;
import com.codename1.graph.GraphPort;
import com.codename1.graph.arrange.ForceArrangement;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.graph.arrange.RadialArrangement;
import com.codename1.graph.arrange.TreeArrangement;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.graph.GraphAdapter;
import com.codename1.ui.graph.GraphPalette;
import com.codename1.ui.graph.NodeComponentFactory;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.graph.PortEditorFactory;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.UndoHistory;

import java.io.IOException;

public class GraphSnippets {
    public void firstGraph() {
        // tag::graph-first[]
        DefaultGraphModel model = new DefaultGraphModel();
        model.addNode(new GraphNode("web", "Web app"));
        model.addNode(new GraphNode("api", "API gateway"));
        model.addNode(new GraphNode("orders", "Orders"));
        model.addNode(new GraphNode("billing", "Billing"));
        model.addNode(new GraphNode("db", "Database"));
        model.addEdge("web", "api");
        model.addEdge("api", "orders");
        model.addEdge("api", "billing");
        model.addEdge("orders", "db");
        model.addEdge("billing", "db");

        NodeGraph graph = new NodeGraph(model);
        graph.arrange(new LayeredArrangement(), false);

        Form form = new Form("Services", new BorderLayout());
        form.add(BorderLayout.CENTER, graph);
        form.show();
        graph.fitToView();
        // end::graph-first[]
    }

    public void groupsAndLabels() {
        // tag::graph-groups[]
        DefaultGraphModel model = new DefaultGraphModel();
        model.addNode(new GraphNode("orders", "Orders"));
        model.addNode(new GraphNode("billing", "Billing"));
        model.addNode(new GraphNode("db", "Database"));

        model.addGroup(new GraphGroup("backend", "Backend"));
        model.getNode("orders").setGroupId("backend");
        model.getNode("billing").setGroupId("backend");

        GraphEdge write = model.addEdge("orders", "db");
        write.setLabel("writes");
        model.getNode("db").setStyleKey("storage");
        // end::graph-groups[]
    }

    public void arrangements() {
        // tag::graph-arrangements[]
        DefaultGraphModel model = new DefaultGraphModel();
        model.addNode(new GraphNode("ceo", "CEO"));
        model.addNode(new GraphNode("cto", "CTO"));
        model.addNode(new GraphNode("cfo", "CFO"));
        model.addEdge("ceo", "cto");
        model.addEdge("ceo", "cfo");
        NodeGraph graph = new NodeGraph(model);

        LayeredArrangement layered = new LayeredArrangement();
        layered.setDirection(LayeredArrangement.LEFT_TO_RIGHT);
        graph.arrange(layered, true);

        TreeArrangement tree = new TreeArrangement();
        tree.setRootId("ceo");
        graph.arrange(tree, true);

        graph.arrange(new RadialArrangement(), true);

        ForceArrangement force = new ForceArrangement();
        force.setIdealLength(140);
        graph.arrange(force, true);
        // end::graph-arrangements[]
    }

    public void liveArrangement() {
        // tag::graph-live[]
        NodeGraph graph = new NodeGraph();
        graph.setLiveArrangement(new ForceArrangement());
        // end::graph-live[]
    }

    public void nodeEditor() {
        // tag::graph-editor[]
        DefaultGraphModel model = new DefaultGraphModel();
        model.addNode(new GraphNode("noise", "Noise", 0, 0, 160, 90)
                .addOutput("out", "Value", "float"));
        model.addNode(new GraphNode("color", "Color ramp", 240, 0, 160, 90)
                .addInput("in", "Factor", "float")
                .addOutput("out", "Color", "color"));
        model.addNode(new GraphNode("material", "Material", 480, 0, 160, 110)
                .addInput("base", "Base color", "color")
                .addInput("rough", "Roughness", "float"));

        NodeGraph graph = new NodeGraph(model);
        graph.setEditingEnabled(true);
        graph.setGridVisible(true);
        graph.setSnap(20);
        graph.setEdgeStyle(NodeGraph.EDGE_CURVED);
        // end::graph-editor[]

        // tag::graph-veto[]
        NodeGraph editor = new NodeGraph();
        editor.addGraphListener(new GraphAdapter() {
            @Override
            public boolean edgeCreating(NodeGraph g, GraphNode source, GraphPort sourcePort,
                    GraphNode target, GraphPort targetPort) {
                return !"material".equals(source.getId());
            }

            @Override
            public void edgeCreated(NodeGraph g, GraphEdge edge) {
                Log.p("Connected " + edge.getSourceId() + " to " + edge.getTargetId());
            }
        });
        // end::graph-veto[]
    }

    public void embedded() {
        // tag::graph-components[]
        NodeGraph graph = new NodeGraph();
        graph.setNodeComponentFactory(new NodeComponentFactory() {
            @Override
            public Component createComponent(NodeGraph g, GraphNode node) {
                if (!"material".equals(node.getId())) {
                    return null;
                }
                return new Slider();
            }
        });
        // end::graph-components[]
    }

    public NodeGraph flowChart() {
        // tag::graph-flowchart[]
        DefaultGraphModel model = new DefaultGraphModel();
        model.addGroup(GraphGroup.lane("shop", "Shop", true, 0, 0, 620, 150));
        model.addGroup(GraphGroup.lane("warehouse", "Warehouse", true, 0, 150, 620, 150));

        GraphNode start = new GraphNode("start", "Order placed", 50, 48, 110, 54)
                .setShape(GraphShapes.TERMINATOR);
        start.setGroupId("shop");
        model.addNode(start);
        GraphNode paid = new GraphNode("paid", "Paid?", 220, 40, 110, 70)
                .setShape(GraphShapes.DECISION);
        paid.setGroupId("shop");
        model.addNode(paid);
        GraphNode cancel = new GraphNode("cancel", "Cancelled", 400, 48, 110, 54)
                .setShape(GraphShapes.TERMINATOR);
        cancel.setGroupId("shop");
        model.addNode(cancel);
        GraphNode stock = new GraphNode("stock", "Stock", 50, 198, 110, 54)
                .setShape(GraphShapes.DATABASE);
        stock.setGroupId("warehouse");
        model.addNode(stock);
        GraphNode pick = new GraphNode("pick", "Pick items", 220, 198, 110, 54)
                .setShape(GraphShapes.SUBPROCESS);
        pick.setGroupId("warehouse");
        model.addNode(pick);
        GraphNode ship = new GraphNode("ship", "Ship", 400, 198, 110, 54)
                .setShape(GraphShapes.DATA);
        ship.setGroupId("warehouse");
        model.addNode(ship);

        model.addEdge("start", "paid");
        model.addEdge("paid", "cancel").setLabel("no");
        model.addEdge("paid", "pick").setLabel("yes");
        model.addEdge("pick", "ship");
        GraphEdge reads = model.addEdge("stock", "pick");
        reads.setLineStyle(GraphEdge.LINE_DASHED);
        reads.setTargetArrow(GraphEdge.ARROW_OPEN);

        NodeGraph graph = new NodeGraph(model);
        graph.setEdgeStyle(NodeGraph.EDGE_ORTHOGONAL);
        graph.setEditingEnabled(true);
        graph.setResizable(true);
        // end::graph-flowchart[]
        return graph;
    }

    public Container palette() {
        // tag::graph-palette[]
        NodeGraph graph = new NodeGraph();
        GraphPalette palette = new GraphPalette(graph);
        palette.add(new GraphNodeTemplate("Step").setShape(GraphShapes.RECT));
        palette.add(new GraphNodeTemplate("Decision")
                .setShape(GraphShapes.DECISION).setSize(110, 70));
        palette.add(new GraphNodeTemplate("Stored data").setShape(GraphShapes.DATABASE));
        palette.add(new GraphNodeTemplate("Multiply")
                .addInput("a", "A", "number")
                .addInput("b", "B", "number")
                .addOutput("out", "Result", "number"));

        Container editor = new Container(new BorderLayout());
        editor.add(BorderLayout.WEST, palette);
        editor.add(BorderLayout.CENTER, graph);
        // end::graph-palette[]
        return editor;
    }

    public void saveAndLoad() {
        // tag::graph-json[]
        NodeGraph graph = new NodeGraph();
        Preferences.set("diagram", GraphJson.toJson(graph.getModel()));

        String saved = Preferences.get("diagram", null);
        if (saved != null) {
            try {
                graph.setModel(GraphJson.fromJson(saved));
            } catch (IOException notAGraph) {
                Log.e(notAGraph);
            }
        }
        // end::graph-json[]
    }

    public Container undoAndClipboard() {
        // tag::graph-undo[]
        final NodeGraph graph = new NodeGraph();
        // every edit goes into the application's history; one button serves
        // the graph and any other component that records into it
        UndoHistory history = UndoHistory.getGlobal();
        Button undo = new Button("Undo");
        undo.addActionListener(e -> graph.undo());
        history.addListener(h -> undo.setEnabled(h.canUndo()));
        Button redo = new Button("Redo");
        redo.addActionListener(e -> graph.redo());
        Button copy = new Button("Copy");
        copy.addActionListener(e -> graph.copySelection());
        Button paste = new Button("Paste");
        paste.addActionListener(e -> graph.paste());
        Button duplicate = new Button("Duplicate");
        duplicate.addActionListener(e -> graph.duplicateSelection());
        // end::graph-undo[]
        return BoxLayout.encloseX(undo, redo, copy, paste, duplicate);
    }

    public void portValues() {
        // tag::graph-port-values[]
        NodeGraph graph = new NodeGraph();
        graph.setPortEditorFactory(new PortEditorFactory() {
            @Override
            public Component createEditor(final NodeGraph g, GraphNode node, final GraphPort port) {
                if (!"number".equals(port.getType())) {
                    return null;
                }
                final TextField field = new TextField(String.valueOf(port.getValue()), "", 4, TextField.DECIMAL);
                field.addActionListener(e -> g.setPortValue(port, Double.valueOf(field.getAsDouble(0))));
                return field;
            }
        });
        // end::graph-port-values[]
    }
}
