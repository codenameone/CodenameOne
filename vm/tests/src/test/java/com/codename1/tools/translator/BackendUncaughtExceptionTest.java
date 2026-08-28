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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An exception no handler catches must end a clean-target program, loudly.
 *
 * It used to be discarded: throwException walked the try-block stack, found no
 * handler, and RETURNED -- so the generated code carried straight on with the
 * statement after the throw, with the method's locals in whatever state the
 * failed operation left them. On an app target something upstream (the EDT's own
 * catch) nearly always exists, which is why it went unnoticed for years. A server
 * binary has none, and the way this surfaced was a database client whose TLS
 * handshake was rejected, after which the program kept going and segfaulted two
 * statements later on a null it should never have had.
 *
 * The three assertions below are the contract: the message is printed, a stack
 * trace is printed, and the process exits non-zero. All three matter -- an exit
 * code with no message is unactionable in a log, and a message with a zero exit
 * makes CI call a failed run a pass.
 */
class BackendUncaughtExceptionTest {

    @Test
    @DisplayName("an uncaught exception reports itself and ends the process")
    void uncaughtExceptionIsFatal() throws Exception {
        if (CompilerHelper.isWindows()) {
            Assumptions.abort("the server-side backend is POSIX-only for now");
        }
        BackendTestSupport.require(Files.isDirectory(BackendTestSupport.backendDir()),
                "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to build the backend");

        Path work = Files.createTempDirectory("backend-uncaught");
        Path binary = work.resolve("uncaught");
        String failure = BackendTestSupport.build("Uncaught", "demo/uncaught", binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
        }

        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.redirectErrorStream(true);
        Process p = run.start();
        String output = BackendTestSupport.readFully(p.getInputStream());
        if (!p.waitFor(2, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            fail("the program did not finish:\n" + output);
        }

        assertTrue(output.indexOf("before the throw") >= 0,
                "the program should have run up to the throw:\n" + output);
        assertTrue(output.indexOf("deliberate failure with a message") >= 0,
                "the exception's message must be reported, not just its type:\n" + output);
        assertTrue(output.indexOf("com_demo_Uncaught.open") >= 0,
                "a stack trace naming the throwing frame must be reported:\n" + output);
        assertEquals(1, p.exitValue(),
                "a program killed by an uncaught exception must not report success:\n" + output);
    }
}
