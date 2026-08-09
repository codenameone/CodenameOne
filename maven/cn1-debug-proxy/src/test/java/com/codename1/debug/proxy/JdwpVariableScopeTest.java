/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.debug.proxy;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The IDE is told which lines each local is in scope for.
 *
 * <p>Every local used to be reported as live from code index 0 for
 * {@code Integer.MAX_VALUE}, so the IDE asked for every slot at every stop.
 * For a slot two disjoint scopes share that means the variable the code has
 * not reached is shown next to the one it has, displaying storage that belongs
 * to the other scope.</p>
 *
 * <p>ParparVM has no bytecode offsets — a JDWP "code index" here is the source
 * line — so a scope travels as its declaration line plus the number of lines
 * it stays live for.</p>
 */
public class JdwpVariableScopeTest {

    /**
     * One method whose slot 2 holds an {@code int} for lines 10-13 and a
     * {@code String} from line 14, plus a receiver that is live throughout.
     */
    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "method\t0\t0\thandler\t(Ljava/lang/Object;)V\t0\n"
          + "line\t0\t10\n"
          + "line\t0\t14\n"
          + "var\t0\t0\tthis\tLcom/example/Main;\t0\t0\n"
          + "var\t0\t2\tcount\tI\t10\t14\n"
          + "var\t0\t2\tlabel\tLjava/lang/String;\t14\t18\n";

    @Test
    public void aScopedLocalReportsItsDeclarationLineAndLength() throws Exception {
        List<Var> vars = variableTable();

        Var count = byName(vars, "count");
        assertEquals("declared at line 10", 10L, count.codeIndex);
        assertEquals("live for lines 10 through 13", 4, count.length);

        Var label = byName(vars, "label");
        assertEquals(14L, label.codeIndex);
        assertEquals(4, label.length);
    }

    /** The two occupants of the shared slot cover disjoint line ranges. */
    @Test
    public void twoLocalsSharingASlotDoNotOverlap() throws Exception {
        List<Var> vars = variableTable();
        Var count = byName(vars, "count");
        Var label = byName(vars, "label");

        assertEquals("both describe the same slot", count.slot, label.slot);
        assertTrue("ranges must not overlap: " + count + " vs " + label,
                count.codeIndex + count.length <= label.codeIndex);
    }

    /**
     * A local with no scope — the receiver, or one the translator synthesised
     * from a store opcode — stays live for the whole method rather than being
     * hidden everywhere.
     */
    @Test
    public void anUnscopedLocalStaysAlwaysLive() throws Exception {
        Var receiver = byName(variableTable(), "this");

        assertEquals(0L, receiver.codeIndex);
        assertEquals(Integer.MAX_VALUE, receiver.length);
    }

    /**
     * Symbol tables written before scopes were tracked stop at the descriptor
     * column. Those locals must load, and must stay always-live.
     */
    @Test
    public void aLegacyTableWithoutScopeColumnsStillLoads() throws Exception {
        String legacy =
                "version\t1\n"
              + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
              + "method\t0\t0\thandler\t(Ljava/lang/Object;)V\t0\n"
              + "var\t0\t2\tcount\tI\n";

        SymbolTable table = SymbolTable.load(new ByteArrayInputStream(
                legacy.getBytes(StandardCharsets.UTF_8)));
        SymbolTable.LocalVarInfo local = table.methodById(0).locals.get(0);

        assertEquals(0, local.startLine);
        assertEquals(0L, local.jdwpCodeIndex());
        assertEquals(Integer.MAX_VALUE, local.jdwpLength());
    }

    /** VariableTableWithGeneric carries the same ranges, plus a generic slot. */
    @Test
    public void theGenericVariantReportsTheSameRanges() throws Exception {
        List<Var> plain = variableTable(2);
        List<Var> generic = variableTable(5);

        assertEquals(plain.size(), generic.size());
        for (int i = 0; i < plain.size(); i++) {
            assertEquals(plain.get(i).name, generic.get(i).name);
            assertEquals(plain.get(i).codeIndex, generic.get(i).codeIndex);
            assertEquals(plain.get(i).length, generic.get(i).length);
        }
    }

