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
package com.codenameone.examples.dash.game;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.Api;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.Input;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Whole races, run on the server's own rooms. The seats have no socket:
/// what goes over the wire is [GameSocket]'s, and these are about what the
/// server decides.
@BackendTest
class OnlineRaceTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Rooms rooms;

    // tag::dash-race-test[]
    @Test
    void aPartyRacesIsRecordedAndReplaysToTheSameScores() throws Exception {
        String host = Api.player(accounts, "host");
        String guest = Api.player(accounts, "guest");
        rooms.host(profile(host), null, Content.CAUSEWAY);
        String code = rooms.partyCode(host);
        assertFalse(code.isEmpty());
        rooms.join(profile(guest), null, code);
        assertEquals("", rooms.raceOf(host));
        // Only the host starts the race.
        rooms.go(guest, null);
        assertEquals("", rooms.raceOf(host));
        rooms.go(host, null);
        String id = rooms.raceOf(host);
        assertFalse(id.isEmpty());
        assertEquals(id, rooms.raceOf(guest));

        // Past the countdown, an input is stamped a few ticks ahead.
        rooms.tick(System.currentTimeMillis() + GameProtocol.COUNTDOWN_MILLIS + 100);
        rooms.input(host, Input.JUMP);
        rooms.fastForward(id);
        assertEquals("", rooms.raceOf(host));

        List mine = Api.list(mvc, Api.as(get("/api/races"), host));
        assertEquals(1, mine.size());
        assertEquals(id, ((Map) mine.get(0)).get("id"));
        assertEquals(2L, Api.number((Map) mine.get(0), "runners"));

        // The record is the level, the seed and every input. Run again here,
        // with the same rules, it ends exactly as it did on the server.
        Map stored = Api.json(mvc, Api.as(get("/api/races/" + id), guest));
        Replay replay = Replay.decode((String) stored.get("replay"));
        assertTrue(replay.eventCount() >= 1);
        Run again = replay.play(Content.causeway(), Content.catalog());
        List scores = (List) stored.get("scores");
        for (int i = 0; i < 2; i++) {
            assertEquals(((Number) scores.get(i)).intValue(), again.runner(i).score());
        }
    }
    // end::dash-race-test[]

    @Test
    void aQuickRaceFillsWithBotsAndNeverCostsTrophies() throws Exception {
        String who = Api.player(accounts, "lonely");
        int before = profile(who).getTrophies();
        rooms.quick(profile(who), null, Content.TUTORIAL);
        String id = rooms.raceOf(who);
        assertFalse(id.isEmpty(), "the test profile gets bots at once");
        rooms.fastForward(id);
        assertTrue(profile(who).getTrophies() >= before);
        Map record = (Map) Api.list(mvc, Api.as(get("/api/races"), who)).get(0);
        assertEquals((long) Rooms.QUICK_RUNNERS, Api.number(record, "runners"));
        // A bot is not someone to report.
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/reports"),
                "{\"raceId\":\"" + id + "\",\"player\":\"1\",\"reason\":\"too good\"}"), who)));
    }

    @Test
    void anInputFloodIsCut() throws Exception {
        String who = Api.player(accounts, "masher");
        rooms.quick(profile(who), null, Content.CAUSEWAY);
        String id = rooms.raceOf(who);
        rooms.tick(System.currentTimeMillis() + GameProtocol.COUNTDOWN_MILLIS + 100);
        for (int i = 0; i < 100; i++) {
            rooms.input(who, Input.LEFT);
        }
        rooms.fastForward(id);
        Map stored = Api.json(mvc, Api.as(get("/api/races/" + id), who));
        Replay replay = Replay.decode((String) stored.get("replay"));
        int mine = 0;
        for (int i = 0; i < replay.eventCount(); i++) {
            if (replay.event(i)[1] == 0) {
                mine++;
            }
        }
        assertTrue(mine <= RaceRoom.FLOOD_LIMIT, "accepted " + mine);
    }

    @Test
    void trophiesSumToZeroAndAloneNeverGoNegative() {
        int sum = 0;
        for (int p = 0; p < 4; p++) {
            sum += Results.trophies(p, 4, 4);
            assertTrue(Results.trophies(p, 4, 1) >= 0);
        }
        assertEquals(0, sum);
        assertEquals(30, Results.trophies(0, 4, 4));
    }

    private Profile profile(String username) {
        return accounts.profile(username);
    }
}
