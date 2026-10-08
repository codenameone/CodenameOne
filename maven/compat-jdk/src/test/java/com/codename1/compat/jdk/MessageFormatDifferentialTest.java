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

import com.codename1.compat.testing.ReferenceJdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/// Holds the `MessageFormat` and `ChoiceFormat` shims against the JDK's own
/// classes: the same pattern and arguments have to give the same text.
public class MessageFormatDifferentialTest {

    private Locale saved;

    @Before
    public void useUsLocale() {
        saved = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(saved);
    }

    private static final String[] PATTERNS = {
        "plain text",
        "{0}",
        "Hello {0}, you have {1} messages",
        "{1} before {0}",
        "{0}{0}{0}",
        "'{0}' is literal, {0} is not",
        "It''s {0}",
        "'quoted '' quote' {0}",
        "a '{' b {0} c '}' d",
        "{0,number}",
        "{0,number,integer}",
        "{0,number,percent}",
        "{0,number,currency}",
        "{0,number,#.##}",
        "{0,number,000.0}",
        "{0,number,#,##0.00;(#,##0.00)}",
        "{0, number , integer }",
        "{0,choice,0#no files|1#one file|1<{0,number,integer} files}",
        "{0,choice,-1#negative|0#zero or more}",
        "missing {5} argument",
        "{2}",
        "trailing quote '",
        "{0} and '{1}' and {1}",
        "{0,number,'#'#}",
        "",
    };

    private static final Object[][] ARGUMENTS = {
        {},
        {"World", Integer.valueOf(3)},
        {Integer.valueOf(0), "x"},
        {Integer.valueOf(1), "x"},
        {Integer.valueOf(2), "x"},
        {Double.valueOf(1234.5678), Long.valueOf(-7)},
        {Double.valueOf(-0.256), null},
        {null, null, null},
        {Long.valueOf(1234567), "it's", Boolean.TRUE},
        {"text", "more"},
        {Integer.valueOf(-5)},
    };

