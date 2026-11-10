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
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.Tcp;
import com.codename1.impl.backend.WireBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/// One connection to a Kafka broker: requests in the non-flexible encoding
/// of the versions this transport uses, answered in order. Every version used
/// is one Kafka 2.1 through 4.x accept, so no version negotiation is needed.
final class KafkaConnection {
    static final int PRODUCE = 0;
    static final int FETCH = 1;
    static final int LIST_OFFSETS = 2;
    static final int METADATA = 3;
    static final int OFFSET_COMMIT = 8;
    static final int OFFSET_FETCH = 9;
    static final int FIND_COORDINATOR = 10;
    static final int JOIN_GROUP = 11;
    static final int HEARTBEAT = 12;
    static final int LEAVE_GROUP = 13;
    static final int SYNC_GROUP = 14;

    private static final byte[] CLIENT_ID = WireBuffer.utf8("cn1");
    private final WireBuffer wire;
    private final String address;
    private int correlation;

    private KafkaConnection(WireBuffer wire, String address) {
        this.wire = wire;
        this.address = address;
    }

    static KafkaConnection open(String host, int port, boolean tls, int timeoutMillis)
            throws IOException {
        Tcp tcp = Tcp.connect(host, port, timeoutMillis);
        boolean opened = false;
        try {
            if (tls) {
                tcp.startTls(host);
            }
            KafkaConnection c = new KafkaConnection(new WireBuffer(tcp, 65536), host + ":" + port);
            opened = true;
            return c;
        } finally {
            if (!opened) {
                tcp.close();
            }
        }
    }

    String address() {
        return address;
    }

    /// Sends one request and reads its response, positioned after the header.
    synchronized In call(int apiKey, int version, Out body) throws IOException {
        int id = ++correlation;
        wire.writeIntBE(2 + 2 + 4 + 2 + CLIENT_ID.length + body.length);
        wire.writeShortBE(apiKey);
        wire.writeShortBE(version);
        wire.writeIntBE(id);
        wire.writeShortBE(CLIENT_ID.length);
        wire.writeBytes(CLIENT_ID);
        wire.writeBytes(body.data, 0, body.length);
        wire.flush();
        int length = wire.readIntBE();
        if (length < 4) {
            throw new IOException("Kafka at " + address + " sent a malformed response");
        }
        In in = new In(wire.readFully(length));
        if (in.int32() != id) {
            throw new IOException("Kafka at " + address + " answered out of order");
        }
        return in;
    }

    void close() {
        wire.getConnection().close();
    }

    /// The message of a Kafka error code, for the ones this transport meets.
    static String error(int code) {
        switch (code) {
            case 1:
                return "OFFSET_OUT_OF_RANGE";
            case 2:
                return "CORRUPT_MESSAGE";
            case 3:
                return "UNKNOWN_TOPIC_OR_PARTITION";
            case 5:
                return "LEADER_NOT_AVAILABLE";
            case 6:
                return "NOT_LEADER_OR_FOLLOWER";
            case 7:
                return "REQUEST_TIMED_OUT";
            case 10:
                return "MESSAGE_TOO_LARGE";
            case 14:
                return "COORDINATOR_LOAD_IN_PROGRESS";
            case 15:
                return "COORDINATOR_NOT_AVAILABLE";
            case 16:
                return "NOT_COORDINATOR";
            case 17:
                return "INVALID_TOPIC_EXCEPTION";
            case 22:
                return "ILLEGAL_GENERATION";
            case 25:
                return "UNKNOWN_MEMBER_ID";
            case 27:
                return "REBALANCE_IN_PROGRESS";
            case 29:
                return "TOPIC_AUTHORIZATION_FAILED";
            case 30:
                return "GROUP_AUTHORIZATION_FAILED";
            default:
                return "error " + code;
        }
    }

    // --------------------------------------------------------------- records

    /// A record read from a batch.
    static final class Record {
        final long offset;
        final byte[] key;
        final byte[] value;
        final Map<String, byte[]> headers;

        Record(long offset, byte[] key, byte[] value, Map<String, byte[]> headers) {
            this.offset = offset;
            this.key = key;
            this.value = value;
            this.headers = headers;
        }
    }

