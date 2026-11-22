/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
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
package com.codename1.db.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The Oracle wire codecs without a server: NUMBER against the bytes Oracle's
/// own `DUMP()` prints, BINARY_DOUBLE round trips, and the statement helpers.
class OracleCodecTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int iter = 0 ; iter < values.length ; iter++) {
            out[iter] = (byte) values[iter];
        }
        return out;
    }

    @Test
    void bcDatesPutTheSignBeforeThePadding() {
        // 44 BC, March 15: century byte 100, year byte 100 - 44 = 56.
        assertEquals("-0044-03-15 12:00:00",
                Oracle.decodeDate(new byte[] {100, 56, 3, 15, 13, 1, 1}, false));
        assertEquals("2024-02-29 00:00:00",
                Oracle.decodeDate(new byte[] {120, 124, 2, 29, 1, 1, 1}, false));
    }

    @Test
    void sessionEndingErrorsAreRecognised() {
        org.junit.jupiter.api.Assertions.assertTrue(Oracle.endsSession(28));
        org.junit.jupiter.api.Assertions.assertTrue(Oracle.endsSession(3113));
        org.junit.jupiter.api.Assertions.assertFalse(Oracle.endsSession(1));
    }

    @Test
    void numberMatchesOracleDump() throws Exception {
        // SELECT DUMP(x) FROM DUAL, on any Oracle.
        Object[][] vectors = {
            {"0", bytes(0x80)},
            {"1", bytes(0xc1, 0x02)},
            {"100", bytes(0xc2, 0x02)},
            {"123.45", bytes(0xc2, 0x02, 0x18, 0x2e)},
            {"-1", bytes(0x3e, 0x64, 0x66)},
            {"-123.45", bytes(0x3d, 0x64, 0x4e, 0x38, 0x66)},
            {"0.01", bytes(0xc0, 0x02)},
        };
        for (Object[] v : vectors) {
            assertArrayEquals((byte[]) v[1], Oracle.encodeNumber((String) v[0]), "encode " + v[0]);
            assertEquals(v[0], Oracle.decodeNumber((byte[]) v[1]), "decode " + v[0]);
        }
    }

    @Test
    void numberRangeEdges() throws Exception {
        // The exponent byte is the base-100 exponent + 193; 0xFF and 0x80 are
        // the largest and smallest positive magnitudes NUMBER holds.
        assertEquals(0xFF, Oracle.encodeNumber("9E125")[0] & 0xff);
        assertEquals(0x80, Oracle.encodeNumber("1E-130")[0] & 0xff);
        assertThrows(java.io.IOException.class, () -> Oracle.encodeNumber("1E126"));
        assertThrows(java.io.IOException.class, () -> Oracle.encodeNumber("1E-131"));
        assertThrows(java.io.IOException.class, () -> Oracle.encodeNumber("1E2147483647"));
        assertEquals("1E+126", Oracle.decodeNumber(new byte[] {(byte) 0xFF, 101}));
        assertThrows(java.io.IOException.class, () -> Oracle.decodeNumber(new byte[0]));
        assertThrows(java.io.IOException.class, () -> Oracle.decodeNumber(new byte[] {(byte) 0xC1, (byte) 0xF0}));
    }

    @Test
    void numberRoundTripsLongs() throws Exception {
        long[] values = {Long.MAX_VALUE, Long.MIN_VALUE + 1, 9999999999L, -42, 1000000};
        for (long value : values) {
            String text = Long.toString(value);
            assertEquals(text, Oracle.decodeNumber(Oracle.encodeNumber(text)));
            assertEquals(Long.valueOf(value), Oracle.number(text));
        }
        assertEquals("12345678901234567890", Oracle.number("12345678901234567890"));
    }

    @Test
    void binaryDoubleRoundTrips() {
        double[] values = {0.0, 1.5, -1.5, Double.MAX_VALUE, -Double.MIN_VALUE, 3.141592653589793};
        for (double value : values) {
            assertEquals(value, Oracle.decodeBinary(Oracle.encodeDouble(value)));
        }
    }

    @Test
    void statementHelpers() {
        assertEquals("SELECT", Oracle.firstKeyword("  /* c */ (select 1 from dual)"));
        assertEquals("SELECT 1 FROM DUAL", Oracle.trimTerminator("SELECT 1 FROM DUAL;", false));
        assertEquals("BEGIN NULL; END;", Oracle.trimTerminator("BEGIN NULL; END;", true));
        assertEquals("id", Oracle.foldLabel("ID"));
        assertEquals("MixedCase", Oracle.foldLabel("MixedCase"));
    }
}
