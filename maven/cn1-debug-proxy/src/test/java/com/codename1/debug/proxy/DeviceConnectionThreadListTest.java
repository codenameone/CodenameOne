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
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Decoding of the device's thread-list event.
 *
 * The device dials out to the proxy, so these drive a real socket: the test
 * plays the app, writing framed events at {@link DeviceConnection} and checking
 * what reaches the listener.
 */
public class DeviceConnectionThreadListTest {

    @Test
    public void aThreadListIsDecodedIntoIdsFlagsAndThreadObjects() throws Exception {
        Recorder recorder = new Recorder();
        try (Harness harness = new Harness(recorder)) {
            harness.write(WireProtocol.EVT_THREAD_LIST, threadList(
                    new long[] { 1, 42 },
                    new boolean[] { true, false },
                    new long[] { 0x1000, 0 }));

            assertTrue("listener should have been called", recorder.threads.await(2, TimeUnit.SECONDS));
            assertArrayEquals(new long[] { 1, 42 }, recorder.threadIds);
            assertEquals(Arrays.asList(true, false), asList(recorder.suspended));
            assertArrayEquals(new long[] { 0x1000, 0 }, recorder.threadObjects);
        }
    }

    /** An empty list is a legitimate answer, not a malformed frame. */
    @Test
    public void anEmptyThreadListIsDeliveredAsAnEmptyList() throws Exception {
        Recorder recorder = new Recorder();
        try (Harness harness = new Harness(recorder)) {
            harness.write(WireProtocol.EVT_THREAD_LIST,
                    threadList(new long[0], new boolean[0], new long[0]));

            assertTrue(recorder.threads.await(2, TimeUnit.SECONDS));
            assertEquals(0, recorder.threadIds.length);
            assertEquals("an empty list is not an unknown event", 0, recorder.unknownEvents.size());
        }
    }

    /**
     * A frame whose declared count outruns its payload must be reported as
     * unknown rather than read off the end of the buffer.
     */
    @Test
    public void aTruncatedThreadListIsRejectedInsteadOfOverrunning() throws Exception {
        Recorder recorder = new Recorder();
        try (Harness harness = new Harness(recorder)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream b = new DataOutputStream(bytes);
            b.writeInt(9);              // claims nine threads...
            b.writeLong(1);             // ...and carries most of one
            harness.write(WireProtocol.EVT_THREAD_LIST, bytes.toByteArray());

            assertTrue(recorder.unknown.await(2, TimeUnit.SECONDS));
            assertEquals(1, recorder.unknownEvents.size());
            assertEquals(WireProtocol.EVT_THREAD_LIST, (int) recorder.unknownEvents.get(0));
            assertEquals("no partial list should have been delivered", 0, recorder.threads.getCount(), 1);
        }
    }

    /** A negative count is the same kind of malformed frame. */
    @Test
    public void aNegativeThreadCountIsRejected() throws Exception {
        Recorder recorder = new Recorder();
        try (Harness harness = new Harness(recorder)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream b = new DataOutputStream(bytes);
            b.writeInt(-1);
            harness.write(WireProtocol.EVT_THREAD_LIST, bytes.toByteArray());

            assertTrue(recorder.unknown.await(2, TimeUnit.SECONDS));
            assertEquals(1, recorder.unknownEvents.size());
        }
    }

    /** The proxy asks for the list with a payload-free command. */
    @Test
    public void getThreadsSendsTheEnumerationCommand() throws Exception {
        Recorder recorder = new Recorder();
        try (Harness harness = new Harness(recorder)) {
            harness.connection.getThreads();

            assertEquals(0, harness.readCommandLength());
            assertEquals(WireProtocol.CMD_GET_THREADS, harness.readCommandCode());
        }
    }

    // ---- helpers -----------------------------------------------------------

    private static byte[] threadList(long[] ids, boolean[] suspended, long[] objects) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(bytes);
        try {
            b.writeInt(ids.length);
            for (int i = 0; i < ids.length; i++) {
                b.writeLong(ids[i]);
                b.writeByte(suspended[i] ? 0x01 : 0x00);
                b.writeLong(objects[i]);
            }
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return bytes.toByteArray();
    }

