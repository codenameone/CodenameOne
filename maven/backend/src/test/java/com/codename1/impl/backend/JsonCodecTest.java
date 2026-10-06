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
package com.codename1.impl.backend;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCodecTest {
    private static final JsonCodec.Path ROOT = JsonCodec.Path.ROOT;

    @Test
    @DisplayName("a whole number is read within its range, and refused outside it")
    void wholeNumbers() {
        assertEquals(Long.MAX_VALUE, JsonCodec.readLong(Long.valueOf(Long.MAX_VALUE), ROOT, "n",
                -1, Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals(3L, JsonCodec.readLong(Double.valueOf(3.0), ROOT, "n", -1, 0, 10));
        assertEquals(Long.MIN_VALUE, JsonCodec.readLong(Double.valueOf(-9.223372036854775808E18),
                ROOT, "n", -1, Long.MIN_VALUE, Long.MAX_VALUE));
        // 2^63 as a double: Long.MAX_VALUE rounds up to it, so a double compare
        // accepted it and the cast clamped it to Long.MAX_VALUE.
        IllegalArgumentException past = assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readLong(Double.valueOf(9223372036854775808.0), ROOT, "n", -1,
                        Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals("$.n: expected a whole number from -9223372036854775808 to "
                + "9223372036854775807, got the number 9.223372036854776E18", past.getMessage());
        assertThrows(IllegalArgumentException.class, () -> JsonCodec.readLong(
                Double.valueOf(2.5), ROOT, "n", -1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> JsonCodec.readLong(
                Long.valueOf(11), ROOT, "n", -1, 0, 10));
    }

    @Test
    @DisplayName("a float is read only when a float can hold it")
    void floats() {
        assertEquals(1.5f, JsonCodec.readFloat(Double.valueOf(1.5), ROOT, "f", -1));
        assertEquals(3f, JsonCodec.readFloat(Long.valueOf(3), ROOT, "f", -1));
        assertEquals(0f, JsonCodec.readFloat(Double.valueOf(0.0), ROOT, "f", -1));
        // Past Float.MAX_VALUE it became infinity, below the smallest float zero.
        IllegalArgumentException big = assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readFloat(Double.valueOf(1e100), ROOT, "f", -1));
        assertEquals("$.f: expected a number within the range of a float, got the number "
                + "1.0E100", big.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readFloat(Double.valueOf(1e-100), ROOT, "f", -1));
    }

    @Test
    @DisplayName("dates are read from milliseconds and ISO-8601, and an impossible one is refused")
    void dates() {
        assertEquals(86400000L, JsonCodec.readDate(Long.valueOf(86400000L), ROOT, "d", -1)
                .getTime());
        assertEquals(86400000L, JsonCodec.readDate("1970-01-02T00:00:00Z", ROOT, "d", -1)
                .getTime());
        assertEquals(86399500L, JsonCodec.readDate("1970-01-02T02:59:59.5+03:00", ROOT, "d", -1)
                .getTime());
        assertEquals(86399500L, JsonCodec.readDate("1970-01-02T02:59:59.5+0300", ROOT, "d", -1)
                .getTime());
        assertEquals(951782400000L, JsonCodec.readDate("2000-02-29", ROOT, "d", -1).getTime());
        assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readDate("2001-02-29", ROOT, "d", -1));
        assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readDate("2001-01-01T25:00", ROOT, "d", -1));
    }

    @Test
    @DisplayName("a refusal names the path of the value it refused")
    void paths() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> JsonCodec.readString(Boolean.TRUE, ROOT.child("items").child(2), "name",
                        -1));
        assertEquals("$.items[2].name: expected a string, got true", e.getMessage());
        assertEquals("$[0]", JsonCodec.enter(ROOT, null, 0).toString());
        assertEquals("$", JsonCodec.enter(ROOT, null, -1).toString());
    }
}
