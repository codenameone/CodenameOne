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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphNode;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/// The growth screens' pure logic: journey graph serialization and
/// validation, segment rule serialization, the promotions calendar's date
/// arithmetic and the quotes board's legal moves. Nothing opens a display.
class MarketingLogicTest {
    private static final List<String> NONE = new ArrayList<String>();

    private static List<NodeTypeDto> catalog() {
        List<NodeTypeDto> all = new ArrayList<NodeTypeDto>();
        all.add(new NodeTypeDto("journey.entry.segment", "ENTRY", "Joins a segment", "", NONE,
                Arrays.asList("out"), Arrays.asList(
                        new NodeParamDto("segmentId", "Segment", "segment", "", NONE))));
        all.add(new NodeTypeDto("journey.wait", "STEP", "Wait", "", Arrays.asList("in"),
                Arrays.asList("out"), Arrays.asList(
                        new NodeParamDto("minutes", "Minutes", "number", "60", NONE))));
        all.add(new NodeTypeDto("journey.exit", "END", "Exit", "", Arrays.asList("in"), NONE,
                new ArrayList<NodeParamDto>()));
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

    private static DefaultGraphModel journey(List<NodeTypeDto> catalog) {
        DefaultGraphModel model = new DefaultGraphModel();
        GraphNode e = node(model, catalog, "e", "journey.entry.segment");
        GraphNode w = node(model, catalog, "w", "journey.wait");
        GraphNode x = node(model, catalog, "x", "journey.exit");
        AutomationCodec.setParam(e, "segmentId", "seg-1");
        AutomationCodec.setParam(w, "minutes", "1440");
        model.connect(e, e.getOutput("out"), w, w.getInput("in"));
        model.connect(w, w.getOutput("out"), x, x.getInput("in"));
        return model;
    }

    private static long at(TimeZone zone, int y, int month0, int day, int h, int m) {
        Calendar c = Calendar.getInstance(zone);
        c.clear();
        c.set(y, month0, day, h, m, 0);
        return c.getTime().getTime();
    }

    @Test
    void aJourneySurvivesTheRoundTripAndValidates() {
        List<NodeTypeDto> catalog = catalog();
        String json = JourneyGraph.toJson(journey(catalog));
        DefaultGraphModel back = JourneyGraph.fromJson(json, catalog);
        assertEquals(3, back.getNodes().size());
        assertEquals(2, back.getEdges().size());
        GraphNode w = back.getNode("w");
        assertEquals("1440", AutomationCodec.param(w, "minutes"));
        assertEquals("journey.wait", AutomationCodec.type(w));
        List<String> problems = JourneyGraph.problems(back, catalog);
        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void aJourneyMissingWhatItNeedsSaysWhat() {
        List<NodeTypeDto> catalog = catalog();
        DefaultGraphModel model = journey(catalog);
        AutomationCodec.setParam(model.getNode("w"), "minutes", "0");
        node(model, catalog, "lost", "journey.exit");
        List<String> problems = JourneyGraph.problems(model, catalog);
        assertTrue(problems.contains("Step w needs a wait of 1 minute to a year"),
                problems.toString());
        assertTrue(problems.contains("Step lost is not connected to the entry"),
                problems.toString());
        assertEquals(Arrays.asList("The journey has no entry"),
                JourneyGraph.problems(new DefaultGraphModel(), catalog));
    }

    @Test
    void nodeCountsBecomeBadges() {
        Map<String, long[]> counts = JourneyGraph.counts(Arrays.asList(
                new JourneyNodeStatDto("w", 3, 120), new JourneyNodeStatDto("x", 0, 7)));
        assertEquals("3 here | 120 passed", JourneyGraph.badge(counts.get("w")));
        assertEquals("7 passed", JourneyGraph.badge(counts.get("x")));
        assertNull(JourneyGraph.badge(counts.get("e")));
    }

    @Test
    void segmentRulesWriteEachValueInItsJsonType() {
        SegmentRules r = SegmentRules.parse("{\"op\":\"OR\",\"rules\":["
                + "{\"field\":\"totalSpent\",\"op\":\">=\",\"value\":10000},"
                + "{\"op\":\"AND\",\"rules\":[{\"field\":\"tags\",\"op\":\"has\",\"value\":\"vip\"}]},"
                + "{\"field\":\"acceptsMarketing\",\"op\":\"is\",\"value\":true}]}");
        assertEquals(SegmentRules.OR, r.op);
        assertEquals(3, r.rules.size());
        assertTrue(r.rules.get(1).isGroup());
        assertEquals("10000", r.rules.get(0).value);
        String json = r.toJson();
        assertTrue(json.startsWith("{\"op\":\"OR\""), json);
        assertTrue(json.contains("\"value\":10000}"), json);
        assertTrue(json.contains("\"value\":true}"), json);
        assertTrue(json.contains("\"field\":\"tags\",\"op\":\"has\",\"value\":\"vip\""), json);
        assertNull(r.problem());
        assertEquals(json, SegmentRules.parse(json).toJson());
    }

    @Test
    void segmentRowsAreCheckedBeforeTheServerIsAsked() {
        SegmentRules r = new SegmentRules();
        SegmentRules.Rule days = SegmentRules.row("lastOrderAt");
        days.value = "9999";
        r.rules.add(days);
        assertEquals("Rule 1 looks back at most 3650 days", r.problem());
        days.value = "30";
        SegmentRules.Rule country = SegmentRules.row("country");
        country.value = "Israel";
        r.rules.add(country);
        assertEquals("Rule 2 needs a two-letter country", r.problem());
        country.value = "IL";
        assertNull(r.problem());
        assertTrue(r.toJson().contains("\"value\":30}"), r.toJson());
        assertEquals(Arrays.asList("withinDays", "notWithinDays"),
                Arrays.asList(SegmentRules.opsFor("createdAt")));
    }

    @Test
    void aDragKeepsTheTimeOfDayAndTheLength() {
        TimeZone zone = TimeZone.getTimeZone("America/New_York");
        long start = at(zone, 2026, 2, 6, 9, 0);
        long end = at(zone, 2026, 2, 7, 21, 0);
        // 9 March is after the clocks went forward on the 8th.
        long target = at(zone, 2026, 2, 9, 0, 30);
        long[] moved = PromoDates.moveToDay(start, end, target, zone);
        assertEquals(at(zone, 2026, 2, 9, 9, 0), moved[0]);
        assertEquals(at(zone, 2026, 2, 10, 21, 0), moved[1]);
        assertEquals(3, PromoDates.daysBetween(start, target, zone));
        assertEquals(-3, PromoDates.daysBetween(target, start, zone));
        long[] back = PromoDates.moveToDay(moved[0], moved[1], start, zone);
        assertEquals(start, back[0]);
        assertEquals(end, back[1]);
    }

    @Test
    void theCalendarRefusesWhatTheServerWould() {
        long now = 1000L * PromoDates.DAY;
        long d = PromoDates.DAY;
        assertNull(PromoDates.refusal("SALE", "SCHEDULED", now + d, now + 2 * d, now));
        assertEquals("Only a sale that has not started can move",
                PromoDates.refusal("SALE", "LIVE", now + d, now + 2 * d, now));
        assertEquals("A sale needs a start before its end, within a year",
                PromoDates.refusal("SALE", "SCHEDULED", now + d, now + 400 * d, now));
        assertEquals("A start before the end",
                PromoDates.refusal("DISCOUNT", "ACTIVE", now + d, now, now));
        assertNull(PromoDates.refusal("CAMPAIGN", "DRAFT", now + d, now + d, now));
        assertEquals("Schedule it in the future",
                PromoDates.refusal("CAMPAIGN", "SCHEDULED", now - d, now - d, now));
        assertEquals("That campaign has already gone out",
                PromoDates.refusal("CAMPAIGN", "SENT", now + d, now + d, now));
        assertEquals(0x6366f1, PromoDates.colour("#6366f1", 0));
        assertEquals(7, PromoDates.colour("indigo", 7));
    }

    @Test
    void theQuotesBoardKnowsTheMerchantsMoves() {
        assertTrue(QuoteMoves.legal("DRAFT", "SENT"));
        assertTrue(QuoteMoves.legal("SENT", "DRAFT"));
        assertTrue(QuoteMoves.legal("SENT", "DECLINED"));
        assertTrue(QuoteMoves.legal("ACCEPTED", "ORDERED"));
        assertFalse(QuoteMoves.legal("SENT", "ACCEPTED"));
        assertFalse(QuoteMoves.legal("DRAFT", "ORDERED"));
        assertFalse(QuoteMoves.legal("ORDERED", "DRAFT"));
        assertFalse(QuoteMoves.legal("DECLINED", "SENT"));
        assertEquals(Arrays.asList("SENT", "DECLINED"), QuoteMoves.targets("DRAFT"));
        assertTrue(QuoteMoves.targets("ORDERED").isEmpty());
        assertEquals("DRAFT", QuoteMoves.column("WEIRD"));
    }

    @Test
    void aCampaignDayReadsInTheMerchantsZone() {
        TimeZone zone = TimeZone.getTimeZone("Europe/London");
        assertEquals(at(zone, 2026, 5, 1, 14, 30), MarketingPane.parseWhen("2026-06-01 14:30", zone));
        assertEquals(at(zone, 2026, 5, 1, 9, 0), MarketingPane.parseWhen("2026-06-01", zone));
        assertEquals(0, MarketingPane.parseWhen("June 1", zone));
    }
}
