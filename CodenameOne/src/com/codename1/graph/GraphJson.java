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
package com.codename1.graph;

import com.codename1.io.JSONParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Writes a graph as JSON and reads it back, to save a diagram or send it.
///
/// ```java
/// String saved = GraphJson.toJson(model);
/// Storage.getInstance().writeObject("diagram", saved);
/// // later
/// DefaultGraphModel loaded = GraphJson.fromJson(saved);
/// graph.setModel(loaded);
/// ```
///
/// Everything the model classes hold is written: nodes with their bounds,
/// shape, ports and port values, edges with their line, arrows and route, and
/// groups and lanes. Values left at their default are omitted, so a small
/// graph is a small document.
///
/// `getUserData` is the application's, and is written only through a
/// `UserDataCodec`. A port value is written when it is a string, a number or
/// a boolean.
public final class GraphJson {
    /// Turns the application's `getUserData` objects into JSON values and
    /// back.
    public interface UserDataCodec {
        /// The JSON form of a node's, edge's or group's user data: a `String`,
        /// `Number`, `Boolean`, `Map` or `List` of those, or null to write
        /// nothing.
        Object encode(Object userData);

        /// The user data for a value `encode` produced, as the parser returned
        /// it.
        Object decode(Object json);
    }

    private GraphJson() {
    }

    /// The whole graph as a JSON object, without user data.
    public static String toJson(GraphModel model) {
        return toJson(model, null, null);
    }

    /// The graph, or part of it, as a JSON object.
    ///
    /// #### Parameters
    ///
    /// - `model`: the graph
    /// - `nodes`: the nodes to write, with the edges that run between two of
    ///   them and no groups; null writes everything
    /// - `codec`: writes user data, or null to leave it out
    public static String toJson(GraphModel model, Collection<GraphNode> nodes, UserDataCodec codec) {
        return JSONParser.toJson(toMap(model, nodes, codec));
    }

