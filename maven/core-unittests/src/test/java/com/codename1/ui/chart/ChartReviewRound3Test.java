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
import com.codename1.ui.plaf.Style;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Third review round of the chart package: one test per finding.
public class ChartReviewRound3Test extends UITestBase {

    private static ChartContext paint(Chart c, int w, int h) {
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        c.paint(Image.createImage(w, h).getGraphics());
        ChartContext ctx = c.getLastContext();
        assertNotNull(ctx);
        return ctx;
    }

    private List<Object[]> drawnStrings(Chart c, int w, int h) {
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c, w, h);
            return new ArrayList<Object[]>(implementation.getDrawnStrings());
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }

    private static boolean contains(List<Object[]> drawn, String s) {
        for (Object[] d : drawn) {
            if (s.equals(d[0])) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    public void radarGridIsDrawnWhenTheFirstRadarIsHidden() {
        RadarSeries a = new RadarSeries("A", "Speed", "Range", "Cost");
        a.setValues(1, 2, 3);
        RadarSeries b = new RadarSeries("B", "Speed", "Range", "Cost");
        b.setValues(3, 2, 1);
        Chart c = new Chart().addSeries(a).addSeries(b);
        a.setVisible(false);
        List<Object[]> drawn = drawnStrings(c, 600, 400);
        assertTrue(contains(drawn, "Speed"), "the spoke labels belong to the grid");
    }

    @FormTest
    public void boxWithNaNIsAGapAndDoesNotPoisonTheAxes() {
        BoxPlotSeries box = new BoxPlotSeries("Box");
        box.addBox(1, Double.NaN, 2, 3, 4, 5);
        box.addBox(2, 1, 2, 3, 4, 6);
        double[] yb = box.getYBounds();
        assertEquals(1, yb[0], 0);
        assertEquals(6, yb[1], 0);
        LineSeries line = new LineSeries("Line");
        line.setData(new double[]{1, 2}, new double[]{2, 3});
        Chart c = new Chart().addSeries(box).addSeries(line);
        paint(c, 600, 400);
        double min = c.getYAxis().getDataMin();
        assertTrue(min == min, "NaN reached the value axis");
    }

    @FormTest
    public void addSamplesReplacesTheBoxAtTheSameX() {
        BoxPlotSeries box = new BoxPlotSeries("Box");
        box.addSamples(1, new double[]{1, 2, 3, 4, 100});
        assertEquals(1, box.size());
        assertEquals(1, box.getOutliers(0).length);
        box.addSamples(1, new double[]{10, 11, 12});
        assertEquals(1, box.size());
        assertEquals(11, box.getY(0), 0);
        assertEquals(0, box.getOutliers(0).length);
        box.addSamples(1, new double[]{1, 2, 3, 4, 100});
        box.addSamples(1, new double[]{Double.NaN, Double.NaN});
        assertEquals(0, box.size());
        box.addSamples(1, new double[]{5});
        assertEquals(0, box.getOutliers(0).length, "a stale outlier survived an all-NaN sample set");
    }

    @FormTest
    public void gaugeBandOutsideTheRangeAndNaNValuePaintCleanly() {
        GaugeSeries g = new GaugeSeries("Speed", 0, 100);
        g.addBand(150, 200, 0xff0000);
        g.addBand(-50, -10, 0x00ff00);
        g.setValue(Double.NaN);
        Chart c = new Chart().addSeries(g);
        List<Object[]> drawn = drawnStrings(c, 400, 300);
        assertTrue(contains(drawn, "-"), "a NaN reading shows as a dash");
        for (Object[] d : drawn) {
            assertFalse(String.valueOf(d[0]).contains("NaN"), "NaN text drawn: " + d[0]);
        }
        g.setNeedle(true);
        drawn = drawnStrings(c, 400, 300);
        assertTrue(contains(drawn, "-"));
    }

    @FormTest
    public void needleGaugeTextStaysInsideAShortChart() {
        GaugeSeries g = new GaugeSeries("Pressure", 0, 100);
        g.setNeedle(true);
        g.addBand(0, 50, 0x00ff00);
        g.addBand(50, 100, 0xff0000);
        g.setValue(42);
        Chart c = new Chart().addSeries(g);
        int h = 160;
        List<Object[]> drawn = drawnStrings(c, 600, h);
        boolean sawName = false;
        for (Object[] d : drawn) {
            String s = (String) d[0];
            if ("Pressure".equals(s)) {
                sawName = true;
                int y = ((Integer) d[2]).intValue();
                assertTrue(y + implementation.getHeight(d[3]) <= h, "name drawn below the chart at " + y);
            }
        }
        assertTrue(sawName);
    }

    @FormTest
    public void sparklineThemeFollowsAThemeOrStyleChange() {
        Sparkline s = new Sparkline(1, 2, 3);
        ChartTheme first = s.currentTheme();
        assertSame(first, s.currentTheme());
        s.refreshTheme(false);
        ChartTheme second = s.currentTheme();
        assertNotSame(first, second);
        s.styleChanged(Style.FG_COLOR, s.getStyle());
        assertNotSame(second, s.currentTheme());
    }

    @FormTest
    public void fadingBarSeriesAndItsNeighbourAgreeOnTheSlots() {
        BarSeries a = new BarSeries("A");
        a.setData(null, new double[]{10});
        BarSeries b = new BarSeries("B");
        b.setData(null, new double[]{10});
        Chart c = new Chart().addSeries(a).addSeries(b);
        ChartContext ctx = paint(c, 600, 400);
        a.setVisible(false);
        ctx = paint(c, 600, 400);
        // half way through A fading out
        ((Series) a).visibility = 0.5f;
        int y = ctx.getPlotY() + ctx.getPlotHeight() / 2 - ctx.getOriginY();
        int lastA = -1;
        int firstB = -1;
        int both = 0;
        for (int x = ctx.getPlotX(); x < ctx.getPlotX() + ctx.getPlotWidth(); x++) {
            int lx = x - ctx.getOriginX();
            boolean hitA = a.hitTest(ctx, lx, y, 0) != null;
            boolean hitB = b.hitTest(ctx, lx, y, 0) != null;
            if (hitA && hitB) {
                both++;
            }
            if (hitA) {
                lastA = x;
            }
            if (hitB && firstB < 0) {
                firstB = x;
            }
        }
        // hit tests are inclusive of both edges, so two touching bars share at most one column
        assertTrue(both <= 1, "the fading bar and its neighbour overlap over " + both + " columns");
        assertTrue(lastA >= 0 && firstB >= lastA, "A ends at " + lastA + ", B starts at " + firstB);
    }

    @FormTest
    public void funnelOnAShortChartDoesNotDrawNegativeBands() {
        String[] names = new String[30];
        double[] vals = new double[30];
        for (int i = 0; i < 30; i++) {
            names[i] = "Stage " + i;
            vals[i] = 100 - i;
        }
        FunnelSeries f = new FunnelSeries("Funnel");
        f.setData(names, vals);
        Chart c = new Chart().addSeries(f);
        ChartContext ctx = paint(c, 300, 40);
        assertNull(f.hitTest(ctx, 150, -50, 0));
    }

    @FormTest
    public void sankeyLayoutFollowsDataAndGeometry() {
        SankeySeries s = new SankeySeries("Flow");
        s.addLink("A", "B", 5);
        Chart c = new Chart().addSeries(s);
        ChartContext ctx = paint(c, 600, 400);
        assertEquals(3, s.getPointCount());
        s.addLink("B", "C", 3);
        ctx = paint(c, 600, 400);
        assertNotNull(s.getPoint(ctx, 2));
        // a resize rebuilds the layout: node C still lies inside the smaller chart
        ctx = paint(c, 300, 200);
        ChartPoint p = s.getPoint(ctx, 2);
        assertTrue(p.getScreenX() + ctx.getOriginX() <= 300, "stale layout after a resize: " + p.getScreenX());
    }

    @Test
    public void lttbKeepsGapsAndIgnoresNaNInTheMath() {
        int n = 1000;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = Math.sin(i / 20.0) * 10;
        }
        for (int i = 400; i < 420; i++) {
            y[i] = Double.NaN;
        }
        y[0] = Double.NaN;
        int[] keep = Downsample.lttb(null, y, 100);
        // a bucket with a gap keeps the gap marker and its best real point, so a few extra
        assertTrue(keep.length >= 100 && keep.length <= 198, "length " + keep.length);
        boolean gap = false;
        int real = 0;
        for (int i = 0; i < keep.length; i++) {
            if (i > 0) {
                assertTrue(keep[i] > keep[i - 1], "indices must ascend");
            }
            if (keep[i] >= 400 && keep[i] < 420) {
                gap = true;
            }
            if (y[keep[i]] == y[keep[i]]) {
                real++;
            }
        }
        assertTrue(gap, "the NaN run was bridged");
        assertTrue(real > 90, "NaN poisoned the triangle math: only " + real + " real points kept");
    }
}
