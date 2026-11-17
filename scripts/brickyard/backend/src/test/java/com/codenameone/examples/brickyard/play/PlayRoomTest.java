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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.rbx.ReplicaClient;
import com.codenameone.examples.brickyard.rbx.ReplicaServer;
import com.codenameone.examples.brickyard.rbx.Scheduler;
import com.codenameone.examples.brickyard.Api;
import com.codenameone.examples.brickyard.account.Accounts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Matchmaking, seats and a room's replication, driven through in-memory
/// links in place of sockets: what goes over the wire is [PlaySocket]'s,
/// and these are about what the server decides.
@BackendTest
class PlayRoomTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private PlayServers servers;

    /// A link that collects what is sent; read on the test thread.
    private static final class Queue implements PlayRoom.Link {
        final ConcurrentLinkedQueue<byte[]> pending = new ConcurrentLinkedQueue<byte[]>();

        @Override
        public void send(byte[] message) {
            pending.add(message);
        }
    }

    /// One player's side: a CLIENT engine fed from the room.
    private static final class Player {
        final Queue down = new Queue();
        final Engine engine = new Engine(Engine.CLIENT);
        final StringBuilder out = new StringBuilder();
        final ReplicaClient client;

        Player(final PlayRoom room) {
            engine.getScheduler().setOutput(new Scheduler.Output() {
                @Override
                public void message(String type, String text, Object owner) {
                    synchronized (out) {
                        out.append(text).append('\n');
                    }
                }
            });
            client = new ReplicaClient(engine, new ReplicaServer.Link() {
                @Override
                public void send(byte[] message) {
                    room.receive(down, message);
                }
            });
        }

        void frame() {
            byte[] m;
            while ((m = down.pending.poll()) != null) {
                client.receive(m);
            }
            engine.step(1 / 60.0);
            client.tick();
        }

        boolean sees(String name) {
            return engine.service("Players").findFirstChild(name) != null;
        }
    }

    @Test
    void playersShareARoomUntilItIsFullAndFriendsJoinIt() throws Exception {
        String owner = Api.player(accounts, "mmowner");
        String a = Api.player(accounts, "mma");
        String b = Api.player(accounts, "mmb");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);

        Map ja = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/join"), "{}"), a));
        Map jb = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/join"), "{}"), b));
        String server = (String) ja.get("serverId");
        assertEquals(server, jb.get("serverId"), "matchmaking fills the open room");
        assertNotEquals(ja.get("ticket"), jb.get("ticket"));

        // Presence carries the real server, so join-a-friend lands there.
        Api.befriend(mvc, a, b);
        Map target = Api.json(mvc, Api.as(get("/api/me/friends/" + b + "/join"), a));
        assertEquals(server, target.get("serverId"));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/join"),
                "{\"serverId\":\"srv-gone\"}"), a)));
        Map priv = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/join"),
                "{\"serverId\":\"private-mma\"}"), a));
        assertEquals("private-mma", priv.get("serverId"));

        // Both seats connect to the running room; each sees the other.
        PlayRoom room = servers.room(server);
        assertNotNull(room);
        Player pa = new Player(room);
        Player pb = new Player(room);
        room.connect(pa.down, 11, a, a);
        room.connect(pb.down, 12, b, b);
        long until = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < until && !(pa.sees(b) && pb.sees(a))) {
            pa.frame();
            pb.frame();
            Thread.sleep(16);
        }
        assertTrue(pa.client.isJoined() && pb.client.isJoined());
        assertTrue(pa.sees(b) && pb.sees(a), "both players replicate to both clients");
        room.disconnect(pa.down);
        room.disconnect(pb.down);
    }

    @Test
    void aLinkThatFloodsItsInboxIsDroppedAndTheRoomLives() {
        PlayRoom room = new PlayRoom("t2", "x", 4, true, place());
        Queue flood = new Queue();
        Queue calm = new Queue();
        room.connect(flood, 1, "Alice", "Alice");
        room.connect(calm, 2, "Bob", "Bob");
        room.frame(System.currentTimeMillis());
        for (int i = 0; i < PlayRoom.MAX_QUEUED_FRAMES * 4; i++) {
            room.receive(flood, new byte[] {0});
        }
        room.frame(System.currentTimeMillis());
        assertEquals(1, room.getServer().getPlayerCount(), "the flooding link lost its seat, the other kept it");
    }

    @Test
    void aRemoteEventRoundTripsThroughTheRoom() {
        PlayRoom room = new PlayRoom("t1", "x", 4, true, place());
        List<Player> players = new ArrayList<Player>();
        players.add(new Player(room));
        players.add(new Player(room));
        room.connect(players.get(0).down, 1, "Alice", "Alice");
        room.connect(players.get(1).down, 2, "Bob", "Bob");
        for (int f = 0; f < 60; f++) {
            room.frame(System.currentTimeMillis());
            for (Player p : players) {
                p.frame();
            }
        }
        assertTrue(players.get(0).out.toString().contains("pong 42"), players.get(0).out.toString());
        assertNotNull(players.get(1).engine.getWorkspace().findFirstChild("FromAlice"),
                "what the server made for Alice reaches Bob");
        assertEquals(2, room.getServer().getPlayerCount());
    }

    @Test
    void leaderstatsReachTheLeaderboardsAsDeltas() {
        Engine s = new Engine(Engine.SERVER);
        Instance script = s.create("Script");
        script.setName("Stats");
        script.put("Source", "game.Players.PlayerAdded:Connect(function(p)\n"
                + "  local ls = Instance.new('Folder'); ls.Name = 'leaderstats'; ls.Parent = p\n"
                + "  local w = Instance.new('IntValue'); w.Name = 'Wins'; w.Value = 5; w.Parent = ls\n"
                + "  local n = Instance.new('StringValue'); n.Name = 'Rank'; n.Value = 'Gold'; n.Parent = ls\n"
                + "  task.delay(0.5, function() w.Value = 8 end)\n"
                + "end)\n");
        script.setParent(s.service("ServerScriptService"));
        PlayRoom room = new PlayRoom("t3", "x", 4, true, s);
        final List<String> scores = new ArrayList<String>();
        room.listen(new PlayRoom.Listener() {
            @Override
            public void closed(PlayRoom r) {
            }

            @Override
            public void joined(PlayRoom r, String username) {
            }

            @Override
            public void left(PlayRoom r, String username, long millis) {
            }

            @Override
            public void scored(PlayRoom r, String username, String stat, long delta) {
                scores.add(username + " " + stat + " " + delta);
            }
        });
        Player alice = new Player(room);
        room.connect(alice.down, 1, "alice@x", "Alice");
        long now = 1000000;
        for (int f = 0; f < 120; f++) {
            now += 1000 / 60;
            room.frame(now);
            alice.frame();
            if (f == 10) {
                // A late first frame takes the baseline: 5 is not a score.
                now += PlayRoom.SCORE_MILLIS;
            }
        }
        room.frame(now + PlayRoom.SCORE_MILLIS);
        assertEquals("[alice@x Wins 3]", scores.toString(),
                "only the rise is reported, and a string stat never is");
    }

    private static Engine place() {
        Engine s = new Engine(Engine.SERVER);
        Instance ping = s.create("RemoteEvent");
        ping.setName("Ping");
        ping.setParent(s.service("ReplicatedStorage"));
        Instance script = s.create("Script");
        script.setName("Server");
        script.put("Source", "local rs = game:GetService('ReplicatedStorage')\n"
                + "rs.Ping.OnServerEvent:Connect(function(p, n)\n"
                + "  local part = Instance.new('Part')\n"
                + "  part.Name = 'From' .. p.Name\n"
                + "  part.Anchored = true\n"
                + "  part.Parent = workspace\n"
                + "  rs.Ping:FireClient(p, n * 2)\n"
                + "end)\n");
        script.setParent(s.service("ServerScriptService"));
        Instance client = s.create("LocalScript");
        client.setName("Client");
        client.put("Source", "local rs = game:GetService('ReplicatedStorage')\n"
                + "rs.Ping.OnClientEvent:Connect(function(v) print('pong', v) end)\n"
                + "if game:GetService('Players').LocalPlayer.Name == 'Alice' then rs.Ping:FireServer(21) end\n");
        client.setParent(s.service("StarterPlayer").findFirstChild("StarterPlayerScripts"));
        return s;
    }
}
