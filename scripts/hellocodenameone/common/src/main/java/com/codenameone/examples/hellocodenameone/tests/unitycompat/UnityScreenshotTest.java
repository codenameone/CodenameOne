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
package com.codenameone.examples.hellocodenameone.tests.unitycompat;

import com.codename1.generated.unity.UnityAppImpl;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.unitycompat.unityengine.Random;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.ui.UnityGameView;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// One scene of the Unity compatibility gallery
/// (scripts/unity-compat-samples/gallery), which this app compiles from its
/// Unity project unmodified. The test loads the scene by itself into a game
/// view whose clock is held, steps it a fixed number of frames of a fixed
/// length -- so the picture does not depend on how fast the device draws --
/// captures the form, and takes the runtime down again: no object, no clock
/// and no view of this scene is left for the next test.
///
/// A scene is drawn by the GPU, as every Unity scene is. On a port without
/// one the test passes without a capture, the way `Gpu3DCubeScreenshotTest`
/// does.
public abstract class UnityScreenshotTest extends BaseTest {
    /// The frames every scene is stepped, and the length of each: a second
    /// and a half of a game running at sixty frames a second, which is long
    /// enough for a body to come to rest and for an animator to be well into
    /// its second clip.
    private static final int FRAMES = 90;
    private static final float FRAME_SECONDS = 1f / 60;
    /// How long the steps may take to be drawn before the test gives up.
    private static final int STEP_BUDGET_MILLIS = 20000;
    private static final int POLL_MILLIS = 50;
    /// After the last step was drawn: the peer presents a frame later than
    /// it renders it on some ports, and the view is still, so waiting costs
    /// nothing but time.
    private static final int PRESENT_MILLIS = 1000;

    private final String scene;
    private final String imageName;
    private UnityGameView view;

    /// `scene` is the name of the scene's file without `.unity`.
    protected UnityScreenshotTest(String scene) {
        this.scene = scene;
        this.imageName = "UnityCompat" + scene;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return CN.isGpuSupported();
    }

    /// The index of the scene in the gallery's build settings, or -1.
    private int sceneIndex() {
        String file = "/" + scene + ".unity";
        int n = UnityRuntime.$sceneCount();
        for (int i = 0; i < n; i++) {
            String path = "/" + UnityRuntime.$scenePath(i);
            if (path.endsWith(file)) {
                return i;
            }
        }
        return -1;
    }

    /// Stops the view and empties the runtime. Also what a rerun starts
    /// with, so an attempt that timed out leaves nothing running.
    private void tearDown() {
        if (view != null) {
            view.stop();
            view.remove();
            view = null;
        }
        UnityRuntime.reset();
    }

    @Override
    public boolean runTest() throws Exception {
        if (!CN.isGpuSupported()) {
            done();
            return true;
        }
        tearDown();
        // The same numbers on every run, for the scenes that ask for random
        // ones (the particle systems).
        Random.InitState(1);
        UnityAppImpl.install();
        int index = sceneIndex();
        if (index < 0) {
            UnityRuntime.reset();
            fail("the gallery has no scene named " + scene);
            return false;
        }
        final UnityGameView gameView = new UnityGameView();
        if (!gameView.isSupported()) {
            UnityRuntime.reset();
            done();
            return true;
        }
        view = gameView;
        gameView.installServices();
        Display display = Display.getInstance();
        UnityRuntime.resize(display.getDisplayWidth(), display.getDisplayHeight());
        UnityRuntime.begin(index);
        final Form form = new Form(new BorderLayout());
        form.getTitleArea().setHidden(true);
        if (form.getToolbar() != null) {
            form.getToolbar().setHidden(true);
        }
        form.setScrollable(false);
        form.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        form.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
        form.add(BorderLayout.CENTER, gameView);
        // Held before it starts: the frames the display draws before the
        // steps below must not move anything.
        gameView.holdClock();
        gameView.start();
        final int before = gameView.framesAdvanced();
        gameView.advance(FRAMES, FRAME_SECONDS);
        form.show();
        awaitSteps(form, gameView, before, 0);
        return true;
    }

    /// Waits for the frame that runs the steps to have been drawn, then for
    /// it to be on screen, then captures.
    private void awaitSteps(final Form form, final UnityGameView gameView, final int before, final int waited) {
        if (view != gameView) {
            // Torn down by a rerun; this attempt is over.
            return;
        }
        if (gameView.framesAdvanced() - before >= FRAMES) {
            UITimer.timer(PRESENT_MILLIS, false, form, new Runnable() {
                @Override
                public void run() {
                    if (view != gameView) {
                        return;
                    }
                    captureWhenSettled(form, imageName, new Runnable() {
                        @Override
                        public void run() {
                            tearDown();
                            done();
                        }
                    });
                }
            });
            return;
        }
        if (waited >= STEP_BUDGET_MILLIS) {
            int ran = gameView.framesAdvanced() - before;
            tearDown();
            fail(scene + ": " + ran + " of " + FRAMES + " frames were drawn in " + waited + " ms");
            return;
        }
        UITimer.timer(POLL_MILLIS, false, form, new Runnable() {
            @Override
            public void run() {
                awaitSteps(form, gameView, before, waited + POLL_MILLIS);
            }
        });
    }
}
