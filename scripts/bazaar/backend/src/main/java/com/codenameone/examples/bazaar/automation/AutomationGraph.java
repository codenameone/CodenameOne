/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.automation;

import com.codename1.backend.Json;
import com.codenameone.examples.bazaar.api.NodeTypeDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An automation's graph as `com.codename1.graph.GraphJson` writes it, read
/// with the backend's JSON parser: nodes with `id`, `style`, `data`,
/// `inputs` and `outputs` (ports with `id`, `label` and `value`), and
/// edges with `from`, `fromPort`, `to` and `toPort`.
///
/// Only what the engine needs is kept. Coordinates, groups and edge styling
/// are the editor's.
public final class AutomationGraph {
    /// A node: its id, its type and its parameters.
    public static final class Node {
        public final String id;
        public final String type;
        final Map data;
        final List<Map> inputs;
        final List<Map> outputs;
        final List<Edge> out = new ArrayList<Edge>();
        int incoming;

        Node(String id, String type, Map data, List<Map> inputs, List<Map> outputs) {
            this.id = id;
            this.type = type;
            this.data = data;
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }

    /// An edge from an output port to an input port.
    public static final class Edge {
        final String id;
        final String from;
        final String fromPort;
        final String to;
        final String toPort;

        Edge(String id, String from, String fromPort, String to, String toPort) {
            this.id = id;
            this.from = from;
            this.fromPort = fromPort;
            this.to = to;
            this.toPort = toPort;
        }
    }

    final Map<String, Node> nodes = new LinkedHashMap<String, Node>();
    final List<Edge> edges = new ArrayList<Edge>();
    final List<String> errors = new ArrayList<String>();

    private AutomationGraph() {
    }

    /// Reads `json`; what cannot be read is listed in [#errors()], never
    /// thrown.
    public static AutomationGraph parse(String json) {
        AutomationGraph g = new AutomationGraph();
        Map root;
        try {
            root = json == null || json.trim().length() == 0 ? null : Json.parseObject(json);
        } catch (IOException | RuntimeException bad) {
            root = null;
        }
        if (root == null) {
            g.errors.add("The graph is not a JSON document");
            return g;
        }
        List nodeList = list(root.get("nodes"));
        for (int iter = 0; iter < nodeList.size(); iter++) {
            if (!(nodeList.get(iter) instanceof Map)) {
                g.errors.add("Node " + (iter + 1) + " is not an object");
                continue;
            }
            Map m = (Map) nodeList.get(iter);
            String id = text(m.get("id"));
            if (id.length() == 0) {
                g.errors.add("Node " + (iter + 1) + " has no id");
                continue;
            }
            if (g.nodes.containsKey(id)) {
                g.errors.add("Two nodes are called " + id);
                continue;
            }
            Map data = data(m.get("data"));
            String type = text(data.get("type"));
            if (type.length() == 0) {
                type = text(m.get("style"));
            }
            if (type.length() == 0) {
                type = text(m.get("type"));
            }
            g.nodes.put(id, new Node(id, type, data, maps(m.get("inputs")),
                    maps(m.get("outputs"))));
        }
        List edgeList = list(root.get("edges"));
        for (int iter = 0; iter < edgeList.size(); iter++) {
            if (!(edgeList.get(iter) instanceof Map)) {
                g.errors.add("Edge " + (iter + 1) + " is not an object");
                continue;
            }
            Map m = (Map) edgeList.get(iter);
            String id = text(m.get("id"));
            g.edges.add(new Edge(id.length() == 0 ? Integer.toString(iter + 1) : id,
                    text(m.get("from")), text(m.get("fromPort")), text(m.get("to")),
                    text(m.get("toPort"))));
        }
        return g;
    }

    /// The node's parameter `name`: an entry of its `data` (or of
    /// `data.params`), else the value of its input port of that name, else
    /// the catalog's default, else "".
    /// What joins a parameter's name to a language in a message step's
    /// translation: `body@es` is the merchant's Spanish text of `body`. The
    /// translations travel inside the node, saved with the graph and gone
    /// with the step.
    public static final String VARIANT = "@";

    /// The language a message step goes out in to a reader of `lang`: `lang`
    /// when the merchant translated every one of `needed` into it, else null,
    /// meaning the original. Whole or nothing, so a reader never gets a
    /// translated body under an untranslated subject.
    public static String variantLanguage(Node node, String lang, String... needed) {
        if (lang == null || lang.length() == 0) {
            return null;
        }
        for (int iter = 0; iter < needed.length; iter++) {
            if (param(node, needed[iter] + VARIANT + lang).length() == 0) {
                return null;
            }
        }
        return lang;
    }

    /// `name` in `lang` (an answer of [#variantLanguage]), or the original
    /// when `lang` is null.
    public static String param(Node node, String name, String lang) {
        return lang == null ? param(node, name) : param(node, name + VARIANT + lang);
    }

