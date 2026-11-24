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
package com.codename1.ui.chart;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A double tap zooms with an animation, and every frame of it has to reach the screen. The
/// frames used to be set in `animate()` and thrown away by the next paint, which recomputes
/// each axis's bounds from the data: the chart sat still for the whole animation and jumped
/// to the zoomed range on the last frame.
public class ChartViewportAnimationTest extends UITestBase {
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static ChartContext paint(Chart c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(600);
        c.setHeight(400);
        c.paint(Image.createImage(600, 400).getGraphics());
        ChartContext ctx = c.getLastContext();
        assertNotNull(ctx);
        return ctx;
    }

    private static Chart sales() {
        Chart c = new Chart().setXAxis(new CategoryAxis(Axis.BOTTOM, MONTHS));
        c.addSeries(new BarSeries("Sales").setData(null,
                new double[]{42, 55, 48, 63, 71, 66, 80, 77, 69, 85, 92, 88}));
        c.setAnimationDuration(0);
        c.setTooltipMode(Chart.TOOLTIP_NONE);
        return c;
    }

    private static void doubleTap(Chart c, ChartContext ctx) {
        int x = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        int y = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        c.pointerPressed(x, y);
        c.pointerReleased(x, y);
        c.pointerPressed(x, y);
        c.pointerReleased(x, y);
    }

    @FormTest
    public void everyFrameOfADoubleTapZoomIsPainted() {
        AnimationTime.setTime(50000);
        try {
            Chart c = sales();
            ChartContext ctx = paint(c);
            double fullMin = c.getXAxis().getVisibleMin();
            double fullMax = c.getXAxis().getVisibleMax();
            doubleTap(c, ctx);
            double lastSpan = fullMax - fullMin;
            for (int t = 60; t < 300; t += 60) {
                AnimationTime.setTime(50000 + t);
                c.animate();
                paint(c);
                double span = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
                assertTrue(span < lastSpan - 1e-6, "frame at " + t + "ms narrowed the x range: "
                        + span + " after " + lastSpan);
                lastSpan = span;
            }
            AnimationTime.setTime(50400);
            c.animate();
            paint(c);
            assertTrue(c.getXAxis().isZoomed());
            double end = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
            assertTrue(end <= lastSpan + 1e-6, "the last frame continues from the one before it");
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void aLinkedChartFollowsEveryFrame() {
        AnimationTime.setTime(70000);
        try {
            Chart a = sales();
            Chart b = sales();
            a.syncWith(b);
            ChartContext ctx = paint(a);
            paint(b);
            double fullSpan = b.getXAxis().getVisibleMax() - b.getXAxis().getVisibleMin();
            doubleTap(a, ctx);
            AnimationTime.setTime(70150);
            a.animate();
            paint(a);
            paint(b);
            double span = b.getXAxis().getVisibleMax() - b.getXAxis().getVisibleMin();
            assertTrue(span < fullSpan - 1e-6, "the linked chart shows the frame too: " + span);
        } finally {
            AnimationTime.reset();
        }
    }
}
