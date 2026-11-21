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
package com.codenameone.examples.bazaar.pos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.mapping.Mappers;
import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosLineDtoCn1Mapper;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosSaleDtoCn1Mapper;
import com.codenameone.examples.bazaar.api.PosSaleResultDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.api.PosTenderDtoCn1Mapper;
import com.codenameone.examples.bazaar.api.TaxRateDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The register's pure logic: totals, tax, discounts, change, the outbox,
/// the snapshot and scanner input. Expected numbers are worked out by hand
/// from the server's rules (half-even rounding, largest remainder).
class PosLogicTest {
    private static PosItemDto item(String id, String product, long price, boolean track,
            int available) {
        return new PosItemDto(id, product, "Mug " + id, "Default", "SKU-" + id, "0000" + id,
                price, 0, true, track, available, "");
    }

    private static DiscountDto discount(String code, String kind, long amount, boolean automatic,
            boolean combinable) {
        return new DiscountDto("d" + code, code, code, kind, amount, 0, 0, 0, "ALL", "",
                automatic, 0, 0, false, combinable, 0, 0, true);
    }

    private static TaxRateDto rate(String region, int bp) {
        return new TaxRateDto("t" + region, "US", region, "Tax " + region, bp, false);
    }

    private static PosCatalogDto catalog(List<PosItemDto> items, List<DiscountDto> discounts,
            List<TaxRateDto> rates, boolean inclusive) {
        return new PosCatalogDto("v1", false, "s", "r", "l", "USD", inclusive, "US", "CA", items,
                rates, discounts);
    }

    @Test
    void roundingIsHalfEven() {
        assertEquals(8, PosPricing.basisPoints(50, 1500));
        assertEquals(4, PosPricing.basisPoints(30, 1500));
        assertEquals(0, PosPricing.divideHalfEven(10, 0));
    }

