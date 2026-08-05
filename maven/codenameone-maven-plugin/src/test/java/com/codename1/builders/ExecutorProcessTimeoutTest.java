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

    @Test
    void aProcessThatSucceedsIsNotReportedAsTimedOutWhileOutputIsStillDraining() throws Exception {
        // The regression this guards: joining the reader thread after waitFor()
        // left the timeout watcher counting, so a command that had already
        // exited 0 could be reported as timed out if the deadline passed while
        // the join was in progress -- surfacing as a random "translator failed"
        // on the iOS and native steps, which are the ones that run with a timeout.
        //
        // Reproducing it needs the join to actually block, which means the pipe
        // must outlive the process: the shell exits immediately while a
        // backgrounded grandchild keeps the inherited stdout open. Without the
        // fix the watcher fires at 1000ms during that wait and exec returns 1.
        TestExecutor e = new TestExecutor();
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "sleep 3 & echo started; exit 0");
        int rc = e.executeProcess(pb, 1000);

        assertEquals(0, rc, "a command that exited 0 must not be reported as timed out");
    }

    @Test
    void outputIsCompleteWhenExecReturns() throws Exception {
        // The out-of-memory check reads the captured output immediately after
        // exec() returns, and a JVM prints OutOfMemoryError at the very end of
        // the stream -- so a truncated tail is the failure that matters.
        TestExecutor e = new TestExecutor();
        StringBuilder sb = new StringBuilder();
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c",
                "for i in $(seq 1 500); do echo line-$i; done; echo java.lang.OutOfMemoryError: Java heap space");
        int rc = executeCapturing(e, pb, sb);

        assertEquals(0, rc);
        assertTrue(TranslatorHeap.looksOutOfMemory(sb.toString()),
                "the tail of the output must be present when exec returns");
        assertTrue(sb.toString().contains("line-500"), "output must not be truncated");
    }

    /** executeProcess(pb, timeout) appends into the executor's own message buffer. */
    private static int executeCapturing(TestExecutor e, ProcessBuilder pb, StringBuilder sink) throws Exception {
        int mark = e.message.length();
        int rc = e.executeProcess(pb, -1);
        sink.append(e.message.substring(mark));
        return rc;
    }

    @Test
    void aProcessThatOverrunsItsDeadlineIsStillReportedAsTimedOut() throws Exception {
        TestExecutor e = new TestExecutor();
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "sleep 30");
        int rc = e.executeProcess(pb, 1000);
        assertEquals(1, rc, "a genuine timeout must still fail");
    }
}
