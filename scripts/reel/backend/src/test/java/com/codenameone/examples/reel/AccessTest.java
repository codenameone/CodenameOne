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
package com.codenameone.examples.reel;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// Who may call what, with tokens earned by signing in: these go through the
/// decoder and the role lookup the app's own requests go through.
@BackendTest
class AccessTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private MockRequestBuilder as(MockRequestBuilder request, String token) {
        return request.header("Host", SignIn.HOST).header("Authorization", "Bearer " + token);
    }

    @Test
    void thePublicCallsNeedNoToken() throws Exception {
        mvc.perform(get("/api/config").header("Host", SignIn.HOST)).andExpect(status().isOk())
                .andExpect(jsonPath("$.branding.serviceName").exists());
        mvc.perform(get("/api/plans").header("Host", SignIn.HOST)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("basic"));
        mvc.perform(get("/api/verify/NOPE-NOPE").header("Host", SignIn.HOST)).andExpect(status().isNotFound());
        mvc.perform(get("/api/home").header("Host", SignIn.HOST)).andExpect(status().isUnauthorized());
    }

    @Test
    void eachRoleReachesItsOwnPartOfTheApi() throws Exception {
        String viewer = SignIn.token(mvc, DemoAccounts.VIEWER, DemoAccounts.PASSWORD);
        String moderator = SignIn.token(mvc, DemoAccounts.MODERATOR, DemoAccounts.PASSWORD);
        String creator = SignIn.token(mvc, DemoAccounts.CREATOR, DemoAccounts.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);

        mvc.perform(as(get("/api/home"), viewer)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/stats"), viewer)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/moderation/queue"), viewer)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/studio/titles"), viewer)).andExpect(status().isForbidden());

        mvc.perform(as(get("/api/studio/titles"), creator)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        mvc.perform(as(get("/api/moderation/queue"), moderator)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/stats"), moderator)).andExpect(status().isForbidden());

        mvc.perform(as(get("/api/admin/stats"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/admin/users"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/moderation/queue"), admin)).andExpect(status().isOk());
    }

    @Test
    void aSuspensionTakesEffectOnTheTokenInHand() throws Exception {
        String username = People.viewer(accounts, "suspended");
        String token = SignIn.token(mvc, username, People.PASSWORD);
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isOk());
        mvc.perform(as(post("/api/admin/users/" + username + "/suspend"), admin)
                        .contentType("application/json").content("{\"suspended\":true,\"reason\":\"test\"}"))
                .andExpect(status().isOk());
        mvc.perform(as(get("/api/me"), token)).andExpect(status().isUnauthorized());
    }
}
