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
package com.codename1.testing.junit;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JUnit 5 extension that boots the Codename One simulator on demand and
 * routes annotated work onto the EDT.
 *
 * <p>Register it via {@link CodenameOneTest @CodenameOneTest} or
 * {@code @ExtendWith(CodenameOneExtension.class)}. The extension is
 * intentionally lightweight:
 * <ul>
 *   <li>Display is initialized lazily on the first test class that uses
 *       the extension and stays up for the rest of the JVM &mdash; tearing
 *       Display down and back up between tests is slow and rarely needed.</li>
 *   <li>{@link SimulatorProperty}/{@link SimulatorProperties} on the class
 *       and on the test method are applied in the right order: SYSTEM
 *       before Display init, DISPLAY after.</li>
 *   <li>{@link RunOnEdt} dispatches the test body via
 *       {@link CN#callSerially(Runnable)} (with a latch) and rethrows any
 *       throwable on the calling thread so JUnit sees the real failure.</li>
 *   <li>{@link Theme}, {@link DarkMode}, {@link LargerText},
 *       {@link Orientation} and {@link RTL} reconfigure the visual
 *       environment per test on the EDT, then trigger a single theme
 *       refresh.</li>
 * </ul>
 *
 * <p>The extension does not reach into Display's private state to reset
 * pending serial calls or pointer flags between tests &mdash; that pattern
 * exists inside the framework's own unit tests but is too invasive for the
 * public API. If a test class needs cross-test cleanup, do it explicitly
 * in {@code @AfterEach}.
 */
public class CodenameOneExtension
        implements BeforeAllCallback, BeforeEachCallback, InvocationInterceptor {

    private static final Object DISPLAY_BOOT_LOCK = new Object();

    @Override
    public void beforeAll(ExtensionContext context) {
        // The simulator's Display.init eventually constructs a JFrame to host
        // the canvas, so a true-headless JVM (no DISPLAY, no Xvfb) will throw
        // HeadlessException the moment we touch it. Abort the whole class
        // instead so the headless CI run reports it as a skip rather than
        // a failure -- and crucially, before we leave Display half-init'd
        // and risk poisoning later tests in the same JVM.
        if (GraphicsEnvironment.isHeadless()) {
            throw new TestAbortedException(
                    "Codename One simulator tests require a graphical display; "
                            + "skipping " + context.getRequiredTestClass().getName()
                            + " because GraphicsEnvironment.isHeadless() is true. "
                            + "Run with Xvfb (or remove java.awt.headless=true) to enable.");
        }
        Class<?> testClass = context.getRequiredTestClass();
        applyProperties(testClass.getAnnotation(SimulatorProperty.class),
                testClass.getAnnotation(SimulatorProperties.class),
                /*displayReady*/ false);
        ensureDisplayInitialized();
        applyProperties(testClass.getAnnotation(SimulatorProperty.class),
                testClass.getAnnotation(SimulatorProperties.class),
                /*displayReady*/ true);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Method method = context.getRequiredTestMethod();
        Class<?> testClass = context.getRequiredTestClass();

        applyProperties(method.getAnnotation(SimulatorProperty.class),
                method.getAnnotation(SimulatorProperties.class),
                /*displayReady*/ true);

        final ResolvedVisualConfig config = ResolvedVisualConfig.resolve(testClass, method);
        if (config.hasAny()) {
            try {
                applyVisualConfigOnEdt(config);
            } catch (Exception e) {
                throw e;
            } catch (Throwable t) {
                // Errors thrown from the EDT-dispatched apply step bubble up as
                // Throwable; rewrap so JUnit's beforeEach contract (throws
                // Exception only) is satisfied without losing the cause.
                throw new RuntimeException(t);
            }
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> ctx,
                                    ExtensionContext extensionContext) throws Throwable {
        Method m = ctx.getExecutable();
        RunOnEdt onEdt = resolveRunOnEdt(m, extensionContext.getRequiredTestClass());
        if (onEdt != null) {
            dispatchOnEdt(invocation, onEdt.timeoutMillis(), describe(m));
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> ctx,
                                          ExtensionContext extensionContext) throws Throwable {
        // Only dispatch lifecycle methods onto the EDT when the *class* asks
        // for it. Method-level @RunOnEdt is scoped to that single @Test.
        RunOnEdt classLevel =
                extensionContext.getRequiredTestClass().getAnnotation(RunOnEdt.class);
        if (classLevel != null) {
            dispatchOnEdt(invocation, classLevel.timeoutMillis(), describe(ctx.getExecutable()));
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> ctx,
                                         ExtensionContext extensionContext) throws Throwable {
        RunOnEdt classLevel =
                extensionContext.getRequiredTestClass().getAnnotation(RunOnEdt.class);
        if (classLevel != null) {
            dispatchOnEdt(invocation, classLevel.timeoutMillis(), describe(ctx.getExecutable()));
        } else {
            invocation.proceed();
        }
    }

    private static RunOnEdt resolveRunOnEdt(Method method, Class<?> testClass) {
        RunOnEdt methodLevel = method.getAnnotation(RunOnEdt.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return testClass.getAnnotation(RunOnEdt.class);
    }

    private static void ensureDisplayInitialized() {
        if (Display.isInitialized()) {
            return;
        }
        synchronized (DISPLAY_BOOT_LOCK) {
            if (!Display.isInitialized()) {
                Display.init(null);
            }
        }
    }

    private static void applyProperties(SimulatorProperty single,
                                        SimulatorProperties multi,
                                        boolean displayReady) {
        if (single != null) {
            applyProperty(single, displayReady);
        }
        if (multi != null) {
            SimulatorProperty[] entries = multi.value();
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    applyProperty(entries[i], displayReady);
                }
            }
        }
    }

    private static void applyProperty(SimulatorProperty prop, boolean displayReady) {
        switch (prop.scope()) {
            case SYSTEM:
                if (!displayReady) {
                    System.setProperty(prop.name(), prop.value());
                }
                break;
            case DISPLAY:
                if (displayReady) {
                    Display.getInstance().setProperty(prop.name(), prop.value());
                }
                break;
            default:
                // Unknown scope - ignore so unknown future values don't break old tests.
        }
    }

    /**
     * Applies the resolved theming / accessibility / orientation / RTL /
     * dark-mode configuration on the Codename One EDT, then triggers one
     * theme refresh so the live form picks up every change in a single pass.
     * Mirrors the body of {@code JavaSEPort.applyThemeOnlyRefresh} via the
     * publicly visible {@link UIManager}/{@link Form} APIs.
     */
    private static void applyVisualConfigOnEdt(final ResolvedVisualConfig cfg) throws Throwable {
        final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>();
        final Object lock = new Object();
        final boolean[] done = new boolean[1];

        Runnable apply = new Runnable() {
            @Override
            public void run() {
                try {
                    if (cfg.theme != null) {
                        installTheme(cfg.theme);
                    }
                    if (cfg.darkMode != null) {
                        Display.getInstance().setDarkMode(cfg.darkMode);
                    }
                    if (cfg.largerTextScale != null) {
                        JavaSEPort port = JavaSEPort.instance;
                        if (port != null) {
                            port.setSimulatorLargerTextScale(cfg.largerTextScale.floatValue());
                        }
                    }
                    if (cfg.orientationPortrait != null) {
                        JavaSEPort port = JavaSEPort.instance;
                        if (port != null) {
                            port.setSimulatorPortrait(cfg.orientationPortrait.booleanValue());
                        }
                    }
                    if (cfg.rtl != null) {
                        UIManager.getInstance().getLookAndFeel().setRTL(cfg.rtl.booleanValue());
                    }
                    refreshThemeInline();
                } catch (Throwable t) {
                    thrown.set(t);
                } finally {
                    synchronized (lock) {
                        done[0] = true;
                        lock.notifyAll();
                    }
                }
            }
        };

        if (CN.isEdt()) {
            apply.run();
        } else {
            CN.callSerially(apply);
            synchronized (lock) {
                while (!done[0]) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }

        Throwable t = thrown.get();
        if (t != null) {
            throw t;
        }
    }

    private static void installTheme(String resourcePath) throws java.io.IOException {
        Resources r = Resources.open(resourcePath);
        String[] names = r.getThemeResourceNames();
        if (names == null || names.length == 0) {
            throw new IllegalStateException(
                    "Theme resource " + resourcePath + " contains no themes");
        }
        Hashtable themeProps = r.getTheme(names[0]);
        UIManager.getInstance().setThemeProps(themeProps);
    }

    /**
     * Runs the theme-refresh sequence inline on the current thread. Caller
     * must already be on the Codename One EDT. Public-API equivalent of
     * {@code JavaSEPort.applyThemeOnlyRefresh}.
     */
    private static void refreshThemeInline() {
        UIManager.getInstance().refreshTheme();
        Form curr = Display.getInstance().getCurrent();
        if (curr != null) {
            curr.refreshTheme(true);
            curr.revalidate();
            curr.repaint();
        }
    }

    /**
     * Runs {@code invocation.proceed()} on the Codename One EDT and rethrows
     * any thrown exception on the calling thread once the EDT has finished
     * (or the timeout expires). The single use of {@link CN#callSerially}
     * with a busy-wait latch &mdash; rather than {@link CN#callSeriallyAndWait(Runnable, int)}
     * &mdash; is so we can propagate the original throwable instance instead
     * of having it logged-and-swallowed inside the EDT helper.
     */
    private static void dispatchOnEdt(Invocation<Void> invocation,
                                      long timeoutMillis,
                                      String description) throws Throwable {
        if (CN.isEdt()) {
            invocation.proceed();
            return;
        }
        final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>();
        final Object lock = new Object();
        final boolean[] completed = new boolean[1];
        final Invocation<Void> capturedInvocation = invocation;

        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    capturedInvocation.proceed();
                } catch (Throwable t) {
                    thrown.set(t);
                }
                synchronized (lock) {
                    completed[0] = true;
                    lock.notifyAll();
                }
            }
        });

        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (lock) {
            while (!completed[0]) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0L) {
                    break;
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
            if (!completed[0]) {
                throw new AssertionError(
                        description + " did not complete within "
                                + timeoutMillis + "ms on the Codename One EDT");
            }
        }
        Throwable t = thrown.get();
        if (t != null) {
            throw t;
        }
    }

    private static String describe(Method m) {
        return m.getDeclaringClass().getSimpleName() + "#" + m.getName();
    }

    /**
     * Snapshot of the visual-environment annotations resolved for a single
     * test method. Method-level annotations take precedence over the
     * class-level ones; absent annotations stay null so {@link
     * #applyVisualConfigOnEdt} can skip them entirely (the extension never
     * "resets" state that the caller did not ask for).
     */
    private static final class ResolvedVisualConfig {
        final String theme;
        final Boolean darkMode;
        final Float largerTextScale;
        final Boolean orientationPortrait;
        final Boolean rtl;

        ResolvedVisualConfig(String theme, Boolean darkMode, Float largerTextScale,
                             Boolean orientationPortrait, Boolean rtl) {
            this.theme = theme;
            this.darkMode = darkMode;
            this.largerTextScale = largerTextScale;
            this.orientationPortrait = orientationPortrait;
            this.rtl = rtl;
        }

        boolean hasAny() {
            return theme != null || darkMode != null || largerTextScale != null
                    || orientationPortrait != null || rtl != null;
        }

        static ResolvedVisualConfig resolve(Class<?> testClass, Method method) {
            Theme theme = pickTheme(testClass, method);
            DarkMode dark = pickDarkMode(testClass, method);
            LargerText lt = pickLargerText(testClass, method);
            Orientation o = pickOrientation(testClass, method);
            RTL rtl = pickRtl(testClass, method);
            return new ResolvedVisualConfig(
                    resolveThemePath(theme),
                    dark == null ? null : Boolean.valueOf(dark.enabled()),
                    lt == null ? null : Float.valueOf(lt.scale()),
                    o == null ? null : Boolean.valueOf(o.value() == Orientation.Value.PORTRAIT),
                    rtl == null ? null : Boolean.valueOf(rtl.enabled()));
        }

        /**
         * Reduces a {@link Theme} annotation to the .res path the extension
         * should install. The {@link Theme#nativeTheme()} enum wins over the
         * {@link Theme#value()} path because it is the more specific signal;
         * a non-empty path is used only when the enum is left at its
         * {@code NONE} default. Returns null when the annotation is absent
         * or carries neither input, so {@link ResolvedVisualConfig#hasAny()}
         * treats it as "nothing to apply" and the extension leaves the
         * current theme alone.
         */
        private static String resolveThemePath(Theme theme) {
            if (theme == null) {
                return null;
            }
            NativeTheme nt = theme.nativeTheme();
            if (nt != null && nt != NativeTheme.NONE) {
                return nt.resourcePath();
            }
            String value = theme.value();
            return (value == null || value.isEmpty()) ? null : value;
        }

        private static Theme pickTheme(Class<?> c, Method m) {
            Theme t = m.getAnnotation(Theme.class);
            return t != null ? t : c.getAnnotation(Theme.class);
        }

        private static DarkMode pickDarkMode(Class<?> c, Method m) {
            DarkMode d = m.getAnnotation(DarkMode.class);
            return d != null ? d : c.getAnnotation(DarkMode.class);
        }

        private static LargerText pickLargerText(Class<?> c, Method m) {
            LargerText l = m.getAnnotation(LargerText.class);
            return l != null ? l : c.getAnnotation(LargerText.class);
        }

        private static Orientation pickOrientation(Class<?> c, Method m) {
            Orientation o = m.getAnnotation(Orientation.class);
            return o != null ? o : c.getAnnotation(Orientation.class);
        }

        private static RTL pickRtl(Class<?> c, Method m) {
            RTL r = m.getAnnotation(RTL.class);
            return r != null ? r : c.getAnnotation(RTL.class);
        }
    }
}
