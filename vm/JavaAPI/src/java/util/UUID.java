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
package java.util;

import com.codename1.impl.JdkHost;

/// A 128 bit identifier: its canonical text form, random (version 4)
/// generation and name based (version 3) generation.
///
/// [#randomUUID()] draws its bits from the platform's secure random source
/// through `JdkHost`. Where the platform installed none it falls back to a
/// `Random` seeded from the clock, which keeps identifiers unique for every
/// practical purpose but NOT unpredictable; an application that uses one as
/// a secret should check that its platform provides secure random bytes.
///
/// [#nameUUIDFromBytes(byte\[\])] needs an MD5 digest from the platform and
/// throws `UnsupportedOperationException` where there is none. The time based
/// accessors (`timestamp`, `clockSequence`, `node`) are not provided.
public final class UUID implements Comparable<UUID>, java.io.Serializable {

    private static final long serialVersionUID = -4856846361193249489L;

    private static final String HEX = "0123456789abcdef";

    /// Made the first time the host has no secure source; see the class
    /// description.
    private static Random fallback;

    private final long mostSigBits;
    private final long leastSigBits;

    /// An identifier from its two halves.
    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    /// A new version 4 identifier; see the class description for where its
    /// randomness comes from.
    public static UUID randomUUID() {
        byte[] bits = new byte[16];
        try {
            JdkHost.get().randomBytes(bits);
        } catch (UnsupportedOperationException noSecureSource) {
            insecureBytes(bits);
        }
        // Version 4 in the high nibble of the seventh byte, the IETF variant
        // in the top two bits of the ninth.
        bits[6] = (byte) ((bits[6] & 0x0f) | 0x40);
        bits[8] = (byte) ((bits[8] & 0x3f) | 0x80);
        return fromBytes(bits);
    }

    private static synchronized void insecureBytes(byte[] out) {
        if (fallback == null) {
            fallback = new Random(System.currentTimeMillis() ^ (System.nanoTime() << 21));
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) fallback.nextInt(256);
        }
    }

    /// The version 3 identifier of `name`: its MD5 digest, with the version
    /// and variant bits set.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if the platform has no MD5 digest
    public static UUID nameUUIDFromBytes(byte[] name) {
        JdkHost.Digest md5 = JdkHost.get().digest("MD5");
        if (md5 == null) {
            throw new UnsupportedOperationException("MD5 not supported");
        }
        md5.update(name, 0, name.length);
        byte[] bits = md5.digest();
        bits[6] = (byte) ((bits[6] & 0x0f) | 0x30);
        bits[8] = (byte) ((bits[8] & 0x3f) | 0x80);
        return fromBytes(bits);
    }

    private static UUID fromBytes(byte[] bits) {
        long most = 0;
        long least = 0;
        for (int i = 0; i < 8; i++) {
            most = (most << 8) | (bits[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            least = (least << 8) | (bits[i] & 0xff);
        }
        return new UUID(most, least);
    }

    /// Parses the text form [#toString()] writes: five groups of hexadecimal
    /// digits separated by `-`. As in the JDK, a group may be shorter than
    /// its full width, and one that is longer is not refused either: the
    /// digits past its width are dropped, so `100000000-0-0-0-0` is the
    /// nil UUID there and here. Only a group of more than sixteen digits,
    /// which no `long` holds, is an error.
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
            if ((value >>> 59) != 0) {
                // Past Long.MAX_VALUE: the JDK's signed parse refuses it
                // rather than wrapping into a different UUID.
                throw new NumberFormatException("Invalid UUID string: " + name);
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

    /// The most significant 64 bits.
    public long getMostSignificantBits() {
        return mostSigBits;
    }

    /// The least significant 64 bits.
    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    /// The version number: 4 for one made by [#randomUUID()], 3 for one made
    /// by [#nameUUIDFromBytes(byte\[\])].
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