    /**
     * Capabilities are reported honestly: the proxy turns the device's
     * disconnect into a VM_DEATH event and supports nothing else on the list.
     * Claiming more would have the IDE offer class redefinition, frame popping
     * or forced returns and then fail when the developer used them.
     */
    @Test
    public void onlyTheCapabilitiesTheProxyActuallyHasAreClaimed() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            JdwpTestClient.Reply original = client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 12, new byte[0]);
            assertEquals(0, original.errorCode);
            assertEquals("the seven original capabilities are all unsupported",
                    7, original.body.length);
            for (byte capability : original.body) {
                assertEquals(0, capability);
            }

            JdwpTestClient.Reply extended = client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 17, new byte[0]);
            assertEquals(0, extended.errorCode);
            assertEquals(32, extended.body.length);
            // Index 13 is canRequestVMDeathEvent.
            for (int i = 0; i < extended.body.length; i++) {
                assertEquals("capability " + i, i == 13 ? 1 : 0, extended.body[i]);
            }
        }
    }

    /**
     * A registered VM_DEATH request gets its own event when the session ends.
     *
     * <p>The capability is only honest if the request path works. The spec's
     * automatic event carries request id 0; a debugger that registered a
     * request is waiting for the id it was handed, and one that never arrives
     * leaves it waiting for a session end that already happened.</p>
     */
    @Test
    public void aRegisteredVmDeathRequestIsMatchedWhenTheSessionEnds() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            byte[] set = new byte[6];
            set[0] = 99;  // VM_DEATH
            set[1] = 2;   // SUSPEND_ALL
            JdwpTestClient.Reply reply = client.send(JdwpTestClient.CS_EVENT_REQUEST, 1, set);
            assertEquals(0, reply.errorCode);
            int rid = reply.stream().readInt();
            assertTrue("a VM_DEATH request should get a real id", rid > 0);

            server.onVmDeath();

            List<Integer> ids = new ArrayList<>();
            int suspendPolicy = -1;
            for (JdwpTestClient.Event e : client.drainEvents()) {
                if (e.eventKind == 99) {
                    ids.add(e.requestId);
                    suspendPolicy = e.suspendPolicy;
                }
            }
            assertTrue("the automatic event must still be sent", ids.contains(0));
            assertTrue("the registered request must be matched, got " + ids, ids.contains(rid));
            assertEquals("the strongest requested policy wins", 2, suspendPolicy);
        }
    }

    /** Clearing it stops the match, so a stale id is not sent to the next IDE. */
    @Test
    public void aClearedVmDeathRequestIsNoLongerMatched() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            byte[] set = new byte[6];
            set[0] = 99;
            int rid = client.send(JdwpTestClient.CS_EVENT_REQUEST, 1, set).stream().readInt();

            byte[] clear = new byte[5];
            clear[0] = 99;
            clear[1] = (byte) (rid >>> 24);
            clear[2] = (byte) (rid >>> 16);
            clear[3] = (byte) (rid >>> 8);
            clear[4] = (byte) rid;
            assertEquals(0, client.send(JdwpTestClient.CS_EVENT_REQUEST, 2, clear).errorCode);

            server.onVmDeath();

            for (JdwpTestClient.Event e : client.drainEvents()) {
                if (e.eventKind == 99) {
                    assertEquals("only the automatic event remains", 0, e.requestId);
                }
            }
        }
    }

    // ---- helpers -----------------------------------------------------------

    private List<Var> variableTable() throws Exception {
        return variableTable(2);
    }

    /** Runs Method.VariableTable (command 2) or VariableTableWithGeneric (5). */
    private List<Var> variableTable(int command) throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            byte[] payload = new byte[16];
            payload[7] = 1;   // refType id 1 -> classId 0
            payload[15] = 1;  // method id 1 -> methodId 0
            JdwpTestClient.Reply reply = client.send(6 /* Method */, command, payload);
            assertEquals(0, reply.errorCode);

            DataInputStream body = reply.stream();
            body.readInt(); // argCnt
            int count = body.readInt();
            List<Var> vars = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long codeIndex = body.readLong();
                String name = readString(body);
                readString(body); // descriptor
                if (command == 5) {
                    readString(body); // generic signature
                }
                int length = body.readInt();
                int slot = body.readInt();
                vars.add(new Var(name, codeIndex, length, slot));
            }
            return vars;
        }
    }

    private void primeSymbols(JdwpServer server) throws Exception {
        server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                TABLE.getBytes(StandardCharsets.UTF_8))));
        server.onHello(1);
    }

    private static String readString(DataInputStream in) throws Exception {
        byte[] utf8 = new byte[in.readInt()];
        in.readFully(utf8);
        return new String(utf8, StandardCharsets.UTF_8);
    }

    private Var byName(List<Var> vars, String name) {
        for (Var v : vars) {
            if (v.name.equals(name)) return v;
        }
        assertNotNull("no local named " + name + " in " + vars, null);
        return null;
    }

    private static final class Var {
        final String name;
        final long codeIndex;
        final int length;
        final int slot;

        Var(String name, long codeIndex, int length, int slot) {
            this.name = name;
            this.codeIndex = codeIndex;
            this.length = length;
            this.slot = slot;
        }

        @Override public String toString() {
            return name + "@slot" + slot + "[" + codeIndex + "+" + length + "]";
        }
    }
}
