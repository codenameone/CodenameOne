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
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Fifth review round of the chart package: one test per finding.
public class ChartReviewRound5Test extends UITestBase {

    private List<Object[]> calls(Chart c, int w, int h) {
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(w, h).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private static int v(Object[] call, int k) {
        return ((Integer) call[k]).intValue();
    }

    @Test
    public void bubbleSizesSlideWithTheirPoints() {
        ScatterSeries s = new ScatterSeries("s");
        s.setMaxPoints(3);
        for (int i = 0; i < 5; i++) {
            s.add(i, i, 10 * i);
        }
        assertEquals(3, s.size());
        assertEquals(2, s.getX(0), 0);
        assertEquals(20, s.getSize(0), 0);
        assertEquals(40, s.getSize(2), 0);
        s.add(9, 9);
        assertTrue(Double.isNaN(s.getSize(2)), "a point added without a size has none");
        assertEquals(30, s.getSize(0), 0);
        s.setSizes(1, 2);
        assertEquals(2, s.getSize(1), 0);
        assertTrue(Double.isNaN(s.getSize(2)), "a point past the sizes array has no size");
    }

    @FormTest
    public void nanBubbleSizeDoesNotCollapseTheOthers() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 2, 3}, new double[]{1, 2, 3}, new double[]{Double.NaN, 100, 1});
        Chart c = new Chart().addSeries(s);
        int max = 0;
        int arcs = 0;
        for (Object[] call : calls(c, 600, 400)) {
            if ("fillArc".equals(call[0])) {
                arcs++;
                max = Math.max(max, v(call, 3));
            }
        }
        assertTrue(arcs >= 3, "arcs " + arcs);
        assertTrue(max > Chart.mm(5), "the largest bubble collapsed to " + max);
    }

    // a NaN maps to pixel 0, above the plot: no whisker (vertical line) or candle-sized rect may
    // start there
    private static void assertInsidePlot(List<Object[]> calls, ChartContext ctx, String op, boolean endIsPoint) {
        int top = ctx.getOriginY() - Chart.mm(1);
        int seen = 0;
        for (Object[] call : calls) {
            if (!op.equals(call[0])) {
                continue;
            }
            boolean mark = endIsPoint ? v(call, 1) == v(call, 3) : v(call, 3) < ctx.getPlotWidth() / 4;
            if (mark) {
                seen++;
                int y1 = endIsPoint ? v(call, 4) : v(call, 2) + v(call, 4);
                assertTrue(Math.min(v(call, 2), y1) >= top, op + " reaches above the plot: " + v(call, 2) + ".." + y1 + " top " + top);
            }
        }
        assertTrue(seen > 0, "nothing of the series was drawn");
    }

    @FormTest
    public void errorBarWithNaNEndDrawsNoWhiskerToZero() {
        ErrorBarSeries s = new ErrorBarSeries("e");
        s.setIntervals(null, new double[]{50, 60, 70}, new double[]{Double.NaN, 55, 65}, new double[]{55, Double.NaN, 75});
        Chart c = new Chart().addSeries(s);
        List<Object[]> drawn = calls(c, 600, 400);
        assertInsidePlot(drawn, c.getLastContext(), "drawLine", true);
    }

    @FormTest
    public void errorBarCapIsBounded() {
        ErrorBarSeries s = new ErrorBarSeries("e");
        s.setIntervals(null, new double[]{5, 6}, new double[]{4, 5}, new double[]{6, 7});
        Chart c = new Chart().addSeries(s);
        // two points across 2000 pixels: 15% of the spacing would be ~150px
        int caps = 0;
        for (Object[] call : calls(c, 2000, 400)) {
            if ("drawLine".equals(call[0]) && v(call, 2) == v(call, 4)) {
                int len = Math.abs(v(call, 3) - v(call, 1));
                if (len > Chart.mm(4) && len < 300) {
                    caps++;
                }
            }
        }
        assertEquals(0, caps, "a cap wider than 4mm");
    }

    @FormTest
    public void candleWithNaNIsAGap() {
        CandlestickSeries s = new CandlestickSeries("c");
        s.setOhlc(null, new double[]{10, Double.NaN, 12}, new double[]{15, 16, Double.NaN},
                new double[]{8, 9, 10}, new double[]{12, 13, 14});
        Chart c = new Chart().addSeries(s);
        assertInsidePlot(calls(c, 600, 400), c.getLastContext(), "fillRect", false);
        s.setStyle(CandlestickSeries.HEIKIN_ASHI);
        double[][] ha = s.heikinAshi();
        assertTrue(Double.isNaN(ha[3][1]) && Double.isNaN(ha[0][2]));
        assertInsidePlot(calls(c, 600, 400), c.getLastContext(), "fillRect", false);
    }

    @FormTest
    public void heikinAshiHitTestUsesTheAveragedRange() {
        CandlestickSeries s = new CandlestickSeries("c");
        // the second candle's HA open is the first one's midpoint, 100, far above its raw high
        s.setOhlc(null, new double[]{100, 10}, new double[]{100, 11}, new double[]{100, 9}, new double[]{100, 10});
        s.setStyle(CandlestickSeries.HEIKIN_ASHI);
        Chart c = new Chart().addSeries(s);
        calls(c, 600, 400);
        ChartContext ctx = c.getLastContext();
        double haHigh = s.heikinAshi()[1][1];
        assertEquals(100, haHigh, 1e-9);
        int x = Math.round(ctx.screenX(s, 1, 10)) - ctx.getOriginX();
        int y = Math.round(ctx.screenY(s, 1, 80)) - ctx.getOriginY();
        ChartPoint p = s.hitTest(ctx, x, y, 2);
        assertNotNull(p, "the drawn HA candle was not hit");
        assertEquals(1, p.getIndex());
        s.setStyle(CandlestickSeries.CANDLES);
        assertNull(s.hitTest(ctx, x, y, 2), "a raw candle does not reach 80");
    }

    @Test
    public void lttbKeepsThePeakOfABucketWithASparseNaN() {
        int n = 1000;
        double[] y = new double[n];
        y[500] = 100;
        y[501] = Double.NaN;
        int[] keep = Downsample.lttb(null, y, 50);
        boolean peak = false;
        boolean gap = false;
        for (int i = 0; i < keep.length; i++) {
            if (i > 0) {
                assertTrue(keep[i] > keep[i - 1]);
            }
            peak |= keep[i] == 500;
            gap |= keep[i] == 501;
        }
        assertTrue(peak, "the peak was dropped for the NaN beside it");
        assertTrue(gap, "the gap was bridged");
        assertTrue(keep.length <= 2 * 50 - 2);
        double[] noGap = new double[n];
        assertEquals(50, Downsample.lttb(null, noGap, 50).length);
    }

    @FormTest
    public void zeroPixelDashesTerminate() {
        implementation.setDipScale(0.1f);
        LineSeries l = new LineSeries("l");
        l.setData(null, new double[]{1, 3, 2});
        l.setDashed(true);
        Chart c = new Chart().addSeries(l);
        c.addAnnotation(Annotation.valueLine(2, "two").setDashed(true));
        c.addAnnotation(Annotation.xLine(1, "one").setDashed(true));
        assertTrue(calls(c, 600, 400).size() > 0);
    }

    @FormTest
    public void aNullTooltipRowIsLeftOut() {
        Chart c = new Chart().setTooltipMode(Chart.TOOLTIP_SHARED);
        c.addSeries(new LineSeries("A").setData(null, new double[]{1, 5, 2}));
        c.addSeries(new LineSeries("B").setData(null, new double[]{2, 3, 4}));
        c.setTooltipRenderer(new TooltipRenderer() {
            public String formatHeader(ChartPoint first) {
                return null;
            }

            public String formatRow(ChartPoint point) {
                return "B".equals(point.getSeries().getName()) ? null : "row-" + point.getSeries().getName();
            }
        });
        calls(c, 600, 400);
        ChartContext ctx = c.getLastContext();
        c.pointerHover(new int[]{ctx.getPlotX() + ctx.getPlotWidth() / 2}, new int[]{ctx.getPlotY() + 5});
        implementation.setRecordDrawnStrings(true);
        List<String> texts = new ArrayList<String>();
        try {
            c.paint(Image.createImage(600, 400).getGraphics());
            for (Object[] d : implementation.getDrawnStrings()) {
                texts.add((String) d[0]);
            }
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
        assertTrue(texts.contains("row-A"), texts.toString());
        assertFalse(texts.contains("null"), texts.toString());
    }

    @FormTest
    public void scatterWithNullSizesIsPlainScatter() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(null, new double[]{1, 2, 3}, null);
        assertEquals(3, s.size());
        // plain points: every dot the same size, as with no sizes at all
        ScatterSeries plain = new ScatterSeries("s");
        plain.setData(null, new double[]{1, 2, 3});
        List<String> want = new ArrayList<String>();
        for (Object[] o : calls(new Chart().addSeries(plain), 400, 300)) {
            want.add(java.util.Arrays.toString(o));
        }
        List<String> got = new ArrayList<String>();
        for (Object[] o : calls(new Chart().addSeries(s), 400, 300)) {
            got.add(java.util.Arrays.toString(o));
        }
        assertEquals(want, got);
    }

    @FormTest
    public void rangeBarEndIsExactlyWhatWasGiven() {
        RangeBarSeries r = new RangeBarSeries("r");
        r.setRanges(null, new double[]{0.1, -2}, new double[]{0.4, 7.3});
        assertEquals(0.4, r.getEnd(0), 0);
        assertEquals(7.3, r.getEnd(1), 0);
        assertEquals(0.1, r.getStart(0), 0);
        r.addRange(2, 0.1, 0.4);
        assertEquals(0.4, r.getEnd(2), 0);
        r.set(0, 1);
        assertEquals(1.1, r.getEnd(0), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> r.setRanges(null, new double[]{1}, new double[]{2, 3}));
        assertThrows(IllegalArgumentException.class,
                () -> r.setRanges(new double[]{1}, new double[]{1, 2}, new double[]{2, 3}));
        assertEquals(3, r.size());
    }

    @FormTest
    public void sharedTooltipSkipsAGapWhateverTheSortOrder() {
        // the point at x=1 is a gap in both series; the unsorted one must answer like the sorted one
        LineSeries sorted = new LineSeries("s");
        sorted.setData(new double[]{0, 1, 2}, new double[]{1, Double.NaN, 3});
        LineSeries unsorted = new LineSeries("u");
        unsorted.setData(new double[]{2, 1, 0}, new double[]{3, Double.NaN, 1});
        Chart c = new Chart().setTooltipMode(Chart.TOOLTIP_SHARED).addSeries(sorted).addSeries(unsorted);
        calls(c, 600, 400);
        ChartContext ctx = c.getLastContext();
        int mid = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        c.pointerHover(new int[]{mid}, new int[]{ctx.getPlotY() + 5});
        implementation.setRecordDrawnStrings(true);
        List<String> texts = new ArrayList<String>();
        try {
            c.paint(Image.createImage(600, 400).getGraphics());
            for (Object[] d : implementation.getDrawnStrings()) {
                texts.add((String) d[0]);
            }
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
        int s = 0;
        int u = 0;
        for (String t : texts) {
            if (t.startsWith("s:")) {
                s++;
            } else if (t.startsWith("u:")) {
                u++;
            }
        }
        assertEquals(s, u, texts.toString());
    }
}
