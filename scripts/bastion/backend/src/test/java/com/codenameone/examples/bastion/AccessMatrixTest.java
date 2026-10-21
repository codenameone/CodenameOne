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
package com.codenameone.examples.bastion;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bastion.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Who may reach what: the admin routes for admins only, the rest for anyone
/// signed in, and nothing for a stranger.
@BackendTest
class AccessMatrixTest {
    private static final String[] ADMINS = {
        "/api/admin/audit/verify",
        "/api/admin/ledger/verify",
    };
    private static final String[] SIGNED_IN = {
        "/api/me",
    };

    @Autowired
    private MockMvc mvc;

    private MockResponse answer(String path, String token) throws Exception {
        MockRequestBuilder request = get(path).header("Host", SignIn.HOST);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    @Test
    void strangersAreAskedToSignIn() throws Exception {
        for (String path : ADMINS) {
            assertEquals(401, answer(path, null).getStatus(), path);
        }
        for (String path : SIGNED_IN) {
            assertEquals(401, answer(path, null).getStatus(), path);
        }
    }

    @Test
    void customersCannotReachTheAdminRoutes() throws Exception {
        String token = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        for (String path : ADMINS) {
            assertEquals(403, answer(path, token).getStatus(), path);
        }
        for (String path : SIGNED_IN) {
            assertEquals(200, answer(path, token).getStatus(), path);
        }
    }

    @Test
    void adminsFindTheRecordsIntact() throws Exception {
        String token = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        for (String path : ADMINS) {
            MockResponse answer = answer(path, token);
            assertEquals(200, answer.getStatus(), path);
            assertEquals(Boolean.TRUE, Json.parseObject(answer.getContentAsString()).get("intact"),
                    path + ": " + answer.getContentAsString());
        }
    }

    @Test
    void aForgedTokenIsRefused() throws Exception {
        String token = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        String forged = token.substring(0, token.length() - 4) + "AAAA";
        assertEquals(401, answer("/api/me", forged).getStatus());
    }
}
