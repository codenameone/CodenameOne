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

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/// End-to-end test for Rive files as images. The `.riv` files next to
/// `theme.css` are rive-runtime test assets; the build turns each into an
/// image, read like any other with `Resources.getImage`. Every frame of the
/// strip draws the four side by side at the same point of their first
/// animation:
///
/// - `juice`: paths, solid fills and a timeline of transforms;
/// - `off_road_car`: a vehicle of many grouped shapes with gradients and strokes;
/// - `circle_clips`: clipping shapes and strokes;
/// - `jellyfish_test`: an image mesh skinned to bones.
///
/// Each frame takes fresh images (`scaled` returns an independent image in
/// its initial state) and advances them from zero, so the capture does not
/// depend on what was drawn before it. Each cell is inset by the padding of
/// the `RiveView` UIID, which is where the strip drew before, so the goldens
/// still hold.
public class RiveAnimatedScreenshotTest extends AbstractAnimationScreenshotTest {
    /// Seconds of animation the strip spans, start to end.
    private static final float SPAN_SECONDS = 2f;

    private static final String[] FILES = {"juice.riv", "off_road_car.riv", "circle_clips.riv", "jellyfish_test.riv"};

    private Image[] images;

    @Override
    protected int getAnimationDurationMillis() {
        return (int) (SPAN_SECONDS * 1000);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        Resources res = Resources.getGlobalResources();
        images = new Image[FILES.length];
        for (int i = 0; i < FILES.length; i++) {
            images[i] = res.getImage(FILES[i]);
        }
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);
        int cell = width / images.length;
        Style s = UIManager.getInstance().getComponentStyle("RiveView");
        int padLeft = s.getPaddingLeft(false);
        int padTop = s.getPaddingTop();
        int innerW = Math.max(1, cell - s.getHorizontalPadding());
        int innerH = Math.max(1, height - s.getVerticalPadding());
        int[] clip = g.getClip();
        for (int i = 0; i < images.length; i++) {
            // the four share one Graphics, and off_road_car's road reaches
            // past its artboard
            g.setClip(i * cell, 0, cell, height);
            Image frame = images[i].scaled(innerW, innerH);
            frame.setAnimationPlaying(false);
            step(frame, (float) progress * SPAN_SECONDS);
            g.drawImage(frame, i * cell + padLeft, padTop);
        }
        g.setClip(clip);
    }

    /// Steps a fresh image to `seconds` the way playback does, a frame at a
    /// time. A state machine starts in Entry: its first advance only enters
    /// the first state, at that animation's time 0, so one call of `seconds`
    /// would show the first frame however large `seconds` is (rive-runtime
    /// behaves the same; a player starts with small steps).
    static void step(Image img, float seconds) {
        img.advanceAnimation(0f);
        float left = seconds;
        while (left > 0f) {
            float dt = Math.min(left, 1f / 60f);
            img.advanceAnimation(dt);
            left -= dt;
        }
    }
}
