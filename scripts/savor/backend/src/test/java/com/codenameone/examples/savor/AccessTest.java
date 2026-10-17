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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.live.LiveTickets;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Who may call what, with tokens earned by signing in: these go through the
/// decoder and the role lookup the app's own requests go through.
@BackendTest
class AccessTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private LiveTickets tickets;

    private MockRequestBuilder as(MockRequestBuilder request, String token) {
        return request.header("Host", SignIn.HOST).header("Authorization", "Bearer " + token);
    }

    @Test
    void eachRoleReachesItsOwnPartOfTheApi() throws Exception {
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        String courier = SignIn.token(mvc, DemoAccounts.COURIER, DemoAccounts.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);

        mvc.perform(as(get("/api/me"), customer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.courier").value(false));
        mvc.perform(as(get("/api/courier/me"), customer)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/stats"), customer)).andExpect(status().isForbidden());

        mvc.perform(as(get("/api/courier/me"), courier)).andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle").value("Blue Toyota Prius"));
        mvc.perform(as(get("/api/admin/users"), courier)).andExpect(status().isForbidden());
        // A courier is a customer too.
        mvc.perform(as(get("/api/deliveries/active"), courier)).andExpect(status().isOk());

        mvc.perform(as(get("/api/admin/stats"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
        mvc.perform(as(get("/api/admin/users"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/couriers"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/deliveries"), admin)).andExpect(status().isOk());
        // An admin who does not drive is not a courier.
        mvc.perform(as(get("/api/courier/me"), admin)).andExpect(status().isForbidden());
    }

    @Test
    void aRoleGrantedOrAnAccountSuspendedTakesEffectOnTheTokenInHand() throws Exception {
        String username = People.customer(accounts, "promoted");
        String token = SignIn.token(mvc, username, People.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/courier/me"), token)).andExpect(status().isForbidden());

        mvc.perform(as(post("/api/admin/users/" + username + "/roles"), admin)
                        .contentType("application/json").content("{\"courier\":true,\"admin\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courier").value(true));
        // The same token: the role is read from the account, not from it.
        mvc.perform(as(get("/api/courier/me"), token)).andExpect(status().isOk());

        mvc.perform(as(post("/api/admin/users/" + username + "/suspend"), admin)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true));
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isUnauthorized());
        assertNull(SignIn.session(mvc, username, People.PASSWORD));

        mvc.perform(as(post("/api/admin/users/" + username + "/suspend"), admin)
                        .contentType("application/json").content("{\"suspended\":false}"))
                .andExpect(status().isOk());
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isOk());
    }

    @Test
    void anAdminCannotLockThemselvesOut() throws Exception {
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        mvc.perform(as(post("/api/admin/users/" + DemoAccounts.ADMIN + "/roles"), admin)
                        .contentType("application/json").content("{\"courier\":false,\"admin\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/admin/users/" + DemoAccounts.ADMIN + "/suspend"), admin)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/admin/users/nobody@test.example/suspend"), admin)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aLiveTicketIsGoodOnce() throws Exception {
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        mvc.perform(post("/api/live/ticket")).andExpect(status().isUnauthorized());
        String body = mvc.perform(as(post("/api/live/ticket"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(30))
                .andReturn().getResponse().getContentAsString();
        String ticket = (String) com.codename1.backend.Json.parseObject(body).get("ticket");
        assertEquals(DemoAccounts.CUSTOMER, tickets.redeem(ticket));
        assertNull(tickets.redeem(ticket));
        assertNull(tickets.redeem("not-a-ticket"));
    }

    @Test
    void placesAreFoundThroughTheServer() throws Exception {
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        mvc.perform(get("/api/geo/search?q=pier&lat=37.8&lng=-122.4"))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/geo/search?q=PIER&lat=37.8&lng=-122.4"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pier 39"));
        // Too little to search for.
        mvc.perform(as(get("/api/geo/search?q=pi&lat=37.8&lng=-122.4"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(as(get("/api/geo/reverse?lat=37.7956&lng=-122.3936"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ferry Building"))
                .andExpect(jsonPath("$.lat").value(37.7956));
    }
}
