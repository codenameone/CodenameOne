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
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twenty-third review round of the chart package.
public class ChartReviewRound23Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private Chart showing(Chart c, long now) {
        AnimationTime.setTime(now);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        AnimationTime.setTime(now + 2000);
        c.animate();
        paint(c);
        return c;
    }

    private List<Object[]> paint(com.codename1.ui.Component c) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(W, H).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private ChartContext frame(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        paint(c);
        return c.getLastContext();
    }

    @FormTest
    public void histogramOrdinaryData() {
        HistogramSeries h = new HistogramSeries("h");
        h.setSamples(new double[]{0, 1, 2, 3}, 4);
        for (int b = 0; b < 4; b++) {
            assertEquals(1, h.getY(b), 1e-9, "bin " + b);
        }
    }

    @FormTest
    public void radarGrowsInAgainstTheTargetScale() {
        RadarSeries s = new RadarSeries("r", "a", "b", "c");
        s.setValues(4, 4, 4);
        AnimationTime.setTime(100000);
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        ChartContext ctx = frame(c, 100150);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid entry");
        double mid = side(s, ctx);
        frame(c, 103000);
        double end = side(s, c.getLastContext());
        assertTrue(end > 5, "positive control: a polygon is drawn " + end);
        assertTrue(mid < end * 0.6, "mid entry the polygon is smaller: " + mid + " vs " + end);
    }

    private static double side(RadarSeries s, ChartContext ctx) {
        ChartPoint p = s.getPoint(ctx, 0);
        ChartPoint q = s.getPoint(ctx, 1);
        double dx = p.getScreenX() - q.getScreenX();
        double dy = p.getScreenY() - q.getScreenY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @FormTest
    public void dashedStepLineIsAxisAligned() {
        LineSeries s = new LineSeries("l");
        s.setValues(1, 5, 2, 8);
        s.setMode(LineSeries.STEP);
        s.setDashed(true);
        s.setMarkers(false);
        showing(new Chart().addSeries(s), 100000);
        Shape shape = implementation.getLastDrawShape();
        assertNotNull(shape);
        PathIterator it = shape.getPathIterator();
        float[] c = new float[6];
        float lx = 0;
        float ly = 0;
        int lines = 0;
        while (!it.isDone()) {
            int t = it.currentSegment(c);
            if (t == PathIterator.SEG_LINETO) {
                lines++;
                assertTrue(Math.abs(c[0] - lx) < 0.01 || Math.abs(c[1] - ly) < 0.01,
                        "dash from " + lx + "," + ly + " to " + c[0] + "," + c[1] + " is diagonal");
            }
            lx = c[0];
            ly = c[1];
            it.next();
        }
        assertTrue(lines > 0, "positive control: dashes drawn");
    }

    @FormTest
    public void dashedHugeSegmentEndsPromptly() {
        LineSeries s = new LineSeries("l");
        s.setValues(0, 1e12);
        s.setDashed(true);
        Chart c = new Chart().addSeries(s);
        c.setYAxis(new NumberAxis(Axis.LEFT).setRange(0.0, 1.0));
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> showing(c, 100000));
    }

    @FormTest
    public void heikinAshiBoundsIncludeTheDrawnCandle() {
        CandlestickSeries s = new CandlestickSeries("c");
        s.addOhlc(0, 300, 100, 50, 60);
        s.setStyle(CandlestickSeries.HEIKIN_ASHI);
        double[] b = s.getYBounds();
        assertTrue(b[1] >= 180 - 1e-9, "averaged open 180 is on the axis: " + b[1]);
    }

    @FormTest
    public void candleHitTestFollowsThePaintedRow() {
        CandlestickSeries s = new CandlestickSeries("c");
        double[] x = {0, 1, 2};
        s.setOhlc(x, new double[]{10, 10, 10}, new double[]{20, 20, 20}, new double[]{5, 5, 5},
                new double[]{15, 15, 15});
        Chart c = new Chart().addSeries(s);
        c.setYAxis(new NumberAxis(Axis.LEFT).setRange(0.0, 200.0));
        showing(c, 100000);
        AnimationTime.setTime(110000);
        s.setOhlc(x, new double[]{150, 150, 150}, new double[]{160, 160, 160}, new double[]{145, 145, 145},
                new double[]{155, 155, 155});
        ChartContext ctx = frame(c, 110200);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 0.6, "positive control: early in the transition");
        int px = Math.round(ctx.screenX(s, 1, 155));
        int py = Math.round(ctx.screenY(s, 1, 155));
        assertNull(s.hitTest(ctx, px, py, 1), "the target is not painted yet");
        frame(c, 113000);
        ChartContext end = c.getLastContext();
        assertNotNull(s.hitTest(end, Math.round(end.screenX(s, 1, 155)), Math.round(end.screenY(s, 1, 155)), 1),
                "positive control: hits once there");
    }

    @FormTest
    public void gaugeBandsFadeWithTheSeries() {
        GaugeSeries g = new GaugeSeries("g", 0, 100);
        g.setValue(50);
        g.addBand(0, 50, 0xff0000);
        g.setNeedle(false);
        Chart c = showing(new Chart().addSeries(g), 100000);
        AnimationTime.setTime(110000);
        g.setVisible(false);
        AnimationTime.setTime(110400);
        c.animate();
        List<Object[]> calls = paint(c);
        int track = -1;
        int maxAlpha = -1;
        for (Object[] call : calls) {
            if ("fillShape".equals(call[0])) {
                int a = (Integer) call[5];
                if (track < 0) {
                    track = a;
                }
                maxAlpha = Math.max(maxAlpha, a);
            }
        }
        assertTrue(track > 0 && track < 250, "positive control: mid fade " + track);
        assertTrue(maxAlpha <= track, "nothing after the bands is drawn opaque: " + maxAlpha + " vs " + track);
    }

    @FormTest
    public void axisNearTheEndOfTheDoubleRange() {
        NumberAxis a = new NumberAxis(Axis.LEFT);
        a.setDataBounds(0, 1.7e308);
        assertTrue(!Double.isInfinite(a.getVisibleMax()) && a.getVisibleMax() >= 1.7e308, "" + a.getVisibleMax());
        double n = a.toNormalized(1.7e308);
        assertTrue(!Double.isNaN(n) && !Double.isInfinite(n) && n > 0.5, "" + n);
        LogAxis l = new LogAxis(Axis.LEFT);
        l.setDataBounds(1, 1.7e308);
        assertTrue(!Double.isInfinite(l.getVisibleMax()) && l.getVisibleMax() >= 1.7e308, "" + l.getVisibleMax());
        double ln = l.toNormalized(1.7e308);
        assertTrue(!Double.isNaN(ln) && !Double.isInfinite(ln), "" + ln);
    }

    @FormTest
    public void categoryBandsCappedByOneRule() {
        CategoryAxis a = new CategoryAxis(Axis.BOTTOM, "a", "b");
        a.setDataBounds(0, 1e9);
        double near = a.getVisibleMax();
        a.setDataBounds(0, 3e9);
        assertEquals(near, a.getVisibleMax(), 1e-9, "the same cap for both");
        a.setDataBounds(0, 5);
        assertEquals(5.5, a.getVisibleMax(), 1e-9, "a point past the labels still gets its band");
    }

    @FormTest
    public void paletteIndexIsAFloorModulo() {
        ChartTheme t = new ChartTheme(false).setPalette(1, 2, 3);
        assertEquals(3, t.getSeriesColor(-1));
        int m = t.getSeriesColor(Integer.MIN_VALUE);
        assertTrue(m >= 1 && m <= 3, "" + m);
    }
}
