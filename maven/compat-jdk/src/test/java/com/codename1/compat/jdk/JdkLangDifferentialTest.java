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

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// [JdkNumbers], [JdkStrings] and [Level] against the JDK members they
/// stand in for, over the values where an implementation written by hand
/// goes wrong: the extremes, zero, the sign and the half.
public class JdkLangDifferentialTest {

    private static final int[] INTS = {0, 1, -1, 2, 7, 64, 100, 0xff0f, 0x40000000, Integer.MAX_VALUE,
        Integer.MIN_VALUE, -2, 12345678};
    private static final long[] LONGS = {0L, 1L, -1L, 5L, 1L << 31, 1L << 32, 1L << 40, Long.MAX_VALUE,
        Long.MIN_VALUE, -77L, 0x123456789abcdefL};
    private static final double[] DOUBLES = {0.0, -0.0, 0.5, -0.5, 1.5, 2.5, -2.5, 3.49, -0.2, 1e300, -1e300,
        4503599627370497.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 27, -8, 0.001};

    @Test
    public void bitOperationsMatch() {
        for (int i : INTS) {
            assertEquals(Integer.bitCount(i), JdkNumbers.bitCount(i));
            assertEquals(Integer.highestOneBit(i), JdkNumbers.highestOneBit(i));
            assertEquals(Integer.lowestOneBit(i), JdkNumbers.lowestOneBit(i));
            assertEquals(Integer.numberOfTrailingZeros(i), JdkNumbers.numberOfTrailingZeros(i));
            assertEquals(Integer.reverse(i), JdkNumbers.reverse(i));
            for (int d : new int[] {0, 1, 31, 32, 33, -1}) {
                assertEquals(Integer.rotateLeft(i, d), JdkNumbers.rotateLeft(i, d));
                assertEquals(Integer.rotateRight(i, d), JdkNumbers.rotateRight(i, d));
            }
        }
        for (long l : LONGS) {
            assertEquals(Long.bitCount(l), JdkNumbers.bitCount(l));
            assertEquals(Long.highestOneBit(l), JdkNumbers.highestOneBit(l));
            assertEquals(Long.lowestOneBit(l), JdkNumbers.lowestOneBit(l));
            assertEquals(Long.numberOfLeadingZeros(l), JdkNumbers.numberOfLeadingZeros(l));
            assertEquals(Long.numberOfTrailingZeros(l), JdkNumbers.numberOfTrailingZeros(l));
            assertEquals(Long.reverse(l), JdkNumbers.reverse(l));
            assertEquals(Long.signum(l), JdkNumbers.signum(l));
            assertEquals(Long.rotateLeft(l, 65), JdkNumbers.rotateLeft(l, 65));
            assertEquals(Long.rotateRight(l, 3), JdkNumbers.rotateRight(l, 3));
            assertEquals(Long.toHexString(l), JdkNumbers.toHexString(l));
            assertEquals(Long.toOctalString(l), JdkNumbers.toOctalString(l));
            assertEquals(Long.toBinaryString(l), JdkNumbers.toBinaryString(l));
        }
    }

    @Test
    public void decodeReadsEveryPrefixAndRejectsWhatTheJdkRejects() {
        for (String s : new String[] {"0", "12", "-12", "+12", "0x1F", "-0X1f", "#ff", "010", "-010", "2147483647",
            "-2147483648", "127", "-128"}) {
            assertEquals(s, Integer.decode(s), JdkNumbers.decodeInteger(s));
            assertEquals(s, Long.decode(s), JdkNumbers.decodeLong(s));
        }
        assertEquals(Short.decode("0x7f"), JdkNumbers.decodeShort("0x7f"));
        assertEquals(Byte.decode("-128"), JdkNumbers.decodeByte("-128"));
        assertEquals(Short.valueOf("-32768"), JdkNumbers.shortValueOf("-32768"));
        assertEquals(Byte.valueOf("7f", 16), JdkNumbers.byteValueOf("7f", 16));
        assertEquals(Long.valueOf("zz", 36), JdkNumbers.longValueOf("zz", 36));
        for (String bad : new String[] {"", "0x", "--1", "0x-1", "2147483648", "x"}) {
            try {
                JdkNumbers.decodeInteger(bad);
                fail(bad);
            } catch (NumberFormatException expected) {
                // As Integer.decode.
            }
        }
        for (String bad : new String[] {"128", "-129"}) {
            try {
                JdkNumbers.byteValueOf(bad);
                fail(bad);
            } catch (NumberFormatException expected) {
                // As Byte.valueOf.
            }
        }
        try {
            JdkNumbers.shortValueOf("32768");
            fail();
        } catch (NumberFormatException expected) {
            // As Short.valueOf.
        }
    }