    /// One record batch (magic 2, uncompressed) holding one record.
    static byte[] batch(byte[] key, byte[] value, Map<String, byte[]> headers, long now) {
        Out record = new Out();
        record.int8(0);
        record.varlong(0);
        record.varint(0);
        record.varbytes(key);
        record.varbytes(value);
        record.varint(headers.size());
        for (Map.Entry<String, byte[]> h : headers.entrySet()) {
            record.varbytes(WireBuffer.utf8(h.getKey()));
            record.varbytes(h.getValue());
        }
        Out tail = new Out();
        tail.int16(0);
        tail.int32(0);
        tail.int64(now);
        tail.int64(now);
        tail.int64(-1);
        tail.int16(-1);
        tail.int32(-1);
        tail.int32(1);
        tail.varint(record.length);
        tail.raw(record.data, 0, record.length);
        Out batch = new Out();
        batch.int64(0);
        batch.int32(4 + 1 + 4 + tail.length);
        batch.int32(-1);
        batch.int8(2);
        batch.int32(Crc32c.of(tail.data, 0, tail.length));
        batch.raw(tail.data, 0, tail.length);
        byte[] out = new byte[batch.length];
        System.arraycopy(batch.data, 0, out, 0, batch.length);
        return out;
    }

    /// The records of `set` at or after `from`, in offset order. A batch cut
    /// off at the end of the set -- the fetch size ended inside it -- is left
    /// for the next fetch; control batches carry no records of the
    /// application's.
    static void records(byte[] set, long from, List<Record> out) throws IOException {
        In in = new In(set);
        while (in.remaining() >= 12) {
            long base = in.int64();
            int length = in.int32();
            if (length < 0 || length > in.remaining()) {
                return;
            }
            In batch = new In(in.take(length));
            batch.int32();
            int magic = batch.int8();
            if (magic != 2) {
                throw new IOException("Kafka sent a message set of format " + magic
                        + ", which predates Kafka 0.11; upgrade the topic's format");
            }
            batch.int32();
            int attributes = batch.int16();
            batch.int32();
            batch.int64();
            batch.int64();
            batch.int64();
            batch.int16();
            batch.int32();
            int count = batch.int32();
            if ((attributes & 0x20) != 0) {
                continue;
            }
            int compression = attributes & 7;
            In records = batch;
            if (compression == 1) {
                records = new In(gunzip(batch.take(batch.remaining())));
            } else if (compression != 0) {
                throw new IOException("Kafka sent a batch compressed with "
                        + (compression == 2 ? "snappy" : compression == 3 ? "lz4" : "zstd")
                        + "; only gzip and uncompressed batches can be read");
            }
            for (int i = 0; i < count; i++) {
                int size = records.varint();
                In r = new In(records.take(size));
                r.int8();
                r.varlong();
                long offset = base + r.varint();
                byte[] key = r.varbytes();
                byte[] value = r.varbytes();
                int n = r.varint();
                Map<String, byte[]> headers = new java.util.LinkedHashMap<String, byte[]>();
                for (int h = 0; h < n; h++) {
                    byte[] name = r.varbytes();
                    byte[] v = r.varbytes();
                    if (name != null) {
                        headers.put(WireBuffer.fromUtf8(name), v);
                    }
                }
                if (offset >= from) {
                    out.add(new Record(offset, key, value, headers));
                }
            }
        }
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        InputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4);
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /// The partition Kafka's own default partitioner picks for `key`, so a
    /// keyed message lands where a Java client would have put it.
    static int partition(byte[] key, int partitions) {
        return (murmur2(key) & 0x7fffffff) % partitions;
    }

