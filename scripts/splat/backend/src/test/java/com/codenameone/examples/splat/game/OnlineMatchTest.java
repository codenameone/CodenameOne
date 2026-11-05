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
package com.codenameone.examples.splat.game;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.splat.Api;
import com.codenameone.examples.splat.account.Accounts;
import com.codenameone.examples.splat.api.GameProtocol;
import com.codenameone.examples.splat.api.PushDeviceDto;
import com.codenameone.examples.splat.domain.Profile;
import com.codenameone.examples.splat.push.PushDevices;
import com.codenameone.examples.splat.push.PushSender;
import com.codenameone.examples.splat.push.RecordingPushSender;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Replay;
import com.codenameone.examples.splat.social.Crews;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Whole matches, run on the server's own rooms. The seats have no socket:
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
    @Autowired
    private Crews crews;
    @Autowired
    private PushDevices devices;
    @Autowired
    private PushSender sender;

    /// Opening a party for a crew pushes everyone else in it, and only them.
    @Test
    void aCrewPartyCallsTheRestOfTheCrew() throws Exception {
        String host = Api.player(accounts, "crewhost");
        String mate = Api.player(accounts, "crewmate");
        String stranger = Api.player(accounts, "stranger");
        devices.register(mate, new PushDeviceDto("fcm", "tok-mate", "android"));
        devices.register(stranger, new PushDeviceDto("fcm", "tok-stranger", "android"));
        String code = crews.create(host, "Night Shift").code();
        crews.join(mate, code);
        RecordingPushSender pushes = (RecordingPushSender) sender;
        rooms.host(profile(host), null, Arenas.DEFAULT, code);
        assertEquals(1, pushes.sentTo("tok-mate").size(), "the crewmate hears about the party");
        assertTrue(pushes.sentTo("tok-stranger").isEmpty(), "nobody outside the crew does");
        // A crew code alone does not let a non-member call that crew.
        rooms.host(profile(stranger), null, Arenas.DEFAULT, code);
        assertEquals(1, pushes.sentTo("tok-mate").size());
    }

    // tag::splat-match-test[]
    @Test
    void aPartyMatchIsRecordedAndReplaysToTheSamePaints() throws Exception {
        String host = Api.player(accounts, "host");
        String guest = Api.player(accounts, "guest");
        rooms.host(profile(host), null, "cul-de-sac:SNACK_BAR", null);
        String code = rooms.partyCode(host);
        assertFalse(code.isEmpty());
        rooms.join(profile(guest), null, code);
        assertEquals("", rooms.raceOf(host));
        // Only the host starts the match.
        rooms.go(guest, null);
        assertEquals("", rooms.raceOf(host));
        rooms.go(host, null);
        String id = rooms.raceOf(host);
        assertFalse(id.isEmpty());
        assertEquals(id, rooms.raceOf(guest));

        // Past the countdown, the host walks forward with the trigger held.
        rooms.tick(System.currentTimeMillis() + GameProtocol.COUNTDOWN_MILLIS + 100);
        rooms.input(host, 1, new Input(0, 100, 0, 0, Input.FIRE, -1));
        rooms.fastForward(id);
        assertEquals("", rooms.raceOf(host));

        List mine = Api.list(mvc, Api.as(get("/api/races"), host));
        assertEquals(1, mine.size());
        assertEquals(id, ((Map) mine.get(0)).get("id"));

        // The record is the field, the seed and every input. Played again
        // here, with the same rules, every player paints exactly as many.
        Map stored = Api.json(mvc, Api.as(get("/api/races/" + id), guest));
        Replay replay = Replay.decode((String) stored.get("replay"));
        Match again = replay.play();
        assertTrue(again.over());
        List scores = (List) stored.get("scores");
        for (int i = 0; i < scores.size(); i++) {
            assertEquals(((Number) scores.get(i)).intValue(), again.player(i).paints());
        }
    }
    // end::splat-match-test[]

    /// A player still seated in a match who joins a party has left that
    /// match; the join used to be dropped without a word until it ended.
    @Test
    void joiningAPartyLeavesAMatchStillRunning() throws Exception {
        String who = Api.player(accounts, "restless");
        String host = Api.player(accounts, "partyhost");
        rooms.quick(profile(who), null, Arenas.DEFAULT);
        String id = rooms.raceOf(who);
        assertFalse(id.isEmpty());
        rooms.host(profile(host), null, Arenas.DEFAULT, null);
        rooms.join(profile(who), null, rooms.partyCode(host));
        assertEquals("", rooms.raceOf(who), "out of the old match");
        assertEquals(rooms.partyCode(host), rooms.partyCode(who), "and in the party");
        rooms.fastForward(id);
    }

    @Test
    void aQuickMatchFillsWithBotsAndNeverCostsTrophies() throws Exception {
        String who = Api.player(accounts, "lonely");
        int before = profile(who).getTrophies();
        rooms.quick(profile(who), null, Arenas.DEFAULT);
        String id = rooms.raceOf(who);
        assertFalse(id.isEmpty(), "the test profile gets bots at once");
        rooms.fastForward(id);
        assertTrue(profile(who).getTrophies() >= before);
        Map record = (Map) Api.list(mvc, Api.as(get("/api/races"), who)).get(0);
        assertEquals((long) Rooms.QUICK_PLAYERS, Api.number(record, "runners"));
        // A bot is not someone to report.
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/reports"),
                "{\"raceId\":\"" + id + "\",\"player\":\"1\",\"reason\":\"too good\"}"), who)));
    }

    @Test
    void anInputFloodIsCutAndOldSequencesAreDropped() {
        MatchRoom room = new MatchRoom("r", Arenas.DEFAULT, "lot7", Arenas.mode(Arenas.DEFAULT), 7L,
                new Seat[] {new Seat("a@x", "A", "semi", 0, 0, null), new Seat("", "Bot", "pump", 0, 1, null)}, 0L);
        int accepted = 0;
        for (int i = 1; i <= 100; i++) {
            if (room.input(0, i, Input.IDLE)) {
                accepted++;
            }
        }
        assertTrue(accepted <= MatchRoom.FLOOD_LIMIT, "accepted " + accepted);
        assertFalse(room.input(0, 3, Input.IDLE), "a replayed sequence is dropped");
        assertFalse(room.input(-1, 200, Input.IDLE));
    }

    @Test
    void anAimbotFlickIsCounted() {
        MatchRoom room = new MatchRoom("r", Arenas.DEFAULT, "lot7", Arenas.mode(Arenas.DEFAULT), 7L,
                new Seat[] {new Seat("a@x", "A", "semi", 0, 0, null), new Seat("", "Bot", "pump", 0, 1, null)}, 0L);
        room.input(0, 1, new Input(0, 0, 0, 0, 0, -1));
        room.step();
        room.input(0, 2, new Input(0, 0, 2048, 0, 0, -1));
        assertEquals(1, room.seats[0].snaps);
    }

    @Test
    void trophiesBalanceAndAloneNeverGoNegative() {
        assertEquals(0, Results.trophies(0, true, 8));
        assertEquals(Results.WIN_TROPHIES, Results.trophies(0, false, 8));
        assertEquals(-Results.LOSS_TROPHIES, Results.trophies(1, false, 8));
        assertTrue(Results.trophies(1, false, 1) >= 0);
    }

    private Profile profile(String username) {
        return accounts.profile(username);
    }
}