    private static List<Boolean> asList(boolean[] values) {
        List<Boolean> out = new ArrayList<>();
        for (boolean v : values) out.add(v);
        return out;
    }

    /** A DeviceConnection with a socket on the other end playing the device. */
    private static final class Harness implements AutoCloseable {
        final DeviceConnection connection;
        private final Socket device;
        private final DataOutputStream out;
        private final java.io.DataInputStream in;
        private final Thread serving;

        private final CountDownLatch ready = new CountDownLatch(1);

        Harness(DeviceConnection.DeviceListener listener) throws Exception {
            int port;
            try (ServerSocket probe = new ServerSocket(0)) {
                port = probe.getLocalPort();
            }
            connection = new DeviceConnection(port, new ReadyGate(listener, ready));
            serving = new Thread(() -> {
                try {
                    connection.acceptAndServe();
                } catch (Exception e) {
                    // Closing the socket at the end of a test is the normal exit.
                }
            }, "device-connection-test");
            serving.setDaemon(true);
            serving.start();

            Socket connected = null;
            for (int i = 0; i < 50 && connected == null; i++) {
                try {
                    connected = new Socket("127.0.0.1", port);
                } catch (IOException retry) {
                    Thread.sleep(20);
                }
            }
            if (connected == null) {
                throw new IllegalStateException("device could not dial in to the proxy");
            }
            device = connected;
            device.setSoTimeout(2000);
            out = new DataOutputStream(device.getOutputStream());
            in = new java.io.DataInputStream(device.getInputStream());

            // Connecting the socket is not enough: the proxy assigns its
            // streams after accept returns, and sending a command before that
            // fails with "device not connected". A round-trip through the read
            // loop proves both ends are up.
            write(WireProtocol.EVT_REPLY_STATUS, new byte[0]);
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the proxy never started reading from the device");
            }
        }

        void write(int code, byte[] payload) throws IOException {
            out.writeInt(payload.length);
            out.writeByte(code);
            out.write(payload);
            out.flush();
        }

        int readCommandLength() throws IOException {
            return in.readInt();
        }

        int readCommandCode() throws IOException {
            return in.readUnsignedByte();
        }

        @Override public void close() {
            try { device.close(); } catch (IOException ignore) {}
            connection.close();
        }
    }

    /**
     * Passes everything through, and signals the first REPLY_STATUS so the
     * harness knows the proxy's read loop is running.
     */
    private static final class ReadyGate extends NoOpDeviceListener {
        private final DeviceConnection.DeviceListener delegate;
        private final CountDownLatch ready;

        ReadyGate(DeviceConnection.DeviceListener delegate, CountDownLatch ready) {
            this.delegate = delegate;
            this.ready = ready;
        }

        @Override public void onThreads(long[] ids, boolean[] suspended, long[] objects) {
            delegate.onThreads(ids, suspended, objects);
        }

        @Override public void onUnknownEvent(int code, byte[] payload) {
            delegate.onUnknownEvent(code, payload);
        }

        @Override public void onReplyStatus() {
            ready.countDown();
            delegate.onReplyStatus();
        }
    }

    /** Captures the one callback each test cares about. */
    private static final class Recorder extends NoOpDeviceListener {
        final CountDownLatch threads = new CountDownLatch(1);
        final CountDownLatch unknown = new CountDownLatch(1);
        final List<Integer> unknownEvents = new ArrayList<>();
        volatile long[] threadIds = new long[0];
        volatile boolean[] suspended = new boolean[0];
        volatile long[] threadObjects = new long[0];

        @Override public void onThreads(long[] ids, boolean[] suspendedFlags, long[] objects) {
            this.threadIds = ids;
            this.suspended = suspendedFlags;
            this.threadObjects = objects;
            threads.countDown();
        }

        @Override public void onUnknownEvent(int code, byte[] payload) {
            unknownEvents.add(code);
            unknown.countDown();
        }
    }
}
