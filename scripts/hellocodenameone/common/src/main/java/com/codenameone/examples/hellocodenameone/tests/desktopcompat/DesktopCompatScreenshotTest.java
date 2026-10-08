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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// One screen of the desktop compatibility layers: Swing, SwingX or JavaFX
/// code that this app compiles from `common/src/main/desktop` and embeds in a
/// form of its own.
///
/// The app has its own main class and the desktop sources name no entry point,
/// so they are built in library mode: relocated like any library, with nothing
/// started on their behalf. A scene is built by `SwingScenes` or `FxScenes`,
/// which hand it over as a Codename One component, and the test shows that
/// component in a plain form and captures the form once it has settled.
public abstract class DesktopCompatScreenshotTest extends BaseTest {

    private final String title;
    private final String imageName;

    protected DesktopCompatScreenshotTest(String title, String imageName) {
        this.title = title;
        this.imageName = imageName;
    }

    /// Builds the scene, as the Codename One component that shows it.
    protected abstract Component scene();

    @Override
    public boolean runTest() throws Exception {
        // Pin the light appearance, as the Android compatibility screens do:
        // the baseline must not depend on the host's appearance setting.
        Display.getInstance().setDarkMode(Boolean.FALSE);
        final Form form = new Form(title, new BorderLayout());
        form.add(BorderLayout.CENTER, scene());
        form.show();
        // A focused text field has a caret, which blinks.
        form.setFocused(null);
        UITimer.timer(1500, false, form, new Runnable() {
            @Override
            public void run() {
                captureWhenSettled(form, imageName, new Runnable() {
                    @Override
                    public void run() {
                        Display.getInstance().setDarkMode(null);
                        done();
                    }
                });
            }
        });
        return true;
    }
}
