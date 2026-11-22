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
package com.codenameone.examples.bazaar.analytics;

import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Refund;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The dashboard's arithmetic on hand-made rows, period against period.
class AnalyticsMathTest {
    static final long DAY = 86400000L;
    static final long T0 = 100L * DAY;

    private static StoreOrder order(String customer, long at, long subtotal, long discount,
            String channel) {
        StoreOrder o = new StoreOrder();
        o.setId(customer + at);
        o.setCustomerId(customer);
        o.setChannel(channel);
        o.setStatus("OPEN");
        o.setFinancial("PAID");
        o.setFulfilment(Orders.FULFILLED);
        o.setSubtotalCents(subtotal);
        o.setDiscountCents(discount);
        o.setTaxCents(subtotal / 10);
        o.setTotalCents(subtotal - discount + subtotal / 10);
        o.setCreatedAt(Instant.ofEpochMilli(at));
        return o;
    }

    private static Cart cart(long at, String email) {
        Cart c = new Cart();
        c.setCreatedAt(Instant.ofEpochMilli(at));
        c.setEmail(email);
        return c;
    }

    @Test
    void kpisComparePeriods() {
        long[] p = Analytics.period(T0, T0 + 7 * DAY, 0);
        assertEquals(T0 - 7 * DAY, p[2]);
        Analytics.Data d = new Analytics.Data();
        // Previous week: alice once, online.
        d.counted.add(order("alice", T0 - 3 * DAY, 4000, 0, Orders.ONLINE));
        // This week: alice again (returning), bob new, one POS sale with no customer.
        d.counted.add(order("alice", T0 + DAY, 10000, 1000, Orders.ONLINE));
        d.counted.add(order("bob", T0 + 2 * DAY + 3600000L, 6000, 0, Orders.ONLINE));
        d.counted.add(order("", T0 + 2 * DAY, 2000, 0, Orders.POS));
        Refund r = new Refund();
        r.setAmountCents(1500);
        r.setCreatedAt(Instant.ofEpochMilli(T0 + 3 * DAY));
        d.refunds.add(r);
        d.carts.add(cart(T0 + DAY, "a@example.com"));
        d.carts.add(cart(T0 + DAY, ""));
        d.carts.add(cart(T0 + 2 * DAY, "b@example.com"));
        d.carts.add(cart(T0 + 5 * DAY, ""));
        d.carts.add(cart(T0 - DAY, ""));

        List<StoreOrder> now = d.between(p[0], p[1]);
        KpiDto k = Analytics.kpis(d, now, p[0], p[1]);
        assertEquals(18000, k.grossSalesCents());
        assertEquals(1000, k.discountsCents());
        assertEquals(1500, k.refundsCents());
        assertEquals(15500, k.netSalesCents());
        assertEquals(3, k.orders());
        assertEquals(17000 / 3, k.averageOrderCents());
        assertEquals(2, k.customers());
        assertEquals(5000, k.returningCustomerBp());
        assertEquals(4, k.carts());
        assertEquals(5000, k.conversionBp());

        KpiDto prev = Analytics.kpis(d, d.between(p[2], p[0]), p[2], p[0]);
        assertEquals(4000, prev.grossSalesCents());
        assertEquals(1, prev.orders());
        assertEquals(0, prev.returningCustomerBp());
        assertEquals(0, prev.refundsCents());
        assertEquals(1, prev.carts());
        assertEquals(10000, prev.conversionBp());

        KpiDto empty = Analytics.kpis(new Analytics.Data(), new java.util.ArrayList<StoreOrder>(),
                p[0], p[1]);
        assertEquals(0, empty.averageOrderCents());
        assertEquals(0, empty.conversionBp());

        // Orders from carts opened before the period never read above 100%.
        Analytics.Data reused = new Analytics.Data();
        reused.counted.add(order("alice", T0 + DAY, 1000, 0, Orders.ONLINE));
        reused.counted.add(order("bob", T0 + DAY, 1000, 0, Orders.ONLINE));
        reused.carts.add(cart(T0 + DAY, ""));
        assertEquals(10000, Analytics.kpis(reused, reused.between(p[0], p[1]), p[0], p[1])
                .conversionBp());

        List<SeriesPointDto> days = Analytics.daily(now, p[0], p[1]);
        assertEquals(7, days.size());
        assertEquals(9000, days.get(1).salesCents());
        assertEquals(2, days.get(2).orders());
        assertEquals(8000, days.get(2).salesCents());
        List<SeriesPointDto> hours = Analytics.hourly(now);
        assertEquals(24, hours.size());
        assertEquals(1, hours.get(1).orders());

        List<FunnelStepDto> f = Analytics.funnel(d, now, p[0], p[1]);
        assertEquals(4, f.get(0).count());
        assertEquals(2, f.get(1).count());
        assertEquals(2, f.get(2).count());
        assertEquals(2, f.get(3).count());
    }

    @Test
    void cancelledAndDraftOrdersDoNotCount() {
        StoreOrder c = order("x", T0, 100, 0, Orders.ONLINE);
        c.setStatus("CANCELLED");
        assertEquals(false, Analytics.counts(c));
        assertEquals(true, Analytics.counts(order("x", T0, 100, 0, Orders.ONLINE)));
    }
}
