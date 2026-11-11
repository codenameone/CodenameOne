/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.messaging.amqp;

import com.codename1.backend.Tcp;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// One AMQP 0-9-1 connection with one channel: the framing, the handshake and
/// the few methods the transport uses.
///
/// Writes are serialized on the connection, so consumer threads acknowledge on
/// a connection whose frames a reader thread is reading. Heartbeats are turned
/// off in the tune: a consumer can be busy in a handler far longer than any
/// heartbeat interval, and only a thread of its own could answer for it. A
/// connection lost under a consumer is found by its reader instead, and the
/// broker puts back every message that was not acknowledged.
final class AmqpConnection {
    static final int METHOD = 1;
    static final int HEADER = 2;
    static final int BODY = 3;
    static final int HEARTBEAT = 8;
    private static final int FRAME_END = 0xCE;
    private static final int CHANNEL = 1;
    /// The largest frame this side asks for; the broker may ask for less.
    private static final int MAX_FRAME = 131072;

    static final int CONNECTION = 10;
    static final int CHANNEL_CLASS = 20;
    static final int QUEUE = 50;
    static final int BASIC = 60;
    static final int CONFIRM = 85;

    private final WireBuffer wire;
    private final Object writeLock = new Object();
    private int frameMax = MAX_FRAME;
    /// Set by whichever thread finds the connection gone; read by the others.
    private final AtomicBoolean broken = new AtomicBoolean();

    private AmqpConnection(WireBuffer wire) {
        this.wire = wire;
    }

    /// An open connection with channel 1 open, to the broker `url` names:
    /// `amqp[s]://user:password@host:port/vhost`, the virtual host
    /// percent-encoded (`%2F` is `/`, the default).
    static AmqpConnection open(String url, int timeoutMillis) throws IOException {
        Address a = Address.parse(url);
        Tcp tcp = Tcp.connect(a.host, a.port, timeoutMillis);
        boolean opened = false;
        try {
            if (a.tls) {
                tcp.startTls(a.host);
            }
            AmqpConnection c = new AmqpConnection(new WireBuffer(tcp));
            c.handshake(a);
            opened = true;
            return c;
        } finally {
            if (!opened) {
                tcp.close();
            }
        }
    }

    private void handshake(Address a) throws IOException {
        byte[] header = {'A', 'M', 'Q', 'P', 0, 0, 9, 1};
        wire.writeBytes(header);
        wire.flush();
        In start = expect(CONNECTION, 10);
        start.octet();
        start.octet();
        start.skipTable();
        String mechanisms = WireBuffer.fromUtf8(start.longstr());
        if (mechanisms.indexOf("PLAIN") < 0) {
            throw new IOException("The AMQP broker offers no PLAIN login (" + mechanisms + ")");
        }
        Out ok = new Out();
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("product", "Codename One");
        ok.table(props);
        ok.shortstr("PLAIN");
        ok.longstr(WireBuffer.utf8("\u0000" + a.user + "\u0000" + a.password));
        ok.shortstr("en_US");
        method(CONNECTION, 11, ok);
        wire.flush();
        In tune = expect(CONNECTION, 30, "log in");
        int channelMax = tune.shortU();
        int serverFrame = tune.int32();
        tune.shortU();
        frameMax = serverFrame == 0 ? MAX_FRAME : Math.min(serverFrame, MAX_FRAME);
        Out tuned = new Out();
        tuned.short16(channelMax);
        tuned.int32(frameMax);
        tuned.short16(0);
        method(CONNECTION, 31, tuned);
        Out open = new Out();
        open.shortstr(a.vhost);
        open.shortstr("");
        open.octet(0);
        method(CONNECTION, 40, open);
        wire.flush();
        expect(CONNECTION, 41, "open virtual host " + a.vhost);
        Out channel = new Out();
        channel.shortstr("");
        rpc(CHANNEL_CLASS, 10, channel, 11);
    }

    boolean isBroken() {
        return broken.get();
    }

    /// Writes one method frame; the caller flushes.
    void method(int classId, int methodId, Out args) throws IOException {
        synchronized (writeLock) {
            frame(METHOD, classId, methodId, args);
        }
    }

    private void frame(int type, int classId, int methodId, Out args) {
        int length = 4 + args.length;
        wire.writeByte(type);
        wire.writeShortBE(type == METHOD && classId == CONNECTION ? 0 : CHANNEL);
        wire.writeIntBE(length);
        wire.writeShortBE(classId);
        wire.writeShortBE(methodId);
        wire.writeBytes(args.data, 0, args.length);
        wire.writeByte(FRAME_END);
    }

