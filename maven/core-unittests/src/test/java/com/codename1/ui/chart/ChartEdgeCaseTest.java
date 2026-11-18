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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.axis.Ticks;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.XYSeries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Edge cases a review found: bad data, odd bounds and gestures that change course.
public class ChartEdgeCaseTest extends UITestBase {

    private static ChartContext paint(Chart c, int w, int h) {
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        c.paint(Image.createImage(w, h).getGraphics());
        return c.getLastContext();
    }

    @FormTest
    public void aNaNSliceDoesNotEmptyThePie() {
        PieSeries p = new PieSeries("p");
        p.setData(new String[]{"a", "b", "c"}, new double[]{5, Double.NaN, 3});
        Chart c = new Chart().addSeries(p);
        ChartContext ctx = paint(c, 600, 400);
        int cx = ctx.getPlotX() + ctx.getPlotWidth() / 2 - ctx.getOriginX();
        int cy = ctx.getPlotY() + ctx.getPlotHeight() / 2 - ctx.getOriginY();
        assertNotNull(c.hitTest(cx + ctx.getPlotHeight() / 5, cy));
        assertEquals(8, p.getTotal(), 1e-9);
    }

    @FormTest
    public void boxPlotIgnoresNaNAndEmptyQuantileIsNaN() {
        assertTrue(Double.isNaN(BoxPlotSeries.quantile(new double[0], 0.5)));
        BoxPlotSeries b = new BoxPlotSeries("b");
        b.addSamples(0, new double[]{1, 2, Double.NaN, 3});
        Chart c = new Chart().addSeries(b);
        paint(c, 400, 300);
        assertTrue(c.getYAxis().getDataMax() >= 3);
        assertFalse(Double.isNaN(c.getYAxis().getDataMax()));
    }

    @FormTest
    public void heikinAshiRecoversAfterAGap() {
        double[] o = {10, 11, Double.NaN, 12, 13};
        double[] h = {12, 13, Double.NaN, 14, 15};
        double[] l = {9, 10, Double.NaN, 11, 12};
        double[] cl = {11, 12, Double.NaN, 13, 14};
        CandlestickSeries s = new CandlestickSeries("c").setOhlc(null, o, h, l, cl);
        double[][] ha = s.heikinAshi();
        assertTrue(Double.isNaN(ha[3][2]));
        assertFalse(Double.isNaN(ha[0][3]));
        assertFalse(Double.isNaN(ha[0][4]));
    }

    @FormTest
    public void identicalSamplesMakeOneBar() {
        double[] v = new double[100];
        java.util.Arrays.fill(v, 5);
        HistogramSeries h = new HistogramSeries("h").setSamples(v, 0);
        assertEquals(1, h.size());
        assertEquals(100, h.getY(0), 0);
    }

    @FormTest
    public void mixedSignsStackFromZero() {
        BarSeries a = new BarSeries("a");
        a.setStackGroup("s");
        a.setData(null, new double[]{10});
        BarSeries b = new BarSeries("b");
        b.setStackGroup("s");
        b.setData(null, new double[]{-4});
        BarSeries c2 = new BarSeries("c");
        c2.setStackGroup("s");
        c2.setData(null, new double[]{5});
        new Chart().addSeries(a).addSeries(b).addSeries(c2);
        assertEquals(0, b.getBase(0), 0);
        assertEquals(10, c2.getBase(0), 0);
    }

    @FormTest
    public void aFixedBoundBeyondTheDataKeepsTheAxisUpright() {
        NumberAxis a = new NumberAxis(Axis.LEFT);
        a.setRange(Double.valueOf(100), null);
        a.setDataBounds(0, 50);
        assertEquals(100, a.getDataMin(), 0);
        assertTrue(a.getDataMax() > a.getDataMin());
        NumberAxis b = new NumberAxis(Axis.LEFT);
        b.setRange(null, Double.valueOf(-10));
        b.setDataBounds(0, 50);
        assertEquals(-10, b.getDataMax(), 0);
        assertTrue(b.getDataMax() > b.getDataMin());
    }

    @FormTest
    public void aSpanPastTheDoubleRangeStillTicks() {
        double[] t = Ticks.linear(-Double.MAX_VALUE, Double.MAX_VALUE, 6);
        assertTrue(t.length <= 6 && t.length >= 2);
    }

    @FormTest
    public void aSecondFingerEndsTheBrush() {
        double[] y = new double[50];
        for (int i = 0; i < y.length; i++) {
            y[i] = i % 5;
        }
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        c.setBrushEnabled(true);
        ChartContext ctx = paint(c, 600, 400);
        final int[] calls = new int[1];
        c.addRangeListener((chart, from, to) -> calls[0]++);
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        int x0 = ctx.getPlotX() + 50;
        c.pointerPressed(x0, py);
        c.pointerDragged(new int[]{x0 + 40}, new int[]{py});
        c.pointerDragged(new int[]{x0 + 120}, new int[]{py});
        c.pointerDragged(new int[]{x0 + 120, x0 + 200}, new int[]{py, py});
        c.pointerDragged(new int[]{x0 + 100, x0 + 260}, new int[]{py, py});
        c.pointerReleased(x0 + 260, py);
        assertEquals(0, calls[0]);
    }

