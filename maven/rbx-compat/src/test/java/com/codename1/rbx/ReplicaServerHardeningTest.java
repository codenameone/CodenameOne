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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/// What a hostile client can send the server: forged call results, moves
/// that add up to a teleport or carry NaN, and tables nested to overflow
/// the stack.
class ReplicaServerHardeningTest {
    private static final ReplicaServer.Link DISCARD = new ReplicaServer.Link() {
        @Override
        public void send(byte[] message) {
        }
    };

    private ReplicaServer server;
    private final StringBuilder log = new StringBuilder();

    private void frames(int n) {
        for (int f = 0; f < n; f++) {
            server.getEngine().step(1 / 60.0);
            server.flush();
        }
    }

    private Engine place(String script) {
        Engine s = new Engine(Engine.SERVER);
        s.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                log.append(type).append(": ").append(text).append('\n');
            }
        });
        Instance q = s.create("RemoteFunction");
        q.setName("Q");
        q.setParent(s.service("ReplicatedStorage"));
        Instance floor = s.create("Part");
        floor.setName("Floor");
        floor.put("Anchored", Boolean.TRUE);
        floor.put("Size", new Vector3(200, 1, 200));
        floor.put("CFrame", CFrame.at(0, 0, 0));
        floor.setParent(s.getWorkspace());
        if (script != null) {
            Instance sc = s.create("Script");
            sc.setParent(s.service("ServerScriptService"));
            sc.put("Source", script);
        }
        return s;
    }

    private static final String ASK_ALICE = "local rs = game:GetService('ReplicatedStorage')\n"
            + "local p\n"
            + "repeat wait() p = game:GetService('Players'):FindFirstChild('Alice') until p\n"
            + "local v = rs.Q:InvokeClient(p)\n"
            + "workspace:SetAttribute('got', v == nil and 'nil' or tostring(v))\n";

    private byte[] result(int call, Object value) throws IOException {
        Wire.Writer w = new Wire.Writer();
        DataOutputStream out = w.op(Wire.RESULT);
        out.writeInt(call);
        Wire.values(out, server.getEngine(), server, new Object[] {value});
        return w.done();
    }

    private byte[] moves(CFrame... frames) throws IOException {
        Wire.Writer w = new Wire.Writer();
        for (CFrame cf : frames) {
            DataOutputStream out = w.op(Wire.MOVE);
            Wire.value(out, server.getEngine(), server, cf);
            Wire.value(out, server.getEngine(), server, null);
            out.writeBoolean(false);
        }
        return w.done();
    }

    @Test
    void onlyTheInvokedPlayerCanAnswer() throws IOException {
        server = new ReplicaServer(place(ASK_ALICE));
        frames(2);
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        int bob = server.connect(DISCARD, 2, "Bob", "Bob");
        frames(5);
        server.receive(bob, result(1, Double.valueOf(99)));
        frames(1);
        assertNull(server.getEngine().getWorkspace().getAttribute("got"), "Bob cannot answer Alice's call");
        server.receive(alice, result(1, Double.valueOf(7)));
        frames(1);
        assertEquals("7", server.getEngine().getWorkspace().getAttribute("got"), log.toString());
    }

    @Test
    void leavingResumesTheCallsWaitingOnThePlayer() {
        server = new ReplicaServer(place(ASK_ALICE));
        frames(2);
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        frames(5);
        assertNull(server.getEngine().getWorkspace().getAttribute("got"));
        server.disconnect(alice);
        frames(1);
        assertEquals("nil", server.getEngine().getWorkspace().getAttribute("got"), log.toString());
    }

    private Instance rootOf(int handle) {
        Instance ch = server.player(handle).ref("Character");
        assertNotNull(ch, "the character spawned");
        return ch.findFirstChild("HumanoidRootPart");
    }

    @Test
    void manyMovesInOneFrameShareOneAllowance() throws IOException {
        server = new ReplicaServer(place(null));
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        frames(60);
        Instance root = rootOf(alice);
        CFrame start = (CFrame) root.prop("CFrame");
        CFrame[] steps = new CFrame[500];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = CFrame.at(start.x + 0.9 * (i + 1), start.y, start.z);
        }
        frames(1);
        server.receive(alice, moves(steps));
        assertTrue(((CFrame) root.prop("CFrame")).x < start.x + 30, "500 small moves are not a teleport");
        assertTrue(server.getRejectedMoves() > 0);
    }

    @Test
    void nonFiniteAndSkewedFramesAreRefused() throws IOException {
        server = new ReplicaServer(place(null));
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        frames(60);
        Instance root = rootOf(alice);
        CFrame start = (CFrame) root.prop("CFrame");
        frames(1);
        server.receive(alice, moves(CFrame.at(Double.NaN, start.y, start.z)));
        frames(1);
        server.receive(alice, moves(CFrame.at(start.x, Double.NEGATIVE_INFINITY, start.z)));
        frames(1);
        server.receive(alice, moves(new CFrame(start.x, start.y, start.z, 5, 0, 0, 0, 5, 0, 0, 0, 5)));
        assertEquals(3, server.getRejectedMoves());
        CFrame now = (CFrame) root.prop("CFrame");
        assertTrue(ReplicaServer.validFrame(now));
        frames(1);
        server.receive(alice, moves(CFrame.at(start.x, start.y - 3, start.z)));
        assertEquals(3, server.getRejectedMoves(), "falling a few studs in a frame is legitimate");
        frames(1);
        server.receive(alice, moves(CFrame.at(start.x, start.y - 2000, start.z)));
        assertEquals(4, server.getRejectedMoves(), "but not dropping through the world");
    }

    private static byte[] nested(int depth) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        for (int i = 0; i < depth; i++) {
            out.writeByte(14); // table
            out.writeInt(1);
            out.writeByte(15); // int key
            out.writeInt(1);
        }
        out.writeByte(0);
        return b.toByteArray();
    }

    @Test
    void deepTablesAndBadKeysAreMalformedNotFatal() throws IOException {
        server = new ReplicaServer(place(null));
        final Engine e = server.getEngine();
        final byte[] deep = nested(100000);
        assertThrows(IOException.class, () -> Wire.value(Wire.reader(deep), e, server));
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        out.writeByte(14);
        out.writeInt(1);
        out.writeByte(3); // number key
        out.writeDouble(Double.NaN);
        out.writeByte(2);
        final byte[] nan = b.toByteArray();
        assertThrows(IOException.class, () -> Wire.value(Wire.reader(nan), e, server));
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        ByteArrayOutputStream ev = new ByteArrayOutputStream();
        DataOutputStream eo = new DataOutputStream(ev);
        eo.writeByte(Wire.EVENT);
        eo.writeInt(0); // nil remote
        eo.writeInt(1); // one argument
        eo.write(deep);
        server.receive(alice, ev.toByteArray());
        assertNotNull(server.player(alice));
    }

    private static byte[] chats(int n, String text, String channel) throws IOException {
        Wire.Writer w = new Wire.Writer();
        for (int k = 0; k < n; k++) {
            DataOutputStream out = w.op(Wire.CHAT);
            out.writeUTF(text);
            out.writeUTF(channel);
        }
        return w.done();
    }

    private final java.util.List<String> heard = new java.util.ArrayList<String>();

    private int chatServer() {
        server = new ReplicaServer(place(null));
        server.getEngine().addListener(new Engine.ChatListener() {
            @Override
            public void chat(Instance from, String text, String channel) {
                heard.add(text);
            }
        });
        frames(2);
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        frames(2);
        return alice;
    }

    @Test
    void chatIsRateLimitedAndOneMessageCannotCarryAFlood() throws IOException {
        int alice = chatServer();
        // One message with 100k chat ops relays only the burst.
        server.receive(alice, chats(100000, "spam", "RBXGeneral"));
        assertEquals((int) ReplicaServer.CHAT_BURST, heard.size());
        server.receive(alice, chats(1, "more", "RBXGeneral"));
        assertEquals((int) ReplicaServer.CHAT_BURST, heard.size(), "no tokens left this frame");
        frames(60);
        server.receive(alice, chats(1, "later", "RBXGeneral"));
        assertEquals("later", heard.get(heard.size() - 1), "the allowance refills with time");
    }

    @Test
    void chatLengthIsCappedAndForgedChannelsAreDropped() throws IOException {
        int alice = chatServer();
        StringBuilder longLine = new StringBuilder();
        for (int k = 0; k < 1000; k++) {
            longLine.append('x');
        }
        server.receive(alice, chats(1, longLine.toString(), "RBXGeneral"));
        assertEquals(ReplicaServer.MAX_CHAT_LENGTH, heard.get(0).length());
        server.receive(alice, chats(1, "hi", "<script>"));
        server.receive(alice, chats(1, "hi", ""));
        assertEquals(1, heard.size(), "lines on forged channels are not relayed");
        assertTrue(ReplicaServer.validChannel("All"));
    }

    @Test
    void invokesPastTheRateAreAnsweredEmpty() throws IOException {
        server = new ReplicaServer(place("local q = game:GetService('ReplicatedStorage').Q\n"
                + "local n = 0\n"
                + "q.OnServerInvoke = function() n = n + 1 workspace:SetAttribute('n', n) return n end\n"));
        frames(2);
        int alice = server.connect(DISCARD, 1, "Alice", "Alice");
        frames(2);
        Instance q = server.getEngine().service("ReplicatedStorage").findFirstChild("Q");
        for (int m = 0; m < 10; m++) {
            Wire.Writer w = new Wire.Writer();
            for (int k = 0; k < ReplicaServer.MAX_OPS_PER_MESSAGE; k++) {
                DataOutputStream out = w.op(Wire.INVOKE);
                out.writeInt(k);
                Wire.ref(out, server.getEngine(), server, q);
                Wire.values(out, server.getEngine(), server, new Object[0]);
            }
            server.receive(alice, w.done());
        }
        frames(1);
        Object n = server.getEngine().getWorkspace().getAttribute("n");
        assertNotNull(n, log.toString());
        assertTrue(((Number) n).intValue() <= (int) ReplicaServer.INVOKE_BURST, "ran " + n);
    }
}
