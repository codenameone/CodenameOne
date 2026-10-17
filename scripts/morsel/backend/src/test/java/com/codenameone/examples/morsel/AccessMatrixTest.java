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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.morsel.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Who may call each of the paths for payments, preferences, onboarding
/// applications, earnings and administration -- every one of them, with tokens
/// earned by signing in.
///
/// A list and not a sample, because the rule for a path is written once, far
/// from the method it guards, and a path left out of it is open to everybody.
@BackendTest
class AccessMatrixTest {
    /// What any signed-in user may call. `GET` first: those are also asked for
    /// with a customer's token, and reading changes nothing.
    private static final String[] SIGNED_IN = {
        "GET /api/account/preferences",
        "GET /api/account/profile",
        "GET /api/payments/config",
        "GET /api/payments/methods",
        "GET /api/payments/receipts",
        "GET /api/payments/receipts/no-such-delivery",
        "GET /api/deliveries/nearby?lat=37.78&lng=-122.41",
        "GET /api/onboarding/application",
        "GET /api/onboarding/application/documents/licence",
        "PUT /api/account/preferences",
        "PUT /api/account/profile",
        "POST /api/account/password",
        "POST /api/account/delete",
        "POST /api/account/push",
        "POST /api/account/push/remove",
        "POST /api/deliveries/no-such-delivery/activity",
        "POST /api/payments/methods/setup",
        "POST /api/payments/methods/setup/none/complete",
        "POST /api/payments/methods/none/default",
        "POST /api/payments/methods/none/remove",
        "POST /api/deliveries/no-such-delivery/tip",
        "PUT /api/onboarding/application",
        "POST /api/onboarding/application/documents",
        "POST /api/onboarding/application/submit",
    };
    private static final String[] COURIERS = {
        "GET /api/courier/earnings/summary",
        "GET /api/courier/payouts",
        "GET /api/courier/payout-account",
        "POST /api/courier/payouts",
        "PUT /api/courier/payout-account",
    };
    private static final String[] ADMINS = {
        "GET /api/admin/stats/series?days=7",
        "GET /api/admin/users/customer@morsel.example",
        "GET /api/admin/applications",
        "GET /api/admin/applications/applicant@morsel.example",
        "GET /api/admin/applications/applicant@morsel.example/documents/licence",
        "GET /api/admin/payments",
        "GET /api/admin/pricing",
        "POST /api/admin/users/customer@morsel.example/flag",
        "POST /api/admin/applications/applicant@morsel.example/approve",
        "POST /api/admin/applications/applicant@morsel.example/reject",
        "POST /api/admin/deliveries/no-such-delivery/cancel",
        "POST /api/admin/deliveries/no-such-delivery/refund",
        "PUT /api/admin/pricing",
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
        return Trips.statusOf(mvc, request);
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
        refused(COURIERS, null, 401);
        refused(ADMINS, null, 401);
        refused(SIGNED_IN, "not-a-token", 401);
    }

    @Test
    void aCustomerIsKeptOutOfOnboardingAndAdministration() throws Exception {
        String customer = SignIn.token(mvc, DemoAccounts.CUSTOMER, DemoAccounts.PASSWORD);
        refused(COURIERS, customer, 403);
        refused(ADMINS, customer, 403);
        admitted(SIGNED_IN, customer);
        // An application is the caller's own: there is no path to another's.
        assertEquals(404, answer("GET /api/onboarding/application/documents/licence", customer));
    }

    @Test
    void aCourierIsKeptOutOfAdministration() throws Exception {
        String courier = SignIn.token(mvc, DemoAccounts.COURIER, DemoAccounts.PASSWORD);
        refused(ADMINS, courier, 403);
        admitted(COURIERS, courier);
        admitted(SIGNED_IN, courier);
    }

    @Test
    void anAdminReachesAdministrationAndDoesNotDrive() throws Exception {
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        admitted(ADMINS, admin);
        admitted(SIGNED_IN, admin);
        refused(COURIERS, admin, 403);
        // The applicant's papers are an admin's to read, and nobody else's.
        assertEquals(200, answer("GET /api/admin/applications/applicant@morsel.example"
                + "/documents/licence", admin));
        String applicant = SignIn.token(mvc, DemoAccounts.APPLICANT, DemoAccounts.PASSWORD);
        assertEquals(200, answer("GET /api/onboarding/application/documents/licence", applicant));
        assertEquals(403, answer("GET /api/admin/applications/applicant@morsel.example"
                + "/documents/licence", applicant));
        // Not approved, so not a courier.
        refused(COURIERS, applicant, 403);
    }
}
