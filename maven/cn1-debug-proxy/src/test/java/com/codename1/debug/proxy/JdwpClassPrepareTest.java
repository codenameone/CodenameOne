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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A registered ClassPrepare request has to actually fire.
 *
 * <p>IntelliJ and NetBeans do not resolve a line breakpoint against the class
 * list alone: any breakpoint whose class they have not seen prepared is
 * deferred to a ClassPrepare event. The proxy accepted those requests, handed
 * back a request id and then never sent anything, so the deferred breakpoints
 * stayed unarmed forever — the "breakpoint in a listener never stops" half of
 * issue #5333.</p>
 *
 * <p>ParparVM links its whole class set into the binary, so every class is
 * prepared before a debugger can connect. Replaying the matching classes at
 * registration is therefore the complete answer, not an approximation.</p>
 */
public class JdwpClassPrepareTest {

    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "class\t1\tcom_example_Main_1\tMain.java\t-1\tcom/example/Main$1\n"
          + "class\t2\tjava_lang_String\tString.java\t-1\tjava/lang/String\n";

    @Test
    public void aMatchingPatternFiresForEveryClassItCovers() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(new String[] { "com.example.*" }, new String[0]);

            List<String> prepared = f.preparedSignatures(rid);
            assertEquals(Arrays.asList("Lcom/example/Main$1;", "Lcom/example/Main;"), prepared);
        }
    }

    /**
     * The inner class is the one that matters here: a listener body compiles
     * into {@code Main$1}, and that is exactly the class an IDE has not seen
     * when the file is first opened.
     */
    @Test
    public void anExactPatternFiresOnlyForThatClass() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(new String[] { "com.example.Main$1" }, new String[0]);

            assertEquals(Collections.singletonList("Lcom/example/Main$1;"),
                    f.preparedSignatures(rid));
        }
    }

    /** IntelliJ attaches java.* excludes to nearly every request it makes. */
    @Test
    public void anExcludePatternSuppressesItsClasses() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(new String[0], new String[] { "java.*" });

            assertEquals(Arrays.asList("Lcom/example/Main$1;", "Lcom/example/Main;"),
                    f.preparedSignatures(rid));
        }
    }

    /** A leading wildcard matches on the tail of the name. */
    @Test
    public void aLeadingWildcardMatchesTheEndOfTheName() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(new String[] { "*.String" }, new String[0]);

            assertEquals(Collections.singletonList("Ljava/lang/String;"),
                    f.preparedSignatures(rid));
        }
    }

    /**
     * The events must not freeze the app. The IDE typically asks for
     * SUSPEND_ALL, but at registration time nothing is stopped and there is no
     * event thread to resume — honouring it would wedge the session on attach.
     */
    @Test
    public void replayedEventsDoNotSuspendTheApplication() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(2 /* SUSPEND_ALL */,
                    new String[] { "com.example.*" }, new String[0]);

            List<JdwpTestClient.Event> events = f.eventsFor(rid);
            assertTrue("expected the replay to produce events", !events.isEmpty());
            for (JdwpTestClient.Event e : events) {
                assertEquals("replayed ClassPrepare must not suspend", 0, e.suspendPolicy);
            }
        }
    }

    /** A cleared request stops matching; re-registering starts it again. */
    @Test
    public void clearingARequestIsAcceptedAndReRegistrationFiresAgain() throws Exception {
        try (Fixture f = new Fixture()) {
            int rid = f.registerClassPrepare(new String[] { "com.example.Main" }, new String[0]);
            assertEquals(1, f.preparedSignatures(rid).size());

            byte[] clear = new byte[5];
            clear[0] = JdwpTestClient.EK_CLASS_PREPARE;
            clear[1] = (byte) (rid >>> 24);
            clear[2] = (byte) (rid >>> 16);
            clear[3] = (byte) (rid >>> 8);
            clear[4] = (byte) rid;
            assertEquals(0, f.client.send(JdwpTestClient.CS_EVENT_REQUEST, 2, clear).errorCode);

            int second = f.registerClassPrepare(new String[] { "com.example.Main" }, new String[0]);
            assertEquals(Collections.singletonList("Lcom/example/Main;"),
                    f.preparedSignatures(second));
        }
    }

    /** A breakpoint request must keep working alongside class-prepare handling. */
    @Test
    public void breakpointRequestsStillGetTheirOwnRequestId() throws Exception {
        try (Fixture f = new Fixture()) {
            // classId 0 / methodId 0 arrive as 1 — the proxy shifts both by one
            // at the JDWP boundary so that id 0 stays reserved for "null".
            byte[] set = JdwpTestClient.lineBreakpointRequest(1L, 1L, 12L);

            JdwpTestClient.Reply reply = f.client.send(JdwpTestClient.CS_EVENT_REQUEST, 1, set);
            assertEquals(0, reply.errorCode);
            assertTrue("a breakpoint request should get a non-zero id",
                    reply.stream().readInt() > 0);
        }
    }

    /**
     * A request pinned to one type by ClassOnly fires only for that type.
     *
     * <p>The modifier was parsed for its width and discarded, so a request
     * carrying it and no name pattern was left with nothing to match on —
     * which reads as "every class". A client asking about one type got a
     * prepare event for the whole symbol table.</p>
     */
    @Test
    public void aClassOnlyRestrictionIsHonoured() throws Exception {
        try (Fixture f = new Fixture()) {
            // classId 1 (Main$1) arrives as JDWP reference id 2.
            JdwpTestClient.Reply reply = f.client.send(JdwpTestClient.CS_EVENT_REQUEST, 1,
                    JdwpTestClient.classPrepareRequest(0, new String[0], new String[0], 2L));
            assertEquals(0, reply.errorCode);
            int rid = reply.stream().readInt();

            assertEquals(Collections.singletonList("Lcom/example/Main$1;"),
                    f.preparedSignatures(rid));
        }
    }

    /** ClassOnly and a pattern together intersect rather than either winning. */
    @Test
    public void aClassOnlyRestrictionCombinesWithAPattern() throws Exception {
        try (Fixture f = new Fixture()) {
            JdwpTestClient.Reply reply = f.client.send(JdwpTestClient.CS_EVENT_REQUEST, 1,
                    JdwpTestClient.classPrepareRequest(0, new String[] { "java.*" },
                            new String[0], 2L));
            assertEquals(0, reply.errorCode);
            int rid = reply.stream().readInt();

            assertTrue("a pattern that excludes the pinned class yields nothing",
                    f.preparedSignatures(rid).isEmpty());
        }
    }

    // ---- fixture -----------------------------------------------------------

    private static final class Fixture implements AutoCloseable {
        final JdwpServer server;
        final JdwpTestClient client;

        Fixture() throws Exception {
            int port = JdwpTestClient.freePort();
            server = new JdwpServer(port);
            client = JdwpTestClient.attach(server, port);
            // Unblock the wait-for-symbols gate; no device is attached, which
            // keeps these tests to the JDWP side of the proxy.
            server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                    TABLE.getBytes(StandardCharsets.UTF_8))));
            server.onHello(1);
        }

        int registerClassPrepare(String[] includes, String[] excludes) throws Exception {
            return registerClassPrepare(0, includes, excludes);
        }

        int registerClassPrepare(int suspendPolicy, String[] includes, String[] excludes)
                throws Exception {
            JdwpTestClient.Reply reply = client.send(JdwpTestClient.CS_EVENT_REQUEST, 1,
                    JdwpTestClient.classPrepareRequest(suspendPolicy, includes, excludes));
            assertEquals(0, reply.errorCode);
            return reply.stream().readInt();
        }

        List<JdwpTestClient.Event> eventsFor(int requestId) throws Exception {
            List<JdwpTestClient.Event> matching = new ArrayList<>();
            for (JdwpTestClient.Event e : client.drainEvents()) {
                if (e.eventKind == JdwpTestClient.EK_CLASS_PREPARE && e.requestId == requestId) {
                    matching.add(e);
                }
            }
            return matching;
        }

        /** Signatures of the classes prepared for a request, sorted for stability. */
        List<String> preparedSignatures(int requestId) throws Exception {
            List<String> signatures = new ArrayList<>();
            for (JdwpTestClient.Event e : eventsFor(requestId)) {
                signatures.add(e.classPrepareSignature());
            }
            Collections.sort(signatures);
            return signatures;
        }

        @Override public void close() {
            client.close();
        }
    }
}
