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
package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.AbstractAnimationScreenshotTest;

/// Frame grids of `com.codename1.ui.chart.Chart` animations. The chart reads
/// `AnimationTime` for every transition, so with the clock pinned per frame by
/// `AbstractAnimationScreenshotTest` each cell is a deterministic point of the animation.
///
/// The chart is placed on the shown host form rather than an off-screen one: a chart starts its
/// entry and data transitions only once it is initialized, which an off-screen form never does.
/// Each frame calls `animate()` (the viewport animation advances there) and paints the form.
abstract class AbstractChart2AnimationScreenshotTest extends AbstractAnimationScreenshotTest {
    private Chart chart;

    /// The chart to animate, with its series added (the entry animation captures them on show).
    protected abstract Chart buildChart();

    /// Runs after the chart is on the form and painted once, with the clock at the start time;
    /// starts the animation the grid shows. The default starts nothing, which is the entry.
    protected void startAnimation(Chart c) {
    }

    protected final Chart getChart() {
        return chart;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        Form host = getHostForm();
        chart = buildChart();
        host.removeAll();
        host.add(BorderLayout.CENTER, chart);
        layoutOffScreen(host);
        // one paint lays out the plot, which pointer handling and zoom read
        Image scratch = Image.createImage(Math.max(1, frameWidth), Math.max(1, frameHeight), 0xffffffff);
        host.paintComponent(scratch.getGraphics(), true);
        scratch.dispose();
        startAnimation(chart);
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        chart.animate();
        getHostForm().paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        chart = null;
        super.finishCapture();
    }
}
