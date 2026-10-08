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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Runs the same patterns and values through this package's `DecimalFormat`
/// and the JDK's, and requires the same text, the same parsed numbers and the
/// same parse positions.
public class DecimalFormatDifferentialTest {

    private static final Locale US = new Locale("en", "US");
    private static final String CURRENCY = String.valueOf((char) 0xA4);
    private static final String PER_MILLE = String.valueOf((char) 0x2030);

    private static final String[] PATTERNS = {
        "0", "#", "0.0", "0.00", "#.##", "#.00", "#,##0.00", "#,###", "#,##0.###", "000.000",
        "0.###E0", "00.###E0", "##0.#####E0", "0.00E00", "##0.##E0", "#E0", "0E0", "000E0", "0.0E0",
        "#,##0.00E0", "###0.###E00",
        "#%", "0.0%", "#,##0%", "#" + PER_MILLE, "0.00" + PER_MILLE,
        "#,##0.00;(#,##0.00)", "0.0;0.0-", "-0;+0", "+0.0;-0.0",
        "'#'#", "$#,##0.00", "0.0 'units'", "'x'''0", "0 'o''clock'", "EUR 0.00", "0.0 kg",
        "#,####.##", "#,##,##0.0", "#,#", "0.#", ".00", "#.", "0.", "000", ".##", "#.0#",
        CURRENCY + "#,##0.00", CURRENCY + CURRENCY + " 0.00", "#,##0.00 " + CURRENCY,
        "0.0000000000", "#.##########", "0.000000000000000000", "00000000000000000000.0",
        "", "abc", "'%'0", "0'%'", "a0b;c0d", "#,##0.###;-#",
    };

    private static final double[] DOUBLES = {
        0, -0.0, 1, -1, 0.5, -0.5, 1.5, 2.5, 3.5, 0.125, 0.135, 0.145, 0.15, 0.25, 0.35, 0.45, 0.05,
        0.005, 0.015, 0.025, 1.005, 1.015, 1.025, 2.675, 4.35, 8.345, 9.995, 99.995, 0.999, 0.9999,
        999.9995, 1234.5678, -1234.5678, 0.001, 0.0005, 0.00049, 0.00051, 1e-7, 1e-10, 123456789.123,
        1e15, 1e16, 1e17, 1e20, 1e22, 1e23, 1.23e25, 12345, 100, 1000, 999, 999.5, 9999.5, 0.1 + 0.2,
        1.0 / 3, 2.0 / 3, 10.0 / 3, 1e3, 1e6, 123456.789, 0.000123456, 1.5e-5, 2.5e-5, 6.02214076e23,
        Double.MAX_VALUE, Double.MIN_VALUE, 5e-324, Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, 0.5000000000000001, 0.49999999999999994, 1.0000000000000002,
        4503599627370496.5, 9007199254740993.0, 72057594037927936.0, -0.001, -0.004, -0.006,
        0.285, 1.115, 5.015, 1.45, 1.55, 1.65, 10.5, 11.5, 0.3, 0.7, 1e-320,
    };

    private static final long[] LONGS = {
        0, 1, -1, 9, 10, 12, 99, 100, 999, 1000, 1234567, -987654321, 1500, 2500, 12345, 15, 25, 125,
        Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, 1000000000000L,
    };

    private static DecimalFormat shim(String pattern) {
        return new DecimalFormat(pattern, new DecimalFormatSymbols(US));
    }

    private static java.text.DecimalFormat jdk(String pattern) {
        return new java.text.DecimalFormat(pattern, new java.text.DecimalFormatSymbols(US));
    }

