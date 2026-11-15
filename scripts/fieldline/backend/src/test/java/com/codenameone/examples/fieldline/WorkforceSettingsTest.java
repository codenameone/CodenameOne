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
import com.codenameone.examples.fieldline.domain.TimeEntry;
import com.codenameone.examples.fieldline.field.Crew;
import com.codenameone.examples.fieldline.repository.TimeEntryRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The company settings beside the planner's weights: rush hours, labour
/// rates, the clock and the timesheets it fills, and the breaks the board
/// draws.
@BackendTest
class WorkforceSettingsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Crew crew;
    @Autowired
    private TimeEntryRepository entries;

    private MockRequestBuilder as(MockRequestBuilder request, String token) {
        return request.header("Host", SignIn.HOST).header("Authorization", "Bearer " + token);
    }

    private String admin() throws Exception {
        return SignIn.token(mvc, DemoAccounts.ADMIN, DemoAccounts.PASSWORD);
    }

    private static String policy(int amFrom, int amTo, int pct, int rounding) {
        return "{\"breakMinutes\":30,\"breakAfterMinutes\":180,\"breakBeforeMinutes\":300,"
                + "\"strictTerritories\":false,\"travelWeight\":10,\"overtimeWeight\":15,"
                + "\"latenessWeight\":30,\"continuityWeight\":300,\"balanceWeight\":40,"
                + "\"territoryWeight\":400,\"urgentSlaHours\":4,\"routineSlaHours\":72,"
                + "\"rushAmFrom\":" + amFrom + ",\"rushAmTo\":" + amTo + ",\"rushPmFrom\":960,"
                + "\"rushPmTo\":1110,\"rushExtraPct\":" + pct + ",\"timeRoundingMinutes\":" + rounding
                + ",\"autoClockOutHours\":12}";
    }

    @Test
    void rushHoursAndClockRulesAreValidated() throws Exception {
        mvc.perform(as(get("/api/admin/policy"), admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.rushExtraPct").value(25))
                .andExpect(jsonPath("$.rushAmFrom").value(420))
                .andExpect(jsonPath("$.timeRoundingMinutes").value(15));
        // A rush hour that ends before it starts, traffic that triples a
        // drive, and a rounding that does not divide an hour.
        mvc.perform(as(Http.body(post("/api/admin/policy"), policy(570, 420, 25, 15)), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/policy"), policy(420, 570, 250, 15)), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/policy"), policy(420, 570, 25, 7)), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/policy"), policy(420, 570, 25, 15)), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rushAmTo").value(570));
    }

    @Test
    void ratesAreAddedAssignedAndOnlyRemovedWhenUnused() throws Exception {
        List seeded = Http.list(mvc, as(get("/api/admin/rates"), admin()));
        assertEquals(3, seeded.size());
        mvc.perform(as(Http.body(post("/api/admin/rates"), "{\"name\":\"Bad\",\"hourlyCents\":3000,"
                + "\"overtimePct\":50,\"weekendPct\":200,\"calloutCents\":0}"), admin()))
                .andExpect(status().isBadRequest());
        List after = Http.list(mvc, as(Http.body(post("/api/admin/rates"), "{\"name\":\"Night crew\","
                + "\"hourlyCents\":4800,\"overtimePct\":175,\"weekendPct\":200,\"calloutCents\":9000}"),
                admin()));
        assertEquals(4, after.size());
        String id = null;
        for (Object o : after) {
            Map r = (Map) o;
            if ("Night crew".equals(r.get("name"))) {
                id = (String) r.get("id");
            }
        }
        String username = People.technician(accounts, "nightshift");
        String tech = crew.of(username).getId();
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + tech
                + "\",\"skills\":[\"HVAC\"],\"maxDriveMinutes\":90,\"regularMinutesPerWeek\":2400,"
                + "\"maxMinutesPerWeek\":2700,\"active\":true,\"rateId\":\"nope\"}"), admin()))
                .andExpect(status().isBadRequest());
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + tech
                + "\",\"skills\":[\"HVAC\"],\"maxDriveMinutes\":90,\"regularMinutesPerWeek\":2400,"
                + "\"maxMinutesPerWeek\":2700,\"active\":true,\"rateId\":\"" + id + "\"}"), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rateId").value(id));
        // Someone is paid at it now, so it stays.
        mvc.perform(as(Http.body(post("/api/admin/rates"), "{\"id\":\"" + id + "\",\"name\":\"\"}"),
                admin())).andExpect(status().isConflict());
        mvc.perform(as(Http.body(post("/api/admin/technicians"), "{\"id\":\"" + tech
                + "\",\"skills\":[\"HVAC\"],\"maxDriveMinutes\":90,\"regularMinutesPerWeek\":2400,"
                + "\"maxMinutesPerWeek\":2700,\"active\":true,\"rateId\":\"rate-standard\"}"), admin()))
                .andExpect(status().isOk());
        assertEquals(3, Http.list(mvc, as(Http.body(post("/api/admin/rates"), "{\"id\":\"" + id
                + "\",\"name\":\"\"}"), admin())).size());
    }

    @Test
    void clockingInAndOutFillsTheTimesheet() throws Exception {
        String username = People.technician(accounts, "clocker");
        Map in = Http.json(mvc, People.asTechnician(Http.body(post("/api/my/clock"), "{\"in\":true}"),
                username));
        assertTrue(Http.number(in, "clockedInAt") > 0);
        // Clocking in twice keeps the shift already open.
        Map again = Http.json(mvc, People.asTechnician(Http.body(post("/api/my/clock"), "{\"in\":true}"),
                username));
        assertEquals(Http.number(in, "clockedInAt"), Http.number(again, "clockedInAt"));
        Map out = Http.json(mvc, People.asTechnician(Http.body(post("/api/my/clock"), "{\"in\":false}"),
                username));
        assertEquals(0, Http.number(out, "clockedInAt"));
        Map sheet = Http.json(mvc, as(get("/api/admin/timesheets?week=0"), admin()));
        String tech = crew.of(username).getId();
        boolean found = false;
        for (Object o : (List) sheet.get("rows")) {
            Map row = (Map) o;
            if (tech.equals(row.get("techId"))) {
                found = true;
                assertEquals("SHIFT", ((Map) ((List) row.get("entries")).get(0)).get("kind"));
            }
        }
        assertTrue(found);
    }

    @Test
    void twoShiftsOpenedTogetherAreCountedOnce() throws Exception {
        String username = People.technician(accounts, "doubletap");
        String tech = crew.of(username).getId();
        long start = System.currentTimeMillis() - 2 * 3600000L;
        for (int i = 0; i < 2; i++) {
            TimeEntry e = new TimeEntry();
            e.setId("tedouble" + i);
            e.setTechId(tech);
            e.setKind("SHIFT");
            e.setJobId("");
            e.setStartAt(start + i * 60000L);
            entries.insert(e);
        }
        Map sheet = Http.json(mvc, as(get("/api/admin/timesheets?week=0"), admin()));
        for (Object o : (List) sheet.get("rows")) {
            Map row = (Map) o;
            if (tech.equals(row.get("techId"))) {
                long shift = Http.number(row, "shiftMinutes");
                assertTrue(shift >= 119 && shift <= 121, "counted once, not twice: " + shift);
            }
        }
        // Reading the clock closes the later duplicate.
        Map day = Http.json(mvc, People.asTechnician(get("/api/my/day"), username));
        assertEquals(start, Http.number(day, "clockedInAt"));
        int open = 0;
        for (TimeEntry e : entries.open(tech)) {
            open += "SHIFT".equals(e.getKind()) ? 1 : 0;
        }
        assertEquals(1, open);
    }

    @Test
    void overlappingShiftsOweTheBreakOfTheWholeSpan() throws Exception {
        // Neither piece alone runs past the default 180 minutes, together
        // they run four hours: one break is owed, not none.
        String username = People.technician(accounts, "overlap");
        String tech = crew.of(username).getId();
        long now = System.currentTimeMillis();
        long[][] spans = {{now - 240 * 60000L, now - 130 * 60000L}, {now - 140 * 60000L, 0}};
        for (int i = 0; i < spans.length; i++) {
            TimeEntry e = new TimeEntry();
            e.setId("teoverlap" + i);
            e.setTechId(tech);
            e.setKind("SHIFT");
            e.setJobId("");
            e.setStartAt(spans[i][0]);
            e.setEndAt(spans[i][1]);
            entries.insert(e);
        }
        Map sheet = Http.json(mvc, as(get("/api/admin/timesheets?week=0"), admin()));
        boolean seen = false;
        for (Object o : (List) sheet.get("rows")) {
            Map row = (Map) o;
            if (tech.equals(row.get("techId"))) {
                seen = true;
                long shift = Http.number(row, "shiftMinutes");
                assertTrue(shift >= 239 && shift <= 241, "merged span: " + shift);
                assertEquals(30, Http.number(row, "breakMinutes"));
            }
        }
        assertTrue(seen);
    }

    @Test
    void lastWeeksTimesheetIsCostedWithOvertime() throws Exception {
        long lastWeek = System.currentTimeMillis() - 7L * 24 * 3600000L;
        Map sheet = Http.json(mvc, as(get("/api/admin/timesheets?week=" + lastWeek), admin()));
        assertTrue(Http.number(sheet, "totalCostCents") > 0);
        int overtime = 0;
        int shifts = 0;
        for (Object o : (List) sheet.get("rows")) {
            overtime += (int) Http.number((Map) o, "overtimeMinutes");
            shifts += (int) Http.number((Map) o, "shiftMinutes");
        }
        assertTrue(shifts > 12 * 5 * 7 * 60, "every technician worked the week: " + shifts);
        assertTrue(overtime > 0);
    }

    @Test
    void theBoardCarriesThePlannedBreaks() throws Exception {
        String dispatcher = SignIn.token(mvc, DemoAccounts.DISPATCHER, DemoAccounts.PASSWORD);
        mvc.perform(as(get("/api/dispatch/board?day=0&days=7"), dispatcher)).andExpect(status().isOk())
                .andExpect(jsonPath("$.breaks[0].techId").exists());
        mvc.perform(as(get("/api/dispatch/staffing?days=7"), dispatcher)).andExpect(status().isOk());
        Map staffing = Http.json(mvc, as(get("/api/dispatch/staffing?days=7"), dispatcher));
        assertTrue(Http.number(staffing, "plannedCostCents") > 0);
    }
}
