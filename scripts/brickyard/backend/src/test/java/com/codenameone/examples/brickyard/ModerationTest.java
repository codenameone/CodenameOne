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
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Reports, the queue, sanctions and their effect, appeals, roles, featuring, KPIs and the audit log.
@BackendTest
class ModerationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void aReportedChatLineMutesItsSenderUntilAnAppealIsGranted() throws Exception {
        String rude = Api.player(accounts, "rude");
        String victim = Api.player(accounts, "victim");
        String mod = Api.player(accounts, "moddy");
        Map line = Api.json(mvc, Api.as(Api.body(post("/api/chat/lobby"), "{\"body\":\"go away\"}"), rude));
        Map r = Api.json(mvc, Api.as(Api.body(post("/api/reports"), "{\"targetType\":\"CHAT\",\"targetId\":\""
                + line.get("id") + "\",\"reason\":\"harassment\"}"), victim));
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/reports"), victim)));
        assertTrue(Api.list(mvc, Api.asStaff(get("/api/admin/reports"), mod, Roles.SUPPORT))
                .toString().contains("harassment"));
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/reports/" + r.get("id")
                + "/resolve"), "{\"kind\":\"MUTE\"}"), mod, Roles.SUPPORT)));
        Map done = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/reports/" + r.get("id") + "/resolve"),
                "{\"kind\":\"MUTE\",\"minutes\":60}"), mod, Roles.MODERATOR));
        assertEquals("RESOLVED", done.get("status"));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/chat/lobby"), "{\"body\":\"hi\"}"), rude)));
        List mine = Api.list(mvc, Api.as(get("/api/me/sanctions"), rude));
        String sanctionId = (String) ((Map) mine.get(0)).get("id");
        Map appeal = Api.json(mvc, Api.as(Api.body(post("/api/me/appeals"),
                "{\"sanctionId\":\"" + sanctionId + "\",\"body\":\"sorry\"}"), rude));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/appeals"),
                "{\"sanctionId\":\"" + sanctionId + "\",\"body\":\"again\"}"), rude)));
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/appeals/" + appeal.get("id") + "/decide"),
                "{\"kind\":\"GRANT\",\"reason\":\"first offence\"}"), mod, Roles.MODERATOR));
        Api.json(mvc, Api.as(Api.body(post("/api/chat/lobby"), "{\"body\":\"hi\"}"), rude));
        String audit = Api.list(mvc, Api.asStaff(get("/api/admin/audit"), mod, Roles.MODERATOR)).toString();
        assertTrue(audit.contains("MUTE") && audit.contains("APPEAL_GRANTED") && audit.contains(mod));
    }

    @Test
    void aBanStopsSpendingAndAnUnpublishedExperienceLeavesTheHomeRows() throws Exception {
        String owner = Api.player(accounts, "banned");
        String mod = Api.player(accounts, "banhammer");
        String admin = Api.player(accounts, "curator");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        Map f = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/featured"),
                "{\"rowName\":\"Popular\",\"experienceId\":\"" + id + "\",\"ordinal\":1}"), admin, Roles.ADMIN));
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/featured"),
                "{\"rowName\":\"Popular\",\"experienceId\":\"" + id + "\"}"), mod, Roles.MODERATOR)));
        assertTrue(Api.list(mvc, Api.as(get("/api/home/featured"), owner)).toString().contains((String) f.get("id")));
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/experiences/" + id + "/unpublish"),
                "{\"reason\":\"stolen assets\"}"), mod, Roles.MODERATOR));
        assertTrue(!Api.list(mvc, Api.as(get("/api/home/featured"), owner)).toString().contains((String) f.get("id")));
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/sanctions"),
                "{\"username\":\"" + owner + "\",\"kind\":\"TEMP_BAN\",\"reason\":\"theft\",\"untilAt\":60}"),
                mod, Roles.MODERATOR));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/wallet/receipts"),
                "{\"store\":\"apple\",\"sku\":\"bricks.400\",\"transactionId\":\"t-ban\"}"), owner)));
        assertEquals(1, Api.list(mvc, Api.asStaff(get("/api/admin/users/" + owner + "/sanctions"),
                mod, Roles.SUPPORT)).size());
    }

    @Test
    void onlyAnAdminGrantsRolesAndReadsKpis() throws Exception {
        String admin = Api.player(accounts, "rootadmin");
        String u = Api.player(accounts, "promoted");
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/roles"),
                "{\"username\":\"" + u + "\",\"role\":\"MODERATOR\",\"grant\":1}"), admin, Roles.MODERATOR)));
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/roles"),
                "{\"username\":\"" + u + "\",\"role\":\"MODERATOR\",\"grant\":1}"), admin, Roles.ADMIN));
        assertTrue(accounts.describe(u).roles().contains(Roles.MODERATOR));
        Api.json(mvc, Api.as(Api.body(post("/api/me/presence"), "{\"status\":\"ONLINE\"}"), u));
        Map k = Api.json(mvc, Api.asStaff(get("/api/admin/kpis"), admin, Roles.ADMIN));
        assertTrue(Api.number(k, "dau") >= 1 && Api.number(k, "ccu") >= 1);
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(get("/api/admin/kpis"), admin, Roles.SUPPORT)));
    }
}
