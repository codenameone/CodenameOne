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
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A customer journey's graph: the journey catalog's kinds, the colour each
/// is drawn in, the per-node counts overlay and the checks the server makes
/// before a journey may go live.
///
/// A journey is stored in the automations' graph JSON (`GraphJson` with each
/// node's parameters as flat text, its type under `type`), so
/// [AutomationCodec] reads and writes it; what differs is the vocabulary --
/// one ENTRY, STEPs, SPLITs with two outputs and END. [#problems] mirrors the
/// server's `Journeys.validate` word for word, so the editor can point at a
/// faulty step before the round trip, and the server's answer, which names
/// steps the same way, outlines the same nodes.
final class JourneyGraph {
    static final String ENTRY = "ENTRY";
    static final String STEP = "STEP";
    static final String SPLIT = "SPLIT";
    static final String END = "END";
    static final String[] KINDS = {ENTRY, STEP, SPLIT, END};
    static final String EXIT = "journey.exit";

    private JourneyGraph() {
    }

    /// Green entries, the brand's indigo for steps, amber splits, grey ends.
    static int kindColor(String kind) {
        if (ENTRY.equalsIgnoreCase(kind)) {
            return 0x10b981;
        }
        if (SPLIT.equalsIgnoreCase(kind)) {
            return 0xf59e0b;
        }
        if (END.equalsIgnoreCase(kind)) {
            return 0x64748b;
        }
        return 0x6366f1;
    }

    static String kindWord(String kind) {
        if (ENTRY.equalsIgnoreCase(kind)) {
            return "Entry";
        }
        if (SPLIT.equalsIgnoreCase(kind)) {
            return "Split";
        }
        if (END.equalsIgnoreCase(kind)) {
            return "End";
        }
        return "Step";
    }

    /// The graph JSON the server stores.
    static String toJson(GraphModel model) {
        return AutomationCodec.toJson(model);
    }

    /// A stored graph, each node given its catalog defaults.
    static DefaultGraphModel fromJson(String json, List<NodeTypeDto> catalog) {
        DefaultGraphModel model = AutomationCodec.fromJson(json);
        for (GraphNode n : model.getNodes()) {
            AutomationCodec.own(n, AutomationCodec.find(catalog, AutomationCodec.type(n)));
        }
        return model;
    }

    /// The counts per node id: `{current, passed}`.
    static Map<String, long[]> counts(List<JourneyNodeStatDto> stats) {
        Map<String, long[]> out = new HashMap<String, long[]>();
        for (int iter = 0; stats != null && iter < stats.size(); iter++) {
            JourneyNodeStatDto s = stats.get(iter);
            if (s != null && s.nodeId() != null) {
                out.put(s.nodeId(), new long[] {s.current(), s.passed()});
            }
        }
        return out;
    }

    /// The badge drawn on a node: "3 here | 120 passed", or null with none.
    static String badge(long[] count) {
        if (count == null || count[0] <= 0 && count[1] <= 0) {
            return null;
        }
        if (count[0] <= 0) {
            return count[1] + " passed";
        }
        return count[0] + " here | " + count[1] + " passed";
    }

    // ------------------------------------------------------------ validation
    /// What the server would refuse in this graph, in its words: not exactly
    /// one entry, unknown steps, a loop, a step no path from the entry
    /// reaches, or a step missing what it needs. Empty when it may go live.
    static List<String> problems(GraphModel model, List<NodeTypeDto> catalog) {
        List<String> out = new ArrayList<String>();
        GraphNode entry = null;
        for (GraphNode n : model.getNodes()) {
            NodeTypeDto t = AutomationCodec.find(catalog, AutomationCodec.type(n));
            if (t == null) {
                out.add("Step " + n.getId() + " is not a journey step");
            } else if (ENTRY.equalsIgnoreCase(t.kind())) {
                if (entry != null) {
                    out.add("A journey has one entry");
                }
                entry = n;
            }
        }
        if (entry == null) {
            out.add("The journey has no entry");
            return out;
        }
        Set<String> reached = new HashSet<String>();
        List<GraphNode> todo = new ArrayList<GraphNode>();
        todo.add(entry);
        while (!todo.isEmpty()) {
            GraphNode n = todo.remove(todo.size() - 1);
            if (!reached.add(n.getId())) {
                continue;
            }
            todo.addAll(next(model, n));
        }
        if (loops(model, entry, new HashSet<String>(), new HashSet<String>())) {
            out.add("The steps loop back on themselves");
        }
        for (GraphNode n : model.getNodes()) {
            NodeTypeDto t = AutomationCodec.find(catalog, AutomationCodec.type(n));
            if (t == null) {
                continue;
            }
            if (!reached.contains(n.getId())) {
                out.add("Step " + n.getId() + " is not connected to the entry");
            }
            String missing = missing(model, n, t);
            if (missing != null) {
                out.add("Step " + n.getId() + " needs " + missing);
            }
        }
        return out;
    }

