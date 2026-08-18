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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interpreted class extending a host class, exercised the way the framework
 * actually drives one.
 *
 * <p>These pin the two defects that only appeared on a device, both of which
 * the single-threaded conformance suite could not have caught.</p>
 *
 * <p>The first is {@code super.} dispatch. A generated shim overrides the
 * framework method and asks the interpreter for it; when the interpreted
 * override then calls {@code super.paint(g)}, routing that back to the peer's
 * {@code paint} lands on the override again. On a device that is unbounded
 * recursion once per frame on the event thread, which reads as a frozen app
 * rather than as a stack overflow.</p>
 *
 * <p>The second is concurrency. The runtime is entered from the thread running
 * a pushed {@code main} and from the event thread calling an interpreted
 * {@code paint}, at the same time. Depth, fuel and the call stack have to
 * belong to the thread, not to the runtime.</p>
 *
 * @author Shai Almog
 */
public class InterpHostSubclassTest {

    /** Stands in for a framework class: non-final, with an overridable method. */
    public static class HostBase {
        private final String tag;

        public HostBase() {
            this("default");
        }

        public HostBase(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }

        public String render() {
            return "host:" + tag;
        }

        /** Framework code calling an overridable method on itself. */
        public String describe() {
            return "[" + render() + "]";
        }

        /**
         * Final, so a generated shim can neither override it nor bridge it --
         * the generator skips final methods, since neither is possible. A
         * pushed subclass may still write {@code super.stamp()}, and that is
         * ordinary Java rather than a mistake.
         */
        public final String stamp() {
            return "stamp:" + tag;
        }
    }

    /** Stands in for a generated shim, written exactly as the generator emits one. */
    public static final class HostBaseShim extends HostBase implements InterpBacked {
        private final InterpObject $interp;
        private final InterpRuntime $runtime;

        HostBaseShim(InterpRuntime rt, InterpObject o) {
            this.$runtime = rt;
            this.$interp = o;
        }

        HostBaseShim(InterpRuntime rt, InterpObject o, String tag) {
            super(tag);
            this.$runtime = rt;
            this.$interp = o;
        }

        public InterpObject getInterpObject() {
            return $interp;
        }

        @Override
        public String render() {
            Object r = $runtime == null ? InterpRuntime.NOT_OVERRIDDEN
                    : $runtime.dispatch($interp, "render", "()Ljava/lang/String;", new Object[]{});
            if (r == InterpRuntime.NOT_OVERRIDDEN) {
                return super.render();
            }
            return (String) r;
        }

        public String super_render() {
            return super.render();
        }
    }

    /** A factory over the shim above, mirroring the device's. */
    private static final class TestFactory implements InterpObjectFactory {
        private InterpRuntime runtime;

        void attach(InterpRuntime rt) {
            this.runtime = rt;
        }

        private static final String HOST_BASE =
                HostBase.class.getName().replace('.', '/');

        public String peerClassName(Object peer) {
        // The JVM reports this faithfully; only ParparVM does not.
        return peer == null ? null : peer.getClass().getName().replace('.', '/');
    }

    public boolean canExtend(String hostSuperclassName) {
            return hostSuperclassName == null || "java/lang/Object".equals(hostSuperclassName)
                    || HOST_BASE.equals(hostSuperclassName);
        }

        public Object createPeer(InterpObject object, String hostSuperclassName,
                                 String[] hostInterfaceNames, String descriptor, Object[] args) {
            if (!HOST_BASE.equals(hostSuperclassName)) {
                return null;
            }
            if ("(Ljava/lang/String;)V".equals(descriptor)) {
                return new HostBaseShim(runtime, object, (String) args[0]);
            }
            return new HostBaseShim(runtime, object);
        }
    }

