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
package com.codenameone.examples.bazaar;

import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.LoyaltyTier;
import com.codenameone.examples.bazaar.journey.JourneyCatalog;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.marketing.AudienceSql;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.sale.SaleEvents;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The growth features' pure parts: points, sale prices, discount stacking,
/// the segment compiler's refusals, A/B winners, signatures and journey
/// graphs. No database.
class GrowthMathTest {
    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Set<String> NONE = Collections.<String>emptySet();

    // ---------------------------------------------------------------- loyalty

    private static LoyaltyProgram program(String rounding) {
        LoyaltyProgram p = new LoyaltyProgram();
        p.setActive(true);
        p.setPointsPerUnit(3);
        p.setUnitCents(200);
        p.setRounding(rounding);
        p.setPointsPerReward(100);
        p.setRewardCents(500);
        p.setMinRedeemPoints(200);
        return p;
    }

    @Test
    void pointsRoundAsTheProgrammeSays() {
        // 1050 cents at 3 points per 2 dollars is 15.75 points.
        assertEquals(15, LoyaltyMath.points(1050, program(LoyaltyMath.DOWN)));
        assertEquals(16, LoyaltyMath.points(1050, program(LoyaltyMath.NEAREST)));
        assertEquals(16, LoyaltyMath.points(1050, program(LoyaltyMath.UP)));
        assertEquals(15, LoyaltyMath.points(1000, program(LoyaltyMath.UP)));
        assertEquals(0, LoyaltyMath.points(-500, program(LoyaltyMath.UP)));
        LoyaltyProgram broken = program(LoyaltyMath.DOWN);
        broken.setUnitCents(0);
        assertEquals(0, LoyaltyMath.points(1000, broken));
    }

    @Test
    void redeemingNeedsTheMinimumWholeRewardsAndTheBalance() {
        LoyaltyProgram p = program(LoyaltyMath.DOWN);
        assertNull(LoyaltyMath.redeemProblem(300, 300, p));
        assertNotNull(LoyaltyMath.redeemProblem(100, 300, p));
        assertNotNull(LoyaltyMath.redeemProblem(250, 300, p));
        assertNotNull(LoyaltyMath.redeemProblem(400, 300, p));
        assertEquals(1500, LoyaltyMath.value(300, p));
        assertEquals(500, LoyaltyMath.value(199, p));
        p.setActive(false);
        assertNotNull(LoyaltyMath.redeemProblem(300, 300, p));
    }

    @Test
    void refundsReverseInProportionAndTheSameTotalAnswersTheSame() {
        assertEquals(0, LoyaltyMath.reversedFor(100, 10000, 0));
        assertEquals(33, LoyaltyMath.reversedFor(100, 9000, 3000));
        assertEquals(33, LoyaltyMath.reversedFor(100, 9000, 3000));
        assertEquals(100, LoyaltyMath.reversedFor(100, 9000, 9000));
        assertEquals(100, LoyaltyMath.reversedFor(100, 9000, 12000));
        assertEquals(100, LoyaltyMath.reversedFor(100, 0, 1));
    }

    @Test
    void tiersPickTheHighestReachedAndTheNext() {
        List<LoyaltyTier> tiers = new ArrayList<LoyaltyTier>();
        tiers.add(tier("gold", 100000));
        tiers.add(tier("bronze", 0));
        tiers.add(tier("silver", 25000));
        assertEquals("bronze", LoyaltyMath.tierFor(24999, tiers).getId());
        assertEquals("silver", LoyaltyMath.tierFor(25000, tiers).getId());
        assertEquals("gold", LoyaltyMath.nextTier(25000, tiers).getId());
        assertNull(LoyaltyMath.nextTier(100000, tiers));
    }

    private static LoyaltyTier tier(String id, long threshold) {
        LoyaltyTier t = new LoyaltyTier();
        t.setId(id);
        t.setThreshold(threshold);
        return t;
    }

    // ------------------------------------------------------------------ sales

    @Test
    void salePricesNeverGoBelowZeroOrAboveThePrice() {
        assertEquals(800, SaleEvents.salePrice(1000, SaleEvents.PERCENT, 2000));
        assertEquals(666, SaleEvents.salePrice(999, SaleEvents.PERCENT, 3333));
        assertEquals(0, SaleEvents.salePrice(1000, SaleEvents.PERCENT, 10000));
        assertEquals(750, SaleEvents.salePrice(1000, SaleEvents.AMOUNT_OFF, 250));
        assertEquals(0, SaleEvents.salePrice(1000, SaleEvents.AMOUNT_OFF, 5000));
        assertEquals(499, SaleEvents.salePrice(1000, SaleEvents.FIXED_PRICE, 499));
        assertEquals(1000, SaleEvents.salePrice(1000, SaleEvents.FIXED_PRICE, 1500));
    }