    private static void report(List<String> failures) {
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(failures.size()).append(" difference(s) from the JDK:\n");
            for (int i = 0; i < failures.size() && i < 60; i++) {
                sb.append(failures.get(i)).append('\n');
            }
            fail(sb.toString());
        }
    }

    @Test
    public void formatsDoublesLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            DecimalFormat mine = shim(pattern);
            java.text.DecimalFormat theirs = jdk(pattern);
            for (double value : DOUBLES) {
                String expected = theirs.format(value);
                String actual = mine.format(value);
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + value + ": jdk=" + expected + " shim=" + actual);
                }
            }
        }
        report(failures);
    }

    @Test
    public void formatsLongsLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            DecimalFormat mine = shim(pattern);
            java.text.DecimalFormat theirs = jdk(pattern);
            for (long value : LONGS) {
                String expected = theirs.format(value);
                String actual = mine.format(value);
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + value + "L: jdk=" + expected + " shim=" + actual);
                }
            }
        }
        report(failures);
    }

    /// Random values across the whole exponent range, and values built to sit
    /// on or next to a rounding boundary, where half-even and the binary
    /// representation both matter.
    @Test
    public void formatsRandomDoublesLikeTheJdk() {
        String[] patterns = {
            "0", "0.0", "0.00", "0.000", "#.####", "#,##0.00", "0.###E0", "##0.##E0", "0.00%", "0.0000000",
        };
        Random random = new Random(20240607L);
        List<String> failures = new ArrayList<String>();
        for (String pattern : patterns) {
            DecimalFormat mine = shim(pattern);
            java.text.DecimalFormat theirs = jdk(pattern);
            for (int i = 0; i < 4000; i++) {
                double value;
                int kind = i % 4;
                if (kind == 0) {
                    value = Double.longBitsToDouble(random.nextLong());
                } else if (kind == 1) {
                    value = (random.nextDouble() - 0.5) * 2000;
                } else if (kind == 2) {
                    // k / 1000 and k / 8: decimal halves, some exact in binary.
                    value = random.nextInt(200000) / (i % 8 < 4 ? 1000.0 : 8.0);
                } else {
                    value = (random.nextInt(2000) + 0.5) / Math.pow(10, random.nextInt(6));
                }
                String expected = theirs.format(value);
                String actual = mine.format(value);
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + value + ": jdk=" + expected + " shim=" + actual);
                }
            }
        }
        report(failures);
    }

    @Test
    public void reproducesPatternsLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            if (pattern.length() == 0) {
                // The empty pattern allows any number of fraction digits, and
                // the JDK runs out of memory writing a '#' for each.
                continue;
            }
            String expected = jdk(pattern).toPattern();
            String actual = shim(pattern).toPattern();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] toPattern: jdk=" + expected + " shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void readsPatternSettingsLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            java.text.DecimalFormat t = jdk(pattern);
            DecimalFormat m = shim(pattern);
            String expected = t.getMinimumIntegerDigits() + "/" + t.getMaximumIntegerDigits() + "/"
                    + t.getMinimumFractionDigits() + "/" + t.getMaximumFractionDigits() + "/"
                    + t.isGroupingUsed() + "/" + t.getGroupingSize() + "/" + t.getMultiplier() + "/"
                    + t.isDecimalSeparatorAlwaysShown() + "/" + t.getPositivePrefix() + "/"
                    + t.getPositiveSuffix() + "/" + t.getNegativePrefix() + "/" + t.getNegativeSuffix();
            String actual = m.getMinimumIntegerDigits() + "/" + m.getMaximumIntegerDigits() + "/"
                    + m.getMinimumFractionDigits() + "/" + m.getMaximumFractionDigits() + "/"
                    + m.isGroupingUsed() + "/" + m.getGroupingSize() + "/" + m.getMultiplier() + "/"
                    + m.isDecimalSeparatorAlwaysShown() + "/" + m.getPositivePrefix() + "/"
                    + m.getPositiveSuffix() + "/" + m.getNegativePrefix() + "/" + m.getNegativeSuffix();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] settings: jdk=" + expected + " shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void rejectsTheMalformedPatternsTheJdkRejects() {
        String[] bad = {
            "0.0.0", "#0#", "0#.0", "0.#0", "0E", "E0", "0E0E0", "#,", "0;0;0", "0'", "%%0", "0;",
            "0.0#0", "#,##0.00;", ";0", "0.0x0", "0x0;y0z0", "0%;0%%", "0E0x0", "0.0x#",
        };
        List<String> failures = new ArrayList<String>();
        for (String pattern : bad) {
            boolean jdkRejects = false;
            boolean shimRejects = false;
            try {
                jdk(pattern);
            } catch (IllegalArgumentException e) {
                jdkRejects = true;
            }
            try {
                shim(pattern);
            } catch (IllegalArgumentException e) {
                shimRejects = true;
            }
            if (jdkRejects != shimRejects) {
                failures.add("[" + pattern + "] jdk rejects=" + jdkRejects + " shim rejects=" + shimRejects);
            }
            if (!jdkRejects && !shimRejects) {
                // Accepted by both: then it has to mean the same thing.
                String expected = jdk(pattern).toPattern() + " " + jdk(pattern).format(-1234.5678);
                String actual = shim(pattern).toPattern() + " " + shim(pattern).format(-1234.5678);
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] jdk=" + expected + " shim=" + actual);
                }
            }
        }
        report(failures);
    }

    private static String describe(Number n, int index, int errorIndex) {
        String value = n == null ? "null" : n.getClass().getSimpleName() + ":" + n;
        return value + " index=" + index + " error=" + errorIndex;
    }

    @Test
    public void parsesLikeTheJdk() {
        String[] patterns = {
            "#,##0.###", "0.00", "#", "#%", "0.0%", "#,##0.00;(#,##0.00)", "$#,##0.00", "0.###E0",
            "0 'units'", "#" + PER_MILLE, "-0;+0", "#,####.##",
        };
        String[] texts = {
            "0", "1", "12", "-12", "1,234", "1,234.5", "1,234.50", "-1,234.5", "12.0", "12.50", ".5", "0.5",
            "-0", "-0.0", "1.", "1,", "1,234,", "1,,2", "abc", "", " 1", "1 ", "1abc", "1.2.3", "--1",
            "50%", "12.5%", "200%", "150.0%", "-5%", "(1,234.50)", "(12", "$5.00", "-$5.00", "$", "$x",
            "1E3", "1.5E3", "1E-3", "1E", "1Ex", "-1.25E2", "1e3", "5 units", "5 unit", "5", "+5", "-5",
            "9223372036854775807", "9223372036854775808", "-9223372036854775808", "-9223372036854775809",
            "123456789012345678901234567890", "0.000", "000123", "00.50", "1" + PER_MILLE,
            "1500" + PER_MILLE, "1,2,3", "1.5E400", "1.0E-400", "0E5", "1,234E2",
            String.valueOf((char) 0x221E), "-" + (char) 0x221E, String.valueOf((char) 0xFFFD), "12,34.5",
        };
        List<String> failures = new ArrayList<String>();
        for (String pattern : patterns) {
            for (int integerOnly = 0; integerOnly < 2; integerOnly++) {
                DecimalFormat mine = shim(pattern);
                java.text.DecimalFormat theirs = jdk(pattern);
                mine.setParseIntegerOnly(integerOnly == 1);
                theirs.setParseIntegerOnly(integerOnly == 1);
                for (String text : texts) {
                    for (int from = 0; from < 2; from++) {
                        String input = from == 0 ? text : "x" + text;
                        java.text.ParsePosition theirPosition = new java.text.ParsePosition(from);
                        ParsePosition myPosition = new ParsePosition(from);
                        Number expected = theirs.parse(input, theirPosition);
                        Number actual = mine.parse(input, myPosition);
                        String e = describe(expected, theirPosition.getIndex(), theirPosition.getErrorIndex());
                        String a = describe(actual, myPosition.getIndex(), myPosition.getErrorIndex());
                        if (!e.equals(a)) {
                            failures.add("[" + pattern + (integerOnly == 1 ? ",integerOnly" : "") + "] \""
                                    + input + "\"@" + from + ": jdk=" + e + " shim=" + a);
                        }
                    }
                }
            }
        }
        report(failures);
    }

    @Test
    public void parseOfAStringThrowsWhereTheJdkThrows() throws Exception {
        assertEquals(java.text.NumberFormat.getInstance(Locale.US).parse("1,234.5"),
                NumberFormat.getInstance(US).parse("1,234.5"));
        try {
            NumberFormat.getInstance(US).parse("abc");
            fail("expected a ParseException");
        } catch (java.text.ParseException e) {
            assertEquals(0, e.getErrorOffset());
            assertEquals("Unparseable number: \"abc\"", e.getMessage());
        }
    }

    @Test
    public void factoriesFormatLikeTheJdk() {
        double[] values = {0, 1, -1, 1234567.891, 0.5, 0.125, 0.126, -0.5, 1.5, 2.5, 1e10, 0.0049, 12.345};
        List<String> failures = new ArrayList<String>();
        for (double v : values) {
            compare(failures, "getInstance", v, java.text.NumberFormat.getInstance(Locale.US).format(v),
                    NumberFormat.getInstance(US).format(v));
            compare(failures, "getNumberInstance", v, java.text.NumberFormat.getNumberInstance(Locale.US).format(v),
                    NumberFormat.getNumberInstance(US).format(v));
            compare(failures, "getIntegerInstance", v,
                    java.text.NumberFormat.getIntegerInstance(Locale.US).format(v),
                    NumberFormat.getIntegerInstance(US).format(v));
            compare(failures, "getPercentInstance", v,
                    java.text.NumberFormat.getPercentInstance(Locale.US).format(v),
                    NumberFormat.getPercentInstance(US).format(v));
            if (v >= 0) {
                compare(failures, "getCurrencyInstance", v,
                        java.text.NumberFormat.getCurrencyInstance(Locale.US).format(v),
                        NumberFormat.getCurrencyInstance(US).format(v));
            }
        }
        report(failures);
    }

    private static void compare(List<String> failures, String what, double value, String expected, String actual) {
        if (!expected.equals(actual)) {
            failures.add(what + " " + value + ": jdk=" + expected + " shim=" + actual);
        }
    }

    /// The one intended difference in the factories. JDK 8's locale data
    /// writes a negative US amount in parentheses, `($1.50)`; later JDKs, with
    /// CLDR data, write `-$1.50`. The shim follows the current form.
    @Test
    public void negativeCurrencyUsesAMinusSign() {
        assertEquals("-$1.50", NumberFormat.getCurrencyInstance(US).format(-1.5));
        assertEquals("-$1,234.57", NumberFormat.getCurrencyInstance(US).format(-1234.567));
    }

    @Test
    public void settersBehaveLikeTheJdk() {
        DecimalFormat mine = shim("#,##0.###");
        java.text.DecimalFormat theirs = jdk("#,##0.###");
        mine.setMaximumFractionDigits(1);
        theirs.setMaximumFractionDigits(1);
        assertEquals(theirs.format(1234.56), mine.format(1234.56));
        mine.setMinimumFractionDigits(4);
        theirs.setMinimumFractionDigits(4);
        assertEquals(theirs.format(1234.56), mine.format(1234.56));
        assertEquals(theirs.getMaximumFractionDigits(), mine.getMaximumFractionDigits());
        mine.setGroupingUsed(false);
        theirs.setGroupingUsed(false);
        assertEquals(theirs.format(1234567), mine.format(1234567));
        mine.setMinimumIntegerDigits(10);
        theirs.setMinimumIntegerDigits(10);
        assertEquals(theirs.format(42), mine.format(42));
        mine.setMaximumIntegerDigits(2);
        theirs.setMaximumIntegerDigits(2);
        assertEquals(theirs.format(1997), mine.format(1997));
        assertEquals(theirs.format(1997.5), mine.format(1997.5));
        assertEquals(theirs.getMinimumIntegerDigits(), mine.getMinimumIntegerDigits());
        mine.setGroupingUsed(true);
        theirs.setGroupingUsed(true);
        mine.setGroupingSize(2);
        theirs.setGroupingSize(2);
        mine.setMaximumIntegerDigits(20);
        theirs.setMaximumIntegerDigits(20);
        mine.setMinimumIntegerDigits(1);
        theirs.setMinimumIntegerDigits(1);
        assertEquals(theirs.format(1234567), mine.format(1234567));
        assertEquals(theirs.toPattern(), mine.toPattern());
        mine.setPositivePrefix("+");
        theirs.setPositivePrefix("+");
        mine.setNegativeSuffix(" CR");
        theirs.setNegativeSuffix(" CR");
        mine.setDecimalSeparatorAlwaysShown(true);
        theirs.setDecimalSeparatorAlwaysShown(true);
        assertEquals(theirs.format(5), mine.format(5));
        assertEquals(theirs.format(-5), mine.format(-5));
        assertEquals(theirs.toPattern(), mine.toPattern());
        mine.setMultiplier(7);
        theirs.setMultiplier(7);
        assertEquals(theirs.format(6), mine.format(6));
        assertEquals(theirs.format(0.5), mine.format(0.5));
        assertEquals(theirs.format(Long.MAX_VALUE), mine.format(Long.MAX_VALUE));
        mine.setMultiplier(-3);
        theirs.setMultiplier(-3);
        assertEquals(theirs.format(6), mine.format(6));
        assertEquals(theirs.format(-2.5), mine.format(-2.5));
    }

    @Test
    public void formatsObjectsByTheirType() {
        NumberFormat mine = NumberFormat.getInstance(US);
        java.text.NumberFormat theirs = java.text.NumberFormat.getInstance(Locale.US);
        Object[] numbers = {
            Integer.valueOf(1234), Long.valueOf(-99999L), Short.valueOf((short) 7), Byte.valueOf((byte) -3),
            Double.valueOf(1234.5678), Float.valueOf(0.5f),
        };
        for (Object number : numbers) {
            assertEquals(theirs.format(number), mine.format(number));
        }
        try {
            mine.format("not a number");
            fail("expected an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // The JDK throws the same for a non-number.
        }
    }

    @Test
    public void reportsFieldPositionsLikeTheJdk() {
        String[] patterns = {"#,##0.00", "0.###E0", "$0.0#", "#"};
        double[] values = {1234.5, -0.25, 7, 1e9};
        for (String pattern : patterns) {
            for (double value : values) {
                for (int field = 0; field < 2; field++) {
                    java.text.FieldPosition theirs = new java.text.FieldPosition(field);
                    FieldPosition mine = new FieldPosition(field);
                    String expected = jdk(pattern).format(value, new StringBuffer("ab"), theirs).toString();
                    String actual = shim(pattern).format(value, new StringBuffer("ab"), mine).toString();
                    assertEquals(expected, actual);
                    String where = pattern + " " + value + " field " + field;
                    assertEquals(where, theirs.getBeginIndex(), mine.getBeginIndex());
                    assertEquals(where, theirs.getEndIndex(), mine.getEndIndex());
                }
            }
        }
    }

    @Test
    public void symbolsChangeTheOutput() {
        DecimalFormatSymbols mySymbols = new DecimalFormatSymbols(US);
        java.text.DecimalFormatSymbols theirSymbols = new java.text.DecimalFormatSymbols(Locale.US);
        mySymbols.setDecimalSeparator(',');
        theirSymbols.setDecimalSeparator(',');
        mySymbols.setGroupingSeparator('.');
        theirSymbols.setGroupingSeparator('.');
        mySymbols.setMinusSign('~');
        theirSymbols.setMinusSign('~');
        mySymbols.setPercent('P');
        theirSymbols.setPercent('P');
        mySymbols.setCurrencySymbol("EUR");
        theirSymbols.setCurrencySymbol("EUR");
        mySymbols.setExponentSeparator("x10^");
        theirSymbols.setExponentSeparator("x10^");
        mySymbols.setInfinity("inf");
        theirSymbols.setInfinity("inf");
        mySymbols.setNaN("nan");
        theirSymbols.setNaN("nan");
        String[] patterns = {"#,##0.00", "0.0%", CURRENCY + " #,##0.00", "0.##E0"};
        double[] values = {1234567.891, -0.5, 0.000123, Double.NaN, Double.NEGATIVE_INFINITY};
        for (String pattern : patterns) {
            DecimalFormat mine = new DecimalFormat(pattern, mySymbols);
            java.text.DecimalFormat theirs = new java.text.DecimalFormat(pattern, theirSymbols);
            for (double value : values) {
                assertEquals(pattern + " " + value, theirs.format(value), mine.format(value));
            }
            String text = theirs.format(-1234.5);
            assertEquals(pattern, theirs.parse(text, new java.text.ParsePosition(0)),
                    mine.parse(text, new ParsePosition(0)));
        }
        // The format holds its own copy, as the JDK's does.
        DecimalFormat mine = new DecimalFormat("0.0", mySymbols);
        mySymbols.setDecimalSeparator('!');
        assertEquals("1,5", mine.format(1.5));
        DecimalFormatSymbols handedOut = mine.getDecimalFormatSymbols();
        handedOut.setDecimalSeparator('?');
        assertEquals("1,5", mine.format(1.5));
        mine.setDecimalFormatSymbols(handedOut);
        assertEquals("1?5", mine.format(1.5));
    }

    @Test
    public void cloneIsAnIndependentEqualCopy() {
        DecimalFormat original = shim("#,##0.00;(#)");
        Object copy = original.clone();
        assertTrue(copy instanceof DecimalFormat);
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        DecimalFormat cloned = (DecimalFormat) copy;
        assertEquals(original.format(-1234.5), cloned.format(-1234.5));
        assertEquals(original.toPattern(), cloned.toPattern());
        cloned.setMaximumFractionDigits(0);
        assertFalse(original.equals(cloned));
        assertEquals("1,234.50", original.format(1234.5));
        Object symbolsCopy = new DecimalFormatSymbols(US).clone();
        assertEquals(new DecimalFormatSymbols(US), symbolsCopy);
    }

    @Test
    public void parseFailureLeavesThePositionAlone() {
        ParsePosition position = new ParsePosition(2);
        assertNull(shim("0.00").parse("ab?", position));
        assertEquals(2, position.getIndex());
        assertEquals(2, position.getErrorIndex());
    }

    /// Before Codename One is initialized there is no `L10NManager` to ask,
    /// and every locale gets the US English symbols.
    @Test
    public void symbolsFallBackToUsEnglishWithoutADisplay() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
        assertEquals('.', symbols.getDecimalSeparator());
        assertEquals(',', symbols.getGroupingSeparator());
        assertEquals("$", symbols.getCurrencySymbol());
        assertEquals(new DecimalFormatSymbols(US), symbols);
    }
}
