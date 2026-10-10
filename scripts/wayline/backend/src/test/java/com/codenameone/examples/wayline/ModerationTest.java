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
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What an admin does about an account or a ride: flag it, block it, cancel
/// it, and the record that is kept of each.
@BackendTest
class ModerationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    @Test
    void anAccountIsFlaggedThenBlockedAndTheRecordSaysWhy() throws Exception {
        String who = People.rider(accounts, "troublesome");
        String token = SignIn.token(mvc, who, People.PASSWORD);
        Map before = Trips.json(mvc, admin(get("/api/admin/stats/series?days=7")));

        // A flag says why, and changes nothing the account can do.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/flag"),
                        "{\"flagged\":true}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/flag"),
                        "{\"flagged\":true,\"reason\":\"Rude to two drivers\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andExpect(jsonPath("$.flagReason").value("Rude to two drivers"))
                .andExpect(jsonPath("$.suspended").value(false));
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true));
        Map after = Trips.json(mvc, admin(get("/api/admin/stats/series?days=7")));
        assertEquals(Trips.number(before, "flaggedUsers") + 1, Trips.number(after, "flaggedUsers"));

        // A block is what stops it: the token in hand, and signing in again.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/suspend"),
                        "{\"suspended\":true,\"reason\":\"And then a third\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true));
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments/methods").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(Trips.body(post("/api/rides").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token),
                        People.ride(60.1699, 24.9384, 60.1800, 24.9500)))
                .andExpect(status().isUnauthorized());
        assertEquals(null, SignIn.session(mvc, who, People.PASSWORD));

        // The account's record, newest first: who did what, and why.
        Map detail = Trips.json(mvc, admin(get("/api/admin/users/" + who)));
        assertEquals(who, ((Map) detail.get("user")).get("username"));
        List events = (List) detail.get("events");
        assertEquals(2, events.size());
        Map block = (Map) events.get(0);
        assertEquals("block", block.get("action"));
        assertEquals("And then a third", block.get("reason"));
        assertEquals(DemoAccounts.ADMIN, block.get("by"));
        assertTrue(Trips.number(block, "at") > 0L);
        assertEquals("flag", ((Map) events.get(1)).get("action"));

        // Both are undone, and that is recorded too.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/suspend"),
                        "{\"suspended\":false}")))
                .andExpect(status().isOk());
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/flag"),
                        "{\"flagged\":false}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(false))
                .andExpect(jsonPath("$.flagReason").value(""));
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/admin/users/" + who)))
                .andExpect(jsonPath("$.events.length()").value(4))
                .andExpect(jsonPath("$.events[0].action").value("unflag"))
                .andExpect(jsonPath("$.events[1].action").value("unblock"));
        mvc.perform(admin(get("/api/admin/users/nobody@test.example")))
                .andExpect(status().isNotFound());
    }

    @Test
    void theDemoDataHasAFlaggedRiderWithAHistory() throws Exception {
        Map detail = Trips.json(mvc, admin(get("/api/admin/users/" + DemoAccounts.FLAGGED_RIDER)));
        Map user = (Map) detail.get("user");
        assertEquals(Boolean.TRUE, user.get("flagged"));
        assertTrue(Trips.number(detail, "rides") > 0L, "the fortnight gave it rides");
        assertEquals("flag", ((Map) ((List) detail.get("events")).get(0)).get("action"));
    }

    @Test
    void anAdminCancelsARideThatIsUnderWay() throws Exception {
        // Helsinki, where nobody drives: the ride waits.
        String rider = People.rider(accounts, "hel-rider");
        String id = (String) Trips.json(mvc, People.asRider(Trips.body(post("/api/rides"),
                People.ride(60.1699, 24.9384, 60.1800, 24.9500)), rider)).get("id");
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/cancel"), "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/cancel"),
                        "{\"reason\":\"Reported as a prank\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_ADMIN"));
        mvc.perform(People.asRider(get("/api/rides/active"), rider))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asRider(get("/api/rides/" + id), rider))
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_ADMIN"));
        // Over is over, and a ride that never was is not found.
        mvc.perform(admin(Trips.body(post("/api/admin/rides/" + id + "/cancel"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        mvc.perform(admin(Trips.body(post("/api/admin/rides/nosuchride/cancel"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isNotFound());
        // A ride that ended this way cost nothing, so it has no receipt.
        mvc.perform(People.asRider(get("/api/payments/receipts/" + id), rider))
                .andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/admin/users/" + rider)))
                .andExpect(jsonPath("$.events[0].action").value("cancel_ride"))
                .andExpect(jsonPath("$.events[0].reason").value("Reported as a prank"));
    }
}
