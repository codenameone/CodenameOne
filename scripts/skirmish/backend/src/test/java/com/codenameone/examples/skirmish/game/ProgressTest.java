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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.skirmish.Api;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.domain.Profile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Levels, quests, chests and achievements, all granted by the server.
@BackendTest
class ProgressTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Rooms rooms;

    @Test
    void aNewPlayerStartsAtLevelOneWithThreeDistinctQuests() throws Exception {
        String who = Api.player(accounts, "rookie");
        Map p = Api.json(mvc, Api.as(get("/api/progress"), who));
        assertEquals(1L, Api.number(p, "level"));
        assertEquals(100L, Api.number(p, "levelEnd"));
        List quests = (List) p.get("quests");
        assertEquals(3, quests.size());
        assertTrue(!((Map) quests.get(0)).get("id").equals(((Map) quests.get(1)).get("id")));
        assertTrue(!((Map) quests.get(1)).get("id").equals(((Map) quests.get(2)).get("id")));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/progress/quest"),
                "{\"questId\":\"" + ((Map) quests.get(0)).get("id") + "\"}"), who)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/progress/quest"),
                "{\"questId\":\"nope\"}"), who)));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/progress/chest"), who)));
    }

    @Test
    void theTutorialPaysOnceAndLevelsUp() throws Exception {
        String who = Api.player(accounts, "trainee");
        long coins = accounts.profile(who).getCoins();
        Map r = Api.json(mvc, Api.as(post("/api/progress/tutorial"), who));
        assertTrue(Boolean.TRUE.equals(r.get("levelUp")) || "true".equals(String.valueOf(r.get("levelUp"))));
        assertEquals("Recruit", ((List) r.get("unlocked")).get(0));
        Map again = Api.json(mvc, Api.as(post("/api/progress/tutorial"), who));
        assertEquals(0L, Api.number(again, "coins"));
        Profile after = accounts.profile(who);
        assertEquals(coins + Api.number(r, "coins"), after.getCoins());
        // The tutorial chest and the level-up chest.
        Map p = Api.json(mvc, Api.as(get("/api/progress"), who));
        assertEquals(2L, Api.number(p, "chests"));
        Map chest = Api.json(mvc, Api.as(post("/api/progress/chest"), who));
        assertTrue(Api.number(chest, "coins") > 0);
        assertEquals(1L, Api.number(Api.json(mvc, Api.as(get("/api/progress"), who)), "chests"));
    }

    @Test
    void aBattleCountsTowardsQuestsAndExperience() throws Exception {
        String who = Api.player(accounts, "grinder");
        rooms.queue(accounts.profile(who), null);
        String id = rooms.matchOf(who);
        rooms.play(who, rooms.cheapestInHand(who), 3500, 24000);
        rooms.fastForward(id);
        rooms.tick(System.currentTimeMillis());
        Map p = Api.json(mvc, Api.as(get("/api/progress"), who));
        assertTrue(Api.number(p, "xp") >= 15);
        Map veteran = null;
        for (Object a : (List) p.get("achievements")) {
            if ("veteran".equals(((Map) a).get("id"))) {
                veteran = (Map) a;
            }
        }
        assertEquals(1L, Api.number(veteran, "progress"));
    }
}
