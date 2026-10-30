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
package com.codenameone.examples.pulse;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.pulse.account.DemoAccounts;
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
    /// with a member's token, and reading changes nothing.
    private static final String[] SIGNED_IN = {
        "GET /api/account/preferences",
        "GET /api/account/profile",
        "GET /api/payments/config",
        "GET /api/payments/methods",
        "GET /api/household",
        "GET /api/bookings",
        "GET /api/bookings/history",
        "GET /api/studio/locations",
        "GET /api/studio/types",
        "GET /api/studio/trainers",
        "GET /api/studio/classes",
        "GET /api/membership",
        "GET /api/membership/plans",
        "GET /api/membership/invoices",
        "GET /api/engagement/stats",
        "GET /api/engagement/away",
        "GET /api/engagement/nudges",
        "GET /api/engagement/workouts",
        "GET /api/social/friends",
        "GET /api/social/feed",
        "PUT /api/account/preferences",
        "PUT /api/account/profile",
        "POST /api/account/password",
        "POST /api/account/delete",
        "POST /api/account/push",
        "POST /api/bookings",
        "POST /api/bookings/none/cancel",
        "POST /api/household",
        "POST /api/membership",
        "POST /api/engagement/away",
        "POST /api/engagement/heartrate",
        "POST /api/social/friends",
        "POST /api/social/invite",
        "POST /api/payments/methods/setup",
    };
    private static final String[] TRAINERS = {
        "GET /api/trainer/classes",
        "GET /api/trainer/shifts",
        "GET /api/trainer/classes/none/roster",
        "POST /api/trainer/classes/none/attendance",
        "POST /api/trainer/shifts/none/swap",
        "POST /api/trainer/shifts/none/take",
    };
    private static final String[] DESK = {
        "GET /api/desk/members?q=riley",
        "GET /api/desk/today",
        "POST /api/desk/checkin",
        "POST /api/desk/walkins",
    };
    private static final String[] OWNERS = {
        "GET /api/owner/dashboard",
        "GET /api/owner/plans",
        "GET /api/owner/rooms",
        "GET /api/owner/staff",
        "GET /api/owner/shifts?location=loc-downtown",
        "GET /api/owner/campaigns",
        "GET /api/owner/invoices",
        "GET /api/admin/users",
        "GET /api/admin/users/member@pulse.example",
        "PUT /api/owner/plans",
        "PUT /api/owner/types",
        "PUT /api/owner/staff",
        "POST /api/owner/classes",
        "POST /api/owner/classes/none/cancel",
        "POST /api/owner/shifts",
        "POST /api/owner/campaigns",
        "POST /api/owner/invoices/none/void",
        "POST /api/admin/users/member@pulse.example/flag",
        "POST /api/admin/users/member@pulse.example/roles",
        "POST /api/admin/users/member@pulse.example/suspend",
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
        refused(TRAINERS, null, 401);
        refused(DESK, null, 401);
        refused(OWNERS, null, 401);
        refused(SIGNED_IN, "not-a-token", 401);
    }

    @Test
    void aMemberIsKeptOutOfTheStaffSide() throws Exception {
        String member = SignIn.token(mvc, DemoAccounts.MEMBER, DemoAccounts.PASSWORD);
        refused(TRAINERS, member, 403);
        refused(DESK, member, 403);
        refused(OWNERS, member, 403);
        admitted(SIGNED_IN, member);
    }

    @Test
    void aTrainerTeachesAndDoesNotRunTheDoorOrTheBooks() throws Exception {
        String trainer = SignIn.token(mvc, DemoAccounts.TRAINER, DemoAccounts.PASSWORD);
        admitted(TRAINERS, trainer);
        admitted(SIGNED_IN, trainer);
        refused(DESK, trainer, 403);
        refused(OWNERS, trainer, 403);
    }

    @Test
    void theDeskRunsTheDoorOnly() throws Exception {
        String desk = SignIn.token(mvc, DemoAccounts.DESK, DemoAccounts.PASSWORD);
        admitted(DESK, desk);
        refused(TRAINERS, desk, 403);
        refused(OWNERS, desk, 403);
    }

    @Test
    void theOwnerReachesEverything() throws Exception {
        String owner = SignIn.token(mvc, DemoAccounts.OWNER, DemoAccounts.PASSWORD);
        admitted(OWNERS, owner);
        admitted(DESK, owner);
        admitted(TRAINERS, owner);
        admitted(SIGNED_IN, owner);
    }
}
