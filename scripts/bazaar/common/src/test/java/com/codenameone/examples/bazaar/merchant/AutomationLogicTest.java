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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/// The automation editor's pure logic: the graph's data codec, the catalog to
/// palette mapping and the matching of server errors to nodes, plus the
/// analytics period and delta arithmetic. Nothing here opens a display.
class AutomationLogicTest {
    private static List<NodeTypeDto> catalog() {
        List<NodeTypeDto> all = new ArrayList<NodeTypeDto>();
        all.add(new NodeTypeDto("order.created", "TRIGGER", "Order placed", "",
                new ArrayList<String>(), Arrays.asList("out"), new ArrayList<NodeParamDto>()));
        all.add(new NodeTypeDto("if.total_over", "CONDITION", "Total over", "",
                Arrays.asList("in"), Arrays.asList("yes", "no"), Arrays.asList(
                        new NodeParamDto("amount", "Amount", "MONEY", "10000",
                                new ArrayList<String>()))));
        all.add(new NodeTypeDto("tag.order", "ACTION", "Tag order", "",
                Arrays.asList("in"), Arrays.asList("out"), Arrays.asList(
                        new NodeParamDto("tag", "Tag", "TEXT", "VIP", new ArrayList<String>()))));
        return all;
    }

    private static GraphNode node(DefaultGraphModel model, List<NodeTypeDto> catalog, String id,
            String type) {
        NodeTypeDto t = AutomationCodec.find(catalog, type);
        GraphNode n = AutomationCodec.template(t).create(id);
        AutomationCodec.own(n, t);
        model.addNode(n);
        return n;
    }

    @Test
    void dataMapSurvivesTheRoundTrip() {
        List<NodeTypeDto> catalog = catalog();
        DefaultGraphModel model = new DefaultGraphModel();
        GraphNode t = node(model, catalog, "t", "order.created");
        GraphNode c = node(model, catalog, "c", "if.total_over");
        GraphNode a = node(model, catalog, "a", "tag.order");
        AutomationCodec.setParam(c, "amount", "25000");
        AutomationCodec.setParam(a, "tag", "Big spender");
        model.connect(t, t.getOutput("out"), c, c.getInput("in"));
        model.connect(c, c.getOutput("yes"), a, a.getInput("in"));

        String json = AutomationCodec.toJson(model);
        assertTrue(json.contains("\"data\""), json);
        DefaultGraphModel back = AutomationCodec.fromJson(json);
        assertEquals(3, back.getNodes().size());
        assertEquals(2, back.getEdges().size());
        GraphNode c2 = back.getNode("c");
        assertEquals("if.total_over", c2.getStyleKey());
        assertEquals("if.total_over", AutomationCodec.type(c2));
        assertEquals("25000", AutomationCodec.param(c2, "amount"));
        assertEquals("Big spender", AutomationCodec.param(back.getNode("a"), "tag"));
        GraphEdge yes = null;
        for (GraphEdge e : back.getEdges()) {
            if ("yes".equals(e.getSourcePortId())) {
                yes = e;
            }
        }
        assertEquals("a", yes.getTargetId());
        assertEquals("in", yes.getTargetPortId());
        assertEquals("order.created", AutomationCodec.triggerKind(back));
    }

    @Test
    void serverNumbersReadAsWholeNumbers() {
        String json = "{\"nodes\":[{\"id\":\"c\",\"label\":\"x\",\"x\":0,\"y\":0,\"w\":180,"
                + "\"h\":64,\"style\":\"if.total_over\",\"data\":{\"type\":\"if.total_over\","
                + "\"amount\":15000}}],\"edges\":[]}";
        GraphNode c = AutomationCodec.fromJson(json).getNode("c");
        assertEquals("15000", AutomationCodec.param(c, "amount"));
        assertEquals("15000", AutomationCodec.text(Double.valueOf(15000)));
        assertEquals("1.5", AutomationCodec.text(Double.valueOf(1.5)));
    }

    @Test
    void typeComesFromStyleWhenDataIsSilent() {
        String json = "{\"nodes\":[{\"id\":\"t\",\"label\":\"x\",\"x\":0,\"y\":0,\"w\":180,"
                + "\"h\":64,\"style\":\"order.paid\"}],\"edges\":[]}";
        GraphNode t = AutomationCodec.fromJson(json).getNode("t");
        assertEquals("order.paid", AutomationCodec.type(t));
        assertEquals("order.paid", AutomationCodec.data(t).get("type"));
        assertEquals(0, AutomationCodec.fromJson("not json").getNodes().size());
        assertEquals(0, AutomationCodec.fromJson("").getNodes().size());
    }

