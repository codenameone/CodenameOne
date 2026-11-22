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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.GpuImage;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// A Rive file with an image mesh skinned to bones (`jellyfish_test`), shown
/// on a form as an image in a label. Where the port has a GPU backend the
/// image is a `GpuImage`: it places a GPU surface over the label's area and
/// draws there, which is the path this captures; `RiveAnimatedScreenshotTest`
/// draws the same file through `Graphics`. The image is paused and advanced
/// to a fixed point, so the capture does not depend on the animation clock.
public class RiveGpuScreenshotTest extends BaseTest {
    // cn1ss-skip: macos, watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.

    @Override
    public boolean runTest() {
        Form form = createForm("Rive on the GPU", new BorderLayout(), "RiveGpu");
        if (!CN.isGpuSupported()) {
            done();
            return true;
        }
        Image img = RiveFeaturesScreenshotTest.fresh("jellyfish_test.riv");
        image = img;
        RiveAnimatedScreenshotTest.step(img, 0.5f);
        form.add(BorderLayout.CENTER, RiveFeaturesScreenshotTest.holder(img));
        form.show();
        return true;
    }

    private Image image;

    @Override
    public boolean shouldTakeScreenshot() {
        return CN.isGpuSupported();
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        // the surface is placed after the first paint and compiles its
        // programs on its first frame, which can take seconds on a slow
        // emulator: poll until it has rendered a frame, then give it one more
        // tick to reach the screen; a generous cap keeps a dead surface from
        // hanging
        final Form form = parent;
        final Runnable ready = run;
        final long deadline = System.currentTimeMillis() + 30000L;
        UITimer.timer(100, false, form, new Runnable() {
            @Override
            public void run() {
                boolean drawn = image instanceof GpuImage
                        && ((GpuImage) image).getFramesRendered() > 0;
                if (drawn || !(image instanceof GpuImage)
                        || System.currentTimeMillis() > deadline) {
                    UITimer.timer(200, false, form, ready);
                } else {
                    UITimer.timer(100, false, form, this);
                }
            }
        });
    }
}
