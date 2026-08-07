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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ParparVM's CHECKCAST is unchecked, so code that relies on a failing cast
 * throwing ClassCastException behaves differently on iOS than on the simulator
 * and Android -- issue #5531, where {@code optDouble}'s
 * {@code catch (Exception)} never ran and the process died instead.
 * {@link CastSemanticsVerifier} is the gate that keeps that pattern out of our
 * code; these tests pin what it does and does not report.
 */
class CastSemanticsVerifierTest {

    @Test
    void reportsACastABroadHandlerWouldSwallow() throws Exception {
        List<CastSemanticsVerifier.Violation> violations = analyze(
                "public class Subject {",
                "    static double optDouble(Object o, double fallback) {",
                "        try {",
                "            return Double.parseDouble((String) o);",
                "        } catch (Exception e) {",
                "            return fallback;",
                "        }",
                "    }",
                "}");
        assertEquals(1, violations.size(), "the reported shape must be caught: " + violations);
        assertEquals("java/lang/String", violations.get(0).castTo);
        assertEquals("java/lang/Exception", violations.get(0).caughtType);
        assertEquals("Subject#optDouble(Ljava/lang/Object;D)D", violations.get(0).key());
    }

    @Test
    void reportsRuntimeExceptionAndThrowableHandlersToo() throws Exception {
        assertEquals(1, analyze(
                "public class Subject {",
                "    static String f(Object o) {",
                "        try { return (String) o; } catch (RuntimeException e) { return null; }",
                "    }",
                "}").size(), "catch(RuntimeException) swallows a failed cast");
        assertEquals(1, analyze(
                "public class Subject {",
                "    static String f(Object o) {",
                "        try { return (String) o; } catch (Throwable e) { return null; }",
                "    }",
                "}").size(), "catch(Throwable) swallows a failed cast");
    }

    @Test
    void ignoresHandlersThatCannotCatchACastFailure() throws Exception {
        assertTrue(analyze(
                "import java.io.IOException;",
                "public class Subject {",
                "    static String f(Object o) {",
                "        try { load(); return (String) o; } catch (IOException e) { return null; }",
                "    }",
                "    static void load() throws IOException { }",
                "}").isEmpty(), "an unrelated exception type cannot absorb a cast failure");
        assertTrue(analyze(
                "public class Subject {",
                "    static String f(Object o) {",
                "        try { return (String) o; } finally { touch(); }",
                "    }",
                "    static void touch() { }",
                "}").isEmpty(), "a finally block rethrows rather than substituting a value");
    }

    @Test
    void ignoresACastOutsideAnyTry() throws Exception {
        assertTrue(analyze(
                "public class Subject {",
                "    static String f(Object o) {",
                "        String s = (String) o;",
                "        try { touch(); } catch (Exception e) { return null; }",
                "        return s;",
                "    }",
                "    static void touch() { }",
                "}").isEmpty(), "a cast the handler does not cover is not a violation");
    }

    /**
     * ParparVM delivers an explicitly thrown ClassCastException perfectly well --
     * only the implicit one from a failed CHECKCAST is missing. A handler with no
     * cast under it is therefore still live, and flagging it would send people
     * editing correct code. This is {@code java.util.AbstractSet.equals}.
     */
    @Test
    void ignoresACatchOfClassCastExceptionWithNoCastUnderIt() throws Exception {
        assertTrue(analyze(
                "public class Subject {",
                "    static boolean f(Object o) {",
                "        try { return o.equals(Subject.class); }",
                "        catch (ClassCastException e) { return false; }",
                "    }",
                "}").isEmpty(), "a live handler for a thrown CCE must not be reported");
    }

    /**
     * The remedy the gate asks for still compiles to a CHECKCAST. Reporting it
     * would make the gate impossible to satisfy.
     */
    @Test
    void ignoresACastGuardedByInstanceof() throws Exception {
        assertTrue(analyze(
                "public class Subject {",
                "    static double optDouble(Object o, double fallback) {",
                "        try {",
                "            if (o instanceof Number) {",
                "                return ((Number) o).doubleValue();",
                "            }",
                "            return fallback;",
                "        } catch (Exception e) {",
                "            return fallback;",
                "        }",
                "    }",
                "}").isEmpty(), "an instanceof-guarded cast cannot fail");
    }

