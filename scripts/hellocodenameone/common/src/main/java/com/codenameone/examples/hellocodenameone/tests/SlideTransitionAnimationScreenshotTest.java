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

/// The push transition from the shapes slide to the charts slide, then the zoom into the
/// diagrams, painted by `SlideAnimation` at six points of progress.
public class SlideTransitionAnimationScreenshotTest extends AbstractAnimationScreenshotTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    private Presentation deck;
    private SlideRenderer renderer;

    @Override
    public boolean runTest() throws Exception {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=SlideTransitionAnimationScreenshotTest status=SKIPPED reason=slides-on-watch");
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
        float scale = Math.min(width / deck.width, height / deck.height);
        int x = Math.round((width - deck.width * scale) / 2);
        int y = Math.round((height - deck.height * scale) / 2);
        // the first half pushes to the charts, the second zooms into the diagrams
        float p = (float) progress * 2;
        Slide from = deck.slides.get(p < 1 ? 0 : 1);
        Slide to = deck.slides.get(p < 1 ? 1 : 2);
        float t = p < 1 ? p : p - 1;
        SlideAnimation.paintTransition(g, renderer, deck, from, null, to, null, x, y, scale,
                Math.min(1, t));
    }

    @Override
    protected void finishCapture() {
        deck = null;
        renderer = null;
        super.finishCapture();
    }
}
