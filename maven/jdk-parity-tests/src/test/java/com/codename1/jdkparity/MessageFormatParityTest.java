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
package com.codename1.jdkparity;

import jdkparity.java.text.ChoiceFormat;
import jdkparity.java.text.FieldPosition;
import jdkparity.java.text.MessageFormat;
import jdkparity.java.text.NumberFormat;
import jdkparity.java.text.ParsePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Holds JavaAPI's `MessageFormat` and `ChoiceFormat`, relocated to
/// `jdkparity.java.text`, against the JDK's own classes: the same pattern and
/// arguments have to give the same text, and the same message the same
/// arguments back.
///
/// JavaAPI's `java.text` is relocated whole, so a date element runs JavaAPI's
/// `DateFormat` as well. Its style based formats are native -- the device
/// formats them -- and only its `SimpleDateFormat` patterns can run here.
public class MessageFormatParityTest {

    private static final boolean JAVA_8 = "1.8".equals(System.getProperty("java.specification.version"));

    private Locale saved;

    @BeforeEach
    public void useUsLocale() {
        saved = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
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
                    // data writes a negative amount in parentheses; JavaAPI
                    // writes the minus sign that current locale data asks
                    // for, and that newer JDKs write too.
                    expected = "-" + expected.substring(1, expected.length() - 1);
                }
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] " + Arrays.toString(arguments) + ": jdk=" + expected
                            + " javaapi=" + actual);
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
            // Ending inside a nested brace is NOT rejected by the JDK (8 through 25).
            "{0,choice,0#{", "a{0}b{0,choice,0#{1", "x{0,choice,0#{1}", "{0,choice,0#'{'",
        };
        List<String> failures = new ArrayList<String>();
        for (String pattern : patterns) {
            boolean jdkRejects = false;
            boolean apiRejects = false;
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
                apiRejects = true;
            }
            if (jdkRejects != apiRejects) {
                failures.add("[" + pattern + "] jdk rejects=" + jdkRejects + " javaapi rejects=" + apiRejects);
            } else if (!jdkRejects && !expected.equals(actual)) {
                failures.add("[" + pattern + "] jdk=" + expected + " javaapi=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void reproducesPatternsLikeTheJdk() {
        assumeTrue(JAVA_8, "After Java 8 the JDK quotes the braces of a pattern nested in a choice when it writes the pattern back");
        List<String> failures = new ArrayList<String>();
        for (String pattern : PATTERNS) {
            String expected = new java.text.MessageFormat(pattern, Locale.US).toPattern();
            String actual = new MessageFormat(pattern, Locale.US).toPattern();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] toPattern: jdk=" + expected + " javaapi=" + actual);
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
            String expected;
            String actual;
            // Only a pattern of letters is formatted here: a style is
            // formatted by the device, through a native method.
            if (pattern.indexOf('-') >= 0 || pattern.indexOf(':') >= 0) {
                expected = new java.text.MessageFormat(pattern, Locale.US).format(new Object[] {when});
                actual = new MessageFormat(pattern, Locale.US).format(new Object[] {when});
                if (!expected.equals(actual)) {
                    failures.add("[" + pattern + "] jdk=" + expected + " javaapi=" + actual);
                }
            }
            expected = new java.text.MessageFormat(pattern, Locale.US).toPattern();
            actual = new MessageFormat(pattern, Locale.US).toPattern();
            if (!expected.equals(actual)) {
                failures.add("[" + pattern + "] toPattern: jdk=" + expected + " javaapi=" + actual);
            }
        }
        report(failures);
    }

    private static String describe(Object[] values, int index, int errorIndex) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < values.length; i++) {
                Object v = values[i];
                if (i > 0) {
                    sb.append(", ");
                }
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Date) {
                    sb.append("Date:").append(((Date) v).getTime());
                } else {
                    sb.append(v.getClass().getSimpleName()).append(':').append(v);
                }
            }
            sb.append(']');
        }
        return sb.append(" index=").append(index).append(" error=").append(errorIndex).toString();
    }

    @Test
    public void parsesLikeTheJdk() {
        String[][] cases = {
            {"Hello {0}, you have {1} messages", "Hello World, you have 3 messages"},
            {"Hello {0}, you have {1} messages", "Hello World, you have 3"},
            {"Hello {0}, you have {1} messages", "Goodbye World"},
            {"{0}{1}", "ab"},
            {"{1} before {0}", "x before y"},
            {"{0} and {0}", "a and b"},
            {"{0,number} items", "1,234.5 items"},
            {"{0,number} items", "lots of items"},
            {"{0,number,integer}-{1}", "42-rest"},
            {"{0,number,percent}", "12%"},
            {"{0,choice,0#none|1#one|2#many}!", "one!"},
            {"{0,choice,0#none|1#one|2#many}!", "several!"},
            {"{0,date,yyyy-MM-dd} at {1}", "2023-11-14 at noon"},
            {"It''s {0}", "It's me"},
            {"plain text", "plain text and more"},
            {"plain text", "plain"},
            {"missing {5} argument", "missing {5} argument"},
            {"", "anything"},
        };
        List<String> failures = new ArrayList<String>();
        for (String[] c : cases) {
            for (int from = 0; from < 2; from++) {
                String input = from == 0 ? c[1] : "x" + c[1];
                java.text.ParsePosition theirPosition = new java.text.ParsePosition(from);
                ParsePosition myPosition = new ParsePosition(from);
                Object[] expected = new java.text.MessageFormat(c[0], Locale.US).parse(input, theirPosition);
                Object[] actual = new MessageFormat(c[0], Locale.US).parse(input, myPosition);
                String e = describe(expected, theirPosition.getIndex(), theirPosition.getErrorIndex());
                String a = describe(actual, myPosition.getIndex(), myPosition.getErrorIndex());
                if (!e.equals(a)) {
                    failures.add("[" + c[0] + "] \"" + input + "\"@" + from + ": jdk=" + e + " javaapi=" + a);
                }
            }
        }
        report(failures);
    }

    @Test
    public void parseOfAStringThrowsWhereTheJdkThrows() throws Exception {
        String pattern = "{0} of {1}";
        assertEquals(Arrays.toString(new java.text.MessageFormat(pattern).parse("a of b")),
                Arrays.toString(new MessageFormat(pattern).parse("a of b")));
        assertEquals(Arrays.toString((Object[]) new java.text.MessageFormat(pattern).parseObject("a of b")),
                Arrays.toString((Object[]) new MessageFormat(pattern).parseObject("a of b")));
        int expectedOffset = -2;
        try {
            new java.text.MessageFormat("[{0}]").parse("{x");
        } catch (java.text.ParseException e) {
            expectedOffset = e.getErrorOffset();
        }
        try {
            new MessageFormat("[{0}]").parse("{x");
            fail("expected a ParseException");
        } catch (jdkparity.java.text.ParseException e) {
            assertEquals(expectedOffset, e.getErrorOffset());
        }
    }

    @Test
    public void formatsAreReadAndSetLikeTheJdk() {
        String pattern = "{1,number,integer} and {0} and {1,choice,0#none|1#some}";
        java.text.MessageFormat theirs = new java.text.MessageFormat(pattern, Locale.US);
        MessageFormat mine = new MessageFormat(pattern, Locale.US);
        java.text.Format[] theirFormats = theirs.getFormats();
        jdkparity.java.text.Format[] myFormats = mine.getFormats();
        assertEquals(theirFormats.length, myFormats.length);
        for (int i = 0; i < theirFormats.length; i++) {
            assertEquals(theirFormats[i] == null ? "null" : theirFormats[i].getClass().getSimpleName(),
                    myFormats[i] == null ? "null" : myFormats[i].getClass().getSimpleName(), "format " + i);
        }
        assertEquals(theirs.getFormatsByArgumentIndex().length, mine.getFormatsByArgumentIndex().length);
        assertNull(mine.getFormatsByArgumentIndex()[0]);
        assertTrue(mine.getFormatsByArgumentIndex()[1] instanceof ChoiceFormat);

        Object[] arguments = {"x", Double.valueOf(0.25)};
        // Element 0 is the {1,number,integer}, which formats the 0.25.
        theirs.setFormat(0, java.text.NumberFormat.getPercentInstance(Locale.US));
        mine.setFormat(0, NumberFormat.getPercentInstance(Locale.US));
        assertEquals(theirs.format(arguments), mine.format(arguments));
        assertEquals(theirs.toPattern(), mine.toPattern());

        theirs.setFormatByArgumentIndex(1, java.text.NumberFormat.getIntegerInstance(Locale.US));
        mine.setFormatByArgumentIndex(1, NumberFormat.getIntegerInstance(Locale.US));
        assertEquals(theirs.format(arguments), mine.format(arguments));
        assertEquals(theirs.toPattern(), mine.toPattern());

        theirs.setFormats(new java.text.Format[] {null, null, null});
        mine.setFormats(new jdkparity.java.text.Format[] {null, null, null});
        assertEquals(theirs.format(arguments), mine.format(arguments));
        assertEquals(theirs.toPattern(), mine.toPattern());
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
                    failures.add("[" + pattern + "] " + value + ": jdk=" + expected + " javaapi=" + actual);
                }
            }
            if (!theirs.format(3L).equals(mine.format(3L))) {
                failures.add("[" + pattern + "] 3L");
            }
            if (!theirs.toPattern().equals(mine.toPattern())) {
                failures.add("[" + pattern + "] toPattern: jdk=" + theirs.toPattern() + " javaapi=" + mine.toPattern());
            }
            if (!Arrays.equals(theirs.getLimits(), mine.getLimits())) {
                failures.add("[" + pattern + "] limits: jdk=" + Arrays.toString(theirs.getLimits()) + " javaapi="
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
            assertEquals(String.valueOf(expected), String.valueOf(actual), text);
            assertEquals(theirPosition.getIndex(), myPosition.getIndex(), text);
            assertEquals(theirPosition.getErrorIndex(), myPosition.getErrorIndex(), text);
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
