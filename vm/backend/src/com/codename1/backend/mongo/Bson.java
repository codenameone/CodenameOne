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
package com.codename1.backend.mongo;

import com.codename1.impl.backend.WireBuffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// BSON, the binary document format MongoDB stores and speaks.
///
/// | Java | BSON |
/// |---|---|
/// | `Double`, `Float` | double |
/// | `String` | string |
/// | `Map` | embedded document (key order kept) |
/// | `List`, `Object[]` | array |
/// | `byte[]` | binary, generic subtype |
/// | [ObjectId] | ObjectId |
/// | `Boolean` | boolean |
/// | `Date` | UTC datetime |
/// | `null` | null |
/// | `Integer`, `Short`, `Byte` | int32 |
/// | `Long` | int64 |
///
/// Decoding reads documents into `LinkedHashMap`, arrays into `ArrayList`, a
/// timestamp as its `Long` and a Decimal128 as its sixteen raw bytes in a
/// [Decimal128Bytes]; other legacy types are refused.
public final class Bson {
    /// The largest document accepted from a server: MongoDB's own limit of
    /// 16 MiB, plus the 16 KiB of headroom it allows a command reply.
    static final int MAX_DOCUMENT = 16 * 1024 * 1024 + 16 * 1024;

    private Bson() {
    }

    /// A Decimal128 value, kept as the sixteen little-endian bytes the server sent:
    /// this runtime has no `BigDecimal` to decode it into.
    public static final class Decimal128Bytes {
        private final byte[] bytes;

        /// The value in these sixteen bytes.
        public Decimal128Bytes(byte[] bytes) {
            this.bytes = new byte[16];
            System.arraycopy(bytes, 0, this.bytes, 0, 16);
        }

        /// A copy of the sixteen bytes.
        public byte[] toByteArray() {
            byte[] out = new byte[16];
            System.arraycopy(bytes, 0, out, 0, 16);
            return out;
        }
    }

