/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.rbx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// One server and two clients joined by in-memory links, stepped in lock
/// step: the snapshot, deltas, remote events, remote functions and
/// character ownership.
class MultiplayerTest {
    private static final class Queue implements ReplicaServer.Link {
        final List<byte[]> pending = new ArrayList<byte[]>();

        @Override
        public void send(byte[] message) {
            pending.add(message);
        }

        List<byte[]> take() {
            List<byte[]> r = new ArrayList<byte[]>(pending);
            pending.clear();
            return r;
        }
    }

    private ReplicaServer server;
    private final List<ReplicaClient> clients = new ArrayList<ReplicaClient>();
    private final List<Queue> down = new ArrayList<Queue>();
    private final List<Queue> up = new ArrayList<Queue>();
    private final List<Integer> handles = new ArrayList<Integer>();
    private final List<StringBuilder> outputs = new ArrayList<StringBuilder>();

    private ReplicaClient join(String name) {
        Queue d = new Queue();
        Queue u = new Queue();
        Engine ce = new Engine(Engine.CLIENT);
        final StringBuilder out = new StringBuilder();
        ce.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.append(type).append(": ").append(text).append('\n');
            }
        });
        ReplicaClient c = new ReplicaClient(ce, u);
        handles.add(Integer.valueOf(server.connect(d, clients.size() + 1, name, name)));
        clients.add(c);
        down.add(d);
        up.add(u);
        outputs.add(out);
        return c;
    }

    private void frames(int n) {
        for (int f = 0; f < n; f++) {
            server.getEngine().step(1 / 60.0);
            server.flush();
            for (int i = 0; i < clients.size(); i++) {
                for (byte[] m : down.get(i).take()) {
                    clients.get(i).receive(m);
                }
                clients.get(i).getEngine().step(1 / 60.0);
                clients.get(i).tick();
                for (byte[] m : up.get(i).take()) {
                    server.receive(handles.get(i).intValue(), m);
                }
            }
        }
    }

    private static Instance add(Engine e, Instance parent, String cls, String name) {
        Instance i = e.create(cls);
        i.setName(name);
        i.setParent(parent);
        return i;
    }

    private Engine place() {
        Engine s = new Engine(Engine.SERVER);
        Instance rs = s.service("ReplicatedStorage");
        add(s, rs, "RemoteEvent", "Ping");
        add(s, rs, "RemoteFunction", "Ask");
        Instance floor = add(s, s.getWorkspace(), "Part", "Floor");
        floor.put("Anchored", Boolean.TRUE);
        floor.put("Size", new Vector3(200, 1, 200));
        floor.put("CFrame", CFrame.at(0, 0, 0));
        Instance secret = add(s, s.service("ServerStorage"), "Folder", "Secret");
        secret.setAttribute("code", "1234");
        add(s, s.service("ServerScriptService"), "Script", "Server").put("Source",
                "local rs = game:GetService('ReplicatedStorage')\n"
                + "rs.Ping.OnServerEvent:Connect(function(p, n)\n"
                + "  local part = Instance.new('Part')\n"
                + "  part.Name = 'From' .. p.Name\n"
                + "  part.Anchored = true\n"
                + "  part:SetAttribute('n', n)\n"
                + "  part.Parent = workspace\n"
                + "  rs.Ping:FireClient(p, n * 2)\n"
                + "end)\n"
                + "rs.Ask.OnServerInvoke = function(p, a, b) return a + b, p.Name end\n");
        Instance sps = s.service("StarterPlayer").findFirstChild("StarterPlayerScripts");
        add(s, sps, "LocalScript", "Client").put("Source",
                "local rs = game:GetService('ReplicatedStorage')\n"
                + "rs.Ping.OnClientEvent:Connect(function(v) print('pong', v) end)\n"
                + "rs.Ping:FireServer(21)\n"
                + "local sum, who = rs.Ask:InvokeServer(2, 3)\n"
                + "print('asked', sum, who)\n");
        return s;
    }

    @Test
    void replicatesWorldEventsAndCalls() {
        server = new ReplicaServer(place());
        ReplicaClient alice = join("Alice");
        frames(5);
        ReplicaClient bob = join("Bob");
        frames(30);
        assertTrue(alice.isJoined() && bob.isJoined());
        Engine a = alice.getEngine();
        Engine b = bob.getEngine();
        assertEquals("Alice", a.getLocalPlayer().getName());
        assertNotNull(a.getWorkspace().findFirstChild("Floor"));
        assertNotNull(b.service("Players").findFirstChild("Alice"));
        assertNotNull(a.service("Players").findFirstChild("Bob"));
        assertTrue(outputs.get(0).toString().contains("pong 42"), outputs.get(0).toString());
        assertTrue(outputs.get(0).toString().contains("asked 5 Alice"), outputs.get(0).toString());
        Instance fromAlice = b.getWorkspace().findFirstChild("FromAlice");
        assertNotNull(fromAlice, "a part the server made for Alice reaches Bob");
        assertEquals(21.0, ((Number) fromAlice.getAttribute("n")).doubleValue(), 0);
        assertNull(a.service("ServerStorage").findFirstChild("Secret"), "server storage stays on the server");
        assertTrue(a.service("ServerScriptService").getChildren().isEmpty());
        Instance serverPart = server.getEngine().getWorkspace().findFirstChild("FromAlice");
        serverPart.put("CFrame", CFrame.at(5, 7, 9));
        frames(2);
        assertEquals(7.0, ((CFrame) fromAlice.prop("CFrame")).y, 1e-9);
        serverPart.destroy();
        frames(2);
        assertNull(b.getWorkspace().findFirstChild("FromAlice"));
    }

    @Test
    void clientsDriveTheirOwnCharacters() {
        server = new ReplicaServer(place());
        ReplicaClient alice = join("Alice");
        join("Bob");
        frames(60);
        Instance mine = alice.getEngine().getLocalPlayer().ref("Character");
        assertNotNull(mine, "Alice's character replicated and bound");
        Instance root = mine.findFirstChild("HumanoidRootPart");
        CFrame start = (CFrame) root.prop("CFrame");
        root.put("CFrame", CFrame.at(start.x + 0.2, start.y, start.z));
        frames(2);
        Instance onServer = server.getEngine().service("Players").findFirstChild("Alice").ref("Character")
                .findFirstChild("HumanoidRootPart");
        assertEquals(start.x + 0.2, ((CFrame) onServer.prop("CFrame")).x, 0.5);
        Instance seenByBob = clients.get(1).getEngine().service("Players").findFirstChild("Alice").ref("Character")
                .findFirstChild("HumanoidRootPart");
        assertEquals(((CFrame) onServer.prop("CFrame")).x, ((CFrame) seenByBob.prop("CFrame")).x, 0.5);
        root.put("CFrame", CFrame.at(start.x + 500, start.y, start.z));
        frames(3);
        assertTrue(server.getRejectedMoves() > 0, "a teleport is refused");
        assertTrue(((CFrame) onServer.prop("CFrame")).x < start.x + 100);
        assertTrue(((CFrame) root.prop("CFrame")).x < start.x + 100, "and the client is corrected");
    }
}
