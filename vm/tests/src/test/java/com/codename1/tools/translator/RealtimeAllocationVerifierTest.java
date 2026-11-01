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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A real-time task's `cycle()` and `periodsMissed(int)` must not allocate; the
/// fixture defines its own `RealtimeTask` and `AllocationAllowed` because the real
/// ones are not on the translator's class path.
class RealtimeAllocationVerifierTest {
    @TempDir
    Path directory;

    @BeforeEach
    @AfterEach
    void cleanParser() {
        Parser.cleanup();
    }

    private static final String[] STUBS = {
        "package com.codename1.realtime; public abstract class RealtimeTask {"
            + " public void setup() {} public abstract void cycle();"
            + " public void periodsMissed(int count) {} public void failed(Throwable t) {} }",
        "package com.codename1.realtime; @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)"
            + " public @interface AllocationAllowed {}",
    };

    private String verify(String body) throws Exception {
        StringBuilder src = new StringBuilder("import com.codename1.realtime.*;\n"
                + "class Boom extends Throwable {}\n"
                + "public class Task extends RealtimeTask {\n int n;\n").append(body).append("\n}\n");
        String[] sources = {STUBS[0], STUBS[1], src.toString()};
        String[] names = {"RealtimeTask", "AllocationAllowed", "Task"};
        java.util.List<String> args = new java.util.ArrayList<String>();
        args.add("-source");
        args.add("8");
        args.add("-target");
        args.add("8");
        args.add("-d");
        args.add(directory.toString());
        for (int i = 0; i < sources.length; i++) {
            Path f = directory.resolve("src").resolve(names[i] + ".java");
            Files.createDirectories(f.getParent());
            Files.write(f, sources[i].getBytes(StandardCharsets.UTF_8));
            args.add(f.toString());
        }
        assertTrue(javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                args.toArray(new String[0])) == 0);
        for (String cls : new String[]{"com/codename1/realtime/RealtimeTask",
            "com/codename1/realtime/AllocationAllowed", "Boom", "Task"}) {
            Parser.parse(new File(directory.toFile(), cls + ".class"));
        }
        try {
            RealtimeAllocationVerifier.run(Parser.getClasses());
            return null;
        } catch (NativeSignatureVerifier.VerificationFailedException ex) {
            return ex.getMessage();
        }
    }

    @Test
    void cleanCyclePasses() throws Exception {
        assertDoesNotThrow(() -> assertTrue(verify("public void cycle() { n++; }") == null));
    }

    @Test
    void newObjectFails() throws Exception {
        String m = verify("public void cycle() { Object o = new Object(); }");
        assertTrue(m != null && m.contains("new java.lang.Object") && m.contains("Task.cycle()V"), m);
    }

    @Test
    void boxingAndArraysFail() throws Exception {
        String m = verify("public void cycle() { Integer i = Integer.valueOf(n); }");
        assertTrue(m != null && m.contains("boxing Integer.valueOf"), m);
        Parser.cleanup();
        m = verify("public void cycle() { byte[] b = new byte[4]; }");
        assertTrue(m != null && m.contains("primitive array"), m);
    }

    @Test
    void periodsMissedIsARootToo() throws Exception {
        String m = verify("public void cycle() {} public void periodsMissed(int c) { Object o = new int[c][2]; }");
        assertTrue(m != null && m.contains("periodsMissed"), m);
    }

    @Test
    void allocationTwoCallsDeepNamesTheChain() throws Exception {
        String m = verify("public void cycle() { a(); }"
                + " static void a() { b(); } static void b() { Object o = new Object(); }");
        assertTrue(m != null && m.contains("Task.b()V") && m.contains("called from Task.a()V <- Task.cycle()V"), m);
        assertTrue(m.contains("Task.java:"), m);
    }

    @Test
    void throwingPasses() throws Exception {
        assertTrue(verify("public void cycle() { try { if (n < 0) { throw new Boom(); } } catch (Boom b) { n = 0; } }") == null);
    }

    @Test
    void allocationAllowedSuppresses() throws Exception {
        assertTrue(verify("public void cycle() { a(); }"
                + " @AllocationAllowed static void a() { Object o = new Object(); }") == null);
    }
}
