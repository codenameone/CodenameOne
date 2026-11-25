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

import com.codename1.presentation.Presentation;
import com.codename1.presentation.Slide;
import com.codename1.presentation.SlideAnimation;
import com.codename1.presentation.SlideRenderer;
import com.codename1.ui.CN;
import com.codename1.ui.Graphics;

/// The builds of the shapes slide: the star zooms in while the card flies up, then the badge
/// fades in and the connector wipes across, painted from `SlideAnimation.states`.
public class SlideBuildAnimationScreenshotTest extends AbstractAnimationScreenshotTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    private Presentation deck;
    private SlideRenderer renderer;

    @Override
    public boolean runTest() throws Exception {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=SlideBuildAnimation status=SKIPPED reason=slides-on-watch");
            done();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        deck = SampleSlides.deck();
        renderer = new SlideRenderer();
    }

    @Override
    protected int framesThatMustMove() {
        return 5;
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress,
            int frameIndex) {
        g.setColor(0x000000);
        g.fillRect(0, 0, width, height);
        Slide s = deck.slides.get(0);
        float scale = Math.min(width / deck.width, height / deck.height);
        int x = Math.round((width - deck.width * scale) / 2);
        int y = Math.round((height - deck.height * scale) / 2);
        // step 0 is what plays on entry, before any click; this slide's first build is
        // on click, so its builds are step 1
        long elapsed = Math.round(progress * SlideAnimation.stepDuration(s, 1));
        renderer.paint(g, deck, s, x, y, scale, SlideAnimation.states(s, 1, elapsed));
    }

    @Override
    protected void finishCapture() {
        deck = null;
        renderer = null;
        super.finishCapture();
    }
}
