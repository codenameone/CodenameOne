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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.LineSeriesSplineProbe;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.chart.series.XYSeries;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Eleventh review round (series half): one test per finding.
public class ChartReviewRound11aTest extends UITestBase {

    private static void size(Chart c, int w, int h) {
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    private static ChartContext paint(Chart c) {
        c.paint(Image.createImage(c.getWidth(), c.getHeight()).getGraphics());
        return c.getLastContext();
    }

    private List<Object[]> calls(Chart c) {
        implementation.setRecordDrawCalls(true);
        implementation.setShapeSupported(true);
        try {
            c.paint(Image.createImage(c.getWidth(), c.getHeight()).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
            implementation.setShapeSupported(false);
        }
    }

    private List<Object[]> strings(Chart c) {
        implementation.setRecordDrawnStrings(true);
        try {
            c.paint(Image.createImage(c.getWidth(), c.getHeight()).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawnStrings());
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }

    private static int v(Object[] call, int k) {
        return ((Integer) call[k]).intValue();
    }

    // settles the entry animation, changes point `changed` through `change`, and returns a context
    // half way through the transition that follows
    private static ChartContext midTransition(Chart c, Runnable change) {
        c.setAnimationDuration(400);
        AnimationTime.setTime(100000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        size(c, 600, 400);
        paint(c);
        AnimationTime.setTime(102000);
        c.animate();
        paint(c);
        change.run();
        AnimationTime.setTime(102000);
        paint(c);
        AnimationTime.setTime(102200);
        c.animate();
        ChartContext ctx = paint(c);
        float t = ctx.getProgress();
        assertTrue(t > 0 && t < 1, "the change is mid transition: " + t);
        return ctx;
    }

    private static void assertOnlyLastMoves(XYSeries s, ChartContext ctx, int channels, String what) {
        int last = s.size() - 1;
        for (int i = 0; i < last; i++) {
            assertEquals(s.getY(i), s.animatedY(ctx, i), 1e-9, what + " " + i + " value stays still");
            for (int k = 0; k < channels; k++) {
                assertEquals(s.animatedAux(null, k, i), s.animatedAux(ctx, k, i), 1e-9,
                        what + " " + i + " channel " + k + " stays still");
            }
        }
        // positive control: the changed point really is moving
        assertTrue(Math.abs(s.animatedY(ctx, last) - s.getY(last)) > 1e-6, what + " that changed is moving");
    }

    @FormTest
    public void candlesOtherThanTheUpdatedOneStayStill() {
        final CandlestickSeries s = new CandlestickSeries("c");
        for (int i = 0; i < 6; i++) {
            s.addOhlc(i, 10 + i, 14 + i, 8 + i, 12 + i);
        }
        try {
            ChartContext ctx = midTransition(new Chart().addSeries(s), new Runnable() {
                public void run() {
                    s.updateLast(40);
                }
            });
            assertOnlyLastMoves(s, ctx, 3, "candle");
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void boxesOtherThanTheChangedOneStayStill() {
        final BoxPlotSeries s = new BoxPlotSeries("b");
        for (int i = 0; i < 5; i++) {
            s.addBox(i, 1, 3, 5, 7, 9);
        }
        try {
            ChartContext ctx = midTransition(new Chart().addSeries(s), new Runnable() {
                public void run() {
                    s.set(4, 8);
                }
            });
            assertOnlyLastMoves(s, ctx, 4, "box");
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void errorBarsOtherThanTheChangedOneStayStillAndTheMarkerFollows() {
        final ErrorBarSeries s = new ErrorBarSeries("e").setMarkers(true);
        s.setIntervals(new double[]{0, 1, 2, 3}, new double[]{5, 6, 7, 8}, new double[]{4, 5, 6, 7},
                new double[]{6, 7, 8, 9});
        try {
            Chart c = new Chart().addSeries(s);
            ChartContext ctx = midTransition(c, new Runnable() {
                public void run() {
                    s.set(3, 2);
                }
            });
            assertOnlyLastMoves(s, ctx, 2, "error bar");
            // the marker sits on the animated value, not the target
            int want = Math.round(ctx.screenY(s, 3, s.animatedY(ctx, 3)));
            int target = Math.round(ctx.screenY(s, 3, s.getY(3)));
            assertTrue(want != target, "positive control: animated and target differ");
            boolean found = false;
            for (Object[] o : calls(c)) {
                if ("fillArc".equals(o[0]) && Math.abs(v(o, 2) + v(o, 4) / 2 - want) <= 1) {
                    found = true;
                }
            }
            assertTrue(found, "a marker is drawn at the animated value");
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void waterfallTotalsFollowTheirBarsThroughSlides() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setSteps(new double[]{10, 5, 0, 3, 0}, new boolean[]{false, false, true, false, true});
        assertTrue(w.isTotal(2));
        assertEquals(15, w.getY(2), 0);
        w.setMaxPoints(4);
        assertEquals(4, w.size());
        assertTrue(w.isTotal(1), "the total moved with its bar");
        assertFalse(w.isTotal(2));
        assertTrue(w.isTotal(3));
        assertEquals(15, w.getY(1), 0, "the total keeps the rows that slid off");
        assertEquals(18, w.getY(3), 0);
        w.add(4, 1);
        assertTrue(w.isTotal(0), "slid one more place");
        assertFalse(w.isTotal(3), "an added step is not a total");
        assertEquals(15, w.getBase(1), 0, "a step after a total starts on it");
        assertEquals(18, w.getY(2), 0, "the total after the slide is the real cumulative sum");
        assertEquals(18, w.getBase(3), 0);
    }

    @FormTest
    public void stackedLineLabelsRideTheAnimatedBase() {
        final LineSeries a = (LineSeries) new LineSeries("a").setData(null, new double[]{10, 10, 10});
        a.setStackGroup("g");
        LineSeries b = (LineSeries) new LineSeries("b").setData(null, new double[]{5, 5, 5});
        b.setStackGroup("g");
        b.setShowValues(true);
        try {
            Chart c = new Chart().addSeries(a).addSeries(b);
            ChartContext ctx = midTransition(c, new Runnable() {
                public void run() {
                    a.set(1, 30);
                }
            });
            double animTop = b.animatedBase(ctx, 1) + b.animatedY(ctx, 1);
            double rawTop = b.getBase(1) + b.animatedY(ctx, 1);
            assertTrue(Math.abs(animTop - rawTop) > 1, "positive control: the base is moving");
            int ay = Math.round(ctx.screenY(b, 1, animTop));
            int ry = Math.round(ctx.screenY(b, 1, rawTop));
            assertTrue(Math.abs(ay - ry) > 4, "positive control: the two bases are pixels apart");
            int px = Math.round(ctx.screenX(b, 1, animTop));
            Object[] l1 = null;
            for (Object[] o : strings(c)) {
                if ("5".equals(o[0]) && (l1 == null || Math.abs(v(o, 1) - px) < Math.abs(v(l1, 1) - px))) {
                    l1 = o;
                }
            }
            assertTrue(l1 != null, "the middle point is labelled");
            // placed above its point, the label's bottom edge sits just over the point it labels
            int bottom = v(l1, 2) + ctx.getFont().getHeight();
            boolean nearAnimated = Math.abs(bottom - ay) < Math.abs(bottom - ry);
            assertTrue(nearAnimated, "the label sits by the animated point, not the settled base");
        } finally {
            AnimationTime.reset();
        }
    }

    @FormTest
    public void rotatedSplineHasNoCusps() {
        // a peak in the middle: drawn rotated, the curve's y (the category) must keep going one way
        LineSeries s = (LineSeries) new LineSeries("s").setData(null, new double[]{1, 9, 2, 8, 3});
        s.setMode(LineSeries.SPLINE);
        Chart c = new Chart().addSeries(s).setRotated(true);
        c.setAnimationDuration(0);
        size(c, 400, 600);
        boolean shape = false;
        for (Object[] o : calls(c)) {
            shape |= "drawShape".equals(o[0]);
        }
        assertTrue(shape, "the rotated spline paints");
        // rotated geometry: the category runs down (y), the value zigzags along x
        float[] px = {10, 90, 20, 80, 30};
        float[] py = {0, 50, 100, 150, 200};
        assertTrue(doublesBack(LineSeriesSplineProbe.cubics(px, py, false), py),
                "positive control: tangents against x make the curve double back along y");
        assertFalse(doublesBack(LineSeriesSplineProbe.cubics(px, py, true), py),
                "with tangents against y every segment keeps going down");
    }

    // true when the curve reverses direction where two segments meet: the tangent arriving at a
    // point and the one leaving it point opposite ways, which draws a cusp
    private static boolean doublesBack(List<float[]> cubics, float[] py) {
        for (int i = 0; i + 1 < cubics.size(); i++) {
            float[] in = cubics.get(i);
            float[] out = cubics.get(i + 1);
            float ix = in[4] - in[2];
            float iy = in[5] - in[3];
            float ox = out[0] - in[4];
            float oy = out[1] - in[5];
            if (ix * ox + iy * oy < 0) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    public void rotatedCandlesHaveBodies() {
        CandlestickSeries s = new CandlestickSeries("c");
        for (int i = 0; i < 4; i++) {
            s.addOhlc(i, 10, 20, 5, 15);
        }
        Chart c = new Chart().addSeries(s).setRotated(true);
        c.setAnimationDuration(0);
        size(c, 600, 400);
        paint(c);
        ChartContext ctx = c.getLastContext();
        int bodyLen = Math.round(Math.abs(ctx.screenX(s, 0, 15) - ctx.screenX(s, 0, 10)));
        assertTrue(bodyLen > 5, "positive control: open to close spans pixels horizontally");
        int bodies = 0;
        for (Object[] o : calls(c)) {
            if ("fillRect".equals(o[0]) && Math.abs(v(o, 3) - bodyLen) <= 1 && v(o, 4) > 3) {
                bodies++;
            }
        }
        assertEquals(4, bodies, "each rotated candle body runs along x and is thick along y");
    }
}
