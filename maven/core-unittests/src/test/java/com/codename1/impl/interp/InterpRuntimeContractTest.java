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
package com.codename1.impl.interp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The device runtime's own guarantees, as distinct from "does it execute
 * bytecode correctly" -- that is the conformance suite's job.
 *
 * <p>These are the properties that make it safe to hand a phone to a stranger
 * with a socket open: a runaway program can be stopped, a program cannot hold
 * the event thread indefinitely, runaway recursion is a diagnosable error
 * rather than a process death, failures name the user's own source lines, and
 * code whose source cannot be shown does not run at all.</p>
 *
 * @author Shai Almog
 */
class InterpRuntimeContractTest {

    private static InterpRuntime load(String className, String source) throws Exception {
        Path dir = Files.createTempDirectory("interp-contract");
        Path src = dir.resolve(className + ".java");
        Files.write(src, source.getBytes(StandardCharsets.UTF_8));
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        int rc = javac.run(null, null, null, "-g", "-nowarn", "-XDstringConcat=inline",
                "-d", dir.toString(), src.toString());
        if (rc != 0) {
            throw new IllegalStateException("fixture did not compile");
        }
        byte[] bundleBytes = InterpTestHarness.buildBundle(dir, className, source);
        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(bundleBytes));
        ReflectionInterpLinker linker = new ReflectionInterpLinker();
        ProxyInterpObjectFactory factory = new ProxyInterpObjectFactory(linker);
        InterpRuntime rt = new InterpRuntime(bundle, linker, factory);
        factory.attach(rt);
        return rt;
    }

    /**
     * A pushed program that never returns has to be stoppable, or the Stop
     * button is decoration and the only recovery is killing the app. The
     * existing BeanShell playground has no such check.
     */
    @Test
    @DisplayName("a runaway loop can be cancelled from another thread")
    void aRunawayLoopCanBeCancelled() throws Exception {
        final InterpRuntime rt = load("Spin",
                "public class Spin { public static void main(String[] a) {"
                + " long n = 0; while (true) { n++; } }}");
        rt.setEdtBudgetMs(0);

        final AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
        Thread runner = new Thread(new Runnable() {
            public void run() {
                try {
                    rt.runMain(new String[0]);
                } catch (Throwable t) {
                    outcome.set(t);
                }
            }
        });
        runner.start();
        Thread.sleep(300);
        rt.requestCancel();
        runner.join(15000);

        assertTrue(!runner.isAlive(), "the interpreter did not stop when asked");
        Throwable t = outcome.get();
        assertNotNull(t, "cancelling should surface as a throwable");
        assertTrue(unwrap(t) instanceof InterpCancelled,
                "expected cancellation, got " + unwrap(t));
    }

    /**
     * Holding the event thread is the failure users actually hit. The budget
     * turns it into an error naming the elapsed time rather than a frozen app.
     */
    @Test
    @DisplayName("a program that never yields is stopped by the EDT budget")
    void theEdtBudgetStopsANonYieldingProgram() throws Throwable {
        InterpRuntime rt = load("Hog",
                "public class Hog { public static void main(String[] a) {"
                + " long n = 0; while (true) { n++; } }}");
        rt.setEdtBudgetMs(250);

        long started = System.currentTimeMillis();
        final InterpRuntime runtime = rt;
        Throwable e = runWhereTheBudgetApplies(new Runnable() {
            public void run() {
                try {
                    runtime.runMain(new String[0]);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }
            }
        });
        assertNotNull(e, "expected the EDT budget to fire");
        assertTrue(unwrap(e) instanceof InterpCancelled,
                "expected cancellation, got " + unwrap(e));
        long elapsed = System.currentTimeMillis() - started;
        assertTrue(elapsed < 15000,
                "the budget should fire promptly, took " + elapsed + "ms");
        assertTrue(e.getMessage().indexOf("without yielding") > 0,
                "the message should explain why, was: " + e.getMessage());
    }

    /// The budget covers one entry into the interpreter, not the session.
    ///
    /// A real application's callbacks arrive long after its main returned --
    /// every button press is one -- and each is a fresh entry. Measuring from
    /// the start of the run instead made the budget expire once and stay
    /// expired, so every later callback failed instantly with "ran without
    /// yielding" having executed nothing.
    @Test
    @DisplayName("a callback long after main still gets a full budget")
    void theBudgetIsPerEntryNotPerSession() throws Throwable {
        InterpRuntime rt = load("Later",
                "public class Later {"
                + " public static void main(String[] a) { }"
                + " public static int work() {"
                + "   int n = 0; for (int i = 0; i < 200000; i++) { n += i; } return n; } }");
        rt.setEdtBudgetMs(2000);
        rt.runMain(new String[0]);

        // Longer than the budget, so a session-wide clock would already be spent.
        Thread.sleep(2200);

        InterpClass c = rt.getBundle().findClass("Later");
        InterpMethod work = c.declaredMethod("work", "()I");
        assertNotNull(work, "work() should be present");
        Object result = rt.invoke(work, null, new Object[0]);
        // Computed the same way rather than hard-coded, so the assertion says
        // "the same as Java" instead of encoding an arithmetic slip.
        int expected = 0;
        for (int i = 0; i < 200000; i++) {
            expected += i;
        }
        assertEquals(Integer.valueOf(expected), result,
                "the callback should run to completion");
    }

    /** Runaway recursion must be diagnosable, not a native stack overflow. */
    @Test
    @DisplayName("unbounded recursion raises a StackOverflowError with an interpreted trace")
    void unboundedRecursionIsBounded() throws Throwable {
        InterpRuntime rt = load("Deep",
                "public class Deep { static int f(int n) { return f(n + 1); }"
                + " public static void main(String[] a) { System.out.println(f(0)); } }");
        rt.setEdtBudgetMs(0);
        rt.setMaxDepth(64);

        try {
            rt.runMain(new String[0]);
            throw new AssertionError("expected the depth cap to fire");
        } catch (InterpThrowable e) {
            assertTrue(e.getThrown() instanceof StackOverflowError,
                    "expected StackOverflowError, got " + e.getThrown());
            String[] stack = e.getInterpretedStack();
            assertTrue(stack.length > 0, "an interpreted trace should be captured");
            assertTrue(stack[0].startsWith("Deep.f("),
                    "innermost frame should be the recursing method, was " + stack[0]);
            assertTrue(stack[0].indexOf("Deep.java:") > 0,
                    "the frame should name the user's source line, was " + stack[0]);
        }
    }

    /**
     * A failure has to point at the user's code. A real stack trace would name
     * the interpreter's frames, which tells the user nothing about their bug.
     */
    @Test
    @DisplayName("an uncaught failure reports the user's own source lines")
    void failuresNameUserSourceLines() throws Throwable {
        InterpRuntime rt = load("Boom",
                "public class Boom {\n"
                + "  static int div(int a, int b) {\n"
                + "    return a / b;\n"
                + "  }\n"
                + "  public static void main(String[] args) {\n"
                + "    System.out.println(div(1, 0));\n"
                + "  }\n"
                + "}\n");
        rt.setEdtBudgetMs(0);

        try {
            rt.runMain(new String[0]);
            throw new AssertionError("expected an ArithmeticException");
        } catch (InterpThrowable e) {
            assertTrue(e.getThrown() instanceof ArithmeticException,
                    "expected ArithmeticException, got " + e.getThrown());
            String trace = e.getInterpretedStackTrace();
            assertTrue(trace.indexOf("Boom.div(Boom.java:3)") > 0,
                    "trace should point at the division, was:\n" + trace);
            assertTrue(trace.indexOf("Boom.main(Boom.java:6)") > 0,
                    "trace should include the caller, was:\n" + trace);
        }
    }

    /**
     * Apple allows an app to download and run code only where the user can see
     * and edit the source (guideline 2.5.2). The runtime enforces that rather
     * than relying on the tool chain to have included it.
     */
    @Test
    @DisplayName("a bundle without sources is refused")
    void aBundleWithoutSourcesIsRefused() throws Exception {
        Path dir = Files.createTempDirectory("interp-nosource");
        String source = "public class Bare { public static void main(String[] a) {} }";
        Path src = dir.resolve("Bare.java");
        Files.write(src, source.getBytes(StandardCharsets.UTF_8));
        javax.tools.ToolProvider.getSystemJavaCompiler()
                .run(null, null, null, "-g", "-nowarn", "-d", dir.toString(), src.toString());

        Class<?> writerClass = Class.forName("com.codename1.tools.translator.InterpBundleWriter");
        Object writer = writerClass.getDeclaredConstructor().newInstance();
        writerClass.getMethod("addClassFile", java.io.File.class)
                .invoke(writer, dir.resolve("Bare.class").toFile());
        writerClass.getMethod("setMainClass", String.class).invoke(writer, "Bare");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        writerClass.getMethod("write", java.io.OutputStream.class).invoke(writer, bos);

        final byte[] bundleBytes = bos.toByteArray();
        java.io.IOException e = assertThrows(java.io.IOException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable {
                        InterpBundleReader.read(new ByteArrayInputStream(bundleBytes));
                    }
                });
        assertTrue(e.getMessage().indexOf("source") >= 0,
                "the refusal should say why, was: " + e.getMessage());
    }

    /**
     * A bundle built by a newer SDK than the installed app must fail with a
     * clear message. The app ships on a store cadence and the SDK does not, so
     * this mismatch is normal rather than exceptional.
     */
    @Test
    @DisplayName("a bundle from an unknown format version is refused by version")
    void anUnknownFormatVersionIsRefused() throws Exception {
        Path dir = Files.createTempDirectory("interp-version");
        String source = "public class V { public static void main(String[] a) {} }";
        Files.write(dir.resolve("V.java"), source.getBytes(StandardCharsets.UTF_8));
        javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-g", "-nowarn", "-d", dir.toString(), dir.resolve("V.java").toString());
        byte[] good = InterpTestHarness.buildBundle(dir, "V", source);

        final byte[] tampered = good.clone();
        // Bytes 4..7 are the version, immediately after the magic word.
        tampered[4] = 0;
        tampered[5] = 0;
        tampered[6] = 0;
        tampered[7] = 99;

        java.io.IOException e = assertThrows(java.io.IOException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable {
                        InterpBundleReader.read(new ByteArrayInputStream(tampered));
                    }
                });
        assertTrue(e.getMessage().indexOf("version") >= 0,
                "the refusal should mention the version, was: " + e.getMessage());
    }

    /**
     * An interpreted class implementing a host interface has to be usable
     * <em>as</em> that interface by host code that has never heard of it. This
     * is the half of the object-factory problem that reflection solves
     * completely; extending a host class is the half that needs a per-platform
     * mechanism.
     */
    @Test
    @DisplayName("an interpreted class implementing a host interface is callable from host code")
    void anInterpretedClassCanImplementAHostInterface() throws Throwable {
        InterpRuntime rt = load("Callback",
                "import java.util.concurrent.Callable;\n"
                + "public class Callback implements Callable<String> {\n"
                + "  private final String tag;\n"
                + "  public Callback(String tag) { this.tag = tag; }\n"
                + "  public String call() { return \"called:\" + tag; }\n"
                + "  public static Object make() { return new Callback(\"x\"); }\n"
                + "  public static void main(String[] a) {}\n"
                + "}\n");
        rt.setEdtBudgetMs(0);

        InterpClass c = rt.getBundle().findClass("Callback");
        assertNotNull(c, "the bundle should contain the class");
        InterpMethod make = c.declaredMethod("make", "()Ljava/lang/Object;");
        assertNotNull(make, "make() should be present");

        Object peer = rt.invoke(make, null, new Object[0]);
        assertTrue(peer instanceof java.util.concurrent.Callable,
                "the peer should be usable as the host interface, was "
                        + (peer == null ? "null" : peer.getClass().getName()));
        // Host code calling through the interface, with no knowledge of the
        // interpreter, must reach the interpreted body.
        assertEquals("called:x", ((java.util.concurrent.Callable) peer).call());
    }

    /// A deviation from the JVM, asserted so it stays a known one.
    ///
    /// The interpreter represents every reference array as `Object[]`. Nothing
    /// records the component type an `anewarray` was given, so `instanceof`
    /// against an array type is answered by inspecting the elements -- which
    /// makes an empty array satisfy any component type, where the JVM would
    /// compare the array's own runtime type and say no.
    ///
    /// It is the safe direction of the two. Answering `false` for an empty
    /// array would break the cast in every generated `values()`, and an enum
    /// with no constants is far more ordinary than code that asks whether an
    /// `Object[]` is a `Color[]`.
    @Test
    @DisplayName("an empty array satisfies any component type -- a known deviation")
    void emptyArrayInstanceOfIsPermissive() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("EmptyArrayCast",
                "public class EmptyArrayCast {"
                + " static class E {}"
                + " public static void main(String[] a) {"
                + " System.out.println(new Object[0] instanceof E[]);"
                + "}}");
        assertEquals("false", r[0].output.trim(), "the JVM compares the array's own type");
        assertEquals("true", r[1].output.trim(),
                "the interpreter has no component type to compare and inspects elements");
    }

    /// A class initializer that throws must not leave the class looking
    /// initialized.
    ///
    /// The obvious implementation is one boolean set before `<clinit>` runs --
    /// which is genuinely necessary, since an initializer that reaches back
    /// into its own class has to be let through or it recurses forever. What
    /// that boolean cannot express is failure: leave it set after the
    /// initializer throws and every later read of a static field returns
    /// whatever half of the initializer managed to assign, and the program
    /// misbehaves somewhere far away instead of reporting the class as broken.
    ///
    /// The first touch surfaces ExceptionInInitializerError, as the JVM does,
    /// and every touch after it says the class is unusable.
    @Test
    @DisplayName("a class whose initializer throws stays broken")
    void aFailedClassInitializerIsSticky() throws Throwable {
        InterpRuntime rt = load("InitFails",
                "public class InitFails {"
                + " static class Boom { static int V; static { V = 7;"
                + "   if (V > 0) { throw new IllegalStateException(\"boom\"); } } }"
                + " public static void main(String[] a) {"
                + "  for (int i = 0; i < 2; i++) {"
                + "   try { System.out.println(i + \":\" + Boom.V); }"
                + "   catch (Throwable t) { System.out.println(i + \":\" + t.getClass().getName()); }"
                + "  }"
                + "}}");
        rt.setEdtBudgetMs(0);

        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(captured, true, "UTF-8"));
            rt.runMain(new String[0]);
        } finally {
            System.setOut(originalOut);
        }
        String out = captured.toString("UTF-8").trim();
        assertTrue(out.startsWith("0:java.lang.ExceptionInInitializerError"),
                "the initializer's own failure should reach the caller, got: " + out);
        assertTrue(out.endsWith("1:java.lang.NoClassDefFoundError"),
                "a second touch must report the class as unusable, not hand back "
                        + "the fields the failed initializer assigned; got: " + out);
    }

    /// Two classes may name the same source file, and both sources must ship.
    ///
    /// `Util.java` in `a` and `Util.java` in `b` is ordinary. The bundle used to
    /// key sources by file name, so the second overwrote the first and the
    /// runtime refused the whole program with "bundle is missing the source file
    /// Util.java" -- for a file that was sitting in the tree it was handed.
    /// Since showing the source is the condition under which this app is allowed
    /// to run pushed code at all, losing one is not a cosmetic bug.
    @Test
    @DisplayName("same-named sources in different packages both survive the bundle")
    void sourcesAreKeyedByPackage() throws Exception {
        Path dir = Files.createTempDirectory("interp-source-keys");
        Path src = dir.resolve("src");
        Files.createDirectories(src.resolve("a"));
        Files.createDirectories(src.resolve("b"));
        Files.write(src.resolve("a/Util.java"),
                "package a;\npublic class Util { public static int v() { return 1; } }\n"
                        .getBytes(StandardCharsets.UTF_8));
        Files.write(src.resolve("b/Util.java"),
                "package b;\npublic class Util { public static int v() { return 2; } }\n"
                        .getBytes(StandardCharsets.UTF_8));
        Path classes = dir.resolve("classes");
        Files.createDirectories(classes);
        int rc = javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-g", "-nowarn", "-d", classes.toString(),
                src.resolve("a/Util.java").toString(), src.resolve("b/Util.java").toString());
        assertEquals(0, rc, "fixture did not compile");

        Class<?> writerClass = Class.forName("com.codename1.tools.translator.InterpBundleWriter");
        Object writer = writerClass.getDeclaredConstructor().newInstance();
        writerClass.getMethod("addClassFile", java.io.File.class)
                .invoke(writer, classes.resolve("a/Util.class").toFile());
        writerClass.getMethod("addClassFile", java.io.File.class)
                .invoke(writer, classes.resolve("b/Util.class").toFile());
        writerClass.getMethod("addSourceTree", java.io.File.class).invoke(writer, src.toFile());
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        writerClass.getMethod("write", java.io.OutputStream.class).invoke(writer, bos);

        // Reading is itself the assertion for the load-bearing half: the reader
        // refuses a bundle whose classes have no source.
        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(bos.toByteArray()));
        assertNotNull(bundle.getSource("a/Util.java"), "a's source was dropped");
        assertNotNull(bundle.getSource("b/Util.java"), "b's source was dropped");
        assertTrue(bundle.getSource("a/Util.java").contains("package a;"));
        assertTrue(bundle.getSource("b/Util.java").contains("package b;"));
    }

    /// A class literal for a pushed type has no host class object behind it, so
    /// the token on the stack is the interpreter's own. The bytecode does not
    /// know that and goes on calling `java.lang.Class` methods on it, which no
    /// linker can serve -- a reflective one rejects the receiver and a native
    /// one has no clazz pointer for a class the device was never built with.
    @Test
    @DisplayName("Class methods work on a class literal for a pushed type")
    void classLiteralsAnswerTheUsualQuestions() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("ClassOps",
                "public class ClassOps {"
                + " interface Marker {}"
                + " static class Thing implements Marker {}"
                + " public static void main(String[] a) {"
                + "  System.out.println(Thing.class.getName());"
                + "  System.out.println(Thing.class.getSimpleName());"
                + "  System.out.println(Marker.class.isInterface());"
                + "  System.out.println(Thing.class.isInstance(new Thing()));"
                + "  System.out.println(new Thing().getClass().getSimpleName());"
                + "  System.out.println(Thing.class.equals(Thing.class));"
                + "}}");
        assertEquals(r[0].output, r[1].output,
                "the interpreter should answer Class the way the JVM does");
    }

    /// `new Entry[1][]` names its component `[LEntry;`, not `Entry`, so a
    /// bundle-membership test that did not look through the brackets asked the
    /// host to load a class only the bundle has.
    @Test
    @DisplayName("arrays of a pushed type allocate in the interpreter at every rank")
    void arraysOfInterpretedTypesAllocate() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("ArrayRanks",
                "public class ArrayRanks {"
                + " static class Entry { int v; Entry(int v) { this.v = v; } }"
                + " public static void main(String[] a) {"
                + "  Entry[] one = new Entry[2];"
                + "  one[0] = new Entry(7);"
                + "  System.out.println(one.length + \":\" + one[0].v + \":\" + one[1]);"
                + "  Entry[][] partial = new Entry[2][];"
                + "  partial[0] = one;"
                + "  System.out.println(partial.length + \":\" + partial[0][0].v + \":\" + partial[1]);"
                + "  Entry[][] full = new Entry[2][3];"
                + "  full[1][2] = new Entry(9);"
                + "  System.out.println(full.length + \":\" + full[1].length + \":\" + full[1][2].v);"
                + "}}");
        assertEquals(r[0].output, r[1].output,
                "allocating an array of a pushed type should not reach the host loader");
    }

    /// JLS 12.4.1: initializing a class initializes the superinterfaces that
    /// declare a default method -- and only those. Initializing all of them
    /// would run initializers Java never runs, which is as wrong as running
    /// them late.
    @Test
    @DisplayName("an interface with a default method is initialized with its implementor")
    void defaultBearingInterfacesInitializeWithTheClass() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("IfaceInit",
                "public class IfaceInit {"
                + " static String log = \"\";"
                + " static String note(String s) { log = log + s; return s; }"
                + " interface WithDefault { String V = note(\"D\"); default int x() { return 1; } }"
                + " interface Plain { String V = note(\"P\"); }"
                + " static class C implements WithDefault, Plain {}"
                + " public static void main(String[] a) {"
                + "  new C();"
                + "  System.out.println(\"after new C: \" + log);"
                + "  System.out.println(Plain.V);"
                + "  System.out.println(\"after reading Plain.V: \" + log);"
                + "}}");
        assertEquals(r[0].output, r[1].output,
                "interface initialization order should match the JVM's");
    }

    /// `B.Z` where an interface declares Z compiles to a field reference owned
    /// by B, which declares no such field. Searching only the superclass chain
    /// answered with B and produced the field's *default* value -- a wrong
    /// number rather than an error, which is the worst way to be wrong.
    @Test
    @DisplayName("statics declared by an interface resolve through implementors")
    void interfaceStaticsResolveThroughImplementors() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("IfaceStatics",
                "public class IfaceStatics {"
                + " interface I { int Z = 7; String S = \"seven\"; }"
                + " static class B implements I {}"
                + " static class C extends B {}"
                + " public static void main(String[] a) {"
                + "  System.out.println(B.Z + \":\" + B.S + \":\" + C.Z);"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// `B.m()` where B inherits a static m from A records B as the owner. The
    /// vtable holds instance methods only, by design, so the lookup missed and
    /// fell through to the host -- which has never heard of B.
    @Test
    @DisplayName("a static method inherited from an interpreted class resolves")
    void inheritedStaticMethodsResolve() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("StaticInherit",
                "public class StaticInherit {"
                + " static class A { static int twice(int v) { return v * 2; } }"
                + " static class B extends A {}"
                + " public static void main(String[] a) {"
                + "  System.out.println(B.twice(21));"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// An interpreted object with no peer reaches host code as itself, and host
    /// code puts it in a HashMap. Identity equality there is not a missing
    /// nicety: keys the program considers equal hash differently and every
    /// lookup misses, quietly.
    @Test
    @DisplayName("equals and hashCode reach the interpreted overrides")
    void equalsAndHashCodeAreDelegated() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("EqualsKeys",
                "import java.util.HashMap;"
                + "public class EqualsKeys {"
                + " static class Key {"
                + "  final int v;"
                + "  Key(int v) { this.v = v; }"
                + "  public boolean equals(Object o) { return o instanceof Key && ((Key)o).v == v; }"
                + "  public int hashCode() { return v; }"
                + " }"
                + " public static void main(String[] a) {"
                + "  HashMap<Key,String> m = new HashMap<Key,String>();"
                + "  m.put(new Key(1), \"one\");"
                + "  System.out.println(m.get(new Key(1)));"
                + "  System.out.println(new Key(2).equals(new Key(2)));"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// `super.toString()` in an override used to dispatch straight back into
    /// that override: unbounded recursion reported as a stack overflow, in code
    /// that reads as ordinary Java.
    @Test
    @DisplayName("super.toString does not recurse into the override")
    void superToStringReachesObject() throws Throwable {
        InterpRuntime rt = load("SuperToString",
                "public class SuperToString {"
                + " static class T { public String toString() { return \"T:\" + super.toString().length(); } }"
                + " public static void main(String[] a) {"
                + "  System.out.println(new T().toString().startsWith(\"T:\"));"
                + "}}");
        rt.setEdtBudgetMs(0);
        java.io.PrintStream out = System.out;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        try {
            System.setOut(new java.io.PrintStream(captured, true, "UTF-8"));
            rt.runMain(new String[0]);
        } finally {
            System.setOut(out);
        }
        assertEquals("true", captured.toString("UTF-8").trim());
    }

    /// wait/notify on an object of a pushed-only class: the wrapper is a real
    /// Java object with a real monitor, and a synchronized block on a peerless
    /// object locks that same wrapper. Without this the runtime reported the
    /// methods as not implemented, so ordinary producer/consumer code failed.
    @Test
    @DisplayName("wait and notify work on a pushed-only class")
    void waitAndNotifyWorkOnPushedObjects() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("WaitNotify",
                "public class WaitNotify {"
                + " static class Lock {}"
                + " static boolean ready;"
                + " public static void main(String[] a) throws Exception {"
                + "  final Lock lock = new Lock();"
                + "  Thread t = new Thread(new Runnable() { public void run() {"
                + "     synchronized (lock) { ready = true; lock.notifyAll(); } } });"
                + "  synchronized (lock) {"
                + "   t.start();"
                + "   while (!ready) { lock.wait(2000); }"
                + "  }"
                + "  System.out.println(\"woken:\" + ready);"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// Object is recorded as an extern and no interpreted class lists it as an
    /// interpreted supertype, so the hierarchy walk answered false for
    /// `x instanceof Object` -- true of every non-null reference there has been.
    @Test
    @DisplayName("a pushed object is an instance of Object")
    void everythingIsAnObject() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("ObjectInstance",
                "public class ObjectInstance {"
                + " static class Thing {}"
                + " public static void main(String[] a) {"
                + "  Object o = new Thing();"
                + "  System.out.println((o instanceof Object) + \":\" + (((Object)new Thing()) != null));"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// JLS 12.4.2 wraps a non-Error initializer failure, which is how Java code
    /// catches it. An Error passes through unwrapped, as the spec says.
    @Test
    @DisplayName("a non-Error initializer failure arrives as ExceptionInInitializerError")
    void initializerFailuresAreWrapped() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("InitWrap",
                "public class InitWrap {"
                + " static class Boom { static int V; static { V = 1; if (V > 0) {"
                + "   throw new IllegalStateException(\"boom\"); } } }"
                + " public static void main(String[] a) {"
                + "  try { System.out.println(Boom.V); }"
                + "  catch (ExceptionInInitializerError e) {"
                + "   System.out.println(\"wrapped:\" + e.getException().getClass().getName()); }"
                + "}}");
        assertEquals(r[0].output, r[1].output);
    }

    /// A deviation from the JVM, asserted so it stays a known one.
    ///
    /// An array of a pushed-only type is an `Object[]` -- there is no host class
    /// to allocate one of -- so the instance carries no component type and a
    /// store into it cannot be checked. Java throws ArrayStoreException here.
    /// Closing it means a wrapper object around every interpreted array,
    /// threaded through every array opcode and every crossing into host code:
    /// see the developer guide for why that is written down rather than done.
    @Test
    @DisplayName("an aliased pushed-type array accepts a foreign element -- a known deviation")
    void arrayStoreIsUncheckedForPushedTypes() throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth("ArrayStoreDeviation",
                "public class ArrayStoreDeviation {"
                + " static class A {}"
                + " static class B {}"
                + " public static void main(String[] x) {"
                + "  Object[] alias = new A[1];"
                + "  try { alias[0] = new B(); System.out.println(\"stored\"); }"
                + "  catch (ArrayStoreException e) { System.out.println(\"refused\"); }"
                + "}}");
        assertEquals("refused", r[0].output.trim(), "the JVM checks the component type");
        assertEquals("stored", r[1].output.trim(),
                "the interpreter has no component type to check");
    }

    /// Runs on the thread the wall-clock budget applies to.
    ///
    /// The budget is the event thread's rule -- a worker computing for ten
    /// seconds blocks nothing -- so a test of it has to be on the event thread
    /// whenever there is one. Surefire reuses the JVM, so whether Display has
    /// been initialized depends on which tests ran first, and asserting from
    /// whatever thread JUnit happens to use would pass or fail on that.
    private static Throwable runWhereTheBudgetApplies(final Runnable body) {
        final Throwable[] thrown = new Throwable[1];
        Runnable capture = new Runnable() {
            public void run() {
                try {
                    body.run();
                } catch (Throwable t) {
                    thrown[0] = t;
                }
            }
        };
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.Display.getInstance().callSeriallyAndWait(capture);
        } else {
            capture.run();
        }
        return thrown[0];
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof InterpThrowable) {
            Object thrown = ((InterpThrowable) t).getThrown();
            if (thrown instanceof Throwable) {
                return (Throwable) thrown;
            }
        }
        return t;
    }
}