    @Test
    void windowsOverlapOnlyWhenTheyShareTime() {
        assertTrue(SaleEvents.overlaps(0, 10, 5, 15));
        assertTrue(SaleEvents.overlaps(5, 15, 0, 10));
        assertTrue(SaleEvents.overlaps(0, 20, 5, 6));
        assertFalse(SaleEvents.overlaps(0, 10, 10, 20));
        assertFalse(SaleEvents.overlaps(10, 20, 0, 10));
    }

    // -------------------------------------------------------------- discounts

    private static PriceLine line(String key, long unit, int qty) {
        return new PriceLine(key, "p-" + key, "v-" + key, unit, qty, 100, true);
    }

    private static Discount auto(String id, String kind, long amount) {
        Discount d = new Discount();
        d.setId(id);
        d.setCode("");
        d.setKind(kind);
        d.setAmount(amount);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setActive(true);
        d.setAutomatic(true);
        return d;
    }

    private static Discount classed(Discount d, String cls, boolean product, boolean order,
            boolean shipping) {
        d.setDiscountClass(cls);
        d.setCombinesProduct(product);
        d.setCombinesOrder(order);
        d.setCombinesShipping(shipping);
        return d;
    }

    private static DiscountEngine.Result run(List<PriceLine> lines, Set<String> member, Discount... autos) {
        return DiscountEngine.apply(lines, Arrays.asList(autos), null, null, NOW, NONE, 900, member);
    }