    /// The same tree `toJson` writes, to embed in a larger document.
    public static Map<String, Object> toMap(GraphModel model, Collection<GraphNode> nodes, UserDataCodec codec) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        Set<String> ids = null;
        List<GraphNode> source = model.getNodes();
        if (nodes != null) {
            ids = new HashSet<String>();
            for (GraphNode n : nodes) {
                ids.add(n.getId());
            }
        }
        List<Object> nodeList = new ArrayList<Object>();
        int count = source.size();
        for (int i = 0; i < count; i++) {
            GraphNode n = source.get(i);
            if (ids == null || ids.contains(n.getId())) {
                nodeList.add(nodeMap(n, codec));
            }
        }
        root.put("nodes", nodeList);
        List<Object> edgeList = new ArrayList<Object>();
        List<GraphEdge> edges = model.getEdges();
        count = edges.size();
        for (int i = 0; i < count; i++) {
            GraphEdge e = edges.get(i);
            if (ids == null || ids.contains(e.getSourceId()) && ids.contains(e.getTargetId())) {
                edgeList.add(edgeMap(e, codec));
            }
        }
        root.put("edges", edgeList);
        if (ids == null) {
            List<Object> groupList = new ArrayList<Object>();
            List<GraphGroup> groups = model.getGroups();
            count = groups.size();
            for (int i = 0; i < count; i++) {
                groupList.add(groupMap(groups.get(i)));
            }
            if (!groupList.isEmpty()) {
                root.put("groups", groupList);
            }
        }
        return root;
    }

    private static void putUserData(Map<String, Object> m, Object userData, UserDataCodec codec) {
        if (codec != null && userData != null) {
            m.put("data", codec.encode(userData));
        }
    }

    private static Map<String, Object> nodeMap(GraphNode n, UserDataCodec codec) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", n.getId());
        m.put("label", n.getLabel());
        m.put("description", n.getDescription());
        m.put("x", Double.valueOf(n.getX()));
        m.put("y", Double.valueOf(n.getY()));
        m.put("w", Double.valueOf(n.getWidth()));
        m.put("h", Double.valueOf(n.getHeight()));
        m.put("shape", n.getShape());
        m.put("group", n.getGroupId());
        m.put("style", n.getStyleKey());
        if (n.isPinned()) {
            m.put("pinned", Boolean.TRUE);
        }
        if (!n.getInputs().isEmpty()) {
            m.put("inputs", portList(n.getInputs()));
        }
        if (!n.getOutputs().isEmpty()) {
            m.put("outputs", portList(n.getOutputs()));
        }
        putUserData(m, n.getUserData(), codec);
        return m;
    }

    private static List<Object> portList(List<GraphPort> ports) {
        List<Object> out = new ArrayList<Object>();
        int n = ports.size();
        for (int i = 0; i < n; i++) {
            GraphPort p = ports.get(i);
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", p.getId());
            m.put("label", p.getLabel());
            m.put("type", p.getType());
            if (p.getMaxConnections() >= 0) {
                m.put("max", Integer.valueOf(p.getMaxConnections()));
            }
            Object v = p.getValue();
            if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                m.put("value", v);
            }
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> edgeMap(GraphEdge e, UserDataCodec codec) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("from", e.getSourceId());
        m.put("fromPort", e.getSourcePortId());
        m.put("to", e.getTargetId());
        m.put("toPort", e.getTargetPortId());
        m.put("label", e.getLabel());
        m.put("style", e.getStyleKey());
        if (!e.isDirected()) {
            m.put("directed", Boolean.FALSE);
        }
        if (e.getLineStyle() != GraphEdge.LINE_SOLID) {
            m.put("line", Integer.valueOf(e.getLineStyle()));
        }
        if (e.getSourceArrow() != GraphEdge.ARROW_NONE) {
            m.put("fromArrow", Integer.valueOf(e.getSourceArrow()));
        }
        if (e.isTargetArrowSet()) {
            m.put("toArrow", Integer.valueOf(e.getTargetArrow()));
        }
        double[] route = e.getRoute();
        if (route != null && route.length > 0) {
            List<Object> r = new ArrayList<Object>();
            for (double coordinate : route) {
                r.add(Double.valueOf(coordinate));
            }
            m.put("route", r);
        }
        putUserData(m, e.getUserData(), codec);
        return m;
    }

    private static Map<String, Object> groupMap(GraphGroup g) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", g.getId());
        m.put("label", g.getLabel());
        m.put("style", g.getStyleKey());
        if (g.isCollapsed()) {
            m.put("collapsed", Boolean.TRUE);
        }
        if (g.isLane()) {
            m.put("lane", Boolean.TRUE);
            if (!g.isHorizontal()) {
                m.put("horizontal", Boolean.FALSE);
            }
            m.put("x", Double.valueOf(g.getX()));
            m.put("y", Double.valueOf(g.getY()));
            m.put("w", Double.valueOf(g.getWidth()));
            m.put("h", Double.valueOf(g.getHeight()));
        }
        return m;
    }

    /// Reads a graph `toJson` wrote, without user data.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not a JSON object, or a node or an
    ///   edge in it has no id
    public static DefaultGraphModel fromJson(String json) throws IOException {
        DefaultGraphModel model = new DefaultGraphModel();
        read(json, model, null);
        return model;
    }

    /// Reads a graph `toJson` wrote into a model, adding to what it holds.
    ///
    /// #### Parameters
    ///
    /// - `json`: the document
    /// - `into`: the model that receives the nodes, edges and groups
    /// - `codec`: reads user data, or null to ignore it
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not a graph document, or a node or an
    ///   edge in it has no id
    public static void read(String json, DefaultGraphModel into, UserDataCodec codec) throws IOException {
        Map<String, Object> root = json == null ? null : JSONParser.parseJSON(json);
        // the parser answers an array, and text that is no JSON at all, with a
        // map of its own making
        if (root == null || !root.containsKey("nodes") && !root.containsKey("edges") && !root.containsKey("groups")) {
            throw new IOException("Not a graph document");
        }
        readMap(root, into, codec);
    }

    /// Reads the tree `toMap` built, as a JSON parser returns it.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when a node or an edge has no id
    public static void readMap(Map<String, Object> root, DefaultGraphModel into, UserDataCodec codec)
            throws IOException {
        List<Object> groups = list(root.get("groups"));
        for (Object raw : groups) {
            Map<String, Object> m = map(raw);
            GraphGroup g = new GraphGroup(required(m, "id"), string(m, "label"));
            g.setStyleKey(string(m, "style"));
            g.setCollapsed(bool(m.get("collapsed"), false));
            if (bool(m.get("lane"), false)) {
                g.setKind(GraphGroup.KIND_LANE);
                g.setHorizontal(bool(m.get("horizontal"), true));
                g.setBounds(number(m, "x", 0), number(m, "y", 0), number(m, "w", 0), number(m, "h", 0));
            }
            into.addGroup(g);
        }
        List<Object> nodes = list(root.get("nodes"));
        for (Object raw : nodes) {
            Map<String, Object> m = map(raw);
            GraphNode n = new GraphNode(required(m, "id"), string(m, "label"),
                    number(m, "x", 0), number(m, "y", 0), number(m, "w", 160), number(m, "h", 64));
            n.setDescription(string(m, "description"));
            n.setShape(string(m, "shape"));
            n.setGroupId(string(m, "group"));
            n.setStyleKey(string(m, "style"));
            n.setPinned(bool(m.get("pinned"), false));
            readPorts(n, list(m.get("inputs")), GraphPort.INPUT);
            readPorts(n, list(m.get("outputs")), GraphPort.OUTPUT);
            if (codec != null && m.get("data") != null) {
                n.setUserData(codec.decode(m.get("data")));
            }
            into.addNode(n);
        }
        List<Object> edges = list(root.get("edges"));
        for (Object raw : edges) {
            Map<String, Object> m = map(raw);
            GraphEdge e = new GraphEdge(required(m, "id"), required(m, "from"), string(m, "fromPort"),
                    required(m, "to"), string(m, "toPort"));
            e.setLabel(string(m, "label"));
            e.setStyleKey(string(m, "style"));
            e.setDirected(bool(m.get("directed"), true));
            e.setLineStyle((int) number(m, "line", GraphEdge.LINE_SOLID));
            e.setSourceArrow((int) number(m, "fromArrow", GraphEdge.ARROW_NONE));
            e.setTargetArrow((int) number(m, "toArrow", -1));
            List<Object> route = list(m.get("route"));
            if (!route.isEmpty()) {
                double[] r = new double[route.size()];
                for (int j = 0; j < r.length; j++) {
                    Object v = route.get(j);
                    r[j] = v instanceof Number ? ((Number) v).doubleValue() : 0;
                }
                e.setRoute(r);
            }
            if (codec != null && m.get("data") != null) {
                e.setUserData(codec.decode(m.get("data")));
            }
            into.addEdge(e);
        }
    }

    private static void readPorts(GraphNode n, List<Object> ports, int direction) throws IOException {
        for (Object raw : ports) {
            Map<String, Object> m = map(raw);
            GraphPort p = new GraphPort(required(m, "id"), direction, string(m, "label"), string(m, "type"));
            p.setMaxConnections((int) number(m, "max", -1));
            p.setValue(m.get("value"));
            n.addPort(p);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) throws IOException {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new IOException("Not a graph document");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        if (o instanceof List) {
            return (List<Object>) o;
        }
        return new ArrayList<Object>();
    }

    private static String string(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof String ? (String) o : null;
    }

    private static String required(Map<String, Object> m, String key) throws IOException {
        String s = string(m, key);
        if (s == null) {
            throw new IOException("Graph document: missing " + key);
        }
        return s;
    }

    private static double number(Map<String, Object> m, String key, double fallback) {
        Object o = m.get(key);
        return o instanceof Number ? ((Number) o).doubleValue() : fallback;
    }

    /// The parser hands back a Boolean or the word, depending on how the
    /// application configured it.
    private static boolean bool(Object o, boolean fallback) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o instanceof String) {
            return "true".equals(o);
        }
        return fallback;
    }
}
