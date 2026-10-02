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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.ui.Display;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The base of the tests that drive the CI's backend
/// (scripts/hellocodenameone/backend) with the app's networking APIs.
///
/// The backend is the same process that receives the screenshots, on the same
/// port, so a leg that runs the suite has it: 10.0.2.2:8765 from the Android
/// emulator, 127.0.0.1:8765 everywhere else, `-Dcn1ss.backend.url` to override.
/// A request that cannot reach it FAILS the test -- an unreachable server is a
/// broken leg, not a reason to skip.
///
/// Every call is asynchronous, so a test is a list of steps: each step sends one
/// request and its callback checks the answer and moves on with [#proceed]. A
/// synchronous call would block the EDT, which the browser port cannot do.
///
/// A step that never hears back FAILS, naming itself, after [#STEP_TIMEOUT_MILLIS].
/// Without that, a request a port drops -- no callback of any kind -- left the
/// suite's per-test timeout to report "timeout waiting for DONE", which says
/// nothing about which request or why.
public abstract class BackendClientTest extends BaseTest {
    /// How long one step may wait for its answer. Under the runner's per-test
    /// budget, and well over the slowest step (a deliberate four-second delay).
    static final long STEP_TIMEOUT_MILLIS = 12000;

    /// Marks a request that reports its own failures through `handleException`.
    /// The application's network error listener (`Lifecycle.handleNetworkError`)
    /// consumes every error it sees, and `NetworkManager` calls a request's own
    /// handler only when no listener consumed the error -- so a step expecting a
    /// failure (the timeout probe) never heard back and a modal "Connection Error"
    /// dialog was left over the screens of later tests. The application leaves
    /// requests carrying this marker alone.
    public interface ReportsOwnErrors {
    }

    private final List<Runnable> steps = new ArrayList<Runnable>();
    private int current = -1;
    private java.util.Timer watchdog;

    /// The backend's base URL, with no trailing slash.
    public static String baseUrl() {
        String url = Display.getInstance().getProperty("cn1ss.backend.url", "");
        if (url == null || url.length() == 0) {
            String host = "and".equals(Display.getInstance().getPlatformName())
                    ? "10.0.2.2" : "127.0.0.1";
            url = "http://" + host + ":8765";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /// `path` on the backend.
    protected static String url(String path) {
        return baseUrl() + path;
    }

    /// Whether this is the browser port, whose cookies and redirects belong to the
    /// browser rather than to Codename One.
    protected static boolean isBrowser() {
        return "HTML5".equals(Display.getInstance().getPlatformName());
    }

    /// Adds the steps of this test, in order.
    protected abstract void defineSteps();

    /// Adds one step. It must end -- in its callback -- with [#proceed] or a failure.
    protected final void step(Runnable step) {
        steps.add(step);
    }

    @Override
    public boolean runTest() {
        steps.clear();
        current = -1;
        defineSteps();
        proceed(-1);
        return true;
    }

    /// The index of the step running now, for [#proceed].
    protected final int currentStep() {
        return current;
    }

    /// Runs the step after `from`, once: a late or repeated callback for a step
    /// that already moved on is ignored rather than running the rest twice.
    protected final void proceed(int from) {
        // Callbacks arrive on the EDT, one at a time, so no lock is needed here.
        if (isFailed() || from != current) {
            return;
        }
        current++;
        Runnable next = current >= steps.size() ? null : steps.get(current);
        if (next == null) {
            stopWatchdog();
            done();
            return;
        }
        watch(current);
        try {
            next.run();
        } catch (Throwable err) {
            stopWatchdog();
            fail("step " + (current + 1) + " threw " + err);
        }
    }

    /// Fails the test if step `step` is still the current one when its time is up.
    private void watch(final int step) {
        if (watchdog == null) {
            watchdog = new java.util.Timer();
        }
        watchdog.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (!isFailed() && current == step) {
                            stopWatchdog();
                            fail(BackendClientTest.this.getClass().getName() + " step " + (step + 1)
                                    + " got no answer in " + STEP_TIMEOUT_MILLIS + "ms: the request "
                                    + "was dropped without a response or an error");
                        }
                    }
                });
            }
        }, STEP_TIMEOUT_MILLIS);
    }

    private void stopWatchdog() {
        if (watchdog != null) {
            watchdog.cancel();
            watchdog = null;
        }
    }

    /// Fails the test, as [#fail] does, from any step's error path.
    protected final void failStep(String message) {
        stopWatchdog();
        fail(getClass().getName() + " step " + (currentStep() + 1) + ": " + message);
    }

    /// Fails the test with `message` unless `condition` holds; true when it does.
    protected final boolean expect(boolean condition, String message) {
        if (!condition) {
            failStep(message);
        }
        return condition;
    }

    /// A JSON number as an int, whichever boxing the parser chose.
    protected static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : Integer.MIN_VALUE;
    }

    /// A value of a parsed JSON object, or null.
    protected static Object field(Object map, String key) {
        return map instanceof Map ? ((Map) map).get(key) : null;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    /// The callbacks outlive runTest(), which a retry would race.
    @Override
    public boolean isRetrySafe() {
        return false;
    }
}
