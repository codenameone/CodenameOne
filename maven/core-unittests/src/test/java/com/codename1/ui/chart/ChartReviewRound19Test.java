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
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Nineteenth review round of the chart package.
public class ChartReviewRound19Test extends UITestBase {

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

    private static List<Object[]> arcs(List<Object[]> drawn) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (Object[] call : drawn) {
            if ("fillArc".equals(call[0])) {
                out.add(call);
            }
        }
        return out;
    }

    @FormTest
    public void aNewBubbleNeverGrowsPastTheLargest() {
        // descending x, so the new point has no old slot to animate from
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{2, 1}, new double[]{5, 6}, new double[]{4, 9});
        Chart c = showing(new Chart().addSeries(s), 100000);
        int max = 0;
        for (Object[] a : arcs(paint(c))) {
            max = Math.max(max, (Integer) a[3]);
        }
        assertTrue(max > 3, "positive control: bubbles drawn " + max);
        AnimationTime.setTime(110000);
        s.add(1.5, 1000, 5);
        AnimationTime.setTime(110300);
        List<Object[]> mid = arcs(paint(c));
        assertTrue(c.getLastContext().getProgress() < 1, "positive control: mid transition");
        for (Object[] a : mid) {
            assertTrue((Integer) a[3] <= max, "bubble " + a[3] + " wider than the largest " + max);
        }
    }

    @FormTest
    public void sharedTooltipOnAScatterOnlyChart() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 2, 3}, new double[]{4, 5, 6}, null);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setTooltipMode(Chart.TOOLTIP_SHARED);
        int without = paint(c).size();
        c.showTooltipAtX(2);
        int with = paint(c).size();
        assertTrue(with > without, "the shared tooltip draws for scatter alone: " + with + " vs " + without);
    }

    @FormTest
    public void setLeavesAQuartileEqualToTheMedian() {
        BoxPlotSeries b = new BoxPlotSeries("b");
        b.addBox(1, 0, 5, 5, 8, 10);
        b.set(0, 7);
        assertEquals(5, b.animatedAux(null, 1, 0), 1e-9, "q1 stays");
        assertEquals(0, b.getMin(0), 1e-9);
        assertEquals(10, b.getMax(0), 1e-9);
    }

    @FormTest
    public void candleSetLeavesTheWickAndUpdateLastWidensIt() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        cs.set(1, 100);
        assertEquals(4, cs.animatedAux(null, 1, 1), 1e-9, "set writes the close alone");
        assertEquals(1, cs.animatedAux(null, 2, 1), 1e-9);
        cs.updateLast(200);
        assertEquals(200, cs.animatedAux(null, 1, 1), 1e-9, "a live tick widens the high");
        cs.updateLast(-5);
        assertEquals(-5, cs.animatedAux(null, 2, 1), 1e-9, "and the low");
        assertEquals(2, cs.animatedAux(null, 0, 1), 1e-9, "the open stays");
    }

    @FormTest
    public void fadingLayerTopMeetsTheBaseAbove() {
        LineSeries a = new LineSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setValues(5, 5, 5).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 200000);
        AnimationTime.setTime(210000);
        a.setVisible(false);
        AnimationTime.setTime(210300);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() < 1 && ctx.getProgress() > 0, "positive control: mid fade");
        float top = ctx.screenY(a, 0, a.animatedBase(ctx, 0) + a.animatedY(ctx, 0));
        float base = ctx.screenY(b, 0, b.animatedBase(ctx, 0));
        assertEquals(top, base, 1, "the layer above rests on the fading layer's painted top");
    }

    @FormTest
    public void denseLabelsKeepTheirPointsAcrossAPan() {
        int n = 5000;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = i;
        }
        LineSeries s = new LineSeries("s");
        s.setData(null, y);
        s.setShowValues(true);
        final Set<Integer> offered = new TreeSet<Integer>();
        s.setValueFormatter(new ValueFormatter() {
            @Override
            public String format(double value, double step) {
                offered.add((int) value);
                return "v";
            }
        });
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        c.getXAxis().setRange(1000.0, 3000.0);
        paint(c);
        Set<Integer> first = window(offered, 1200, 2800);
        c.getXAxis().setRange(1001.0, 3001.0);
        offered.clear();
        paint(c);
        Set<Integer> second = window(offered, 1200, 2800);
        assertTrue(first.size() > 10 && first.size() < 1600, "positive control: thinned " + first.size());
        assertEquals(first, second, "the same points are labelled after a one point pan");
    }

    private static Set<Integer> window(Set<Integer> all, int lo, int hi) {
        Set<Integer> out = new TreeSet<Integer>();
        for (Integer v : all) {
            if (v >= lo && v <= hi) {
                out.add(v);
            }
        }
        return out;
    }

    @FormTest
    public void removedSeriesDropsItsTransition() {
        LineSeries s = new LineSeries("s");
        s.setValues(1, 1, 1);
        LineSeries t = new LineSeries("t");
        t.setValues(1, 1, 1);
        Chart c = showing(new Chart().addSeries(s).addSeries(t), 300000);
        AnimationTime.setTime(310000);
        s.setValues(20, 20, 20);
        AnimationTime.setTime(310300);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(s.animatedY(ctx, 0) < 20, "positive control: mid transition");
        c.removeSeries(s);
        assertEquals(20, s.animatedY(ctx, 0), 1e-9, "a removed series keeps no snapshot");
        AnimationTime.setTime(320000);
        c.animate();
        paint(c);
        t.setValues(20, 20, 20);
        AnimationTime.setTime(320300);
        paint(c);
        ctx = c.getLastContext();
        assertTrue(t.animatedY(ctx, 0) < 20, "positive control: mid transition");
        c.clearSeries();
        assertEquals(20, t.animatedY(ctx, 0), 1e-9, "nor does a cleared one");
    }

    @FormTest
    public void describeSkipsHiddenSeriesAndGaps() {
        LineSeries a = new LineSeries("alpha");
        a.setValues(1, 2, 3);
        LineSeries b = new LineSeries("bravo");
        b.setValues(4, 5);
        b.setVisible(false);
        LineSeries g = new LineSeries("gaps");
        g.setValues(Double.NaN, Double.NaN);
        Chart c = new Chart().addSeries(a).addSeries(b).addSeries(g).setAnimationDuration(0);
        String d = c.describe();
        assertTrue(d.contains("alpha"), d);
        assertFalse(d.contains("bravo"), "a hidden series is not read out: " + d);
        assertFalse(d.contains("NaN"), d);
        for (String[] row : c.getTableRows()) {
            assertFalse(row[2].contains("NaN"), "no row for a gap: " + row[0] + " " + row[2]);
        }
    }

    @FormTest
    public void sparklineDrawsALonePointBetweenGaps() {
        Sparkline sp = new Sparkline(1, 2, Double.NaN, 5, Double.NaN, 3, 4);
        assertEquals(2, arcs(paint(sp)).size(), "the lone 5 and the last point");
    }

    @FormTest
    public void sparklineSpanThatOverflows() {
        Sparkline sp = new Sparkline(-Double.MAX_VALUE, 0, Double.MAX_VALUE).setShowLast(false).setShowExtremes(true);
        List<Object[]> a = arcs(paint(sp));
        assertEquals(2, a.size());
        int maxY = (Integer) a.get(0)[2];
        int minY = (Integer) a.get(1)[2];
        assertTrue(minY > maxY + H / 2, "the minimum is drawn at the bottom: " + minY + " max at " + maxY);
    }
}
