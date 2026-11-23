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
import com.codename1.ui.Display;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twenty-fifth review round of the chart package.
public class ChartReviewRound25Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
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

    private Chart show(Chart c, long now) {
        AnimationTime.setTime(now);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        AnimationTime.setTime(now + 3000);
        c.animate();
        paint(c);
        return c;
    }

    private ChartContext frame(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        paint(c);
        return c.getLastContext();
    }


    private static int sectors(List<Object[]> calls) {
        int n = 0;
        for (Object[] call : calls) {
            if ("fillShape".equals(call[0])) {
                n++;
            }
        }
        return n;
    }

    @FormTest
    public void pieOfHugeValuesDrawsWithoutHanging() {
        PieSeries s = new PieSeries("p");
        s.setData(new String[]{"a", "b"}, new double[]{1e308, 1e308});
        final Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> calls = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> paint(c));
        assertEquals(2, sectors(calls), "both halves drawn");
        s.setData(new String[]{"a", "b"}, new double[]{Double.MAX_VALUE, 1});
        calls = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> paint(c));
        assertEquals(1, sectors(calls), "the tiny slice is too thin to draw, the huge one is drawn");
    }

    @FormTest
    public void sectorOfNonFiniteSweepIsBounded() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            com.codename1.ui.chart.layout.Shapes.sector(0, 0, 10, 0, 0, Double.POSITIVE_INFINITY);
            com.codename1.ui.chart.layout.Shapes.sector(0, 0, 10, 0, 0, Double.NaN);
            com.codename1.ui.chart.layout.Shapes.sector(0, 0, 10, 5, 0, -1e300);
        });
    }

    @FormTest
    public void unsortedSlidingSeriesKeepsItsOwnValues() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{5, 1, 9, 3}, new double[]{10, 20, 30, 40});
        s.setMaxPoints(4);
        Chart c = new Chart().addSeries(s);
        c.getXAxis().setRange(0.0, 10.0);
        c.getYAxis().setRange(0.0, 100.0);
        show(c, 300000);
        s.add(2, 50);
        ChartContext ctx = frame(c, 303300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        double[] expect = {20, 30, 40};
        for (int i = 0; i < 3; i++) {
            assertEquals(expect[i], s.animatedY(ctx, i), 1e-9, "point " + i + " keeps its own value");
        }
    }

    @FormTest
    public void unsortedRemovedRowKeepsNeighboursValues() {
        BoxPlotSeries s = new BoxPlotSeries("b");
        s.addBox(5, 0, 5, 10, 15, 20);
        s.addBox(1, 0, 15, 20, 25, 30);
        s.addBox(9, 0, 25, 30, 35, 40);
        s.addBox(3, 0, 35, 40, 45, 50);
        Chart c = new Chart().addSeries(s);
        c.getXAxis().setRange(0.0, 10.0);
        c.getYAxis().setRange(0.0, 100.0);
        show(c, 400000);
        double[] before = {s.animatedY(null, 0), s.animatedY(null, 2), s.animatedY(null, 3)};
        s.addSamples(1, new double[0]);
        assertEquals(3, s.size(), "positive control: the row is gone");
        ChartContext ctx = frame(c, 403300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        for (int i = 0; i < 3; i++) {
            assertEquals(before[i], s.animatedY(ctx, i), 1e-9, "point " + i + " keeps its own value");
        }
    }

    @FormTest
    public void categoryAxisWithoutLabelsTakesItsBandsFromTheData() {
        CategoryAxis a = new CategoryAxis(Axis.BOTTOM);
        a.setDataBounds(0, 19999);
        assertEquals(19999.5, a.getVisibleMax(), 1e-9);
    }

    @FormTest
    public void rotatedChartMeasuresSecondarySeriesOnThePrimaryAxis() {
        BarSeries a = new BarSeries("a");
        a.setValues(1, 2, 3);
        BarSeries b = new BarSeries("b");
        b.setValues(100, 200, 300);
        b.setSecondaryAxis(true);
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        c.setSecondaryYAxis(new com.codename1.ui.chart.axis.NumberAxis(Axis.RIGHT));
        c.setRotated(true);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getValueAxis(b) == c.getYAxis(), "the secondary series uses the primary axis");
        assertTrue(c.getYAxis().getDataMax() >= 300, "the primary axis holds the secondary data " + c.getYAxis().getDataMax());
    }

    @FormTest
    public void flatRangeAtMaxValueStaysFinite() {
        Axis a = new com.codename1.ui.chart.axis.NumberAxis(Axis.LEFT);
        a.setDataBounds(Double.MAX_VALUE, Double.MAX_VALUE);
        assertTrue(!Double.isInfinite(a.getDataMin()) && !Double.isInfinite(a.getDataMax()), "finite bounds");
        assertTrue(a.getDataMin() < a.getDataMax(), "non-empty range");
        double n = (Double.MAX_VALUE - a.getDataMin()) / (a.getDataMax() - a.getDataMin());
        assertTrue(n >= 0 && n <= 1, "the point lies on the axis " + n);
    }

    @FormTest
    public void shrinkingRadarHasANiceOuterRing() throws Exception {
        RadarSeries s = new RadarSeries("r", "a", "b", "c");
        s.setValues(1000, 1000, 1000);
        Chart c = show(new Chart().addSeries(s), 500000);
        s.setValues(10, 10, 10);
        ChartContext ctx = frame(c, 503300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        double top = ChartReviewRound12Test.fieldOf(s, "lastMax");
        double shown = 10 + (1000 - 10) * (1 - ctx.getProgress());
        assertTrue(top >= shown, "the ring holds the polygon");
        double mag = Math.pow(10, Math.floor(Math.log10(top)));
        double lead = top / mag;
        boolean nice = Math.abs(lead - Math.rint(lead)) < 1e-9 || Math.abs(lead * 2 - Math.rint(lead * 2)) < 1e-9;
        assertTrue(nice, "outer ring " + top + " is a round value");
    }

    @FormTest
    public void rotatedZoomedChartPansWithLeftAndRight() {
        BarSeries a = new BarSeries("a");
        a.setValues(1, 2, 3, 4, 5);
        BarSeries b = new BarSeries("b");
        b.setValues(5, 4, 3, 2, 1);
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        c.setRotated(true);
        c.setZoomEnabled(true);
        paint(c);
        c.getYAxis().setVisibleRange(1, 3);
        paint(c);
        assertTrue(c.getYAxis().isZoomed(), "positive control: the value axis is zoomed");
        double before = c.getYAxis().getVisibleMin();
        c.keyReleased(Display.getInstance().getKeyCode(Display.GAME_RIGHT));
        assertTrue(Math.abs(c.getYAxis().getVisibleMin() - before) > 1e-9, "right pans the horizontal value axis");
    }
}