    /**
     * The guard only covers the branch it proves. A second, unguarded cast to a
     * different type in the same method must still be reported, or the
     * suppression would be a hole big enough to hide the original bug in.
     */
    @Test
    void stillReportsAnUnguardedCastAlongsideAGuardedOne() throws Exception {
        List<CastSemanticsVerifier.Violation> violations = analyze(
                "public class Subject {",
                "    static String f(Object o, Object p) {",
                "        try {",
                "            if (o instanceof Number) {",
                "                return ((Number) o).toString();",
                "            }",
                "            return (String) p;",
                "        } catch (Exception e) {",
                "            return null;",
                "        }",
                "    }",
                "}");
        assertEquals(1, violations.size(), "only the guarded cast is exempt: " + violations);
        assertEquals("java/lang/String", violations.get(0).castTo);
    }

    /** Reassigning the local invalidates what the instanceof proved about it. */
    @Test
    void stillReportsWhenTheGuardedLocalIsReassigned() throws Exception {
        List<CastSemanticsVerifier.Violation> violations = analyze(
                "public class Subject {",
                "    static String f(Object o, Object other) {",
                "        try {",
                "            if (o instanceof String) {",
                "                o = other;",
                "                return (String) o;",
                "            }",
                "            return null;",
                "        } catch (Exception e) {",
                "            return null;",
                "        }",
                "    }",
                "}");
        assertEquals(1, violations.size(), "the proof does not survive the store: " + violations);
    }

    @Test
    void baselineEntriesAreKeyedByMethodAndCarryANote() throws Exception {
        Path baseline = Files.createTempFile("cast-semantics-baseline", ".txt");
        Files.write(baseline, ("# a comment\n\nSubject#optDouble(Ljava/lang/Object;D)D|pre-existing\n")
                .getBytes(StandardCharsets.UTF_8));

        Set<String> keys = CastSemanticsVerifier.readBaseline(baseline.toFile());
        assertEquals(1, keys.size(), "comments and blank lines are ignored");
        assertTrue(keys.contains("Subject#optDouble(Ljava/lang/Object;D)D"));
    }

    /**
     * Guards the shipped baseline against drifting into a stale-entry dump: every
     * key must still name a method the analyzer could produce.
     */
    @Test
    void theShippedBaselineIsWellFormed() throws Exception {
        File baseline = repoFile("scripts/cast-semantics-baseline.txt");
        assertTrue(baseline.exists(), "the baseline ships with the repo: " + baseline);
        List<String> lines = Files.readAllLines(baseline.toPath(), StandardCharsets.UTF_8);
        int entries = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            entries++;
            int bar = trimmed.indexOf('|');
            assertTrue(bar > 0, "entry needs a '|note': " + trimmed);
            String key = trimmed.substring(0, bar);
            assertTrue(key.indexOf('#') > 0, "entry key must be class#method(desc): " + trimmed);
            assertTrue(key.indexOf('(') > key.indexOf('#'),
                    "entry key must carry the descriptor so overloads stay distinct: " + trimmed);
            assertFalse(trimmed.substring(bar + 1).trim().isEmpty(),
                    "entry needs a note: " + trimmed);
        }
        assertTrue(entries > 0, "the baseline should not be empty while debt remains");
    }

    private static File repoFile(String relative) {
        // the tests run from vm/tests
        return new File("../..", relative).getAbsoluteFile();
    }

    /** Compiles one source file and analyzes the classes it produced. */
    private List<CastSemanticsVerifier.Violation> analyze(String... sourceLines) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "these tests need a JDK, not a JRE");

        Path dir = Files.createTempDirectory("cast-semantics");
        Path source = dir.resolve("Subject.java");
        StringBuilder text = new StringBuilder();
        for (String line : sourceLines) {
            text.append(line).append('\n');
        }
        Files.write(source, text.toString().getBytes(StandardCharsets.UTF_8));

        Path classes = Files.createDirectory(dir.resolve("classes"));
        int result = compiler.run(null, null, null,
                "-nowarn", "-g", "-d", classes.toString(), source.toString());
        assertEquals(0, result, "the test subject should compile:\n" + text);

        List<CastSemanticsVerifier.Violation> violations =
                new ArrayList<CastSemanticsVerifier.Violation>(
                        CastSemanticsVerifier.analyzeTree(classes.toFile()));
        return violations;
    }
}