    private static InterpRuntime load(String className, String source) throws Exception {
        Path dir = Files.createTempDirectory("interp-subclass");
        Files.write(dir.resolve(className + ".java"), source.getBytes(StandardCharsets.UTF_8));
        // The fixture extends a class declared in this test, so it has to
        // compile against these test classes. Surefire often hands
        // java.class.path a manifest-only jar, so take the location of this
        // class instead -- that is the directory the fixture needs.
        String cp = InterpHostSubclassTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()
                + java.io.File.pathSeparator + System.getProperty("java.class.path");
        java.io.ByteArrayOutputStream diagnostics = new java.io.ByteArrayOutputStream();
        int rc = javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, diagnostics,
                "-g", "-nowarn", "-XDstringConcat=inline",
                "-cp", cp,
                "-d", dir.toString(), dir.resolve(className + ".java").toString());
        if (rc != 0) {
            throw new IllegalStateException("fixture did not compile:\n"
                    + diagnostics.toString("UTF-8"));
        }
        byte[] bundleBytes = InterpTestHarness.buildBundle(dir, className, source);
        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(bundleBytes));
        ReflectionInterpLinker linker = new ReflectionInterpLinker();
        TestFactory factory = new TestFactory();
        InterpRuntime rt = new InterpRuntime(bundle, linker, factory);
        factory.attach(rt);
        rt.setEdtBudgetMs(0);
        return rt;
    }

    private static final String FINAL_SUPER_SOURCE =
            "public class SubFinal extends com.codename1.impl.interp.InterpHostSubclassTest.HostBase {\n"
          + "  public SubFinal() { super(\"pushed\"); }\n"
          + "  private String superStamp() { return super.stamp(); }\n"
          + "  public static String viaSuper() { return new SubFinal().superStamp(); }\n"
          + "  public static String viaPlain() { return new SubFinal().stamp(); }\n"
          + "  public static void main(String[] a) {}\n"
          + "}\n";

    /**
     * {@code super.} on a *final* host method has no bridge to call, because
     * the shim generator skips what it cannot override. Insisting on
     * {@code super_stamp} anyway reported a missing method for a program that
     * is perfectly legal Java; with nothing overriding the method, calling it
     * directly is exactly what the super call means.
     */
    @Test
    @DisplayName("super. on a final host method calls it directly")
    void superCallOnAFinalMethodNeedsNoBridge() throws Throwable {
        InterpRuntime rt = load("SubFinal", FINAL_SUPER_SOURCE);
        InterpClass c = rt.getBundle().findClass("SubFinal");
        assertEquals("stamp:pushed",
                rt.invoke(c.declaredMethod("viaSuper", "()Ljava/lang/String;"),
                        null, new Object[0]));
        // The same value without `super.`, so the assertion above is about the
        // route rather than about what stamp() returns.
        assertEquals("stamp:pushed",
                rt.invoke(c.declaredMethod("viaPlain", "()Ljava/lang/String;"),
                        null, new Object[0]));
    }

    private static final String SUBCLASS_SOURCE =
            "public class Sub extends com.codename1.impl.interp.InterpHostSubclassTest.HostBase {\n"
          + "  public Sub() { super(\"pushed\"); }\n"
          + "  @Override public String render() { return \"interp+\" + super.render(); }\n"
          + "  public static Object make() { return new Sub(); }\n"
          + "  public static void main(String[] a) {}\n"
          + "}\n";

    /**
     * {@code super.render()} must reach the framework implementation. Routing it
     * back through the peer's override would recurse until the stack or the
     * depth cap gave out.
     */
    @Test
    @DisplayName("super. from an interpreted override reaches the host implementation")
    void superCallReachesTheHostImplementation() throws Throwable {
        InterpRuntime rt = load("Sub", SUBCLASS_SOURCE);
        Object peer = rt.invoke(rt.getBundle().findClass("Sub")
                .declaredMethod("make", "()Ljava/lang/Object;"), null, new Object[0]);

        assertTrue(peer instanceof HostBase, "the peer should be a HostBase");
        // "interp+" proves the override ran; "host:pushed" proves super. reached
        // the framework body rather than looping back into the override.
        assertEquals("interp+host:pushed", ((HostBase) peer).render());
    }

    /**
     * The superclass constructor the interpreted class chained to has to be the
     * one the peer runs, or its arguments are silently discarded.
     */
    @Test
    @DisplayName("the superclass constructor arguments reach the peer")
    void superConstructorArgumentsAreNotLost() throws Throwable {
        InterpRuntime rt = load("Sub", SUBCLASS_SOURCE);
        Object peer = rt.invoke(rt.getBundle().findClass("Sub")
                .declaredMethod("make", "()Ljava/lang/Object;"), null, new Object[0]);
        assertEquals("pushed", ((HostBase) peer).tag());
    }

    /**
     * Framework code calling an overridable method on itself must reach the
     * interpreted override -- this is what {@code Form.show()} calling
     * {@code paint()} depends on.
     */
    @Test
    @DisplayName("a host self-call reaches the interpreted override")
    void hostSelfCallReachesTheOverride() throws Throwable {
        InterpRuntime rt = load("Sub", SUBCLASS_SOURCE);
        Object peer = rt.invoke(rt.getBundle().findClass("Sub")
                .declaredMethod("make", "()Ljava/lang/Object;"), null, new Object[0]);
        assertEquals("[interp+host:pushed]", ((HostBase) peer).describe());
    }

    /**
     * The event thread calling an interpreted override while another thread is
     * running interpreted code is the normal case on a device, not an edge one:
     * every repaint does it. Shared depth/fuel/call-stack state made the two
     * corrupt each other.
     */
    @Test
    @DisplayName("concurrent entry from two threads stays correct")
    void concurrentEntryIsThreadSafe() throws Throwable {
        final InterpRuntime rt = load("Sub", SUBCLASS_SOURCE);
        final InterpMethod make = rt.getBundle().findClass("Sub")
                .declaredMethod("make", "()Ljava/lang/Object;");
        final HostBase peer = (HostBase) rt.invoke(make, null, new Object[0]);

        final int threads = 4;
        final int iterations = 200;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        for (int t = 0; t < threads; t++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            String s = peer.render();
                            if (!"interp+host:pushed".equals(s)) {
                                throw new AssertionError("got " + s);
                            }
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                }
            }, "interp-concurrency-" + t).start();
        }
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "threads did not finish");
        assertNull(failure.get(), "concurrent dispatch failed: " + failure.get());
    }

    /**
     * A depth cap tripped on one thread must not affect another. With shared
     * state a deep call on the pushed-program thread would make the event
     * thread's next repaint fail for no reason.
     */
    @Test
    @DisplayName("the depth cap is per thread")
    void theDepthCapIsPerThread() throws Throwable {
        final InterpRuntime rt = load("Deep",
                "public class Deep { static int f(int n) { return f(n + 1); }\n"
                + "  static int shallow() { return 7; }\n"
                + "  public static void main(String[] a) {} }");
        rt.setEdtBudgetMs(0);
        rt.setMaxDepth(32);

        final InterpMethod deep = rt.getBundle().findClass("Deep").declaredMethod("f", "(I)I");
        final InterpMethod shallow = rt.getBundle().findClass("Deep")
                .declaredMethod("shallow", "()I");

        // Exhaust the cap on this thread.
        try {
            rt.invoke(deep, null, new Object[]{Integer.valueOf(0)});
            throw new AssertionError("expected the depth cap to fire");
        } catch (InterpThrowable expected) {
            assertTrue(expected.getThrown() instanceof StackOverflowError);
        }

        final AtomicReference<Object> result = new AtomicReference<Object>();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread other = new Thread(new Runnable() {
            public void run() {
                try {
                    result.set(rt.invoke(shallow, null, new Object[0]));
                } catch (Throwable t) {
                    failure.set(t);
                }
            }
        });
        other.start();
        other.join(30000);

        assertNull(failure.get(), "the other thread should be unaffected: " + failure.get());
        assertEquals(Integer.valueOf(7), result.get());
    }

    /** After the cap fires, the same thread must be usable again. */
    @Test
    @DisplayName("depth unwinds cleanly so the thread stays usable")
    void depthUnwindsCleanly() throws Throwable {
        InterpRuntime rt = load("Deep2",
                "public class Deep2 { static int f(int n) { return f(n + 1); }\n"
                + "  static int shallow() { return 5; }\n"
                + "  public static void main(String[] a) {} }");
        rt.setEdtBudgetMs(0);
        rt.setMaxDepth(32);
        InterpClass c = rt.getBundle().findClass("Deep2");

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                rt.invoke(c.declaredMethod("f", "(I)I"), null, new Object[]{Integer.valueOf(0)});
                throw new AssertionError("expected the depth cap to fire");
            } catch (InterpThrowable expected) {
                assertNotNull(expected.getThrown());
            }
            assertEquals(Integer.valueOf(5),
                    rt.invoke(c.declaredMethod("shallow", "()I"), null, new Object[0]));
        }
    }
}
