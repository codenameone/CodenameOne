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
package com.codenameone.examples.papercut;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.papercut.account.Accounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The console: every route is ADMIN-only, every action lands in the audit
/// log, and REMOVE on a flag takes the run off the boards.
@BackendTest
class AdminTest {
    private static final String ADMIN = "admin@papercut.example";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void everyRouteNeedsTheAdminRole() throws Exception {
        String who = Api.player(accounts, "nosy");
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/stats"), who)));
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/audit"), who)));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/admin/players/" + who + "/action"),
                "{\"action\":\"BAN\",\"reason\":\"me\",\"days\":0}"), who)));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/admin/daily"), "{\"seed\":1}"), who)));
        Map stats = Api.json(mvc, Api.asAdmin(get("/api/admin/stats"), ADMIN));
        assertTrue(Api.number(stats, "players") >= 19);
        assertTrue(Api.number(stats, "flagsOpen") >= 1);
        assertFalse(Api.list(mvc, Api.asAdmin(get("/api/admin/players?q=papercut"), ADMIN)).isEmpty());
    }

    @Test
    void aBanTakesAPlayerOffTheBoardsAndIsAudited() throws Exception {
        String who = Api.player(accounts, "banned");
        Api.submit(mvc, who, "CLASSIC", 12345678, 3600);
        assertEquals(who, top("CLASSIC", "TODAY", who));
        Map banned = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/players/" + who + "/action"),
                "{\"action\":\"BAN\",\"reason\":\"test ban\",\"days\":0}"), ADMIN));
        assertTrue((Boolean) banned.get("suspended"));
        assertFalse(who.equals(top("CLASSIC", "TODAY", "player@papercut.example")));
        assertTrue(audited(who, "BAN"));
        Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/players/" + who + "/action"),
                "{\"action\":\"UNBAN\",\"reason\":\"appeal\",\"days\":0}"), ADMIN));
        assertEquals(who, top("CLASSIC", "TODAY", "player@papercut.example"));
        Map reset = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/players/" + who + "/action"),
                "{\"action\":\"RESET_SCORES\",\"reason\":\"start over\",\"days\":0}"), ADMIN));
        assertEquals(0L, Api.number(reset, "bestClassic"));
        assertFalse(who.equals(top("CLASSIC", "TODAY", "player@papercut.example")));
        assertTrue(audited(who, "RESET_SCORES"));
    }

    @Test
    void removingAFlaggedRunStrikesItAndRecomputesTheBest() throws Exception {
        String who = Api.player(accounts, "suspect");
        Api.submit(mvc, who, "FOLD", 4000, 60);
        Map odd = Api.played(mvc, who, "FOLD", 900000, 300, 10, 60, 20);
        assertTrue((Boolean) odd.get("flagged"));
        String flag = null;
        for (Object f : Api.list(mvc, Api.asAdmin(get("/api/admin/flags?open=true"), ADMIN))) {
            if (who.equals(((Map) f).get("username"))) {
                flag = (String) ((Map) f).get("id");
            }
        }
        assertTrue(flag != null);
        Map closed = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/flags/" + flag + "/resolve"),
                "{\"verdict\":\"REMOVE\"}"), ADMIN));
        assertFalse((Boolean) closed.get("open"));
        Map me = Api.json(mvc, Api.as(get("/api/me"), who));
        assertEquals(4000L, Api.number(me, "bestFold"));
        assertFalse((Boolean) me.get("flagged"));
        for (Object row : Api.list(mvc, Api.as(get("/api/leaderboard?mode=FOLD&period=ALL"), who))) {
            if (who.equals(((Map) row).get("username"))) {
                assertEquals(4000L, Api.number((Map) row, "score"));
            }
        }
        assertEquals(409, Api.statusOf(mvc, Api.asAdmin(Api.body(post("/api/admin/flags/" + flag + "/resolve"),
                "{\"verdict\":\"KEEP\"}"), ADMIN)));
        assertTrue(audited(who, "FLAG REMOVE"));
    }

    @Test
    void aBroadcastIsAuditedAndCountsItsAudience() throws Exception {
        String who = Api.player(accounts, "listener");
        Api.json(mvc, Api.as(Api.body(post("/api/account/push"),
                "{\"provider\":\"fcm\",\"token\":\"device-token-for-listener-0001\",\"platform\":\"ios\"}"), who));
        Map sent = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/broadcast"),
                "{\"title\":\"New paper\",\"body\":\"Gold foil is in the shop\",\"days\":0}"), ADMIN));
        assertTrue(Api.number(sent, "targets") >= 1);
        assertTrue(audited("", "BROADCAST"));
    }

    private String top(String mode, String period, String asker) throws Exception {
        List board = Api.list(mvc, Api.as(get("/api/leaderboard?mode=" + mode + "&period=" + period), asker));
        return board.isEmpty() ? null : (String) ((Map) board.get(0)).get("username");
    }

    private boolean audited(String username, String action) throws Exception {
        for (Object a : Api.list(mvc, Api.asAdmin(get("/api/admin/audit"), ADMIN))) {
            Map row = (Map) a;
            if (username.equals(row.get("username")) && action.equals(row.get("action"))
                    && ADMIN.equals(row.get("admin"))) {
                return true;
            }
        }
        return false;
    }
}
