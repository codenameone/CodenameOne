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
package com.codename1.bluetooth;

/// Immutable 128-bit Bluetooth UUID value type used throughout the
/// `com.codename1.bluetooth` API for services, characteristics and
/// descriptors. The Codename One core has no `java.util.UUID`, so this class
/// fills that role and adds Bluetooth-specific conveniences: 16/32-bit SIG
/// assigned numbers are expanded over the Bluetooth Base UUID via
/// [#fromShort(int)] and recognized by [#isShortUuid()].
///
/// ```java
/// BluetoothUuid heartRate = BluetoothUuid.fromShort(0x180D);
/// BluetoothUuid custom = BluetoothUuid.fromString(
///         "5f47a3c0-1234-4e6b-9d00-000000000001");
/// ```
public final class BluetoothUuid {

    private static final long BASE_MSB_SUFFIX = 0x1000L;
    private static final long BASE_LSB = 0x800000805F9B34FBL;

    private final long msb;
    private final long lsb;

    /// Creates a UUID from its raw 64-bit halves, mirroring
    /// `java.util.UUID(long, long)`.
    public BluetoothUuid(long mostSigBits, long leastSigBits) {
        this.msb = mostSigBits;
        this.lsb = leastSigBits;
    }

    /// Parses a UUID string. Accepts the full canonical 36 character form
    /// (`0000180d-0000-1000-8000-00805f9b34fb`), an 8 hex digit 32-bit SIG
    /// assigned number (`0000180D`) or a 4 hex digit 16-bit one (`180D`);
    /// the short forms are expanded over the Bluetooth Base UUID. Case
    /// insensitive. Throws `IllegalArgumentException` on malformed input.
    public static BluetoothUuid fromString(String uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid is null");
        }
        String s = uuid.trim();
        int len = s.length();
        if (len == 4 || len == 8) {
            return fromShort((int) parseHex(s, 0, len));
        }
        if (len == 36) {
            if (s.charAt(8) != '-' || s.charAt(13) != '-'
                    || s.charAt(18) != '-' || s.charAt(23) != '-') {
                throw new IllegalArgumentException("Malformed UUID: " + uuid);
            }
            long high = (parseHex(s, 0, 8) << 32)
                    | (parseHex(s, 9, 13) << 16)
                    | parseHex(s, 14, 18);
            long low = (parseHex(s, 19, 23) << 48)
                    | parseHex(s, 24, 36);
            return new BluetoothUuid(high, low);
        }
        throw new IllegalArgumentException("Malformed UUID: " + uuid);
    }

    /// Expands a 16 or 32-bit Bluetooth SIG assigned number over the
    /// Bluetooth Base UUID, e.g. `fromShort(0x180D)` yields
    /// `0000180d-0000-1000-8000-00805f9b34fb` (the Heart Rate service).
    public static BluetoothUuid fromShort(int assignedNumber) {
        return new BluetoothUuid(
                ((assignedNumber & 0xFFFFFFFFL) << 32) | BASE_MSB_SUFFIX,
                BASE_LSB);
    }

    /// The most significant 64 bits, mirroring
    /// `java.util.UUID.getMostSignificantBits()`.
    public long getMostSignificantBits() {
        return msb;
    }

    /// The least significant 64 bits, mirroring
    /// `java.util.UUID.getLeastSignificantBits()`.
    public long getLeastSignificantBits() {
        return lsb;
    }

    /// `true` when this UUID is a Bluetooth Base UUID derivation, i.e. a
    /// 16/32-bit SIG assigned number expanded via [#fromShort(int)] or an
    /// equivalent string form. When `true`, [#getShortValue()] returns the
    /// assigned number.
    public boolean isShortUuid() {
        return (msb & 0xFFFFFFFFL) == BASE_MSB_SUFFIX && lsb == BASE_LSB;
    }

    /// The 16/32-bit SIG assigned number of a Base-UUID derivation. Throws
    /// `IllegalStateException` when [#isShortUuid()] is `false`.
    public int getShortValue() {
        if (!isShortUuid()) {
            throw new IllegalStateException(
                    "Not a Bluetooth Base UUID derivation: " + toString());
        }
        return (int) (msb >>> 32);
    }

    /// Canonical lowercase 36 character form, e.g.
    /// `0000180d-0000-1000-8000-00805f9b34fb`.
    public String toString() {
        StringBuilder sb = new StringBuilder(36);
        appendHex(sb, msb >>> 32, 8);
        sb.append('-');
        appendHex(sb, msb >>> 16, 4);
        sb.append('-');
        appendHex(sb, msb, 4);
        sb.append('-');
        appendHex(sb, lsb >>> 48, 4);
        sb.append('-');
        appendHex(sb, lsb, 12);
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BluetoothUuid)) {
            return false;
        }
        BluetoothUuid u = (BluetoothUuid) o;
        return msb == u.msb && lsb == u.lsb;
    }

    public int hashCode() {
        long hilo = msb ^ lsb;
        return (int) (hilo >> 32) ^ (int) hilo;
    }

    private static long parseHex(String s, int from, int to) {
        long v = 0;
        for (int i = from; i < to; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) {
                throw new IllegalArgumentException("Malformed UUID: " + s);
            }
            v = (v << 4) | d;
        }
        return v;
    }

    private static void appendHex(StringBuilder sb, long value, int digits) {
        for (int i = digits - 1; i >= 0; i--) {
            sb.append(Character.forDigit((int) ((value >> (i * 4)) & 0xf), 16));
        }
    }

    /// The Bluetooth Base UUID, `00000000-0000-1000-8000-00805f9b34fb`. All
    /// 16/32-bit SIG assigned numbers are derivations of it.
    public static final BluetoothUuid BASE =
            new BluetoothUuid(BASE_MSB_SUFFIX, BASE_LSB);

    /// The Client Characteristic Configuration descriptor (`0x2902`) written
    /// by the stack when enabling notifications/indications.
    public static final BluetoothUuid CCCD = fromShort(0x2902);

    /// The classic Serial Port Profile service (`0x1101`) used as the default
    /// UUID for RFCOMM connections.
    public static final BluetoothUuid SPP = fromShort(0x1101);
}
