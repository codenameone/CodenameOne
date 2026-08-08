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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A thread that stops while the thread list is in flight still appears in it.
 *
 * <p>The device samples its thread list at the moment the request reaches it.
 * A thread that stops after that is described by a reply that predates it, and
 * rebuilding the proxy's view from that reply dropped it — out of the very
 * {@code AllThreads} the IDE issues on being told the thread stopped. The
 * suspension is the newer fact, so it wins.</p>
 *
 * <p>The opposite case has to keep working: a thread that stopped, was
 * reported, and then exited must leave the list rather than sitting in the
 * IDE's thread panel for the rest of the session. What separates them is
 * whether the suspension is newer than the request, not the suspension alone —
 * see {@link JdwpThreadListTest#aThreadThatStoppedAndThenDiedDoesNotLinger}.</p>
 */
public class JdwpThreadListRaceTest {

    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tjava_lang_Thread\tThread.java\t-1\tjava/lang/Thread\n"
          + "method\t0\t0\trun\t()V\t0\n"
          + "line\t0\t12\n";

    private static final long ESTABLISHED = 7;
    private static final long LATE_STOPPER = 21;

    /**
     * The device answers with a list that does not mention the late thread,
     * having reported that thread's breakpoint first — the order the proxy
     * sees when a thread stops just after the request goes out.
     */
    @Test
    public void aThreadThatStopsWhileTheListIsInFlightSurvivesTheSnapshot() throws Exception {
        try (Fixture f = new Fixture(true)) {
            List<Long> threads = f.allThreads();

            assertTrue("the thread that just stopped should be listed: " + threads,
                    threads.contains(LATE_STOPPER));
            assertTrue("the established thread should still be listed: " + threads,
                    threads.contains(ESTABLISHED));
        }
    }

    /**
     * A thread that stopped before the request went out and is absent from the
     * answer really has gone, and is dropped.
     *
     * <p>This is the case that keeps the rule honest. Retaining every suspended
     * thread a snapshot omits would be simpler and would satisfy the test
     * above, at the cost of a dead row that never leaves the IDE's thread
     * panel.</p>
     */
    @Test
    public void aThreadThatStoppedBeforeTheRequestAndIsAbsentIsDropped() throws Exception {
        try (Fixture f = new Fixture(false)) {
            f.allThreads();   // one refresh completes first
            // Stops after that refresh and before the next one is asked for.
            f.server.onBreakpointHit(LATE_STOPPER, 0, 12);

            List<Long> threads = f.allThreads();

            assertFalse("a thread the device no longer reports should go: " + threads,
                    threads.contains(LATE_STOPPER));
        }
    }

    /**
     * A device that never answers the enumeration still lists the threads
     * events have revealed — an older build whose runtime does not send
     * {@code EVT_THREAD_LIST}.
     *
     * <p>Characterisation, not a regression test: it passes whether or not
     * {@code refreshed} distinguishes "a snapshot arrived" from "the request is
     * no longer outstanding". The fallback it guards cannot currently
     * contribute an id, because {@code knownThreads} only ever gains one
     * alongside {@code deviceThreads} and is pruned to a subset of it on every
     * snapshot. The distinction is still worth drawing — the flag otherwise
     * reports a silent device as a successful refresh — but nothing observable
     * turns on it until that invariant changes, and this test is here to catch
     * the day a timeout starts clearing the thread map instead.</p>
     */
    @Test
    public void aDeviceThatNeverAnswersFallsBackToThreadsSeenInEvents() throws Exception {
        try (Fixture f = new Fixture(false, true)) {
            // Primed here rather than awaited from the device: this device is
            // deliberately unresponsive, and the breakpoint below needs them.
            f.server.onSymbols(SymbolTable.load(new java.io.ByteArrayInputStream(
                    TABLE.getBytes(StandardCharsets.UTF_8))));
            f.server.onBreakpointHit(LATE_STOPPER, 0, 12);

            List<Long> threads = f.allThreads();

            assertTrue("the thread an event revealed should still be listed: " + threads,
                    threads.contains(LATE_STOPPER));
        }
    }

    // ---- fixture -----------------------------------------------------------

    private final class Fixture implements AutoCloseable {
        final JdwpServer server;
        private final JdwpTestClient client;
        private final RacingDevice device;

        Fixture(boolean stopDuringRequest) throws Exception {
            this(stopDuringRequest, false);
        }

        Fixture(boolean stopDuringRequest, boolean silent) throws Exception {
            int jdwpPort = JdwpTestClient.freePort();
            int devicePort = JdwpTestClient.freePort();
            server = new JdwpServer(jdwpPort, devicePort);

            DeviceConnection connection = new DeviceConnection(devicePort, server);
            server.setDevice(connection);
            Thread serving = new Thread(() -> {
                try { connection.acceptAndServe(); } catch (Exception ignore) { }
            }, "device-serve");
            serving.setDaemon(true);
            serving.start();

            device = new RacingDevice(devicePort, server, stopDuringRequest, silent);
            client = JdwpTestClient.attach(server, jdwpPort);
            device.connect();
            device.awaitHello();
        }

        /** VirtualMachine.AllThreads, which round-trips CMD_GET_THREADS. */
        List<Long> allThreads() throws Exception {
            JdwpTestClient.Reply reply =
                    client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 4, new byte[0]);
            DataInputStream body = reply.stream();
            int count = body.readInt();
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(JdwpTestClient.fromJdwpThread(body.readLong()));
            }
            return ids;
        }

        @Override public void close() {
            client.close();
            device.close();
        }
    }

    /**
     * Answers the thread list with a snapshot that omits {@link #LATE_STOPPER},
     * optionally reporting that thread's breakpoint first so the proxy learns
     * of the stop before the stale list arrives. Runs on its own thread: the
     * proxy blocks waiting for each reply.
     */
    private static final class RacingDevice {
        private final JdwpServer server;
        private final boolean stopDuringRequest;
        /** Never answers CMD_GET_THREADS, like a runtime that predates it. */
        private final boolean silent;
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final Thread pump;
        private final Object helloLock = new Object();
        private volatile boolean helloDone;
        private volatile boolean running = true;

        RacingDevice(int devicePort, JdwpServer server, boolean stopDuringRequest,
                     boolean silent) throws Exception {
            this.server = server;
            this.stopDuringRequest = stopDuringRequest;
            this.silent = silent;
            Socket connected = null;
            for (int i = 0; i < 100 && connected == null; i++) {
                try {
                    connected = new Socket("127.0.0.1", devicePort);
                } catch (IOException retry) {
                    Thread.sleep(20);
                }
            }
            if (connected == null) throw new IllegalStateException("proxy never listened");
            socket = connected;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            pump = new Thread(this::serve, "racing-device");
            pump.setDaemon(true);
        }

        void connect() throws Exception {
            send(WireProtocol.EVT_HELLO, new byte[] { 0, 0, 1 });
            pump.start();
        }

        void awaitHello() throws Exception {
            synchronized (helloLock) {
                long deadline = System.currentTimeMillis() + 5000;
                while (!helloDone && System.currentTimeMillis() < deadline) {
                    helloLock.wait(deadline - System.currentTimeMillis());
                }
            }
        }

        private void serve() {
            try {
                while (running) {
                    int len = in.readInt();
                    int cmd = in.readUnsignedByte();
                    byte[] payload = new byte[len];
                    in.readFully(payload);
                    handle(cmd, payload);
                }
            } catch (Exception done) {
                // Socket closed at the end of a test.
            }
        }

        private void handle(int cmd, byte[] p) throws Exception {
            switch (cmd) {
                case WireProtocol.CMD_GET_SYMBOLS: {
                    byte[] gz = gzip(TABLE.getBytes(StandardCharsets.UTF_8));
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(b);
                    d.writeInt(gz.length);
                    d.writeInt(0);
                    d.writeInt(gz.length);
                    d.write(gz);
                    send(WireProtocol.EVT_SYMBOLS, b.toByteArray());
                    synchronized (helloLock) { helloDone = true; helloLock.notifyAll(); }
                    return;
                }
                case WireProtocol.CMD_GET_THREADS: {
                    if (silent) return;   // an older runtime simply ignores it
                    if (stopDuringRequest) {
                        // The stop reaches the proxy before the list it raced.
                        server.onBreakpointHit(LATE_STOPPER, 0, 12);
                    }
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(b);
                    d.writeInt(1);
                    d.writeLong(ESTABLISHED);
                    d.writeByte(0);
                    d.writeLong(0);
                    send(WireProtocol.EVT_THREAD_LIST, b.toByteArray());
                    return;
                }
                default:
                    send(WireProtocol.EVT_REPLY_STATUS, new byte[0]);
            }
        }

        private synchronized void send(int code, byte[] payload) throws IOException {
            out.writeInt(payload.length);
            out.writeByte(code);
            out.write(payload);
            out.flush();
        }

        private static byte[] gzip(byte[] raw) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
                gz.write(raw);
            }
            return bytes.toByteArray();
        }

        void close() {
            running = false;
            try { socket.close(); } catch (IOException ignore) { }
        }
    }
}
