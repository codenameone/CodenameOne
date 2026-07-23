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
package com.codename1.bluetooth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@link BluetoothUuid} value-type contract: SIG short-form expansion,
 * string parsing in the 4/8/36 character forms, canonical lowercase
 * rendering, equality and the well-known constants. Pure value type -- no
 * Display required.
 */
class BluetoothUuidTest {

    private static final String HEART_RATE =
            "0000180d-0000-1000-8000-00805f9b34fb";

    @Test
    void fromShortExpandsOverTheBaseUuid() {
        BluetoothUuid u = BluetoothUuid.fromShort(0x180D);
        assertEquals(HEART_RATE, u.toString());
        assertEquals(0x0000180d00001000L, u.getMostSignificantBits());
        assertEquals(0x800000805F9B34FBL, u.getLeastSignificantBits());
    }

    @Test
    void fromStringAcceptsFourHexDigits() {
        assertEquals(BluetoothUuid.fromShort(0x180D),
                BluetoothUuid.fromString("180D"));
    }

    @Test
    void fromStringAcceptsEightHexDigits() {
        assertEquals(BluetoothUuid.fromShort(0x180D),
                BluetoothUuid.fromString("0000180D"));
        assertEquals(BluetoothUuid.fromShort(0x12345678),
                BluetoothUuid.fromString("12345678"));
    }

    @Test
    void fromStringAcceptsCanonicalThirtySixCharacterForm() {
        BluetoothUuid u = BluetoothUuid.fromString(
                "5f47a3c0-1234-4e6b-9d00-000000000001");
        assertEquals(0x5F47A3C012344E6BL, u.getMostSignificantBits());
        assertEquals(0x9D00000000000001L, u.getLeastSignificantBits());
        assertEquals("5f47a3c0-1234-4e6b-9d00-000000000001", u.toString());
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(BluetoothUuid.fromString(HEART_RATE),
                BluetoothUuid.fromString(HEART_RATE.toUpperCase()));
        assertEquals(BluetoothUuid.fromString("180d"),
                BluetoothUuid.fromString("180D"));
    }

    @Test
    void fromStringRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(null));
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(""));
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString("180"));
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString("180x"));
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString("0000180g"));
        // 35 characters -- one short of the canonical form
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(
                        HEART_RATE.substring(0, 35)));
        // 37 characters
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(HEART_RATE + "0"));
        // dash in the wrong place
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(
                        "0000180d00000-1000-8000-00805f9b34fb"));
        // non-hex character inside the canonical form
        assertThrows(IllegalArgumentException.class,
                () -> BluetoothUuid.fromString(
                        "0000180d-0000-1000-8000-00805f9b34fg"));
    }

    @Test
    void toStringIsCanonicalLowercase() {
        BluetoothUuid u = BluetoothUuid.fromString(HEART_RATE.toUpperCase());
        assertEquals(HEART_RATE, u.toString());
        assertEquals("00002902-0000-1000-8000-00805f9b34fb",
                BluetoothUuid.CCCD.toString());
    }

    @Test
    void isShortUuidRecognizesBaseUuidDerivations() {
        assertTrue(BluetoothUuid.fromShort(0x180D).isShortUuid());
        assertTrue(BluetoothUuid.fromString(HEART_RATE).isShortUuid());
        assertEquals(0x180D,
                BluetoothUuid.fromString(HEART_RATE).getShortValue());
        assertEquals(0x12345678,
                BluetoothUuid.fromShort(0x12345678).getShortValue());
        assertFalse(BluetoothUuid.fromString(
                "5f47a3c0-1234-4e6b-9d00-000000000001").isShortUuid());
    }

    @Test
    void getShortValueThrowsOnNonBaseDerivations() {
        BluetoothUuid custom = BluetoothUuid.fromString(
                "5f47a3c0-1234-4e6b-9d00-000000000001");
        assertThrows(IllegalStateException.class, custom::getShortValue);
    }

    @Test
    void equalsAndHashCodeAreValueBased() {
        BluetoothUuid a = BluetoothUuid.fromShort(0x180D);
        BluetoothUuid b = BluetoothUuid.fromString("180d");
        BluetoothUuid c = BluetoothUuid.fromShort(0x180F);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, HEART_RATE);
    }

    @Test
    void wellKnownConstantsAreCorrect() {
        assertEquals("00000000-0000-1000-8000-00805f9b34fb",
                BluetoothUuid.BASE.toString());
        assertEquals(0x2902, BluetoothUuid.CCCD.getShortValue());
        assertEquals(0x1101, BluetoothUuid.SPP.getShortValue());
        assertEquals(BluetoothUuid.fromShort(0x2902), BluetoothUuid.CCCD);
        assertEquals(BluetoothUuid.fromShort(0x1101), BluetoothUuid.SPP);
    }

    @Test
    void rawHalvesConstructorMirrorsJavaUtilUuid() {
        BluetoothUuid u = new BluetoothUuid(0x0000180d00001000L,
                0x800000805F9B34FBL);
        assertEquals(BluetoothUuid.fromShort(0x180D), u);
        assertEquals(HEART_RATE, u.toString());
    }
}
