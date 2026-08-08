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

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Thread naming, end to end through a stand-in device.
 *
 * Resolving a name is three round trips — the thread object's class, its
 * {@code name} field, then that String's contents — and none of it runs
 * without something answering on the device port. The other thread tests drive
 * the proxy's listener callbacks directly and so never exercise the path at
 * all; this one speaks the wire protocol so the resolution and its cache are
 * actually covered.
 */
public class JdwpThreadNameTest {

    /** java.lang.Thread with a name field, plus a String class to resolve to. */
    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tjava_lang_Thread\tThread.java\t-1\tjava/lang/Thread\n"
          + "class\t1\tjava_lang_String\tString.java\t-1\tjava/lang/String\n"
          + "field\t0\t7\tname\tLjava/lang/String;\t1\n";

    private static final long THREAD_ID = 42;
    private static final long THREAD_OBJECT = 0x5000;
    private static final long NAME_STRING = 0x6000;

    private FakeDevice device;

    @After
    public void tearDown() {
        if (device != null) device.close();
    }

    @Test
    public void aThreadIsNamedFromItsThreadObject() throws Exception {
        try (Harness h = new Harness("EDT")) {
            // An IDE enumerates before it names, which is what tells the proxy
            // which java.lang.Thread each id belongs to.
            h.refreshThreadList();
            assertEquals("EDT", h.threadName(THREAD_ID));
        }
    }

    /**
     * A rename is picked up after the next thread-list refresh.
     *
     * <p>Naming costs three round trips, so the result is cached. Kept across
     * refreshes, a pool thread renamed per task would carry its first label
     * for the rest of its life.</p>
     */
    @Test
    public void aRenamedThreadIsRelabelledAfterTheNextRefresh() throws Exception {
        try (Harness h = new Harness("worker-1")) {
            h.refreshThreadList();
            assertEquals("worker-1", h.threadName(THREAD_ID));

            h.device.threadName = "worker-2";
            assertEquals("the cache holds until the list is refreshed",
                    "worker-1", h.threadName(THREAD_ID));

            h.refreshThreadList();
            assertEquals("worker-2", h.threadName(THREAD_ID));
        }
    }

    /** A thread with no Thread object still gets a usable label. */
    @Test
    public void aThreadWithNoThreadObjectFallsBack() throws Exception {
        try (Harness h = new Harness("ignored")) {
            h.device.threadObject = 0;
            h.refreshThreadList();
            assertEquals("Thread-" + THREAD_ID, h.threadName(THREAD_ID));
        }
    }

    /**
     * Object questions about a thread reach its java.lang.Thread, not the
     * thread id read as a pointer.
     *
     * <p>ParparVM thread ids are small integers and travel to the IDE as-is.
     * jdb asks {@code ObjectReference.ReferenceType} on them to render the
     * Threads panel, so forwarding the id to the device as an object pointer
     * both addresses nothing and — for an odd id — is indistinguishable from a
     * tagged int. Every odd-numbered thread showed as a java.lang.Integer.</p>
     */
    @Test
    public void objectQueriesAboutAThreadResolveToItsThreadObject() throws Exception {
        try (Harness h = new Harness("EDT")) {
            h.refreshThreadList();

            // ObjectReference.ReferenceType on the thread id.
            JdwpTestClient.Reply reply = h.client.send(
                    9 /* ObjectReference */, 1, JdwpTestClient.threadId(THREAD_ID));
            assertEquals(0, reply.errorCode);
            DataInputStream body = reply.stream();
            body.readByte();                       // refTypeTag
            long refType = body.readLong();

            // The fake device answers GET_OBJECT_CLASS with java.lang.Thread
            // (classId 0), which travels as JDWP reference id 1. Getting that
            // back means the thread object was asked about, not the raw id.
            assertEquals("the Thread object's class, not the id read as a pointer",
                    1L, refType);
            assertEquals("and the device was asked about the thread object",
                    THREAD_OBJECT, h.device.lastObjectClassQuery);
        }
    }

    // ---- harness -----------------------------------------------------------

