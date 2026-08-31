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
package com.codename1.call;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two reports that move a live call reach the platform in order.
 *
 * <p>Guarding the state was not enough. The check released the session
 * monitor before the bridge call, so a connecting report could pass its test,
 * a connect on another thread could move the session to {@code ACTIVE} and
 * tell Telecom {@code setActive}, and the connecting report could then arrive
 * with {@code setDialing}. Telecom regresses to dialing while the session
 * stays active: not a race about which correct state wins, but the two halves
 * of this API disagreeing where the user can see it.</p>
 *
 * <p><b>Why this is a source check.</b> Reaching that window from a test
 * needs a seam between the state test and the bridge call, and a seam that
 * exists only for a test is worse than the test is worth. So this asserts the
 * structure instead: both reports take the {@code reporting} lock, and the
 * lock is not the session monitor -- which is the part that matters, because
 * the monitor cannot be held across a bridge call without deadlocking against
 * a port callback.</p>
 *
 * <p>The behavioural half lives in {@code LocalCallTest}, which pins the
 * state rule sequentially and says plainly that it does not cover the
 * interleaving.</p>
 */
class ReportSerialisationParityTest {

    private static final String SESSION =
            "../../CodenameOne/src/com/codename1/call/session/"
            + "CallSession.java";

    static boolean sourcePresent() {
        return new File(SESSION).exists();
    }

    private static String body(String src, String signature) {
        int at = src.indexOf(signature);
        assertTrue(at >= 0, signature + " has to exist");
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, signature + " has to end");
        StringBuilder sb = new StringBuilder();
        for (String line : src.substring(at, end).split("\n", -1)) {
            if (!line.trim().startsWith("//")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String source() throws Exception {
        return new String(Files.readAllBytes(new File(SESSION).toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    @EnabledIf("sourcePresent")
    void bothReportsHoldTheSameLockAcrossTheirBridgeCall() throws Exception {
        String src = source();
        String connecting = body(src, "public void reportStartedConnecting()");
        String connected = body(src, "public void reportConnected()");
        for (String[] which : new String[][] {
                {"reportStartedConnecting", connecting},
                {"reportConnected", connected}}) {
            String name = which[0];
            String text = which[1];
            int lock = text.indexOf("synchronized (reporting)");
            int call = text.indexOf("b.report");
            assertTrue(lock >= 0,
                    name + " has to serialise against the other report");
            assertTrue(lock < call,
                    name + " has to take the lock BEFORE it tells the"
                    + " platform, or the ordering it buys is nothing");
        }
    }

    @Test
    @EnabledIf("sourcePresent")
    void theLockIsNotTheSessionMonitor() throws Exception {
        // The monitor cannot be held across a bridge call: the ports call
        // back into Java, so that is a deadlock rather than an ordering. A
        // separate lock, held only by reports and only for the length of one,
        // orders them without standing between a port callback and the state
        // it needs.
        String src = source();
        assertTrue(src.contains("private final Object reporting"),
                "the lock is its own object");
        for (String signature : new String[] {
                "public void reportStartedConnecting()",
                "public void reportConnected()"}) {
            assertFalse(body(src, signature).contains("synchronized (this)"),
                    signature + " must not hold the session monitor across a"
                    + " bridge call");
        }
    }
}
