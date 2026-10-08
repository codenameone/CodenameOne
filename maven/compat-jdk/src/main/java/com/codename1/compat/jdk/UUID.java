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
package com.codename1.compat.jdk;

import java.util.Random;

/// `java.util.UUID` for the Codename One runtime: a 128 bit identifier, its
/// canonical text form, and random (version 4) generation.
///
/// [#randomUUID()] draws from `java.util.Random`, which is all the device
/// library offers this class; the identifiers are unique for every practical
/// purpose and are NOT unpredictable. Do not use one as a secret -- a session
/// token, a password reset code. `com.codename1.security.SecureRandom` is
/// what Codename One provides for that.
///
/// `nameUUIDFromBytes` (which needs MD5) and the time based accessors
/// (`timestamp`, `clockSequence`, `node`) are not provided.
public final class UUID implements Comparable<UUID>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final String HEX = "0123456789abcdef";

    /// One generator for the life of the application, seeded from the clock
    /// when the class is first used.
    private static final Random RANDOM = new Random(System.currentTimeMillis() ^ (System.nanoTime() << 21)
            ^ System.identityHashCode(HEX));

    private final long mostSigBits;
    private final long leastSigBits;

    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    /// A new version 4 identifier; see the class description for what its
    /// randomness is good for.
    public static UUID randomUUID() {
        long most = RANDOM.nextLong();
        long least = RANDOM.nextLong();
        // Version 4 in the high nibble of the seventh byte, the IETF variant
        // in the top two bits of the ninth.
        most = (most & 0xffffffffffff0fffL) | 0x0000000000004000L;
        least = (least & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new UUID(most, least);
    }

    /// Parses the text form [#toString()] writes: five groups of hexadecimal
    /// digits separated by `-`. As in the JDK, a group may be shorter than
    /// its full width.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `name` is not in that form
    public static UUID fromString(String name) {
        long[] groups = new long[5];
        int group = 0;
        int digits = 0;
        long value = 0;
        int n = name.length();
        for (int i = 0; i <= n; i++) {
            char c = i < n ? name.charAt(i) : '-';
            if (c == '-') {
                if (digits == 0 || group > 4) {
                    throw new IllegalArgumentException("Invalid UUID string: " + name);
                }
                groups[group++] = value;
                value = 0;
                digits = 0;
                continue;
            }
            int d = hexDigit(c);
            if (d < 0 || digits >= 16) {
                throw new IllegalArgumentException("Invalid UUID string: " + name);
            }
            value = (value << 4) | d;
            digits++;
        }
        if (group != 5) {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }
        long most = ((groups[0] & 0xffffffffL) << 32) | ((groups[1] & 0xffffL) << 16) | (groups[2] & 0xffffL);
        long least = ((groups[3] & 0xffffL) << 48) | (groups[4] & 0xffffffffffffL);
        return new UUID(most, least);
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    /// The version number: 4 for one made by [#randomUUID()].
    public int version() {
        return (int) ((mostSigBits >> 12) & 0x0f);
    }

    /// The variant number: 2 for the IETF layout every identifier made here
    /// has.
    public int variant() {
        return (int) ((leastSigBits >>> (64 - (leastSigBits >>> 62))) & (leastSigBits >> 63));
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(36);
        hex(out, mostSigBits >> 32, 8);
        out.append('-');
        hex(out, mostSigBits >> 16, 4);
        out.append('-');
        hex(out, mostSigBits, 4);
        out.append('-');
        hex(out, leastSigBits >> 48, 4);
        out.append('-');
        hex(out, leastSigBits, 12);
        return out.toString();
    }

    private static void hex(StringBuilder out, long value, int digits) {
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            out.append(HEX.charAt((int) ((value >> shift) & 0xf)));
        }
    }

    @Override
    public int hashCode() {
        long hilo = mostSigBits ^ leastSigBits;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UUID)) {
            return false;
        }
        UUID other = (UUID) obj;
        return mostSigBits == other.mostSigBits && leastSigBits == other.leastSigBits;
    }

    /// Orders by the most significant half, then the least, each as a signed
    /// number -- the order the JDK defines.
    @Override
    public int compareTo(UUID val) {
        if (mostSigBits != val.mostSigBits) {
            return mostSigBits < val.mostSigBits ? -1 : 1;
        }
        if (leastSigBits != val.leastSigBits) {
            return leastSigBits < val.leastSigBits ? -1 : 1;
        }
        return 0;
    }
}
