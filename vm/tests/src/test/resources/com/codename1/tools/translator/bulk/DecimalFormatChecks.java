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

import com.codename1.impl.JdkHost;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

/// java.text.DecimalFormat, NumberFormat and DecimalFormatSymbols: the same
/// patterns and values give the same text, the same parsed numbers and the
/// same parse positions.
///
/// JavaAPI keeps Java 8's US English symbols where the JDK's locale data
/// changed them later (CLDR from Java 9), and the HotSpot reference is a
/// later JDK. Where that is the only difference -- NaN is written U+FFFD by
/// Java 8 and `NaN` by CLDR -- the HotSpot run sets the Java 8 symbol on its
/// own symbols ([#usSymbols]) so the formatting itself is still compared,
/// and [#java8UsSymbols] pins JavaAPI's defaults to Java 8's on ParparVM.
public final class DecimalFormatChecks {
    private DecimalFormatChecks() {
    }

    private static final Locale US = new Locale("en", "US");
    private static final String CURRENCY = String.valueOf((char) 0xA4);
    private static final String PER_MILLE = String.valueOf((char) 0x2030);
    private static final String JAVA8_NAN = String.valueOf((char) 0xFFFD);

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

    /// The US English symbols as Java 8 has them: on HotSpot the JDK's, with
    /// the one symbol CLDR changed set back; on ParparVM JavaAPI's own.
    private static DecimalFormatSymbols usSymbols() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(US);
        if (Bulk.onHotSpot()) {
            s.setNaN(JAVA8_NAN);
        }
        return s;
    }

    private static DecimalFormat format(String pattern) {
        return new DecimalFormat(pattern, usSymbols());
    }

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

    /// The pattern's meaning as `toPattern` and a formatted value, or
    /// `rejected`.
    private static String malformed(String pattern) {
        try {
            DecimalFormat f = format(pattern);
            // Accepted: then it has to mean the same thing.
            return f.toPattern() + " " + f.format(-1234.5678);
        } catch (IllegalArgumentException e) {
            return "rejected";
        }
    }

    /// US English with every symbol a format writes changed.
    private static DecimalFormatSymbols changedSymbols() {
        DecimalFormatSymbols symbols = usSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        symbols.setMinusSign('~');
        symbols.setPercent('P');
        symbols.setCurrencySymbol("EUR");
        symbols.setExponentSeparator("x10^");
        symbols.setInfinity("inf");
        symbols.setNaN("nan");
        return symbols;
    }

    /// java.util.Random's generator, written out so that the values do not
    /// depend on the Random under test elsewhere.
    private static final class DecimalFormatRandom {
        private long seed;

        DecimalFormatRandom(long seed) {
            this.seed = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
        }

        private int next(int bits) {
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            return (int) (seed >>> (48 - bits));
        }

        long nextLong() {
            return ((long) next(32) << 32) + next(32);
        }

        double nextDouble() {
            return (((long) next(26) << 27) + next(27)) * 0x1.0p-53;
        }

        int nextInt(int bound) {
            int r = next(31);
            int m = bound - 1;
            if ((bound & m) == 0) {
                return (int) ((bound * (long) r) >> 31);
            }
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
                // rejected: retry
            }
            return r;
        }
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("DecimalFormat.formatsDoublesLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                DecimalFormat f = format(pattern);
                for (double value : DOUBLES) {
                    p("[" + pattern + "] " + value, f.format(value));
                }
            }
        });
        Bulk.check("DecimalFormat.formatsLongsLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                DecimalFormat f = format(pattern);
                for (long value : LONGS) {
                    p("[" + pattern + "] " + value + "L", f.format(value));
                }
            }
        });
        // Random values across the whole exponent range, and values built to
        // sit on or next to a rounding boundary, where half-even and the
        // binary representation both matter.
        Bulk.check("DecimalFormat.formatsRandomDoublesLikeTheJdk", () -> {
            String[] patterns = {
                "0", "0.0", "0.00", "0.000", "#.####", "#,##0.00", "0.###E0", "##0.##E0", "0.00%", "0.0000000",
            };
            DecimalFormatRandom random = new DecimalFormatRandom(20240607L);
            for (String pattern : patterns) {
                DecimalFormat f = format(pattern);
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
                    p("[" + pattern + "] " + value, f.format(value));
                }
            }
        });
        Bulk.check("DecimalFormat.reproducesPatternsLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                if (pattern.length() == 0) {
                    // The empty pattern allows any number of fraction digits,
                    // and the JDK runs out of memory writing a '#' for each.
                    continue;
                }
                p("[" + pattern + "] toPattern", format(pattern).toPattern());
            }
        });
        Bulk.check("DecimalFormat.readsPatternSettingsLikeTheJdk", () -> {
            for (String pattern : PATTERNS) {
                DecimalFormat m = format(pattern);
                p("[" + pattern + "] settings", m.getMinimumIntegerDigits() + "/" + m.getMaximumIntegerDigits()
                        + "/" + m.getMinimumFractionDigits() + "/" + m.getMaximumFractionDigits() + "/"
                        + m.isGroupingUsed() + "/" + m.getGroupingSize() + "/" + m.getMultiplier() + "/"
                        + m.isDecimalSeparatorAlwaysShown() + "/" + m.getPositivePrefix() + "/"
                        + m.getPositiveSuffix() + "/" + m.getNegativePrefix() + "/" + m.getNegativeSuffix());
            }
        });
        Bulk.check("DecimalFormat.rejectsTheMalformedPatternsTheJdkRejects", () -> {
            String[] bad = {
                "0.0.0", "#0#", "0#.0", "0.#0", "0E", "E0", "0E0E0", "#,", "0;0;0", "0'", "%%0", "0;",
                "0.0#0", "#,##0.00;", ";0", "0%;0%%",
            };
            for (String pattern : bad) {
                p("[" + pattern + "]", malformed(pattern));
            }
        });
        // Text between the digits of a pattern: after Java 8 the JDK reads it
        // as a suffix of both subpatterns, where Java 8 (and JavaAPI) end the
        // positive subpattern's digits at it. The Java 8 answers, taken from
        // Java 8.
        Bulk.cn1Only("DecimalFormat.readsTextBetweenDigitsAsJava8", () -> {
            Bulk.expectEquals("#0.00x;#0.00 1234.57", malformed("0.0x0"), "0.0x0");
            Bulk.expectEquals("#00x;y#00 y1235", malformed("0x0;y0z0"), "0x0;y0z0");
            Bulk.expectEquals("00E0x;00E0 12E2", malformed("0E0x0"), "0E0x0");
            Bulk.expectEquals("#0.0#x;#0.0# 1234.57", malformed("0.0x#"), "0.0x#");
        });
        Bulk.check("DecimalFormat.parsesLikeTheJdk", () -> {
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
            for (String pattern : patterns) {
                for (int integerOnly = 0; integerOnly < 2; integerOnly++) {
                    DecimalFormat f = format(pattern);
                    f.setParseIntegerOnly(integerOnly == 1);
                    for (String text : texts) {
                        for (int from = 0; from < 2; from++) {
                            String input = from == 0 ? text : "x" + text;
                            ParsePosition position = new ParsePosition(from);
                            Number n = f.parse(input, position);
                            String value = n == null ? "null" : simpleName(n) + ":" + n;
                            p("[" + pattern + (integerOnly == 1 ? ",integerOnly" : "") + "] \"" + input
                                    + "\"@" + from, value + " index=" + position.getIndex() + " error="
                                    + position.getErrorIndex());
                        }
                    }
                }
            }
        });
        Bulk.check("DecimalFormat.parseOfAStringThrowsWhereTheJdkThrows", () -> {
            p("parse", NumberFormat.getInstance(US).parse("1,234.5"));
            try {
                NumberFormat.getInstance(US).parse("abc");
                p("abc", "parsed");
            } catch (ParseException e) {
                p("offset", e.getErrorOffset());
                p("message", e.getMessage());
            }
        });
        Bulk.check("DecimalFormat.factoriesFormatLikeTheJdk", () -> {
            double[] values = {0, 1, -1, 1234567.891, 0.5, 0.125, 0.126, -0.5, 1.5, 2.5, 1e10, 0.0049, 12.345};
            for (double v : values) {
                p("getInstance " + v, NumberFormat.getInstance(US).format(v));
                p("getNumberInstance " + v, NumberFormat.getNumberInstance(US).format(v));
                p("getIntegerInstance " + v, NumberFormat.getIntegerInstance(US).format(v));
                p("getPercentInstance " + v, NumberFormat.getPercentInstance(US).format(v));
                if (v >= 0) {
                    p("getCurrencyInstance " + v, NumberFormat.getCurrencyInstance(US).format(v));
                }
            }
        });
        // JDK 8's locale data writes a negative US amount in parentheses,
        // `($1.50)`; later JDKs, with CLDR data, write `-$1.50`, as JavaAPI
        // does.
        Bulk.check("DecimalFormat.negativeCurrencyUsesAMinusSign", () -> {
            p("1.5", NumberFormat.getCurrencyInstance(US).format(-1.5));
            p("1234.567", NumberFormat.getCurrencyInstance(US).format(-1234.567));
        });
        Bulk.check("DecimalFormat.settersBehaveLikeTheJdk", () -> {
            DecimalFormat f = format("#,##0.###");
            f.setMaximumFractionDigits(1);
            p("maxFraction", f.format(1234.56));
            f.setMinimumFractionDigits(4);
            p("minFraction", f.format(1234.56));
            p("getMaxFraction", f.getMaximumFractionDigits());
            f.setGroupingUsed(false);
            p("ungrouped", f.format(1234567));
            f.setMinimumIntegerDigits(10);
            p("minInteger", f.format(42));
            f.setMaximumIntegerDigits(2);
            p("maxInteger", f.format(1997));
            p("maxIntegerFraction", f.format(1997.5));
            p("getMinInteger", f.getMinimumIntegerDigits());
            f.setGroupingUsed(true);
            f.setGroupingSize(2);
            f.setMaximumIntegerDigits(20);
            f.setMinimumIntegerDigits(1);
            p("groupingSize", f.format(1234567));
            p("pattern", f.toPattern());
            f.setPositivePrefix("+");
            f.setNegativeSuffix(" CR");
            f.setDecimalSeparatorAlwaysShown(true);
            p("affixes", f.format(5));
            p("negativeAffixes", f.format(-5));
            p("affixPattern", f.toPattern());
            f.setMultiplier(7);
            p("times7", f.format(6));
            p("times7Half", f.format(0.5));
            p("times7Max", f.format(Long.MAX_VALUE));
            f.setMultiplier(-3);
            p("timesMinus3", f.format(6));
            p("timesMinus3Negative", f.format(-2.5));
        });
        Bulk.check("DecimalFormat.formatsObjectsByTheirType", () -> {
            NumberFormat f = NumberFormat.getInstance(US);
            Object[] numbers = {
                Integer.valueOf(1234), Long.valueOf(-99999L), Short.valueOf((short) 7), Byte.valueOf((byte) -3),
                Double.valueOf(1234.5678), Float.valueOf(0.5f),
            };
            for (Object number : numbers) {
                p(simpleName(number) + " " + number, f.format(number));
            }
            Bulk.pThrown("notANumber", IllegalArgumentException.class, () -> f.format("not a number"));
        });
        Bulk.check("DecimalFormat.reportsFieldPositionsLikeTheJdk", () -> {
            String[] patterns = {"#,##0.00", "0.###E0", "$0.0#", "#"};
            double[] values = {1234.5, -0.25, 7, 1e9};
            for (String pattern : patterns) {
                for (double value : values) {
                    for (int field = 0; field < 2; field++) {
                        FieldPosition position = new FieldPosition(field);
                        String text = format(pattern).format(value, new StringBuffer("ab"), position).toString();
                        p(pattern + " " + value + " field " + field, text + " " + position.getBeginIndex() + "-"
                                + position.getEndIndex());
                    }
                }
            }
        });
        Bulk.check("DecimalFormat.symbolsChangeTheOutput", () -> {
            DecimalFormatSymbols symbols = changedSymbols();
            // The currency pattern is in symbolsChangeTheCurrencyOutput.
            String[] patterns = {"#,##0.00", "0.0%", "0.##E0"};
            double[] values = {1234567.891, -0.5, 0.000123, Double.NaN, Double.NEGATIVE_INFINITY};
            for (String pattern : patterns) {
                DecimalFormat f = new DecimalFormat(pattern, symbols);
                for (double value : values) {
                    p(pattern + " " + value, f.format(value));
                }
                String text = f.format(-1234.5);
                p(pattern + " parse " + text, f.parse(text, new ParsePosition(0)));
            }
            // The format holds its own copy, as the JDK's does.
            DecimalFormat f = new DecimalFormat("0.0", symbols);
            symbols.setDecimalSeparator('!');
            p("copied", f.format(1.5));
            DecimalFormatSymbols handedOut = f.getDecimalFormatSymbols();
            handedOut.setDecimalSeparator('?');
            p("handedOutCopy", f.format(1.5));
            f.setDecimalFormatSymbols(handedOut);
            p("set", f.format(1.5));
        });
        // After Java 8 the JDK groups a currency pattern with a monetary
        // grouping separator of its own, which setGroupingSeparator leaves
        // alone; Java 8 (and JavaAPI) group it with the grouping separator.
        // The Java 8 answers, taken from Java 8.
        Bulk.cn1Only("DecimalFormat.symbolsChangeTheCurrencyOutput", () -> {
            DecimalFormat f = new DecimalFormat(CURRENCY + " #,##0.00", changedSymbols());
            Bulk.expectEquals("EUR 1.234.567.89", f.format(1234567.891), "1234567.891");
            Bulk.expectEquals("~EUR 0.50", f.format(-0.5), "-0.5");
            Bulk.expectEquals("EUR 0.00", f.format(0.000123), "0.000123");
            Bulk.expectEquals("nan", f.format(Double.NaN), "NaN");
            Bulk.expectEquals("~EUR inf", f.format(Double.NEGATIVE_INFINITY), "-Infinity");
            Bulk.expectEquals("-1.234", String.valueOf(f.parse("~EUR 1.234.50", new ParsePosition(0))), "parse");
        });
        Bulk.check("DecimalFormat.cloneIsAnIndependentEqualCopy", () -> {
            DecimalFormat original = format("#,##0.00;(#)");
            Object copy = original.clone();
            p("isDecimalFormat", copy instanceof DecimalFormat);
            p("notSame", original != copy);
            p("equal", original.equals(copy));
            p("sameHash", original.hashCode() == copy.hashCode());
            DecimalFormat cloned = (DecimalFormat) copy;
            p("sameFormat", original.format(-1234.5).equals(cloned.format(-1234.5)));
            p("samePattern", original.toPattern().equals(cloned.toPattern()));
            cloned.setMaximumFractionDigits(0);
            p("equalAfterChange", original.equals(cloned));
            p("originalKept", original.format(1234.5));
            Object symbolsCopy = usSymbols().clone();
            p("symbolsEqual", usSymbols().equals(symbolsCopy));
        });
        Bulk.check("DecimalFormat.parseFailureLeavesThePositionAlone", () -> {
            ParsePosition position = new ParsePosition(2);
            p("parsed", format("0.00").parse("ab?", position));
            p("index", position.getIndex());
            p("error", position.getErrorIndex());
        });
        // With no platform to say what the device's locale writes -- the
        // bulk host answers no locale symbols, as before Codename One is
        // initialized -- every locale gets the US English symbols. The JDK
        // answers French symbols here.
        Bulk.cn1Only("DecimalFormat.symbolsFallBackToUsEnglishWithoutADisplay", () -> {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
            Bulk.expectEquals(Character.valueOf('.'), Character.valueOf(symbols.getDecimalSeparator()), "decimal");
            Bulk.expectEquals(Character.valueOf(','), Character.valueOf(symbols.getGroupingSeparator()), "grouping");
            Bulk.expectEquals("$", symbols.getCurrencySymbol(), "currency");
            Bulk.expectEquals(new DecimalFormatSymbols(US), symbols, "equal to en_US");
        });
        // JavaAPI's US English symbols are Java 8's; the HotSpot run cannot
        // say so, since its JDK takes CLDR's.
        Bulk.cn1Only("DecimalFormat.java8UsSymbols", () -> {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(US);
            Bulk.expectEquals(JAVA8_NAN, symbols.getNaN(), "NaN");
            Bulk.expectEquals(String.valueOf((char) 0x221E), symbols.getInfinity(), "infinity");
            Bulk.expectEquals("E", symbols.getExponentSeparator(), "exponent");
            Bulk.expectEquals(JAVA8_NAN, NumberFormat.getInstance(US).format(Double.NaN), "NaN formatted");
        });
        hostChecks();
    }

    /// A device that writes "1.234.567,5" and prices in euros.
    private static final class DecimalFormatGermanDevice extends BulkHost {
        @Override
        public String[] localeNumberSymbols() {
            return new String[] {",", ".", "EUR"};
        }
    }

    /// A device that writes a decimal comma and does not group.
    private static final class DecimalFormatUngroupedDevice extends BulkHost {
        @Override
        public String[] localeNumberSymbols() {
            return new String[] {",", null, null};
        }
    }

    /// A platform whose localization fails outright.
    private static final class DecimalFormatBrokenDevice extends BulkHost {
        @Override
        public String[] localeNumberSymbols() {
            throw new IllegalStateException("no localization");
        }
    }

    /// JavaAPI's `DecimalFormatSymbols` takes the device locale's separators
    /// and currency from the installed `JdkHost`: Codename One's own, so
    /// these run on ParparVM only. The harness reinstalls its host after each.
    private static void hostChecks() {
        Bulk.cn1Only("DecimalFormatSymbolsHost.deviceSymbolsComeFromTheHost", () -> {
            JdkHost.install(new DecimalFormatGermanDevice());
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("de", "DE"));
            Bulk.expectEquals(Character.valueOf(','), Character.valueOf(symbols.getDecimalSeparator()), "decimal");
            Bulk.expectEquals(Character.valueOf(','), Character.valueOf(symbols.getMonetaryDecimalSeparator()),
                    "monetary decimal");
            Bulk.expectEquals(Character.valueOf('.'), Character.valueOf(symbols.getGroupingSeparator()), "grouping");
            Bulk.expectEquals("EUR", symbols.getCurrencySymbol(), "currency");
            Bulk.expectEquals("1.234.567,5", new DecimalFormat("#,##0.0", symbols).format(1234567.5), "format");
            Bulk.expectEquals("1.234,567", NumberFormat.getNumberInstance(new Locale("de", "DE")).format(1234.567),
                    "number instance");
        });
        Bulk.cn1Only("DecimalFormatSymbolsHost.usEnglishIsNeverAskedOfTheHost", () -> {
            JdkHost.install(new DecimalFormatGermanDevice());
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "US"));
            Bulk.expectEquals(Character.valueOf('.'), Character.valueOf(symbols.getDecimalSeparator()), "decimal");
            Bulk.expectEquals(Character.valueOf(','), Character.valueOf(symbols.getGroupingSeparator()), "grouping");
            Bulk.expectEquals("$", symbols.getCurrencySymbol(), "currency");
        });
        Bulk.cn1Only("DecimalFormatSymbolsHost.aDecimalCommaNeverGroupsWithAComma", () -> {
            JdkHost.install(new DecimalFormatUngroupedDevice());
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("fr", "FR"));
            Bulk.expectEquals(Character.valueOf(','), Character.valueOf(symbols.getDecimalSeparator()), "decimal");
            Bulk.expectEquals(Character.valueOf('.'), Character.valueOf(symbols.getGroupingSeparator()), "grouping");
            Bulk.expectEquals("$", symbols.getCurrencySymbol(), "currency");
        });
        Bulk.cn1Only("DecimalFormatSymbolsHost.aFailingHostLeavesUsEnglish", () -> {
            JdkHost.install(new DecimalFormatBrokenDevice());
            Bulk.expectEquals(new DecimalFormatSymbols(new Locale("en", "US")),
                    new DecimalFormatSymbols(new Locale("fr", "FR")), "fallback");
        });
    }
}
