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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * A debugger's side of a JDWP session, for tests that want to drive
 * {@link JdwpServer} the way an IDE does.
 *
 * Handles the accept loop, the handshake, request-id bookkeeping and the fact
 * that replies and asynchronous event packets arrive interleaved on the same
 * socket — so a test can say "send this command, read that reply" and "what
 * events did the server push".
 */
final class JdwpTestClient implements AutoCloseable {

    private static final byte[] HANDSHAKE = "JDWP-Handshake".getBytes(StandardCharsets.US_ASCII);

    // JDWP command sets used by the tests.
    static final int CS_VIRTUAL_MACHINE = 1;
    static final int CS_THREAD_REFERENCE = 11;
    static final int CS_THREAD_GROUP_REF = 12;
    static final int CS_EVENT_REQUEST = 15;

    static final int EK_BREAKPOINT = 2;
    static final int EK_CLASS_PREPARE = 8;

    static final int MOD_CLASS_MATCH = 5;
    static final int MOD_CLASS_EXCLUDE = 6;

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final List<Event> events = new ArrayList<>();
    private int nextId = 1;

    private JdwpTestClient(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Starts the server on {@code port} — which must be the port it was
     * constructed with — attaches, and completes the handshake.
     */
    static JdwpTestClient attach(JdwpServer server, int port) throws Exception {
        Thread serverThread = new Thread(() -> {
            try {
                server.acceptAndServe();
            } catch (Exception e) {
                // The socket closing at the end of a test is the normal exit.
            }
        }, "jdwp-server-test");
        serverThread.setDaemon(true);
        serverThread.start();

        JdwpTestClient client = new JdwpTestClient(connect(port));
        client.out.write(HANDSHAKE);
        client.out.flush();
        byte[] reply = new byte[HANDSHAKE.length];
        client.in.readFully(reply);
        assertEquals("JDWP-Handshake", new String(reply, StandardCharsets.US_ASCII));
        client.socket.setSoTimeout(5000);
        return client;
    }

    static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static Socket connect(int port) throws Exception {
        Exception last = null;
        for (int i = 0; i < 50; i++) {
            try {
                return new Socket("127.0.0.1", port);
            } catch (Exception e) {
                last = e;
                Thread.sleep(20);
            }
        }
        throw last;
    }

    /** Sends a command and returns its reply, buffering any events seen on the way. */
    Reply send(int commandSet, int command, byte[] payload) throws IOException {
        int id = nextId++;
        out.writeInt(11 + payload.length);
        out.writeInt(id);
        out.writeByte(0);
        out.writeByte(commandSet);
        out.writeByte(command);
        out.write(payload);
        out.flush();
        return readReply(id);
    }

    private Reply readReply(int requestId) throws IOException {
        while (true) {
            Packet packet = readPacket();
            if (packet.isReply) {
                if (packet.id == requestId) {
                    return new Reply(packet.errorCode, packet.body);
                }
            } else {
                events.addAll(Event.parseAll(packet.body));
            }
        }
    }

    /**
     * Drains events the server has already pushed. Reads until the socket goes
     * quiet, so a caller does not need to know how many to expect.
     */
    List<Event> drainEvents() throws IOException {
        socket.setSoTimeout(300);
        try {
            while (true) {
                Packet packet = readPacket();
                if (!packet.isReply) {
                    events.addAll(Event.parseAll(packet.body));
                }
            }
        } catch (IOException expected) {
            // Timeout: nothing more queued.
        } finally {
            socket.setSoTimeout(5000);
        }
        List<Event> drained = new ArrayList<>(events);
        events.clear();
        return drained;
    }

    private Packet readPacket() throws IOException {
        int length = in.readInt();
        int id = in.readInt();
        int flags = in.readUnsignedByte();
        Packet packet = new Packet();
        packet.id = id;
        packet.isReply = (flags & 0x80) != 0;
        if (packet.isReply) {
            packet.errorCode = in.readUnsignedShort();
        } else {
            in.readUnsignedByte(); // command set
            in.readUnsignedByte(); // command
        }
        packet.body = new byte[length - 11];
        in.readFully(packet.body);
        return packet;
    }

    @Override public void close() {
        try { socket.close(); } catch (IOException ignore) {}
    }

    // ---- payload builders --------------------------------------------------

    /** EventRequest.Set for a ClassPrepare with the given include/exclude patterns. */
    static byte[] classPrepareRequest(int suspendPolicy, String[] includes, String[] excludes) {
        return classPrepareRequest(suspendPolicy, includes, excludes, -1);
    }

    /** {@code onlyClassJdwpId} of -1 omits the ClassOnly modifier. */
    static byte[] classPrepareRequest(int suspendPolicy, String[] includes,
                                      String[] excludes, long onlyClassJdwpId) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(bytes);
        try {
            b.writeByte(EK_CLASS_PREPARE);
            b.writeByte(suspendPolicy);
            b.writeInt(includes.length + excludes.length + (onlyClassJdwpId >= 0 ? 1 : 0));
            if (onlyClassJdwpId >= 0) {
                b.writeByte(4);   // ClassOnly
                b.writeLong(onlyClassJdwpId);
            }
            for (String pattern : includes) {
                b.writeByte(MOD_CLASS_MATCH);
                writeString(b, pattern);
            }
            for (String pattern : excludes) {
                b.writeByte(MOD_CLASS_EXCLUDE);
                writeString(b, pattern);
            }
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return bytes.toByteArray();
    }

    /**
     * EventRequest.Set for a line breakpoint, carrying the one LocationOnly
     * modifier an IDE sends: typeTag(1) + classID(8) + methodID(8) +
     * codeIndex(8), where ParparVM's "code index" is the source line.
     */
    static byte[] lineBreakpointRequest(long classId, long methodId, long line) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(bytes);
        try {
            b.writeByte(EK_BREAKPOINT);
            b.writeByte(2);   // SUSPEND_ALL
            b.writeInt(1);    // one modifier
            b.writeByte(7);   // LocationOnly
            b.writeByte(1);   // refTypeTag CLASS
            b.writeLong(classId);
            b.writeLong(methodId);
            b.writeLong(line);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return bytes.toByteArray();
    }

    static byte[] threadId(long tid) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream b = new DataOutputStream(bytes);
        try {
            b.writeLong(tid);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return bytes.toByteArray();
    }

    private static void writeString(DataOutputStream b, String s) throws IOException {
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        b.writeInt(utf8.length);
        b.write(utf8);
    }

    // ---- packet types ------------------------------------------------------

    private static final class Packet {
        int id;
        boolean isReply;
        int errorCode;
        byte[] body;
    }

    static final class Reply {
        final int errorCode;
        final byte[] body;

        Reply(int errorCode, byte[] body) {
            this.errorCode = errorCode;
            this.body = body;
        }

        DataInputStream stream() {
            return new DataInputStream(new ByteArrayInputStream(body));
        }
    }

    /** A composite event packet, flattened to its first (and for us only) event. */
    static final class Event {
        final int suspendPolicy;
        final int eventKind;
        final int requestId;
        final byte[] rest;

        private Event(int suspendPolicy, int eventKind, int requestId, byte[] rest) {
            this.suspendPolicy = suspendPolicy;
            this.eventKind = eventKind;
            this.requestId = requestId;
            this.rest = rest;
        }

        /**
         * Splits a composite packet into its events.
         *
         * A composite can carry more than one — a VM death matching several
         * registered requests, for instance — so each event's payload has to
         * be consumed by kind to find where the next one starts.
         */
        static List<Event> parseAll(byte[] body) {
            DataInputStream b = new DataInputStream(new ByteArrayInputStream(body));
            List<Event> events = new ArrayList<>();
            try {
                int suspendPolicy = b.readUnsignedByte();
                int count = b.readInt();
                for (int i = 0; i < count; i++) {
                    int eventKind = b.readUnsignedByte();
                    int requestId = b.readInt();
                    events.add(new Event(suspendPolicy, eventKind, requestId,
                            readEventPayload(b, eventKind)));
                }
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
            return events;
        }

        /** Consumes the kind-specific tail of one event, returning its bytes. */
        private static byte[] readEventPayload(DataInputStream b, int eventKind)
                throws IOException {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(out);
            switch (eventKind) {
                case 99:  // VM_DEATH — no payload
                    break;
                case 90:  // VM_START — thread
                    w.writeLong(b.readLong());
                    break;
                case 1:   // SINGLE_STEP
                case 2: { // BREAKPOINT — thread + location
                    w.writeLong(b.readLong());
                    w.writeByte(b.readUnsignedByte());
                    w.writeLong(b.readLong());
                    w.writeLong(b.readLong());
                    w.writeLong(b.readLong());
                    break;
                }
                case 8: { // CLASS_PREPARE — thread, tag, typeID, signature, status
                    w.writeLong(b.readLong());
                    w.writeByte(b.readUnsignedByte());
                    w.writeLong(b.readLong());
                    int len = b.readInt();
                    byte[] sig = new byte[len];
                    b.readFully(sig);
                    w.writeInt(len);
                    w.write(sig);
                    w.writeInt(b.readInt());
                    break;
                }
                default:
                    throw new IOException("test client cannot size event kind " + eventKind);
            }
            return out.toByteArray();
        }

        /**
         * The class signature carried by a ClassPrepare event, which follows
         * thread(8), refTypeTag(1) and typeID(8).
         */
        String classPrepareSignature() {
            DataInputStream b = new DataInputStream(new ByteArrayInputStream(rest));
            try {
                b.readLong();          // thread
                b.readUnsignedByte();  // refTypeTag
                b.readLong();          // typeID
                byte[] utf8 = new byte[b.readInt()];
                b.readFully(utf8);
                return new String(utf8, StandardCharsets.UTF_8);
            } catch (IOException impossible) {
                throw new AssertionError(impossible);
            }
        }

        @Override public String toString() {
            return "Event{kind=" + eventKind + ", rid=" + requestId
                    + ", suspendPolicy=" + suspendPolicy + "}";
        }
    }
}
