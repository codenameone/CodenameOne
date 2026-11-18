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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.layout.SankeyLayout;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.VolumeSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The data model of the series: adds, slides, gaps and live data.
public class ChartDataModelTest extends UITestBase {

    private static int[] render(Chart c) {
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(300);
        c.setHeight(300);
        Image img = Image.createImage(300, 300, 0xffffffff);
        c.paint(img.getGraphics());
        return img.getRGB();
    }

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

    @FormTest
    public void addGivesExtraChannelsADefaultAndSlideForgetsTheDroppedPoint() {
        RangeBarSeries r = new RangeBarSeries("r");
        r.addRange(0, 5, 9);
        r.add(1, 4);
        assertEquals(0, r.getStart(1), 1e-9);
        assertEquals(4, r.getEnd(1), 1e-9);
        ErrorBarSeries e = new ErrorBarSeries("e");
        e.setIntervals(null, new double[]{1}, new double[]{0}, new double[]{3});
        e.setMaxPoints(1);
        e.add(1, 7);
        assertEquals(7, e.getLow(0), 1e-9);
        assertEquals(7, e.getHigh(0), 1e-9);
    }

    @FormTest
    public void aSlidingSeriesAnimatesByXAndAnAppendedPointGrowsFromTheLast() {
        LineSeries s = new LineSeries("l");
        s.setData(null, new double[]{10, 20, 30});
        s.setMaxPoints(3);
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        c.setAnimationDuration(1000000);
        s.add(3, 40);
        ChartContext ctx = paint(c);
        assertTrue(ctx.getProgress() < 0.01f);
        // x=1 kept its value 20 although it moved from index 1 to 0
        assertEquals(20, s.animatedY(ctx, 0), 0.5);
        assertEquals(30, s.animatedY(ctx, 1), 0.5);
        // the new point starts at the previous last point's value, not at 0
        assertEquals(30, s.animatedY(ctx, 2), 0.5);
    }

    @FormTest
    public void nanAndNegativeTreeValuesCountAsNothing() {
        TreeNode root = new TreeNode("root");
        root.add(new TreeNode("a", 4));
        root.add(new TreeNode("b", Double.NaN));
        root.add(new TreeNode("c", -3));
        assertEquals(4, root.getValue(), 1e-9);
        SankeyLayout lay = new SankeyLayout(3, new int[]{0, 1}, new int[]{1, 2}, new double[]{Double.NaN, 5},
                0, 0, 300, 200, 10, 4);
        assertEquals(0, lay.getLinkWidth(0), 1e-6);
        assertTrue(lay.getLinkWidth(1) > 0);
        assertFalse(Float.isNaN(lay.getNodeY(2)));
    }

    @FormTest
    public void aNaNRadarValueDoesNotBlankTheChart() {
        RadarSeries a = new RadarSeries("a", "x", "y", "z").setValues(3, Double.NaN, 5);
        RadarSeries b = new RadarSeries("b", "x", "y", "z").setValues(3, 0, 5);
        assertArrayEquals(render(new Chart().addSeries(b)), render(new Chart().addSeries(a)));
    }

