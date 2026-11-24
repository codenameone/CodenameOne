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
import com.codename1.ui.chart.series.Downsample;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Gestures to viewport math, data transitions, and how large series are reduced.
public class ChartInteractionTest extends UITestBase {

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

    private static Chart lineChart(int n) {
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = (i * 13) % 11;
        }
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        c.setAnimationDuration(0);
        return c;
    }

    // the x value drawn at a horizontal screen position
    private static double xAt(Chart c, ChartContext ctx, int px) {
        Axis a = c.getXAxis();
        return a.fromNormalized((px - ctx.getPlotX()) / (double) ctx.getPlotWidth());
    }

    @FormTest
    public void wheelZoomKeepsTheValueUnderThePointer() {
        Chart c = lineChart(200);
        ChartContext ctx = paint(c);
        int px = ctx.getPlotX() + ctx.getPlotWidth() / 3;
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        double before = xAt(c, ctx, px);
        double span = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
        assertTrue(c.mouseWheel(new WheelEvent(c, px, py, 0, -120, false, 0)));
        double after = xAt(c, ctx, px);
        double zoomedSpan = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
        assertTrue(zoomedSpan < span, "zoomed in");
        assertEquals(before, after, span * 1e-6);
    }

    @FormTest
    public void dragPansByTheDistanceMoved() {
        Chart c = lineChart(200);
        ChartContext ctx = paint(c);
        c.getXAxis().zoom(2, 0.5);
        double min = c.getXAxis().getVisibleMin();
        double span = c.getXAxis().getVisibleMax() - min;
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        int x0 = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        int dx = ctx.getPlotWidth() / 4;
        c.pointerPressed(x0, py);
        c.pointerDragged(new int[]{x0 + 20}, new int[]{py});
        c.pointerDragged(new int[]{x0 + dx}, new int[]{py});
        // content follows the finger: dragging right shows earlier values
        assertEquals(min - span * (dx / (double) ctx.getPlotWidth()), c.getXAxis().getVisibleMin(), span * 1e-6);
        c.pointerReleased(x0 + dx, py);
    }

    @FormTest
    public void sidewaysPinchZoomsXAroundItsCenter() {
        Chart c = lineChart(200);
        ChartContext ctx = paint(c);
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        int cx = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        double center = xAt(c, ctx, cx);
        double ySpan = c.getYAxis().getVisibleMax() - c.getYAxis().getVisibleMin();
        double span = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
        c.pointerPressed(cx - 50, py);
        c.pointerDragged(new int[]{cx - 50, cx + 50}, new int[]{py, py});
        c.pointerDragged(new int[]{cx - 100, cx + 100}, new int[]{py, py});
        double zoomedSpan = c.getXAxis().getVisibleMax() - c.getXAxis().getVisibleMin();
        assertEquals(span / 2, zoomedSpan, span * 1e-6);
        assertEquals(center, xAt(c, ctx, cx), span * 1e-6);
        // fingers side by side leave the value axis alone
        assertEquals(ySpan, c.getYAxis().getVisibleMax() - c.getYAxis().getVisibleMin(), 1e-9);
        c.pointerReleased(cx + 100, py);
    }

    @FormTest
    public void changedValuesStartFromWhereTheyWere() {
        LineSeries s = new LineSeries("l");
        s.setData(null, new double[]{10, 10, 10});
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        ChartContext ctx = paint(c);
        c.setAnimationDuration(1000000);
        s.setData(null, new double[]{30, 30, 30});
        ctx = paint(c);
        float t = ctx.getProgress();
        assertTrue(t < 0.01f, "progress " + t);
        // the value moves from the old picture to the new one
        assertEquals(10 + 20 * t, s.animatedY(ctx, 1), 0.5);
        assertEquals(30, s.getY(1), 1e-9);
        c.setAnimationDuration(0);
        s.setData(null, new double[]{30, 30, 30});
        ctx = paint(c);
        assertEquals(30, s.animatedY(ctx, 1), 1e-9);
    }

    @FormTest
    public void aSpikeInFiftyThousandPointsIsDrawn() {
        int n = 50000;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = (i % 7) / 7.0;
        }
        y[31234] = 100;
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        c.setAnimationDuration(0);
        // every shipping port draws shapes; the test implementation only does so when asked
        implementation.setShapeSupported(true);
        implementation.resetShapeTracking();
        ChartContext ctx;
        Shape line;
        try {
            ctx = paint(c);
            line = implementation.getLastDrawShape();
        } finally {
            implementation.setShapeSupported(false);
        }
        assertNotNull(line);
        Rectangle b = line.getBounds();
        // the line reaches the top of the plot, where only the spike is
        assertTrue(b.getY() <= ctx.getPlotY() + ctx.getPlotHeight() / 20, "line top " + b.getY() + " plot " + ctx.getPlotY());
    }

    @FormTest
    public void lttbKeepsEndsAndTheSpike() {
        int n = 50000;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = (i % 7) / 7.0;
        }
        y[31234] = 100;
        int[] keep = Downsample.lttb(null, y, 500);
        assertEquals(500, keep.length);
        assertEquals(0, keep[0]);
        assertEquals(n - 1, keep[keep.length - 1]);
        boolean spike = false;
        for (int i = 0; i < keep.length; i++) {
            if (i > 0) {
                assertTrue(keep[i] > keep[i - 1]);
            }
            spike |= keep[i] == 31234;
        }
        assertTrue(spike);
        assertEquals(100, Downsample.pick(y, keep)[java.util.Arrays.binarySearch(keep, 31234)], 0);
        assertEquals(10, Downsample.lttb(null, new double[10], 50).length);
    }

    @FormTest
    public void rtlTooltipOpensTowardTheStart() {
        Chart c = lineChart(20);
        c.setTooltipMode(Chart.TOOLTIP_SHARED);
        c.setRTL(true);
        ChartContext ctx = paint(c);
        int ax = ctx.getPlotX() + ctx.getPlotWidth() * 3 / 4;
        int ay = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        java.util.Set<String> plain = new java.util.HashSet<String>();
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c);
            for (Object[] d : implementation.getDrawnStrings()) {
                plain.add((String) d[0]);
            }
            c.pointerHover(new int[]{ax}, new int[]{ay});
            implementation.setRecordDrawnStrings(true);
            paint(c);
            int tips = 0;
            for (Object[] d : implementation.getDrawnStrings()) {
                String str = (String) d[0];
                if (plain.contains(str)) {
                    continue;
                }
                tips++;
                int right = ((Integer) d[1]).intValue() + implementation.stringWidth(d[3], str);
                assertTrue(right <= ax, "'" + str + "' ends at " + right + ", point at " + ax);
            }
            assertTrue(tips > 0, "no tooltip drawn");
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }
}
