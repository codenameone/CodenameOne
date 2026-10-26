/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

import java.io.ByteArrayOutputStream;
import java.util.List;

/// Ethereum's Recursive Length Prefix encoding, the byte form every
/// transaction is signed and broadcast in. Only encoding: a node's answers
/// arrive as JSON, so nothing here ever parses RLP from outside.
///
/// An item is a byte string or a list of items. Integers are byte strings
/// too, big-endian with no leading zero bytes (zero is the empty string), and
/// [#quantity] is the only way a number should get in: a stray leading zero
/// makes a transaction every node refuses.
public final class Rlp {
    private Rlp() {
    }

    /// One byte string.
    public static byte[] string(byte[] value) {
        if (value.length == 1 && (value[0] & 0xff) < 0x80) {
            return new byte[] {value[0]};
        }
        return concat(prefix(0x80, value.length), value);
    }

    /// A list of already encoded items.
    public static byte[] list(List<byte[]> encodedItems) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] item : encodedItems) {
            body.write(item, 0, item.length);
        }
        byte[] payload = body.toByteArray();
        return concat(prefix(0xc0, payload.length), payload);
    }

    /// A non-negative integer.
    public static byte[] quantity(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative quantity");
        }
        return string(Wei.trim(Wei.ofLong(value)));
    }

    /// An unsigned big-endian integer, leading zeros stripped.
    public static byte[] quantity(byte[] unsigned) {
        return string(Wei.trim(unsigned));
    }

    private static byte[] prefix(int offset, int length) {
        if (length < 56) {
            return new byte[] {(byte) (offset + length)};
        }
        byte[] len = Wei.trim(Wei.ofLong(length));
        byte[] out = new byte[1 + len.length];
        out[0] = (byte) (offset + 55 + len.length);
        System.arraycopy(len, 0, out, 1, len.length);
        return out;
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
