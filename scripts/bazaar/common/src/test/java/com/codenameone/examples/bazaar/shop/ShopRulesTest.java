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
package com.codenameone.examples.bazaar.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Variants, the cart estimate, order steps and the search query, built from
/// DTOs directly. Nothing here opens a display.
class ShopRulesTest {
    private static VariantDto variant(String id, String o1, String o2, int available) {
        return new VariantDto(id, "p", o1 + " / " + o2, o1, o2, null, null, null, 1000, 0, 0, 0,
                true, true, false, 0, available,
                Arrays.asList(new StockDto("l1", "Downtown", available), new StockDto("l2", "Mall", 1)),
                false, "", "", 0L, "", 0, "");
    }

    private static final List<OptionDto> OPTIONS = Arrays.asList(
            new OptionDto("o1", "Size", Arrays.asList("S", "M"), 0),
            new OptionDto("o2", "Colour", Arrays.asList("Red", "Blue"), 1));

    @Test
    void optionsResolveToAVariant() {
        List<VariantDto> vs = Arrays.asList(variant("a", "S", "Red", 0), variant("b", "S", "Blue", 3),
                variant("c", "M", "Red", 9));
        String[] chosen = Variants.initial(OPTIONS, vs);
        assertEquals("S", chosen[0]);
        assertEquals("Blue", chosen[1]);
        assertEquals("b", Variants.resolve(OPTIONS, vs, chosen).id());
        assertNull(Variants.resolve(OPTIONS, vs, new String[] {"M", "Blue", null}));
        assertFalse(Variants.possible(OPTIONS, vs, chosen, 1, "Red"));
        assertTrue(Variants.possible(OPTIONS, vs, new String[] {"M", "Blue", null}, 1, "Red"));
        assertEquals("Downtown", Variants.bestStock(vs.get(2)).locationName());
        assertTrue(Variants.low(new StockDto("l", "x", 3)));
        assertFalse(Variants.low(new StockDto("l", "x", 6)));
    }

    private static CartDto cart(String rate, long shipping, boolean inclusive, long gift) {
        return new CartDto("c", "s", null, "USD", Collections.emptyList(), "SAVE", "GC", 5000, 500,
                shipping, 400, inclusive, 4900 + shipping, gift, 4900 + shipping - gift, false,
                null, null, rate, null, true);
    }

    private static String captions(CartDto cart) {
        StringBuilder out = new StringBuilder();
        for (Estimate.Row row : Estimate.rows(cart)) {
            out.append(row.caption).append(row.note == null ? "" : "(" + row.note + ")").append(';');
        }
        return out.toString();
    }

    @Test
    void estimateRows() {
        assertEquals("Subtotal;Discount;Shipping(At checkout);Tax;Total;",
                captions(cart(null, 0, false, 0)));
        assertEquals("Subtotal;Discount;Shipping(Free);Tax included;Total;Gift card;Due now;",
                captions(cart("r1", 0, true, 1000)));
        List<Estimate.Row> rows = Estimate.rows(cart("r1", 700, false, 0));
        assertEquals(-500, rows.get(1).cents);
        assertEquals(Estimate.SAVING, rows.get(1).kind);
        assertEquals(700, rows.get(2).cents);
    }

    private static OrderDto order(String status, String financial, String fulfilment,
            String... parcels) {
        FulfillmentSummaryDto[] f = new FulfillmentSummaryDto[parcels.length];
        for (int iter = 0; iter < parcels.length; iter++) {
            f[iter] = new FulfillmentSummaryDto("f" + iter, parcels[iter], null, null, 0);
        }
        return new OrderDto("o", "s", 7, "online", status, financial, fulfilment, "u", "e", 0, 0, 0,
                0, 0, 0, "USD", null, null, null, null, null, null, 0, null, null, null, null,
                Arrays.asList(f), 0, null);
    }

    @Test
    void orderSteps() {
        assertEquals(1, OrderSteps.reached(order("OPEN", "PENDING", "UNFULFILLED")));
        assertEquals(2, OrderSteps.reached(order("OPEN", "AUTHORIZED", "UNFULFILLED")));
        assertEquals(3, OrderSteps.reached(order("OPEN", "PAID", "UNFULFILLED", "SHIPPED")));
        assertEquals(4, OrderSteps.reached(order("OPEN", "PAID", "FULFILLED", "DELIVERED")));
        assertEquals(0, OrderSteps.reached(order("CANCELLED", "PAID", "UNFULFILLED")));
        assertTrue(OrderSteps.cancellable(order("OPEN", "AUTHORIZED", "UNFULFILLED")));
        assertFalse(OrderSteps.cancellable(order("OPEN", "AUTHORIZED", "UNFULFILLED", "IN_TRANSIT")));
        assertFalse(OrderSteps.cancellable(order("CANCELLED", "PAID", "UNFULFILLED")));
        assertEquals("Cancelled", OrderSteps.label(order("CANCELLED", "PAID", "UNFULFILLED")));
        assertEquals("Shipped", OrderSteps.label(order("OPEN", "PAID", "PARTIAL")));
    }

    @Test
    void searchQueryParams() {
        SearchQuery q = new SearchQuery();
        assertNull(q.sortParam());
        assertNull(q.words());
        q.q = "  mug ";
        assertEquals("mug", q.words());
        assertEquals("relevance", q.sortParam());
        q.type = "Mug";
        q.min = 500;
        assertEquals(2, q.active());
        assertEquals("500", q.minParam());
        assertNull(q.maxParam());
        q.clearFilters();
        assertEquals(0, q.active());
        assertEquals("mug", q.words());
    }
}
