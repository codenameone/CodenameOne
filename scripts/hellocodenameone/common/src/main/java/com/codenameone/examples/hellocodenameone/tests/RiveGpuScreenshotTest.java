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

import com.codename1.generated.rive.JellyfishTestRive;
import com.codename1.impl.rive.GpuRiveRenderer;
import com.codename1.rive.RiveView;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// A Rive file with an image mesh skinned to bones (`jellyfish_test`), shown
/// on a form. Where the port has a GPU backend `RiveView` draws it into a
/// `RenderView` through `GpuRiveRenderer`, which is the path this captures;
/// `RiveAnimatedScreenshotTest` paints the same file into an image, through
/// `Graphics`. The view is paused (views play by default) and advanced to a
/// fixed point, so the capture does not depend on the animation clock.
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
        framesBefore = GpuRiveRenderer.getTotalFramesDrawn();
        RiveView view = new RiveView(new JellyfishTestRive());
        view.pause();
        step(view, 0.5f);
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    private int framesBefore;

    @Override
    public boolean shouldTakeScreenshot() {
        return CN.isGpuSupported();
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        // the surface compiles its programs on the first frame, which can
        // take seconds on a slow emulator: poll until the GPU renderer has
        // drawn a frame (an impl counter, not public API), then give it one more tick to
        // reach the screen; a generous cap keeps a dead surface from hanging
        final Form form = parent;
        final Runnable ready = run;
        final long deadline = System.currentTimeMillis() + 30000L;
        UITimer.timer(100, false, form, new Runnable() {
            @Override
            public void run() {
                if (GpuRiveRenderer.getTotalFramesDrawn() > framesBefore
                        || System.currentTimeMillis() > deadline) {
                    UITimer.timer(200, false, form, ready);
                } else {
                    UITimer.timer(100, false, form, this);
                }
            }
        });
    }

    /// Steps a fresh view to `seconds` the way playback does, a frame at a
    /// time. A state machine starts in Entry: its first advance only enters
    /// the first state, at that animation's time 0, so one call of `seconds`
    /// would show the first frame however large `seconds` is (rive-runtime
    /// behaves the same; a player starts with small steps).
    private static void step(RiveView view, float seconds) {
        view.advance(0f);
        float left = seconds;
        while (left > 0f) {
            float dt = Math.min(left, 1f / 60f);
            view.advance(dt);
            left -= dt;
        }
    }
}