    @Test
    void ownGivesACopyItsOwnMapWithDefaults() {
        List<NodeTypeDto> catalog = catalog();
        DefaultGraphModel model = new DefaultGraphModel();
        GraphNode c = node(model, catalog, "c", "if.total_over");
        assertEquals("10000", AutomationCodec.param(c, "amount"));
        GraphNode copy = c.copy("c2");
        AutomationCodec.own(copy, AutomationCodec.find(catalog, "if.total_over"));
        assertNotSame(c.getUserData(), copy.getUserData());
        AutomationCodec.setParam(copy, "amount", "1");
        assertEquals("10000", AutomationCodec.param(c, "amount"));
    }

    @Test
    void catalogMapsToPaletteByKind() {
        List<NodeTypeDto> catalog = catalog();
        assertEquals(1, AutomationCodec.ofKind(catalog, "TRIGGER").size());
        assertEquals(1, AutomationCodec.ofKind(catalog, "CONDITION").size());
        assertEquals("tag.order", AutomationCodec.ofKind(catalog, "ACTION").get(0).type());
        GraphNodeTemplate tpl = AutomationCodec.template(catalog.get(1));
        assertEquals("if.total_over", tpl.getName());
        assertEquals("Total over", tpl.getLabel());
        assertEquals("if.total_over", tpl.getStyleKey());
        GraphNode n = tpl.create("n1");
        assertEquals(1, n.getInputs().size());
        assertEquals("in", n.getInputs().get(0).getId());
        assertEquals(2, n.getOutputs().size());
        assertEquals("no", n.getOutputs().get(1).getId());
        assertTrue(AutomationCodec.kindColor("TRIGGER") != AutomationCodec.kindColor("ACTION"));
    }

    @Test
    void errorsPointAtTheNodesTheyName() {
        List<NodeTypeDto> catalog = catalog();
        DefaultGraphModel model = new DefaultGraphModel();
        GraphNode t = node(model, catalog, "t", "order.created");
        GraphNode a = node(model, catalog, "a1", "tag.order");
        node(model, catalog, "a", "tag.order");
        GraphEdge e = model.connect(t, t.getOutput("out"), a, a.getInput("in"));
        Set<String> named = AutomationCodec.nodesNamed(model, Arrays.asList(
                "Step a needs a tag", "Connection " + e.getId() + " enters somewhere odd"));
        assertTrue(named.contains("a"));
        assertTrue(named.contains("a1"));
        assertFalse(named.contains("t"));
        assertTrue(AutomationCodec.word("leads into the trigger t.", "t"));
        assertFalse(AutomationCodec.word("Step a1 needs", "a"));
    }

    // --------------------------------------------------------------- analytics

    private static final long NOV_20 = Period.days(2026, 11, 20) * Period.DAY;

    @Test
    void presetsCutOnUtcMidnight() {
        long now = NOV_20 + 15 * 3600000L;
        Period today = Period.preset(Period.TODAY, now);
        assertEquals(NOV_20, today.from);
        assertEquals(NOV_20 + Period.DAY, today.to);
        Period week = Period.preset(Period.LAST_7, now);
        assertEquals(7, week.days());
        assertEquals("2026-11-14", Period.iso(week.from));
        assertEquals("2026-11-01", Period.iso(Period.preset(Period.MONTH, now).from));
        assertEquals("2026-01-01", Period.iso(Period.preset(Period.YEAR, now).from));
        assertEquals("2026-11-19", Period.iso(Period.preset(Period.YESTERDAY, now).from));
        assertEquals(30, Period.preset("nonsense", now).days());
    }

    @Test
    void previousPeriodIsAsLongAndEndsWhereThisStarts() {
        Period p = Period.preset(Period.LAST_30, NOV_20);
        Period before = p.previous();
        assertEquals(p.from, before.to);
        assertEquals(p.to - p.from, before.to - before.from);
        assertEquals(p.from - (p.to - p.from), before.from);
        Period custom = Period.custom(NOV_20, NOV_20 - 2 * Period.DAY);
        assertEquals(3, custom.days());
        assertEquals(Period.MAX_DAYS, Period.custom(0, NOV_20).days());
    }

    @Test
    void civilCalendarRoundTrips() {
        assertEquals(0, Period.days(1970, 1, 1));
        int[] leap = Period.civil(Period.days(2024, 2, 29));
        assertEquals(2024, leap[0]);
        assertEquals(2, leap[1]);
        assertEquals(29, leap[2]);
        assertEquals(Period.days(2024, 3, 1), Period.days(2024, 2, 29) + 1);
    }

    @Test
    void deltasFormatAndNeverDivideByZero() {
        assertEquals("+12.5%", Period.delta(11250, 10000));
        assertEquals("-3%", Period.delta(9700, 10000));
        assertEquals("0%", Period.delta(5, 5));
        assertEquals("0%", Period.delta(0, 0));
        assertEquals("New", Period.delta(100, 0));
        assertEquals("-100%", Period.delta(0, 400));
        assertEquals("+200%", Period.delta(3, 1));
        assertEquals("33.3%", Period.percent(3333));
        assertTrue(Period.good(5, 3, false));
        assertTrue(Period.good(2, 3, true));
    }
}