    /// Sends a method and waits for its reply on a connection no reader
    /// thread reads; a close from the broker is thrown with its reason.
    In rpc(int classId, int methodId, Out args, int replyId) throws IOException {
        synchronized (writeLock) {
            frame(METHOD, classId, methodId, args);
            flushLocked();
        }
        return expect(classId, replyId, describe(classId, methodId));
    }

    private void flushLocked() throws IOException {
        try {
            wire.flush();
        } catch (IOException err) {
            broken.set(true);
            throw err;
        }
    }

    /// Publishes `body` to the default exchange, routed to `queue`.
    void publish(String queue, String contentType, Map<String, Object> headers, byte[] body)
            throws IOException {
        Out args = new Out();
        args.short16(0);
        args.shortstr("");
        args.shortstr(queue);
        args.octet(0);
        Out props = new Out();
        props.short16(BASIC);
        props.short16(0);
        props.int64(body.length);
        int flags = (1 << 13) | (1 << 12);
        if (contentType != null) {
            flags |= 1 << 15;
        }
        props.short16(flags);
        if (contentType != null) {
            props.shortstr(contentType);
        }
        props.table(headers);
        // Persistent, so a durable queue keeps it across a broker restart.
        props.octet(2);
        synchronized (writeLock) {
            frame(METHOD, BASIC, 40, args);
            wire.writeByte(HEADER);
            wire.writeShortBE(CHANNEL);
            wire.writeIntBE(props.length);
            wire.writeBytes(props.data, 0, props.length);
            wire.writeByte(FRAME_END);
            int max = frameMax - 8;
            for (int at = 0; at < body.length; at += max) {
                int n = Math.min(max, body.length - at);
                wire.writeByte(BODY);
                wire.writeShortBE(CHANNEL);
                wire.writeIntBE(n);
                wire.writeBytes(body, at, n);
                wire.writeByte(FRAME_END);
            }
            flushLocked();
        }
    }

    /// Acknowledges one delivery.
    void ack(long deliveryTag) throws IOException {
        Out args = new Out();
        args.int64(deliveryTag);
        args.octet(0);
        synchronized (writeLock) {
            frame(METHOD, BASIC, 80, args);
            flushLocked();
        }
    }

    /// Declares the durable queue `name` with `arguments` (null for none).
    void declare(String name, Map<String, Object> arguments) throws IOException {
        Out args = new Out();
        args.short16(0);
        args.shortstr(name);
        args.octet(1 << 1);
        args.table(arguments);
        rpc(QUEUE, 10, args, 11);
    }

    /// One frame. Only one thread reads a connection.
    Frame read() throws IOException {
        try {
            int type;
            byte[] payload;
            do {
                type = wire.read();
                // Every frame this side reads is on channel 0 or its one channel.
                wire.readShortBE();
                payload = wire.readFully(wire.readIntBE());
                if (wire.read() != FRAME_END) {
                    throw new IOException("The AMQP broker sent a malformed frame");
                }
            } while (type == HEARTBEAT);
            return new Frame(type, payload);
        } catch (IOException err) {
            broken.set(true);
            throw err;
        }
    }

    private In expect(int classId, int methodId) throws IOException {
        return expect(classId, methodId, describe(classId, methodId));
    }

    /// The next method frame, which must be `classId.methodId`; a close from
    /// the broker is thrown with its reason, and anything else is a protocol
    /// error.
    private In expect(int classId, int methodId, String doing) throws IOException {
        while (true) {
            Frame f = read();
            if (f.type != METHOD) {
                continue;
            }
            In in = new In(f.payload);
            int c = in.shortU();
            int m = in.shortU();
            if (c == classId && m == methodId) {
                return in;
            }
            closed(in, c, m, doing);
            if (c == BASIC && (m == 80 || m == 120)) {
                // A confirm arriving behind a reply is the caller's to read.
                continue;
            }
            broken.set(true);
            throw new IOException("The AMQP broker answered " + c + "." + m + " to " + doing);
        }
    }

    /// Throws the broker's reason when `c.m` is a channel or connection close,
    /// answering it first.
    void closed(In in, int c, int m, String doing) throws IOException {
        if (c == CONNECTION && m == 50 || c == CHANNEL_CLASS && m == 40) {
            int code = in.shortU();
            String text = in.shortstr();
            broken.set(true);
            try {
                synchronized (writeLock) {
                    frame(METHOD, c, m + 1, new Out());
                    wire.flush();
                }
            } catch (IOException ignored) {
                // Closing anyway.
            }
            throw new IOException("The AMQP broker refused to " + doing + ": " + code + " "
                    + text);
        }
    }

