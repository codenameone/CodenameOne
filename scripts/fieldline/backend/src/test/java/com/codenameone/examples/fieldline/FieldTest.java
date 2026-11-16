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
import com.codenameone.examples.fieldline.account.DemoAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The field work end to end: a job driven from the road to a signed record,
/// a drag on the board checked against the rules, an absence covered by the
/// scheduler, and the assistant answering.
@BackendTest
class FieldTest {
    /// A 1x1 PNG, standing in for a photograph and a signature.
    static final String PIXEL = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
            + "YPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired
    private MockMvc mvc;

    private Map board() throws Exception {
        return Http.json(mvc, People.asDispatcher(get("/api/dispatch/board?day=0&days=5"),
                DemoAccounts.DISPATCHER));
    }

    /// An open job of the technician's, of the kind asked for if there is one.
    private Map jobOf(String techId, String kind) throws Exception {
        Map found = null;
        for (Object o : (List) board().get("jobs")) {
            Map job = (Map) o;
            if (techId.equals(job.get("techId")) && "SCHEDULED".equals(job.get("status"))) {
                if (kind.equals(job.get("kind"))) {
                    return job;
                }
                if (found == null) {
                    found = job;
                }
            }
        }
        assertNotNull(found, "the plan gave " + techId + " no work");
        return found;
    }

    @Test
    void theDemoWeekIsPlanned() throws Exception {
        Map board = board();
        List jobs = (List) board.get("jobs");
        assertTrue(jobs.size() >= 60, "jobs: " + jobs.size());
        int assigned = 0;
        for (Object o : jobs) {
            if (((String) ((Map) o).get("techId")).length() > 0) {
                assigned++;
            }
        }
        // A handful may not fit anyone's week; most must.
        assertTrue(assigned * 10 >= jobs.size() * 8, assigned + " of " + jobs.size());
        assertNotNull(board.get("kpis"));
        assertNotNull(board.get("lastRun"));
    }

