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
package com.codename1.impl;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the contract behind {@code Display.exitAndClearTask()}: a port that has no notion of a
 * recents list must still exit, by degrading to {@code exitApplication()}, and a port that does
 * override the hook must not lose the {@code setOnExit} callback that the plain exit path runs.
 *
 * <p>The methods are exercised on the implementation rather than through {@code Display} because
 * {@code Display.exitAndClearTask()} latches {@code codenameOneExited}, which would leak into
 * every test that shares the JVM.</p>
 */
class ExitAndClearTaskTest {

    /**
     * A port that knows nothing about clearing tasks -- the default every implementation in the
     * tree except Android inherits.
     */
    private static class PlainPort extends TestCodenameOneImplementation {
        int exitApplicationCalls;

        PlainPort() {
            // The no-arg constructor publishes itself as the shared singleton that UITestBase
            // reads, so take the overload that does not and leave the JVM alone.
            super(true);
        }

        @Override
        public void exitApplication() {
            exitApplicationCalls++;
        }
    }

    /**
     * A port that does implement the platform hook, standing in for Android.
     */
    private static class ClearingPort extends PlainPort {
        int clearCalls;

        @Override
        public boolean isExitAndClearTaskSupported() {
            return true;
        }

        @Override
        public void exitApplicationAndClearTask() {
            clearCalls++;
        }
    }

    @Test
    void unsupportedPlatformFallsBackToExitApplication() {
        PlainPort impl = new PlainPort();
        assertFalse(impl.isExitAndClearTaskSupported(),
                "a port that does not override the hook must not claim support");
        impl.exitApplicationAndClearTask();
        assertEquals(1, impl.exitApplicationCalls,
                "the fallback is a plain exit, not a no-op");
    }

    @Test
    void supportingPlatformDoesNotFallBack() {
        ClearingPort impl = new ClearingPort();
        assertTrue(impl.isExitAndClearTaskSupported());
        impl.exitApplicationAndClearTask();
        assertEquals(1, impl.clearCalls);
        assertEquals(0, impl.exitApplicationCalls,
                "the platform hook replaces exitApplication(), it does not run before it");
    }

    @Test
    void onExitCallbackRunsExactlyLikeThePlainExitPath() {
        final int[] exitCallbackRuns = new int[1];
        Runnable previous = readOnExit();
        try {
            CodenameOneImplementation.setOnExit(new Runnable() {
                public void run() {
                    exitCallbackRuns[0]++;
                }
            });

            PlainPort plain = new PlainPort();
            plain.exit();
            assertEquals(1, exitCallbackRuns[0]);

            ClearingPort clearing = new ClearingPort();
            clearing.exitAndClearTask();
            assertEquals(2, exitCallbackRuns[0],
                    "exitAndClearTask() must honour setOnExit() the same way exit() does");
            assertEquals(1, clearing.clearCalls);
        } finally {
            CodenameOneImplementation.setOnExit(previous);
        }
    }

    /**
     * {@code onExit} is static and package private state with no getter, so the only way to leave
     * the JVM as we found it is to read the field back before overwriting it.
     */
    private static Runnable readOnExit() {
        try {
            java.lang.reflect.Field f = CodenameOneImplementation.class.getDeclaredField("onExit");
            f.setAccessible(true);
            return (Runnable) f.get(null);
        } catch (Exception e) {
            throw new AssertionError("onExit field is no longer reachable", e);
        }
    }
}
