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
package com.codenameone.examples.skirmish;

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.api.RegisterDto;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The steps most tests take: making a player, calling the API as them, and
/// reading what came back.
public final class Api {
    public static final String PASSWORD = "a-long-password";

    private Api() {
    }

    /// A fresh player of the test's own, so no test meets what another left.
    public static String player(Accounts accounts, String name) throws Exception {
        return accounts.register(new RegisterDto(name + "@test.example", PASSWORD, name)).username();
    }

    public static MockRequestBuilder as(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_PLAYER"));
    }

    public static MockRequestBuilder asAdmin(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_PLAYER", "ROLE_ADMIN"));
    }

    public static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    public static Map json(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return Json.parseObject(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    public static List list(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return (List) Json.parse(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    public static int statusOf(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    public static long number(Map object, String field) {
        return ((Number) object.get(field)).longValue();
    }
}
