/*
 * Copyright (c) 2025, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;

/// End-to-end test for the build-time Lottie transcoder. Mirrors
/// {@link SVGAnimatedScreenshotTest}: the source asset lands in
/// `src/main/css/`, the build-time transcoder lowers it into the SVG
/// pipeline, and the auto-generated SVGRegistry replaces the CSS-emitted
/// placeholder before this test runs. The clock is pinned by
/// {@link AbstractAnimationScreenshotTest} so the captured frame is
/// deterministic.
public class LottieAnimatedScreenshotTest extends AbstractAnimationScreenshotTest {

    private static final int ANIM_DURATION_MS = 1000;

    private Image spinner;
    private Image pulse;

    @Override
    public boolean shouldTakeScreenshot() {
        return !"HTML5".equals(Display.getInstance().getPlatformName());
    }

    @Override
    public boolean runTest() throws Exception {
        if ("HTML5".equals(Display.getInstance().getPlatformName())) {
            done();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected int getAnimationDurationMillis() {
        return ANIM_DURATION_MS;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        Resources res = SVGStaticScreenshotTest.resolveGlobalResources();
        spinner = res == null ? null : res.getImage("lottie_spinner.json");
        pulse = res == null ? null : res.getImage("lottie_pulse.json");
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height,
                               double progress, int frameIndex) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);

        if (spinner == null || pulse == null) {
            g.setColor(0xFF0000);
            g.drawString("Lottie registry not installed", 10, 20);
            return;
        }

        int half = width / 2;
        Image scaledSpinner = spinner.scaled(half, height);
        Image scaledPulse = pulse.scaled(width - half, height);
        g.drawImage(scaledSpinner, 0, 0);
        g.drawImage(scaledPulse, half, 0);
    }
}
