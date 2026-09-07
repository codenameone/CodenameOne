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
package com.codenameone.examples.hellocodenameone;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DeviceRunnerInstrumentationTest {
    private static final String TAG = "DeviceRunnerTest";

    /// Printed by SystemBackNavigationTest once its probe form is on screen.
    /// The app cannot press the system back itself -- injecting a system key
    /// needs a permission no app holds -- so the two halves meet on the log.
    private static final String BACK_PROBE_READY = "CN1SS:BACKPROBE:READY";

    /// Printed from that probe's back command, i.e. the back action completed
    /// its trip through the port and the Codename One form stack.
    private static final String BACK_PROBE_INVOKED = "CN1SS:BACKPROBE:INVOKED";

    /// Printed by the runner as it starts each test. Seeing one is what tells
    /// the back assertions below that the suite really got going, so "the probe
    /// never announced" can be reported as the failure it is rather than
    /// confused with an app that never started.
    private static final String SUITE_TEST_MARKER = "CN1SS:INFO:suite starting test=";

    /// How long an injected back gets to come back round as a Codename One
    /// back command. Measured at well under a second on an emulator; the
    /// margin is for a loaded runner, and it is wider than the suite's 30s
    /// per-test budget so the runner's one retry re-announces rather than
    /// races this.
    private static final long BACK_ANSWER_TIMEOUT_MS = 45_000L;

    @Test
    public void launchMainActivityAndWaitForDeviceRunner() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        grantNotificationPermission(context.getPackageName());
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull("Launch intent not found for package " + context.getPackageName(), intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        SuiteOutcome outcome = waitForDeviceRunner();
        if (!outcome.finished) {
            // Soft, and deliberately so: this wait is capped well below the
            // length of a full suite, and the shell harness picks the
            // completion marker up afterwards.
            Log.w(TAG, "DeviceRunner did not emit completion marker; proceeding without hard failure");
        }
        // The back assertions ARE hard. SystemBackNavigationTest runs second,
        // so a suite that produced any output at all reached it within seconds
        // -- long before this wait could expire -- and both of these say
        // something is really wrong rather than merely slow.
        if (outcome.sawSuiteOutput) {
            assertTrue("The suite ran but never announced " + BACK_PROBE_READY
                            + "; SystemBackNavigationTest did not reach its probe form",
                    outcome.backProbeReady);
            // Nothing answering the injected back means the system back action
            // no longer reaches Codename One, which is what Android 16 does to
            // a port that still relies on Activity.onBackPressed().
            assertTrue("System back was injected while " + BACK_PROBE_READY
                            + " was showing, but the app never reported "
                            + BACK_PROBE_INVOKED
                            + "; the back action did not reach Codename One",
                    outcome.backProbeInvoked);
        }
    }

    /// Presses the system back the way the hardware key and the three-button
    /// navigation bar do, through the shell rather than through an accessibility
    /// action: on Android 16 the framework turns that key into an
    /// `OnBackInvokedCallback` dispatch, so this is the path the port's callback
    /// has to survive.
    private void injectSystemBack() {
        try {
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            ParcelFileDescriptor pfd = automation.executeShellCommand("input keyevent KEYCODE_BACK");
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                byte[] buffer = new byte[256];
                while (fis.read(buffer) > 0) {
                    // draining the pipe is what lets the command run to completion
                }
            } finally {
                pfd.close();
            }
            Log.i(TAG, "Injected KEYCODE_BACK for the back-navigation probe");
        } catch (Throwable t) {
            Log.w(TAG, "Could not inject KEYCODE_BACK: " + t);
        }
    }

    /// What the log watch below observed, so the assertions can distinguish
    /// "back never arrived" from "the suite never got that far".
    private static final class SuiteOutcome {
        boolean finished;
        boolean sawSuiteOutput;
        boolean backProbeReady;
        boolean backProbeInvoked;
    }

    /// Pre-grants POST_NOTIFICATIONS, the way an unattended instrumentation suite has to.
    ///
    /// The suite's SurfacesPublishTest starts a live activity, which on Android 13+ lowers to an
    /// ongoing notification and therefore needs this permission. Without the grant the port asks
    /// for it, the system dialog opens over the app, and nobody is there to answer: the requesting
    /// thread waits on it indefinitely and the modal sits on top of every screenshot that follows,
    /// so the suite stops emitting output partway through and never reaches its completion marker.
    /// Granting up front is what a device user does once by hand, and it lets the suite exercise
    /// the granted path rather than the prompt.
    ///
    /// Failures are logged and ignored: below API 33 the permission does not exist and `pm grant`
    /// rejects it, which is correct and must not fail the run.
    private void grantNotificationPermission(String packageName) {
        String command = "pm grant " + packageName + " android.permission.POST_NOTIFICATIONS";
        try {
            UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            ParcelFileDescriptor pfd = automation.executeShellCommand(command);
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                // draining the pipe is what lets the command run to completion
                byte[] buffer = new byte[256];
                while (fis.read(buffer) > 0) {
                    // discard
                }
            } finally {
                pfd.close();
            }
            Log.i(TAG, "Granted POST_NOTIFICATIONS to " + packageName);
        } catch (Throwable t) {
            Log.w(TAG, "Could not grant POST_NOTIFICATIONS (expected below API 33): " + t);
        }
    }

    private SuiteOutcome waitForDeviceRunner() throws Exception {
        final long timeoutMs = 900_000L;
        final String endMarker = "CN1SS:SUITE:FINISHED";

        SuiteOutcome outcome = new SuiteOutcome();
        long deadline = System.currentTimeMillis() + timeoutMs;
        // Set when a back is injected; nothing answering by then is the answer.
        long backAnswerDeadline = 0L;
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor pfd = automation.executeShellCommand("logcat -v brief");
        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while (System.currentTimeMillis() < deadline) {
                if (backAnswerDeadline > 0L && !outcome.backProbeInvoked
                        && System.currentTimeMillis() > backAnswerDeadline) {
                    // Return now rather than sit out the rest of the suite
                    // budget. This is not only about speed: an unmigrated port
                    // LEAVES the activity on a back press, and this
                    // instrumentation runs inside the app's own process, so
                    // waiting means the process dies first and the run is
                    // reported as "Process crashed" -- red, but saying nothing
                    // about back navigation. Failing here says it.
                    Log.w(TAG, "No answer to the injected back within the probe window");
                    return outcome;
                }
                if (reader.ready() && (line = reader.readLine()) != null) {
                    if (line.contains(SUITE_TEST_MARKER)) {
                        outcome.sawSuiteOutput = true;
                    }
                    if (line.contains(BACK_PROBE_READY)) {
                        // Injected per announcement rather than once: the
                        // runner re-runs a test that timed out silently, and a
                        // second probe has to be answered too. The window is
                        // wider than that retry (the suite's per-test budget is
                        // 30s) so the retry's announcement moves the deadline
                        // rather than racing it.
                        outcome.backProbeReady = true;
                        outcome.backProbeInvoked = false;
                        injectSystemBack();
                        backAnswerDeadline = System.currentTimeMillis() + BACK_ANSWER_TIMEOUT_MS;
                    } else if (line.contains(BACK_PROBE_INVOKED)) {
                        outcome.backProbeInvoked = true;
                        backAnswerDeadline = 0L;
                    } else if (line.contains(endMarker)) {
                        outcome.finished = true;
                        return outcome;
                    }
                } else {
                    Thread.sleep(200);
                }
            }
        } finally {
            try {
                pfd.close();
            } catch (Exception ignored) {
            }
        }
        return outcome;
    }
}
