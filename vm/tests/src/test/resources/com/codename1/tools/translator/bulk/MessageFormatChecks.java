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

import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/// java.text.MessageFormat and ChoiceFormat: the same pattern and arguments
/// give the same text, and the same message the same arguments back.
///
/// A date element runs `DateFormat` as well. Its style based formats are
/// native on Codename One -- the device formats them -- so only a
/// `SimpleDateFormat` pattern is formatted here.
public final class MessageFormatChecks {
    private MessageFormatChecks() {
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

    /// Prints `key=value`, the key escaped as the value is: a pattern here
    /// carries characters outside ASCII. A key that starts with the `[` of
    /// a pattern gets a prefix, because the test drops every line starting
    /// with `[` as a runtime diagnostic.
    private static void p(String key, Object value) {
        Bulk.p(Bulk.esc(key.startsWith("[") ? "p" + key : key), value);
    }

    private static String simpleName(Object o) {
        String n = o.getClass().getName();
        return n.substring(n.lastIndexOf('.') + 1);
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
                    sb.append(simpleName(v)).append(':').append(v);
                }
            }
            sb.append(']');
        }
        return sb.append(" index=").append(index).append(" error=").append(errorIndex).toString();
    }

    /// The message `pattern` makes of `arguments`, or that it was refused.
    private static String formatted(String pattern, Object[] arguments) {
        try {
            return new MessageFormat(pattern, Locale.US).format(arguments);
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException";
        }
    }

    /// Runs the group. The default zone is pinned to UTC as well as the
    /// locale: a date pattern formats in the default zone, and the HotSpot
    /// reference would otherwise take the machine's zone where the
    /// translated program does not.
    public static void run() {
        Locale saved = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            messages();
            choices();
        } finally {
            TimeZone.setDefault(savedZone);
            Locale.setDefault(saved);
        }
    }

    private static void messages() {
        Bulk.check("MessageFormat.formatsLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                for (Object[] arguments : ARGUMENTS) {
                    p("[" + pattern + "] " + Arrays.toString(arguments), formatted(pattern, arguments));
                }
            }
        });
        Bulk.check("MessageFormat.staticFormatMatchesTheJdk", () -> {
            p("twoArguments", MessageFormat.format("{0} of {1}", "a", Integer.valueOf(12345)));
            p("noArguments", MessageFormat.format("no arguments", new Object[0]));
            p("nullArguments", MessageFormat.format("{0}", (Object[]) null));
        });
        Bulk.check("MessageFormat.rejectsWhatTheJdkRejects", () -> {
            String[] patterns = {
                "{", "{0", "{x}", "{-1}", "{0,bogus}", "{0,number,#.#.#}", "{0}}", "}", "{ 0 }", "{0,}", "{}",
                "{0,number", "{0,choice,1#a|0#b}", "{1.5}",
                // Ending inside a nested brace is NOT rejected by the JDK (8 through 25).
                "{0,choice,0#{", "a{0}b{0,choice,0#{1", "x{0,choice,0#{1}", "{0,choice,0#'{'",
            };
            for (String pattern : patterns) {
                p("[" + pattern + "]", formatted(pattern, new Object[] {Integer.valueOf(1)}));
            }
        });
        Bulk.check("MessageFormat.reproducesPatternsLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                p("[" + pattern + "] toPattern", new MessageFormat(pattern, Locale.US).toPattern());
            }
        });
        Bulk.check("MessageFormat.formatsDatesLikeTheJdk", () -> {
            Date when = new Date(1700000000000L);
            String[] patterns = {
                "{0,date}", "{0,date,short}", "{0,date,medium}", "{0,date,long}", "{0,time}", "{0,time,short}",
                "{0,date,yyyy-MM-dd}", "{0,time,HH:mm}", "{0}", "{0, date, full}", "{0,time,long}",
            };
            for (String pattern : patterns) {
                // Only a pattern of letters is formatted here: a style is
                // formatted by the device, through a native method.
                if (pattern.indexOf('-') >= 0 || pattern.indexOf(':') >= 0) {
                    p("[" + pattern + "]", new MessageFormat(pattern, Locale.US).format(new Object[] {when}));
                }
                p("[" + pattern + "] toPattern", new MessageFormat(pattern, Locale.US).toPattern());
            }
        });
        Bulk.check("MessageFormat.parsesLikeTheJdk", () -> {
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
            for (String[] c : cases) {
                for (int from = 0; from < 2; from++) {
                    String input = from == 0 ? c[1] : "x" + c[1];
                    ParsePosition position = new ParsePosition(from);
                    Object[] values = new MessageFormat(c[0], Locale.US).parse(input, position);
                    p("[" + c[0] + "] \"" + input + "\"@" + from,
                            describe(values, position.getIndex(), position.getErrorIndex()));
                }
            }
        });
        Bulk.check("MessageFormat.parseOfAStringThrowsWhereTheJdkThrows", () -> {
            String pattern = "{0} of {1}";
            p("parse", Arrays.toString(new MessageFormat(pattern).parse("a of b")));
            p("parseObject", Arrays.toString((Object[]) new MessageFormat(pattern).parseObject("a of b")));
            try {
                new MessageFormat("[{0}]").parse("{x");
                p("parsed", true);
            } catch (ParseException e) {
                p("errorOffset", e.getErrorOffset());
            }
        });
        Bulk.check("MessageFormat.formatsAreReadAndSetLikeTheJdk", () -> {
            String pattern = "{1,number,integer} and {0} and {1,choice,0#none|1#some}";
            MessageFormat f = new MessageFormat(pattern, Locale.US);
            Format[] formats = f.getFormats();
            p("formats", formats.length);
            for (int i = 0; i < formats.length; i++) {
                p("format " + i, formats[i] == null ? "null" : simpleName(formats[i]));
            }
            Format[] byArgument = f.getFormatsByArgumentIndex();
            p("byArgument", byArgument.length);
            p("byArgument0", byArgument[0] == null ? "null" : simpleName(byArgument[0]));
            p("byArgument1IsChoice", byArgument[1] instanceof ChoiceFormat);

            Object[] arguments = {"x", Double.valueOf(0.25)};
            // Element 0 is the {1,number,integer}, which formats the 0.25.
            f.setFormat(0, NumberFormat.getPercentInstance(Locale.US));
            p("setFormat", f.format(arguments));
            p("setFormatPattern", f.toPattern());

            f.setFormatByArgumentIndex(1, NumberFormat.getIntegerInstance(Locale.US));
            p("setFormatByArgumentIndex", f.format(arguments));
            p("setFormatByArgumentIndexPattern", f.toPattern());

            f.setFormats(new Format[] {null, null, null});
            p("setFormats", f.format(arguments));
            p("setFormatsPattern", f.toPattern());
        });
        Bulk.check("MessageFormat.appendsAndComparesLikeAFormat", () -> {
            MessageFormat format = new MessageFormat("{0}-{1}");
            StringBuffer out = new StringBuffer("> ");
            format.format(new Object[] {"a", "b"}, out, new FieldPosition(0));
            p("appended", out.toString());
            p("equal", format.equals(new MessageFormat("{0}-{1}")));
            p("sameHash", format.hashCode() == new MessageFormat("{0}-{1}").hashCode());
            p("notEqual", format.equals(new MessageFormat("{0}+{1}")));
            Object copy = format.clone();
            p("cloneEqual", format.equals(copy));
            format.applyPattern("{0}");
            p("cloneEqualAfterApply", format.equals(copy));
        });
    }

    private static void choices() {
        Bulk.check("MessageFormat.choosesLikeTheJdk", () -> {
            for (String pattern : CHOICES) {
                ChoiceFormat f = new ChoiceFormat(pattern);
                for (double value : CHOICE_VALUES) {
                    p("[" + pattern + "] " + value, f.format(value));
                }
                p("[" + pattern + "] 3L", f.format(3L));
                p("[" + pattern + "] toPattern", f.toPattern());
                p("[" + pattern + "] limits", Arrays.toString(f.getLimits()));
                p("[" + pattern + "] formats", Arrays.toString(f.getFormats()));
            }
        });
        Bulk.check("MessageFormat.choiceFromArraysMatchesTheJdk", () -> {
            double[] limits = {0, 1, ChoiceFormat.nextDouble(1)};
            String[] formats = {"none", "single", "several"};
            ChoiceFormat f = new ChoiceFormat(limits, formats);
            p("toPattern", f.toPattern());
            for (double value : CHOICE_VALUES) {
                p("" + value, f.format(value));
            }
            p("limits", Arrays.toString(f.getLimits()));
            Bulk.pThrown("differentLengths", IllegalArgumentException.class,
                    () -> new ChoiceFormat(new double[] {0, 1}, new String[] {"a"}));
        });
        Bulk.check("MessageFormat.choiceParsesLikeTheJdk", () -> {
            ChoiceFormat f = new ChoiceFormat("0#zero|1#one|2#two|2<twenty");
            for (String text : new String[] {"zero", "one", "two", "twenty", "three", "", "onex", "tw"}) {
                ParsePosition position = new ParsePosition(0);
                Number n = f.parse(text, position);
                p(text, String.valueOf(n) + " index=" + position.getIndex() + " error="
                        + position.getErrorIndex());
            }
        });
        Bulk.check("MessageFormat.nextAndPreviousDoubleMatchTheJdk", () -> {
            double[] values = {
                0.0, -0.0, 1.0, -1.0, 0.1, 1e300, -1e300, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 4.9E-324,
                2.2250738585072014E-308,
            };
            for (double value : values) {
                p("next " + value, Double.doubleToLongBits(ChoiceFormat.nextDouble(value)));
                p("previous " + value, Double.doubleToLongBits(ChoiceFormat.previousDouble(value)));
            }
            p("next NaN", Double.doubleToLongBits(ChoiceFormat.nextDouble(Double.NaN)));
        });
        Bulk.check("MessageFormat.positionsBehaveLikeTheJdk", () -> {
            ParsePosition position = new ParsePosition(3);
            p("index", position.getIndex());
            p("errorIndex", position.getErrorIndex());
            position.setErrorIndex(5);
            p("hash", new ParsePosition(3).hashCode());
            p("equalWithError", position.equals(new ParsePosition(3)));
            position.setErrorIndex(-1);
            p("equalWithoutError", position.equals(new ParsePosition(3)));

            FieldPosition field = new FieldPosition(NumberFormat.FRACTION_FIELD);
            p("field", field.getField());
            p("integerField", NumberFormat.INTEGER_FIELD);
            field.setBeginIndex(2);
            field.setEndIndex(4);
            p("begin", field.getBeginIndex());
            p("end", field.getEndIndex());
        });
    }
}
