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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the interaction between the timeout watcher and the output reader in
 * {@code Executor.executeProcess}: the translator's out-of-memory diagnostic
 * needs the captured output to be complete when exec() returns, which means
 * joining the reader -- and joining the reader must not leave the timeout
 * watcher running past the deadline on a process that already finished.
 *
 * <p>The child processes are JVMs running the helpers below rather than shell
 * commands, so these run on Windows as well as Unix.
 */
class ExecutorProcessTimeoutTest {

    /** Executor is abstract; none of these hooks are exercised here. */
    private static final class TestExecutor extends Executor {
        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }
    }

    /** Sleeps, keeping whatever stdout it inherited open for that long. */
    public static final class Sleeper {
        public static void main(String[] args) throws Exception {
            Thread.sleep(Long.parseLong(args[0]));
        }
    }

    /**
     * Starts a {@link Sleeper} that inherits this process's stdout, then exits
     * immediately. The pipe therefore outlives this process, which is what makes
     * the reader join in executeProcess actually block.
     */
    public static final class ExitsLeavingChildHoldingOutput {
        public static void main(String[] args) throws Exception {
            ProcessBuilder pb = javaProcess(Sleeper.class, args[0]);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.start();
            System.out.println("started");
            System.out.flush();
        }
    }

    /** Prints many lines and then an out-of-memory signature as the very last line. */
    public static final class PrintsThenFailsWithOom {
        public static void main(String[] args) {
            for (int i = 1; i <= 500; i++) {
                System.out.println("line-" + i);
            }
            System.out.println("java.lang.OutOfMemoryError: Java heap space");
            System.out.flush();
        }
    }

    /** A JVM launch of the given helper class, portable across platforms. */
    private static ProcessBuilder javaProcess(Class<?> main, String... args) {
        String java = new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
        java.util.List<String> cmd = new java.util.ArrayList<String>();
        cmd.add(java);
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(main.getName());
        for (String a : args) {
            cmd.add(a);
        }
        return new ProcessBuilder(cmd);
    }

    @Test
    void aProcessThatSucceedsIsNotReportedAsTimedOutWhileOutputIsStillDraining() throws Exception {
        // The regression this guards: joining the reader thread after waitFor()
        // left the timeout watcher counting, so a command that had already
        // exited 0 could be reported as timed out if the deadline passed while
        // the join was in progress -- surfacing as a random "translator failed"
        // on the iOS and native steps, which are the ones that run with a timeout.
        //
        // Reproducing it needs the join to actually block, which means the pipe
        // must outlive the process. Without the fix the watcher fires at 1000ms
        // during that wait and exec returns 1.
        TestExecutor e = new TestExecutor();
        int rc = e.executeProcess(javaProcess(ExitsLeavingChildHoldingOutput.class, "4000"), 1000);

        assertEquals(0, rc, "a command that exited 0 must not be reported as timed out");
    }

    @Test
    void outputIsCompleteWhenExecReturns() throws Exception {
        // The out-of-memory check reads the captured output immediately after
        // exec() returns, and a JVM prints OutOfMemoryError at the very end of
        // the stream -- so a truncated tail is the failure that matters.
        TestExecutor e = new TestExecutor();
        int mark = e.message.length();
        int rc = e.executeProcess(javaProcess(PrintsThenFailsWithOom.class), -1);
        String out = e.message.substring(mark);

        assertEquals(0, rc);
        assertTrue(TranslatorHeap.looksOutOfMemory(out),
                "the tail of the output must be present when exec returns");
        assertTrue(out.contains("line-500"), "output must not be truncated");
    }

    @Test
    void aProcessThatOverrunsItsDeadlineIsStillReportedAsTimedOut() throws Exception {
        TestExecutor e = new TestExecutor();
        int rc = e.executeProcess(javaProcess(Sleeper.class, "30000"), 1000);
        assertEquals(1, rc, "a genuine timeout must still fail");
    }
}
