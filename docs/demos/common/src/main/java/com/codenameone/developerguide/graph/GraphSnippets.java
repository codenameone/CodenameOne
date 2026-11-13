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
import com.codename1.graph.GraphPort;
import com.codename1.graph.arrange.ForceArrangement;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.graph.arrange.RadialArrangement;
import com.codename1.graph.arrange.TreeArrangement;
import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.graph.GraphAdapter;
import com.codename1.ui.graph.NodeComponentFactory;
import com.codename1.ui.graph.NodeGraph;

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

}
