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

/// XP, the login streak, quests and the season pass.
@BackendTest
class ProgressionTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private Map event(String u, String name, int amount) throws Exception {
        return Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/progress"),
                "{\"username\":\"" + u + "\",\"eventName\":\"" + name + "\",\"amount\":" + amount + "}")));
    }

    @Test
    void theFirstLoginOfADayPaysOnce() throws Exception {
        String u = Api.player(accounts, "streaker");
        Map first = Api.json(mvc, Api.as(post("/api/me/login"), u));
        assertEquals(1, Api.number(first, "streak"));
        assertEquals(5, Api.number(first, "bricksAwarded"));
        assertEquals(0, Api.number(Api.json(mvc, Api.as(post("/api/me/login"), u)), "bricksAwarded"));
    }

    @Test
    void engineEventsAdvanceXpAndQuests() throws Exception {
        String admin = Api.player(accounts, "questadmin");
        String u = Api.player(accounts, "quester");
        Map q = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/quests"),
                "{\"title\":\"Collect coins\",\"cadence\":\"DAILY\",\"eventName\":\"coin-q1\",\"target\":5,"
                        + "\"rewardXp\":100,\"rewardBricks\":20}"), admin, Roles.ADMIN));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/engine/progress"),
                "{\"username\":\"" + u + "\",\"eventName\":\"coin-q1\",\"amount\":5}"), u)));
        event(u, "coin-q1", 3);
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/me/quests/" + q.get("id") + "/claim"), u)));
        Map p = event(u, "coin-q1", 3);
        assertEquals(60, Api.number(p, "xp"));
        Map claimed = Api.json(mvc, Api.as(post("/api/me/quests/" + q.get("id") + "/claim"), u));
        assertEquals(1, Api.number(claimed, "claimed"));
        assertEquals(5, Api.number(claimed, "progress"));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/me/quests/" + q.get("id") + "/claim"), u)));
        Map after = Api.json(mvc, Api.as(get("/api/me/progress"), u));
        assertEquals(160, Api.number(after, "xp"));
        assertEquals(2, Api.number(after, "level"));
        assertEquals(Accounts.START_BRICKS + 20,
                Api.number(Api.json(mvc, Api.as(get("/api/me/wallet"), u)), "balance"));
    }

    @Test
    void theSeasonPassPaysEachTierOncePerTrack() throws Exception {
        String u = Api.player(accounts, "seasoned");
        List tiers = Api.list(mvc, Api.as(get("/api/season"), u));
        assertEquals(10, tiers.size());
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u)));
        for (int i = 0; i < 6; i++) {
            event(u, "play", 10);
        }
        Api.json(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u)));
        assertEquals(402, Api.statusOf(mvc, Api.as(post("/api/me/season/claim?tier=1&track=PREMIUM"), u)));
        assertEquals(402, Api.statusOf(mvc, Api.as(post("/api/me/season/premium"), u)));
        String admin = Api.player(accounts, "seasonadmin");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/economy/adjust"),
                "{\"username\":\"" + u + "\",\"amount\":400,\"reason\":\"test\"}"), admin, Roles.ADMIN));
        Api.json(mvc, Api.as(post("/api/me/season/premium"), u));
        Map t = Api.json(mvc, Api.as(post("/api/me/season/claim?tier=1&track=PREMIUM"), u));
        assertEquals(1, Api.number(t, "premiumClaimed"));
        assertEquals(Accounts.START_BRICKS + 10 + 25,
                Api.number(Api.json(mvc, Api.as(get("/api/me/wallet"), u)), "balance"));
    }
}
