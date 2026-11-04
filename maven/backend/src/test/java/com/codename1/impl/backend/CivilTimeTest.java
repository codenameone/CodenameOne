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

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CivilTimeTest {

    private static int[] civil(long days) {
        int[] out = new int[3];
        CivilTime.civilFromDays(days, out);
        return out;
    }

    private static String date(long days) {
        return CivilTime.appendDate(new StringBuilder(), days, '-').toString();
    }

    @Test
    void epochZero() {
        assertEquals(0, CivilTime.daysFromCivil(1970, 1, 1));
        assertArrayEquals(new int[] {1970, 1, 1}, civil(0));
        assertEquals("1970-01-01", date(0));
        assertArrayEquals(new int[] {1969, 12, 31}, civil(-1));
    }

    @Test
    void marchFirst1970() {
        assertEquals(59, CivilTime.daysFromCivil(1970, 3, 1));
        assertArrayEquals(new int[] {1970, 3, 1}, civil(59));
    }

    @Test
    void leapCenturies() {
        assertTrue(CivilTime.isLeap(2000));
        assertFalse(CivilTime.isLeap(1900));
        assertTrue(CivilTime.isLeap(-4));
        assertTrue(CivilTime.isLeap(0));
        assertEquals(29, CivilTime.daysInMonth(2000, 2));
        assertEquals(28, CivilTime.daysInMonth(1900, 2));
        assertEquals(30, CivilTime.daysInMonth(1900, 4));
        long feb29 = CivilTime.daysFromCivil(2000, 2, 29);
        assertEquals(LocalDate.of(2000, 2, 29).toEpochDay(), feb29);
        assertArrayEquals(new int[] {2000, 3, 1}, civil(feb29 + 1));
        long feb28 = CivilTime.daysFromCivil(1900, 2, 28);
        assertEquals(feb28 + 1, CivilTime.daysFromCivil(1900, 3, 1));
        assertArrayEquals(new int[] {1900, 3, 1}, civil(feb28 + 1));
    }

    @Test
    void negativeYears() {
        long d = CivilTime.daysFromCivil(-44, 3, 15);
        assertEquals(LocalDate.of(-44, 3, 15).toEpochDay(), d);
        assertArrayEquals(new int[] {-44, 3, 15}, civil(d));
        assertEquals("-0044-03-15", date(d));
        long y0 = CivilTime.daysFromCivil(0, 2, 29);
        assertEquals(LocalDate.of(0, 2, 29).toEpochDay(), y0);
        assertArrayEquals(new int[] {-1, 12, 31}, civil(CivilTime.daysFromCivil(0, 1, 1) - 1));
    }

    @Test
    void roundTripAgainstJavaTime() {
        int[] out = new int[3];
        for (long days = -800000; days <= 800000; days += 7) {
            CivilTime.civilFromDays(days, out);
            LocalDate ref = LocalDate.ofEpochDay(days);
            assertEquals(ref.getYear(), out[0]);
            assertEquals(ref.getMonthValue(), out[1]);
            assertEquals(ref.getDayOfMonth(), out[2]);
            assertEquals(days, CivilTime.daysFromCivil(out[0], out[1], out[2]));
            assertEquals(ref.lengthOfMonth(), CivilTime.daysInMonth(out[0], out[1]));
        }
    }

    @Test
    void padding() {
        assertEquals("007", CivilTime.pad(new StringBuilder(), 7, 3).toString());
        assertEquals("12345", CivilTime.pad(new StringBuilder(), 12345, 2).toString());
        assertEquals("0", CivilTime.pad(new StringBuilder(), 0, 1).toString());
        assertEquals("00", CivilTime.pad(new StringBuilder(), 0, 2).toString());
        assertEquals("-05", CivilTime.pad(new StringBuilder(), -5, 2).toString());
        assertEquals("-9223372036854775808",
                CivilTime.pad(new StringBuilder(), Long.MIN_VALUE, 4).toString());
        assertEquals("9223372036854775807",
                CivilTime.pad(new StringBuilder(), Long.MAX_VALUE, 4).toString());
    }
}
