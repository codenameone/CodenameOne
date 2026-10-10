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
import com.codenameone.examples.wayline.live.LiveTickets;
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
        String rider = SignIn.token(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        String driver = SignIn.token(mvc, DemoAccounts.DRIVER, DemoAccounts.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);

        mvc.perform(as(get("/api/me"), rider)).andExpect(status().isOk())
                .andExpect(jsonPath("$.driver").value(false));
        mvc.perform(as(get("/api/driver/me"), rider)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/stats"), rider)).andExpect(status().isForbidden());

        mvc.perform(as(get("/api/driver/me"), driver)).andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle").value("Blue Toyota Prius"));
        mvc.perform(as(get("/api/admin/users"), driver)).andExpect(status().isForbidden());
        // A driver is a rider too.
        mvc.perform(as(get("/api/rides/active"), driver)).andExpect(status().isOk());

        mvc.perform(as(get("/api/admin/stats"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"));
        mvc.perform(as(get("/api/admin/users"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/drivers"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/rides"), admin)).andExpect(status().isOk());
        // An admin who does not drive is not a driver.
        mvc.perform(as(get("/api/driver/me"), admin)).andExpect(status().isForbidden());
    }

    @Test
    void aRoleGrantedOrAnAccountSuspendedTakesEffectOnTheTokenInHand() throws Exception {
        String username = People.rider(accounts, "promoted");
        String token = SignIn.token(mvc, username, People.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/driver/me"), token)).andExpect(status().isForbidden());

        mvc.perform(as(post("/api/admin/users/" + username + "/roles"), admin)
                        .contentType("application/json").content("{\"driver\":true,\"admin\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver").value(true));
        // The same token: the role is read from the account, not from it.
        mvc.perform(as(get("/api/driver/me"), token)).andExpect(status().isOk());

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
                        .contentType("application/json").content("{\"driver\":false,\"admin\":false}"))
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
        String rider = SignIn.token(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        mvc.perform(post("/api/live/ticket")).andExpect(status().isUnauthorized());
        String body = mvc.perform(as(post("/api/live/ticket"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(30))
                .andReturn().getResponse().getContentAsString();
        String ticket = (String) com.codename1.backend.Json.parseObject(body).get("ticket");
        assertEquals(DemoAccounts.RIDER, tickets.redeem(ticket));
        assertNull(tickets.redeem(ticket));
        assertNull(tickets.redeem("not-a-ticket"));
    }

    @Test
    void placesAreFoundThroughTheServer() throws Exception {
        String rider = SignIn.token(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        mvc.perform(get("/api/geo/search?q=pier&lat=37.8&lng=-122.4"))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/geo/search?q=PIER&lat=37.8&lng=-122.4"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pier 39"));
        // Too little to search for.
        mvc.perform(as(get("/api/geo/search?q=pi&lat=37.8&lng=-122.4"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(as(get("/api/geo/reverse?lat=37.7956&lng=-122.3936"), rider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ferry Building"))
                .andExpect(jsonPath("$.lat").value(37.7956));
    }
}