    static int murmur2(byte[] data) {
        int length = data.length;
        final int m = 0x5bd1e995;
        int h = 0x9747b28c ^ length;
        int words = length / 4;
        for (int i = 0; i < words; i++) {
            int at = i * 4;
            int k = (data[at] & 0xff) + ((data[at + 1] & 0xff) << 8)
                    + ((data[at + 2] & 0xff) << 16) + ((data[at + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> 24;
            k *= m;
            h *= m;
            h ^= k;
        }
        int tail = length & ~3;
        int left = length % 4;
        if (left == 3) {
            h ^= (data[tail + 2] & 0xff) << 16;
        }
        if (left >= 2) {
            h ^= (data[tail + 1] & 0xff) << 8;
        }
        if (left >= 1) {
            h ^= data[tail] & 0xff;
            h *= m;
        }
        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h;
    }

    /// CRC-32C, the checksum of a record batch.
    static final class Crc32c {
        private static final int[] TABLE = new int[256];

        static {
            for (int i = 0; i < 256; i++) {
                int c = i;
                for (int k = 0; k < 8; k++) {
                    c = (c & 1) != 0 ? (c >>> 1) ^ 0x82F63B78 : c >>> 1;
                }
                TABLE[i] = c;
            }
        }

        private Crc32c() {
        }

        static int of(byte[] data, int offset, int length) {
            int c = 0xffffffff;
            for (int i = offset; i < offset + length; i++) {
                c = TABLE[(c ^ data[i]) & 0xff] ^ (c >>> 8);
            }
            return ~c;
        }
    }

    // ------------------------------------------------------------- encoding

    /// A request body being written.
    static final class Out {
        byte[] data = new byte[256];
        int length;

        private void room(int n) {
            if (length + n > data.length) {
                byte[] bigger = new byte[Math.max(data.length * 2, length + n)];
                System.arraycopy(data, 0, bigger, 0, length);
                data = bigger;
            }
        }

        Out int8(int v) {
            room(1);
            data[length++] = (byte) v;
            return this;
        }

        Out int16(int v) {
            int8(v >> 8);
            return int8(v);
        }

        Out int32(int v) {
            int16(v >> 16);
            return int16(v);
        }

        Out int64(long v) {
            int32((int) (v >>> 32));
            return int32((int) v);
        }

        Out bool(boolean v) {
            return int8(v ? 1 : 0);
        }

        Out raw(byte[] b, int offset, int n) {
            room(n);
            System.arraycopy(b, offset, data, length, n);
            length += n;
            return this;
        }

        /// A string with an int16 length; null is length -1.
        Out string(String s) {
            if (s == null) {
                return int16(-1);
            }
            byte[] b = WireBuffer.utf8(s);
            int16(b.length);
            return raw(b, 0, b.length);
        }

        /// Bytes with an int32 length; null is length -1.
        Out bytes(byte[] b) {
            if (b == null) {
                return int32(-1);
            }
            int32(b.length);
            return raw(b, 0, b.length);
        }

        Out bytes(Out b) {
            int32(b.length);
            return raw(b.data, 0, b.length);
        }

        Out varint(int v) {
            return varlong(v);
        }

        /// A zigzag varint.
        Out varlong(long v) {
            long z = (v << 1) ^ (v >> 63);
            while ((z & ~0x7fL) != 0) {
                int8((int) ((z & 0x7f) | 0x80));
                z >>>= 7;
            }
            return int8((int) z);
        }

        Out varbytes(byte[] b) {
            if (b == null) {
                return varint(-1);
            }
            varint(b.length);
            return raw(b, 0, b.length);
        }
    }

    /// A response being read.
    static final class In {
        private final byte[] b;
        private int p;

        In(byte[] b) {
            this.b = b;
        }

        int remaining() {
            return b.length - p;
        }

        private void need(int n) throws IOException {
            if (n < 0 || p + n > b.length) {
                throw new IOException("Kafka sent a truncated response");
            }
        }

        int int8() throws IOException {
            need(1);
            return b[p++];
        }

        int int16() throws IOException {
            need(2);
            int v = (short) (((b[p] & 0xff) << 8) | (b[p + 1] & 0xff));
            p += 2;
            return v;
        }

        int int32() throws IOException {
            need(4);
            int v = ((b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16)
                    | ((b[p + 2] & 0xff) << 8) | (b[p + 3] & 0xff);
            p += 4;
            return v;
        }

        long int64() throws IOException {
            long hi = int32();
            return (hi << 32) | (int32() & 0xffffffffL);
        }

        boolean bool() throws IOException {
            return int8() != 0;
        }

        byte[] take(int n) throws IOException {
            need(n);
            byte[] out = new byte[n];
            System.arraycopy(b, p, out, 0, n);
            p += n;
            return out;
        }

        String string() throws IOException {
            int n = int16();
            return n < 0 ? null : WireBuffer.fromUtf8(take(n));
        }

        byte[] bytes() throws IOException {
            int n = int32();
            return n < 0 ? null : take(n);
        }

        long varlong() throws IOException {
            long z = 0;
            int shift = 0;
            while (true) {
                int v = int8();
                z |= (long) (v & 0x7f) << shift;
                if ((v & 0x80) == 0) {
                    break;
                }
                shift += 7;
                if (shift > 63) {
                    throw new IOException("Kafka sent a malformed varint");
                }
            }
            return (z >>> 1) ^ -(z & 1);
        }

        int varint() throws IOException {
            return (int) varlong();
        }

        byte[] varbytes() throws IOException {
            int n = varint();
            return n < 0 ? null : take(n);
        }
    }
}
