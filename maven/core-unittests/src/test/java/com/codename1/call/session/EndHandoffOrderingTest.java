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
package com.codename1.call.session;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A validation and the native handoff it authorises are one step.
 *
 * <p>Every operation that reaches a port hands off by call id alone -- the
 * SPI carries no session identity, deliberately, because it crosses into
 * Objective-C through ParparVM. So a check of "do I still own this id"
 * followed by an unordered handoff is not a check at all: a replacement can
 * claim the id in between, and the port then acts on whatever holds it now.
 * For {@code end()} that meant finishing somebody else's live call and
 * acknowledging success, while that call stayed registered in Java.</p>
 *
 * <p><b>Why this is a source check.</b> The window is between two statements
 * on one thread and closing it is the fix; reaching it from a test needs a
 * seam in production code that exists only for the test, which is worse than
 * the test is worth. So this asserts the structure: {@code end()} holds
 * {@code HANDOFF} across both, {@code report()} holds it across the
 * registration and its own handoff -- one without the other orders nothing --
 * and the lock is not the registry monitor, which the ports take while
 * delivering.</p>
 */
class EndHandoffOrderingTest {

    private static final String SESSION =
            "../../CodenameOne/src/com/codename1/call/session/"
            + "CallSession.java";

    private static final String CALLS =
            "../../CodenameOne/src/com/codename1/call/session/Calls.java";

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()),
                StandardCharsets.UTF_8);
    }

    private static String body(String src, String signature, String until) {
        int at = src.indexOf(signature);
        assertTrue(at >= 0, signature + " has to exist");
        int end = src.indexOf(until, at);
        assertTrue(end > at, until + " has to follow " + signature);
        StringBuilder sb = new StringBuilder();
        for (String line : src.substring(at, end).split("\n", -1)) {
            if (!line.trim().startsWith("//") && !line.trim().startsWith("///")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    void endHoldsTheHandoffLockAcrossItsOwnershipCheck() throws Exception {
        String body = body(read(SESSION),
                "public AsyncResource<Boolean> end(CallEndReason reason)",
                // The method SIGNATURE, not the bare name: end()'s own
                // comments explain why reportEndedRemotely never had this
                // hole, so searching for the name truncated the body before
                // the lock and the test failed on its own explanation.
                "public void reportEndedRemotely(");
        int lock = body.indexOf("synchronized (Calls.HANDOFF)");
        int owns = body.indexOf("Calls.owns(callId, this)");
        int handoff = body.indexOf("b.endCall(");
        assertTrue(lock >= 0, "end has to order itself against the registry");
        assertTrue(lock < owns,
                "the lock is taken before the check, or the check can go"
                + " stale inside it");
        assertTrue(owns < handoff,
                "and the handoff happens under the same lock");
    }

    @Test
    void reportHoldsItAcrossRegistrationAndItsOwnHandoff() throws Exception {
        // Half a lock orders nothing. If a report can register an id and
        // reach the port without taking this, an end validated against the
        // previous owner still slips past it.
        String src = read(CALLS);
        int lock = src.indexOf("synchronized (HANDOFF) {");
        int put = src.indexOf("SESSIONS.put(id, session);");
        int reportIncoming = src.indexOf("b.reportIncomingCall(");
        assertTrue(lock >= 0, "report has to take the same lock");
        assertTrue(lock < put, "before it publishes the session");
        assertTrue(put < reportIncoming, "and hold it through the handoff");
    }

    @Test
    void theLockIsNotTheRegistryMonitor() throws Exception {
        // SESSIONS is taken by getSession, which the ports call while
        // delivering, so holding it across a bridge call is a deadlock rather
        // than an ordering.
        String src = read(CALLS);
        assertTrue(src.contains("static final Object HANDOFF = new Object();"),
                "the lock is its own object, not the registry monitor");
    }
}
