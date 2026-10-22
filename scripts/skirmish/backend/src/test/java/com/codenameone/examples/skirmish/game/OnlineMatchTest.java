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
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Whole matches, played on the server's own rooms. The seats have no socket:
/// what goes over the wire is [GameSocket]'s, and these are about what the
/// server decides.
@BackendTest
class OnlineMatchTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Rooms rooms;

    // tag::skirmish-match-test[]
    @Test
    void aChallengeIsPlayedRecordedAndReplaysToTheSameResult() throws Exception {
        String host = Api.player(accounts, "host");
        String guest = Api.player(accounts, "guest");
        Map challenge = Api.json(mvc, Api.as(post("/api/challenges"), host));
        String code = (String) challenge.get("code");
        assertTrue(((String) challenge.get("url")).endsWith("?challenge=" + code));

        rooms.challenge(profile(host), null, code);
        assertEquals("", rooms.matchOf(host));
        rooms.challenge(profile(guest), null, code);
        String id = rooms.matchOf(host);
        assertFalse(id.isEmpty());
        assertEquals(id, rooms.matchOf(guest));

        // A play is stamped a few ticks ahead, then the match runs out.
        rooms.play(host, rooms.cheapestInHand(host), 3500, 24000);
        rooms.fastForward(id);
        rooms.tick(System.currentTimeMillis());
        assertEquals("", rooms.matchOf(host));

        List mine = Api.list(mvc, Api.as(get("/api/matches"), host));
        assertEquals(1, mine.size());
        assertEquals(id, ((Map) mine.get(0)).get("id"));

        // The record is the seed, the decks and the plays. Run again here,
        // with the same rules, it ends exactly as it did on the server.
        Map stored = Api.json(mvc, Api.as(get("/api/matches/" + id), guest));
        Replay replay = Replay.decode((String) stored.get("replay"));
        assertEquals(1, replay.commands().size());
        Match again = replay.start();
        again.runToEnd(replay.commands());
        assertEquals(Api.number(stored, "result"), again.result());
        assertEquals(Api.number(stored, "crowns0"), again.player(0).crowns());
        assertEquals(Api.number(stored, "crowns1"), again.player(1).crowns());
    }
    // end::skirmish-match-test[]

    @Test
    void aPlayerAloneGetsTheBotAndNoTrophiesChange() throws Exception {
        String who = Api.player(accounts, "lonely");
        int before = profile(who).getTrophies();
        rooms.queue(profile(who), null);
        String id = rooms.matchOf(who);
        assertFalse(id.isEmpty(), "the test profile pairs with the bot at once");
        rooms.fastForward(id);
        rooms.tick(System.currentTimeMillis());
        assertEquals(before, profile(who).getTrophies());
        Map record = (Map) Api.list(mvc, Api.as(get("/api/matches"), who)).get(0);
        assertEquals(Rooms.BOT_NAME, record.get("opponentName"));
        // The bot is not someone to report.
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/reports"),
                "{\"matchId\":\"" + id + "\",\"reason\":\"too good\"}"), who)));
    }

    @Test
    void leavingThreeMatchesInARowRaisesAFlag() throws Exception {
        String who = Api.player(accounts, "quitter");
        for (int i = 0; i < 3; i++) {
            rooms.queue(profile(who), null);
            String id = rooms.matchOf(who);
            rooms.leave(who);
            rooms.fastForward(id);
            rooms.tick(System.currentTimeMillis());
        }
        assertTrue(profile(who).isFlagged());
    }

    @Test
    void aPlayTheRulesRefuseIsNotStamped() throws Exception {
        String who = Api.player(accounts, "cheater");
        rooms.queue(profile(who), null);
        String id = rooms.matchOf(who);
        // Across the river, and a card not in the deck: seat 0 plays from the
        // bottom of the arena.
        rooms.play(who, "ogre", 3500, 8000);
        rooms.play(who, "drake", 3500, 24000);
        rooms.fastForward(id);
        rooms.tick(System.currentTimeMillis());
        Map stored = Api.json(mvc, Api.as(get("/api/matches/" + id), who));
        Replay replay = Replay.decode((String) stored.get("replay"));
        for (com.codenameone.examples.skirmish.rules.Command c : replay.commands()) {
            assertEquals(1, c.player, "only the bot's plays were accepted");
        }
    }

    private Profile profile(String username) {
        return accounts.profile(username);
    }
}
