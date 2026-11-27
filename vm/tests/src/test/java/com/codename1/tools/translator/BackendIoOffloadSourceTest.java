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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The source half of the guard that keeps local I/O off the virtual-thread
/// hosts; BackendHttpIntegrationTest's "...does not hold the virtual-thread
/// host" tests are the runtime half.
///
/// The runtime test can only block what a test can make block on purpose: a
/// SQLite step. (A FIFO open used to be the other one; opens no longer wait
/// for a FIFO's writer, so nothing in the file natives can be made to stall.) A read, pread, write, mkstemp,
/// realpath or sendfile that went back to running inline on the host would pass
/// them. So this checks the shape directly, with no build: in the two native
/// files that do local I/O, every blocking call sits inside a job function --
/// the code cn1BackendOffload runs on its pool -- and never in a native's own
/// body. It also refuses a `synchronized` method on the SQLite connection,
/// because a virtual thread holding a monitor is pinned and cannot park, which
/// makes every offload under it inert.
class BackendIoOffloadSourceTest {

    /// The calls that wait on the disk or on a database lock.
    private static final Pattern BLOCKING = Pattern.compile(
            "(?<![A-Za-z0-9_])(open|read|pread|write|mkstemp|realpath|sendfile"
            + "|sqlite3_open|sqlite3_prepare_v2|sqlite3_step|sqlite3_close)\\s*\\("
            + "|SYS_openat2\\s*,");

    /// A top-level C function definition, by its first line.
    private static final Pattern FUNCTION = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_ \\*]*?([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^;]*\\)\\s*\\{\\s*$");

    /// Helpers that are not themselves jobs but are only ever called from one.
    private static final Set<String> JOB_HELPERS = new HashSet<>(Arrays.asList(
            "cn1OpenReadPath", "cn1BackendDanglingLinkIn"));
    /// Blocking calls that must stay on the calling thread. A connection with
    /// SQL functions written in Java steps here: such a function runs inside
    /// sqlite3_step and re-enters the VM on the thread it is called on, which
    /// an offload pool thread is not. The thread is yielded around the step,
    /// so the collector is not held up.
    private static final Set<String> INLINE_BY_DESIGN = new HashSet<>(Arrays.asList(
            "com_codename1_backend_Db_stepInlineImpl___long_R_int"));

    @Test
    void everyBlockingCallInTheIoNativesRunsInAnOffloadJob() throws Exception {
        Path nativeDir = BackendTestSupport.backendDir().resolve("native");
        List<String> violations = new ArrayList<>();
        for (String name : new String[] {"cn1_backend_files.c", "cn1_backend_db.c"}) {
            Path file = nativeDir.resolve(name);
            assertTrue(Files.isRegularFile(file), "missing " + file);
            String[] lines = new String(Files.readAllBytes(file), StandardCharsets.US_ASCII)
                    .split("\n", -1);
            String function = null;
            boolean inComment = false;
            int offloads = 0;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher def = FUNCTION.matcher(line);
                if (def.matches()) {
                    function = def.group(1);
                }
                String code = stripComments(line, inComment);
                inComment = endsInComment(line, inComment);
                if (code.contains("cn1BackendOffload(")) {
                    offloads++;
                }
                if (function == null || !BLOCKING.matcher(code).find()) {
                    continue;
                }
                if (function.endsWith("Job") || function.endsWith("JobRun")
                        || JOB_HELPERS.contains(function) || INLINE_BY_DESIGN.contains(function)) {
                    continue;
                }
                violations.add(name + ":" + (i + 1) + " in " + function + ": " + line.trim());
            }
            assertTrue(offloads > 0, name + " no longer calls cn1BackendOffload at all");
        }
        // The io_uring half: the ring exists, and the plain file natives try it.
        String offload = new String(Files.readAllBytes(nativeDir.resolve("cn1_backend_offload.c")),
                StandardCharsets.US_ASCII);
        assertTrue(offload.contains("__NR_io_uring_setup") && offload.contains("IOSQE_IO_HARDLINK"),
                "cn1_backend_offload.c lost its io_uring path; Linux file I/O would all hop "
                + "to the pool");
        String files = new String(Files.readAllBytes(nativeDir.resolve("cn1_backend_files.c")),
                StandardCharsets.US_ASCII);
        for (String call : new String[] {"cn1BackendUringRead(fd", "cn1BackendUringWrite(fd",
                "cn1BackendUringOpenat2("}) {
            assertTrue(files.contains(call), "cn1_backend_files.c no longer calls " + call);
        }
        assertTrue(violations.isEmpty(),
                "a blocking call runs on the caller's thread instead of in an offload job. "
                + "On a virtual thread that holds the host -- and every request on it -- "
                + "for the whole call. Move it into a *Job function and run that through "
                + "cn1BackendOffload (see cn1_backend_offload.c):\n  "
                + String.join("\n  ", violations));
    }

    @Test
    void theSqliteConnectionIsNotGuardedByAMonitor() throws Exception {
        Path db = BackendTestSupport.backendDir()
                .resolve("impl/parparvm/com/codename1/backend/Db.java");
        Path pool = BackendTestSupport.backendDir()
                .resolve("src/com/codename1/backend/DbPool.java");
        String dbSource = new String(Files.readAllBytes(db), StandardCharsets.US_ASCII);
        String poolSource = new String(Files.readAllBytes(pool), StandardCharsets.US_ASCII);
        // A synchronized METHOD; the gate's own synchronized (gate) blocks are
        // the instant-long critical sections it is built from, and are fine.
        assertFalse(Pattern.compile("synchronized\\s+[A-Za-z_][\\w<>\\[\\]]*\\s+"
                        + "[A-Za-z_]\\w*\\s*\\(").matcher(dbSource).find(),
                "Db has a synchronized method again. A virtual thread holding a monitor "
                + "is pinned and cannot park, so its SQLite step would hold the host for "
                + "the whole query. Use the connection gate (Db.enter/exit).");
        assertFalse(Pattern.compile("synchronized\\s+Db\\s+borrow\\s*\\(").matcher(poolSource).find(),
                "DbPool.borrow is synchronized again. A virtual thread waiting in it "
                + "wait()s on the host's monitor and holds the host until a connection "
                + "frees up. Nap outside the monitor, as borrow does now.");
    }

    private static String stripComments(String line, boolean inComment) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean comment = inComment;
        while (i < line.length()) {
            if (comment) {
                int end = line.indexOf("*/", i);
                if (end < 0) {
                    return out.toString();
                }
                i = end + 2;
                comment = false;
            } else if (line.startsWith("/*", i)) {
                comment = true;
                i += 2;
            } else if (line.startsWith("//", i)) {
                return out.toString();
            } else {
                out.append(line.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    private static boolean endsInComment(String line, boolean inComment) {
        boolean comment = inComment;
        int i = 0;
        while (i < line.length()) {
            if (comment) {
                int end = line.indexOf("*/", i);
                if (end < 0) {
                    return true;
                }
                i = end + 2;
                comment = false;
            } else if (line.startsWith("/*", i)) {
                comment = true;
                i += 2;
            } else if (line.startsWith("//", i)) {
                return false;
            } else {
                i++;
            }
        }
        return comment;
    }
}
