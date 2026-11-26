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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The demo automations' graphs, written in the shape
/// `com.codename1.graph.GraphJson` reads: what the editor opens them as.
public final class DemoGraphs {
    private final List<Object> nodes = new ArrayList<Object>();
    private final List<Object> edges = new ArrayList<Object>();

    private DemoGraphs() {
    }

    /// Order placed, total over `cents`: tag the order and the customer VIP.
    public static String vip(long cents) {
        DemoGraphs g = new DemoGraphs();
        g.node("t", NodeCatalog.ORDER_CREATED, "Order placed", 40, 120);
        g.node("c", NodeCatalog.IF_TOTAL_OVER, "Total over $" + cents / 100, 260, 120,
                "amount", Long.valueOf(cents));
        g.node("a1", NodeCatalog.TAG_ORDER, "Tag order VIP", 500, 60, "tag", "VIP");
        g.node("a2", NodeCatalog.TAG_CUSTOMER, "Tag customer VIP", 500, 180, "tag", "VIP");
        g.edge("e1", "t", NodeCatalog.OUT, "c");
        g.edge("e2", "c", NodeCatalog.YES, "a1");
        g.edge("e3", "c", NodeCatalog.YES, "a2");
        return g.json();
    }

    /// Stock low: tell the staff and draft a purchase order.
    public static String lowStock() {
        DemoGraphs g = new DemoGraphs();
        g.node("t", NodeCatalog.INVENTORY_LOW, "Stock low", 40, 120);
        g.node("a1", NodeCatalog.NOTIFY_STAFF, "Tell the staff", 280, 60, "message",
                "Stock is running low");
        g.node("a2", NodeCatalog.INVENTORY_REORDER, "Draft purchase order", 280, 180,
                "quantity", Long.valueOf(0));
        g.edge("e1", "t", NodeCatalog.OUT, "a1");
        g.edge("e2", "t", NodeCatalog.OUT, "a2");
        return g.json();
    }

    /// A node of `type` with its catalog ports; `params` are name, value
    /// pairs kept in its `data`.
    private void node(String id, String type, String label, double x, double y,
            Object... params) {
        NodeTypeDto t = NodeCatalog.find(type);
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("label", label);
        m.put("x", Double.valueOf(x));
        m.put("y", Double.valueOf(y));
        m.put("w", Double.valueOf(180));
        m.put("h", Double.valueOf(64));
        m.put("style", type);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("type", type);
        for (int iter = 0; iter + 1 < params.length; iter += 2) {
            data.put((String) params[iter], params[iter + 1]);
        }
        m.put("data", data);
        m.put("inputs", ports(t.inputs()));
        m.put("outputs", ports(t.outputs()));
        nodes.add(m);
    }

    private static List<Object> ports(List<String> ids) {
        List<Object> out = new ArrayList<Object>();
        for (int iter = 0; iter < ids.size(); iter++) {
            Map<String, Object> p = new LinkedHashMap<String, Object>();
            p.put("id", ids.get(iter));
            p.put("label", ids.get(iter));
            p.put("type", "flow");
            out.add(p);
        }
        return out;
    }

    private void edge(String id, String from, String fromPort, String to) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("from", from);
        m.put("fromPort", fromPort);
        m.put("to", to);
        m.put("toPort", NodeCatalog.IN);
        edges.add(m);
    }

    private String json() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("nodes", nodes);
        root.put("edges", edges);
        return Json.write(root);
    }
}
