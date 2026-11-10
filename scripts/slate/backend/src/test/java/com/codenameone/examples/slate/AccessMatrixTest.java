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
import com.codenameone.examples.slate.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Who may call each path, for every role -- with tokens earned by signing
/// in.
///
/// A list and not a sample, because the rule for a path is written once, far
/// from the method it guards, and a path left out of it is open to everybody.
@BackendTest
class AccessMatrixTest {
    /// What any signed-in user may call. `GET` first: those are also asked for
    /// with a client's token, and reading changes nothing.
    private static final String[] SIGNED_IN = {
        "GET /api/account/preferences",
        "GET /api/account/profile",
        "GET /api/payments/config",
        "GET /api/payments/methods",
        "GET /api/business",
        "GET /api/business/policy",
        "GET /api/locations",
        "GET /api/services",
        "GET /api/providers",
        "GET /api/slots?service=svc-1&location=loc-main&day=2030-01-07",
        "GET /api/appointments",
        "GET /api/appointments/history",
        "GET /api/waitlist",
        "PUT /api/account/preferences",
        "PUT /api/account/profile",
        "POST /api/account/password",
        "POST /api/account/delete",
        "POST /api/account/push",
        "POST /api/slots/find",
        "POST /api/appointments/hold",
        "POST /api/appointments/none/confirm",
        "POST /api/appointments/none/cancel",
        "POST /api/appointments/none/reschedule",
        "POST /api/waitlist",
        "POST /api/waitlist/none/cancel",
        "POST /api/payments/methods/setup",
    };

    /// A provider's own diary.
    private static final String[] PROVIDERS = {
        "GET /api/provider/me",
        "GET /api/provider/schedule?from=0&days=7",
        "GET /api/provider/hours",
        "GET /api/provider/time-off",
        "GET /api/provider/calendars",
        "GET /api/provider/calendars/config",
        "PUT /api/provider/me",
        "PUT /api/provider/hours",
        "POST /api/provider/time-off",
        "POST /api/provider/calendars",
        "POST /api/provider/calendars/none/sync",
        "POST /api/provider/busy",
        "POST /api/provider/appointments/none/complete",
        "POST /api/provider/appointments/none/no-show",
        "POST /api/provider/appointments/none/cancel",
    };

    /// Running the business.
    private static final String[] OWNERS = {
        "GET /api/owner/services",
        "GET /api/owner/providers",
        "GET /api/owner/providers/provider@slate.example/hours",
        "GET /api/owner/locations",
        "GET /api/owner/resources",
        "GET /api/owner/schedule?day=2030-01-07",
        "GET /api/owner/dashboard",
        "GET /api/admin/users",
        "GET /api/admin/users/client@slate.example",
        "PUT /api/owner/business",
        "PUT /api/owner/policy",
        "PUT /api/owner/services",
        "PUT /api/owner/providers",
        "PUT /api/owner/providers/provider@slate.example/hours",
        "PUT /api/owner/locations",
        "PUT /api/owner/resources",
        "POST /api/owner/preview",
        "POST /api/admin/users/client@slate.example/flag",
        "POST /api/admin/users/client@slate.example/roles",
        "POST /api/admin/users/client@slate.example/suspend",
    };

    @Autowired
    private MockMvc mvc;

    private static MockRequestBuilder request(String call) {
        int space = call.indexOf(' ');
        String method = call.substring(0, space);
        String path = call.substring(space + 1);
        MockRequestBuilder request = "GET".equals(method) ? get(path)
                : "PUT".equals(method) ? put(path) : post(path);
        return "GET".equals(method) ? request
                : request.contentType("application/json").content("{}");
    }

    private int answer(String call, String token) throws Exception {
        MockRequestBuilder request = request(call).header("Host", SignIn.HOST);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private void refused(String[] calls, String token, int status) throws Exception {
        for (int iter = 0; iter < calls.length; iter++) {
            assertEquals(status, answer(calls[iter], token), calls[iter]);
        }
    }

    /// The readers among `calls` get past the door: whatever they answer, it
    /// is not a refusal to say who may ask.
    private void admitted(String[] calls, String token) throws Exception {
        for (int iter = 0; iter < calls.length; iter++) {
            if (calls[iter].startsWith("GET ")) {
                int status = answer(calls[iter], token);
                assertTrue(status != 401 && status != 403, calls[iter] + " answered " + status);
            }
        }
    }

    @Test
    void nothingIsAnsweredWithoutSigningIn() throws Exception {
        refused(SIGNED_IN, null, 401);
        refused(PROVIDERS, null, 401);
        refused(OWNERS, null, 401);
        refused(SIGNED_IN, "not-a-token", 401);
    }

    @Test
    void aClientIsKeptOutOfTheBusinessSide() throws Exception {
        String client = SignIn.token(mvc, DemoAccounts.CLIENT, DemoAccounts.PASSWORD);
        refused(PROVIDERS, client, 403);
        refused(OWNERS, client, 403);
        admitted(SIGNED_IN, client);
    }

    @Test
    void aProviderKeepsTheirDiaryAndDoesNotRunTheBusiness() throws Exception {
        String provider = SignIn.token(mvc, DemoAccounts.PROVIDER, DemoAccounts.PASSWORD);
        admitted(PROVIDERS, provider);
        admitted(SIGNED_IN, provider);
        refused(OWNERS, provider, 403);
    }

    @Test
    void theOwnerRunsTheBusinessAndKeepsNoDiary() throws Exception {
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);
        admitted(OWNERS, owner);
        admitted(SIGNED_IN, owner);
        // An owner who also takes bookings is given the provider role too;
        // owning the business is not having a diary in it.
        refused(PROVIDERS, owner, 403);
    }
}
