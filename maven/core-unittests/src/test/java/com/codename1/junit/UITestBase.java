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
package com.codename1.junit;

import com.codename1.impl.ImplementationFactory;
import com.codename1.io.Util;
import com.codename1.testing.SafeL10NManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

/**
 * Provides a minimal initialized {@link Display} environment for unit tests that instantiate UI components.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UITestBase {
    protected Display display;
    protected TestCodenameOneImplementation implementation;

    protected void waitFor(CountDownLatch latch, int timeout) {
        waitFor(latch, 0, timeout);
    }

    protected void waitFor(CountDownLatch latch, int count, int timeout) {
        while(latch.getCount() > count) {
            assertTrue(timeout > 0);
            TestUtils.waitFor(5);
            timeout -= 5;
        }
    }

    @BeforeAll
    protected void setUpDisplay() throws Exception {
        DisplayTest.initInvokeAndBlockThreads();
        ensureDisplayInitialized();
    }

    /// Brings the Display up if it is down, and adopts it if it is already up.
    ///
    /// Called from `@BeforeAll` AND from `@BeforeEach`, which is the part that
    /// matters. The teardown that clears the flag belongs to the PREVIOUS class's
    /// EDT and runs whenever that thread gets round to it -- which can be after
    /// this class's `@BeforeAll` has looked and found a live Display. Recovering
    /// only there fixed the ordering where the teardown lands first and left the
    /// whole class dead when it lands second: every test in it reports
    /// "FormTest timed out after 5000ms; edt=display-not-initialized", which is
    /// what CI produced for ValidatorTest and a local run never does, because
    /// the window is only open while a loaded machine is descheduling the old
    /// thread.
    ///
    /// Waiting for that thread to finish instead was tried and reverted: it
    /// timed out for over a hundred classes and made the suite far slower.
    private void ensureDisplayInitialized() throws Exception {
        if (!Display.isInitialized()) {
            // Display.init() only does anything when codenameOneRunning is false,
            // and isInitialized() is that flag AND impl.isInitialized(). The two
            // come apart: the previous class's EDT calls impl.deinitialize() on
            // its way out, on whatever implementation is current BY THEN -- so if
            // it is still leaving while this class starts, it clears the flag on
            // the implementation this class is about to use. init() then returns
            // without doing anything, because codenameOneRunning is still true,
            // and every test in the class times out reporting
            // "display-not-initialized".
            //
            // Clearing the flag first makes the init below real in that case, and
            // costs nothing in the normal one where it is already false. Cheaper
            // and more reliable than waiting for the old thread: it does not need
            // the EDT to have noticed anything yet.
            Display.deinitialize();
            implementation = TestCodenameOneImplementation.getInstance();
            if (implementation == null) {
                implementation = new TestCodenameOneImplementation();
            }
            final TestCodenameOneImplementation implRef = implementation;
            ImplementationFactory.setInstance(new ImplementationFactory() {
                @Override
                public Object createImplementation() {
                    return implRef;
                }
            });
            // Setup SafeL10NManager before init if possible, or immediately after
            // But L10NManager is fetched from implementation.
            implementation.setLocalizationManager(new SafeL10NManager("en", "US"));

            Display.init(null);
        } else {
            implementation = TestCodenameOneImplementation.getInstance();
            implementation.setLocalizationManager(new SafeL10NManager("en", "US"));
        }
        Util.setImplementation(implementation);
        display = Display.getInstance();
    }

    @BeforeEach
    protected void setUpImplementation() {
        // No Display recovery here, deliberately. EDTTestInterceptor dispatches
        // @BeforeEach onto the dispatch thread, so when that thread is the thing
        // that has gone, this method never runs at all -- a recovery placed here
        // is queued behind the failure it exists to repair. It lives in
        // FormTestInterceptor.beforePretest(), which runs on the test thread
        // before any dispatch.
        implementation = TestCodenameOneImplementation.getInstance();
        implementation.setLocalizationManager(new SafeL10NManager("en", "US"));
    }

    @AfterEach
    protected void tearDownDisplay() throws Exception {
        DisplayTest.flushEdt();
        disposeLeftoverWindows();
        resetUIManager();
        com.codename1.ui.Toolbar.setGlobalToolbar(false);
        if (implementation != null) {
            implementation.reset();
        }

        // Clear pending serial calls on the Display to avoid pollution
        try {
            Field pendingField = Display.class.getDeclaredField("pendingSerialCalls");
            pendingField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Runnable> pending = (List<Runnable>) pendingField.get(display);
            if (pending != null) {
                pending.clear();
            }

            // runningSerialCallsQueue is NOT cleared. This teardown is itself
            // dispatched onto the EDT by EDTTestInterceptor, so it runs while that
            // queue is being drained -- and emptying it discards any sibling
            // @AfterEach dispatch still waiting in it. The waiter for that dispatch
            // then burns its full timeout and reports "FormTest timed out after
            // 5000ms" from a test that was never slow, which is how FileTreeTest and
            // SpanLabelWidthCapTest failed intermittently on CI and never locally.
            // flushEdt() above has already drained the queue; there is nothing left
            // to clear that is not someone else's work in flight.
        } catch (Exception ignored) {
        }

        // Reset pointer/drag state on the Display so a prior test that simulated a drag
        // doesn't silently swallow action events in the next test (List.fireActionEvent
        // short-circuits when Display.hasDragOccured() is true).
        resetDisplayBooleanField("dragOccured", false);
        resetDisplayBooleanField("pointerPressedAndNotReleasedOrDragged", false);
        // The per-window half of the same state. Leaving it set would carry a held
        // press from a window one test opened into the next test's assertions, which
        // the singleton reset above cannot reach.
        resetDisplayBooleanArrayField("selectionPressed");
        resetDisplayIntField("dragPathLength", 0);
    }

    /// Takes down any native window the test left open.
    ///
    /// `Desktop` is a process-wide singleton (`Desktop.INSTANCE`) holding one
    /// mutable `windows` list, and nothing else here clears it. A test that opens
    /// a window and does not dispose it -- because it asserted its way out early,
    /// or because the dispose it asked for was marshalled onto the dispatch thread
    /// and had not run yet -- therefore hands its window to every test that follows,
    /// in this class and in every class after it in the same JVM. The next test to
    /// count windows counts one too many, which is how WindowTest failed on master
    /// at 66b82a0f with `anOwnedWindowIsDisposedWithItsOwner` expecting 2 and
    /// getting 3, while the commits either side of it passed on identical code.
    ///
    /// A leaked window is worse than a wrong count when it is modal: it holds the
    /// release the dispatch thread is waiting for, so the serial-call queue stops
    /// draining and the next class reports every test as
    /// "FormTest timed out after 5000ms" with a `pendingSerialCalls` count that
    /// climbs test by test -- a live dispatch thread with nothing getting through.
    ///
    /// Runs before `implementation.reset()`, which nulls the window manager that
    /// `dispose()` needs to reach the peer, and is followed by another flush
    /// because disposing fires window events and those listeners queue work.
    private void disposeLeftoverWindows() throws Exception {
        if (!Display.isInitialized() || !Display.getInstance().isEdt()) {
            // dispose() marshals itself onto the dispatch thread when called from
            // anywhere else, so off-EDT the disposal would land after the next test
            // has already started -- exactly the leak this exists to close.
            return;
        }
        com.codename1.ui.Window[] open = com.codename1.ui.Desktop.getInstance().getWindows();
        if (open.length == 0) {
            return;
        }
        for (com.codename1.ui.Window w : open) {
            try {
                w.dispose();
            } catch (RuntimeException ignored) {
                // A window whose peer never opened can throw on the way down. It is
                // the registry entry that has to go, and the loop below removes it
                // whether or not dispose() got that far.
            }
        }
        // dispose() deregisters, so this is normally empty already. It is not when a
        // window failed to open and was registered without a peer to tear down, and
        // leaving that entry behind would defeat the whole method.
        Field windows = com.codename1.ui.Desktop.class.getDeclaredField("windows");
        windows.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> registry = (List<Object>) windows.get(com.codename1.ui.Desktop.getInstance());
        if (registry != null) {
            synchronized (registry) {
                registry.clear();
            }
        }
        // deregisterWindow() drops the focus alongside the registry entry; clearing
        // the list behind its back would leave a disposed window still answering
        // Desktop.getFocusedWindow(), which is what a Dialog resolves its host from.
        Field focused = com.codename1.ui.Desktop.class.getDeclaredField("focusedWindow");
        focused.setAccessible(true);
        focused.set(com.codename1.ui.Desktop.getInstance(), null);
        DisplayTest.flushEdt();
    }

    private void resetDisplayBooleanArrayField(String name) {
        try {
            Field f = Display.class.getDeclaredField(name);
            f.setAccessible(true);
            boolean[] values = (boolean[]) f.get(display);
            if (values != null) {
                java.util.Arrays.fill(values, false);
            }
        } catch (Exception ignored) {
        }
    }

    private void resetDisplayBooleanField(String name, boolean value) {
        try {
            Field f = Display.class.getDeclaredField(name);
            f.setAccessible(true);
            f.setBoolean(display, value);
        } catch (Exception ignored) {
        }
    }

    private void resetDisplayIntField(String name, int value) {
        try {
            Field f = Display.class.getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(display, value);
        } catch (Exception ignored) {
        }
    }

    @AfterAll
    protected void tearDownClass() {
        Display.deinitialize();
    }


    private void resetUIManager() throws Exception {
        UIManager.getInstance().setThemeProps(new Hashtable());
        UIManager.getInstance().getLookAndFeel().setRTL(false);
    }


    /**
     * Processes any pending serial calls that were queued via {@link Display#callSerially(Runnable)}.
     */
    protected void flushSerialCalls() {
        DisplayTest.flushEdt();
    }

    protected void tapComponent(Component c) {
        implementation.tapComponent(c);
        flushSerialCalls();
    }
}
