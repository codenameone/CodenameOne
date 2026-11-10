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
package com.codenameone.examples.slate;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.slate.account.Accounts;
import com.codenameone.examples.slate.account.DemoAccounts;
import com.codenameone.examples.slate.live.LiveTickets;
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
        String client = SignIn.token(mvc, DemoAccounts.CLIENT, DemoAccounts.PASSWORD);
        String provider = SignIn.token(mvc, DemoAccounts.PROVIDER, DemoAccounts.PASSWORD);
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);

        mvc.perform(as(get("/api/me"), client)).andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value(false));
        mvc.perform(as(get("/api/provider/me"), client)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/owner/dashboard"), client)).andExpect(status().isForbidden());

        mvc.perform(as(get("/api/provider/me"), provider)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/users"), provider)).andExpect(status().isForbidden());
        // A provider can book like anyone else.
        mvc.perform(as(get("/api/appointments"), provider)).andExpect(status().isOk());

        mvc.perform(as(get("/api/owner/dashboard"), owner)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/users"), owner)).andExpect(status().isOk());
    }

    /// Every account the owner's list shows opens, the demo crowd's included.
    @Test
    void everyListedAccountOpens() throws Exception {
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);
        java.util.List<com.codenameone.examples.slate.api.UserDto> users = accounts.all();
        for (int iter = 0; iter < users.size(); iter++) {
            String username = users.get(iter).username();
            mvc.perform(as(get("/api/admin/users/" + username), owner)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value(username));
        }
    }

    @Test
    void aRoleGrantedOrAnAccountSuspendedTakesEffectOnTheTokenInHand() throws Exception {
        String username = People.account(accounts, "promoted", false);
        String token = SignIn.token(mvc, username, People.PASSWORD);
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/provider/me"), token)).andExpect(status().isForbidden());

        mvc.perform(as(post("/api/admin/users/" + username + "/roles"), owner)
                        .contentType("application/json")
                        .content("{\"provider\":true,\"owner\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value(true));
        // The same token: the role is read from the account, not from it.
        mvc.perform(as(get("/api/provider/me"), token)).andExpect(status().isOk());

        mvc.perform(as(post("/api/admin/users/" + username + "/suspend"), owner)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true));
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isUnauthorized());
        assertNull(SignIn.session(mvc, username, People.PASSWORD));

        mvc.perform(as(post("/api/admin/users/" + username + "/suspend"), owner)
                        .contentType("application/json").content("{\"suspended\":false}"))
                .andExpect(status().isOk());
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isOk());
    }

    @Test
    void anOwnerCannotLockThemselvesOut() throws Exception {
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);
        mvc.perform(as(post("/api/admin/users/" + DemoAccounts.OWNER + "/roles"), owner)
                        .contentType("application/json")
                        .content("{\"provider\":false,\"owner\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/admin/users/" + DemoAccounts.OWNER + "/suspend"), owner)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/admin/users/nobody@test.example/suspend"), owner)
                        .contentType("application/json").content("{\"suspended\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aLiveTicketIsGoodOnce() throws Exception {
        String client = SignIn.token(mvc, DemoAccounts.CLIENT, DemoAccounts.PASSWORD);
        mvc.perform(post("/api/live/ticket")).andExpect(status().isUnauthorized());
        String body = mvc.perform(as(post("/api/live/ticket"), client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(30))
                .andReturn().getResponse().getContentAsString();
        String ticket = (String) com.codename1.backend.Json.parseObject(body).get("ticket");
        assertEquals(DemoAccounts.CLIENT, tickets.redeem(ticket));
        assertNull(tickets.redeem(ticket));
        assertNull(tickets.redeem("not-a-ticket"));
    }
}
