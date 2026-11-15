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
import com.codenameone.examples.brickyard.leaderboard.Leaderboards;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Server-authoritative leaderboards, their anti-cheat bound, and badges.
@BackendTest
class LeaderboardTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private Accounts accounts;

    @Autowired
    private Leaderboards leaderboards;

    private void score(String id, String user, long delta) throws Exception {
        Api.json(mvc, Api.asEngine(Api.body(post("/api/leaderboards/" + id + "/scores"),
                "{\"stat\":\"Wins\",\"userId\":\"" + user + "\",\"delta\":" + delta + "}")));
    }

    @Test
    void theBoardRanksTotalsForEachPeriodAndForFriends() throws Exception {
        String owner = Api.player(accounts, "boardowner");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        String a = Api.player(accounts, "racer");
        String b = Api.player(accounts, "runner");
        String c = Api.player(accounts, "jumper");
        score(id, a, 3);
        score(id, b, 5);
        score(id, a, 4);
        score(id, c, 1);
        List all = Api.list(mvc, Api.as(get("/api/leaderboards/" + id + "/Wins?period=all&limit=2"), a));
        assertEquals(2, all.size());
        assertEquals(a, ((Map) all.get(0)).get("userId"));
        assertEquals(7L, Api.number((Map) all.get(0), "value"));
        assertEquals(2L, Api.number((Map) all.get(1), "rank"));
        assertEquals(3, Api.list(mvc, Api.as(get("/api/leaderboards/" + id + "/Wins?period=daily&limit=10"),
                a)).size());
        assertEquals(3, Api.list(mvc, Api.as(get("/api/leaderboards/" + id + "/Wins?period=weekly&limit=10"),
                a)).size());
        Api.befriend(mvc, a, c);
        List friends = Api.list(mvc, Api.as(get("/api/leaderboards/" + id + "/Wins/friends?period=all"), a));
        assertEquals(2, friends.size());
        assertTrue(!friends.toString().contains(b));
    }

    @Test
    void aPlayerClientCannotWriteAScore() throws Exception {
        String owner = Api.player(accounts, "honest");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/leaderboards/" + id + "/scores"),
                "{\"stat\":\"Wins\",\"userId\":\"" + owner + "\",\"delta\":1}"), owner)));
    }

    @Test
    void aWriteOutOfBoundsIsRejectedAndFlagged() throws Exception {
        String owner = Api.player(accounts, "strict");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        String cheat = Api.player(accounts, "cheater");
        Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/bounds"),
                "{\"stat\":\"Wins\",\"maxPerMinute\":100}"), owner));
        score(id, cheat, 60);
        assertEquals(422, Api.statusOf(mvc, Api.asEngine(Api.body(post("/api/leaderboards/" + id + "/scores"),
                "{\"stat\":\"Wins\",\"userId\":\"" + cheat + "\",\"delta\":50}"))));
        assertEquals(1, leaderboards.flagsOf(id).size());
        assertEquals(cheat, leaderboards.flagsOf(id).get(0).getUserId());
        List board = Api.list(mvc, Api.as(get("/api/leaderboards/" + id + "/Wins?period=all&limit=10"), cheat));
        assertEquals(60L, Api.number((Map) board.get(0), "value"));
    }

    @Test
    void anEngineAwardsABadgeOnce() throws Exception {
        String owner = Api.player(accounts, "badgemaker");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Map badge = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/badges"),
                "{\"name\":\"First Steps\",\"description\":\"Joined\"}"), owner));
        String badgeId = (String) badge.get("id");
        String player = Api.player(accounts, "achiever");
        String award = "{\"userId\":\"" + player + "\"}";
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/badges/" + badgeId + "/award"), award),
                player)));
        Api.json(mvc, Api.asEngine(Api.body(post("/api/badges/" + badgeId + "/award"), award)));
        Api.json(mvc, Api.asEngine(Api.body(post("/api/badges/" + badgeId + "/award"), award)));
        List mine = Api.list(mvc, Api.as(get("/api/me/badges"), player));
        assertEquals(1, mine.size());
        assertEquals(badgeId, ((Map) mine.get(0)).get("badgeId"));
    }
}
