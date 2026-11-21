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
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twentieth review round of the chart package.
public class ChartReviewRound20Test extends UITestBase {

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

    private void paint(com.codename1.ui.Component c) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
    }

    // moves to `t`, lets the chart advance its transition and paints that frame
    private ChartContext frame(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        paint(c);
        return c.getLastContext();
    }

    @FormTest
    public void setMovesTheDerivedCandleChannels() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.add(0, 5);
        cs.set(0, 9);
        for (int k = 0; k < 3; k++) {
            assertEquals(9, cs.animatedAux(null, k, 0), 1e-9, "derived channel " + k + " follows the close");
        }
    }

    @FormTest
    public void setMovesTheDerivedErrorBar() {
        ErrorBarSeries e = new ErrorBarSeries("e");
        e.add(0, 5);
        e.set(0, 9);
        assertEquals(9, e.animatedAux(null, 0, 0), 1e-9);
        assertEquals(9, e.animatedAux(null, 1, 0), 1e-9);
    }

    @FormTest
    public void boundsKeepACloseOrMedianSetOutsideTheRange() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        cs.set(1, 100);
        assertTrue(cs.getYBounds()[1] >= 100, "the close stays on the axis");
        BoxPlotSeries b = new BoxPlotSeries("b");
        b.addBox(1, 0, 5, 5, 8, 10);
        b.set(0, 50);
        assertTrue(b.getYBounds()[1] >= 50, "the median stays on the axis");
    }

    @FormTest
    public void removingALowerLayerSlidesTheBase() {
        LineSeries a = new LineSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setValues(5, 5, 5).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 100000);
        AnimationTime.setTime(110000);
        c.removeSeries(a);
        ChartContext ctx = frame(c, 110300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        double base = b.animatedBase(ctx, 0);
        assertTrue(base > 0 && base < 10, "the base slides from 10 to 0: " + base);
        // hover finds the point where it is drawn
        ChartPoint p = b.getPoint(ctx, 0);
        float drawn = ctx.screenY(b, 0, base + b.animatedY(ctx, 0)) - ctx.getOriginY();
        assertEquals(drawn, p.getScreenY(), 1, "the point is where it is painted");
    }

    @FormTest
    public void joiningAStackGroupSlidesTheBase() {
        LineSeries a = new LineSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setValues(5, 5, 5);
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 200000);
        AnimationTime.setTime(210000);
        b.setStackGroup("g");
        ChartContext ctx = frame(c, 210300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        double base = b.animatedBase(ctx, 0);
        assertTrue(base > 0 && base < 10, "the base slides from 0 to 10: " + base);
    }

    @FormTest
    public void aSignChangeEndsWithoutOverlap() {
        LineSeries a = new LineSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        LineSeries b = new LineSeries("b");
        b.setValues(5, 5, 5).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 300000);
        AnimationTime.setTime(310000);
        a.setValues(-10, -10, -10);
        frame(c, 310300);
        ChartContext ctx = frame(c, 315000);
        assertEquals(0, b.animatedBase(ctx, 0), 1e-9, "a positive layer rests on zero, above the negative one");
        assertEquals(-10, a.animatedBase(ctx, 0) + a.animatedY(ctx, 0), 1e-9);
    }

    @FormTest
    public void aFadingCandleWithAStackGroupIsNotScaled() {
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{10, 12}, new double[]{14, 16}, new double[]{8, 9}, new double[]{12, 14});
        cs.setStackGroup("g");
        LineSeries keep = new LineSeries("keep");
        keep.setValues(1, 2);
        Chart c = showing(new Chart().addSeries(keep).addSeries(cs), 400000);
        AnimationTime.setTime(410000);
        cs.setVisible(false);
        ChartContext ctx = frame(c, 410300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid fade");
        assertEquals(12, cs.animatedY(ctx, 0), 1e-9, "the close is not scaled by the fade");
        assertEquals(10, cs.animatedAux(ctx, 0, 0), 1e-9, "and the open moves with it");
    }

    @FormTest
    public void aBubbleGrowsInOnEntry() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 2}, new double[]{5, 6}, new double[]{4, 9});
        Chart c = new Chart().addSeries(s).setAnimationDuration(1000);
        AnimationTime.setTime(500000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        ChartContext ctx = frame(c, 500300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid entry");
        double size = s.animatedAux(ctx, 0, 1);
        assertTrue(size < 9, "the bubble grows in from nothing: " + size);
    }

    @FormTest
    public void describeCountsValuesAndRanges() {
        LineSeries a = new LineSeries("alpha");
        a.setValues(1, Double.NaN, 3);
        RangeBarSeries r = new RangeBarSeries("range");
        r.addRange(0, 100, 120);
        Chart c = new Chart().addSeries(a).addSeries(r).setAnimationDuration(0);
        String d = c.describe();
        assertTrue(d.contains("alpha: 2 values"), d);
        assertTrue(d.contains(r.formatValue(null, 100)) && d.contains(r.formatValue(null, 120)), "a range reads as its span: " + d);
        a.setVisible(false);
        r.setVisible(false);
        assertTrue(c.describe().contains("No data"), "all hidden reads as empty: " + c.describe());
    }

    @FormTest
    public void annotationAnAxisCannotPlaceHasNoLabel() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2}, new double[]{1, 10, 100});
        // a square-root scale, like a log scale, has no position for a negative value: it
        // normalizes to NaN
        Axis sqrt = new NumberAxis(Axis.LEFT) {
            @Override
            protected double forward(double value) {
                return Math.sqrt(value);
            }

            @Override
            protected double inverse(double t) {
                return t * t;
            }
        };
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setYAxis(sqrt);
        c.addAnnotation(Annotation.valueLine(-5, "zeroline").setIncludeInRange(false));
        c.addAnnotation(Annotation.valueLine(10, "tenline"));
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c);
            List<Object[]> strings = implementation.getDrawnStrings();
            boolean zero = false;
            boolean ten = false;
            for (Object[] st : strings) {
                zero |= "zeroline".equals(st[0]);
                ten |= "tenline".equals(st[0]);
            }
            assertTrue(ten, "positive control: a placeable line is labelled");
            assertFalse(zero, "a line the axis cannot place is not labelled");
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }
}