    @FormTest
    public void oneFingerOverTheSameGeometryBrushes() {
        // positive control for aSecondFingerEndsTheBrush: the same drag with one finger
        double[] y = new double[50];
        for (int i = 0; i < y.length; i++) {
            y[i] = i % 5;
        }
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        c.setBrushEnabled(true);
        ChartContext ctx = paint(c, 600, 400);
        final int[] calls = new int[1];
        c.addRangeListener((chart, from, to) -> calls[0]++);
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        int x0 = ctx.getPlotX() + 50;
        c.pointerPressed(x0, py);
        c.pointerDragged(new int[]{x0 + 40}, new int[]{py});
        c.pointerDragged(new int[]{x0 + 120}, new int[]{py});
        c.pointerDragged(new int[]{x0 + 200}, new int[]{py});
        c.pointerDragged(new int[]{x0 + 260}, new int[]{py});
        c.pointerReleased(x0 + 260, py);
        assertEquals(1, calls[0]);
    }

    @FormTest
    public void aPressStopsAZoomAnimation() {
        double[] y = new double[50];
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        ChartContext ctx = paint(c, 600, 400);
        int px = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        // a double tap starts an animated zoom
        c.pointerPressed(px, py);
        c.pointerReleased(px, py);
        c.pointerPressed(px, py);
        c.pointerReleased(px, py);
        double startMin = c.getXAxis().getVisibleMin();
        double startMax = c.getXAxis().getVisibleMax();
        long until = System.currentTimeMillis() + 2000;
        boolean moved = false;
        while (!moved && System.currentTimeMillis() < until) {
            c.animate();
            moved = c.getXAxis().getVisibleMin() != startMin || c.getXAxis().getVisibleMax() != startMax;
        }
        assertTrue(moved, "the double tap should have started a zoom");
        c.pointerPressed(px, py);
        double min = c.getXAxis().getVisibleMin();
        double max = c.getXAxis().getVisibleMax();
        c.animate();
        assertEquals(min, c.getXAxis().getVisibleMin(), 1e-9);
        assertEquals(max, c.getXAxis().getVisibleMax(), 1e-9);
        c.pointerReleased(px, py);
    }

    @FormTest
    public void aShortChartWithTitleAndLegendPaintsNothingBroken() {
        Chart c = new Chart().setTitle("A title").addSeries(new LineSeries("one").setData(null, new double[]{1, 2}))
                .addSeries(new LineSeries("two").setData(null, new double[]{2, 1}));
        ChartContext ctx = paint(c, 300, 30);
        assertTrue(ctx.getPlotWidth() >= 0 && ctx.getPlotHeight() >= 0);
        c.pointerPressed(10, 10);
        c.pointerReleased(10, 10);
        c.pointerPressed(10, 10);
        c.pointerReleased(10, 10);
        assertFalse(Double.isNaN(c.getXAxis().getVisibleMin()));
    }

    @FormTest
    public void nearestIndexOfXPicksTheClosest() {
        XYSeries s = new LineSeries("l").setData(new double[]{0, 10, 20, 30}, new double[]{1, 1, 1, 1});
        assertEquals(0, s.nearestIndexOfX(-5));
        assertEquals(1, s.nearestIndexOfX(14));
        assertEquals(2, s.nearestIndexOfX(16));
        assertEquals(3, s.nearestIndexOfX(99));
    }

    @FormTest
    public void unsortedOrGappySeriesStillFindTheirPoints() {
        XYSeries scatter = new LineSeries("s").setData(new double[]{5, 1, 9, 3}, new double[]{1, 2, 3, 4});
        assertFalse(scatter.isAscending());
        XYSeries gaps = new LineSeries("g").setData(new double[]{0, 1, 2, 3}, new double[]{1, Double.NaN, Double.NaN, 4});
        assertTrue(gaps.isAscending());
        assertEquals(0, gaps.nearestIndexOfX(1.2));
        assertEquals(3, gaps.nearestIndexOfX(2.2));
        gaps.add(-1, 0);
        assertFalse(gaps.isAscending());
    }

    @FormTest
    public void bothFixedBoundsReversedAreTakenAsARange() {
        NumberAxis a = new NumberAxis(Axis.LEFT);
        a.setRange(Double.valueOf(10), Double.valueOf(5));
        a.setDataBounds(0, 50);
        assertEquals(5, a.getDataMin(), 0);
        assertEquals(10, a.getDataMax(), 0);
    }
}
