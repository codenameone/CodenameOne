/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    private long quoted(String rider) throws Exception {
        return Trips.number(Trips.json(mvc, People.asRider(Trips.body(post("/api/rides/quote"),
                People.ride(50.0755, 14.4378, 50.0900, 14.4600)), rider)), "amountCents");
    }

    @Test
    void aChangeOfPriceChangesTheNextQuote() throws Exception {
        // Prague.
        String rider = People.rider(accounts, "prg-rider");
        String before = mvc.perform(admin(get("/api/admin/pricing"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.baseFare").value(250))
                .andExpect(jsonPath("$.perKm").value(120))
                .andExpect(jsonPath("$.minimumFare").value(500))
                .andExpect(jsonPath("$.surgeMultiplier").value(1.0))
                .andReturn().getResponse().getContentAsString();
        long was = quoted(rider);
        // The other tests share these prices, so they are put back whatever
        // happens here.
        try {
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), "{\"baseFare\":1250,"
                            + "\"perKm\":120,\"perMinute\":30,\"minimumFare\":500,"
                            + "\"serviceFeePercent\":10,\"commissionPercent\":20,"
                            + "\"surgeMultiplier\":1,\"comfortMultiplier\":1.35,"
                            + "\"xlMultiplier\":1.7}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baseFare").value(1250));
            assertEquals(was + 1000L, quoted(rider));
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), "{\"baseFare\":250,"
                            + "\"perKm\":120,\"perMinute\":30,\"minimumFare\":500,"
                            + "\"serviceFeePercent\":10,\"commissionPercent\":20,"
                            + "\"surgeMultiplier\":2,\"comfortMultiplier\":1.35,"
                            + "\"xlMultiplier\":1.7}")))
                    .andExpect(status().isOk());
            // Part by part, each rounded, so twice the fare to within a cent
            // or two.
            long surged = quoted(rider);
            assertTrue(Math.abs(surged - was * 2L) <= 2L, "surged to " + surged);
            // Prices that could only be a mistake change nothing.
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), "{\"baseFare\":-1,"
                            + "\"perKm\":120,\"perMinute\":30,\"minimumFare\":500,"
                            + "\"serviceFeePercent\":10,\"commissionPercent\":20,"
                            + "\"surgeMultiplier\":1,\"comfortMultiplier\":1.35,"
                            + "\"xlMultiplier\":1.7}")))
                    .andExpect(status().isBadRequest());
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), "{\"baseFare\":250,"
                            + "\"perKm\":120,\"perMinute\":30,\"minimumFare\":500,"
                            + "\"serviceFeePercent\":10,\"commissionPercent\":95,"
                            + "\"surgeMultiplier\":1,\"comfortMultiplier\":1.35,"
                            + "\"xlMultiplier\":1.7}")))
                    .andExpect(status().isBadRequest());
            assertEquals(surged, quoted(rider));
            // And they are an admin's to change.
            mvc.perform(People.asRider(Trips.body(put("/api/admin/pricing"), before), rider))
                    .andExpect(status().isForbidden());
        } finally {
            mvc.perform(admin(Trips.body(put("/api/admin/pricing"), before)))
                    .andExpect(status().isOk());
        }
        assertEquals(was, quoted(rider));
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
            assertTrue(Trips.number(day, "rides") >= Trips.number(day, "completed")
                    + Trips.number(day, "cancelled"), "day " + iter);
            assertTrue(Trips.number(day, "revenue") >= 0L);
            assertNotNull(day.get("newUsers"));
        }
        assertEquals(24, ((List) week.get("byHour")).size());
        List products = (List) week.get("byProduct");
        assertEquals(3, products.size());
        assertEquals("standard", ((Map) products.get(0)).get("name"));
        assertEquals("comfort", ((Map) products.get(1)).get("name"));
        assertEquals("xl", ((Map) products.get(2)).get("name"));
        assertEquals("USD", week.get("currency"));
        assertNotNull(week.get("byState"));
        assertTrue(((List) week.get("topDrivers")).size() <= 5);
        // The demo data has somebody waiting to be reviewed and somebody
        // flagged, and a fortnight of rides behind them.
        assertTrue(Trips.number(week, "pendingApplications") >= 1L);
        assertTrue(Trips.number(week, "flaggedUsers") >= 1L);
        assertTrue(Trips.number(week, "averageFare") > 0L);
        double rate = ((Number) week.get("completionRate")).doubleValue();
        assertTrue(rate > 0d && rate <= 1d, "completion rate " + rate);
        long rides = 0L;
        for (int iter = 0; iter < days.size(); iter++) {
            rides += Trips.number((Map) days.get(iter), "rides");
        }
        assertTrue(rides > 0L, "the week has rides in it");

        // A silly number is the fortnight; and there is a most.
        mvc.perform(admin(get("/api/admin/stats/series?days=0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(14));
        mvc.perform(admin(get("/api/admin/stats/series?days=1000")))
                .andExpect(jsonPath("$.days.length()").value(90))
                .andExpect(jsonPath("$.byHour.length()").value(24));
    }
}