    @Test
    public void formatsLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            for (Object[] arguments : ARGUMENTS) {
                String expected;
                try {
                    expected = new java.text.MessageFormat(pattern, Locale.US).format(arguments);
                } catch (IllegalArgumentException e) {
                    expected = "IllegalArgumentException";
                }
                String actual;
                try {
                    actual = new MessageFormat(pattern, Locale.US).format(arguments);
                } catch (IllegalArgumentException e) {
                    actual = "IllegalArgumentException";
                }
                if (pattern.indexOf("currency") >= 0 && expected.startsWith("(")) {
                    // Intended difference. A JDK reading its own locale
                    // data writes a negative amount in parentheses; the shim
                    // writes the minus sign that current locale data asks
                    // for, and that newer JDKs write too.
                    expected = "-" + expected.substring(1, expected.length() - 1);
                }
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + Arrays.toString(arguments) + ": jdk=" + expected
                            + " shim=" + actual);
                }
            }
        }
        report(failures);
    }

    @Test
    public void staticFormatMatchesTheJdk() {
        assertEquals(java.text.MessageFormat.format("{0} of {1}", "a", Integer.valueOf(12345)),
                MessageFormat.format("{0} of {1}", "a", Integer.valueOf(12345)));
        assertEquals(java.text.MessageFormat.format("no arguments", new Object[0]),
                MessageFormat.format("no arguments", new Object[0]));
        assertEquals(java.text.MessageFormat.format("{0}", (Object[]) null),
                MessageFormat.format("{0}", (Object[]) null));
    }

    @Test
    public void rejectsWhatTheJdkRejects() {
        String[] patterns = {
            "{", "{0", "{x}", "{-1}", "{0,bogus}", "{0,number,#.#.#}", "{0}}", "}", "{ 0 }", "{0,}", "{}",
            "{0,number", "{0,choice,1#a|0#b}", "{1.5}",
        };
        List<String> failures = new ArrayList<String>();
        for (String pattern : patterns) {
            boolean jdkRejects = false;
            boolean shimRejects = false;
            String expected = null;
            String actual = null;
            try {
                expected = new java.text.MessageFormat(pattern, Locale.US).format(new Object[] {Integer.valueOf(1)});
            } catch (IllegalArgumentException e) {
                jdkRejects = true;
            }
            try {
                actual = new MessageFormat(pattern, Locale.US).format(new Object[] {Integer.valueOf(1)});
            } catch (IllegalArgumentException e) {
                shimRejects = true;
            }
            if (jdkRejects != shimRejects) {
                failures.add("[" + pattern + "] jdk rejects=" + jdkRejects + " shim rejects=" + shimRejects);
            } else if (!jdkRejects && !expected.equals(actual)) {
                failures.add("[" + pattern + "] jdk=" + expected + " shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void reproducesPatternsLikeTheJdk() {
        ReferenceJdk.assume("quotes the braces of a pattern nested in a choice when it writes the pattern back");
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            String expected = new java.text.MessageFormat(pattern, Locale.US).toPattern();
            String actual = new MessageFormat(pattern, Locale.US).toPattern();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] toPattern: jdk=" + expected + " shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void formatsDatesLikeTheJdk() {
        Date when = new Date(1700000000000L);
        String[] patterns = {
            "{0,date}", "{0,date,short}", "{0,date,medium}", "{0,date,long}", "{0,time}", "{0,time,short}",
            "{0,date,yyyy-MM-dd}", "{0,time,HH:mm}", "{0}", "{0, date, full}", "{0,time,long}",
        };
        List<String> failures = new ArrayList<String>();
        for (String pattern : patterns) {
            String expected = new java.text.MessageFormat(pattern, Locale.US).format(new Object[] {when});
            String actual = new MessageFormat(pattern, Locale.US).format(new Object[] {when});
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] jdk=" + expected + " shim=" + actual);
            }
            expected = new java.text.MessageFormat(pattern, Locale.US).toPattern();
            actual = new MessageFormat(pattern, Locale.US).toPattern();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] toPattern: jdk=" + expected + " shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void appendsAndComparesLikeAFormat() {
        MessageFormat format = new MessageFormat("{0}-{1}");
        StringBuffer out = new StringBuffer("> ");
        format.format(new Object[] {"a", "b"}, out, new FieldPosition(0));
        assertEquals("> a-b", out.toString());
        assertEquals(format, new MessageFormat("{0}-{1}"));
        assertEquals(format.hashCode(), new MessageFormat("{0}-{1}").hashCode());
        assertNotEquals(format, new MessageFormat("{0}+{1}"));
        Object copy = format.clone();
        assertEquals(format, copy);
        format.applyPattern("{0}");
        assertNotEquals(format, copy);
    }

    // ------------------------------------------------------------------
    // ChoiceFormat
    // ------------------------------------------------------------------

    // Written as casts: the sources are ASCII.
    private static final char INFINITY = (char) 0x221E;
    private static final char AT_LEAST = (char) 0x2264;

    private static final String[] CHOICES = {
        "0#zero|1#one|2#two",
        "0#no files|1#one file|1<many files",
        "-1#negative|0#zero|0<positive",
        "1#only",
        "0.5#half|1.5#one and a half",
        "-" + INFINITY + "#low|0#mid|100#high",
        "0#a''b|1#'c|d'",
        "1" + AT_LEAST + "at least one|5<more than five",
    };

    private static final double[] CHOICE_VALUES = {
        -1000, -1, -0.5, 0, 0.25, 0.5, 1, 1.0000001, 1.5, 2, 5, 5.5, 99, 100, 1e9,
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
    };

    @Test
    public void choosesLikeTheJdk() {
        List<String> failures = new ArrayList<String>();
        for (String pattern : CHOICES) {
            java.text.ChoiceFormat theirs = new java.text.ChoiceFormat(pattern);
            ChoiceFormat mine = new ChoiceFormat(pattern);
            for (double value : CHOICE_VALUES) {
                String expected = theirs.format(value);
                String actual = mine.format(value);
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + value + ": jdk=" + expected + " shim=" + actual);
                }
            }
            if (!theirs.format(3L).equals(mine.format(3L))) {
                failures.add("[" + pattern + "] 3L");
            }
            if (!theirs.toPattern().equals(mine.toPattern())) {
                failures.add("[" + pattern + "] toPattern: jdk=" + theirs.toPattern() + " shim=" + mine.toPattern());
            }
            if (!Arrays.equals(theirs.getLimits(), mine.getLimits())) {
                failures.add("[" + pattern + "] limits: jdk=" + Arrays.toString(theirs.getLimits()) + " shim="
                        + Arrays.toString(mine.getLimits()));
            }
            if (!Arrays.equals(theirs.getFormats(), mine.getFormats())) {
                failures.add("[" + pattern + "] formats");
            }
        }
        report(failures);
    }

    @Test
    public void choiceFromArraysMatchesTheJdk() {
        double[] limits = {0, 1, java.text.ChoiceFormat.nextDouble(1)};
        String[] formats = {"none", "single", "several"};
        java.text.ChoiceFormat theirs = new java.text.ChoiceFormat(limits, formats);
        ChoiceFormat mine = new ChoiceFormat(limits, formats);
        assertEquals(theirs.toPattern(), mine.toPattern());
        for (double value : CHOICE_VALUES) {
            assertEquals(theirs.format(value), mine.format(value));
        }
        assertArrayEquals(theirs.getLimits(), mine.getLimits(), 0.0);
        try {
            new ChoiceFormat(new double[] {0, 1}, new String[] {"a"});
            fail("limits and formats of different lengths");
        } catch (IllegalArgumentException expected) {
            // As the JDK.
        }
    }

    @Test
    public void choiceParsesLikeTheJdk() {
        String pattern = "0#zero|1#one|2#two|2<twenty";
        java.text.ChoiceFormat theirs = new java.text.ChoiceFormat(pattern);
        ChoiceFormat mine = new ChoiceFormat(pattern);
        for (String text : new String[] {"zero", "one", "two", "twenty", "three", "", "onex", "tw"}) {
            java.text.ParsePosition theirPosition = new java.text.ParsePosition(0);
            ParsePosition myPosition = new ParsePosition(0);
            Number expected = theirs.parse(text, theirPosition);
            Number actual = mine.parse(text, myPosition);
            assertEquals(text, String.valueOf(expected), String.valueOf(actual));
            assertEquals(text, theirPosition.getIndex(), myPosition.getIndex());
            assertEquals(text, theirPosition.getErrorIndex(), myPosition.getErrorIndex());
        }
    }

    @Test
    public void nextAndPreviousDoubleMatchTheJdk() {
        double[] values = {
            0.0, -0.0, 1.0, -1.0, 0.1, 1e300, -1e300, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 4.9E-324, 2.2250738585072014E-308,
        };
        for (double value : values) {
            assertEquals(Double.doubleToLongBits(java.text.ChoiceFormat.nextDouble(value)),
                    Double.doubleToLongBits(ChoiceFormat.nextDouble(value)));
            assertEquals(Double.doubleToLongBits(java.text.ChoiceFormat.previousDouble(value)),
                    Double.doubleToLongBits(ChoiceFormat.previousDouble(value)));
        }
        assertEquals(Double.doubleToLongBits(java.text.ChoiceFormat.nextDouble(Double.NaN)),
                Double.doubleToLongBits(ChoiceFormat.nextDouble(Double.NaN)));
    }

    @Test
    public void positionsBehaveLikeTheJdk() {
        ParsePosition position = new ParsePosition(3);
        assertEquals(3, position.getIndex());
        assertEquals(-1, position.getErrorIndex());
        position.setErrorIndex(5);
        assertEquals(new java.text.ParsePosition(3).hashCode(), new ParsePosition(3).hashCode());
        assertNotEquals(position, new ParsePosition(3));
        position.setErrorIndex(-1);
        assertEquals(position, new ParsePosition(3));

        FieldPosition field = new FieldPosition(NumberFormat.FRACTION_FIELD);
        assertEquals(java.text.NumberFormat.FRACTION_FIELD, field.getField());
        assertEquals(java.text.NumberFormat.INTEGER_FIELD, NumberFormat.INTEGER_FIELD);
        field.setBeginIndex(2);
        field.setEndIndex(4);
        assertEquals(2, field.getBeginIndex());
        assertEquals(4, field.getEndIndex());
    }

    private static void report(List<String> failures) {
        if (failures.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(failures.size()).append(" differences from the JDK:\n");
        for (int i = 0; i < failures.size() && i < 60; i++) {
            sb.append(failures.get(i)).append('\n');
        }
        fail(sb.toString());
    }
}
