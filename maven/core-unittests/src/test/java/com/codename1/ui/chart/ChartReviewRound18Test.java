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
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Eighteenth review round of the chart package.
public class ChartReviewRound18Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;
    private static final int HIGH = 1;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    // a showing chart, so data changes animate; time starts at `now`
    private Chart showing(Chart c, long now) {
        AnimationTime.setTime(now);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        settle(c, now + 2000);
        return c;
    }

    private List<Object[]> paint(Chart c) {
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

    private void settle(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        paint(c);
    }

    @FormTest
    public void rotatedAreaClosesOnTheValueBaseline() {
        LineSeries s = new LineSeries("s").setFilled(true);
        s.setData(null, new double[]{1, 3, 2, 4});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setRotated(true);
        paint(c);
        Shape lineShape = implementation.getLastDrawShape();
        Rectangle line = lineShape.getBounds();
        List<float[]> lineVerts = vertices(lineShape);
        Shape fillShape = implementation.getLastFillShape();
        Rectangle fill = fillShape.getBounds();
        assertEquals(line.getY(), fill.getY(), 1, "fill top " + fill + " line " + line);
        assertEquals(line.getY() + line.getHeight(), fill.getY() + fill.getHeight(), 1, "fill bottom " + fill + " line " + line);
        // every vertex of the area is a vertex of the line or sits on the vertical baseline at
        // the left; closing it with a horizontal pixel used as a y left vertices elsewhere
        for (float[] v : vertices(fillShape)) {
            boolean onBaseline = Math.abs(v[0] - fill.getX()) <= 1;
            boolean onLine = false;
            for (float[] l : lineVerts) {
                if (Math.abs(l[0] - v[0]) <= 0.5f && Math.abs(l[1] - v[1]) <= 0.5f) {
                    onLine = true;
                    break;
                }
            }
            if (onBaseline) {
                boolean rowOfLine = false;
                for (float[] l : lineVerts) {
                    if (Math.abs(l[1] - v[1]) <= 0.5f) {
                        rowOfLine = true;
                        break;
                    }
                }
                assertTrue(rowOfLine || onLine, "a baseline vertex stays on a category row: " + v[0] + "," + v[1]);
            } else {
                assertTrue(onLine, "stray area vertex " + v[0] + "," + v[1]);
            }
        }
    }

    private static List<float[]> vertices(Shape shape) {
        List<float[]> out = new ArrayList<float[]>();
        PathIterator it = shape.getPathIterator();
        float[] c = new float[6];
        while (!it.isDone()) {
            int seg = it.currentSegment(c);
            if (seg == PathIterator.SEG_MOVETO || seg == PathIterator.SEG_LINETO) {
                out.add(new float[]{c[0], c[1]});
            }
            it.next();
        }
        return out;
    }

    @FormTest
    public void setStackGroupTweensFromTheScreenNotAnOldTransition() {
        LineSeries s = new LineSeries("s");
        s.setValues(1, 1, 1);
        Chart c = showing(new Chart().addSeries(s), 100000);
        AnimationTime.setTime(103000);
        s.setValues(10, 10, 10);
        settle(c, 105000);
        AnimationTime.setTime(108000);
        s.setValues(20, 20, 20);
        settle(c, 110000);
        AnimationTime.setTime(110000);
        s.setStackGroup("g");
        AnimationTime.setTime(110300);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() < 1, "positive control: mid transition");
        assertEquals(20, s.animatedY(ctx, 0), 1e-9, "nothing moved, so nothing replays an old tween");
    }

    @FormTest
    public void setSecondaryAxisTweensFromTheScreenNotAnOldTransition() {
        LineSeries s = new LineSeries("s");
        s.setValues(1, 1, 1);
        Chart c = showing(new Chart().addSeries(s), 200000);
        AnimationTime.setTime(203000);
        s.setValues(20, 20, 20);
        settle(c, 205000);
        AnimationTime.setTime(205000);
        s.setSecondaryAxis(true);
        AnimationTime.setTime(205300);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() < 1, "positive control: mid transition");
        assertEquals(20, s.animatedY(ctx, 1), 1e-9);
    }

    private static List<Integer> arcWidths(List<Object[]> drawn) {
        List<Integer> out = new ArrayList<Integer>();
        for (Object[] call : drawn) {
            if ("fillArc".equals(call[0])) {
                out.add((Integer) call[3]);
            }
        }
        return out;
    }

    @FormTest
    public void existingBubblesKeepTheirSizeWhenAPointIsAdded() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 2}, new double[]{5, 6}, new double[]{4, 9});
        Chart c = showing(new Chart().addSeries(s), 300000);
        List<Integer> before = arcWidths(paint(c));
        AnimationTime.setTime(310000);
        s.add(3, 7, 1);
        AnimationTime.setTime(310100);
        List<Integer> mid = arcWidths(paint(c));
        assertTrue(c.getLastContext().getProgress() < 0.5f, "positive control: early in the transition");
        assertTrue(before.size() >= 2 && mid.size() >= 2, before + " " + mid);
        assertEquals(before.get(0), mid.get(0), "the first bubble did not change");
        assertEquals(before.get(1), mid.get(1), "the second bubble did not change");
    }

    @FormTest
    public void updateLastTweensTheWick() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        Chart c = showing(new Chart().addSeries(cs), 400000);
        AnimationTime.setTime(410000);
        cs.updateLast(10);
        AnimationTime.setTime(410300);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() < 1, "positive control: mid transition");
        double high = cs.animatedAux(ctx, HIGH, 1);
        assertTrue(high > 4 && high < 10, "the high moves from 4 toward 10, it does not snap: " + high);
        assertEquals(10, cs.animatedAux(null, HIGH, 1), 1e-9);
    }

    @FormTest
    public void setWidensTheCandleToTheNewClose() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        cs.set(1, 100);
        assertEquals(100, cs.animatedAux(null, HIGH, 1), 1e-9, "the high includes the close");
        cs.set(1, -5);
        assertEquals(-5, cs.animatedAux(null, 2, 1), 1e-9, "the low includes the close");
        assertEquals(2, cs.animatedAux(null, 0, 1), 1e-9, "the open stays");
    }

    @FormTest
    public void stackedBaseSlidesWhileTheLayerBelowFades() {
        LineSeries a = new LineSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setValues(5, 5, 5).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 500000);
        assertEquals(10, b.getBase(0), 1e-9, "positive control");
        AnimationTime.setTime(510000);
        a.setVisible(false);
        AnimationTime.setTime(510300);
        paint(c);
        double base = b.getBase(0);
        assertTrue(base > 0 && base < 10, "mid fade the base is between: " + base);
        settle(c, 515000);
        assertEquals(0, b.getBase(0), 1e-9);
    }

    @FormTest
    public void denseValueLabelsAreBoundedByThePlot() {
        int n = 10000;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            y[i] = i % 17;
        }
        LineSeries s = new LineSeries("s");
        s.setData(null, y);
        s.setShowValues(true);
        final int[] formatted = new int[1];
        s.setValueFormatter(new ValueFormatter() {
            @Override
            public String format(double value, double step) {
                formatted[0]++;
                return "v";
            }
        });
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        formatted[0] = 0;
        paint(c);
        assertTrue(formatted[0] > 0, "positive control: labels are offered");
        assertTrue(formatted[0] <= W, "one label per pixel at most, not one per point: " + formatted[0]);
    }
}
