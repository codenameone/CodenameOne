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
package com.codenameone.examples.hellocodenameone.tests.desktopcompat;

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// A modal dialog of a desktop compatibility layer, captured WHILE IT IS
/// OPEN over a form of this app, then answered from code.
///
/// The question is asked the way desktop code asks it: one call that
/// returns the answer, and blocks until there is one. The test therefore
/// runs in two strands on the event dispatch thread. The asking strand sits
/// inside the blocking call; a timer, which the dialog's nested event loop
/// keeps serving, waits for the dialog to be on screen, captures it and
/// presses its default button. The asking strand then returns, and the test
/// passes only if what it returned is the button that was pressed.
///
/// The dialog is kept inside the app's own window on every port: on a
/// desktop port it would otherwise be a window of the window manager, which
/// a capture of the form does not contain.
public abstract class DesktopCompatDialogScreenshotTest extends BaseTest {

    /// How long the dialog may take to appear. Well under the runner's
    /// shortest per-test timeout, so a dialog that never shows is reported
    /// as that rather than as an anonymous timeout.
    private static final int APPEAR_BUDGET_MS = 6000;
    private static final int POLL_MS = 100;

    private final String title;
    private final String imageName;
    private String failure;

    protected DesktopCompatDialogScreenshotTest(String title, String imageName) {
        this.title = title;
        this.imageName = imageName;
    }

    /// Asks the question and answers the name of what was chosen. Blocks.
    protected abstract String ask();

    /// Whether the dialog of [#ask()] is showing.
    protected abstract boolean showing();

    /// Presses the dialog's affirmative button; false if it is not there.
    protected abstract boolean answer();

    /// What [#ask()] returns when [#answer()] pressed the button.
    protected abstract String expected();

    /// The asking strand outlives runTest(): a rerun on this instance could be
    /// completed by the first attempt's answer.
    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        Display.getInstance().setDarkMode(Boolean.FALSE);
        failure = null;
        final Form host = new Form(title, new BorderLayout());
        host.add(BorderLayout.CENTER, new Label("The application's own form"));
        host.show();
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                watch(host, 0);
                String got;
                try {
                    got = ask();
                } catch (RuntimeException thrown) {
                    got = null;
                    failure = "the dialog threw " + thrown;
                }
                Display.getInstance().setDarkMode(null);
                if (failure == null && !expected().equals(got)) {
                    failure = "the blocking call returned " + got + ", expected " + expected();
                }
                if (failure != null) {
                    fail(imageName + ": " + failure);
                } else {
                    done();
                }
            }
        });
        return true;
    }

    /// Waits for the dialog to be the current form, then captures it. A plain
    /// timer of the display, not a UITimer: those belong to a form, and the
    /// host's stop running the moment the dialog covers it.
    private void watch(final Form host, final int waitedMs) {
        CN.setTimeout(POLL_MS, new Runnable() {
            @Override
            public void run() {
                final Form current = Display.getInstance().getCurrent();
                if (showing() && current != null && current != host) {
                    // The same settle the other screens get before a capture.
                    CN.setTimeout(1500, new Runnable() {
                        @Override
                        public void run() {
                            capture(current);
                        }
                    });
                    return;
                }
                if (waitedMs >= APPEAR_BUDGET_MS) {
                    failure = "the dialog was not on screen after " + waitedMs + "ms (showing=" + showing() + ")";
                    // Let the asking strand go, or it blocks the suite.
                    answer();
                    return;
                }
                watch(host, waitedMs + POLL_MS);
            }
        });
    }

    private void capture(Form dialog) {
        captureWhenSettled(dialog, imageName, new Runnable() {
            @Override
            public void run() {
                if (!answer()) {
                    failure = "the dialog had no button to press";
                }
            }
        });
    }
}