    /// Waits for the broker's confirm of the last publish.
    void awaitConfirm() throws IOException {
        while (true) {
            Frame f = read();
            if (f.type != METHOD) {
                continue;
            }
            In in = new In(f.payload);
            int c = in.shortU();
            int m = in.shortU();
            if (c == BASIC && m == 80) {
                return;
            }
            if (c == BASIC && m == 120) {
                throw new IOException("The AMQP broker did not accept the message");
            }
            closed(in, c, m, "publish");
        }
    }

    void close() {
        broken.set(true);
        try {
            Out args = new Out();
            args.short16(200);
            args.shortstr("bye");
            args.short16(0);
            args.short16(0);
            synchronized (writeLock) {
                frame(METHOD, CONNECTION, 50, args);
                wire.flush();
            }
        } catch (IOException ignored) {
            // The socket is closed below either way.
        }
        wire.getConnection().close();
    }

    private static String describe(int classId, int methodId) {
        switch (classId * 1000 + methodId) {
            case CONNECTION * 1000 + 10:
                return "start a connection";
            case CHANNEL_CLASS * 1000 + 10:
                return "open a channel";
            case QUEUE * 1000 + 10:
                return "declare a queue";
            case BASIC * 1000 + 10:
                return "set the prefetch";
            case BASIC * 1000 + 20:
                return "consume";
            case CONFIRM * 1000 + 10:
                return "turn on publisher confirms";
            default:
                return "run " + classId + "." + methodId;
        }
    }

    /// A frame read off the connection.
    static final class Frame {
        final int type;
        final byte[] payload;

        Frame(int type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    /// Method arguments being written.
    static final class Out {
        byte[] data = new byte[128];
        int length;

        private void room(int n) {
            if (length + n > data.length) {
                byte[] bigger = new byte[Math.max(data.length * 2, length + n)];
                System.arraycopy(data, 0, bigger, 0, length);
                data = bigger;
            }
        }

        void octet(int v) {
            room(1);
            data[length++] = (byte) v;
        }

        void short16(int v) {
            octet(v >> 8);
            octet(v);
        }

        void int32(int v) {
            short16(v >>> 16);
            short16(v);
        }

        void int64(long v) {
            int32((int) (v >>> 32));
            int32((int) v);
        }

        void bytes(byte[] b) {
            room(b.length);
            System.arraycopy(b, 0, data, length, b.length);
            length += b.length;
        }

        void shortstr(String s) {
            byte[] b = WireBuffer.utf8(s);
            if (b.length > 255) {
                throw new IllegalArgumentException("An AMQP name is at most 255 bytes: " + s);
            }
            octet(b.length);
            bytes(b);
        }

        void longstr(byte[] b) {
            int32(b.length);
            bytes(b);
        }

        /// A field table of string, int, long and boolean values.
        void table(Map<String, Object> values) {
            int at = length;
            int32(0);
            if (values != null) {
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    Object v = e.getValue();
                    if (v == null) {
                        continue;
                    }
                    shortstr(e.getKey());
                    if (v instanceof Integer) {
                        octet('I');
                        int32(((Integer) v).intValue());
                    } else if (v instanceof Long) {
                        octet('l');
                        int64(((Long) v).longValue());
                    } else if (v instanceof Boolean) {
                        octet('t');
                        octet(((Boolean) v).booleanValue() ? 1 : 0);
                    } else {
                        octet('S');
                        longstr(WireBuffer.utf8(v.toString()));
                    }
                }
            }
            int size = length - at - 4;
            data[at] = (byte) (size >>> 24);
            data[at + 1] = (byte) (size >>> 16);
            data[at + 2] = (byte) (size >>> 8);
            data[at + 3] = (byte) size;
        }
    }

    /// Method arguments being read.
    static final class In {
        private final byte[] b;
        private int p;

        In(byte[] b) {
            this.b = b;
        }

        private void need(int n) throws IOException {
            if (n < 0 || p + n > b.length) {
                throw new IOException("The AMQP broker sent a truncated frame");
            }
        }

        int octet() throws IOException {
            need(1);
            return b[p++] & 0xff;
        }

        int shortU() throws IOException {
            return (octet() << 8) | octet();
        }

        int int32() throws IOException {
            return (shortU() << 16) | shortU();
        }

        long int64() throws IOException {
            return ((long) int32() << 32) | (int32() & 0xffffffffL);
        }

