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

import com.codename1.backend.Crypto;

/// A MongoDB object identifier: twelve bytes, a four-byte timestamp first.
public final class ObjectId {
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final byte[] PROCESS = random(5);
    private static final java.util.concurrent.atomic.AtomicInteger COUNTER =
            new java.util.concurrent.atomic.AtomicInteger(
                    ((random(1)[0] & 0xff) << 16) | ((random(1)[0] & 0xff) << 8));

    private final byte[] bytes;

    /// Random bytes for uniqueness, not secrecy: an identifier is public, so a
    /// failing system source falls back to the clock rather than failing a class
    /// initializer.
    private static byte[] random(int n) {
        try {
            return Crypto.randomBytes(n);
        } catch (java.io.IOException err) {
            byte[] b = new byte[n];
            long t = System.nanoTime() ^ System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                b[i] = (byte) (t >>> (i * 8));
            }
            return b;
        }
    }

    /// The identifier held in these twelve bytes.
    public ObjectId(byte[] bytes) {
        if (bytes == null || bytes.length != 12) {
            throw new IllegalArgumentException("An ObjectId is twelve bytes");
        }
        this.bytes = copy(bytes);
    }

    /// A new identifier: the current time, a per-process random value and a
    /// counter, as the MongoDB drivers generate them.
    public static ObjectId generate() {
        byte[] b = new byte[12];
        int seconds = (int) (System.currentTimeMillis() / 1000L);
        b[0] = (byte) (seconds >>> 24);
        b[1] = (byte) (seconds >>> 16);
        b[2] = (byte) (seconds >>> 8);
        b[3] = (byte) seconds;
        System.arraycopy(PROCESS, 0, b, 4, 5);
        int c = COUNTER.getAndIncrement();
        b[9] = (byte) (c >>> 16);
        b[10] = (byte) (c >>> 8);
        b[11] = (byte) c;
        return new ObjectId(b);
    }

    /// The identifier spelled as 24 hex digits.
    public static ObjectId parse(String hex) {
        if (hex == null || hex.length() != 24) {
            throw new IllegalArgumentException("An ObjectId is 24 hex digits: " + hex);
        }
        byte[] b = new byte[12];
        for (int i = 0; i < 12; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Not hex: " + hex);
            }
            b[i] = (byte) ((hi << 4) | lo);
        }
        return new ObjectId(b);
    }

    private static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }

    /// The twelve bytes.
    public byte[] toByteArray() {
        return copy(bytes);
    }

    /// The creation time, in milliseconds since the epoch.
    public long getTimestampMillis() {
        long s = ((bytes[0] & 0xffL) << 24) | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
        return s * 1000L;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 12; i++) {
            sb.append(HEX[(bytes[i] >> 4) & 15]).append(HEX[bytes[i] & 15]);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectId)) {
            return false;
        }
        byte[] other = ((ObjectId) o).bytes;
        for (int i = 0; i < 12; i++) {
            if (bytes[i] != other[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 17;
        for (int i = 0; i < 12; i++) {
            h = h * 31 + bytes[i];
        }
        return h;
    }
}
