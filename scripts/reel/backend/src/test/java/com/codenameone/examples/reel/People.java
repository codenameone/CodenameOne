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

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.api.SignupDto;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// Accounts made for one test, and requests made as one.
public final class People {
    public static final String PASSWORD = "a-long-password";

    private People() {
    }

    public static String viewer(Accounts accounts, String name) throws Exception {
        return accounts.register(new SignupDto(name + "@test.example", PASSWORD, name)).username();
    }

    /// Saves the simulated card that always works and starts the plan's trial.
    public static void subscribe(MockMvc mvc, String user, String plan) throws Exception {
        String setup = mvc.perform(asViewer(post("/api/payments/methods/setup"), user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = (String) Json.parseObject(setup).get("id");
        mvc.perform(asViewer(post("/api/payments/methods/setup/" + id + "/complete"), user)
                        .contentType("application/json")
                        .content("{\"number\":\"4242424242424242\",\"expMonth\":12,\"expYear\":2040,"
                                + "\"cvc\":\"123\",\"holder\":\"T\"}"))
                .andExpect(status().isOk());
        mvc.perform(asViewer(post("/api/billing/subscribe"), user).contentType("application/json")
                        .content("{\"planCode\":\"" + plan + "\"}"))
                .andExpect(status().isOk());
    }

    public static MockRequestBuilder asViewer(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_VIEWER"));
    }

    public static MockRequestBuilder asCreator(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_VIEWER", "ROLE_CREATOR"));
    }

    public static MockRequestBuilder asModerator(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_VIEWER", "ROLE_MODERATOR"));
    }

    public static MockRequestBuilder asAdmin(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_VIEWER", "ROLE_ADMIN"));
    }
}
