/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.papercut.account.Accounts;
import com.codenameone.examples.papercut.api.RunDto;
import com.codenameone.examples.papercut.run.DailySeeds;
import com.codenameone.examples.papercut.run.Runs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The leaderboards' periods, the friends board and the daily sheet.
@BackendTest
class BoardTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Runs runs;

    @Test
    void aBoardKeepsEachPlayersBestWithinItsPeriod() throws Exception {
        String now = Api.player(accounts, "nowplayer");
        String old = Api.player(accounts, "oldtimer");
        Api.submit(mvc, now, "FOLD", 8000000, 3600);
        Api.submit(mvc, now, "FOLD", 7000000, 3600);
        runs.accept(old, "FOLD", new RunDto(null, "FOLD", 8100000, 4060, 40, 3600, 20),
                System.currentTimeMillis() - 20 * Clock.DAY);
        List all = Api.list(mvc, Api.as(get("/api/leaderboard?mode=FOLD&period=ALL"), now));
        assertTrue(all.size() <= 50);
        Map first = (Map) all.get(0);
        Map second = (Map) all.get(1);
        assertEquals(old, first.get("username"));
        assertEquals(now, second.get("username"));
        assertTrue((Boolean) second.get("me"));
        assertEquals(2L, Api.number(second, "rank"));
        assertEquals(1, count(all, now));
        for (String period : new String[] {"WEEK", "TODAY"}) {
            List windowed = Api.list(mvc, Api.as(get("/api/leaderboard?mode=FOLD&period=" + period), now));
            assertEquals(now, ((Map) windowed.get(0)).get("username"));
            assertEquals(0, count(windowed, old));
        }
        assertEquals(400, Api.statusOf(mvc, Api.as(get("/api/leaderboard?mode=CHESS&period=ALL"), now)));
    }

    @Test
    void theFriendsBoardIsTheAskerAndTheirFriends() throws Exception {
        String me = Api.player(accounts, "befriender");
        String pal = Api.player(accounts, "palfriend");
        Api.submit(mvc, me, "CLASSIC", 5000, 60);
        Api.submit(mvc, pal, "CLASSIC", 6000, 60);
        List friends = Api.list(mvc, Api.as(Api.body(post("/api/friends"), "{\"name\":\"palfriend\"}"), me));
        assertEquals(1, friends.size());
        assertEquals(6000L, Api.number((Map) friends.get(0), "bestClassic"));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/friends"),
                "{\"name\":\"" + me + "\"}"), me)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/friends"),
                "{\"name\":\"nobody at all\"}"), me)));
        List board = Api.list(mvc, Api.as(get("/api/leaderboard?mode=CLASSIC&period=FRIENDS"), me));
        assertEquals(2, board.size());
        assertEquals(pal, ((Map) board.get(0)).get("username"));
        assertTrue((Boolean) ((Map) board.get(1)).get("me"));
        Api.list(mvc, Api.as(Api.body(post("/api/friends/remove"), "{\"name\":\"" + pal + "\"}"), me));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/leaderboard?mode=CLASSIC&period=FRIENDS"), me)).size());
    }

    @Test
    void theDailySeedIsStableWithinADayAndAnAdminCanPickIt() throws Exception {
        assertEquals("1970-01-01", Clock.date(0));
        assertEquals("2026-11-20", Clock.date(1795132800000L + 3600000L));
        assertEquals(DailySeeds.hashed("2026-11-20"), DailySeeds.hashed("2026-11-20"));
        assertTrue(DailySeeds.hashed("2026-11-20") != DailySeeds.hashed("2026-11-21"));
        String who = Api.player(accounts, "dailyplayer");
        long seed = Api.number(Api.json(mvc, Api.as(get("/api/daily"), who)), "seed");
        assertEquals(seed, Api.number(Api.json(mvc, Api.as(get("/api/daily"), who)), "seed"));
        assertEquals(seed, Api.number(Api.json(mvc, Api.as(Api.body(post("/api/runs/start"),
                "{\"mode\":\"DAILY\"}"), who)), "seed"));
        Map picked = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/daily"), "{\"seed\":4242}"),
                "admin@papercut.example"));
        assertEquals(4242L, Api.number(picked, "seed"));
        assertEquals(4242L, Api.number(Api.json(mvc, Api.as(Api.body(post("/api/runs/start"),
                "{\"mode\":\"DAILY\"}"), who)), "seed"));
        Api.submit(mvc, who, "DAILY", 9000000, 3600);
        Map daily = Api.json(mvc, Api.as(get("/api/daily"), who));
        assertEquals(9000000L, Api.number(daily, "myBest"));
        assertEquals(9000000L, Api.number(daily, "topScore"));
        assertEquals("dailyplayer", daily.get("topName"));
        assertTrue(Api.number(daily, "players") > 1);
    }

    private static int count(List board, String username) {
        int n = 0;
        for (Object row : board) {
            if (username.equals(((Map) row).get("username"))) {
                n++;
            }
        }
        return n;
    }
}
