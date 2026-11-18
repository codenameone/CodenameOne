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
package com.codenameone.examples.brickyard;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.account.Accounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Engine event ingest and the creator dashboard.
@BackendTest
class AnalyticsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private void event(String id, String kind, String user, String name, String detail, int version) throws Exception {
        Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/events"), "{\"experienceId\":\"" + id
                + "\",\"kind\":\"" + kind + "\",\"username\":\"" + user + "\",\"name\":\"" + name
                + "\",\"amount\":30,\"detail\":\"" + detail + "\",\"version\":" + version + "}")));
    }

    @Test
    void theDashboardCountsVisitsAndGroupsErrors() throws Exception {
        String owner = Api.player(accounts, "analyst");
        String other = Api.player(accounts, "nosy");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        event(id, "VISIT", "p1", "", "", 1);
        event(id, "VISIT", "p2", "", "", 1);
        event(id, "ERROR", "p1", "attempt to index nil", "Script:3", 1);
        event(id, "ERROR", "p2", "attempt to index nil", "Script:4", 1);
        event(id, "ERROR", "p2", "attempt to index nil", "Script:4", 2);
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/engine/events"),
                "{\"experienceId\":\"" + id + "\",\"kind\":\"VISIT\",\"amount\":0,\"version\":1}"), owner)));
        assertEquals(400, Api.statusOf(mvc, Api.asEngine(Api.body(post("/api/engine/events"),
                "{\"experienceId\":\"" + id + "\",\"kind\":\"NOPE\",\"amount\":0,\"version\":1}"))));
        List visits = Api.list(mvc, Api.as(get("/api/experiences/" + id + "/analytics/visits?days=7"), owner));
        assertEquals(7, visits.size());
        assertEquals(2, Api.number((Map) visits.get(6), "count"));
        List errors = Api.list(mvc, Api.as(get("/api/experiences/" + id + "/analytics/errors"), owner));
        assertEquals(2, errors.size());
        assertEquals(2, Api.number((Map) errors.get(0), "count"));
        assertEquals("Script:4", ((Map) errors.get(0)).get("sampleStack"));
        Map retention = Api.json(mvc, Api.as(get("/api/experiences/" + id + "/analytics/retention"), owner));
        assertEquals(0, Api.number(retention, "cohortD1"));
        assertEquals(0, Api.list(mvc, Api.as(get("/api/experiences/" + id + "/analytics/revenue"), owner)).size());
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/experiences/" + id + "/analytics/visits?days=7"), other)));
    }
}
