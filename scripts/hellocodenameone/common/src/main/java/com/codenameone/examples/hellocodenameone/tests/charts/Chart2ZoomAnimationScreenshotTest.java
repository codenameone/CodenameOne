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

import com.codename1.ui.Graphics;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartContext;

/// The animated zoom of a double tap in the plot: the viewport eases over 300ms from the full
/// range to half of it, centered on the tap.
public class Chart2ZoomAnimationScreenshotTest extends AbstractChart2AnimationScreenshotTest {
    @Override
    protected Chart buildChart() {
        Chart c = new Chart();
        // no entry animation and no tooltip on the taps: the grid shows the zoom alone
        c.setAnimationDuration(0);
        c.setTooltipMode(Chart.TOOLTIP_NONE);
        return AbstractChart2ScreenshotTest.monthlySales(c);
    }

    @Override
    protected void startAnimation(Chart c) {
        // aim at the plot, not the component centre (title and legend move that), in absolute
        // coordinates: the context is in the parent's paint coordinates. Near the plot's top
        // edge, above the bars, so the first tap selects nothing: a tap selects whatever
        // hitTest finds whatever the tooltip mode, and that highlight would show in every frame
        ChartContext ctx = c.getLastContext();
        int x = c.getAbsoluteX() - c.getX() + ctx.getPlotX() + ctx.getPlotWidth() / 2;
        int y = c.getAbsoluteY() - c.getY() + ctx.getPlotY() + Math.max(1, ctx.getPlotHeight() / 50);
        // two taps at one pinned instant are a double tap
        c.pointerPressed(x, y);
        c.pointerReleased(x, y);
        c.pointerPressed(x, y);
        c.pointerReleased(x, y);
        if (c.getSelection() != null) {
            failAfterCapture("Chart2ZoomAnimation: the tap selected " + c.getSelection().getLabel());
        }
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        super.renderFrame(g, width, height, progress, frameIndex);
        if (progress >= 1 && !getChart().getXAxis().isZoomed()) {
            failAfterCapture("Chart2ZoomAnimation: the double tap did not zoom the x axis");
        }
    }

    @Override
    protected int getAnimationDurationMillis() {
        return 300;
    }

    @Override
    protected int framesThatMustMove() {
        // the zoom runs the whole 300 ms strip: every frame moves
        return 5;
    }
}
