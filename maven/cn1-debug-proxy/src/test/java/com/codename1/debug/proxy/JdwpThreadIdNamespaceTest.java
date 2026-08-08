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

    /** ParparVM's tagged encoding of an int. */
    private static long taggedInt(int value) {
        return ((long) value << 1) | 1L;
    }
}
