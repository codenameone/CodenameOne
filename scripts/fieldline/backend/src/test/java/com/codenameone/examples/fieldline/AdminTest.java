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
package com.codenameone.examples.fieldline;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.fieldline.account.Accounts;
import com.codenameone.examples.fieldline.account.DemoAccounts;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.field.Crew;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The admin console's API: roles, suspension and the crew records.
@BackendTest
class AdminTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Crew crew;

    private MockRequestBuilder as(MockRequestBuilder request, String token) {
        return request.header("Host", SignIn.HOST).header("Authorization", "Bearer " + token);
    }

    private String dispatcher() throws Exception {
        return SignIn.token(mvc, DemoAccounts.DISPATCHER, DemoAccounts.PASSWORD);
    }

    private String admin() throws Exception {
        return SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
    }

    @Test
    void onlyAnAdminReachesTheConsole() throws Exception {
        String dispatcher = SignIn.token(mvc, DemoAccounts.DISPATCHER, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/admin/accounts"), dispatcher)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/accounts"), admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
        mvc.perform(as(get("/api/admin/technicians"), admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void aSuspendedAccountIsTurnedAwayAndTakenOffThePlan() throws Exception {
        String username = People.technician(accounts, "suspended");
        String token = SignIn.token(mvc, username, People.PASSWORD);
        mvc.perform(as(get("/api/my/availability"), token)).andExpect(status().isOk());

        mvc.perform(as(Http.body(post("/api/admin/accounts"), "{\"username\":\"" + username
                + "\",\"technician\":true,\"dispatcher\":false,\"admin\":false,"
                + "\"suspended\":true}"), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.suspended").value(true));
        // The token it already holds stops working on its next request.
        mvc.perform(as(get("/api/my/availability"), token)).andExpect(status().isUnauthorized());
        assertFalse(crew.of(username).isActive());

        mvc.perform(as(Http.body(post("/api/admin/accounts"), "{\"username\":\"" + username
                + "\",\"technician\":true,\"dispatcher\":true,\"admin\":false,"
                + "\"suspended\":false}"), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dispatcher").value(true));
        assertTrue(crew.of(username).isActive());
        UserDto now = accounts.describe(username);
        assertTrue(now.dispatcher());
        assertFalse(now.suspended());
    }

    @Test
    void anAdminCannotLockThemselvesOut() throws Exception {
        mvc.perform(as(Http.body(post("/api/admin/accounts"), "{\"username\":\""
                + DemoAccounts.ADMIN + "\",\"technician\":false,\"dispatcher\":true,"
                + "\"admin\":false,\"suspended\":false}"), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/accounts"), "{\"username\":\""
                + DemoAccounts.ADMIN + "\",\"technician\":false,\"dispatcher\":true,"
                + "\"admin\":true,\"suspended\":true}"), admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crewLimitsAreSavedAndChecked() throws Exception {
        String username = People.technician(accounts, "skilled");
        String id = crew.of(username).getId();
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + id
                + "\",\"skills\":[\"HVAC\",\"GAS\"],\"maxDriveMinutes\":90,"
                + "\"regularMinutesPerWeek\":2400,\"maxMinutesPerWeek\":2700,\"active\":true}"),
                admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxDriveMinutes").value(90));
        assertEquals(90, crew.of(username).getMaxDriveMinutes());
        // A week longer than seventy hours is a typo, not a plan.
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + id
                + "\",\"skills\":[\"HVAC\"],\"maxDriveMinutes\":90,"
                + "\"regularMinutesPerWeek\":2400,\"maxMinutesPerWeek\":6000,\"active\":true}"),
                admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + id
                + "\",\"skills\":[],\"maxDriveMinutes\":90,"
                + "\"regularMinutesPerWeek\":2400,\"maxMinutesPerWeek\":2700,\"active\":true}"),
                admin()))
                .andExpect(status().isBadRequest());
        // Certificates carry an expiry, territories and the daily caps are kept.
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + id
                + "\",\"skills\":[\"GAS\"],\"maxDriveMinutes\":90,"
                + "\"regularMinutesPerWeek\":2400,\"maxMinutesPerWeek\":2700,\"active\":true,"
                + "\"certifications\":[\"GAS_SAFE=2030-01-31\"],\"territories\":[\"NORTH\"],"
                + "\"maxJobsPerDay\":5,\"maxMinutesPerDay\":480}"), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxJobsPerDay").value(5))
                .andExpect(jsonPath("$.certifications[0]").value("GAS_SAFE=2030-01-31"));
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + id
                + "\",\"skills\":[\"GAS\"],\"maxDriveMinutes\":90,"
                + "\"regularMinutesPerWeek\":2400,\"maxMinutesPerWeek\":2700,\"active\":true,"
                + "\"certifications\":[\"GAS_SAFE=next spring\"]}"), admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thePolicyIsAdminOnlyAndValidated() throws Exception {
        mvc.perform(as(get("/api/admin/policy"), admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.breakMinutes").value(30));
        mvc.perform(as(get("/api/admin/policy"), dispatcher())).andExpect(status().isForbidden());
    }

    @Test
    void theStaffingReportCoversEveryDay() throws Exception {
        mvc.perform(as(get("/api/dispatch/staffing?days=7"), dispatcher()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(7));
    }
}
