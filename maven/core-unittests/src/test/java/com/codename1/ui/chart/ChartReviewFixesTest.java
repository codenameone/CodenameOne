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
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.XYSeries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unsorted data, stale selections, keyboard gaps, linked charts, log ticks and flings.
public class ChartReviewFixesTest extends UITestBase {

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

    private static void key(Chart c, int game) {
        c.keyReleased(Display.getInstance().getKeyCode(game));
    }

    private static void assertEveryPointHits(XYSeries s, ChartContext ctx) {
        for (int i = 0; i < s.size(); i++) {
            ChartPoint p = s.getPoint(ctx, i);
            ChartPoint hit = s.hitTest(ctx, p.getScreenX(), p.getScreenY(), 3);
            assertNotNull(hit, "point " + i + " of " + s.getName());
            assertEquals(i, hit.getIndex());
        }
    }

    @FormTest
    public void unsortedLineAndBarPointsAreAllHit() {
        LineSeries line = new LineSeries("l");
        line.setData(new double[]{5, 1, 9, 3}, new double[]{1, 2, 3, 4});
        BarSeries bar = new BarSeries("b");
        bar.setData(new double[]{5, 1, 9, 3}, new double[]{4, 3, 2, 1});
        Chart c = new Chart().addSeries(line);
        c.setAnimationDuration(0);
        assertEveryPointHits(line, paint(c));
        // zoomed onto the low x values, which sit at both ends of the array
        c.getXAxis().setVisibleRange(0.5, 5.5);
        ChartContext ctx = paint(c);
        for (int i : new int[]{0, 1, 3}) {
            ChartPoint p = line.getPoint(ctx, i);
            assertNotNull(line.hitTest(ctx, p.getScreenX(), p.getScreenY(), 3), "zoomed point " + i);
        }
        Chart b = new Chart().addSeries(bar);
        b.setAnimationDuration(0);
        ChartContext bctx = paint(b);
        for (int i = 0; i < bar.size(); i++) {
            ChartPoint p = bar.getPoint(bctx, i);
            assertNotNull(bar.hitTest(bctx, p.getScreenX(), p.getScreenY(), 3), "bar " + i);
        }
    }

    @FormTest
    public void selectionFollowsPanAndDiesWithItsSeries() {
        LineSeries s = new LineSeries("l");
        double[] y = new double[50];
        for (int i = 0; i < y.length; i++) {
            y[i] = i % 7;
        }
        s.setData(null, y);
        Chart c = new Chart().addSeries(s).setTooltipMode(Chart.TOOLTIP_POINT);
        c.setAnimationDuration(0);
        paint(c);
        c.getXAxis().setVisibleRange(0, 20);
        paint(c);
        key(c, Display.GAME_RIGHT);
        key(c, Display.GAME_RIGHT);
        ChartPoint sel = c.getSelection();
        assertNotNull(sel);
        int before = sel.getScreenX();
        c.getXAxis().setVisibleRange(0.5, 20.5);
        assertTrue(c.getXAxis().isZoomed());
        ChartContext ctx = paint(c);
        ChartPoint now = c.getSelection();
        assertEquals(s.getPoint(ctx, sel.getIndex()).getScreenX(), now.getScreenX());
        assertTrue(now.getScreenX() < before, "the tooltip moved with the pan");

        s.clear();
        assertNull(c.getSelection());
        s.setData(null, y);
        paint(c);
        key(c, Display.GAME_RIGHT);
        assertNotNull(c.getSelection());
        c.removeSeries(s);
        assertNull(c.getSelection());
    }

    @FormTest
    public void keyboardSkipsGaps() {
        LineSeries s = new LineSeries("l");
        s.setData(null, new double[]{1, Double.NaN, Double.NaN, 3});
        Chart c = new Chart().addSeries(s).setTooltipMode(Chart.TOOLTIP_POINT);
        c.setAnimationDuration(0);
        paint(c);
        key(c, Display.GAME_RIGHT);
        assertEquals(0, c.getSelection().getIndex());
        key(c, Display.GAME_RIGHT);
        assertEquals(3, c.getSelection().getIndex());
        key(c, Display.GAME_LEFT);
        assertEquals(0, c.getSelection().getIndex());
    }

