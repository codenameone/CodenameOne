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

import com.codename1.generated.rive.CircleClipsRive;
import com.codename1.generated.rive.JellyfishTestRive;
import com.codename1.generated.rive.JuiceRive;
import com.codename1.generated.rive.OffRoadCarRive;
import com.codename1.rive.RiveFile;
import com.codename1.rive.RiveView;
import com.codename1.ui.Graphics;

/// End-to-end test for the build-time Rive translation. The `.riv` files in
/// `src/main/rive` are rive-runtime test assets, each translated into a class
/// by the build; every frame of the strip draws the four side by side at the
/// same point of their first animation:
///
/// - `juice`: paths, solid fills and a timeline of transforms;
/// - `off_road_car`: a vehicle of many grouped shapes with gradients and strokes;
/// - `circle_clips`: clipping shapes and strokes;
/// - `jellyfish_test`: an image mesh skinned to bones. Painted into an image,
///   the view draws it through `Graphics`; on screen with a GPU backend it
///   draws through `com.codename1.gpu`.
///
/// Each frame builds fresh views and advances them from zero, so the capture
/// does not depend on what was drawn before it.
public class RiveAnimatedScreenshotTest extends AbstractAnimationScreenshotTest {
    /// Seconds of animation the strip spans, start to end.
    private static final float SPAN_SECONDS = 2f;

    private RiveFile[] files;

    @Override
    protected int getAnimationDurationMillis() {
        return (int) (SPAN_SECONDS * 1000);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        files = new RiveFile[]{new JuiceRive(), new OffRoadCarRive(), new CircleClipsRive(), new JellyfishTestRive()};
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);
        int cell = width / files.length;
        int[] clip = g.getClip();
        for (int i = 0; i < files.length; i++) {
            // A form gives each component its own clip; here the four share
            // one Graphics, and off_road_car's road reaches past its artboard
            g.setClip(i * cell, 0, cell, height);
            RiveView view = new RiveView(files[i]);
            view.pause();
            view.advance((float) progress * SPAN_SECONDS);
            view.setX(i * cell);
            view.setY(0);
            view.setWidth(cell);
            view.setHeight(height);
            view.paint(g);
        }
        g.setClip(clip);
    }
}
