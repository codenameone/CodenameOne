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
package com.codename1.io.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Encodes and decodes MQTT 3.1.1 control packets on plain streams, so it is
/// independent of the transport. QoS 2 packets (PUBREC, PUBREL, PUBCOMP) and a
/// PUBLISH at QoS 2 are rejected with an [MqttException].
public final class MqttCodec {
    /// The largest remaining length the protocol can express, 268435455.
    public static final int MAX_REMAINING_LENGTH = 268435455;

    private MqttCodec() {
    }

    /// Encodes a remaining length as the protocol's variable length integer.
    ///
    /// #### Parameters
    ///
    /// - `value`: 0 to 268435455
    ///
    /// #### Returns
    ///
    /// one to four bytes
    public static byte[] encodeRemainingLength(int value) {
        if (value < 0 || value > MAX_REMAINING_LENGTH) {
            throw new IllegalArgumentException("remaining length out of range: " + value);
        }
        byte[] tmp = new byte[4];
        int n = 0;
        do {
            int digit = value % 128;
            value = value / 128;
            if (value > 0) {
                digit |= 0x80;
            }
            tmp[n++] = (byte) digit;
        } while (value > 0);
        byte[] out = new byte[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    /// Encodes a packet to its wire form.
    ///
    /// #### Parameters
    ///
    /// - `p`: the packet
    ///
    /// #### Returns
    ///
    /// the fixed header followed by the variable header and payload
    ///
    /// #### Throws
    ///
    /// `MqttException` when the packet cannot be represented
    public static byte[] encode(MqttPacket p) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int flags = 0;
        switch (p.getType()) {
            case MqttPacket.CONNECT:
                writeConnect(p, body);
                break;
            case MqttPacket.CONNACK:
                body.write(p.isSessionPresent() ? 1 : 0);
                body.write(p.getReturnCode());
                break;
            case MqttPacket.PUBLISH:
                flags = (p.isDup() ? 8 : 0) | (p.getQos() << 1) | (p.isRetain() ? 1 : 0);
                if (p.getQos() < 0 || p.getQos() > 1) {
                    throw new MqttException("Only QoS 0 and 1 are supported");
                }
                writeString(body, p.getTopic());
                if (p.getQos() > 0) {
                    writeId(body, p.getPacketId());
                }
                body.write(p.getPayload(), 0, p.getPayload().length);
                break;
            case MqttPacket.PUBACK:
            case MqttPacket.UNSUBACK:
                writeId(body, p.getPacketId());
                break;
            case MqttPacket.SUBSCRIBE:
                flags = 2;
                writeId(body, p.getPacketId());
                if (p.getFilters().length == 0 || p.getFilters().length != p.getQosList().length) {
                    throw new MqttException("SUBSCRIBE needs matching, non empty filters and QoS lists");
                }
                for (int i = 0; i < p.getFilters().length; i++) {
                    writeString(body, p.getFilters()[i]);
                    int q = p.getQosList()[i];
                    if (q < 0 || q > 1) {
                        throw new MqttException("Only QoS 0 and 1 are supported");
                    }
                    body.write(q);
                }
                break;
            case MqttPacket.SUBACK:
                writeId(body, p.getPacketId());
                for (int i = 0; i < p.getQosList().length; i++) {
                    body.write(p.getQosList()[i]);
                }
                break;
            case MqttPacket.UNSUBSCRIBE:
                flags = 2;
                writeId(body, p.getPacketId());
                if (p.getFilters().length == 0) {
                    throw new MqttException("UNSUBSCRIBE needs at least one filter");
                }
                for (int i = 0; i < p.getFilters().length; i++) {
                    writeString(body, p.getFilters()[i]);
                }
                break;
            case MqttPacket.PINGREQ:
            case MqttPacket.PINGRESP:
            case MqttPacket.DISCONNECT:
                break;
            default:
                throw new MqttException("Unsupported packet type " + p.getType());
        }
        byte[] b = body.toByteArray();
        byte[] rl = encodeRemainingLength(b.length);
        byte[] out = new byte[1 + rl.length + b.length];
        out[0] = (byte) ((p.getType() << 4) | flags);
        System.arraycopy(rl, 0, out, 1, rl.length);
        System.arraycopy(b, 0, out, 1 + rl.length, b.length);
        return out;
    }

    /// Encodes a packet and writes it to a stream, without flushing it.
    ///
    /// #### Parameters
    ///
    /// - `p`: the packet
    ///
    /// - `out`: the destination
    public static void write(MqttPacket p, OutputStream out) throws IOException {
        out.write(encode(p));
    }

    private static void writeConnect(MqttPacket p, ByteArrayOutputStream b) throws IOException {
        writeString(b, "MQTT");
        b.write(4);
        int f = 0;
        if (p.getUsername() != null) {
            f |= 0x80;
        }
        if (p.getPassword() != null) {
            if (p.getUsername() == null) {
                throw new MqttException("A password needs a user name");
            }
            f |= 0x40;
        }
        if (p.getWillTopic() != null) {
            f |= 0x04 | (p.getWillQos() << 3) | (p.isWillRetain() ? 0x20 : 0);
        }
        if (p.isCleanSession()) {
            f |= 0x02;
        }
        b.write(f);
        b.write(p.getKeepAlive() >> 8);
        b.write(p.getKeepAlive());
        String id = p.getClientId() == null ? "" : p.getClientId();
        if (id.length() == 0 && !p.isCleanSession()) {
            throw new MqttException("An empty client id needs a clean session");
        }
        writeString(b, id);
        if (p.getWillTopic() != null) {
            writeString(b, p.getWillTopic());
            writeBytes(b, p.getWillPayload() == null ? new byte[0] : p.getWillPayload());
        }
        if (p.getUsername() != null) {
            writeString(b, p.getUsername());
        }
        if (p.getPassword() != null) {
            writeBytes(b, p.getPassword());
        }
    }

    private static void writeId(ByteArrayOutputStream b, int id) throws IOException {
        if (id < 1 || id > 65535) {
            throw new MqttException("Packet id out of range: " + id);
        }
        b.write(id >> 8);
        b.write(id);
    }

    private static void writeString(ByteArrayOutputStream b, String s) throws IOException {
        if (s == null) {
            throw new MqttException("Missing string");
        }
        if (s.indexOf('\0') >= 0) {
            throw new MqttException("MQTT strings must not contain U+0000");
        }
        writeBytes(b, s.getBytes("UTF-8"));
    }

    private static void writeBytes(ByteArrayOutputStream b, byte[] data) throws IOException {
        if (data.length > 65535) {
            throw new MqttException("Field longer than 65535 bytes");
        }
        b.write(data.length >> 8);
        b.write(data.length);
        b.write(data, 0, data.length);
    }

    /// Measures the first packet in a buffer.
    ///
    /// #### Parameters
    ///
    /// - `buf`: received bytes
    ///
    /// - `off`: where the packet starts
    ///
    /// - `len`: how many bytes are available from `off`
    ///
    /// - `maxRemaining`: the largest remaining length to accept
    ///
    /// #### Returns
    ///
    /// the total size of the packet, fixed header included, or -1 when more
    /// bytes are needed
    ///
    /// #### Throws
    ///
    /// `MqttException` when the length is malformed or above the limit
    public static int packetLength(byte[] buf, int off, int len, int maxRemaining) throws IOException {
        int remaining = 0;
        int mult = 1;
        int i = 1;
        while (true) {
            if (i >= len) {
                return -1;
            }
            int d = buf[off + i] & 0xff;
            remaining += (d & 0x7f) * mult;
            i++;
            if ((d & 0x80) == 0) {
                break;
            }
            if (i > 4) {
                throw new MqttException("Malformed remaining length");
            }
            mult *= 128;
        }
        if (remaining > maxRemaining) {
            throw new MqttException("Packet of " + remaining + " bytes exceeds the limit of " + maxRemaining);
        }
        int total = i + remaining;
        return len >= total ? total : -1;
    }

    /// Reads one packet from a stream, blocking until it is complete.
    ///
    /// #### Parameters
    ///
    /// - `in`: the source
    ///
    /// - `maxRemaining`: the largest remaining length to accept
    ///
    /// #### Returns
    ///
    /// the packet
    ///
    /// #### Throws
    ///
    /// `EOFException` when the stream ends, `MqttException` for a malformed or
    /// oversized packet
    public static MqttPacket read(InputStream in, int maxRemaining) throws IOException {
        int first = in.read();
        if (first < 0) {
            throw new EOFException("End of stream before a packet");
        }
        int remaining = 0;
        int mult = 1;
        for (int i = 0; ; i++) {
            int d = in.read();
            if (d < 0) {
                throw new EOFException("End of stream in a packet header");
            }
            remaining += (d & 0x7f) * mult;
            if ((d & 0x80) == 0) {
                break;
            }
            if (i == 3) {
                throw new MqttException("Malformed remaining length");
            }
            mult *= 128;
        }
        if (remaining > maxRemaining) {
            throw new MqttException("Packet of " + remaining + " bytes exceeds the limit of " + maxRemaining);
        }
        byte[] body = new byte[remaining];
        int got = 0;
        while (got < remaining) {
            int r = in.read(body, got, remaining - got);
            if (r < 0) {
                throw new EOFException("End of stream in a packet body");
            }
            got += r;
        }
        return decode(first, body);
    }

    /// Decodes a packet from its first byte and its body.
    ///
    /// #### Parameters
    ///
    /// - `first`: the fixed header's first byte (type and flags)
    ///
    /// - `body`: the bytes after the remaining length
    ///
    /// #### Returns
    ///
    /// the packet
    public static MqttPacket decode(int first, byte[] body) throws IOException {
        int type = (first >> 4) & 0x0f;
        int flags = first & 0x0f;
        int[] pos = new int[] {0};
        switch (type) {
            case MqttPacket.CONNECT:
                expectFlags(flags, 0, type);
                return decodeConnect(body, pos);
            case MqttPacket.CONNACK:
                expectFlags(flags, 0, type);
                if (body.length != 2 || (body[0] & 0xfe) != 0) {
                    throw new MqttException("Malformed CONNACK");
                }
                return MqttPacket.connack(body[0] == 1, body[1] & 0xff);
            case MqttPacket.PUBLISH:
                return decodePublish(flags, body, pos);
            case MqttPacket.PUBACK:
                expectFlags(flags, 0, type);
                return MqttPacket.puback(readOnlyId(body));
            case MqttPacket.UNSUBACK:
                expectFlags(flags, 0, type);
                return MqttPacket.unsuback(readOnlyId(body));
            case MqttPacket.SUBSCRIBE:
                expectFlags(flags, 2, type);
                return decodeSubscribe(body, pos);
            case MqttPacket.SUBACK:
                expectFlags(flags, 0, type);
                int id = readId(body, pos);
                int[] codes = new int[body.length - 2];
                if (codes.length == 0) {
                    throw new MqttException("SUBACK without return codes");
                }
                for (int i = 0; i < codes.length; i++) {
                    codes[i] = body[2 + i] & 0xff;
                    if (codes[i] > 2 && codes[i] != 0x80) {
                        throw new MqttException("Bad SUBACK return code " + codes[i]);
                    }
                }
                return MqttPacket.suback(id, codes);
            case MqttPacket.UNSUBSCRIBE:
                expectFlags(flags, 2, type);
                int uid = readId(body, pos);
                java.util.ArrayList<String> fl = new java.util.ArrayList<String>();
                while (pos[0] < body.length) {
                    fl.add(readString(body, pos));
                }
                if (fl.isEmpty()) {
                    throw new MqttException("UNSUBSCRIBE without filters");
                }
                return MqttPacket.unsubscribe(uid, fl.toArray(new String[fl.size()]));
            case MqttPacket.PINGREQ:
            case MqttPacket.PINGRESP:
            case MqttPacket.DISCONNECT:
                expectFlags(flags, 0, type);
                if (body.length != 0) {
                    throw new MqttException("Packet type " + type + " must have no body");
                }
                return type == MqttPacket.PINGREQ ? MqttPacket.pingreq()
                        : type == MqttPacket.PINGRESP ? MqttPacket.pingresp() : MqttPacket.disconnect();
            case 5:
            case 6:
            case 7:
                throw new MqttException("QoS 2 packets are not supported (type " + type + ")");
            default:
                throw new MqttException("Unknown packet type " + type);
        }
    }

    private static void expectFlags(int flags, int expected, int type) throws MqttException {
        if (flags != expected) {
            throw new MqttException("Bad fixed header flags " + flags + " for packet type " + type);
        }
    }

    private static MqttPacket decodePublish(int flags, byte[] body, int[] pos) throws IOException {
        int qos = (flags >> 1) & 3;
        if (qos == 2) {
            throw new MqttException("QoS 2 is not supported");
        }
        if (qos == 3) {
            throw new MqttException("Invalid QoS 3");
        }
        String topic = readString(body, pos);
        int id = 0;
        if (qos > 0) {
            id = readId(body, pos);
        }
        byte[] payload = new byte[body.length - pos[0]];
        System.arraycopy(body, pos[0], payload, 0, payload.length);
        return MqttPacket.publish(topic, payload, qos, (flags & 1) != 0, (flags & 8) != 0, id);
    }

    private static MqttPacket decodeSubscribe(byte[] body, int[] pos) throws IOException {
        int id = readId(body, pos);
        java.util.ArrayList<String> filters = new java.util.ArrayList<String>();
        java.util.ArrayList<Integer> qos = new java.util.ArrayList<Integer>();
        while (pos[0] < body.length) {
            filters.add(readString(body, pos));
            if (pos[0] >= body.length) {
                throw new MqttException("SUBSCRIBE filter without QoS");
            }
            int q = body[pos[0]++] & 0xff;
            if (q > 1) {
                throw new MqttException("QoS " + q + " is not supported");
            }
            qos.add(Integer.valueOf(q));
        }
        if (filters.isEmpty()) {
            throw new MqttException("SUBSCRIBE without filters");
        }
        int[] q = new int[qos.size()];
        for (int i = 0; i < q.length; i++) {
            q[i] = qos.get(i).intValue();
        }
        return MqttPacket.subscribe(id, filters.toArray(new String[filters.size()]), q);
    }

    private static MqttPacket decodeConnect(byte[] body, int[] pos) throws IOException {
        String proto = readString(body, pos);
        if (!"MQTT".equals(proto)) {
            throw new MqttException("Unsupported protocol name " + proto);
        }
        if (pos[0] + 4 > body.length) {
            throw new MqttException("Truncated CONNECT");
        }
        int level = body[pos[0]++] & 0xff;
        if (level != 4) {
            throw new MqttException("Unsupported protocol level " + level);
        }
        int f = body[pos[0]++] & 0xff;
        if ((f & 1) != 0) {
            throw new MqttException("Reserved CONNECT flag set");
        }
        int keepAlive = ((body[pos[0]] & 0xff) << 8) | (body[pos[0] + 1] & 0xff);
        pos[0] += 2;
        String clientId = readString(body, pos);
        String willTopic = null;
        byte[] willPayload = null;
        int willQos = (f >> 3) & 3;
        boolean will = (f & 0x04) != 0;
        if (!will && (willQos != 0 || (f & 0x20) != 0)) {
            throw new MqttException("Will flags without a will");
        }
        if (willQos > 1) {
            throw new MqttException("QoS 2 is not supported");
        }
        if (will) {
            willTopic = readString(body, pos);
            willPayload = readBytes(body, pos);
        }
        String user = null;
        byte[] pass = null;
        if ((f & 0x80) != 0) {
            user = readString(body, pos);
        }
        if ((f & 0x40) != 0) {
            if (user == null) {
                throw new MqttException("Password flag without user name flag");
            }
            pass = readBytes(body, pos);
        }
        if (pos[0] != body.length) {
            throw new MqttException("Trailing bytes in CONNECT");
        }
        MqttPacket p = new MqttPacket(MqttPacket.CONNECT);
        p.setConnectFields(clientId, (f & 0x02) != 0, keepAlive, user, pass, willTopic, willPayload,
                willQos, (f & 0x20) != 0);
        return p;
    }

    private static int readOnlyId(byte[] body) throws IOException {
        int[] pos = new int[] {0};
        int id = readId(body, pos);
        if (pos[0] != body.length) {
            throw new MqttException("Trailing bytes after packet id");
        }
        return id;
    }

    private static int readId(byte[] body, int[] pos) throws IOException {
        if (pos[0] + 2 > body.length) {
            throw new MqttException("Truncated packet id");
        }
        int id = ((body[pos[0]] & 0xff) << 8) | (body[pos[0] + 1] & 0xff);
        pos[0] += 2;
        if (id == 0) {
            throw new MqttException("Packet id 0");
        }
        return id;
    }

    private static byte[] readBytes(byte[] body, int[] pos) throws IOException {
        if (pos[0] + 2 > body.length) {
            throw new MqttException("Truncated length prefix");
        }
        int n = ((body[pos[0]] & 0xff) << 8) | (body[pos[0] + 1] & 0xff);
        pos[0] += 2;
        if (pos[0] + n > body.length) {
            throw new MqttException("Truncated field");
        }
        byte[] out = new byte[n];
        System.arraycopy(body, pos[0], out, 0, n);
        pos[0] += n;
        return out;
    }

    private static String readString(byte[] body, int[] pos) throws IOException {
        String s = new String(readBytes(body, pos), "UTF-8");
        if (s.indexOf('\0') >= 0) {
            throw new MqttException("MQTT strings must not contain U+0000");
        }
        return s;
    }
}
