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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The IDE's thread list should show every live thread, not only the ones that
 * have already stopped somewhere.
 *
 * <p>{@code CMD_GET_THREADS} was a device-side stub that replied empty, and the
 * proxy never sent it — so {@code VirtualMachine.AllThreads} could only report
 * ids harvested from breakpoint and step events. A developer who had not hit a
 * breakpoint yet saw an empty Threads panel, which is what
 * <a href="https://github.com/codenameone/CodenameOne/issues/5333">issue
 * #5333</a> asked about.</p>
 */
public class JdwpThreadListTest {

    /** Two classes and one method, enough for the symbol-dependent paths. */
    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "method\t0\t0\tstart\t()V\t0\n"
          + "line\t0\t12\n";

    @Test
    public void allThreadsListsThreadsThatNeverRaisedAnEvent() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7, 9 },
                             new boolean[] { false, false },
                             new long[] { 0, 0 });

            assertEquals(Arrays.asList(7L, 9L), allThreads(client));
        }
    }

    @Test
    public void theThreadGroupReportsTheSameThreadsAsTheVm() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 3, 4, 5 },
                             new boolean[] { false, true, false },
                             new long[] { 0, 0, 0 });

            JdwpTestClient.Reply reply = client.send(
                    JdwpTestClient.CS_THREAD_GROUP_REF, 3, JdwpTestClient.threadId(0xCAFEL));
            assertEquals(0, reply.errorCode);
            DataInputStream body = reply.stream();
            List<Long> ids = new ArrayList<>();
            int count = body.readInt();
            for (int i = 0; i < count; i++) {
                ids.add(body.readLong());
            }
            assertEquals(Arrays.asList(3L, 4L, 5L), ids);
            assertEquals("a group with no child groups", 0, body.readInt());
        }
    }

    /**
     * Suspension is per thread. The old answer came from a single
     * "last thread that suspended" field, so with two threads parked at once
     * the IDE was told one of them was running.
     */
    @Test
    public void statusReflectsEachThreadsOwnSuspendState() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7, 9 },
                             new boolean[] { true, false },
                             new long[] { 0, 0 });

            assertEquals("suspended thread", 1, suspendStatus(client, 7));
            assertEquals("running thread", 0, suspendStatus(client, 9));
            assertEquals("suspend count follows the same flag", 1, suspendCount(client, 7));
            assertEquals(0, suspendCount(client, 9));
        }
    }

    /** A thread the device no longer reports must leave the list. */
    @Test
    public void deadThreadsDropOutOfTheList() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7, 9 }, new boolean[] { false, false }, new long[] { 0, 0 });
            assertEquals(Arrays.asList(7L, 9L), allThreads(client));

            server.onThreads(new long[] { 9 }, new boolean[] { false }, new long[] { 0 });
            assertEquals(Arrays.asList(9L), allThreads(client));
        }
    }

    /**
     * A breakpoint reports the thread as suspended straight away.
     *
     * <p>The cached list is only as fresh as the last refresh, and a thread is
     * normally enumerated while running and stopped a moment later. Preferring
     * the cache unconditionally left the IDE told that the very thread it had
     * just stopped at was still running.</p>
     */
    @Test
    public void aSuspendEventUpdatesTheCachedThreadState() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            // Enumerated while both are running...
            server.onThreads(new long[] { 7, 9 },
                             new boolean[] { false, false },
                             new long[] { 0, 0 });
            assertEquals(0, suspendStatus(client, 7));

            // ...then one stops, with no refresh in between.
            server.onBreakpointHit(7, 0, 12);

            assertEquals("the stopped thread is suspended", 1, suspendStatus(client, 7));
            assertEquals("the other one is not", 0, suspendStatus(client, 9));
        }
    }

    /** Resuming puts it back, so the panel does not stick on "suspended". */
    @Test
    public void resumingClearsTheCachedSuspendState() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7 }, new boolean[] { false }, new long[] { 0 });
            server.onBreakpointHit(7, 0, 12);
            assertEquals(1, suspendStatus(client, 7));

            // VirtualMachine.Resume
            assertEquals(0, client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 9, new byte[0]).errorCode);

            assertEquals("resumed threads are running again", 0, suspendStatus(client, 7));
        }
    }

    /**
     * A thread that stopped once and later exited must leave the list.
     *
     * <p>Event-derived ids accumulate for the life of the session, so unioning
     * them into every answer kept a phantom row in the IDE for every worker
     * that had ever hit a breakpoint. They are a fallback for when the device
     * cannot answer, not an addition to when it can.</p>
     */
    @Test
    public void aThreadThatStoppedAndThenDiedDoesNotLinger() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7, 21 }, new boolean[] { false, false }, new long[] { 0, 0 });
            server.onBreakpointHit(21, 0, 12);
            assertEquals(Arrays.asList(7L, 21L), allThreads(client));

            // The worker exits; the device no longer reports it.
            server.onThreads(new long[] { 7 }, new boolean[] { false }, new long[] { 0 });

            assertEquals(Arrays.asList(7L), allThreads(client));
        }
    }

    /**
     * A device that never answers the enumeration — an older build, whose
     * runtime replies with a bare status instead of a thread list — must not
     * make the panel worse than it was. Event-derived ids stay.
     *
     * <p>Note the distinction from an <em>empty</em> list: that is a device
     * that answered, and is treated as authoritative.</p>
     */
    @Test
    public void threadsSeenInEventsSurviveADeviceThatCannotEnumerate() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onBreakpointHit(21, 0, 12);
            // No onThreads at all — the device never sent EVT_THREAD_LIST.

            assertEquals(Arrays.asList(21L), allThreads(client));
        }
    }

    /** An empty list from a device that can answer means there is nothing to show. */
    @Test
    public void anEmptyDeviceListIsTreatedAsAuthoritative() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onBreakpointHit(21, 0, 12);
            server.onThreads(new long[0], new boolean[0], new long[0]);

            assertEquals(Collections.<Long>emptyList(), allThreads(client));
        }
    }

    /**
     * Naming needs a {@code java.lang.Thread} instance and a live device to
     * read it through. Without either, the synthetic name is still returned —
     * an unnamed row beats a failed request.
     */
    @Test
    public void threadNameFallsBackWhenTheDeviceReportsNoThreadObject() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onThreads(new long[] { 7 }, new boolean[] { false }, new long[] { 0 });

            JdwpTestClient.Reply reply = client.send(
                    JdwpTestClient.CS_THREAD_REFERENCE, 1, JdwpTestClient.threadId(7));
            assertEquals(0, reply.errorCode);
            DataInputStream body = reply.stream();
            byte[] utf8 = new byte[body.readInt()];
            body.readFully(utf8);
            assertEquals("Thread-7", new String(utf8, StandardCharsets.UTF_8));
        }
    }

    // ---- helpers -----------------------------------------------------------

    /**
     * Gets the server past its wait-for-symbols gate without a device
     * attached, so the thread queries answer from the last reported list
     * instead of round-tripping.
     */
    private void primeSymbols(JdwpServer server) throws Exception {
        server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                TABLE.getBytes(StandardCharsets.UTF_8))));
        server.onHello(1);
    }

    private List<Long> allThreads(JdwpTestClient client) throws Exception {
        JdwpTestClient.Reply reply = client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 4, new byte[0]);
        assertEquals(0, reply.errorCode);
        DataInputStream body = reply.stream();
        int count = body.readInt();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(body.readLong());
        }
        assertTrue("ids should be reported in a stable order", isSorted(ids));
        return ids;
    }

    private int suspendStatus(JdwpTestClient client, long tid) throws Exception {
        JdwpTestClient.Reply reply = client.send(
                JdwpTestClient.CS_THREAD_REFERENCE, 4, JdwpTestClient.threadId(tid));
        assertEquals(0, reply.errorCode);
        DataInputStream body = reply.stream();
        body.readInt(); // thread status
        return body.readInt();
    }

    private int suspendCount(JdwpTestClient client, long tid) throws Exception {
        JdwpTestClient.Reply reply = client.send(
                JdwpTestClient.CS_THREAD_REFERENCE, 12, JdwpTestClient.threadId(tid));
        assertEquals(0, reply.errorCode);
        return reply.stream().readInt();
    }

    private boolean isSorted(List<Long> ids) {
        for (int i = 1; i < ids.size(); i++) {
            if (ids.get(i - 1) > ids.get(i)) return false;
        }
        return true;
    }
}