    private static long sum(List<PriceLine> lines) {
        long s = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            assertTrue(lines.get(iter).netCents() >= 0);
            s += lines.get(iter).discountCents;
        }
        return s;
    }

    @Test
    void tieredTakesTheHighestTierReached() {
        Discount d = auto("t", DiscountEngine.TIERED, 0);
        d.setRules("{\"tiers\":[{\"min\":5000,\"bps\":500},{\"min\":10000,\"bps\":1000},"
                + "{\"min\":20000,\"cents\":3000}]}");
        List<PriceLine> lines = Arrays.asList(line("a", 6000, 2));
        assertEquals(1200, run(lines, null, d).totalCents);
        lines = Arrays.asList(line("a", 4000, 1));
        assertEquals(0, run(lines, null, d).totalCents);
        lines = Arrays.asList(line("a", 25000, 1));
        assertEquals(3000, run(lines, null, d).totalCents);
        assertEquals(3000, sum(lines));
    }

    @Test
    void volumeBreaksArePerProduct() {
        Discount d = auto("v", DiscountEngine.VOLUME, 0);
        d.setRules("{\"breaks\":[{\"qty\":3,\"bps\":1000},{\"qty\":6,\"bps\":2000}]}");
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 6), line("b", 1000, 2));
        DiscountEngine.Result r = run(lines, null, d);
        assertEquals(1200, r.totalCents);
        assertEquals(1200, lines.get(0).discountCents);
        assertEquals(0, lines.get(1).discountCents);
    }

    @Test
    void bundleSellsCompleteSetsAtThePrice() {
        Discount d = auto("b", DiscountEngine.BUNDLE, 0);
        d.setRules("{\"price\":2500,\"items\":[{\"variantId\":\"v-a\",\"qty\":1},"
                + "{\"variantId\":\"v-b\",\"qty\":2}]}");
        // Two sets (a + b + b at 3000) and one b over.
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 2), line("b", 1000, 5));
        assertEquals(1000, run(lines, null, d).totalCents);
        assertEquals(1000, sum(lines));
    }

    @Test
    void buyTwoGetOneFreeDiscountsTheCheapest() {
        Discount d = auto("x", DiscountEngine.BUY_X_GET_Y, 0);
        d.setBuyQty(2);
        d.setGetQty(1);
        List<PriceLine> lines = Arrays.asList(line("a", 3000, 2), line("b", 1000, 1));
        assertEquals(1000, run(lines, null, d).totalCents);
    }

    @Test
    void classesDecideWhatStacks() {
        Discount product = classed(auto("p", DiscountEngine.PERCENT, 1000), DiscountEngine.CLASS_PRODUCT,
                false, true, true);
        product.setAppliesTo(DiscountEngine.PRODUCT);
        product.setTargetId("p-a");
        Discount order = classed(auto("o", DiscountEngine.FIXED, 500), DiscountEngine.CLASS_ORDER,
                true, false, true);
        List<PriceLine> lines = Arrays.asList(line("a", 10000, 1), line("b", 5000, 1));
        DiscountEngine.Result r = run(lines, null, product, order);
        assertEquals(2, r.applied.size());
        assertEquals(1500, r.totalCents);

        // The order discount no longer combines with product ones: the larger alone wins.
        order.setCombinesProduct(false);
        lines = Arrays.asList(line("a", 10000, 1), line("b", 5000, 1));
        r = run(lines, null, product, order);
        assertEquals(1, r.applied.size());
        assertEquals(1000, r.totalCents);
        assertTrue(DiscountEngine.compatible(product, product) == product.isCombinesProduct());
    }

    @Test
    void memberPerksNeedTheMembership() {
        Discount perk = auto("m", DiscountEngine.PERCENT, 1000);
        perk.setEligibility("TIER:gold");
        List<PriceLine> lines = Arrays.asList(line("a", 5000, 1));
        assertEquals(0, run(lines, null, perk).totalCents);
        lines = Arrays.asList(line("a", 5000, 1));
        assertEquals(0, run(lines, new HashSet<String>(Arrays.asList("TIER:silver")), perk).totalCents);
        lines = Arrays.asList(line("a", 5000, 1));
        assertEquals(500, run(lines, new HashSet<String>(Arrays.asList("TIER:gold")), perk).totalCents);
    }

    // --------------------------------------------------------------- segments

    private static void refused(String rules) {
        try {
            AudienceSql.compile(rules, "store-1", NOW.toEpochMilli());
            fail("compiled " + rules);
        } catch (ResponseStatusException expected) {
            assertEquals(400, expected.getStatus());
        }
        assertNotNull(AudienceSql.problem(rules), rules);
    }

    @Test
    void segmentRulesCompileToBoundParametersOnly() {
        String hostile = "x'); DROP TABLE customer; --";
        AudienceSql.Query q = AudienceSql.compile("{\"op\":\"OR\",\"rules\":[{\"field\":\"totalSpent\","
                + "\"op\":\">=\",\"value\":5000},{\"op\":\"AND\",\"rules\":[{\"field\":\"tags\",\"op\":\"has\","
                + "\"value\":\"vip\"},{\"field\":\"lastOrderAt\",\"op\":\"withinDays\",\"value\":30}]}]}",
                "store-1", NOW.toEpochMilli());
        assertTrue(q.where.indexOf("?") > 0, q.where);
        assertTrue(q.params.contains("store-1"), q.params.toString());
        assertTrue(q.params.contains(Long.valueOf(5000)), q.params.toString());
        assertFalse(q.where.indexOf("vip") >= 0, q.where);
        assertFalse(q.where.indexOf("store-1") >= 0, q.where);
        // An empty tree matches the whole store.
        assertTrue(AudienceSql.compile("", "store-1", NOW.toEpochMilli()).params.contains("store-1"));
        assertNull(AudienceSql.problem("{\"op\":\"AND\",\"rules\":[]}"));
        assertFalse(hostile.isEmpty());
    }

    @Test
    void segmentInjectionAttemptsAreRefused() {
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"1=1 OR email\",\"op\":\"=\",\"value\":1}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"c.password\",\"op\":\"=\",\"value\":1}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"totalSpent\",\"op\":\"= 0 OR 1=1 --\",\"value\":1}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"totalSpent\",\"op\":\">\",\"value\":\"0 OR 1=1\"}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"tags\",\"op\":\"has\",\"value\":\"x' OR '1'='1\"}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"tags\",\"op\":\"has\",\"value\":\"50%\"}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"country\",\"op\":\"is\",\"value\":\"US'--\"}]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"tier\",\"op\":\"is\",\"value\":\"a) OR (1=1\"}]}");
        refused("{\"op\":\"UNION SELECT\",\"rules\":[]}");
        refused("{\"op\":\"AND\",\"rules\":[{\"field\":\"lastOrderAt\",\"op\":\"withinDays\",\"value\":99999}]}");
        refused("[1,2]");
        refused("not json");
        StringBuilder deep = new StringBuilder();
        for (int iter = 0; iter < 10; iter++) {
            deep.append("{\"op\":\"AND\",\"rules\":[");
        }
        for (int iter = 0; iter < 10; iter++) {
            deep.append("]}");
        }
        refused(deep.toString());
        StringBuilder wide = new StringBuilder("{\"op\":\"OR\",\"rules\":[");
        for (int iter = 0; iter < 40; iter++) {
            wide.append(iter == 0 ? "" : ",").append("{\"field\":\"ordersCount\",\"op\":\">\",\"value\":1}");
        }
        refused(wide.append("]}").toString());
    }

    // -------------------------------------------------------------- campaigns

    @Test
    void theBetterOpenRateWinsAndATieKeepsA() {
        assertEquals("A", Campaigns.winner(100, 30, 100, 30));
        assertEquals("B", Campaigns.winner(100, 30, 50, 16));
        assertEquals("A", Campaigns.winner(100, 30, 50, 15));
        assertEquals("A", Campaigns.winner(10, 0, 0, 0));
        assertEquals("B", Campaigns.winner(0, 0, 10, 1));
    }

    @Test
    void signaturesVerifyOnlyTheirOwnText() throws Exception {
        String sig = Tokens.sign("secret", "u:abc");
        assertTrue(Tokens.verify("secret", "u:abc", sig));
        assertFalse(Tokens.verify("secret", "u:abd", sig));
        assertFalse(Tokens.verify("other", "u:abc", sig));
        assertFalse(Tokens.verify("secret", "u:abc", null));
        assertFalse(Tokens.verify("secret", "u:abc", sig.substring(1)));
        assertEquals("ada@example.com", Tokens.mailbox(" Ada+news@Example.COM "));
        assertEquals(8, Tokens.code(8).length());
    }

    // --------------------------------------------------------------- journeys

    private static String graph(String nodes, String edges) {
        return "{\"nodes\":[" + nodes + "],\"edges\":[" + edges + "]}";
    }

    private static String node(String id, String type, String params) {
        return "{\"id\":\"" + id + "\",\"data\":{\"type\":\"" + type + "\""
                + (params.length() == 0 ? "" : "," + params) + "}}";
    }

    private static String edge(String from, String port, String to) {
        return "{\"from\":\"" + from + "\",\"fromPort\":\"" + port + "\",\"to\":\"" + to + "\",\"toPort\":\"in\"}";
    }

    @Test
    void journeyGraphsAreChecked() {
        String good = graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "\"event\":\"order.paid\"") + ","
                + node("w", JourneyCatalog.WAIT, "\"minutes\":60") + ","
                + node("x", JourneyCatalog.EXIT, ""), edge("e", "out", "w") + "," + edge("w", "out", "x"));
        assertTrue(Journeys.validate(good).isEmpty(), Journeys.validate(good).toString());
        assertFalse(Journeys.validate("nope").isEmpty());
        // No entry.
        assertFalse(Journeys.validate(graph(node("x", JourneyCatalog.EXIT, ""), "")).isEmpty());
        // Unknown step, missing wait, dangling step, unreached step.
        assertFalse(Journeys.validate(graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "") + ","
                + node("z", "journey.teleport", ""), edge("e", "out", "z"))).isEmpty());
        assertFalse(Journeys.validate(graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "") + ","
                + node("w", JourneyCatalog.WAIT, "\"minutes\":0") + "," + node("x", JourneyCatalog.EXIT, ""),
                edge("e", "out", "w") + "," + edge("w", "out", "x"))).isEmpty());
        assertFalse(Journeys.validate(graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "") + ","
                + node("t", JourneyCatalog.TAG_ADD, "\"tag\":\"x\""), edge("e", "out", "t"))).isEmpty());
        assertFalse(Journeys.validate(graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "") + ","
                + node("x", JourneyCatalog.EXIT, "") + "," + node("y", JourneyCatalog.EXIT, ""),
                edge("e", "out", "x"))).isEmpty());
        assertFalse(Journeys.validate(graph(node("e", JourneyCatalog.ENTRY_TRIGGER, "") + ","
                + node("m", JourneyCatalog.TAG_ADD, "\"tag\":\"x\"") + "," + node("x", JourneyCatalog.EXIT, ""),
                edge("e", "out", "m") + "," + edge("m", "out", "m") + "," + edge("m", "out", "x"))).isEmpty());
        assertEquals(11, JourneyCatalog.all().size());
    }
}
