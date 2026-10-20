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
package com.codenameone.examples.savor;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.order.Quotes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What an admin runs the service by: the prices, and the figures the
/// dashboard draws.
@BackendTest
class PricingAndStatsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Quotes fares;
    @Autowired
    private DataSource db;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    /// The delivery fee of the same order, quoted now.
    private long quoted(String customer) throws Exception {
        return Trips.number(Trips.json(mvc, People.asCustomer(Trips.body(post("/api/orders/quote"),
                Trips.cart(mvc, Trips.trip(50.0755, 14.4378, 50.0900, 14.4600))), customer)),
                "deliveryFeeCents");
    }

    private static String prices(long base, double surge, double commission) {
        return "{\"baseCents\":" + base + ",\"perKmCents\":60,\"surge\":" + surge
                + ",\"smallOrderCents\":1200,\"smallOrderFeeCents\":200,"
                + "\"serviceFeePercent\":10,\"taxPercent\":8,\"commissionPercent\":"
                + commission + "}";
    }

    @Test
    void aChangeOfPriceChangesTheNextQuote() throws Exception {
        // Prague.
        String customer = People.customer(accounts, "prg-customer");
        String before = mvc.perform(admin(get("/api/admin/pricing"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.baseCents").value(199))
                .andExpect(jsonPath("$.perKmCents").value(60))
                .andExpect(jsonPath("$.smallOrderCents").value(1200))
                .andExpect(jsonPath("$.surge").value(1.0))
                .andReturn().getResponse().getContentAsString();
        long was = quoted(customer);
        // The other tests share these prices, so they are put back whatever
        // happens here.
        try {
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), prices(1199, 1, 20))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseCents").value(1199));
            assertEquals(was + 1000L, quoted(customer));
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), prices(199, 2, 20))))
                    .andExpect(status().isOk());
            // Rounded once, so twice the fee to within a cent.
            long surged = quoted(customer);
            assertTrue(Math.abs(surged - was * 2L) <= 1L, "surged to " + surged);
            // Prices that could only be a mistake change nothing.
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), prices(-1, 1, 20))))
                    .andExpect(status().isBadRequest());
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), prices(199, 1, 95))))
                    .andExpect(status().isBadRequest());
            assertEquals(surged, quoted(customer));
            // And they are an admin's to change.
            mvc.perform(People.asCustomer(Trips.body(put("/api/admin/pricing"), before), customer))
                    .andExpect(status().isForbidden());
        } finally {
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), before)))
                    .andExpect(status().isOk());
        }
        assertEquals(was, quoted(customer));
    }

    @Test
    void adminsSavingPricesTogetherAllSave() throws Exception {
        // As a fresh server has it: the row a migration created, which nobody
        // has saved over. The prices are the settings' until somebody does.
        PricingDto before = fares.pricing();
        db.execute("UPDATE pricing SET updated_at = 0 WHERE id = 1", new Object[0]);
        assertEquals(199L, fares.pricing().baseCents());
        Map rows = db.queryOne("SELECT COUNT(*) AS n FROM pricing", new Object[0]);
        assertEquals(1L, ((Number) rows.get("n")).longValue());

        // Several admins save at once. When the first save created the row,
        // they all found none, all inserted it, and all but one failed.
        final int admins = 6;
        final AtomicInteger saved = new AtomicInteger();
        final AtomicInteger ready = new AtomicInteger();
        final AtomicBoolean go = new AtomicBoolean();
        final Throwable[] failed = new Throwable[admins];
        Thread[] threads = new Thread[admins];
        try {
            for (int iter = 0; iter < admins; iter++) {
                final int slot = iter;
                threads[iter] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ready.incrementAndGet();
                        long giveUp = System.currentTimeMillis() + 30000L;
                        while (!go.get() && System.currentTimeMillis() < giveUp) {
                            Thread.yield();
                        }
                        try {
                            fares.save(DemoAccounts.ADMIN, new PricingDto("USD", 300L + slot,
                                    60L, 1d, 1200L, 200L, 10d, 8d, 20d));
                            saved.incrementAndGet();
                        } catch (Throwable err) {
                            failed[slot] = err;
                        }
                    }
                }, "pricing-" + iter);
                threads[iter].start();
            }
            long giveUp = System.currentTimeMillis() + 30000L;
            while (ready.get() < admins) {
                assertTrue(System.currentTimeMillis() < giveUp, "the threads started");
                Thread.sleep(1L);
            }
            go.set(true);
            for (int iter = 0; iter < admins; iter++) {
                threads[iter].join(60000L);
                if (failed[iter] != null) {
                    throw new AssertionError("save " + iter + " failed", failed[iter]);
                }
            }
            assertEquals(admins, saved.get(), "every save was saved");
            long base = fares.pricing().baseCents();
            assertTrue(base >= 300L && base < 300L + admins, "one of them is in force: " + base);
            rows = db.queryOne("SELECT COUNT(*) AS n FROM pricing", new Object[0]);
            assertEquals(1L, ((Number) rows.get("n")).longValue(), "and there is one row");
        } finally {
            // The other tests share these prices.
            fares.save(DemoAccounts.ADMIN, before);
        }
    }

    @Test
    void theDashboardsSeriesHasEveryDayAndEveryHour() throws Exception {
        Map week = Trips.json(mvc, admin(get("/api/admin/stats/series?days=7")));
        List days = (List) week.get("days");
        assertEquals(7, days.size());
        // Oldest first, ending today, and a day with nothing in it is there.
        Map last = (Map) days.get(6);
        assertEquals(Days.iso(System.currentTimeMillis()), last.get("date"));
        assertEquals(Days.iso(System.currentTimeMillis() - 6L * 86400000L),
                ((Map) days.get(0)).get("date"));
        for (int iter = 0; iter < days.size(); iter++) {
            Map day = (Map) days.get(iter);
            assertTrue(Trips.number(day, "orders") >= Trips.number(day, "delivered")
                    + Trips.number(day, "cancelled"), "day " + iter);
            assertTrue(Trips.number(day, "revenue") >= 0L);
            assertNotNull(day.get("newUsers"));
        }
        assertEquals(24, ((List) week.get("byHour")).size());
        // Busiest first.
        List kitchens = (List) week.get("byRestaurant");
        assertTrue(kitchens.size() >= 1);
        for (int iter = 1; iter < kitchens.size(); iter++) {
            assertTrue(Trips.number((Map) kitchens.get(iter - 1), "count")
                    >= Trips.number((Map) kitchens.get(iter), "count"));
        }
        assertEquals("USD", week.get("currency"));
        assertNotNull(week.get("byState"));
        assertTrue(((List) week.get("topCouriers")).size() <= 5);
        // The demo data has somebody waiting to be reviewed and somebody
        // flagged, and a fortnight of orders behind them.
        assertTrue(Trips.number(week, "pendingApplications") >= 1L);
        assertTrue(Trips.number(week, "flaggedUsers") >= 1L);
        assertTrue(Trips.number(week, "averageOrder") > 0L);
        assertTrue(((Number) week.get("averageDeliveryMinutes")).doubleValue() > 0d);
        double rate = ((Number) week.get("completionRate")).doubleValue();
        assertTrue(rate > 0d && rate <= 1d, "completion rate " + rate);
        long placed = 0L;
        for (int iter = 0; iter < days.size(); iter++) {
            placed += Trips.number((Map) days.get(iter), "orders");
        }
        assertTrue(placed > 0L, "the week has orders in it");

        // A silly number is the fortnight; and there is a most.
        mvc.perform(admin(get("/api/admin/stats/series?days=0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(14));
        mvc.perform(admin(get("/api/admin/stats/series?days=1000")))
                .andExpect(jsonPath("$.days.length()").value(90))
                .andExpect(jsonPath("$.byHour.length()").value(24));
    }
}
