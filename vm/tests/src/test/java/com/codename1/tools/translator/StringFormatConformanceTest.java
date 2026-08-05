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

package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive conformance for {@code java.lang.StringFormatter} against the JDK's own
 * {@code java.util.Formatter} (issue #5482).
 *
 * <p>The formatter lives in {@code java.lang}, so no ordinary test can load it -- a
 * class loader refuses any name starting with {@code java.}. This test copies the
 * JavaAPI sources into a neutral package, compiles them, and drives the result through
 * reflection, which makes a million-case differential possible in a plain unit test.</p>
 *
 * <p>It walks every combination of the eight flags, the supported conversions, a set of
 * widths and precisions, and a set of argument types: roughly a million single-specifier
 * format strings, each compared against the JDK for the exact rendering or the exact
 * exception. Single-specifier is deliberate -- the JVM validates a whole format string
 * before formatting any of it, so a format with several broken specifiers can legitimately
 * report a different one first. Restricting to one specifier removes that variable, so
 * every mismatch here is a real defect.</p>
 *
 * <p>This exists because the curated tables did not catch several validation-ordering
 * defects that review did: the JVM reports an unsupported flag on {@code %c} ahead of a
 * missing width but the other way round for {@code %s}, and it rejects a wrong argument
 * type for {@code %x} ahead of an unsupported sign flag. Ordering rules like those are
 * not something a hand-written table finds.</p>
 */
class StringFormatConformanceTest {

    private static final char[] FLAGS = {'-', '+', ' ', '0', ',', '(', '#', '<'};
    private static final String[] CONVERSIONS = {
        "s", "S", "b", "B", "h", "H", "c", "C", "d", "o", "x", "X", "e", "E", "f", "g", "G", "n", "%"
    };
    private static final String[] WIDTHS = {"", "0", "5", "12"};
    private static final String[] PRECISIONS = {"", ".0", ".2", ".6"};

    /**
     * {@code %a} and {@code %t} are unimplemented by design and raise
     * UnknownFormatConversionException; they are covered by StringFormatIntegrationTest
     * and deliberately absent from CONVERSIONS.
     */
    @Test
    void everySingleSpecifierMatchesTheJdk() throws Exception {
        // The loader has to stay open for the whole sweep, not just long enough to
        // resolve the method: the relocated exception classes are loaded lazily, the
        // first time a malformed specifier throws one.
        try (URLClassLoader loader = compileRelocatedFormatter()) {
            runSweep(loadFormatMethod(loader));
        }
    }

    private void runSweep(Method format) throws Exception {

        Object[] arguments = {
            "txt", "", Integer.valueOf(42), Integer.valueOf(-42), Integer.valueOf(0),
            Long.valueOf(-1L), Long.valueOf(Long.MIN_VALUE), Double.valueOf(1.5),
            Double.valueOf(-0.0), Double.valueOf(1.0 / 3.0), Double.valueOf(Double.NaN),
            Double.valueOf(Double.POSITIVE_INFINITY), Float.valueOf(1.1f),
            Character.valueOf('a'), Boolean.TRUE, null, Byte.valueOf((byte) -1),
            Short.valueOf((short) -1)
        };

        // The JDK formats through the default locale; this formatter is locale
        // independent by design, so compare on the locale whose separators it uses.
        // Java 8 ignores a width on the literal "%" conversion; Java 9 honours it, and
        // this formatter follows the modern behaviour. Detect which JDK is running the
        // test rather than pinning a version, and account for the skips explicitly.
        boolean legacyPercentWidth = "%".equals(String.format("%5%"));

        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        long total = 0;
        long skipped = 0;
        Map<String, String> mismatches = new LinkedHashMap<>();
        try {
            for (int mask = 0; mask < (1 << FLAGS.length); mask++) {
                StringBuilder flags = new StringBuilder();
                for (int bit = 0; bit < FLAGS.length; bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        flags.append(FLAGS[bit]);
                    }
                }
                for (String conversion : CONVERSIONS) {
                    for (String width : WIDTHS) {
                        for (String precision : PRECISIONS) {
                            String spec = "%" + flags + width + precision + conversion;
                            boolean legacySkip = legacyPercentWidth && "%".equals(conversion)
                                    && !width.isEmpty() && !"0".equals(width);
                            for (Object argument : arguments) {
                                if (legacySkip) {
                                    skipped++;
                                    continue;
                                }
                                total++;
                                String expected = referenceOutcome(spec, argument);
                                String actual = actualOutcome(format, spec, argument);
                                if (!expected.equals(actual) && mismatches.size() < 25) {
                                    mismatches.put(spec + " <- " + describeArgument(argument),
                                            "jdk=" + expected + " cn1=" + actual);
                                } else if (!expected.equals(actual)) {
                                    mismatches.put("(further mismatches suppressed)", "");
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(previous);
        }

        assertTrue(total > 900000, "expected the full sweep to run, only " + total + " cases did");
        assertTrue(legacyPercentWidth || skipped == 0,
                "a modern JDK should not skip any case, skipped " + skipped);

        StringBuilder report = new StringBuilder();
        for (Map.Entry<String, String> entry : mismatches.entrySet()) {
            report.append("\n  ").append(entry.getKey()).append("  ").append(entry.getValue());
        }
        assertEquals("", report.toString(),
                mismatches.size() + " of " + total + " single-specifier cases diverged from the JDK:");

        checkOversizedNumericFields(format);
    }

    /**
     * Numbers too large for an int are their own class of bug, and the sweep above cannot
     * reach them because its widths stop at 12. An invented cap here once rejected
     * {@code "%1000001d"}, which the JVM formats quite happily.
     */
    private void checkOversizedNumericFields(Method format) {
        // Large but representable fields behave the same on every JDK.
        String[] representable = { "%1000001d", "%12.1000001f", "%1000001$d" };
        for (String spec : representable) {
            assertEquals(referenceOutcome(spec, Integer.valueOf(1)),
                    actualOutcome(format, spec, Integer.valueOf(1)),
                    "large numeric field " + spec + " should behave like the JVM");
        }

        // A field too large for an int is silently dropped up to at least Java 11 and
        // rejected from Java 17 on; this formatter follows the modern behaviour. Detect
        // which one is running rather than pinning a version -- and note the probe itself
        // throws on a modern JDK, so it has to be guarded.
        boolean legacyOversized;
        try {
            legacyOversized = "1".equals(String.format("%2147483648d", Integer.valueOf(1)));
        } catch (IllegalArgumentException rejected) {
            legacyOversized = false;
        }
        if (legacyOversized) {
            return;
        }
        String[] overflowing = {
            "%2147483648d", "%99999999999d", "%.2147483648f", "%.99999999999f",
            "%2147483648$d", "%99999999999$d"
        };
        for (String spec : overflowing) {
            assertEquals(referenceOutcome(spec, Integer.valueOf(1)),
                    actualOutcome(format, spec, Integer.valueOf(1)),
                    "overflowing numeric field " + spec + " should behave like the JVM");
        }
    }

    private static String describeArgument(Object argument) {
        if (argument == null) {
            return "null";
        }
        return argument.getClass().getSimpleName() + " " + argument;
    }

    /** The JDK's own answer: either the rendering or the exception it raises. */
    private static String referenceOutcome(String spec, Object argument) {
        try {
            return "V:" + String.format(spec, new Object[] { argument });
        } catch (Throwable t) {
            return "E:" + t.getClass().getSimpleName();
        }
    }

    private static String actualOutcome(Method format, String spec, Object argument) {
        try {
            return "V:" + format.invoke(null, spec, new Object[] { argument });
        } catch (InvocationTargetException e) {
            return "E:" + e.getCause().getClass().getSimpleName();
        } catch (Exception e) {
            return "E:" + e.getClass().getSimpleName();
        }
    }

    private Method loadFormatMethod(URLClassLoader loader) throws Exception {
        Class<?> formatter = loader.loadClass("cn1format.StringFormatter");
        Method format = formatter.getDeclaredMethod("format", String.class, Object[].class);
        format.setAccessible(true);
        return format;
    }

    /**
     * Copies java.lang.StringFormatter and the java.util format exceptions it uses into a
     * neutral package, compiles them, and returns a loader over the result. The rewrite is
     * textual and deliberately narrow: only the package and import statements move, so the
     * logic under test is the shipping source.
     */
    private URLClassLoader compileRelocatedFormatter() throws Exception {
        Path javaApi = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        assertTrue(Files.isDirectory(javaApi), "JavaAPI sources should be at " + javaApi);

        Path work = Files.createTempDirectory("string-format-conformance");
        Path pkg = work.resolve("src").resolve("cn1format");
        Files.createDirectories(pkg);

        List<String> files = new ArrayList<>();
        files.add(relocate(javaApi.resolve("java/lang/StringFormatter.java"), pkg));
        try (Stream<Path> paths = Files.list(javaApi.resolve("java/util"))) {
            for (Path candidate : (Iterable<Path>) paths.sorted()::iterator) {
                String name = candidate.getFileName().toString();
                if (name.endsWith("FormatException.java") || name.contains("Format")
                        && name.endsWith("Exception.java")) {
                    files.add(relocate(candidate, pkg));
                }
            }
        }
        assertTrue(files.size() > 5, "expected the format exceptions to be relocated, found " + files);

        Path classes = work.resolve("classes");
        Files.createDirectories(classes);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "a JDK (not a JRE) is required to run this test");
        List<String> args = new ArrayList<>();
        args.add("-nowarn");
        args.add("-d");
        args.add(classes.toString());
        args.addAll(files);
        assertEquals(0, compiler.run(null, null, System.err, args.toArray(new String[0])),
                "the relocated formatter should compile");

        return new URLClassLoader(new URL[] { classes.toUri().toURL() },
                getClass().getClassLoader());
    }

    private String relocate(Path source, Path targetDirectory) throws IOException {
        assertTrue(Files.isRegularFile(source), "expected to relocate " + source);
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        text = text.replace("package java.lang;", "package cn1format;")
                .replace("package java.util;", "package cn1format;")
                .replace("import java.util.", "import cn1format.");
        // Package private in the shipping source; reflection needs it reachable here.
        text = text.replace("final class StringFormatter", "public final class StringFormatter");
        Path target = targetDirectory.resolve(source.getFileName().toString());
        Files.write(target, text.getBytes(StandardCharsets.UTF_8));
        return target.toString();
    }
}
