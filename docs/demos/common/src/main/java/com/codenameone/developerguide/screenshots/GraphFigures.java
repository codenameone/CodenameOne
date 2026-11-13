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
package com.codenameone.developerguide.screenshots;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphNode;
import com.codename1.graph.layout.ForceLayout;
import com.codename1.graph.layout.GraphLayout;
import com.codename1.graph.layout.LayeredLayout;
import com.codename1.graph.layout.RadialLayout;
import com.codename1.graph.layout.TreeLayout;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

/// The figures of the node graph chapter. Every layout is deterministic, and
/// the force layout is run to completion before the picture is taken, so a
/// figure is the same picture every run.
public final class GraphFigures {
    private GraphFigures() {
    }

    private interface Scene {
        NodeGraph build();
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;

        Fig(String id, Scene scene) {
            this.id = id;
            this.scene = scene;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            Form f = new Form("Graph", new BorderLayout());
            f.add(BorderLayout.CENTER, scene.build());
            return f;
        }

        @Override
        public void afterShow(Form form) {
            NodeGraph g = (NodeGraph) ((BorderLayout) form.getContentPane().getLayout())
                    .getCenter();
            g.fitToView();
            form.getContentPane().repaint();
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();
        v.add(landscape("graph-services", () -> laid(services(), new LayeredLayout())));
        v.add(landscape("graph-groups", GraphFigures::grouped));
        v.add(landscape("graph-tree", () -> laid(org(), new TreeLayout())));
        v.add(landscape("graph-radial", () -> laid(small(org()), new RadialLayout())));
        v.add(landscape("graph-force", GraphFigures::force));
        v.add(landscape("graph-editor", GraphFigures::editor));
        v.add(landscape("graph-components", GraphFigures::embedded));
        Fig dark = new Fig("graph-services-appearance", () -> laid(services(), new LayeredLayout()));
        v.add(new FigureVariant(dark, FigureDevice.ANDROID, true, true, "graph-services-dark.png"));
        v.add(new FigureVariant(dark, FigureDevice.IOS, false, true, "graph-services-ios.png"));
        return v.toArray(new FigureVariant[v.size()]);
    }

    private static FigureVariant landscape(String name, Scene scene) {
        return new FigureVariant(new Fig(name, scene), FigureDevice.ANDROID, false, true, name + ".png");
    }

    private static NodeGraph laid(DefaultGraphModel model, GraphLayout layout) {
        NodeGraph g = new NodeGraph(model);
        g.applyLayout(layout, false);
        return g;
    }

    private static DefaultGraphModel services() {
        DefaultGraphModel m = new DefaultGraphModel();
        String[][] nodes = {
            {"web", "Web app"}, {"mobile", "Mobile app"}, {"api", "API gateway"},
            {"auth", "Auth"}, {"orders", "Orders"}, {"billing", "Billing"},
            {"search", "Search"}, {"db", "Orders DB"}, {"queue", "Event queue"},
            {"mail", "Mailer"}
        };
        for (String[] n : nodes) {
            m.addNode(new GraphNode(n[0], n[1]));
        }
        String[][] edges = {
            {"web", "api"}, {"mobile", "api"}, {"api", "auth"}, {"api", "orders"},
            {"api", "billing"}, {"api", "search"}, {"orders", "db"}, {"billing", "db"},
            {"orders", "queue"}, {"queue", "mail"}, {"search", "db"}
        };
        for (String[] e : edges) {
            m.addEdge(e[0], e[1]);
        }
        m.getNode("db").setStyleKey("storage");
        m.getNode("queue").setStyleKey("storage");
        return m;
    }

    private static NodeGraph grouped() {
        DefaultGraphModel m = services();
        m.addGroup(new GraphGroup("clients", "Clients"));
        m.addGroup(new GraphGroup("backend", "Backend"));
        m.getNode("web").setGroupId("clients");
        m.getNode("mobile").setGroupId("clients");
        for (String id : new String[] {"auth", "orders", "billing", "search"}) {
            m.getNode(id).setGroupId("backend");
        }
        for (GraphEdge e : m.getEdges()) {
            if ("db".equals(e.getTargetId())) {
                e.setLabel("SQL");
            }
        }
        NodeGraph g = laid(m, new LayeredLayout());
        g.setEdgeStyle(NodeGraph.EDGE_ORTHOGONAL);
        return g;
    }

