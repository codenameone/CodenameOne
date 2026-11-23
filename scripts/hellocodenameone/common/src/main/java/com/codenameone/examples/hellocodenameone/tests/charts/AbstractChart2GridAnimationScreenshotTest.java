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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.hellocodenameone.tests.AbstractAnimationScreenshotTest;

/// A filmstrip of several charts animating at once, laid out in a grid, so that every chart
/// type is seen in motion without a filmstrip per type. Each frame of the strip shows the
/// whole grid at one moment. Every chart's cell must change from each of the first frames to
/// the next (`framesThatMustMove`): a chart type that jumps to its final picture instead of
/// animating leaves its cell unchanged, and the test fails naming that chart. A golden
/// cannot catch that by itself, since it records the stillness as the truth.
abstract class AbstractChart2GridAnimationScreenshotTest extends AbstractAnimationScreenshotTest {
    static final int DURATION = 600;
    private Chart[] charts;
    private Image probe;
    private long[] lastHashes;

    /// The charts of the grid, not yet shown; each is given `DURATION` before the capture.
    protected abstract Chart[] buildCharts();

    /// Starts what the filmstrip shows; by default nothing, so the charts' entry animations
    /// run. The clock is at the strip's start time.
    protected void startAnimation(Chart[] c) {
    }

    protected int columns() {
        return 2;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        Form host = getHostForm();
        charts = buildCharts();
        Container grid = new Container(new GridLayout((charts.length + columns() - 1) / columns(), columns()));
        for (Chart c : charts) {
            c.setAnimationDuration(DURATION);
            c.setTooltipMode(Chart.TOOLTIP_NONE);
            grid.add(c);
        }
        host.removeAll();
        host.add(BorderLayout.CENTER, grid);
        layoutOffScreen(host);
        Image scratch = Image.createImage(Math.max(1, frameWidth), Math.max(1, frameHeight), 0xffffffff);
        host.paintComponent(scratch.getGraphics(), true);
        scratch.dispose();
        startAnimation(charts);
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        for (Chart c : charts) {
            c.animate();
        }
        if (frameIndex > framesThatMustMove()) {
            getHostForm().paintComponent(g, true);
            return;
        }
        // painted into an image of our own so each chart's cell can be read back
        if (probe == null || probe.getWidth() != width || probe.getHeight() != height) {
            probe = Image.createImage(width, height, 0xffffffff);
        }
        Graphics pg = probe.getGraphics();
        pg.setColor(0xffffff);
        pg.fillRect(0, 0, width, height);
        getHostForm().paintComponent(pg, true);
        g.drawImage(probe, 0, 0);
        int[] rgb = probe.getRGB();
        long[] hashes = new long[charts.length];
        for (int i = 0; i < charts.length; i++) {
            hashes[i] = cellHash(rgb, width, height, charts[i]);
            if (lastHashes != null && hashes[i] == lastHashes[i]) {
                failAfterCapture("the chart \"" + charts[i].getTitle() + "\" did not move between frames F"
                        + frameIndex + " and F" + (frameIndex + 1) + ": it jumps instead of animating");
            }
        }
        lastHashes = hashes;
    }

    private static long cellHash(int[] rgb, int width, int height, Chart c) {
        int x0 = Math.max(0, c.getAbsoluteX());
        int y0 = Math.max(0, c.getAbsoluteY());
        int x1 = Math.min(width, x0 + c.getWidth());
        int y1 = Math.min(height, y0 + c.getHeight());
        long h = 1125899906842597L;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                h = 31 * h + rgb[y * width + x];
            }
        }
        return h;
    }

    @Override
    protected int getAnimationDurationMillis() {
        return DURATION;
    }

    @Override
    protected int framesThatMustMove() {
        // frames 0..4 are inside the animation; the sixth is its end, which can equal the
        // fifth once easing has nearly settled
        return 4;
    }

    @Override
    protected void finishCapture() {
        charts = null;
        lastHashes = null;
        super.finishCapture();
        // released once nothing draws into it any more
        if (probe != null) {
            probe.dispose();
            probe = null;
        }
    }
}
