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
package com.codenameone.examples.parley;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.parley.account.Accounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reports and what the admins do with them.
@BackendTest
class ModerationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void aReportIsResolvedByAnAdmin() throws Exception {
        String reporter = People.user(accounts, "reporter");
        String subject = People.user(accounts, "reported");
        String admin = People.admin(accounts, "moderator");
        assertEquals(400, Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/reports"),
                Chat.map("username", reporter, "conversationId", "", "reason", "me",
                        "evidence", "")), reporter)));
        Map report = Chat.json(mvc, People.asUser(Chat.body(post("/api/reports"),
                Chat.map("username", subject, "conversationId", "", "reason", "Spam",
                        "evidence", "Buy now")), reporter));
        String id = (String) report.get("id");
        assertEquals("OPEN", report.get("status"));

        assertEquals(403, Chat.statusOf(mvc, People.asUser(get("/api/admin/reports"), reporter)));
        List open = Chat.list(mvc, People.asAdmin(get("/api/admin/reports?status=OPEN"), admin));
        boolean listed = false;
        for (Object each : open) {
            listed |= id.equals(((Map) each).get("id"));
        }
        assertTrue(listed);
        assertEquals(400, Chat.statusOf(mvc, People.asAdmin(Chat.body(
                post("/api/admin/reports/" + id + "/resolve"), Chat.map("reason", "")), admin)));
        Map resolved = Chat.json(mvc, People.asAdmin(Chat.body(
                post("/api/admin/reports/" + id + "/resolve"), Chat.map("reason", "Warned")),
                admin));
        assertEquals("RESOLVED", resolved.get("status"));

        Map detail = Chat.json(mvc, People.asAdmin(get("/api/admin/users/" + subject), admin));
        assertEquals(1L, Chat.number(detail, "reportsAgainst"));
        assertEquals("report", ((Map) ((List) detail.get("events")).get(0)).get("action"));
    }

    @Test
    void theDashboardCountsWithoutReading() throws Exception {
        String admin = People.admin(accounts, "counter");
        Map stats = Chat.json(mvc, People.asAdmin(get("/api/admin/stats"), admin));
        assertTrue(Chat.number(stats, "users") > 0);
        Map series = Chat.json(mvc, People.asAdmin(get("/api/admin/stats/series?days=7"), admin));
        assertEquals(7, ((List) series.get("days")).size());
        assertEquals(24, ((List) series.get("byHour")).size());
    }
}