    private final class Harness implements AutoCloseable {
        private final JdwpServer server;
        private final JdwpTestClient client;
        final FakeDevice device;

        Harness(String initialName) throws Exception {
            int jdwpPort = JdwpTestClient.freePort();
            int devicePort = JdwpTestClient.freePort();
            server = new JdwpServer(jdwpPort, devicePort);

            // The proxy has to be listening before the device dials in.
            DeviceConnection connection = new DeviceConnection(devicePort, server);
            server.setDevice(connection);
            Thread serving = new Thread(() -> {
                try { connection.acceptAndServe(); } catch (Exception ignore) { }
            }, "device-serve");
            serving.setDaemon(true);
            serving.start();

            device = new FakeDevice(devicePort, initialName);
            JdwpThreadNameTest.this.device = device;

            client = JdwpTestClient.attach(server, jdwpPort);
            device.connect();
            device.awaitHello();
        }

        String threadName(long tid) throws Exception {
            JdwpTestClient.Reply reply = client.send(
                    JdwpTestClient.CS_THREAD_REFERENCE, 1, JdwpTestClient.threadId(tid));
            assertEquals(0, reply.errorCode);
            DataInputStream body = reply.stream();
            byte[] utf8 = new byte[body.readInt()];
            body.readFully(utf8);
            return new String(utf8, StandardCharsets.UTF_8);
        }

        /** VirtualMachine.AllThreads, which round-trips CMD_GET_THREADS. */
        void refreshThreadList() throws Exception {
            assertEquals(0, client.send(JdwpTestClient.CS_VIRTUAL_MACHINE, 4, new byte[0]).errorCode);
        }

        @Override public void close() {
            client.close();
        }
    }

    /**
     * Answers the handful of commands naming needs, and nothing else. Runs on
     * its own thread because the proxy blocks waiting for each reply.
     */
    private static final class FakeDevice {
        volatile String threadName;
        volatile long threadObject = THREAD_OBJECT;
        volatile long lastObjectClassQuery = -1;
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final Thread pump;
        private final Object helloLock = new Object();
        private volatile boolean helloDone;
        private volatile boolean running = true;

        FakeDevice(int devicePort, String initialName) throws Exception {
            this.threadName = initialName;
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
            pump = new Thread(this::serve, "fake-device");
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
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(b);
                    d.writeInt(1);
                    d.writeLong(THREAD_ID);
                    d.writeByte(0);
                    d.writeLong(threadObject);
                    send(WireProtocol.EVT_THREAD_LIST, b.toByteArray());
                    return;
                }
                case WireProtocol.CMD_GET_OBJECT_CLASS: {
                    lastObjectClassQuery = ((long) readInt(p, 0) << 32)
                            | (readInt(p, 4) & 0xffffffffL);
                    // Thread object -> java.lang.Thread (classId 0).
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(b);
                    d.writeInt(0);
                    d.writeByte(0);
                    send(WireProtocol.EVT_OBJECT_CLASS, b.toByteArray());
                    return;
                }
                case WireProtocol.CMD_GET_OBJECT_FIELDS: {
                    // The one field asked for is name; answer with the String ref.
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(b);
                    d.writeInt(1);
                    d.writeByte('L');
                    d.writeLong(NAME_STRING);
                    send(WireProtocol.EVT_OBJECT_FIELDS, b.toByteArray());
                    return;
                }
                case WireProtocol.CMD_GET_STRING: {
                    send(WireProtocol.EVT_STRING_VALUE,
                            threadName.getBytes(StandardCharsets.UTF_8));
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

        private static int readInt(byte[] b, int off) {
            return ((b[off] & 0xff) << 24) | ((b[off + 1] & 0xff) << 16)
                 | ((b[off + 2] & 0xff) << 8) | (b[off + 3] & 0xff);
        }

        private static byte[] gzip(byte[] raw) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
                gz.write(raw);
            }
            return out.toByteArray();
        }

        void close() {
            running = false;
            try { socket.close(); } catch (IOException ignore) {}
        }
    }

    /** Unused, but keeps the port helper honest if the harness changes. */
    @SuppressWarnings("unused")
    private static int unusedPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
