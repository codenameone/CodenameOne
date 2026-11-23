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
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twenty-second review round of the chart package.
public class ChartReviewRound22Test extends UITestBase {

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
    public void layerAddedMidTransitionStartsOnThePaintedStack() {
        BarSeries a = new BarSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        BarSeries b = new BarSeries("b");
        b.setValues(10, 10, 10).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 100000);
        AnimationTime.setTime(110000);
        a.set(0, 50);
        b.set(0, 50);
        ChartContext mid = frame(c, 110300);
        assertTrue(mid.getProgress() > 0 && mid.getProgress() < 1, "positive control: mid transition");
        BarSeries top = new BarSeries("top");
        top.setValues(5, 5, 5).setStackGroup("g");
        c.addSeries(top);
        ChartContext ctx = frame(c, 110300);
        assertEquals(0, ctx.getProgress(), 1e-6, "positive control: the new transition starts");
        double painted = b.animatedBase(ctx, 0) + b.animatedY(ctx, 0);
        assertTrue(painted > 20 && painted < 100, "positive control: lower layers mid way " + painted);
        assertEquals(painted, top.animatedBase(ctx, 0), 1e-6, "the new layer starts on the painted top");
    }

    @FormTest
    public void nonLayerInAStackGroupKeepsItsOwnBounds() {
        BarSeries bar = new BarSeries("b");
        bar.setValues(10, 10).setStackGroup("g");
        ScatterSeries s = new ScatterSeries("s");
        s.setValues(50, 60).setStackGroup("g");
        new Chart().addSeries(bar).addSeries(s);
        double[] yb = s.getYBounds();
        // a scatter pads its range by 4% so the extreme dots are drawn whole
        assertEquals(50 - 10 * 0.04, yb[0], 1e-9, "zero is not folded into a scatter's bounds");
        assertEquals(60 + 10 * 0.04, yb[1], 1e-9);
    }

    @FormTest
    public void rangeBarIsNoStackLayer() {
        BarSeries a = new BarSeries("a");
        a.setValues(10).setStackGroup("g");
        RangeBarSeries r = new RangeBarSeries("r");
        r.addRange(0, 5, 8);
        r.setStackGroup("g");
        BarSeries c = new BarSeries("c");
        c.setValues(4).setStackGroup("g");
        new Chart().addSeries(a).addSeries(r).addSeries(c);
        assertEquals(10, c.getBase(0), 1e-9, "a range bar below lifts nothing");
        assertEquals(5, r.getBase(0), 1e-9, "positive control: the range keeps its start");
    }

    @FormTest
    public void categoryAxisCoversPointsPastTheLastLabel() {
        CategoryAxis ax = new CategoryAxis(Axis.BOTTOM, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        ax.setDataBounds(0, 11);
        assertEquals(11.5, ax.getDataMax(), 1e-9);
        ax.setDataBounds(0, 3);
        assertEquals(9.5, ax.getDataMax(), 1e-9, "positive control: labels keep their bands");
    }

    @FormTest
    public void pieTotalSkipsAnInfiniteSlice() {
        PieSeries p = new PieSeries("p");
        p.setData(new String[]{"a", "b", "c"}, new double[]{1, Double.POSITIVE_INFINITY, 3});
        assertEquals(4, p.getTotal(), 1e-9);
    }

    @FormTest
    public void radarVertexIsWherePainted() {
        RadarSeries s = new RadarSeries("r", "a", "b", "c");
        s.setValues(2, 4, 4);
        Chart c = showing(new Chart().addSeries(s), 100000);
        ChartPoint before = s.getPoint(c.getLastContext(), 0);
        AnimationTime.setTime(110000);
        s.setValues(4, 4, 4);
        ChartContext ctx = frame(c, 110300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        ChartPoint mid = s.getPoint(ctx, 0);
        frame(c, 113000);
        ChartPoint after = s.getPoint(c.getLastContext(), 0);
        double full = dist(before, after);
        assertTrue(full > 5, "positive control: the vertex moves " + full);
        assertTrue(dist(before, mid) > 1 && dist(mid, after) > 1, "the vertex is mid way, not at its target");
        assertNotNull(s.hitTest(ctx, mid.getScreenX(), mid.getScreenY(), 1), "a tap on the painted vertex hits");
    }

    private static double dist(ChartPoint p, ChartPoint q) {
        double dx = p.getScreenX() - q.getScreenX();
        double dy = p.getScreenY() - q.getScreenY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @FormTest
    public void heatmapTableRowsUseItsSecondaryAxis() {
        HeatmapSeries h = new HeatmapSeries("h");
        h.setMatrix(new double[][]{{1, 2}, {3, 4}});
        h.setSecondaryAxis(true);
        new Chart().addSeries(h).setYAxis(new CategoryAxis(Axis.LEFT, "P0", "P1"))
                .setSecondaryYAxis(new CategoryAxis(Axis.RIGHT, "S0", "S1"));
        List<String[]> rows = new ArrayList<String[]>();
        h.collectTableRows(rows);
        assertTrue(rows.get(0)[1].startsWith("S0"), rows.get(0)[1]);
    }

    @FormTest
    public void flatHugeSparklineIsInTheMiddle() {
        Sparkline small = new Sparkline(1, 1, 1);
        Sparkline huge = new Sparkline(1e17, 1e17, 1e17);
        assertEquals(arcY(paint(small)), arcY(paint(huge)), "a flat series draws through the middle");
    }

    private static int arcY(List<Object[]> drawn) {
        for (Object[] call : drawn) {
            if ("fillArc".equals(call[0])) {
                return (Integer) call[2];
            }
        }
        throw new AssertionError("no dot drawn");
    }

    @FormTest
    public void histogramOverTheWholeDoubleRange() {
        HistogramSeries h = new HistogramSeries("h");
        h.setSamples(new double[]{-Double.MAX_VALUE, Double.MAX_VALUE}, 2);
        assertEquals(1, h.getY(0), 1e-9);
        assertEquals(1, h.getY(1), 1e-9);
        assertTrue(!Double.isInfinite(h.getX(1)) && !Double.isNaN(h.getX(1)), "bin centre is finite");
        assertThrows(IllegalArgumentException.class, () -> h.setSamples(new double[]{1, 2}, 20000));
    }
}
