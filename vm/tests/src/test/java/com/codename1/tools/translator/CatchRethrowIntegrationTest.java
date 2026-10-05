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
package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An exception thrown from a catch handler leaves the whole try statement: the
 * statement's other handlers do not see it.
 *
 * A try with two catch clauses registers two try blocks, one per handler, and
 * throwException used to pop only the block that matched -- so when that handler
 * threw (a rethrow, or a new exception), its sibling handler for the SAME try was
 * still registered and caught it. {@code catch (IOException e) { throw e; } catch
 * (Exception e) { ... }} swallowed the rethrow on every native target; JUnit's
 * assertAll stand-in, which rethrows an OutOfMemoryError ahead of a catch
 * (Throwable), is how it surfaced.
 */
class CatchRethrowIntegrationTest {

    /// One compiler, not the diagonal: the bug is in how the translator emits
    /// handlers, which no javac version changes, and each configuration costs a
    /// full translate and native build -- across all of them this test alone was
    /// ten minutes of a job that has a ninety-minute limit.
    static java.util.stream.Stream<CompilerHelper.CompilerConfig> oneCompilerConfig() {
        return CompilerHelper.getDiagonalCompilers().stream()
                .filter(CompilerHelper::isJavaApiCompatible)
                .limit(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("oneCompilerConfig")
    void anExceptionFromAHandlerLeavesTheTryStatement(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("rethrow-sources");
        Path classesDir = Files.createTempDirectory("rethrow-classes");
        Path javaApiDir = Files.createTempDirectory("rethrow-java-api");
        Files.write(sourceDir.resolve("RethrowApp.java"), source().getBytes(StandardCharsets.UTF_8));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, sourceDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("rethrow-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "RethrowApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "RethrowApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("RethrowApp"));
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=127"),
                "an exception thrown from a catch handler was caught by a sibling handler:\n" + output);
    }

    private static String source() {
        return "public class RethrowApp {\n"
                + "    static int cleanups;\n"
                + "    static void fail(String why) { throw new IllegalStateException(why); }\n"
                // A typed handler rethrows; the broader sibling must not see it.
                + "    static int rethrow() {\n"
                + "        try { fail(\"a\"); } catch (IllegalStateException e) { throw e; }\n"
                + "        catch (RuntimeException e) { return 100; }\n"
                + "        return 200;\n"
                + "    }\n"
                // A typed handler throws something new; same.
                + "    static int throwNew() {\n"
                + "        try { fail(\"b\"); } catch (IllegalStateException e) {\n"
                + "            throw new UnsupportedOperationException(\"from the handler\"); }\n"
                + "        catch (RuntimeException e) { return 100; }\n"
                + "        return 200;\n"
                + "    }\n"
                // The same inside a try/finally: the finally runs once.
                + "    static int withFinally() {\n"
                + "        try {\n"
                + "            try { fail(\"c\"); } catch (IllegalStateException e) { throw e; }\n"
                + "            catch (RuntimeException e) { return 100; }\n"
                + "        } finally { cleanups++; }\n"
                + "        return 200;\n"
                + "    }\n"
                // A handler of an inner try that throws is still caught by an OUTER try.
                + "    static int nested() {\n"
                + "        try {\n"
                + "            try { fail(\"d\"); } catch (IllegalStateException e) { throw e; }\n"
                + "            catch (RuntimeException e) { return 100; }\n"
                + "        } catch (IllegalStateException outer) { return 7; }\n"
                + "        return 200;\n"
                + "    }\n"
                // SelfTest's long-skipped VM probe: a catch around a try/finally in
                // the same method must see what the finally rethrows.
                + "    static int touched;\n"
                + "    static String nestedFinally() {\n"
                + "        try {\n"
                + "            try { throw new java.io.IOException(\"thrown in the inner try\"); }\n"
                + "            finally { touched++; }\n"
                + "        } catch (Exception caught) { return \"caught\"; }\n"
                + "    }\n"
                + "    public static void main(String[] args) {\n"
                + "        int score = 0;\n"
                + "        try { rethrow(); } catch (IllegalStateException e) {\n"
                + "            if (\"a\".equals(e.getMessage())) { score |= 1; } }\n"
                + "        try { throwNew(); } catch (UnsupportedOperationException e) { score |= 2; }\n"
                + "        try { withFinally(); } catch (IllegalStateException e) { score |= 4; }\n"
                + "        if (cleanups == 1) { score |= 8; }\n"
                + "        if (nested() == 7) { score |= 16; }\n"
                + "        try { fail(\"e\"); } catch (RuntimeException e) { score |= 32; }\n"
                + "        if (\"caught\".equals(nestedFinally()) && touched == 1) { score |= 64; }\n"
                + "        System.out.println(\"RESULT=\" + score);\n"
                + "    }\n"
                + "}\n";
    }
}
