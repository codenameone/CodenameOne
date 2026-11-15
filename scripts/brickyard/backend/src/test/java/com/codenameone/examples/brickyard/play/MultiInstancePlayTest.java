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
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.Base64;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.Api;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.PlaceDto;
import com.codenameone.examples.brickyard.domain.AnalyticsEvent;
import com.codenameone.examples.brickyard.domain.PlayServer;
import com.codenameone.examples.brickyard.repository.AnalyticsEventRepository;
import com.codenameone.examples.brickyard.repository.PlayServerRepository;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Two game-server instances sharing one database: the directory, tickets,
/// heartbeats and reaping, live counts, and what a room reports when a
/// player comes and goes (presence, VISIT and SESSION).
@BackendTest
class MultiInstancePlayTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private PlayServers servers;
    @Autowired
    private PlayServerRepository rows;
    @Autowired
    private PlayTickets tickets;
    @Autowired
    private LiveServers live;
    @Autowired
    private AnalyticsEventRepository events;

    /// A place source that counts how often the place was read.
    private static final class Counting implements PlayServers.PlaceSource {
        final AtomicInteger reads = new AtomicInteger();

        @Override
        public PlaceDto place() throws java.io.IOException {
            reads.incrementAndGet();
            return new PlaceDto(1, Base64.encode(Api.sample()));
        }
    }

    @Test
    void aSecondInstanceSendsPlayersToTheRoomTheFirstRuns() throws Exception {
        PlayServers other = new PlayServers(rows, null, "instance-b", "ws://b.example/ws/play");
        try {
            int roomsBefore = live.rooms();
            long ccuBefore = live.ccu();
            Counting place = new Counting();
            PlayServers.Placement first = other.seat("mi-a", "exp-mi", 4, "", place);
            assertNotNull(first.room);
            assertEquals(1, place.reads.get());

            // This instance has no room of its own: it sends the player to B's,
            // with B's socket URL, and never reads the place.
            Counting unused = new Counting();
            PlayServers.Placement second = servers.seat("mi-b", "exp-mi", 4, "", unused);
            assertEquals(first.serverId, second.serverId, "matchmaking crosses instances");
            assertEquals("ws://b.example/ws/play", second.url);
            assertNull(second.room);
            assertEquals(0, unused.reads.get(), "seating into a running room reads no place");

            // A friend's server is found by id on the other instance too.
            PlayServers.Placement friend = servers.seat("mi-c", "exp-mi", 4, first.serverId, unused);
            assertEquals("ws://b.example/ws/play", friend.url);

            // Seating into a room this instance knows locally reads no place either.
            other.seat("mi-d", "exp-mi", 4, first.serverId, unused);
            assertEquals(0, unused.reads.get());

            assertEquals(roomsBefore + 1, live.rooms());
            assertTrue(live.ccu() > ccuBefore);

            // A ticket from either instance is redeemable once, anywhere.
            String t = tickets.issue("mi-b", first.serverId);
            PlayTickets.Seat seat = tickets.redeem(t);
            assertNotNull(seat);
            assertEquals(first.serverId, seat.serverId);
            assertNull(tickets.redeem(t), "single use");

            // B stops beating: its room is reaped and no longer counted.
            PlayServer row = rows.findById(first.serverId);
            row.setHeartbeat(Clock.now() - PlayServers.STALE_MILLIS - 1000);
            rows.save(row);
            servers.heartbeat();
            assertNull(rows.findById(first.serverId));
            assertEquals(roomsBefore, live.rooms());
        } finally {
            other.closeAll();
        }
    }

    @Test
    void leavingARoomClearsPresenceAndRecordsTheSession() throws Exception {
        String owner = Api.player(accounts, "mipowner");
        String a = Api.player(accounts, "mipa");
        String b = Api.player(accounts, "mipb");
        Api.befriend(mvc, a, b);
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        Map j = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/join"), "{}"), a));
        String server = (String) j.get("serverId");
        assertEquals("", j.get("url"), "a single instance leaves the socket URL to the app");
        assertEquals("IN_GAME", Api.json(mvc, Api.as(get("/api/me/friends/" + a + "/join"), b)).get("status"));

        PlayRoom room = servers.room(server);
        PlayRoom.Link link = new PlayRoom.Link() {
            @Override
            public void send(byte[] message) {
            }
        };
        room.connect(link, 7, a, a);
        long until = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < until && count(id, "VISIT") == 0) {
            Thread.sleep(20);
        }
        assertEquals(1, count(id, "VISIT"));
        room.disconnect(link);
        until = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < until && count(id, "SESSION") == 0) {
            Thread.sleep(20);
        }
        assertEquals(1, count(id, "SESSION"));
        assertEquals(409, Api.statusOf(mvc, Api.as(get("/api/me/friends/" + a + "/join"), b)),
                "a player who left is no longer joinable");
    }

    private int count(String experienceId, String kind) {
        int n = 0;
        for (AnalyticsEvent e : events.of(experienceId, 0)) {
            if (kind.equals(e.getKind())) {
                n++;
            }
        }
        return n;
    }
}