    @Test
    void exclusiveTaxUsesTheRegionOverTheCountry() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 1999, true, 5), 3);
        PosCatalogDto c = catalog(new ArrayList<PosItemDto>(), new ArrayList<DiscountDto>(),
                Arrays.asList(rate("", 100), rate("CA", 825)), false);
        PosPricing.Totals t = cart.totals(c, null, 0);
        assertEquals(5997, t.subtotalCents);
        assertEquals(495, t.taxCents);
        assertEquals(6492, t.totalCents);
    }

    @Test
    void inclusiveTaxAddsNothing() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 1000, true, 5), 1);
        PosCatalogDto c = new PosCatalogDto("v", false, "s", "r", "l", "EUR", true, "US", "",
                new ArrayList<PosItemDto>(), Arrays.asList(rate("", 2000)),
                new ArrayList<DiscountDto>());
        PosPricing.Totals t = cart.totals(c, null, 0);
        assertEquals(167, t.taxCents);
        assertEquals(1000, t.totalCents);
    }

    @Test
    void codePercentIsSpreadAndTaxedOnTheNet() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 1000, true, 5), 1);
        cart.add(item("2", "p2", 3000, true, 5), 1);
        cart.setCode("save10");
        PosCatalogDto c = catalog(new ArrayList<PosItemDto>(),
                Arrays.asList(discount("SAVE10", "PERCENT", 1000, false, true)),
                Arrays.asList(rate("", 1000)), false);
        PosPricing.Totals t = cart.totals(c, null, 0);
        assertNull(t.codeProblem);
        assertEquals(400, t.discountCents);
        assertEquals(100, t.lines.get(0).discountCents);
        assertEquals(300, t.lines.get(1).discountCents);
        assertEquals(360, t.taxCents);
        assertEquals(3960, t.totalCents);
    }

    @Test
    void theBetterDiscountWinsAndTheCodeIsToldWhy() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 4000, true, 5), 1);
        cart.setCode("SAVE10");
        PosCatalogDto c = catalog(new ArrayList<PosItemDto>(),
                Arrays.asList(discount("AUTO5", "FIXED", 500, true, false),
                        discount("SAVE10", "PERCENT", 1000, false, true)),
                new ArrayList<TaxRateDto>(), false);
        PosPricing.Totals t = cart.totals(c, null, 0);
        assertEquals(500, t.discountCents);
        assertEquals("A better discount already applies", t.codeProblem);
        assertEquals(3500, t.totalCents);
    }

    @Test
    void anUnknownCodeIsReported() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 4000, true, 5), 1);
        cart.setCode("NOPE");
        PosPricing.Totals t = cart.totals(catalog(new ArrayList<PosItemDto>(),
                new ArrayList<DiscountDto>(), new ArrayList<TaxRateDto>(), false), null, 0);
        assertEquals("That code is not valid", t.codeProblem);
        assertEquals(4000, t.totalCents);
    }

    @Test
    void orderDiscountBecomesUnitPrices() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 1000, true, 5), 3);
        cart.add(item("2", "p2", 500, true, 5), 1);
        cart.orderAmount(100);
        assertArrayEquals(new long[] {972, 486}, cart.unitPrices());
        List<PosLineDto> lines = cart.uploadLines();
        assertEquals(972, lines.get(0).unitCents());
        PosPricing.Totals t = cart.totals(null, null, 0);
        assertEquals(3402, t.totalCents);
    }

    @Test
    void lineDiscountsNeverReachZero() {
        PosCart cart = new PosCart();
        cart.add(item("1", "p1", 1000, true, 5), 1);
        cart.linePrice(0, 0);
        assertEquals(1, cart.items().get(0).unitCents());
        cart.linePrice(0, -1);
        cart.linePercent(0, 10000);
        assertEquals(1, cart.items().get(0).unitCents());
        cart.lineAmount(0, 250);
        assertEquals(750, cart.items().get(0).unitCents());
        // A discounted line is kept apart from a new one of the same item.
        cart.add(item("1", "p1", 1000, true, 5), 1);
        assertEquals(2, cart.items().size());
    }

    @Test
    void changeAndSplitTenders() {
        Tenders t = new Tenders(1234);
        assertNull(t.add(Tenders.CARD, 1000, "auth"));
        assertEquals("One card payment per sale", t.add(Tenders.CARD, 100, "x"));
        assertFalse(t.complete());
        assertEquals(234, t.remaining());
        t.add(Tenders.CASH, 500, "");
        assertTrue(t.complete());
        assertEquals(266, t.change());
        Tenders over = new Tenders(1000);
        over.add(Tenders.GIFT_CARD, 1500, "GC");
        assertEquals(1000, over.list().get(0).amountCents());
        assertEquals(0, over.change());
        assertArrayEquals(new long[] {1234, 1300, 1500, 2000, 5000, 10000},
                Tenders.quickCash(1234));
    }

    private static Outbox.Codec codec() {
        PosSaleDtoCn1Mapper.register();
        PosLineDtoCn1Mapper.register();
        PosTenderDtoCn1Mapper.register();
        return new Outbox.Codec() {
            @Override
            public String encode(PosSaleDto sale) {
                return Mappers.toJson(sale);
            }

            @Override
            public PosSaleDto decode(String text) {
                return Mappers.fromJson(text, PosSaleDto.class);
            }
        };
    }

    private static PosSaleDto sale(String id, String variant, int q) {
        return new PosSaleDto(id, "shift", "", "", "", "", 1L,
                Arrays.asList(new PosLineDto(variant, q, 1000, "", true, false)),
                Arrays.asList(new PosTenderDto("CASH", 2000, "")), "", 0L, "");
    }

    private static PosSaleResultDto result(String id, String status) {
        return new PosSaleResultDto(id, status, "o" + id, 7, 1000, 0, "OK".equals(status), "m");
    }

    @Test
    void aSaleRefusedForAnApprovalWaitsAndGoesAgainUnderItsOwnId() {
        final Map<String, String> disk = new HashMap<String, String>();
        Outbox.Persist persist = new Outbox.Persist() {
            @Override
            public String read(String key) {
                return disk.get(key);
            }

            @Override
            public void write(String key, String value) {
                disk.put(key, value);
            }
        };
        Outbox box = new Outbox(persist, codec());
        box.add(sale("q", "v1", 1));
        List<Outbox.Entry> batch = box.due(0);
        box.sending(batch);
        box.answered(batch, Arrays.asList(result("q", "APPROVAL")), 10);
        assertEquals(Outbox.APPROVAL, box.find("q").state);
        assertEquals(1, box.awaitingApproval());
        assertEquals(1, box.errors());
        assertTrue(box.find("q").open(), "the server does not have it");
        assertTrue(box.due(1000).isEmpty(), "it waits for a manager, not a timer");
        assertFalse(box.retry("q"), "a plain retry would only be refused again");
        // The waiting survives a restart.
        Outbox reloaded = new Outbox(persist, codec());
        assertEquals(Outbox.APPROVAL, reloaded.find("q").state);
        assertFalse(reloaded.reapprove("q", ""));
        assertTrue(reloaded.reapprove("q", "appr-1"));
        assertEquals("q", reloaded.find("q").sale.clientId());
        assertEquals("appr-1", reloaded.find("q").sale.approvalId());
        assertEquals(1, reloaded.find("q").sale.lines().size());
        assertEquals(1, reloaded.due(0).size());
        assertEquals("appr-1", new Outbox(persist, codec()).find("q").sale.approvalId());
        assertFalse(reloaded.reapprove("q", "appr-2"), "only a waiting sale is re-approved");
    }

    @Test
    void outboxStateMachine() {
        final Map<String, String> disk = new HashMap<String, String>();
        Outbox.Persist persist = new Outbox.Persist() {
            @Override
            public String read(String key) {
                return disk.get(key);
            }

            @Override
            public void write(String key, String value) {
                disk.put(key, value);
            }
        };
        Outbox box = new Outbox(persist, codec());
        box.add(sale("a", "v1", 2));
        box.add(sale("b", "v2", 1));
        box.add(sale("c", "v1", 1));
        assertNotNull(disk.get(Outbox.KEY));
        assertEquals(3, box.pending());
        List<Outbox.Entry> batch = box.due(100);
        assertEquals(3, batch.size());
        box.sending(batch);
        // A restart mid-request finds the sales waiting again.
        assertEquals(3, new Outbox(persist, codec()).due(100).size());
        box.answered(batch, Arrays.asList(result("c", "ERROR"), result("b", "DUPLICATE"),
                result("a", "OK")), 100);
        assertEquals(Outbox.SYNCED, box.find("a").state);
        assertTrue(box.find("a").oversold);
        assertEquals(Outbox.DUPLICATE, box.find("b").state);
        assertEquals(Outbox.ERROR, box.find("c").state);
        assertEquals(0, box.pending());
        assertEquals(1, box.errors());
        assertEquals(Integer.valueOf(1), box.unsyncedUnits().get("v1"));
        assertTrue(box.retry("c"));
        List<Outbox.Entry> again = box.due(200);
        assertEquals(1, again.size());
        box.sending(again);
        box.unanswered(again, 1000);
        assertEquals(Outbox.PENDING, box.find("c").state);
        assertEquals(3000, box.find("c").nextAt);
        assertTrue(box.due(2999).isEmpty());
        assertEquals(1, box.due(3000).size());
        Outbox reloaded = new Outbox(persist, codec());
        assertEquals(Outbox.SYNCED, reloaded.find("a").state);
        assertEquals("oa", reloaded.find("a").orderId);
        assertEquals(2, reloaded.find("a").sale.lines().get(0).quantity());
        assertEquals(Outbox.PENDING, reloaded.find("c").state);
        // The same client id is one sale.
        reloaded.add(sale("a", "v9", 9));
        assertEquals(3, reloaded.entries().size());
        assertEquals(2000, Outbox.delay(1));
        assertEquals(4000, Outbox.delay(2));
        assertEquals(300000, Outbox.delay(30));
    }

    @Test
    void snapshotMergeAndLocalStock() {
        PosCatalogDto held = catalog(Arrays.asList(item("1", "p", 1000, true, 5),
                item("2", "p", 1000, false, 0)), new ArrayList<DiscountDto>(),
                new ArrayList<TaxRateDto>(), false);
        PosCatalogDto unchanged = new PosCatalogDto("v1", true, "s", "r", "l", "USD", false, "",
                "", new ArrayList<PosItemDto>(), new ArrayList<TaxRateDto>(),
                new ArrayList<DiscountDto>());
        Map<String, Integer> unsynced = new HashMap<String, Integer>();
        unsynced.put("1", Integer.valueOf(2));
        assertSame(held, Snapshot.merge(held, unchanged, unsynced));
        PosCatalogDto fresh = catalog(Arrays.asList(item("1", "p", 1100, true, 4)),
                new ArrayList<DiscountDto>(), new ArrayList<TaxRateDto>(), false);
        PosCatalogDto merged = Snapshot.merge(held, fresh, unsynced);
        assertEquals(2, Snapshot.item(merged, "1").available());
        assertEquals(1100, Snapshot.item(merged, "1").priceCents());
        PosCatalogDto after = Snapshot.sold(held, Arrays.asList(new PosLineDto("1", 7, 0, "", true, false),
                new PosLineDto("2", 3, 0, "", true, false)));
        assertEquals(-2, Snapshot.item(after, "1").available());
        assertEquals(0, Snapshot.item(after, "2").available());
        assertEquals(1, Snapshot.products(after.items()).size());
        assertEquals(2, Snapshot.filter(after.items(), "mug", null).size());
        assertEquals(1, Snapshot.filter(after.items(), "sku-2", null).size());
        assertEquals(0, Snapshot.filter(after.items(), "", Arrays.asList("x")).size());
    }

    @Test
    void wedgeScannerInput() {
        Wedge.Scan s = Wedge.parse("0123456789\r\n");
        assertEquals("0123456789", s.code);
        assertEquals(1, s.quantity);
        Wedge.Scan many = Wedge.parse(" 3*ABC-1\t");
        assertEquals("ABC-1", many.code);
        assertEquals(3, many.quantity);
        assertEquals("*X", Wedge.parse("*X").code);
        assertNull(Wedge.parse("\r\n"));
        assertEquals(1042, Wedge.orderNumber("BAZAAR-ORDER:1042"));
        assertEquals(7, Wedge.orderNumber("bazaar-order:7\r"));
        assertEquals(55, Wedge.orderNumber("#55"));
        assertEquals(-1, Wedge.orderNumber("1042x"));
        assertTrue(Wedge.looksLikeCode("4006381333931"));
        assertFalse(Wedge.looksLikeCode("red mug"));
        List<PosItemDto> items = Arrays.asList(item("1", "p", 1, true, 1), item("2", "p", 1, true, 1));
        assertEquals("2", Wedge.find(items, "00002").variantId());
        assertEquals("1", Wedge.find(items, "sku-1").variantId());
        assertNull(Wedge.find(items, "nothing"));
        assertEquals(165000, CashCount.total(new int[] {1, 1, 0, 0, 3, 0, 0, 0, 0, 0}) * 10);
        assertEquals(-50, CashCount.variance(950, 1000));
    }
}
