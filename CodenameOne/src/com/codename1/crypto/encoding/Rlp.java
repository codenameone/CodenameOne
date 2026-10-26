/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.encoding;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/// Recursive Length Prefix encoding as used by Ethereum. Items are either byte
/// arrays or lists of items. Decoding rejects every non-canonical form.
@com.codename1.impl.SharedWithBackend
public final class Rlp {

    private Rlp() {
    }

    /// Encodes a byte array or a nested [List] of them.
    ///
    /// #### Parameters
    ///
    /// - `item`: a `byte[]` or a `java.util.List` whose elements are again either
    ///
    /// #### Returns
    ///
    /// the RLP bytes
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if an element is neither a byte array nor a list
    public static byte[] encode(Object item) {
        if (item instanceof byte[]) {
            byte[] b = (byte[]) item;
            if (b.length == 1 && (b[0] & 0xff) < 0x80) {
                return new byte[] {b[0]};
            }
            return concat(prefix(b.length, 0x80), b);
        }
        if (item instanceof List) {
            List l = (List) item;
            List<byte[]> parts = new ArrayList<byte[]>(l.size());
            int total = 0;
            for (int i = 0; i < l.size(); i++) {
                byte[] e = encode(l.get(i));
                parts.add(e);
                total += e.length;
            }
            byte[] pre = prefix(total, 0xc0);
            byte[] out = new byte[pre.length + total];
            System.arraycopy(pre, 0, out, 0, pre.length);
            int p = pre.length;
            for (int i = 0; i < parts.size(); i++) {
                byte[] e = parts.get(i);
                System.arraycopy(e, 0, out, p, e.length);
                p += e.length;
            }
            return out;
        }
        throw new IllegalArgumentException("RLP items must be byte[] or List");
    }

    /// Encodes a non-negative long as a minimal big-endian byte string (zero is the empty string).
    ///
    /// #### Parameters
    ///
    /// - `value`: a value of zero or more
    ///
    /// #### Returns
    ///
    /// the RLP bytes
    public static byte[] encodeLong(long value) {
        return encode(longBytes(value));
    }

    /// Encodes a non-negative integer as a minimal big-endian byte string (zero is the empty string).
    ///
    /// #### Parameters
    ///
    /// - `value`: a value of zero or more
    ///
    /// #### Returns
    ///
    /// the RLP bytes
    public static byte[] encodeBigInteger(BigInteger value) {
        return encode(bigBytes(value));
    }

    /// Returns the minimal big-endian bytes of a non-negative long, empty for zero.
    ///
    /// #### Parameters
    ///
    /// - `value`: a value of zero or more
    ///
    /// #### Returns
    ///
    /// the unpadded bytes, for use as an RLP string item
    public static byte[] longBytes(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value");
        }
        int n = 0;
        for (long v = value; v != 0; v >>>= 8) {
            n++;
        }
        byte[] out = new byte[n];
        for (int i = n - 1; i >= 0; i--) {
            out[i] = (byte) value;
            value >>>= 8;
        }
        return out;
    }

    /// Returns the minimal big-endian bytes of a non-negative integer, empty for zero.
    ///
    /// #### Parameters
    ///
    /// - `value`: a value of zero or more
    ///
    /// #### Returns
    ///
    /// the unpadded bytes, for use as an RLP string item
    public static byte[] bigBytes(BigInteger value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Negative value");
        }
        if (value.signum() == 0) {
            return new byte[0];
        }
        byte[] b = value.toByteArray();
        if (b[0] == 0) {
            byte[] t = new byte[b.length - 1];
            System.arraycopy(b, 1, t, 0, t.length);
            return t;
        }
        return b;
    }

    /// Decodes one RLP item that must span the whole input.
    ///
    /// #### Parameters
    ///
    /// - `data`: the RLP bytes
    ///
    /// #### Returns
    ///
    /// a `byte[]` for a string or a `java.util.List` of decoded items for a list
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the input is truncated, has trailing bytes or is not canonical
    public static Object decode(byte[] data) {
        int[] end = new int[1];
        Object o = decodeAt(data, 0, data.length, end);
        if (end[0] != data.length) {
            throw new IllegalArgumentException("Trailing bytes after RLP item");
        }
        return o;
    }

    private static Object decodeAt(byte[] d, int pos, int limit, int[] end) {
        if (pos >= limit) {
            throw new IllegalArgumentException("Truncated RLP");
        }
        int b = d[pos] & 0xff;
        if (b < 0x80) {
            end[0] = pos + 1;
            return new byte[] {d[pos]};
        }
        if (b < 0xb8) {
            int len = b - 0x80;
            int start = pos + 1;
            check(start, len, limit);
            if (len == 1 && (d[start] & 0xff) < 0x80) {
                throw new IllegalArgumentException("Non-canonical single byte");
            }
            end[0] = start + len;
            return slice(d, start, len);
        }
        if (b < 0xc0) {
            int ll = b - 0xb7;
            int len = readLength(d, pos + 1, ll, limit);
            if (len < 56) {
                throw new IllegalArgumentException("Non-canonical long string");
            }
            int start = pos + 1 + ll;
            check(start, len, limit);
            end[0] = start + len;
            return slice(d, start, len);
        }
        int len;
        int start;
        if (b < 0xf8) {
            len = b - 0xc0;
            start = pos + 1;
        } else {
            int ll = b - 0xf7;
            len = readLength(d, pos + 1, ll, limit);
            if (len < 56) {
                throw new IllegalArgumentException("Non-canonical long list");
            }
            start = pos + 1 + ll;
        }
        check(start, len, limit);
        int stop = start + len;
        List<Object> items = new ArrayList<Object>();
        int p = start;
        while (p < stop) {
            items.add(decodeAt(d, p, stop, end));
            p = end[0];
        }
        end[0] = stop;
        return items;
    }

    private static int readLength(byte[] d, int pos, int ll, int limit) {
        if (pos + ll > limit) {
            throw new IllegalArgumentException("Truncated RLP length");
        }
        if (d[pos] == 0) {
            throw new IllegalArgumentException("Leading zero in RLP length");
        }
        if (ll > 4 || (ll == 4 && (d[pos] & 0x80) != 0)) {
            throw new IllegalArgumentException("RLP length too large");
        }
        int len = 0;
        for (int i = 0; i < ll; i++) {
            len = (len << 8) | (d[pos + i] & 0xff);
        }
        return len;
    }

    private static void check(int start, int len, int limit) {
        if (len < 0 || len > limit - start) {
            throw new IllegalArgumentException("Truncated RLP payload");
        }
    }

    private static byte[] slice(byte[] d, int start, int len) {
        byte[] out = new byte[len];
        System.arraycopy(d, start, out, 0, len);
        return out;
    }

    private static byte[] prefix(int len, int base) {
        if (len < 56) {
            return new byte[] {(byte) (base + len)};
        }
        byte[] lb = longBytes(len);
        byte[] out = new byte[1 + lb.length];
        out[0] = (byte) (base + 55 + lb.length);
        System.arraycopy(lb, 0, out, 1, lb.length);
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
