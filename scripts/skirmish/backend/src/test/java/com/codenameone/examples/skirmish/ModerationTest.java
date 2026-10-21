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
package com.codenameone.examples.skirmish;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.account.DemoAccounts;
import com.codenameone.examples.skirmish.admin.Flags;
import com.codenameone.examples.skirmish.admin.Moderation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// God mode: finding a problem player, acting on them, and the record kept.
@BackendTest
class ModerationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Flags flags;
    @Autowired
    private Moderation moderation;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return Api.asAdmin(request, DemoAccounts.ADMIN);
    }

    @Test
    void onlyAdminsSeeTheConsole() throws Exception {
        String who = Api.player(accounts, "curious");
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/stats"), who)));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/admin/players/" + who
                + "/action"), "{\"action\":\"BAN\",\"reason\":\"me\"}"), who)));
        Map stats = Api.json(mvc, admin(get("/api/admin/stats")));
        assertTrue(Api.number(stats, "players") >= 4);
    }

    @Test
    void aFlaggedPlayerIsFoundSuspendedAndTheRecordSaysWhy() throws Exception {
        String who = Api.player(accounts, "griefer");
        flags.auto(who, "Sent 40 invalid plays in one match", null);
        List open = Api.list(mvc, admin(get("/api/admin/flags?open=true")));
        Map flag = null;
        for (Object o : open) {
            if (who.equals(((Map) o).get("username"))) {
                flag = (Map) o;
            }
        }
        assertTrue(flag != null, "the automatic flag is in the queue");
        assertEquals(Flags.AUTO, flag.get("kind"));

        List found = Api.list(mvc, admin(get("/api/admin/players?q=griefer")));
        assertEquals(1, found.size());
        assertEquals(Boolean.TRUE, ((Map) found.get(0)).get("flagged"));

        assertEquals(400, Api.statusOf(mvc, admin(Api.body(post("/api/admin/players/" + who
                + "/action"), "{\"action\":\"SUSPEND\",\"reason\":\"griefing\",\"days\":0}"))));
        Map player = Api.json(mvc, admin(Api.body(post("/api/admin/players/" + who + "/action"),
                "{\"action\":\"SUSPEND\",\"reason\":\"griefing\",\"days\":3}")));
        assertEquals(Boolean.TRUE, player.get("suspended"));
        assertFalse(accounts.isEnabled(who));

        Api.json(mvc, admin(post("/api/admin/flags/" + flag.get("id") + "/resolve")));
        Map detail = Api.json(mvc, admin(get("/api/admin/players/" + who)));
        List actions = (List) detail.get("actions");
        assertTrue(((String) ((Map) actions.get(0)).get("action")).startsWith(Moderation.SUSPEND));
        assertEquals(DemoAccounts.ADMIN, ((Map) actions.get(0)).get("admin"));
        assertEquals("griefing", ((Map) actions.get(0)).get("reason"));
        assertEquals(Boolean.FALSE, ((Map) ((List) detail.get("flags")).get(0)).get("open"));

        // A suspension ends by itself.
        assertTrue(moderation.lift(System.currentTimeMillis() + 4L * 24 * 3600 * 1000) >= 1);
        assertTrue(accounts.isEnabled(who));
    }

    @Test
    void anAdminCannotActOnThemselves() throws Exception {
        assertEquals(400, Api.statusOf(mvc, admin(Api.body(post("/api/admin/players/"
                + DemoAccounts.ADMIN + "/action"), "{\"action\":\"BAN\",\"reason\":\"oops\"}"))));
    }

    @Test
    void theDemoTroublemakerIsWaitingInTheQueue() throws Exception {
        Map detail = Api.json(mvc, admin(get("/api/admin/players/" + DemoAccounts.TROUBLE)));
        assertEquals(Boolean.TRUE, ((Map) detail.get("player")).get("flagged"));
    }
}
