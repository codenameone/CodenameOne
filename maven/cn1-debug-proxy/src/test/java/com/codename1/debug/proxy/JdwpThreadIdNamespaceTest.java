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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * A thread ID can never be mistaken for a reference, or the reverse.
 *
 * <p>A debugger is entitled to ask about a thread ID as an object — jdb does
 * exactly that while drawing its thread panel — so the proxy has to be able to
 * tell one from the other. ParparVM's thread IDs are small integers, which is
 * the same shape as two other things that arrive here:</p>
 *
 * <ul>
 *   <li>tagged ints, encoded as {@code (v << 1) | 1}, so {@code Integer 0} is
 *       1 — indistinguishable from thread 1;</li>
 *   <li>heap references, which are aligned and so even.</li>
 * </ul>
 *
 * <p>These tests state the separation as a property over the values that
 * actually collide, rather than restating the arithmetic that produces it.</p>
 */
public class JdwpThreadIdNamespaceTest {

    /** Round-tripping a thread ID gives back the device's own. */
    @Test
    public void aThreadIdSurvivesTheRoundTrip() {
        for (long tid : new long[] { 0, 1, 2, 3, 7, 21, 1023, 65535, 1L << 31 }) {
            assertEquals(tid, JdwpTestClient.fromJdwpThread(JdwpTestClient.toJdwpThread(tid)));
        }
    }

    /**
     * No thread ID equals the tagged encoding of any int — the collision that
     * had {@code Integer 0} and thread 1 share the value 1.
     */
    @Test
    public void noThreadIdCollidesWithATaggedInt() {
        for (long tid = 0; tid < 512; tid++) {
            long threadId = JdwpTestClient.toJdwpThread(tid);
            for (int value = -512; value < 512; value++) {
                assertNotEquals("thread " + tid + " collides with Integer " + value,
                        taggedInt(value), threadId);
            }
        }
    }

    /**
     * Every thread ID is even, which is what rules out the whole tagged-int
     * range at once rather than the sample above.
     */
    @Test
    public void everyThreadIdIsEvenAndSoNeverATaggedValue() {
        for (long tid = 0; tid < 1024; tid++) {
            long threadId = JdwpTestClient.toJdwpThread(tid);
            assertEquals("thread " + tid + " must be even", 0, threadId & 1);
        }
    }

    /**
     * Every thread ID sits above the addresses a 64-bit iOS process can hold,
     * so it cannot equal a real reference either.
     */
    @Test
    public void everyThreadIdIsAboveTheAddressSpace() {
        // Darwin user-space pointers stay well inside 2^48.
        long ceiling = 1L << 48;
        for (long tid = 0; tid < 1024; tid++) {
            assertTrue("thread " + tid + " must not look like a pointer",
                    JdwpTestClient.toJdwpThread(tid) > ceiling);
        }
    }

    /**
     * Every thread ID the IDE is handed carries the tag, including the ones
     * that are placeholders rather than real threads.
     *
     * <p>VM_START and a replayed CLASS_PREPARE both name a thread the proxy
     * has to invent, and both wrote it raw. That is the same defect the
     * namespace exists to prevent, twice: the ID decodes to a different device
     * thread on the way back — raw 1 to thread 0 — and a small raw value is
     * indistinguishable from a tagged int.</p>
     */
    @Test
    public void eventsNamingAPlaceholderThreadStillTagIt() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                    TABLE.getBytes(StandardCharsets.UTF_8))));
            server.onHello(1);

            // A ClassPrepare request replays the already-loaded classes.
            client.send(JdwpTestClient.CS_EVENT_REQUEST, 1,
                    JdwpTestClient.classPrepareRequest(0, new String[0], new String[0]));

            boolean sawVmStart = false;
            boolean sawClassPrepare = false;
            for (JdwpTestClient.Event e : client.drainEvents()) {
                if (e.eventKind != 90 && e.eventKind != 8) continue;
                long threadId = new DataInputStream(
                        new ByteArrayInputStream(e.rest)).readLong();
                assertTrue("event kind " + e.eventKind + " must carry a tagged thread id,"
                                + " got 0x" + Long.toHexString(threadId),
                        (threadId & JdwpTestClient.THREAD_ID_TAG) != 0);
                assertEquals("and it must be even, like every thread id",
                        0, threadId & 1);
                if (e.eventKind == 90) sawVmStart = true;
                if (e.eventKind == 8) sawClassPrepare = true;
            }
            assertTrue("expected a VM_START", sawVmStart);
            assertTrue("expected a replayed CLASS_PREPARE", sawClassPrepare);
        }
    }

    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "method\t0\t0\thandler\t()V\t0\n";

    /** ParparVM's tagged encoding of an int. */
    private static long taggedInt(int value) {
        return ((long) value << 1) | 1L;
    }
}