    @Test
    void aJobIsTakenFromTheRoadToASignedRecord() throws Exception {
        String me = DemoAccounts.SECOND_TECHNICIAN;
        Map job = jobOf("t102", "INSPECTION");
        String id = (String) job.get("id");
        String path = "/api/jobs/" + id;

        mvc.perform(People.asTechnician(Http.body(post(path + "/status"),
                        "{\"status\":\"EN_ROUTE\",\"lat\":0,\"lng\":0}"), me))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EN_ROUTE"));
        // Never backwards.
        mvc.perform(People.asTechnician(Http.body(post(path + "/status"),
                        "{\"status\":\"SCHEDULED\",\"lat\":0,\"lng\":0}"), me))
                .andExpect(status().isConflict());
        // Checked in a long way from the site: allowed, and noted.
        mvc.perform(People.asTechnician(Http.body(post(path + "/status"),
                        "{\"status\":\"ON_SITE\",\"lat\":45.0,\"lng\":-122.0}"), me))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ON_SITE"));
        // Somebody else's job is not theirs to see.
        mvc.perform(People.asTechnician(get(path), DemoAccounts.TECHNICIAN))
                .andExpect(status().isForbidden());

        // Not done until every line is answered.
        mvc.perform(People.asTechnician(Http.body(post(path + "/status"),
                        "{\"status\":\"COMPLETED\",\"lat\":0,\"lng\":0}"), me))
                .andExpect(status().isConflict());
        Map detail = Http.json(mvc, People.asTechnician(get(path), me));
        List checks = (List) detail.get("checklist");
        assertFalse(checks.isEmpty());
        StringBuilder answers = new StringBuilder("{\"checklist\":[");
        for (int iter = 0; iter < checks.size(); iter++) {
            answers.append(iter == 0 ? "" : ",").append("{\"index\":").append(iter)
                    .append(",\"label\":\"\",\"result\":\"").append(iter == 1 ? "FAIL" : "PASS")
                    .append("\",\"note\":\"").append(iter == 1 ? "Worn, replace next visit" : "")
                    .append("\"}");
        }
        answers.append("],\"notes\":\"All good apart from one item.\"}");
        mvc.perform(People.asTechnician(Http.body(put(path + "/checklist"), answers.toString()), me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklist[1].result").value("FAIL"));

        // A photo, and what the assistant makes of it.
        Map photo = Http.json(mvc, People.asTechnician(Http.body(post(path + "/photos"),
                "{\"data\":\"" + PIXEL + "\",\"mime\":\"image/png\",\"note\":\"Corrosion on the fitting\"}"),
                me));
        Map diagnosis = (Map) photo.get("diagnosis");
        assertNotNull(diagnosis);
        assertTrue(((String) diagnosis.get("issue")).length() > 0);
        Map report = Http.json(mvc, People.asTechnician(post(path + "/report"), me));
        assertTrue(((String) report.get("summary")).length() > 0);

        Map signed = Http.json(mvc, People.asTechnician(Http.body(post(path + "/signature"),
                "{\"signerName\":\"Chris Customer\",\"png\":\"" + PIXEL + PIXEL
                        + "\",\"lat\":45.5,\"lng\":-122.6}"), me));
        assertEquals(64, ((String) signed.get("reportHash")).length());
        mvc.perform(People.asTechnician(get(path + "/signature"), me))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.signerName").value("Chris Customer"));
        // The signed record is closed.
        mvc.perform(People.asTechnician(Http.body(put(path + "/checklist"), answers.toString()), me))
                .andExpect(status().isConflict());

        mvc.perform(People.asTechnician(Http.body(post(path + "/status"),
                        "{\"status\":\"COMPLETED\",\"lat\":0,\"lng\":0}"), me))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void aDragIsCheckedAgainstTheRules() throws Exception {
        // Priya climbs elevators; Luis does plumbing and gas.
        Map job = jobOf("t103", "MAINTENANCE");
        Map answer = Http.json(mvc, People.asDispatcher(Http.body(post("/api/dispatch/move"),
                "{\"jobId\":\"" + job.get("id") + "\",\"techId\":\"t104\",\"start\":"
                        + job.get("plannedStart") + ",\"apply\":false}"), DemoAccounts.DISPATCHER));
        if (!((List) job.get("skills")).contains("PLUMBING") && !((List) job.get("skills")).contains("GAS")) {
            assertEquals(Boolean.FALSE, answer.get("ok"));
            assertTrue(((String) answer.get("message")).length() > 0);
        }
        // Technicians do not plan.
        mvc.perform(People.asTechnician(Http.body(post("/api/dispatch/move"), "{}"),
                DemoAccounts.TECHNICIAN)).andExpect(status().isForbidden());
    }

    @Test
    void anAbsenceIsCoveredByTheRestOfTheCrew() throws Exception {
        Map board = board();
        long from = ((Number) board.get("dayStart")).longValue();
        long to = from + ((Number) board.get("days")).longValue() * 24 * 3600 * 1000;
        Map run = Http.json(mvc, People.asDispatcher(Http.body(post("/api/dispatch/absence"),
                "{\"techId\":\"t108\",\"start\":" + from + ",\"end\":" + to
                        + ",\"reason\":\"Sick\"}"), DemoAccounts.DISPATCHER));
        assertNotNull(run.get("id"));
        for (Object o : (List) board().get("jobs")) {
            Map job = (Map) o;
            if ("SCHEDULED".equals(job.get("status"))) {
                assertFalse("t108".equals(job.get("techId")), "still on t108: " + job.get("id"));
            }
        }
    }

    @Test
    void theAssistantAnswersWithoutAModel() throws Exception {
        Map reply = Http.json(mvc, People.asTechnician(Http.body(post("/api/assistant"),
                "{\"message\":\"The boiler shows a lockout, what should I check?\",\"history\":[]}"),
                DemoAccounts.TECHNICIAN));
        assertEquals("demo", reply.get("source"));
        assertTrue(((String) reply.get("reply")).length() > 20, (String) reply.get("reply"));
    }
}
