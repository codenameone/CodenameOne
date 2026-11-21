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
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChartReviewRound30Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private void paint(com.codename1.ui.Component c) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
    }

    @FormTest
    public void pickOfNullXKeepsOriginalPositions() {
        int n = 200;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = (i % 17 == 0) ? 100 : Math.sin(i * 0.3) * (i % 5);
        }
        int[] keep = Downsample.lttb(null, y, 20);
        LineSeries s = new LineSeries("s");
        s.setData(Downsample.pick(null, keep), Downsample.pick(y, keep));
        assertEquals(keep.length, s.getPointCount());
        for (int i = 0; i < keep.length; i++) {
            assertEquals(keep[i], s.getX(i), 0, "point " + i + " stays at its original x");
        }
    }

    @FormTest
    public void waterfallKeepsTheRealTotalAfterSliding() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setMaxPoints(3);
        for (int i = 0; i < 5; i++) {
            w.add(1);
        }
        assertEquals(3, w.getPointCount());
        // the last bar starts on the four earlier deltas
        assertEquals(4, w.getBase(2), 1e-9);
        WaterfallSeries t = new WaterfallSeries("t");
        t.setSteps(new double[]{1, 1, 1, 1, 0}, new boolean[]{false, false, false, false, true});
        t.setMaxPoints(2);
        assertEquals(4, t.getY(1), 1e-9, "the total shows the real cumulative sum");
        WaterfallSeries d = new WaterfallSeries("d");
        d.setMaxPoints(2);
        d.setSteps(new double[]{1, 1, 1, 1, 0}, new boolean[]{false, false, false, false, true});
        assertEquals(4, d.getY(1), 1e-9, "rows trimmed by setSteps still count");
    }

    @FormTest
    public void shortRadarSeriesIsAnOpenLine() {
        RadarSeries grid = new RadarSeries("g", "A", "B", "C", "D", "E");
        grid.setValues(5, 5, 5, 5, 5);
        RadarSeries s = new RadarSeries("s");
        s.setValues(3, 3, 3);
        Chart c = new Chart().addSeries(grid).addSeries(s).setAnimationDuration(0);
        paint(c);
        Shape last = implementation.getLastDrawShape();
        assertNotNull(last);
        PathIterator it = last.getPathIterator();
        float[] pt = new float[6];
        int segs = 0;
        boolean closed = false;
        while (!it.isDone()) {
            int type = it.currentSegment(pt);
            if (type == PathIterator.SEG_CLOSE) {
                closed = true;
            } else {
                segs++;
            }
            it.next();
        }
        assertFalse(closed, "no chord from the third spoke back to the first");
        assertEquals(3, segs, "one vertex per value");
    }

    @FormTest
    public void radarsShareTheGridOwnersSpokeCount() {
        RadarSeries a = new RadarSeries("a", "A", "B", "C", "D", "E");
        a.setValues(5, 5, 5, 5, 5);
        RadarSeries b = new RadarSeries("b", "X", "Y", "Z");
        b.setValues(4, 4, 4);
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        paint(c);
        ChartContext ctx = c.getLastContext();
        double cx = ctx.getPlotX() + ctx.getPlotWidth() / 2.0 - ctx.getOriginX();
        double cy = ctx.getPlotY() + ctx.getPlotHeight() / 2.0 - ctx.getOriginY();
        ChartPoint p0 = b.getPoint(ctx, 0);
        ChartPoint p1 = b.getPoint(ctx, 1);
        double d = Math.toDegrees(Math.abs(Math.atan2(p1.getScreenY() - cy, p1.getScreenX() - cx)
                - Math.atan2(p0.getScreenY() - cy, p0.getScreenX() - cx)));
        if (d > 180) {
            d = 360 - d;
        }
        assertEquals(72, d, 2, "the second series sits on the grid's five spokes");
    }

    @FormTest
    public void failedSelectionClearsTheOldOne() {
        SankeySeries s = new SankeySeries("s");
        s.addLink("A", "B", 5).addLink("B", "C", 3);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        c.setSelection(s, 1);
        assertNotNull(c.getSelection(), "positive control");
        s.addLink("C", "D", 2);
        c.setSelection(s, 3);
        assertNull(c.getSelection(), "the replaced point is no longer selected");
        paint(c);
        assertNotNull(c.getSelection());
        assertEquals(3, c.getSelection().getIndex());
    }

    @FormTest
    public void selectionOnAHiddenSeriesWaitsUntilItIsPainted() {
        PieSeries a = new PieSeries("a");
        a.setData(new String[]{"a", "b"}, new double[]{1, 2});
        PieSeries b = new PieSeries("b");
        b.setData(new String[]{"x", "y", "z"}, new double[]{1, 2, 3});
        b.setVisible(false);
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        paint(c);
        c.setSelection(b, 2);
        paint(c);
        assertNull(c.getSelection(), "nothing to select while hidden");
        b.setVisible(true);
        paint(c);
        assertNotNull(c.getSelection(), "applied once the series is painted");
        assertEquals(2, c.getSelection().getIndex());
    }

    @FormTest
    public void heatmapOnALogAxisHitsItsCells() {
        HeatmapSeries s = new HeatmapSeries("h");
        double[][] m = new double[12][3];
        for (int r = 0; r < m.length; r++) {
            for (int k = 0; k < 3; k++) {
                m[r][k] = r + k + 1;
            }
        }
        s.setMatrix(m);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        c.setXAxis(new CategoryAxis(Axis.BOTTOM, "a", "b", "c"));
        c.setYAxis(new LogAxis(Axis.LEFT));
        paint(c);
        ChartContext ctx = c.getLastContext();
        // row 0 spans -0.5 to 0.5, which a log axis cannot place; every other cell can be tapped
        for (int r = 1; r < m.length; r++) {
            for (int k = 0; k < 3; k++) {
                int x = Math.round(ctx.screenX(s, k, r)) - ctx.getOriginX();
                int y = Math.round(ctx.screenY(s, k, r)) - ctx.getOriginY();
                ChartPoint p = s.hitTest(ctx, x, y, 4);
                assertNotNull(p, "the tap lands on cell " + r + "," + k);
                assertEquals(s.cellId(r, k), p.getIndex());
            }
        }
    }
}
