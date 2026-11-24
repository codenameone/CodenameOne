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
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.axis.TimeAxis;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound26Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private void paint(com.codename1.ui.Component c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
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

    @FormTest
    public void unsortedTrimKeepsEachPointsOwnValue() {
        ScatterSeries s = new ScatterSeries("s");
        double[] xs = {5, 1, 9, 3, 7, 2, 8, 4, 6, 0};
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            ys[i] = 10 * (i + 1);
        }
        s.setData(xs, ys);
        Chart c = new Chart().addSeries(s);
        c.getXAxis().setRange(0.0, 10.0);
        c.getYAxis().setRange(0.0, 200.0);
        show(c, 500000);
        s.setMaxPoints(5);
        AnimationTime.setTime(503300);
        c.animate();
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        for (int i = 0; i < 5; i++) {
            assertEquals(ys[i + 5], s.animatedY(ctx, i), 1e-9, "point " + i + " tweens from its own value");
        }
    }

    @FormTest
    public void pieLabelOfHugeSlicesIsAPercentage() {
        PieSeries s = new PieSeries("p");
        s.setData(new String[]{"a", "b"}, new double[]{1.5e308, 1.5e308});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c);
            List<Object[]> strings = implementation.getDrawnStrings();
            boolean half = false;
            for (Object[] str : strings) {
                String t = (String) str[0];
                assertTrue(t.indexOf("NaN") < 0, t);
                half |= t.indexOf("50%") >= 0;
            }
            assertTrue(half, "a 50% label is drawn: " + strings.size());
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }

    @FormTest
    public void labelLessCategoryAxisIgnoresAFarOutlier() {
        LineSeries s = new LineSeries("l");
        double[] xs = new double[21];
        double[] ys = new double[21];
        for (int i = 0; i < 20; i++) {
            xs[i] = i;
            ys[i] = i;
        }
        xs[20] = 1e9;
        s.setData(xs, ys);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        c.setXAxis(new CategoryAxis(Axis.BOTTOM));
        paint(c);
        assertEquals(19.5, c.getXAxis().getVisibleMax(), 1e-9, "bands cover the real points");
        CategoryAxis a = new CategoryAxis(Axis.BOTTOM);
        a.setDataBounds(0, 1e9);
        assertEquals(0.5, a.getVisibleMax(), 1e-9, "an outlier alone is ignored, not clamped to the cap");
    }

    @FormTest
    public void singleTimestampPadsByAnHour() {
        TimeAxis t = new TimeAxis(Axis.BOTTOM);
        double ts = 1.7e12;
        t.setDataBounds(ts, ts);
        assertTrue(t.getVisibleMax() - t.getVisibleMin() <= 86400000.0,
                "range " + (t.getVisibleMax() - t.getVisibleMin()));
        assertTrue(t.getVisibleMin() <= ts && t.getVisibleMax() >= ts, "the point is in range");
    }

    @FormTest
    public void flatOverflowNeverMovesAFixedBound() {
        NumberAxis a = new NumberAxis(Axis.LEFT);
        a.setRange(Double.valueOf(1.7e308), null);
        a.setDataBounds(1.7e308, 1.7e308);
        assertEquals(1.7e308, a.getVisibleMin(), 0, "the fixed min stays");
        assertTrue(!Double.isInfinite(a.getVisibleMax()) && a.getVisibleMax() > 1.7e308, "" + a.getVisibleMax());
        NumberAxis b = new NumberAxis(Axis.LEFT);
        b.setRange(null, Double.valueOf(-1.7e308));
        b.setDataBounds(-1.7e308, -1.7e308);
        assertEquals(-1.7e308, b.getVisibleMax(), 0, "the fixed max stays");
        assertTrue(!Double.isInfinite(b.getVisibleMin()) && b.getVisibleMin() < -1.7e308, "" + b.getVisibleMin());
    }

    @FormTest
    public void gaugeRangeInTheWrongOrderIsSwapped() {
        GaugeSeries g = new GaugeSeries("g", 0, 1).setRange(100, 0);
        assertEquals(0, g.getRangeMin(), 0);
        assertEquals(100, g.getRangeMax(), 0);
    }
}
