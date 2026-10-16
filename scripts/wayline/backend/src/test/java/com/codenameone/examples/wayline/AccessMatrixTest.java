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
package com.codenameone.examples.wayline;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.wayline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Who may call each of the paths for payments, preferences, driving
/// applications, earnings and administration -- every one of them, with tokens
/// earned by signing in.
///
/// A list and not a sample, because the rule for a path is written once, far
/// from the method it guards, and a path left out of it is open to everybody.
@BackendTest
class AccessMatrixTest {
    /// What any signed-in user may call. `GET` first: those are also asked for
    /// with a rider's token, and reading changes nothing.
    private static final String[] SIGNED_IN = {
        "GET /api/account/preferences",
        "GET /api/account/profile",
        "GET /api/payments/config",
        "GET /api/payments/methods",
        "GET /api/payments/receipts",
        "GET /api/payments/receipts/no-such-ride",
        "GET /api/rides/nearby?lat=37.78&lng=-122.41",
        "GET /api/driving/application",
        "GET /api/driving/application/documents/licence",
        "PUT /api/account/preferences",
        "PUT /api/account/profile",
        "POST /api/account/password",
        "POST /api/account/delete",
        "POST /api/account/push",
        "POST /api/account/push/remove",
        "POST /api/rides/no-such-ride/activity",
        "POST /api/payments/methods/setup",
        "POST /api/payments/methods/setup/none/complete",
        "POST /api/payments/methods/none/default",
        "POST /api/payments/methods/none/remove",
        "POST /api/rides/no-such-ride/tip",
        "PUT /api/driving/application",
        "POST /api/driving/application/documents",
        "POST /api/driving/application/submit",
    };
    private static final String[] DRIVERS = {
        "GET /api/driver/earnings/summary",
        "GET /api/driver/payouts",
        "GET /api/driver/payout-account",
        "POST /api/driver/payouts",
        "PUT /api/driver/payout-account",
    };
    private static final String[] ADMINS = {
        "GET /api/admin/stats/series?days=7",
        "GET /api/admin/users/rider@wayline.example",
        "GET /api/admin/applications",
        "GET /api/admin/applications/applicant@wayline.example",
        "GET /api/admin/applications/applicant@wayline.example/documents/licence",
        "GET /api/admin/payments",
        "GET /api/admin/pricing",
        "POST /api/admin/users/rider@wayline.example/flag",
        "POST /api/admin/applications/applicant@wayline.example/approve",
        "POST /api/admin/applications/applicant@wayline.example/reject",
        "POST /api/admin/rides/no-such-ride/cancel",
        "POST /api/admin/rides/no-such-ride/refund",
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
        refused(DRIVERS, null, 401);
        refused(ADMINS, null, 401);
        refused(SIGNED_IN, "not-a-token", 401);
    }

    @Test
    void aRiderIsKeptOutOfDrivingAndAdministration() throws Exception {
        String rider = SignIn.token(mvc, DemoAccounts.RIDER, DemoAccounts.PASSWORD);
        refused(DRIVERS, rider, 403);
        refused(ADMINS, rider, 403);
        admitted(SIGNED_IN, rider);
        // An application is the caller's own: there is no path to another's.
        assertEquals(404, answer("GET /api/driving/application/documents/licence", rider));
    }

    @Test
    void aDriverIsKeptOutOfAdministration() throws Exception {
        String driver = SignIn.token(mvc, DemoAccounts.DRIVER, DemoAccounts.PASSWORD);
        refused(ADMINS, driver, 403);
        admitted(DRIVERS, driver);
        admitted(SIGNED_IN, driver);
    }

    @Test
    void anAdminReachesAdministrationAndDoesNotDrive() throws Exception {
        String admin = SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
        admitted(ADMINS, admin);
        admitted(SIGNED_IN, admin);
        refused(DRIVERS, admin, 403);
        // The applicant's papers are an admin's to read, and nobody else's.
        assertEquals(200, answer("GET /api/admin/applications/applicant@wayline.example"
                + "/documents/licence", admin));
        String applicant = SignIn.token(mvc, DemoAccounts.APPLICANT, DemoAccounts.PASSWORD);
        assertEquals(200, answer("GET /api/driving/application/documents/licence", applicant));
        assertEquals(403, answer("GET /api/admin/applications/applicant@wayline.example"
                + "/documents/licence", applicant));
        // Not approved, so not a driver.
        refused(DRIVERS, applicant, 403);
    }
}