    private static DefaultGraphModel org() {
        DefaultGraphModel m = new DefaultGraphModel();
        String[][] people = {
            {"ceo", "CEO", null}, {"cto", "CTO", "ceo"}, {"cfo", "CFO", "ceo"},
            {"coo", "COO", "ceo"}, {"eng", "Engineering", "cto"}, {"design", "Design", "cto"},
            {"it", "IT", "cto"}, {"acct", "Accounting", "cfo"}, {"legal", "Legal", "cfo"},
            {"ops", "Operations", "coo"}, {"support", "Support", "coo"},
            {"mobile", "Mobile", "eng"}, {"server", "Server", "eng"}
        };
        for (String[] p : people) {
            m.addNode(new GraphNode(p[0], p[1]));
            if (p[2] != null) {
                m.addEdge(p[2], p[0]);
            }
        }
        return m;
    }

    private static NodeGraph force() {
        ForceLayout layout = new ForceLayout();
        layout.setIdealLength(110);
        return laid(network(), layout);
    }

    private static DefaultGraphModel small(DefaultGraphModel m) {
        for (GraphNode n : m.getNodes()) {
            n.setSize(110, 40);
        }
        return m;
    }

    private static DefaultGraphModel network() {
        DefaultGraphModel m = new DefaultGraphModel();
        String[] names = {"core", "edge", "db", "web 1", "web 2", "web 3", "cache", "api 1", "api 2",
            "auth", "jobs", "pg 1", "pg 2", "pg 3", "backup"};
        for (int i = 0; i < names.length; i++) {
            m.addNode(new GraphNode("n" + i, names[i], 0, 0, 90, 36));
        }
        // three switches, their hosts, and links between the switches
        int[][] links = {
            {0, 1}, {0, 2}, {1, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {1, 7}, {1, 8},
            {1, 9}, {1, 10}, {2, 11}, {2, 12}, {2, 13}, {2, 14}, {3, 4}, {9, 10},
            {13, 14}, {6, 7}, {11, 12}
        };
        for (int[] l : links) {
            GraphEdge e = m.addEdge("n" + l[0], "n" + l[1]);
            e.setDirected(false);
        }
        return m;
    }

    private static DefaultGraphModel shader() {
        DefaultGraphModel m = new DefaultGraphModel();
        m.addNode(new GraphNode("noise", "Noise", 0, 40, 160, 90)
                .addOutput("out", "Value", "float"));
        m.addNode(new GraphNode("ramp", "Color ramp", 240, 0, 160, 90)
                .addInput("in", "Factor", "float")
                .addOutput("out", "Color", "color"));
        m.addNode(new GraphNode("rough", "Roughness", 240, 140, 160, 70)
                .addOutput("out", "Value", "float"));
        m.addNode(new GraphNode("material", "Material", 480, 40, 170, 110)
                .addInput("base", "Base color", "color")
                .addInput("rough", "Roughness", "float"));
        m.connect(m.getNode("noise"), m.getNode("noise").getPort("out"),
                m.getNode("ramp"), m.getNode("ramp").getPort("in"));
        m.connect(m.getNode("ramp"), m.getNode("ramp").getPort("out"),
                m.getNode("material"), m.getNode("material").getPort("base"));
        m.connect(m.getNode("rough"), m.getNode("rough").getPort("out"),
                m.getNode("material"), m.getNode("material").getPort("rough"));
        return m;
    }

    private static NodeGraph editor() {
        NodeGraph g = new NodeGraph(shader());
        g.setGridVisible(true);
        g.setEdgeStyle(NodeGraph.EDGE_CURVED);
        g.select(g.getModel().getNode("ramp"));
        return g;
    }

    private static NodeGraph embedded() {
        DefaultGraphModel m = shader();
        m.getNode("rough").setSize(160, 110);
        NodeGraph g = new NodeGraph(m);
        g.setGridVisible(true);
        g.setEdgeStyle(NodeGraph.EDGE_CURVED);
        g.setNodeComponentFactory((graph, node) -> {
            if (!"rough".equals(node.getId())) {
                return null;
            }
            Slider s = new Slider();
            s.setEditable(true);
            s.setProgress(40);
            return (Component) s;
        });
        return g;
    }
}