    @FormTest
    public void linkedChartFollowsEveryFrameAndUnsyncStops() {
        AnimationTime.setTime(10000);
        try {
            Chart a = new Chart().addSeries(new LineSeries("a").setData(null, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
            Chart b = new Chart().addSeries(new LineSeries("b").setData(null, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
            a.setAnimationDuration(0);
            b.setAnimationDuration(0);
            a.syncWith(b);
            paint(a);
            paint(b);
            double fullMax = b.getXAxis().getVisibleMax();
            a.keyReleased('+');
            AnimationTime.setTime(10100);
            a.animate();
            double mid = a.getXAxis().getVisibleMax();
            assertTrue(mid < fullMax, "the zoom is under way");
            assertEquals(mid, b.getXAxis().getVisibleMax(), 1e-9);
            AnimationTime.setTime(10500);
            a.animate();
            assertTrue(b.getXAxis().isZoomed());

            a.unsync();
            double bMin = b.getXAxis().getVisibleMin();
            double bMax = b.getXAxis().getVisibleMax();
            a.keyReleased('+');
            AnimationTime.setTime(11000);
            a.animate();
            assertEquals(bMin, b.getXAxis().getVisibleMin(), 1e-9);
            assertEquals(bMax, b.getXAxis().getVisibleMax(), 1e-9);
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void logAxisTicksWithinADecade() {
        LogAxis a = new LogAxis(Axis.LEFT);
        a.setDataBounds(1, 100);
        a.setVisibleRange(2, 8);
        double[] t = a.ticks(6);
        assertTrue(t.length >= 2, "ticks " + t.length);
        for (double v : t) {
            assertTrue(v >= 2 * 0.999 && v <= 8 * 1.001, "tick " + v);
        }
    }

    @FormTest
    public void setDataValidatesBeforeChanging() {
        LineSeries s = new LineSeries("l");
        s.setData(null, new double[]{1, 2});
        assertThrows(IllegalArgumentException.class, () -> s.setData(new double[]{1}, new double[]{1, 2, 3}));
        assertEquals(2, s.size());
    }

    @FormTest
    public void addingToAnotherChartMovesTheSeries() {
        LineSeries s = new LineSeries("l");
        s.setData(null, new double[]{1, 2});
        Chart a = new Chart().addSeries(s);
        Chart b = new Chart().addSeries(s);
        assertTrue(a.getSeries().isEmpty());
        assertEquals(1, b.getSeries().size());
        b.addSeries(s);
        assertEquals(1, b.getSeries().size());
    }

    @FormTest
    public void flingStopsAtTheDataEdge() {
        AnimationTime.setTime(20000);
        try {
            double[] y = new double[100];
            for (int i = 0; i < y.length; i++) {
                y[i] = i % 5;
            }
            Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
            c.setAnimationDuration(0);
            paint(c);
            c.getXAxis().setVisibleRange(0, 20);
            ChartContext ctx = paint(c);
            assertTrue(c.getXAxis().isZoomed());
            int x0 = ctx.getPlotX() + 50;
            int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
            // dragging right shows smaller x values, and the viewport is already at the start
            c.pointerPressed(x0, py);
            AnimationTime.setTime(20010);
            c.pointerDragged(new int[]{x0 + 40}, new int[]{py});
            AnimationTime.setTime(20020);
            c.pointerDragged(new int[]{x0 + 120}, new int[]{py});
            AnimationTime.setTime(20025);
            c.pointerReleased(x0 + 120, py);
            AnimationTime.setTime(20040);
            assertTrue(c.animate(), "the fling ran one frame");
            AnimationTime.setTime(20060);
            assertFalse(c.animate(), "nothing moved, so the fling ended");
        } finally {
            AnimationTime.reset();
        }
    }
}
