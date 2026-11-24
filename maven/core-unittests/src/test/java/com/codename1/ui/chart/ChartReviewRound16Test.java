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
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.chart.series.VolumeSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Sixteenth review round of the chart package: the last sweep for non-finite values.
public class ChartReviewRound16Test extends UITestBase {

    private static final double INF = Double.POSITIVE_INFINITY;

    private void place(Chart c, int w, int h) {
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    private List<Object[]> calls(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        place(c, w, h);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(w, h).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private List<Object[]> exportCalls(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        implementation.setRecordDrawCalls(true);
        try {
            c.toImage(w, h);
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private static void assertNoHugeCoordinates(List<Object[]> drawn) {
        assertFalse(drawn.isEmpty(), "positive control: something was drawn");
        for (Object[] call : drawn) {
            for (int k = 1; k < call.length; k++) {
                if (call[k] instanceof Integer) {
                    int v = ((Integer) call[k]).intValue();
                    assertTrue(Math.abs(v) < 1000000, "draw call " + Arrays.toString(call));
                }
            }
        }
    }

    // an infinity is a gap exactly as NaN is: each indicator answers the same with either
    private static double[] withGap(double gap) {
        return new double[]{1, 3, 2, 5, gap, 4, 6, 5, 7, 8, 6, 9, 10, 9, 11};
    }

    @Test
    public void indicatorsRecoverAfterAnInfinity() {
        assertArrayEquals(Indicators.sma(withGap(Double.NaN), 3), Indicators.sma(withGap(INF), 3), 1e-9);
        assertArrayEquals(Indicators.ema(withGap(Double.NaN), 3), Indicators.ema(withGap(INF), 3), 1e-9);
        assertArrayEquals(Indicators.rsi(withGap(Double.NaN), 3), Indicators.rsi(withGap(INF), 3), 1e-9);
        double[][] bn = Indicators.bollinger(withGap(Double.NaN), 3, 2);
        double[][] bi = Indicators.bollinger(withGap(INF), 3, 2);
        double[][] mn = Indicators.macd(withGap(Double.NaN), 2, 3, 2);
        double[][] mi = Indicators.macd(withGap(-INF), 2, 3, 2);
        for (int k = 0; k < 3; k++) {
            assertArrayEquals(bn[k], bi[k], 1e-9, "bollinger " + k);
            assertArrayEquals(mn[k], mi[k], 1e-9, "macd " + k);
        }
        double[] sma = Indicators.sma(withGap(INF), 3);
        assertFalse(Double.isNaN(sma[sma.length - 1]), "positive control: the average recovers");
    }

    @FormTest
    public void stackedBaseNeverMatchesANaNXSlot() {
        LineSeries a = new LineSeries("a");
        a.setData(new double[]{0, Double.NaN, 2}, new double[]{1, 5, 1});
        a.setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setData(new double[]{0, 1, 2}, new double[]{1, 1, 1});
        b.setStackGroup("g");
        new Chart().addSeries(a).addSeries(b);
        assertEquals(1, b.getBase(0), 0, "positive control: x 0 stacks on a");
        assertEquals(0, b.getBase(1), 0, "x 1 has nothing under it; a's NaN-x slot is not x 1");
    }

    // exposes the protected colour of a volume bar
    static class ProbeVolume extends VolumeSeries {
        ProbeVolume(CandlestickSeries prices, double[] volume) {
            super("v", prices, volume);
        }

        int color(ChartContext ctx, int i) {
            return colorAt(ctx, i);
        }
    }

    @FormTest
    public void volumeNeverTakesTheColourOfANaNXCandle() {
        CandlestickSeries p = new CandlestickSeries("p");
        p.setOhlc(new double[]{0, Double.NaN, 2, 3}, new double[]{1, 5, 1, 1},
                new double[]{2, 6, 2, 2}, new double[]{0, 1, 0, 0}, new double[]{2, 2, 2, 2});
        ProbeVolume v = new ProbeVolume(p, new double[]{1, 1, 1, 1});
        v.setData(new double[]{0, 1, 2, 3}, new double[]{1, 1, 1, 1});
        Chart c = new Chart().addSeries(p).addSeries(v).setAnimationDuration(0);
        calls(c, 400, 300);
        ChartContext ctx = c.getLastContext();
        assertNotEquals(v.getColor(), v.color(ctx, 0), "positive control: x 0 is matched to its candle");
        assertEquals(v.getColor(), v.color(ctx, 1), "x 1 has no candle; the NaN-x candle is not it");
    }

    @FormTest
    public void candleWithAnInfiniteHighIsAGap() {
        CandlestickSeries s = new CandlestickSeries("c");
        s.setOhlc(new double[]{0, 1, 2}, new double[]{1, 2, 3}, new double[]{2, INF, 4},
                new double[]{0, 1, 2}, new double[]{2, 3, 4});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        assertNoHugeCoordinates(calls(c, 400, 300));
        ChartContext ctx = c.getLastContext();
        int cx = (int) ctx.screenX(s, 1, 2.5) - ctx.getOriginX();
        int top = ctx.getPlotY() + 2 - ctx.getOriginY();
        ChartPoint hit = s.hitTest(ctx, cx, top, 4);
        assertTrue(hit == null || hit.getIndex() != 1, "the gap candle has no wick to hit");
        int cx0 = (int) ctx.screenX(s, 0, 1.5) - ctx.getOriginX();
        int y0 = (int) ctx.screenY(s, 0, 1.5) - ctx.getOriginY();
        ChartPoint control = s.hitTest(ctx, cx0, y0, 4);
        assertTrue(control != null && control.getIndex() == 0, "positive control: a real candle hits");
    }

    @FormTest
    public void transitionFromAnInfinityShowsThePoint() {
        AnimationTime.setTime(50000);
        try {
            LineSeries s = new LineSeries("s");
            s.setData(new double[]{0, 1, 2}, new double[]{1, INF, 3});
            Chart c = new Chart().addSeries(s).setAnimationDuration(400);
            Form f = new Form(new BorderLayout());
            f.add(BorderLayout.CENTER, c);
            f.show();
            place(c, 400, 300);
            c.paint(Image.createImage(400, 300).getGraphics());
            AnimationTime.setTime(52000);
            c.animate();
            c.paint(Image.createImage(400, 300).getGraphics());
            s.setData(new double[]{0, 1, 2}, new double[]{1, 2, 3});
            c.paint(Image.createImage(400, 300).getGraphics());
            AnimationTime.setTime(52200);
            c.animate();
            c.paint(Image.createImage(400, 300).getGraphics());
            ChartContext ctx = c.getLastContext();
            assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition " + ctx.getProgress());
            assertEquals(2, s.animatedY(ctx, 1), 1e-9, "the point appears at its target rather than vanishing");
        } finally {
            AnimationTime.reset();
        }
    }

    @Test
    public void downsampleNaNAreaDoesNotBlockTheRealExtreme() {
        // the second point's area overflows to NaN (infinity minus infinity); the third's is a
        // real (infinite) extreme and must win
        double[] y = {-1e308, 1e308, -1e308, 5, 1e308};
        int[] out = Downsample.lttb(null, y, 3);
        assertEquals(3, out.length, Arrays.toString(out));
        assertEquals(2, out[1], Arrays.toString(out));
    }

    @Test
    public void restoreViewportIgnoresAnImpossibleRange() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 10}, new double[]{0, 1});
        Chart c = new Chart().addSeries(s);
        Axis x = c.getXAxis();
        x.restoreViewport(2, 4, true);
        assertEquals(2, x.getVisibleMin(), 0, "positive control");
        x.restoreViewport(Double.NaN, 5, true);
        x.restoreViewport(3, INF, true);
        x.restoreViewport(6, 5, false);
        assertEquals(2, x.getVisibleMin(), 0);
        assertEquals(4, x.getVisibleMax(), 0);
        assertTrue(x.isZoomed());
    }

    private Chart sharedTooltipChart() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2, 3, 4}, new double[]{1, 4, 2, 8, 5});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setTooltipMode(Chart.TOOLTIP_SHARED);
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        return c;
    }

    @FormTest
    public void exportLeavesOutTheTooltip() {
        Chart plain = sharedTooltipChart();
        Chart tipped = sharedTooltipChart();
        tipped.showTooltipAtX(3);
        assertTrue(calls(tipped, 400, 300).size() > calls(plain, 400, 300).size(),
                "positive control: the tooltip draws on screen");
        // toImage paints the export, then lays the chart out again at its own size into a scratch
        // image (which may draw the tooltip: that is the screen state, not the export). So the
        // tooltip chart's calls must open with exactly the plain chart's export calls
        List<Object[]> ep = exportCalls(plain, 400, 300);
        List<Object[]> et = exportCalls(tipped, 400, 300);
        assertTrue(et.size() >= ep.size(), "positive control");
        assertEquals(Arrays.deepToString(ep.toArray()), Arrays.deepToString(et.subList(0, ep.size()).toArray()),
                "the export is the chart, not the pointer state");
    }

    private List<Object[]> bubbles(double first) {
        ScatterSeries s = new ScatterSeries("s");
        s.add(1, 1, first);
        s.add(2, 2, 5);
        s.add(3, 1, 2);
        return calls(new Chart().addSeries(s).setAnimationDuration(0), 400, 300);
    }

    @FormTest
    public void infiniteBubbleSizeIsAGap() {
        List<Object[]> inf = bubbles(INF);
        assertNoHugeCoordinates(inf);
        // an infinite size is a plain dot like NaN, and never becomes the scale every other
        // bubble is sized against
        assertEquals(Arrays.deepToString(bubbles(Double.NaN).toArray()), Arrays.deepToString(inf.toArray()));
    }

    @FormTest
    public void infiniteGaugeValueIsNoReading() {
        int nan = calls(new Chart().addSeries(new GaugeSeries("g", 0, 10).setValue(Double.NaN)).setAnimationDuration(0), 300, 300).size();
        int inf = calls(new Chart().addSeries(new GaugeSeries("g", 0, 10).setValue(INF)).setAnimationDuration(0), 300, 300).size();
        int real = calls(new Chart().addSeries(new GaugeSeries("g", 0, 10).setValue(5)).setAnimationDuration(0), 300, 300).size();
        assertNotEquals(nan, real, "positive control: a reading draws more");
        assertEquals(nan, inf, "an infinite reading draws like no reading");
    }

    @Test
    public void formatNumberOfAnInfinityIsEmpty() {
        assertEquals("", Axis.formatNumber(INF, 1));
        assertEquals("", Axis.formatNumber(-INF, 0));
    }
}
