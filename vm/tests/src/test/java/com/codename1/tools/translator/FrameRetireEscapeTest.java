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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Frame-exit retirement stamps an object dead when the frame that allocated it
/// returns, on the strength of a proof that no reference to it left the frame. The
/// proof allows the object as the RECEIVER of a call only if the method that call
/// runs keeps `this` to itself, and these are the callees it used to wave through
/// without reading: a native, an override reached through a supertype, and a
/// callee's own callee.
///
/// The first one shipped. `Thread t = new Thread(r); t.start();` retired a running
/// thread's Thread object, the collector swept it, its finalizer released the
/// thread's native record, and the next root scan of that thread read unmapped
/// memory.
class FrameRetireEscapeTest {
    private static final String SOURCE = "class Retire {"
            + "static Object sink;"
            // Stands in for java.lang.Thread: start() is native and publishes `this`.
            + "static class Starter { int n; native void start(); void set(int v){ n = v; } }"
            + "static class Base { void run(){} }"
            + "static class Leaky extends Base { void run(){ sink = this; } }"
            + "static abstract class Task { abstract void go(); }"
            + "static class LeakyTask extends Task { void go(){ sink = this; } }"
            + "static class Chain { int n;"
            + "  void begin(){ register(); } void register(){ sink = this; }"
            + "  void count(){ bump(); } void bump(){ n++; } }"
            // The control: nothing leaves the frame, so the site must stay retirable.
            + "static int local(){ Starter s = new Starter(); s.set(3); return s.n; }"
            + "static int localChain(){ Chain c = new Chain(); c.count(); return c.n; }"
            + "static void started(){ Starter s = new Starter(); s.start(); }"
            + "static void overridden(){ Base b = new Leaky(); b.run(); }"
            + "static void implemented(){ Task t = new LeakyTask(); t.go(); }"
            + "static void transitive(){ Chain c = new Chain(); c.begin(); }"
            + "}";

    @BeforeEach
    @AfterEach
    void reset() {
        Parser.cleanup();
    }

    @Test
    void receiverCallsAreJudgedByTheMethodThatRuns() throws Exception {
        Path dir = Files.createTempDirectory("frame-retire");
        Path source = dir.resolve("Retire.java");
        Files.write(source, SOURCE.getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-d", dir.toString(), source.toString()));
        // Every constructor chain ends in Object.<init>, which the proof follows, so
        // the closed world needs an Object. The running JDK's is as good as any.
        Path objectClass = dir.resolve("Object.class");
        try (InputStream in = Object.class.getResourceAsStream("Object.class")) {
            assertNotNull(in, "the JDK must expose java/lang/Object.class");
            Files.copy(in, objectClass);
        }
        Parser.parse(objectClass.toFile());
        for (String name : new String[]{"Retire", "Retire$Starter", "Retire$Base",
                "Retire$Leaky", "Retire$Task", "Retire$LeakyTask", "Retire$Chain"}) {
            Parser.parse(dir.resolve(name + ".class").toFile());
        }

        // Non-vacuity first: an analysis that refuses everything passes every
        // assertion below it.
        assertTrue(retires("local"), "an object used only inside its frame is retirable");
        assertTrue(retires("localChain"),
                "a callee whose own callees keep `this` does not disqualify the site");

        assertFalse(retires("started"),
                "a native receiver call has no body to check: Thread.start() publishes `this`");
        assertFalse(retires("overridden"),
                "the override runs, not the superclass method the call site names");
        assertFalse(retires("implemented"),
                "an abstract method has no body; the implementation is what runs");
        assertFalse(retires("transitive"),
                "a callee that calls another method on `this` is only as safe as that one");
    }

    private static boolean retires(String methodName) {
        for (ByteCodeClass c : Parser.getClasses()) {
            if (!"Retire".equals(c.getClsName())) {
                continue;
            }
            for (BytecodeMethod m : c.getMethods()) {
                if (m.getMethodName().equals(methodName)) {
                    m.collectRetireCandidates();
                    m.analyzeRetirableLocals();
                    return m.frameRetireScopeDecl().length() > 0;
                }
            }
        }
        throw new AssertionError("no method " + methodName);
    }
}