    public static String param(Node node, String name) {
        Object v = node.data.get(name);
        if (v == null && node.data.get("params") instanceof Map) {
            v = ((Map) node.data.get("params")).get(name);
        }
        if (v == null) {
            for (int iter = 0; iter < node.inputs.size(); iter++) {
                if (name.equals(text(node.inputs.get(iter).get("id")))) {
                    v = node.inputs.get(iter).get("value");
                }
            }
        }
        if (v == null) {
            NodeTypeDto t = NodeCatalog.find(node.type);
            for (int iter = 0; t != null && iter < t.params().size(); iter++) {
                if (name.equals(t.params().get(iter).name())) {
                    v = t.params().get(iter).defaultValue();
                }
            }
        }
        if (v instanceof Number && ((Number) v).doubleValue() == ((Number) v).longValue()) {
            return Long.toString(((Number) v).longValue());
        }
        return v == null ? "" : v.toString().trim();
    }

    /// [#param] as a whole number, `fallback` when it is not one.
    public static long number(Node node, String name, long fallback) {
        String text = param(node, name);
        try {
            return text.length() == 0 ? fallback : Long.parseLong(text);
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    public List<String> errors() {
        return errors;
    }

    /// The trigger nodes.
    public List<Node> triggers() {
        List<Node> out = new ArrayList<Node>();
        for (Node n : nodes.values()) {
            if (NodeCatalog.isTrigger(n.type)) {
                out.add(n);
            }
        }
        return out;
    }

    /// The type of the first trigger, or "".
    public String triggerKind() {
        List<Node> t = triggers();
        return t.isEmpty() ? "" : t.get(0).type;
    }

    /// Every node, in document order.
    public List<Node> nodes() {
        return new ArrayList<Node>(nodes.values());
    }

    public Node node(String id) {
        return nodes.get(id);
    }

    /// The nodes an edge from `node` leads to. For a condition only the edges
    /// leaving its `yes` (or `no`) port count.
    public List<Node> next(Node node, String branch) {
        List<Node> out = new ArrayList<Node>();
        for (int iter = 0; iter < edges.size(); iter++) {
            Edge e = edges.get(iter);
            if (!e.from.equals(node.id)) {
                continue;
            }
            if (branch != null && !branch.equals(portName(node.outputs, e.fromPort))) {
                continue;
            }
            Node to = nodes.get(e.to);
            if (to != null) {
                out.add(to);
            }
        }
        return out;
    }

    /// What `port` stands for: `yes` or `no` when its id or label says so,
    /// else its id.
    static String portName(List<Map> ports, String port) {
        if (NodeCatalog.YES.equalsIgnoreCase(port) || NodeCatalog.NO.equalsIgnoreCase(port)) {
            return NodeCatalog.YES.equalsIgnoreCase(port) ? NodeCatalog.YES : NodeCatalog.NO;
        }
        for (int iter = 0; iter < ports.size(); iter++) {
            if (port.equals(text(ports.get(iter).get("id")))) {
                String label = text(ports.get(iter).get("label"));
                if (NodeCatalog.YES.equalsIgnoreCase(label)) {
                    return NodeCatalog.YES;
                }
                if (NodeCatalog.NO.equalsIgnoreCase(label)) {
                    return NodeCatalog.NO;
                }
            }
        }
        return port;
    }

    /// Everything wrong with the graph: what could not be read, no trigger or
    /// triggers of two kinds, unknown types, edges to missing nodes or ports,
    /// edges into a trigger, a condition edge that is neither yes nor no, a
    /// node no trigger reaches, a missing required parameter, and cycles.
    public List<String> validate() {
        List<String> out = new ArrayList<String>(errors);
        if (!errors.isEmpty()) {
            return out;
        }
        if (nodes.isEmpty()) {
            out.add("The automation has no steps");
            return out;
        }
        List<Node> triggers = triggers();
        if (triggers.isEmpty()) {
            out.add("The automation has no trigger");
        }
        for (int iter = 1; iter < triggers.size(); iter++) {
            if (!triggers.get(iter).type.equals(triggers.get(0).type)) {
                out.add("Every trigger must be of one kind");
                break;
            }
        }
        for (Node n : nodes.values()) {
            n.out.clear();
            n.incoming = 0;
            if (NodeCatalog.find(n.type) == null) {
                out.add("Step " + n.id + " has an unknown type '" + n.type + "'");
            }
        }
        for (int iter = 0; iter < edges.size(); iter++) {
            Edge e = edges.get(iter);
            Node from = nodes.get(e.from);
            Node to = nodes.get(e.to);
            if (from == null || to == null) {
                out.add("Connection " + e.id + " leads to a step that is not there");
                continue;
            }
            if (!hasPort(from, from.outputs, e.fromPort, false)) {
                out.add("Connection " + e.id + " leaves " + from.id + " from port '" + e.fromPort
                        + "', which it does not have");
            }
            if (!hasPort(to, to.inputs, e.toPort, true)) {
                out.add("Connection " + e.id + " enters " + to.id + " at port '" + e.toPort
                        + "', which it does not have");
            }
            if (NodeCatalog.isTrigger(to.type)) {
                out.add("Connection " + e.id + " leads into the trigger " + to.id);
            }
            if (NodeCatalog.isCondition(from.type)) {
                String branch = portName(from.outputs, e.fromPort);
                if (!NodeCatalog.YES.equals(branch) && !NodeCatalog.NO.equals(branch)) {
                    out.add("Condition " + from.id + " leads on from '" + e.fromPort
                            + "'; a condition leads on from yes or no");
                }
            }
            from.out.add(e);
            to.incoming++;
        }
        for (Node n : nodes.values()) {
            if (!NodeCatalog.isTrigger(n.type) && n.incoming == 0
                    && NodeCatalog.find(n.type) != null) {
                out.add("Step " + n.id + " is not connected to anything before it");
            }
            String missing = missingParam(n);
            if (missing != null) {
                out.add("Step " + n.id + " needs " + missing);
            }
        }
        String cycle = cycle();
        if (cycle != null) {
            out.add("The steps loop back through " + cycle);
        }
        return out;
    }

    private static String missingParam(Node n) {
        if (NodeCatalog.IF_TOTAL_OVER.equals(n.type) && number(n, "amount", -1) < 0) {
            return "an amount";
        }
        if ((NodeCatalog.TAG_ORDER.equals(n.type) || NodeCatalog.TAG_CUSTOMER.equals(n.type)
                || NodeCatalog.IF_CUSTOMER_TAG.equals(n.type)
                || NodeCatalog.IF_PRODUCT_TAG.equals(n.type)) && param(n, "tag").length() == 0) {
            return "a tag";
        }
        if (NodeCatalog.WEBHOOK_SEND.equals(n.type) && param(n, "webhookId").length() == 0) {
            return "a webhook";
        }
        if (NodeCatalog.WAIT_MINUTES.equals(n.type) && number(n, "minutes", 0) <= 0) {
            return "a number of minutes";
        }
        return null;
    }

    /// Whether `port` is one the node has: one it declares, or, when it
    /// declares none, one its type has. A blank port is the node's only one.
    private static boolean hasPort(Node n, List<Map> declared, String port, boolean input) {
        if (port.length() == 0) {
            return true;
        }
        if (!declared.isEmpty()) {
            for (int iter = 0; iter < declared.size(); iter++) {
                if (port.equals(text(declared.get(iter).get("id")))) {
                    return true;
                }
            }
            return false;
        }
        NodeTypeDto t = NodeCatalog.find(n.type);
        return t != null && (input ? t.inputs() : t.outputs()).contains(port);
    }

    /// The id of a node on a cycle, or null; a depth-first walk, colouring
    /// nodes white (absent), grey (1, on the path) and black (2, done).
    private String cycle() {
        Map<String, Integer> colour = new HashMap<String, Integer>();
        for (String start : nodes.keySet()) {
            if (colour.containsKey(start)) {
                continue;
            }
            List<String> stack = new ArrayList<String>();
            List<Integer> at = new ArrayList<Integer>();
            stack.add(start);
            at.add(Integer.valueOf(0));
            colour.put(start, Integer.valueOf(1));
            while (!stack.isEmpty()) {
                int top = stack.size() - 1;
                Node n = nodes.get(stack.get(top));
                int i = at.get(top).intValue();
                if (i >= n.out.size()) {
                    colour.put(n.id, Integer.valueOf(2));
                    stack.remove(top);
                    at.remove(top);
                    continue;
                }
                at.set(top, Integer.valueOf(i + 1));
                String to = n.out.get(i).to;
                Integer c = colour.get(to);
                if (c == null) {
                    colour.put(to, Integer.valueOf(1));
                    stack.add(to);
                    at.add(Integer.valueOf(0));
                } else if (c.intValue() == 1) {
                    return to;
                }
            }
        }
        return null;
    }

    private static List list(Object o) {
        return o instanceof List ? (List) o : new ArrayList();
    }

    private static List<Map> maps(Object o) {
        List<Map> out = new ArrayList<Map>();
        List l = list(o);
        for (int iter = 0; iter < l.size(); iter++) {
            if (l.get(iter) instanceof Map) {
                out.add((Map) l.get(iter));
            }
        }
        return out;
    }

    /// A node's `data`: a map, or JSON text of one, or nothing.
    private static Map data(Object o) {
        if (o instanceof Map) {
            return (Map) o;
        }
        if (o instanceof String && ((String) o).trim().startsWith("{")) {
            try {
                return Json.parseObject((String) o);
            } catch (IOException | RuntimeException notJson) {
                return new HashMap();
            }
        }
        return new HashMap();
    }

    static String text(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