    /// The nodes an edge leaves `n` for, in edge order.
    static List<GraphNode> next(GraphModel model, GraphNode n) {
        List<GraphNode> out = new ArrayList<GraphNode>();
        for (GraphEdge e : model.getEdges()) {
            if (n.getId().equals(e.getSourceId())) {
                GraphNode to = model.getNode(e.getTargetId());
                if (to != null) {
                    out.add(to);
                }
            }
        }
        return out;
    }

    static boolean loops(GraphModel model, GraphNode n, Set<String> path, Set<String> done) {
        if (path.contains(n.getId())) {
            return true;
        }
        if (!done.add(n.getId())) {
            return false;
        }
        path.add(n.getId());
        List<GraphNode> after = next(model, n);
        for (int iter = 0; iter < after.size(); iter++) {
            if (loops(model, after.get(iter), path, done)) {
                return true;
            }
        }
        path.remove(n.getId());
        return false;
    }

    private static String missing(GraphModel model, GraphNode n, NodeTypeDto t) {
        String type = t.type();
        if (!EXIT.equals(type) && next(model, n).isEmpty()) {
            return "a step after it";
        }
        if ("journey.entry.segment".equals(type) && empty(n, "segmentId")) {
            return "a segment";
        }
        if ("journey.entry.trigger".equals(type) && !empty(n, "event")
                && !"order.paid".equals(AutomationCodec.param(n, "event").trim())) {
            return "an event it knows (order.paid)";
        }
        if ("journey.wait".equals(type)) {
            long m = number(n, "minutes", 0);
            if (m <= 0 || m > 365L * 24 * 60) {
                return "a wait of 1 minute to a year";
            }
        }
        if ("journey.split.condition".equals(type) && empty(n, "segmentId") && empty(n, "tag")) {
            return "a segment or a tag";
        }
        if ("journey.split.ab".equals(type)) {
            long p = number(n, "percent", 50);
            if (p < 0 || p > 100) {
                return "a percent from 0 to 100";
            }
        }
        if ("journey.send.email".equals(type) && (empty(n, "subject") || empty(n, "body"))) {
            return "a subject and a message";
        }
        if (("journey.send.sms".equals(type) || "journey.send.push".equals(type))
                && empty(n, "body")) {
            return "a message";
        }
        if ("journey.points.add".equals(type)) {
            long p = number(n, "points", 0);
            if (p <= 0 || p > 100000) {
                return "1 to 100000 points";
            }
        }
        if ("journey.tag.add".equals(type) && empty(n, "tag")) {
            return "a tag";
        }
        return null;
    }

    private static boolean empty(GraphNode n, String param) {
        return AutomationCodec.param(n, param).trim().length() == 0;
    }

    /// A whole-number parameter; `fallback` when it is empty, and -1 (out of
    /// every range) when it is not a number.
    static long number(GraphNode n, String param, long fallback) {
        String v = AutomationCodec.param(n, param).trim();
        if (v.length() == 0) {
            return fallback;
        }
        long out = 0;
        for (int iter = 0; iter < v.length(); iter++) {
            char c = v.charAt(iter);
            if (c < '0' || c > '9' || out > 100000000000L) {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }
}
