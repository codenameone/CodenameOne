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

/// LangMembersApp, which used to be translated and built by a test of its own,
/// run as one check of the bulk program. Its `fixtureMain` (the translator
/// allows one `main` per program) prints what its `main` always
/// printed (case lines and a RESULT line); a difference names the line.
public final class LangMembersChecks {
    private LangMembersChecks() {
    }

    /// Runs the group.
    public static void run() {
        // scalb by a huge negative factor underflows to a signed zero;
        // cbrt of a subnormal keeps the JDK's one-ulp accuracy.
        Bulk.check("Lang.scalbUnderflowsAndCbrtOfSubnormals", () -> {
            Bulk.p("scalb.max.minInt", Double.doubleToRawLongBits(Math.scalb(Double.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalb.-max.minInt", Double.doubleToRawLongBits(Math.scalb(-Double.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalbf.max.minInt", Float.floatToRawIntBits(Math.scalb(Float.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalb.min.2098", Double.doubleToRawLongBits(Math.scalb(Double.MIN_VALUE, 2098)));
            double[] tiny = {Double.MIN_VALUE, -Double.MIN_VALUE, 3 * Double.MIN_VALUE, 0x1.0p-1030, Double.MIN_NORMAL / 3};
            for (double t : tiny) {
                long got = Double.doubleToRawLongBits(Math.cbrt(t));
                long want = Double.doubleToRawLongBits(StrictMath.cbrt(t));
                Bulk.p("cbrt " + t, Math.abs(got - want) <= 1);
            }
        });
        Bulk.check("Lang.tanhSmallInputsAreAccurate", () -> {
            double[] xs = {-0.25444256546893085, 0.9900105052487167, -0.5414934855402953, 0.5, 1e-3, 1e-9, 0.999999};
            for (double x : xs) {
                Bulk.p("tanh " + x, Math.abs(Math.tanh(x) - StrictMath.tanh(x)) <= 2 * Math.ulp(StrictMath.tanh(x)));
            }
        });
        Bulk.check("Lang.unassignedCjkExtensionGapsAreNotLetters", () -> {
            int[] cps = {0x2a6f0, 0x2b73f, 0x2b81e, 0x2cea2, 0x2ee5e, 0x2f7ff, 0x2fa1e, 0x2ffff, 0x2f800, 0x2b740};
            for (int cp : cps) {
                Bulk.p(Integer.toHexString(cp), Character.isLetter(cp));
            }
        });
        Bulk.check("Lang.aStringDecodedOutOfRangeThrows", () -> {
            int[][] ranges = {{1, 1}, {-1, 1}, {0, -1}, {2, 0}};
            for (int[] r : ranges) {
                String got;
                try {
                    got = new String(new byte[1], r[0], r[1], java.nio.charset.StandardCharsets.UTF_8);
                } catch (IndexOutOfBoundsException e) {
                    got = "IOOBE";
                }
                Bulk.p(r[0] + "," + r[1], got);
            }
        });
        Bulk.check("Lang.hypotIsWithinAnUlp", () -> {
            double[][] xy = {{-0x1.30ad286c08edp745, 0x1.be3f115b1361dp769}, {2.5917508825783748e48, 1.5152149925235254e41},
                    {3, 4}, {1e-310, 3e-311}, {Double.MIN_VALUE, Double.MIN_VALUE}, {1e300, 1e300}, {Double.MAX_VALUE, 1},
                    {0.1, 0.2}, {1, 1e-20}, {-0.0, 0.0}};
            for (double[] v : xy) {
                long got = Double.doubleToLongBits(Math.hypot(v[0], v[1]));
                long want = Double.doubleToLongBits(StrictMath.hypot(v[0], v[1]));
                Bulk.p("hypot " + v[0] + "," + v[1], Math.abs(got - want) <= 1);
            }
        });
        Bulk.check("Lang.stringBufferComparesBothWays", () -> {
            StringBuffer a = new StringBuffer("abc");
            StringBuffer b = new StringBuffer("abd");
            Bulk.p("ab", Integer.signum(a.compareTo(b)));
            Bulk.p("ba", Integer.signum(b.compareTo(a)));
            Bulk.p("aa", a.compareTo(a));
        });
        Bulk.check("Lang.underscoreIsNotATagSeparator", () -> {
            Bulk.p("en_US", java.util.Locale.forLanguageTag("en_US").toLanguageTag());
            Bulk.p("en-US", java.util.Locale.forLanguageTag("en-US").toLanguageTag());
        });
        Bulk.check("Lang.illFormedLocalePartsLeaveTheTag", () -> {
            Bulk.p("a-US", new java.util.Locale("a", "US").toLanguageTag());
            Bulk.p("en-USA", new java.util.Locale("en", "USA").toLanguageTag());
            Bulk.p("en-419", new java.util.Locale("en", "419").toLanguageTag());
        });
        Bulk.check("Lang.aQueryKeepsItsLaterQuestionMarks", () -> {
            try {
                java.net.URI u = new java.net.URI("http://h/p?a?b");
                Bulk.p("path", u.getPath());
                Bulk.p("query", u.getQuery());
            } catch (java.net.URISyntaxException e) {
                Bulk.p("threw", e.getClass().getName());
            }
        });
        Bulk.check("Lang.nullArgumentsAreRefused", () -> {
            try {
                new String(new byte[] {65}, (String) null);
                Bulk.p("newString", "accepted");
            } catch (Exception e) {
                Bulk.p("newString", e.getClass().getName());
            }
            try {
                "A".getBytes((String) null);
                Bulk.p("getBytes", "accepted");
            } catch (Exception e) {
                Bulk.p("getBytes", e.getClass().getName());
            }
            try {
                java.time.Clock.fixed(null, java.time.ZoneOffset.UTC);
                Bulk.p("clockInstant", "accepted");
            } catch (Exception e) {
                Bulk.p("clockInstant", e.getClass().getName());
            }
            try {
                java.time.Clock.fixed(java.time.Instant.EPOCH, null);
                Bulk.p("clockZone", "accepted");
            } catch (Exception e) {
                Bulk.p("clockZone", e.getClass().getName());
            }
            try {
                new java.security.SecureRandom((byte[]) null);
                Bulk.p("seedCtor", "accepted");
            } catch (Exception e) {
                Bulk.p("seedCtor", e.getClass().getName());
            }
            try {
                new java.security.SecureRandom().setSeed((byte[]) null);
                Bulk.p("setSeed", "accepted");
            } catch (Exception e) {
                Bulk.p("setSeed", e.getClass().getName());
            }
        });
        Bulk.check("Lang.aSplitCodePointIsOutsideTheRange", () -> {
            String smile = new String(Character.toChars(0x1F600));
            Bulk.p("cut", smile.indexOf(0x1F600, 0, 1));
            Bulk.p("whole", smile.indexOf(0x1F600, 0, 2));
        });
        Bulk.check("Lang.anOversizedUuidPartIsRefused", () -> {
            try {
                Bulk.p("parsed", java.util.UUID.fromString("ffffffffffffffff-0-0-0-0"));
            } catch (IllegalArgumentException e) {
                Bulk.p("threw", e instanceof NumberFormatException);
            }
            Bulk.p("max", java.util.UUID.fromString("7fffffff-0-0-0-0"));
        });
        Bulk.check("Lang.aNullPrintWriterTargetIsRefused", () -> {
            try {
                new java.io.PrintWriter((java.io.Writer) null);
                Bulk.p("writer", "accepted");
            } catch (NullPointerException e) {
                Bulk.p("writer", "npe");
            }
        });
        Bulk.check("Lang.anEmptyChoiceIsSkipped", () -> {
            java.text.ChoiceFormat f = new java.text.ChoiceFormat("0#a||1#b");
            Bulk.p("zero", f.format(0));
            Bulk.p("one", f.format(1));
        });
        Bulk.check("Lang.sharpSHasNoSingleUpperCase", () -> {
            Bulk.p("char", (int) Character.toUpperCase((char) 0xdf));
            Bulk.p("codePoint", Character.toUpperCase(0xdf));
        });
        Bulk.check("Lang.anUnwritableVariantLeavesTheTag", () -> {
            Bulk.p("space", new java.util.Locale("en", "", "a b").toLanguageTag());
            Bulk.p("short", new java.util.Locale("en", "", "ab").toLanguageTag());
        });
        Bulk.check("Lang.bufferedWrappersRefuseNull", () -> {
            try {
                new java.io.BufferedReader((java.io.Reader) null);
                Bulk.p("reader", "accepted");
            } catch (NullPointerException e) {
                Bulk.p("reader", "npe");
            }
            try {
                new java.io.BufferedWriter((java.io.Writer) null);
                Bulk.p("writer", "accepted");
            } catch (NullPointerException e) {
                Bulk.p("writer", "npe");
            }
        });
        Bulk.check("Lang.spliteratorsReportOrderAndDistinctness", () -> {
            java.util.List<String> list = new java.util.ArrayList<String>(java.util.Arrays.asList("a", "b"));
            java.util.Set<String> set = new java.util.HashSet<String>(list);
            Bulk.p("listOrdered", list.spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));
            Bulk.p("setDistinct", set.spliterator().hasCharacteristics(java.util.Spliterator.DISTINCT));
            Bulk.p("linkedOrdered", new java.util.LinkedList<String>(list).spliterator()
                    .hasCharacteristics(java.util.Spliterator.ORDERED));
        });
        Bulk.check("Lang.aLargeStringWriteGoesThroughTheBuffer", () -> {
            try {
                java.io.StringWriter sink = new java.io.StringWriter();
                java.io.BufferedWriter w = new java.io.BufferedWriter(sink, 3);
                w.write("abcdefghij", 1, 8);
                w.write("xy");
                w.flush();
                Bulk.p("out", sink.toString());
            } catch (java.io.IOException e) {
                Bulk.p("threw", e.getClass().getName());
            }
        });
        Bulk.check("Lang.adaptersKeepTheirDownstreamCharacteristics", () -> {
            java.util.stream.Collector<String, ?, java.util.Set<String>> set = java.util.stream.Collectors.toSet();
            Bulk.p("mapping", new java.util.TreeSet<Object>(java.util.stream.Collectors.mapping(
                    java.util.function.Function.<String>identity(), set).characteristics()));
            Bulk.p("filtering", new java.util.TreeSet<Object>(java.util.stream.Collectors.filtering(
                    (String x) -> true, set).characteristics()));
            Bulk.p("andThen", new java.util.TreeSet<Object>(java.util.stream.Collectors.collectingAndThen(
                    set, java.util.Set::size).characteristics()));
        });
        Bulk.check("Lang.comparableCharSequencesUseTheirOwnOrder", () -> {
            Bulk.p("builders", Integer.signum(CharSequence.compare(new StringBuilder("b"), new StringBuilder("a"))));
            Bulk.p("mixed", Integer.signum(CharSequence.compare("b", new StringBuilder("a"))));
        });
        Bulk.check("Lang.charsetReadersRefuseANullStream", () -> {
            try {
                new java.io.InputStreamReader((java.io.InputStream) null, java.nio.charset.StandardCharsets.UTF_8);
                Bulk.p("reader", "accepted");
            } catch (NullPointerException e) {
                Bulk.p("reader", "npe");
            }
            try {
                new java.io.OutputStreamWriter((java.io.OutputStream) null, java.nio.charset.StandardCharsets.UTF_8);
                Bulk.p("writer", "accepted");
            } catch (NullPointerException e) {
                Bulk.p("writer", "npe");
            }
        });
        Bulk.check("Lang.aContinuedKeyStillTakesItsSeparator", () -> {
            try {
                java.util.Properties p = new java.util.Properties();
                p.load(new java.io.StringReader("key \\\n=value\nk2 \\\n  :v2\nk3=a=b\nk4 x=y\n"));
                Bulk.p("props", new java.util.TreeMap<Object, Object>(p));
            } catch (java.io.IOException e) {
                Bulk.p("threw", e.getClass().getName());
            }
        });
        Bulk.check("Lang.anEmptyReferenceKeepsAnEmptyPath", () -> {
            try {
                java.net.URI base = new java.net.URI("http://h/a/b?x#y");
                String[] refs = {"?q", "#f", "", "c", "../c", "/d", "//g/p"};
                for (String r : refs) {
                    Bulk.p("path[" + r + "]", new java.net.URI(r).getPath());
                    Bulk.p("resolve[" + r + "]", base.resolve(new java.net.URI(r)).toString());
                }
                Bulk.p("hostOnly", new java.net.URI("http://h").getPath());
                String[] paths = {"/a/b/../c/", "/a/./b/.", "/a/b/..", "/", "a/../b"};
                for (String n : paths) {
                    Bulk.p("normalize[" + n + "]", new java.net.URI(n).normalize().toString());
                }
            } catch (java.net.URISyntaxException e) {
                Bulk.p("threw", e.getClass().getName());
            }
        });
        Bulk.check("Lang.aSortedIteratorSpliteratorHasNaturalOrder", () -> {
            java.util.Spliterator<String> sp = java.util.Spliterators.spliterator(
                    java.util.Arrays.asList("a", "b").iterator(), 2, java.util.Spliterator.SORTED);
            Bulk.p("comparator", String.valueOf(sp.getComparator()));
        });
        Bulk.check("Lang.aNullAffixLeavesTheFormatAlone", () -> {
            java.text.DecimalFormat f = new java.text.DecimalFormat("#0");
            f.setPositivePrefix("+");
            try {
                f.setPositivePrefix(null);
            } catch (NullPointerException e) {
                Bulk.p("npe", "yes");
            }
            Bulk.p("prefix", f.getPositivePrefix());
            Bulk.p("suffix", f.getPositiveSuffix());
        });
        Bulk.check("Lang.onlyTwentyThreeFiftyNineHasALeapSecond", () -> {
            String[] in = {"2020-12-31T23:59:60Z", "2020-01-01T12:30:60Z"};
            for (String t : in) {
                try {
                    Bulk.p(t, java.time.Instant.parse(t).toString());
                } catch (java.time.DateTimeException e) {
                    Bulk.p(t, "rejected");
                }
            }
        });
        Bulk.check("Lang.aCloseClosesTheDelegateAfterAnError", () -> {
            final boolean[] closed = new boolean[2];
            java.io.OutputStream sink = new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    throw new AssertionError("write");
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    throw new AssertionError("write");
                }

                @Override
                public void close() {
                    closed[0] = true;
                }
            };
            java.io.Writer wsink = new java.io.Writer() {
                @Override
                public void write(char[] c, int off, int len) {
                    throw new AssertionError("write");
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                    closed[1] = true;
                }
            };
            try {
                java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(sink);
                out.write(1);
                out.close();
            } catch (Throwable t) {
                Bulk.p("stream", t.getClass().getName());
            }
            try {
                java.io.BufferedWriter out = new java.io.BufferedWriter(wsink);
                out.write('x');
                out.close();
            } catch (Throwable t) {
                Bulk.p("writer", t.getClass().getName());
            }
            Bulk.p("streamClosed", closed[0]);
            Bulk.p("writerClosed", closed[1]);
        });
        Bulk.check("Lang.aCustomMessageFormatSurvivesCloneAndEquals", () -> {
            java.text.Format shout = new java.text.Format() {
                @Override
                public StringBuffer format(Object o, StringBuffer to, java.text.FieldPosition pos) {
                    return to.append(String.valueOf(o).toUpperCase());
                }

                @Override
                public Object parseObject(String s, java.text.ParsePosition pos) {
                    return null;
                }
            };
            java.text.MessageFormat a = new java.text.MessageFormat("hi {0}");
            java.text.MessageFormat plain = new java.text.MessageFormat("hi {0}");
            a.setFormat(0, shout);
            java.text.MessageFormat copy = (java.text.MessageFormat) a.clone();
            Bulk.p("copy", copy.format(new Object[] {"bob"}));
            Bulk.p("equalsPlain", a.equals(plain));
        });
        Bulk.check("Lang.flatMappingKeepsTheConsumingFailure", () -> {
            String got;
            try {
                java.util.stream.Stream.of(1).collect(java.util.stream.Collectors.flatMapping(
                        i -> java.util.stream.Stream.of(i).onClose(() -> { throw new IllegalStateException("close"); }),
                        java.util.stream.Collectors.reducing(0, v -> { throw new IllegalArgumentException("consume"); }, Integer::sum)));
                got = "none";
            } catch (RuntimeException e) {
                got = e.getMessage() + " suppressed=" + e.getSuppressed().length;
            }
            Bulk.p("got", got);
        });
        Bulk.check("Lang.collectorsReportTheirCharacteristics", () -> {
            java.util.Set<java.util.stream.Collector.Characteristics> set = java.util.stream.Collectors.toSet().characteristics();
            Bulk.p("toList", java.util.stream.Collectors.toList().characteristics());
            Bulk.p("toSet", set.contains(java.util.stream.Collector.Characteristics.UNORDERED) + ","
                    + set.contains(java.util.stream.Collector.Characteristics.IDENTITY_FINISH) + "," + set.size());
            Bulk.p("toCollection", java.util.stream.Collectors.toCollection(java.util.ArrayList::new).characteristics());
            Bulk.p("joining", java.util.stream.Collectors.joining().characteristics());
        });
        Bulk.check("LangMembersApp", () -> LangMembersApp.fixtureMain(new String[0]));
    }
}
