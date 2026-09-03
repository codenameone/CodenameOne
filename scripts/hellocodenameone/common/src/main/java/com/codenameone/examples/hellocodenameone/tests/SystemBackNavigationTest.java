/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

/// Proves the Android system back action still reaches the Codename One form
/// stack.
///
/// Android 16 turns predictive back on by default for an app that targets API
/// 36, and the platform then stops calling `Activity.onBackPressed()`: back is
/// delivered to an `OnBackInvokedCallback`, and without one the system pops the
/// activity off the task so the current form's back command never runs. Every
/// Codename One Android build targets API 36, so this is the whole of back
/// navigation, not an edge case.
///
/// The test cannot press back itself -- injecting a system key needs a
/// permission no app holds -- so it hands that half to the instrumentation
/// process. It shows a form carrying a back command, announces
/// `CN1SS:BACKPROBE:READY`, and blocks. `DeviceRunnerInstrumentationTest`
/// watches the log for that line and injects one back through `UiAutomation`,
/// which is the same dispatch path the hardware key and the back gesture use.
/// The back command completing is what ends the test; the instrumentation
/// fails the run when nothing answers, which is exactly what an unmigrated port
/// produces.
///
/// It runs near the front of the suite rather than at the end, because the
/// instrumentation's own wait is shorter than a full suite: a probe announced
/// after that wait expired would be answered by nobody and the assertion would
/// silently never run.
///
/// Every other port skips it: no other platform this suite runs on has a
/// system back action for the instrumentation to inject.
public class SystemBackNavigationTest extends BaseTest {
    /// Printed once the probe form is on screen. The instrumentation process
    /// blocks on this line before injecting, so the back cannot be delivered
    /// to whatever form the previous test left behind.
    static final String READY_MARKER = "CN1SS:BACKPROBE:READY";

    /// Printed from the back command, purely so a failing run shows in the log
    /// whether the event arrived late or never.
    static final String INVOKED_MARKER = "CN1SS:BACKPROBE:INVOKED";

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        if (!"and".equals(Display.getInstance().getPlatformName())) {
            // Scoped to Android in the port-status contract, so no other port's
            // report carries this test; say so in the log all the same, because
            // a reader of a suite log should not have to consult the contract
            // to learn why a test went by in no time.
            System.out.println("CN1SS:INFO:test=SystemBackNavigationTest status=SKIPPED"
                    + " reason=no-system-back-action");
            done();
            return true;
        }
        Form probeForm = new Form("System Back", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                // Announced from onShowCompleted so the injected back lands on
                // this form rather than on the one it is replacing.
                System.out.println(READY_MARKER);
            }
        };
        probeForm.add(BorderLayout.CENTER, new Label("Waiting for the system back action"));
        Command back = new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // No showBack(): the next test shows its own form, and an
                // extra back transition here would only be one more animation
                // for it to start behind.
                System.out.println(INVOKED_MARKER);
                done();
            }
        };
        probeForm.setBackCommand(back);
        probeForm.show();
        return true;
    }
}
