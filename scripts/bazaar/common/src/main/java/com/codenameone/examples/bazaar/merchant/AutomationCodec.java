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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphJson;
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The automation graph's convention with the server, with nothing that draws.
///
/// A node's style key is its type id, and its user data is a `Map` of its
/// parameters with the type repeated under `type`; `GraphJson` writes that
/// map as the node's `data`. Ports are named `in`, `out`, `yes` and `no` and
/// carry no type, so any output joins any input (the server checks the rest).
/// Parameter values are kept as text, whole numbers for money and counts,
/// because the server reads them with `Long.parseLong`.
final class AutomationCodec implements GraphJson.UserDataCodec {
    /// The kinds of node, as the catalog names them.
    static final String TRIGGER = "TRIGGER";
    static final String CONDITION = "CONDITION";
    static final String ACTION = "ACTION";
    static final String[] KINDS = {TRIGGER, CONDITION, ACTION};
    /// The data key the type is repeated under.
    static final String TYPE = "type";
    static final double WIDTH = 180;
    static final double HEIGHT = 64;

    static final AutomationCodec INSTANCE = new AutomationCodec();

    // tag::bazaar-automation-encode[]
    @Override
    public Object encode(Object userData) {
        if (!(userData instanceof Map)) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Object e : ((Map<?, ?>) userData).entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
            if (entry.getKey() != null && entry.getValue() != null) {
                out.put(entry.getKey().toString(), text(entry.getValue()));
            }
        }
        return out;
    }
    // end::bazaar-automation-encode[]

    @Override
    public Object decode(Object json) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (json instanceof Map) {
            for (Object e : ((Map<?, ?>) json).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
                if (entry.getKey() != null && entry.getValue() != null
                        && !(entry.getValue() instanceof Map) && !(entry.getValue() instanceof List)) {
                    out.put(entry.getKey().toString(), text(entry.getValue()));
                }
            }
        }
        return out;
    }

    /// A value as the server wants to read it: a whole number without
    /// `.0`, anything else as its text.
    static String text(Object v) {
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
            return v.toString();
        }
        return v == null ? "" : v.toString();
    }

    /// The graph as the server stores it.
    static String toJson(GraphModel model) {
        return GraphJson.toJson(model, null, INSTANCE);
    }

    /// Reads a stored graph; an empty or broken one is an empty model.
    static DefaultGraphModel fromJson(String json) {
        DefaultGraphModel model = new DefaultGraphModel();
        if (json == null || json.trim().length() == 0) {
            return model;
        }
        try {
            GraphJson.read(json, model, INSTANCE);
        } catch (IOException broken) {
            model.clear();
        }
        for (GraphNode n : model.getNodes()) {
            Map<String, Object> data = data(n);
            String type = (String) data.get(TYPE);
            if (type == null || type.length() == 0) {
                type = n.getStyleKey();
                if (type != null) {
                    data.put(TYPE, type);
                }
            } else if (n.getStyleKey() == null) {
                n.setStyleKey(type);
            }
        }
        return model;
    }

    /// The node's parameter map, made when it has none. Never shared with a
    /// copy: a duplicated node gets its own in [#own].
    @SuppressWarnings("unchecked")
    static Map<String, Object> data(GraphNode n) {
        Object d = n.getUserData();
        if (d instanceof Map) {
            return (Map<String, Object>) d;
        }
        Map<String, Object> made = new LinkedHashMap<String, Object>();
        if (n.getStyleKey() != null) {
            made.put(TYPE, n.getStyleKey());
        }
        n.setUserData(made);
        return made;
    }

    /// Gives a node made from a template or a copy a map of its own, with
    /// the type and the catalog's defaults for what it does not say.
    static void own(GraphNode n, NodeTypeDto type) {
        Map<String, Object> mine = new LinkedHashMap<String, Object>();
        if (n.getUserData() instanceof Map) {
            mine.putAll(data(n));
        }
        String id = type != null ? type.type() : n.getStyleKey();
        if (id != null) {
            mine.put(TYPE, id);
            n.setStyleKey(id);
        }
        for (int iter = 0; type != null && type.params() != null && iter < type.params().size();
                iter++) {
            NodeParamDto p = type.params().get(iter);
            if (!mine.containsKey(p.name()) && p.defaultValue() != null) {
                mine.put(p.name(), p.defaultValue());
            }
        }
        n.setUserData(mine);
    }

    /// The node's type id.
    static String type(GraphNode n) {
        Object t = n.getUserData() instanceof Map ? data(n).get(TYPE) : null;
        return t != null ? t.toString() : n.getStyleKey();
    }

    static String param(GraphNode n, String name) {
        Object v = data(n).get(name);
        return v == null ? "" : text(v);
    }

    static void setParam(GraphNode n, String name, String value) {
        data(n).put(name, value == null ? "" : value);
    }

    // ---------------------------------------------------------------- catalog

    /// The catalog's types of one kind, in its order.
    static List<NodeTypeDto> ofKind(List<NodeTypeDto> catalog, String kind) {
        List<NodeTypeDto> out = new ArrayList<NodeTypeDto>();
        for (int iter = 0; catalog != null && iter < catalog.size(); iter++) {
            if (kind.equalsIgnoreCase(catalog.get(iter).kind())) {
                out.add(catalog.get(iter));
            }
        }
        return out;
    }

    static NodeTypeDto find(List<NodeTypeDto> catalog, String type) {
        for (int iter = 0; catalog != null && type != null && iter < catalog.size(); iter++) {
            if (type.equals(catalog.get(iter).type())) {
                return catalog.get(iter);
            }
        }
        return null;
    }

    /// The palette item for a type: named after the type, labelled as the
    /// catalog labels it, styled by its type and with the catalog's ports.
    static GraphNodeTemplate template(NodeTypeDto t) {
        GraphNodeTemplate tpl = new GraphNodeTemplate(t.type());
        tpl.setLabel(t.label() == null ? t.type() : t.label());
        tpl.setDescription(t.description());
        tpl.setStyleKey(t.type());
        tpl.setSize(WIDTH, HEIGHT);
        for (int iter = 0; t.inputs() != null && iter < t.inputs().size(); iter++) {
            String port = t.inputs().get(iter);
            tpl.addInput(port, port, null);
        }
        for (int iter = 0; t.outputs() != null && iter < t.outputs().size(); iter++) {
            String port = t.outputs().get(iter);
            tpl.addOutput(port, port, null);
        }
        return tpl;
    }

    /// The colour a kind is drawn in: green triggers, amber conditions, the
    /// brand's indigo for actions.
    static int kindColor(String kind) {
        if (TRIGGER.equalsIgnoreCase(kind)) {
            return 0x10b981;
        }
        if (CONDITION.equalsIgnoreCase(kind)) {
            return 0xf59e0b;
        }
        return 0x6366f1;
    }

    // ------------------------------------------------------------- validation

    /// The ids of the nodes an error message names, as a whole word: the
    /// server says "Step a1 needs ..." or "Connection e2 leaves c ...".
    static Set<String> nodesNamed(GraphModel model, List<String> errors) {
        Set<String> out = new HashSet<String>();
        for (int e = 0; errors != null && e < errors.size(); e++) {
            String text = errors.get(e);
            for (GraphNode n : model.getNodes()) {
                if (word(text, n.getId())) {
                    out.add(n.getId());
                }
            }
            for (GraphEdge edge : model.getEdges()) {
                if (word(text, edge.getId())) {
                    out.add(edge.getTargetId());
                }
            }
        }
        return out;
    }

    /// True when `id` occurs in `text` with no letter, digit, dot or
    /// underscore on either side.
    static boolean word(String text, String id) {
        if (text == null || id == null || id.length() == 0) {
            return false;
        }
        int from = 0;
        while (true) {
            int at = text.indexOf(id, from);
            if (at < 0) {
                return false;
            }
            int end = at + id.length();
            boolean before = at == 0 || !part(text.charAt(at - 1));
            boolean after = end >= text.length() || !part(text.charAt(end))
                    || text.charAt(end) == '.' && (end + 1 >= text.length()
                            || !part(text.charAt(end + 1)));
            if (before && after) {
                return true;
            }
            from = at + 1;
        }
    }

    private static boolean part(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_'
                || c == '.' || c == '-';
    }

    /// The kind of the graph's first trigger, or null.
    static String triggerKind(GraphModel model) {
        for (GraphNode n : model.getNodes()) {
            String t = type(n);
            if (t != null && n.getInputs().isEmpty() && !n.getOutputs().isEmpty()) {
                return t;
            }
        }
        return null;
    }
}