    @Test
    public void mathMatches() {
        for (double d : DOUBLES) {
            assertEquals(String.valueOf(d), Double.doubleToLongBits(Math.rint(d)),
                    Double.doubleToLongBits(JdkNumbers.rint(d)));
            assertEquals(String.valueOf(d), Double.doubleToLongBits(Math.signum(d)),
                    Double.doubleToLongBits(JdkNumbers.signum(d)));
            assertEquals(Double.isFinite(d), JdkNumbers.isFinite(d));
            assertEquals(String.valueOf(d), Math.hypot(d, 3), JdkNumbers.hypot(d, 3), Math.abs(Math.hypot(d, 3)) * 1e-14);
        }
        assertEquals(Math.hypot(Double.NaN, Double.POSITIVE_INFINITY),
                JdkNumbers.hypot(Double.NaN, Double.POSITIVE_INFINITY), 0);
        assertEquals(5.0, JdkNumbers.hypot(3, 4), 0);
        for (int x : INTS) {
            for (int y : new int[] {1, -1, 2, -2, 3, -3, 7, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                assertEquals(Math.floorDiv(x, y), JdkNumbers.floorDiv(x, y));
                assertEquals(Math.floorMod(x, y), JdkNumbers.floorMod(x, y));
                assertEquals(Math.floorDiv((long) x, (long) y), JdkNumbers.floorDiv((long) x, (long) y));
                assertEquals(Math.floorMod((long) x, (long) y), JdkNumbers.floorMod((long) x, (long) y));
                assertEquals(exact(x, y, 0), mine(x, y, 0));
                assertEquals(exact(x, y, 1), mine(x, y, 1));
                assertEquals(exact(x, y, 2), mine(x, y, 2));
            }
        }
        for (long x : LONGS) {
            for (long y : LONGS) {
                assertEquals(exactLong(x, y, 0), mineLong(x, y, 0));
                assertEquals(exactLong(x, y, 1), mineLong(x, y, 1));
                assertEquals(x + "*" + y, exactLong(x, y, 2), mineLong(x, y, 2));
            }
        }
        // The device answers 0 for an integer division by zero; this must not.
        try {
            JdkNumbers.floorMod(5L, 0L);
            fail();
        } catch (ArithmeticException expected) {
            // As Math.floorMod.
        }
        assertEquals(Math.toIntExact(-5L), JdkNumbers.toIntExact(-5L));
        try {
            JdkNumbers.toIntExact(1L << 31);
            fail();
        } catch (ArithmeticException expected) {
            // As Math.toIntExact.
        }
    }

    private static String exact(int x, int y, int op) {
        try {
            return String.valueOf(op == 0 ? Math.addExact(x, y) : op == 1 ? Math.subtractExact(x, y)
                    : Math.multiplyExact(x, y));
        } catch (ArithmeticException e) {
            return "overflow";
        }
    }

    private static String mine(int x, int y, int op) {
        try {
            return String.valueOf(op == 0 ? JdkNumbers.addExact(x, y) : op == 1 ? JdkNumbers.subtractExact(x, y)
                    : JdkNumbers.multiplyExact(x, y));
        } catch (ArithmeticException e) {
            return "overflow";
        }
    }

    private static String exactLong(long x, long y, int op) {
        try {
            return String.valueOf(op == 0 ? Math.addExact(x, y) : op == 1 ? Math.subtractExact(x, y)
                    : Math.multiplyExact(x, y));
        } catch (ArithmeticException e) {
            return "overflow";
        }
    }

    private static String mineLong(long x, long y, int op) {
        try {
            return String.valueOf(op == 0 ? JdkNumbers.addExact(x, y) : op == 1 ? JdkNumbers.subtractExact(x, y)
                    : JdkNumbers.multiplyExact(x, y));
        } catch (ArithmeticException e) {
            return "overflow";
        }
    }

    @Test
    public void stringsAndBuildersMatch() {
        assertEquals(String.join("/", "a", null, "c"), JdkStrings.join("/", "a", null, "c"));
        assertEquals(String.join("/", Arrays.asList("a", "b")), JdkStrings.join("/", Arrays.asList("a", "b")));
        assertEquals("ABC def".toLowerCase(Locale.ROOT), JdkStrings.toLowerCase("ABC def", Locale.ROOT));
        assertSame("abc", JdkStrings.toLowerCase("abc", Locale.ROOT));
        // Character by character: the dotless i of a Turkish device never appears.
        assertEquals("title", JdkStrings.toLowerCase("TITLE", new Locale("tr", "TR")));
        for (int start = 0; start <= 5; start++) {
            for (int end = start; end <= 8; end++) {
                assertEquals(new StringBuilder("hello").replace(start, end, "XY").toString(),
                        JdkStrings.replace(new StringBuilder("hello"), start, end, "XY").toString());
                assertEquals(new StringBuffer("hello").replace(start, end, "").toString(),
                        JdkStrings.replace(new StringBuffer("hello"), start, end, "").toString());
            }
        }
        try {
            JdkStrings.replace(new StringBuilder("hello"), 6, 7, "x");
            fail();
        } catch (StringIndexOutOfBoundsException expected) {
            // As StringBuilder.replace.
        }
        StringBuilder sb = new StringBuilder("a-b-c");
        assertEquals(sb.indexOf("-", 2), JdkStrings.indexOf(sb, "-", 2));
        assertEquals(sb.lastIndexOf("-"), JdkStrings.lastIndexOf(sb, "-"));
        assertEquals(sb.substring(2), JdkStrings.substring(sb, 2));
        assertEquals("ab-b-c", JdkStrings.insert(new StringBuilder("b-b-c"), 0, new char[] {'a'}).toString());
    }

    @Test
    public void charactersMatchAcrossLatin1AndTheCommonScripts() {
        for (int c = 0; c < 0x250; c++) {
            assertEquals("letter " + Integer.toHexString(c), Character.isLetter((char) c), JdkStrings.isLetter((char) c));
            assertEquals(Character.isISOControl((char) c), JdkStrings.isISOControl((char) c));
            assertEquals(Character.isLetterOrDigit(c), JdkStrings.isLetterOrDigit(c));
        }
        for (char c : new char[] {0x3b1, 0x416, 0x5d0, 0x4e2d, 0x3042, 0xac00}) {
            assertTrue(Integer.toHexString(c), JdkStrings.isLetter(c));
        }
        for (char c : new char[] {0x2014, 0x20ac, 0x3001, 0x37e}) {
            assertFalse(Integer.toHexString(c), JdkStrings.isLetter(c));
        }
        for (int radix : new int[] {1, 2, 10, 16, 36, 37}) {
            for (int digit = -1; digit < 38; digit++) {
                assertEquals(Character.forDigit(digit, radix), JdkStrings.forDigit(digit, radix));
            }
        }
        for (char c : new char[] {'0', '9', 'a', 'Z', '-', ' '}) {
            assertEquals(Character.getNumericValue(c), JdkStrings.getNumericValue(c));
        }
        assertFalse(JdkStrings.isLetter(0x1f600));
        assertEquals(0x1f600, JdkStrings.toUpperCase(0x1f600));
    }

    @Test
    public void levelsParseAndCompareAsTheJdksDo() {
        for (java.util.logging.Level jdk : new java.util.logging.Level[] {java.util.logging.Level.OFF,
            java.util.logging.Level.SEVERE, java.util.logging.Level.WARNING, java.util.logging.Level.INFO,
            java.util.logging.Level.CONFIG, java.util.logging.Level.FINE, java.util.logging.Level.FINER,
            java.util.logging.Level.FINEST, java.util.logging.Level.ALL}) {
            Level mine = Level.parse(jdk.getName());
            assertEquals(jdk.getName(), mine.getName());
            assertEquals(jdk.intValue(), mine.intValue());
            assertSame(mine, Level.parse(String.valueOf(jdk.intValue())));
            assertEquals(jdk.toString(), mine.toString());
        }
        Level custom = new Level("AUDIT", 850) {
            private static final long serialVersionUID = 1L;
        };
        assertEquals("AUDIT", custom.getName());
        assertNull(custom.getResourceBundleName());
        assertEquals(java.util.logging.Level.parse("123").getName(), Level.parse("123").getName());
        try {
            Level.parse("LOUD");
            fail();
        } catch (IllegalArgumentException expected) {
            // As Level.parse.
        }
        Logger quiet = Logger.getAnonymousLogger();
        assertTrue(quiet.isLoggable(Level.INFO));
        assertFalse(quiet.isLoggable(Level.FINE));
        quiet.setLevel(Level.OFF);
        assertFalse(quiet.isLoggable(Level.SEVERE));
        assertSame(Logger.getLogger("a.b"), Logger.getLogger("a.b"));
    }
}