        private byte[] take(int n) throws IOException {
            need(n);
            byte[] out = new byte[n];
            System.arraycopy(b, p, out, 0, n);
            p += n;
            return out;
        }

        String shortstr() throws IOException {
            return WireBuffer.fromUtf8(take(octet()));
        }

        byte[] longstr() throws IOException {
            return take(int32());
        }

        void skipTable() throws IOException {
            int n = int32();
            need(n);
            p += n;
        }

        /// A field table with every value as text; binary, array and nested
        /// table values are left out.
        Map<String, String> table() throws IOException {
            int n = int32();
            need(n);
            int end = p + n;
            Map<String, String> out = new LinkedHashMap<String, String>();
            while (p < end) {
                String key = shortstr();
                String value = value();
                if (value != null) {
                    out.put(key, value);
                }
            }
            return out;
        }

        private String value() throws IOException {
            int type = octet();
            switch (type) {
                case 't':
                    return octet() != 0 ? "true" : "false";
                case 'b':
                    return Integer.toString((byte) octet());
                case 'B':
                    return Integer.toString(octet());
                case 's':
                    return Integer.toString((short) shortU());
                case 'u':
                    return Integer.toString(shortU());
                case 'I':
                    return Integer.toString(int32());
                case 'i':
                    return Long.toString(int32() & 0xffffffffL);
                case 'l':
                case 'L':
                case 'T':
                    return Long.toString(int64());
                case 'f':
                    need(4);
                    p += 4;
                    return null;
                case 'd':
                    need(8);
                    p += 8;
                    return null;
                case 'D':
                    need(5);
                    p += 5;
                    return null;
                case 'S':
                    return WireBuffer.fromUtf8(longstr());
                case 'x':
                case 'A':
                case 'F':
                    skipTable();
                    return null;
                case 'V':
                    return null;
                default:
                    throw new IOException("The AMQP broker sent a field of unknown type " + type);
            }
        }
    }

    /// The parts of an AMQP URL.
    static final class Address {
        boolean tls;
        String host = "localhost";
        int port;
        String user = "guest";
        String password = "guest";
        String vhost = "/";

        static Address parse(String url) throws IOException {
            Address a = new Address();
            int scheme = url.indexOf("://");
            if (scheme < 0) {
                throw new IOException("Not an AMQP URL: " + url);
            }
            String s = url.substring(0, scheme);
            a.tls = "amqps".equalsIgnoreCase(s);
            if (!a.tls && !"amqp".equalsIgnoreCase(s)) {
                throw new IOException("Not an AMQP URL: " + url);
            }
            String rest = url.substring(scheme + 3);
            int q = rest.indexOf('?');
            if (q >= 0) {
                rest = rest.substring(0, q);
            }
            int slash = rest.indexOf('/');
            if (slash >= 0) {
                String path = rest.substring(slash + 1);
                if (path.length() > 0) {
                    a.vhost = decode(path);
                }
                rest = rest.substring(0, slash);
            }
            int at = rest.lastIndexOf('@');
            if (at >= 0) {
                String credentials = rest.substring(0, at);
                rest = rest.substring(at + 1);
                int colon = credentials.indexOf(':');
                a.user = decode(colon < 0 ? credentials : credentials.substring(0, colon));
                a.password = colon < 0 ? "" : decode(credentials.substring(colon + 1));
            }
            int colon = rest.lastIndexOf(':');
            a.port = a.tls ? 5671 : 5672;
            if (colon > 0 && rest.indexOf(']') < colon) {
                try {
                    a.port = Integer.parseInt(rest.substring(colon + 1));
                } catch (NumberFormatException err) {
                    throw new IOException("Not a port in " + url, err);
                }
                rest = rest.substring(0, colon);
            }
            if (rest.length() > 0) {
                a.host = rest.startsWith("[") && rest.endsWith("]")
                        ? rest.substring(1, rest.length() - 1) : rest;
            }
            return a;
        }

        /// Percent-decodes ASCII escapes.
        private static String decode(String s) {
            if (s.indexOf('%') < 0) {
                return s;
            }
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '%' && i + 2 < s.length()) {
                    int hi = Character.digit(s.charAt(i + 1), 16);
                    int lo = Character.digit(s.charAt(i + 2), 16);
                    if (hi >= 0 && lo >= 0) {
                        sb.append((char) (hi * 16 + lo));
                        i += 2;
                        continue;
                    }
                }
                sb.append(c);
            }
            return sb.toString();
        }
    }
}