    @FormTest
    public void waterfallSkipsNaNAndRecomputesOnEveryChange() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setSteps(new double[]{10, Double.NaN, 5, 0}, new boolean[]{false, false, false, true});
        assertEquals(10, w.getBase(2), 1e-9);
        assertEquals(15, w.getY(3), 1e-9);
        double[] b = w.getYBounds();
        assertEquals(0, b[0], 1e-9);
        assertEquals(15, b[1], 1e-9);
        w.set(0, 20);
        assertEquals(20, w.getBase(2), 1e-9);
        assertEquals(25, w.getY(3), 1e-9);
    }

    @FormTest
    public void indicatorsRecoverFromAGapAndValidateThePeriod() {
        double[] v = {1, 2, Double.NaN, 4, 5, 6};
        double[] sma = Indicators.sma(v, 2);
        assertTrue(Double.isNaN(sma[2]));
        assertTrue(Double.isNaN(sma[3]));
        assertEquals(4.5, sma[4], 1e-9);
        double[][] bb = Indicators.bollinger(v, 2, 2);
        assertEquals(5.5, bb[0][5], 1e-9);
        assertFalse(Double.isNaN(bb[1][5]));
        double[] rsi = Indicators.rsi(new double[]{1, 2, 3, Double.NaN, 4, 5, 6}, 2);
        assertTrue(Double.isNaN(rsi[4]));
        assertTrue(Double.isNaN(rsi[5]));
        assertEquals(100, rsi[6], 1e-9);
        assertEquals(50, Indicators.rsi(new double[]{3, 3, 3, 3}, 2)[3], 1e-9);
        assertTrue(Double.isNaN(Indicators.ema(new double[]{1, 1, Double.NaN, 1}, 2)[2]));
        assertThrows(IllegalArgumentException.class, () -> Indicators.sma(v, 0));
        assertThrows(IllegalArgumentException.class, () -> Indicators.rsi(v, -1));
    }

    @FormTest
    public void heikinAshiFollowsEveryChange() {
        CandlestickSeries c = new CandlestickSeries("c");
        c.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        c.setStyle(CandlestickSeries.HEIKIN_ASHI);
        Chart chart = new Chart().addSeries(c);
        int[] before = render(chart);
        c.updateLast(10);
        assertFalse(java.util.Arrays.equals(before, render(chart)));
    }

    @FormTest
    public void sessionAxisFollowsALiveSeries() {
        SessionTimeAxis a = new SessionTimeAxis(Axis.BOTTOM, new long[]{1000, 2000, 3000});
        a.setMaxTimes(3);
        a.addTime(4000);
        assertEquals(1, a.getFirstIndex());
        assertEquals(2000, a.timeAt(1));
        assertEquals(4000, a.timeAt(3));
        LineSeries s = new LineSeries("s");
        s.setData(null, new double[]{1, 2, 3});
        s.setMaxPoints(3);
        s.add(4);
        assertEquals(1, s.getX(0), 1e-9);
        assertEquals(a.timeAt(s.getX(0)), 2000);
    }

    @FormTest
    public void volumeMatchesCandlesByX() {
        CandlestickSeries p = new CandlestickSeries("p");
        p.setOhlc(new double[]{5, 6}, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        VolumeSeries v = new VolumeSeries("v", p, new double[]{100, 200});
        assertEquals(6, v.getX(1), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> new VolumeSeries("v", p, new double[]{1}));
    }

    @FormTest
    public void setMaxPointsTrimsAndSetChecksTheIndex() {
        LineSeries s = new LineSeries("s");
        s.setValues(1, 2, 3, 4, 5);
        s.setMaxPoints(2);
        assertEquals(2, s.size());
        assertEquals(4, s.getY(0), 1e-9);
        assertThrows(IndexOutOfBoundsException.class, () -> s.set(2, 1));
    }

    @FormTest
    public void heatmapIdsDoNotAliasAndAnEmptyMatrixHasARange() {
        HeatmapSeries h = new HeatmapSeries("h");
        h.setMatrix(new double[][]{new double[70000], new double[70000]});
        assertTrue(h.cellId(1, 0) != h.cellId(0, 65536));
        h.setMatrix(new double[][]{{Double.NaN, Double.NaN}});
        double[] r = h.getValueRange();
        assertTrue(r[0] < r[1]);
        assertEquals(0, r[0], 1e-9);
    }

    @FormTest
    public void sankeyCyclesAreBackLinksAndBadNodesAreRejected() {
        SankeyLayout lay = new SankeyLayout(3, new int[]{0, 1, 2}, new int[]{1, 2, 0}, new double[]{5, 5, 5},
                0, 0, 300, 200, 10, 4);
        assertTrue(lay.isBackLink(2));
        assertFalse(lay.isBackLink(0));
        assertEquals(3, lay.getColumns());
        assertThrows(IllegalArgumentException.class, () -> new SankeyLayout(2, new int[]{0}, new int[]{5},
                new double[]{1}, 0, 0, 300, 200, 10, 4));
        SankeySeries s = new SankeySeries("s").addLink("a", "b", 3).addLink("b", "a", 1);
        render(new Chart().addSeries(s));
    }

    @FormTest
    public void boxPlotSamplesGetTukeyWhiskersAndOutliers() {
        BoxPlotSeries b = new BoxPlotSeries("b");
        b.addSamples(0, new double[]{1, 2, 3, 4, 5, 100});
        assertEquals(5, b.getMax(0), 1e-9);
        assertEquals(1, b.getMin(0), 1e-9);
        assertArrayEquals(new double[]{100}, b.getOutliers(0), 1e-9);
        b.addBox(1, 0, 1, 2, 3, 50);
        assertEquals(50, b.getMax(1), 1e-9);
        assertEquals(0, b.getOutliers(1).length);
    }

    @FormTest
    public void sparklineBreaksAtGaps() {
        Sparkline s = new Sparkline(1, 2, Double.NaN, 3, 4);
        s.setX(0);
        s.setY(0);
        s.setWidth(100);
        s.setHeight(30);
        s.paint(Image.createImage(100, 30).getGraphics());
        assertEquals(2, s.lastSegments);
    }

    @FormTest
    public void funnelIgnoresNaNAndNegativeStages() {
        FunnelSeries f = new FunnelSeries("f");
        f.setData(new String[]{"a", "b", "c"}, new double[]{10, Double.NaN, -4});
        FunnelSeries g = new FunnelSeries("f");
        g.setData(new String[]{"a", "b", "c"}, new double[]{10, 0, 0});
        assertArrayEquals(render(new Chart().addSeries(g)), render(new Chart().addSeries(f)));
    }
}
