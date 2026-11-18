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
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Fifteenth review round of the chart package: one test per finding.
public class ChartReviewRound15Test extends UITestBase {

    private void place(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    @FormTest
    public void infiniteValueIsNeverNearestAndNeverDescribed() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2, 3}, new double[]{1, 2, Double.POSITIVE_INFINITY, 3});
        assertTrue(s.isAscending());
        assertNotEquals(2, s.nearestIndexOfX(2), "the infinite point is a gap");
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        ChartContext ctx = c.getLastContext();
        int mid = (s.screenPosition(ctx, 1, false) + s.screenPosition(ctx, 3, false)) / 2;
        assertNotEquals(2, s.nearestScreenIndex(ctx, mid, false), "the infinite point is a gap on screen too");
        String d = c.describe();
        assertFalse(d.contains("Infinity"), d);
    }

    @FormTest
    public void nonFiniteXIsAGap() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, Double.NaN, 4}, new double[]{1, 2, 3});
        assertFalse(s.isAscending(), "a NaN x cannot be binary searched");
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        place(c, 400, 300);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(400, 300).getGraphics());
            for (Object[] call : implementation.getDrawCalls()) {
                for (Object o : call) {
                    String v = o instanceof float[] ? Arrays.toString((float[]) o) : String.valueOf(o);
                    assertFalse(v.contains("NaN"), "draw call " + Arrays.toString(call));
                }
            }
        } finally {
            implementation.setRecordDrawCalls(false);
        }
        ChartContext ctx = c.getLastContext();
        int sx = s.screenPosition(ctx, 2, false);
        int sy = s.screenPosition(ctx, 2, true);
        ChartPoint p = s.hitTest(ctx, sx, sy, 10);
        assertNotNull(p);
        assertEquals(2, p.getIndex());
        assertNotEquals(1, s.nearestScreenIndex(ctx, sx, false));
    }

    @Test
    public void lttbTreatsInfinityAsAGap() {
        double inf = Double.POSITIVE_INFINITY;
        double[] y = {1, inf, inf, inf, inf, inf, inf, inf, inf, 2};
        int[] out = Downsample.lttb(null, y, 4);
        for (int i : out) {
            assertTrue(i >= 0 && i < y.length, "index " + i + " in " + Arrays.toString(out));
        }
    }

    private Chart zoomable() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, new double[]{1, 4, 2, 8, 5, 7, 3, 9, 6, 2});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        return c;
    }

    private void assertExportKeepsTheScreen(Chart c) {
        Axis x = c.getXAxis();
        double lo = x.getVisibleMin();
        double hi = x.getVisibleMax();
        boolean zoomed = x.isZoomed();
        c.toImage(400, 300);
        assertEquals(lo, x.getVisibleMin(), 0, "the on-screen minimum is restored exactly");
        assertEquals(hi, x.getVisibleMax(), 0, "the on-screen maximum is restored exactly");
        assertEquals(zoomed, x.isZoomed());
    }

    @FormTest
    public void exportMidTweenZoomingInRestoresTheScreenExactly() {
        AnimationTime.setTime(70000);
        try {
            Chart c = zoomable();
            ChartContext ctx = c.getLastContext();
            int px = ctx.getPlotX() + ctx.getPlotWidth() / 2;
            int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
            c.pointerPressed(px, py);
            c.pointerReleased(px, py);
            AnimationTime.setTime(70050);
            c.pointerPressed(px, py);
            c.pointerReleased(px, py);
            AnimationTime.setTime(70150);
            c.animate();
            assertFalse(c.getXAxis().isZoomed(), "positive control: mid zoom-in, not yet zoomed");
            assertTrue(c.getXAxis().getVisibleMin() > c.getXAxis().getDataMin(), "positive control: mid zoom-in");
            assertExportKeepsTheScreen(c);
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void exportMidTweenZoomingOutRestoresTheScreenExactly() {
        AnimationTime.setTime(80000);
        try {
            Chart c = zoomable();
            c.getXAxis().setVisibleRange(2, 4);
            c.paint(Image.createImage(400, 300).getGraphics());
            c.resetZoom();
            AnimationTime.setTime(80299);
            c.animate();
            assertTrue(c.getXAxis().isZoomed(), "positive control: mid zoom-out");
            assertExportKeepsTheScreen(c);
        } finally {
            AnimationTime.reset();
        }
    }
}
