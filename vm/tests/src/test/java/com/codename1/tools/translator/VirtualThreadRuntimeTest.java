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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Builds and runs the virtual-thread runtime's own C test.
 *
 * The context switch is hand-written assembly, and the ways it can be wrong --
 * a clobbered callee-saved register, a stack that does not survive the round
 * trip -- do not show up as a compile error or as a crash near the cause. They
 * show up much later as a corrupted value in unrelated Java code, which is why
 * the checks live in C where they can watch specific registers rather than in a
 * translated program where they cannot.
 *
 * The test source used to be built by hand. Nothing ran it, so it was coverage
 * on paper only: the switch could have been broken in any commit and stayed
 * green. This drives it from the suite, out of the SAME staged resources a
 * generated project gets, so it also proves those resources are present and
 * mutually consistent -- the failure mode that shipped a project whose C half
 * had no assembly to link against.
 */
class VirtualThreadRuntimeTest {

    @Test
    @DisplayName("the virtual-thread switch preserves registers, stacks and ordering")
    void runtimeTestsPass() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the switch is not written for the Windows calling convention");
        }
        String arch = System.getProperty("os.arch", "");
        if (!arch.equals("aarch64") && !arch.equals("arm64")
                && !arch.equals("x86_64") && !arch.equals("amd64")) {
            Assumptions.abort("no context switch is written for " + arch);
        }

        Path work = Files.createTempDirectory("virtual-thread-runtime");
        // The same three resources emitVirtualThreadRuntime copies into a generated
        // project. Reading them from the classpath rather than from the source tree
        // means this fails when the build stops staging them, which is the thing that
        // silently produces a project that cannot link.
        for (String name : new String[] {
                "cn1_virtual_thread.h", "cn1_virtual_thread.c", "cn1_virtual_thread_asm.S" }) {
            try (InputStream in = ByteCodeTranslator.class.getResourceAsStream("/" + name)) {
                assertTrue(in != null, name + " is not staged on the translator classpath");
                Files.copy(in, work.resolve(name));
            }
        }

        Path testSource = Paths.get("virtualthread", "test_virtual_thread.c").toAbsolutePath();
        assertTrue(Files.exists(testSource), "missing " + testSource);

        Path binary = work.resolve("test_virtual_thread");
        List<String> compile = new ArrayList<>(Arrays.asList(
                "cc", "-O2", "-std=gnu11", "-I", work.toString(),
                testSource.toString(),
                work.resolve("cn1_virtual_thread.c").toString(),
                work.resolve("cn1_virtual_thread_asm.S").toString(),
                "-o", binary.toString()));
        String compileOutput = run(compile, 5);
        assertTrue(Files.exists(binary), "the runtime did not build:\n" + compileOutput);

        String output = run(Arrays.asList(binary.toString()), 5);
        assertTrue(output.contains("ALL VIRTUAL THREAD TESTS PASSED"),
                "the virtual-thread runtime reported a failure:\n" + output);
        // Not asserted as a number: the cost is hardware- and load-dependent, and a
        // threshold here would fail on a busy CI runner without anything being wrong.
        // Its presence proves the timing loop ran at all.
        assertTrue(output.contains("switch cost"),
                "the switch-cost measurement did not run:\n" + output);
    }

    private static String run(List<String> command, int timeoutMinutes) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process p = builder.start();

        // Drained on a SEPARATE thread, because the timeout below is worthless
        // otherwise. Reading inline blocks until the child closes stdout, so a
        // context-switch regression that hangs the binary would never reach waitFor
        // -- the Maven job would sit until CI killed it, instead of this test
        // failing. A timeout that the hang it guards against prevents from ever
        // being evaluated is not a timeout.
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Thread drain = new Thread(new Runnable() {
            public void run() {
                byte[] buffer = new byte[4096];
                int read;
                try {
                    while ((read = p.getInputStream().read(buffer)) > 0) {
                        synchronized (out) { out.write(buffer, 0, read); }
                    }
                } catch (java.io.IOException ignored) {
                    // the stream closes under us when the process is destroyed
                }
            }
        }, "vt-runtime-output");
        drain.setDaemon(true);
        drain.start();

        boolean finished = p.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
        }
        // Bounded join: the drain ends when the stream closes, which destroying the
        // process guarantees, but a bound here keeps a wedged reader from replacing
        // the hang this method just avoided.
        drain.join(TimeUnit.SECONDS.toMillis(30));
        String output;
        synchronized (out) { output = new String(out.toByteArray(), StandardCharsets.UTF_8); }
        if (!finished) {
            fail("timed out: " + command + "\n" + output);
        }
        assertEquals(0, p.exitValue(), "failed: " + command + "\n" + output);
        return output;
    }
}