    /// One document as BSON.
    public static byte[] encode(Map document) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeDocument(out, document, 0);
        return out.toByteArray();
    }

    private static void writeDocument(ByteArrayOutputStream out, Map doc, int depth) {
        if (depth > 100) {
            throw new IllegalArgumentException("A document nested over 100 levels deep");
        }
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        Iterator it = doc.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            writeElement(body, String.valueOf(e.getKey()), e.getValue(), depth);
        }
        writeInt(out, body.size() + 5);
        byte[] b = body.toByteArray();
        out.write(b, 0, b.length);
        out.write(0);
    }

    private static void writeArray(ByteArrayOutputStream out, List list, int depth) {
        Map asDoc = new LinkedHashMap();
        for (int i = 0; i < list.size(); i++) {
            asDoc.put(Integer.toString(i), list.get(i));
        }
        writeDocument(out, asDoc, depth + 1);
    }

    private static void writeElement(ByteArrayOutputStream out, String key, Object v,
            int depth) {
        if (v == null) {
            header(out, 0x0A, key);
        } else if (v instanceof String) {
            header(out, 0x02, key);
            writeString(out, (String) v);
        } else if (v instanceof Double || v instanceof Float) {
            header(out, 0x01, key);
            writeLong(out, Double.doubleToLongBits(((Number) v).doubleValue()));
        } else if (v instanceof Integer || v instanceof Short || v instanceof Byte) {
            header(out, 0x10, key);
            writeInt(out, ((Number) v).intValue());
        } else if (v instanceof Long) {
            header(out, 0x12, key);
            writeLong(out, ((Long) v).longValue());
        } else if (v instanceof Boolean) {
            header(out, 0x08, key);
            out.write(((Boolean) v).booleanValue() ? 1 : 0);
        } else if (v instanceof Date) {
            header(out, 0x09, key);
            writeLong(out, ((Date) v).getTime());
        } else if (v instanceof ObjectId) {
            header(out, 0x07, key);
            byte[] b = ((ObjectId) v).toByteArray();
            out.write(b, 0, b.length);
        } else if (v instanceof byte[]) {
            byte[] b = (byte[]) v;
            header(out, 0x05, key);
            writeInt(out, b.length);
            out.write(0);
            out.write(b, 0, b.length);
        } else if (v instanceof Decimal128Bytes) {
            header(out, 0x13, key);
            byte[] b = ((Decimal128Bytes) v).toByteArray();
            out.write(b, 0, b.length);
        } else if (v instanceof Map) {
            header(out, 0x03, key);
            writeDocument(out, (Map) v, depth + 1);
        } else if (v instanceof List) {
            header(out, 0x04, key);
            writeArray(out, (List) v, depth);
        } else if (v instanceof Object[]) {
            Object[] a = (Object[]) v;
            List l = new ArrayList(a.length);
            for (int i = 0; i < a.length; i++) {
                l.add(a[i]);
            }
            header(out, 0x04, key);
            writeArray(out, l, depth);
        } else {
            throw new IllegalArgumentException("BSON cannot hold a "
                    + v.getClass().getName() + " (field " + key + ")");
        }
    }

    private static void header(ByteArrayOutputStream out, int type, String key) {
        out.write(type);
        byte[] k = WireBuffer.utf8(key);
        for (int i = 0; i < k.length; i++) {
            if (k[i] == 0) {
                // A NUL ends a BSON key; one inside it would split the field and
                // let the rest of the name be read as a value.
                throw new IllegalArgumentException("A BSON key cannot contain NUL");
            }
        }
        out.write(k, 0, k.length);
        out.write(0);
    }

    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] b = WireBuffer.utf8(s);
        writeInt(out, b.length + 1);
        out.write(b, 0, b.length);
        out.write(0);
    }

    static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v);
        out.write(v >>> 8);
        out.write(v >>> 16);
        out.write(v >>> 24);
    }

    private static void writeLong(ByteArrayOutputStream out, long v) {
        writeInt(out, (int) v);
        writeInt(out, (int) (v >>> 32));
    }

    /// The document these bytes hold.
    public static Map decode(byte[] data) throws IOException {
        return decode(data, 0, data.length);
    }

    /// The document at `offset`, which must span exactly `length` bytes.
    public static Map decode(byte[] data, int offset, int length) throws IOException {
        int[] at = new int[] {offset};
        Map doc = readDocument(data, at, offset + length, 0);
        if (at[0] != offset + length) {
            throw new IOException("Trailing bytes after a BSON document");
        }
        return doc;
    }

    static int intAt(byte[] d, int at) {
        return (d[at] & 0xff) | ((d[at + 1] & 0xff) << 8) | ((d[at + 2] & 0xff) << 16)
                | ((d[at + 3] & 0xff) << 24);
    }

    private static long longAt(byte[] d, int at) {
        return (intAt(d, at) & 0xffffffffL) | ((long) intAt(d, at + 4) << 32);
    }

    private static void need(int at, int n, int limit) throws IOException {
        if (n < 0 || at + n > limit || at + n < at) {
            throw new IOException("A BSON document is truncated");
        }
    }

    private static Map readDocument(byte[] d, int[] at, int limit, int depth)
            throws IOException {
        if (depth > 100) {
            throw new IOException("A BSON document nested over 100 levels deep");
        }
        need(at[0], 5, limit);
        int start = at[0];
        int size = intAt(d, start);
        need(start, size, limit);
        if (size < 5 || d[start + size - 1] != 0) {
            throw new IOException("A BSON document has a bad length");
        }
        int end = start + size - 1;
        at[0] = start + 4;
        Map doc = new LinkedHashMap();
        while (at[0] < end) {
            int type = d[at[0]++] & 0xff;
            String key = cstring(d, at, end);
            doc.put(key, readValue(d, at, end, type, depth));
        }
        at[0] = start + size;
        return doc;
    }

    private static String cstring(byte[] d, int[] at, int limit) throws IOException {
        int s = at[0];
        int e = s;
        while (e < limit && d[e] != 0) {
            e++;
        }
        if (e >= limit) {
            throw new IOException("A BSON key is not terminated");
        }
        at[0] = e + 1;
        return utf8(d, s, e - s);
    }

    private static String utf8(byte[] d, int off, int len) {
        byte[] b = new byte[len];
        System.arraycopy(d, off, b, 0, len);
        return WireBuffer.fromUtf8(b);
    }

    private static Object readValue(byte[] d, int[] at, int limit, int type, int depth)
            throws IOException {
        int p = at[0];
        switch (type) {
            case 0x01:
                need(p, 8, limit);
                at[0] = p + 8;
                return Double.valueOf(Double.longBitsToDouble(longAt(d, p)));
            case 0x02: {
                need(p, 4, limit);
                int n = intAt(d, p);
                need(p + 4, n, limit);
                if (n < 1 || d[p + 4 + n - 1] != 0) {
                    throw new IOException("A BSON string has a bad length");
                }
                at[0] = p + 4 + n;
                return utf8(d, p + 4, n - 1);
            }
            case 0x03:
                return readDocument(d, at, limit, depth + 1);
            case 0x04: {
                Map asDoc = readDocument(d, at, limit, depth + 1);
                return new ArrayList(asDoc.values());
            }
            case 0x05: {
                need(p, 5, limit);
                int n = intAt(d, p);
                need(p + 5, n, limit);
                byte[] b = new byte[n];
                System.arraycopy(d, p + 5, b, 0, n);
                at[0] = p + 5 + n;
                return b;
            }
            case 0x07: {
                need(p, 12, limit);
                byte[] b = new byte[12];
                System.arraycopy(d, p, b, 0, 12);
                at[0] = p + 12;
                return new ObjectId(b);
            }
            case 0x08:
                need(p, 1, limit);
                at[0] = p + 1;
                return Boolean.valueOf(d[p] != 0);
            case 0x09:
                need(p, 8, limit);
                at[0] = p + 8;
                return new Date(longAt(d, p));
            case 0x0A:
                return null;
            case 0x10:
                need(p, 4, limit);
                at[0] = p + 4;
                return Integer.valueOf(intAt(d, p));
            case 0x11:
            case 0x12:
                need(p, 8, limit);
                at[0] = p + 8;
                return Long.valueOf(longAt(d, p));
            case 0x13: {
                need(p, 16, limit);
                byte[] b = new byte[16];
                System.arraycopy(d, p, b, 0, 16);
                at[0] = p + 16;
                return new Decimal128Bytes(b);
            }
            default:
                throw new IOException("Unsupported BSON type 0x" + Integer.toHexString(type));
        }
    }
}
