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
package com.codenameone.examples.dash;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.rules.Content;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Leaderboards and crews. A daily score counts only on the day's seed, and a
/// crew's board lists only its members.
@BackendTest
class ScoresTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private static String result(String replay) {
        Map<String, Object> m = new HashMap<>();
        m.put("replay", replay);
        return Json.write(m);
    }

    @Test
    void theDailyBoardTakesOnlyTheDaysSeed() throws Exception {
        String who = Api.player(accounts, "daily-runner");
        Map daily = Api.json(mvc, Api.as(get("/api/daily"), who));
        assertEquals(Content.ENDLESS, daily.get("level"));
        long seed = Long.parseLong((String) daily.get("seed"));
        // A clumsy bot, so the endless level ends soon.
        String today = Runs.bot(Content.endless(), Content.catalog(), seed, 10).encode();
        String other = Runs.bot(Content.endless(), Content.catalog(), seed + 1, 10).encode();
        Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(other)), who));
        // The development server's seeded field is on the board too, so the
        // test looks for its own runner rather than counting rows.
        assertTrue(row(Api.list(mvc, Api.as(get("/api/scores?level=endless&board=day&limit=50"), who)),
                "daily-runner") == null);
        Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(today)), who));
        Map mine = row(Api.list(mvc, Api.as(get("/api/scores?level=endless&board=day&limit=50"), who)),
                "daily-runner");
        assertTrue(mine != null);
        // Anyone can watch the run behind a score.
        String id = (String) mine.get("replayId");
        String watcher = Api.player(accounts, "daily-watcher");
        assertEquals(today, Api.json(mvc, Api.as(get("/api/races/" + id), watcher)).get("replay"));
        // The ghost is the closest run above the watcher's (none): whichever
        // of the day's runners that is, it was run on the day's seed.
        String ghost = (String) Api.json(mvc, Api.as(get("/api/ghost?level=endless&board=day"), watcher)).get("replay");
        assertTrue(ghost.contains("|" + seed + "|"), ghost);
    }

    @Test
    void theAllTimeBoardRefusesASeedTheAppWasNotHanded() throws Exception {
        String who = Api.player(accounts, "seed-picker");
        // Any seed but the ones the server hands out: searched for offline.
        long picked = 987654321L;
        String run = Runs.bot(Content.endless(), Content.catalog(), picked, 10).encode();
        Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(run)), who));
        for (Object row : Api.list(mvc, Api.as(get("/api/scores?level=endless&board=all&limit=50"), who))) {
            assertTrue(!"seed-picker".equals(((Map) row).get("displayName")), "picked seed reached the board");
        }
        String fixed = Runs.bot(Content.endless(), Content.catalog(), 1L, 10).encode();
        Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(fixed)), who));
        boolean listed = false;
        for (Object row : Api.list(mvc, Api.as(get("/api/scores?level=endless&board=all&limit=50"), who))) {
            listed |= "seed-picker".equals(((Map) row).get("displayName"));
        }
        assertTrue(listed, "the fixed seed is fair");
    }

    @Test
    void theWeeksSeedTheAppDerivesCountsOnTheWeekAndAllTime() throws Exception {
        String who = Api.player(accounts, "week-runner");
        // The way LevelsForm.week derives it: days since the epoch, over 7.
        int week = (int) (System.currentTimeMillis() / 86400000L) / 7;
        long seed = Content.weeklySeed(week, Content.ENDLESS);
        String run = Runs.bot(Content.endless(), Content.catalog(), seed, 10).encode();
        Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(run)), who));
        for (String board : new String[] {"week", "all"}) {
            boolean listed = false;
            for (Object row : Api.list(mvc, Api.as(get("/api/scores?level=endless&board=" + board + "&limit=50"), who))) {
                listed |= "week-runner".equals(((Map) row).get("displayName"));
            }
            assertTrue(listed, "the week's seed missed the " + board + " board");
        }
    }

    @Test
    void aCrewBoardListsItsMembersOnly() throws Exception {
        String owner = Api.player(accounts, "crew-owner");
        String mate = Api.player(accounts, "crew-mate");
        String stranger = Api.player(accounts, "crew-stranger");
        Map crew = Api.json(mvc, Api.as(Api.body(post("/api/crews"), "{\"name\":\"Night Owls\"}"), owner));
        String code = (String) crew.get("code");
        Map joined = Api.json(mvc, Api.as(post("/api/crews/" + code + "/join"), mate));
        assertEquals(2, ((List) joined.get("members")).size());
        for (String who : new String[] {owner, mate, stranger}) {
            String run = Runs.bot(Content.tutorial(), Content.catalog(), 1L, 80).encode();
            Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), result(run)), who));
        }
        List board = Api.list(mvc, Api.as(get("/api/scores?level=first-steps&board=crew%3A" + code + "&limit=50"), owner));
        assertEquals(2, board.size());
        List all = Api.list(mvc, Api.as(get("/api/scores?level=first-steps&board=all&limit=50"), owner));
        assertTrue(all.size() >= 3);
        assertEquals(1, Api.list(mvc, Api.as(get("/api/crews"), mate)).size());
        Api.json(mvc, Api.as(post("/api/crews/" + code + "/leave"), mate));
        assertEquals(0, Api.list(mvc, Api.as(get("/api/crews"), mate)).size());
        assertEquals(404, Api.statusOf(mvc, Api.as(post("/api/crews/NOPE/join"), stranger)));
    }

    private static Map row(List board, String name) {
        for (Object o : board) {
            if (name.equals(((Map) o).get("displayName"))) {
                return (Map) o;
            }
        }
        return null;
    }
}
