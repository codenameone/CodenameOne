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
package com.codenameone.examples.morsel;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.morsel.account.Accounts;
import com.codenameone.examples.morsel.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// What an admin does about an account or a delivery: flag it, block it, cancel
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
        String who = People.customer(accounts, "troublesome");
        String token = SignIn.token(mvc, who, People.PASSWORD);
        Map before = Trips.json(mvc, admin(get("/api/admin/stats/series?days=7")));

        // A flag says why, and changes nothing the account can do.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/flag"),
                        "{\"flagged\":true}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/flag"),
                        "{\"flagged\":true,\"reason\":\"Rude to two couriers\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andExpect(jsonPath("$.flagReason").value("Rude to two couriers"))
                .andExpect(jsonPath("$.suspended").value(false));
        mvc.perform(get("/api/me").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true));
        Map after = Trips.json(mvc, admin(get("/api/admin/stats/series?days=7")));
        assertEquals(Trips.number(before, "flaggedUsers") + 1, Trips.number(after, "flaggedUsers"));

        // Not in the middle of a delivery: nobody could end it. Waiting for a
        // courier is in the middle of one.
        String delivery = (String) Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                People.delivery(41.3874, 2.1686, 41.3974, 2.1786)), who)).get("id");
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + who + "/suspend"),
                        "{\"suspended\":true}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asCustomer(post("/api/deliveries/" + delivery + "/cancel"), who))
                .andExpect(status().isOk());

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
        mvc.perform(Trips.body(post("/api/deliveries").header("Host", SignIn.HOST)
                        .header("Authorization", "Bearer " + token),
                        People.delivery(60.1699, 24.9384, 60.1800, 24.9500)))
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
    void anAdminWhoWasJustRemovedChangesNobody() throws Exception {
        // Two admins taking each other's role at the same moment were both
        // authorized before either committed. The one whose turn comes second
        // is looked at again inside its transaction, and by then is no admin.
        String first = People.customer(accounts, "admin-one");
        String second = People.customer(accounts, "admin-two");
        accounts.setRoles(DemoAccounts.ADMIN, first, false, true);
        accounts.setRoles(DemoAccounts.ADMIN, second, false, true);
        accounts.setRoles(first, second, false, false);
        try {
            accounts.setRoles(second, first, false, false);
            fail("an admin who lost the role still withdrew another's");
        } catch (ResponseStatusException refused) {
            assertEquals(403, refused.getStatus());
        }
        try {
            accounts.suspend(second, first, true, "");
            fail("an admin who lost the role still blocked another");
        } catch (ResponseStatusException refused) {
            assertEquals(403, refused.getStatus());
        }
        assertTrue(accounts.describe(first).admin(), "the first admin is one still");
        assertFalse(accounts.describe(first).suspended(), "and is not blocked");
    }

    @Test
    void theDemoDataHasAFlaggedCustomerWithAHistory() throws Exception {
        Map detail = Trips.json(mvc, admin(get("/api/admin/users/" + DemoAccounts.FLAGGED_CUSTOMER)));
        Map user = (Map) detail.get("user");
        assertEquals(Boolean.TRUE, user.get("flagged"));
        assertTrue(Trips.number(detail, "deliveries") > 0L, "the fortnight gave it deliveries");
        assertEquals("flag", ((Map) ((List) detail.get("events")).get(0)).get("action"));
    }

    @Test
    void anAdminCancelsADeliveryThatIsUnderWay() throws Exception {
        // Helsinki, where nobody drives: the delivery waits.
        String customer = People.customer(accounts, "hel-customer");
        String id = (String) Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                People.delivery(60.1699, 24.9384, 60.1800, 24.9500)), customer)).get("id");
        mvc.perform(admin(Trips.body(post("/api/admin/deliveries/" + id + "/cancel"), "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(Trips.body(post("/api/admin/deliveries/" + id + "/cancel"),
                        "{\"reason\":\"Reported as a prank\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_ADMIN"));
        mvc.perform(People.asCustomer(get("/api/deliveries/active"), customer))
                .andExpect(jsonPath("$.state").value("NONE"));
        mvc.perform(People.asCustomer(get("/api/deliveries/" + id), customer))
                .andExpect(jsonPath("$.state").value("CANCELLED_BY_ADMIN"));
        // Over is over, and a delivery that never was is not found.
        mvc.perform(admin(Trips.body(post("/api/admin/deliveries/" + id + "/cancel"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isConflict());
        mvc.perform(admin(Trips.body(post("/api/admin/deliveries/nosuchdelivery/cancel"),
                        "{\"reason\":\"Again\"}")))
                .andExpect(status().isNotFound());
        // A delivery that ended this way cost nothing, so it has no receipt.
        mvc.perform(People.asCustomer(get("/api/payments/receipts/" + id), customer))
                .andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/admin/users/" + customer)))
                .andExpect(jsonPath("$.events[0].action").value("cancel_delivery"))
                .andExpect(jsonPath("$.events[0].reason").value("Reported as a prank"));
    }

    @Test
    void aCourierKeepsTheRoleUntilTheDeliveryTheyAreOnHasEnded() throws Exception {
        // Tallinn.
        String customer = People.customer(accounts, "tll-customer");
        String courier = People.courier(accounts, "tll-courier");
        Trips.online(mvc, courier, 59.4370, 24.7536);
        String id = (String) Trips.json(mvc, People.asCustomer(Trips.body(post("/api/deliveries"),
                People.delivery(59.4370, 24.7536, 59.4470, 24.7636)), customer)).get("id");
        mvc.perform(People.asCourier(post("/api/courier/offers/" + id + "/accept"), courier))
                .andExpect(status().isOk());

        // Withdrawn now, the courier could do nothing more for this customer.
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + courier + "/roles"),
                        "{\"courier\":false,\"admin\":false}")))
                .andExpect(status().isConflict());
        mvc.perform(People.asCourier(post("/api/courier/deliveries/" + id + "/arrived"), courier))
                .andExpect(status().isOk());

        mvc.perform(admin(Trips.body(post("/api/admin/deliveries/" + id + "/cancel"),
                        "{\"reason\":\"Courier is leaving\"}")))
                .andExpect(status().isOk());
        mvc.perform(admin(Trips.body(post("/api/admin/users/" + courier + "/roles"),
                        "{\"courier\":false,\"admin\":false}")))
                .andExpect(status().isOk());
    }
}
