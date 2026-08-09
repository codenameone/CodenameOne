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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The debugger can hold events and get them all back when it releases them.
 *
 * <p>{@code HoldEvents} and {@code ReleaseEvents} are how a debugger keeps
 * events from arriving while it updates its own view of the VM. jdb's event
 * controller issues {@code HoldEvents} before anything else, so declining it
 * put {@code Unexpected JDWP Error: 100} on the first line of every session,
 * before the developer had typed a command — with nothing naming the command
 * that failed.</p>
 */
public class JdwpEventHoldTest {

    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "method\t0\t0\thandler\t()V\t0\n";

    private static final int VM_HOLD_EVENTS = 15;
    private static final int VM_RELEASE_EVENTS = 16;
    private static final int EK_VM_DEATH = 99;

    /** Both commands are accepted rather than declined. */
    @Test
    public void holdAndReleaseAreAccepted() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            assertEquals("HoldEvents", 0,
                    client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_HOLD_EVENTS, new byte[0]).errorCode);
            assertEquals("ReleaseEvents", 0,
                    client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_RELEASE_EVENTS, new byte[0]).errorCode);
        }
    }

    /** While held, an event does not reach the debugger. */
    @Test
    public void anEventRaisedWhileHeldDoesNotArriveYet() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_HOLD_EVENTS, new byte[0]);

            server.onVmDeath();

            assertEquals("nothing should arrive while events are held",
                    0, countVmDeaths(client.drainEvents()));
        }
    }

    /** Releasing delivers what was held rather than dropping it. */
    @Test
    public void releasingDeliversTheEventsThatWereHeld() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_HOLD_EVENTS, new byte[0]);
            server.onVmDeath();
            client.drainEvents();  // confirm nothing yet; held

            client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_RELEASE_EVENTS, new byte[0]);

            assertTrue("the held event should arrive on release",
                    countVmDeaths(client.drainEvents()) > 0);
        }
    }

    /** After releasing, events flow immediately again. */
    @Test
    public void eventsFlowAgainOnceReleased() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_HOLD_EVENTS, new byte[0]);
            client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_RELEASE_EVENTS, new byte[0]);

            server.onVmDeath();

            assertTrue("an event raised after release should not be queued",
                    countVmDeaths(client.drainEvents()) > 0);
        }
    }

    /**
     * A hold does not outlive the session that asked for it.
     *
     * <p>An IDE that detaches between {@code HoldEvents} and
     * {@code ReleaseEvents} left the hold in place, so the next attach queued
     * its own VM_START instead of sending it — and a later release delivered
     * events carrying the previous IDE's request ids to a debugger that never
     * asked for them.</p>
     */
    @Test
    public void aHoldDoesNotSurviveTheSessionThatSetIt() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient first = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            first.send(JdwpTestClient.CS_VIRTUAL_MACHINE, VM_HOLD_EVENTS, new byte[0]);
            server.onVmDeath();   // queued behind the hold
        }

        try (JdwpTestClient second = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onVmDeath();

            List<JdwpTestClient.Event> events = second.drainEvents();
            assertTrue("the new session's events should not be held",
                    countVmDeaths(events) > 0);
        }
    }

    private int countVmDeaths(List<JdwpTestClient.Event> events) {
        int count = 0;
        for (JdwpTestClient.Event e : events) {
            if (e.eventKind == EK_VM_DEATH) count++;
        }
        return count;
    }

    private void primeSymbols(JdwpServer server) throws Exception {
        server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                TABLE.getBytes(StandardCharsets.UTF_8))));
        server.onHello(1);
    }
}
