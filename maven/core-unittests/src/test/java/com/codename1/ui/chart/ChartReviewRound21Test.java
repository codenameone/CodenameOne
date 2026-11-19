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
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twenty-first review round of the chart package.
public class ChartReviewRound21Test extends UITestBase {

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
    public void anAddedLayerRestsOnThePaintedStack() {
        BarSeries a = new BarSeries("a");
        a.setValues(10, 10, 10).setStackGroup("g");
        BarSeries b = new BarSeries("b");
        b.setValues(10, 10, 10).setStackGroup("g");
        Chart c = showing(new Chart().addSeries(a).addSeries(b), 100000);
        AnimationTime.setTime(110000);
        BarSeries top = new BarSeries("top");
        top.setValues(5, 5, 5).setStackGroup("g");
        c.addSeries(top);
        ChartContext ctx = frame(c, 110300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        double painted = b.animatedBase(ctx, 0) + b.animatedY(ctx, 0);
        assertEquals(20, painted, 1e-6, "positive control: the lower layers are settled");
        assertEquals(painted, top.animatedBase(ctx, 0), 1e-6, "the new layer starts on the stack, not below it");
        assertTrue(top.animatedY(ctx, 0) < 5, "positive control: its own value grows in");
    }

    @FormTest
    public void barHitTestUsesThePaintedRect() {
        BarSeries a = new BarSeries("a");
        a.setValues(10, 10, 10);
        Chart c = showing(new Chart().addSeries(a).setYAxis(fixedAxis()), 100000);
        AnimationTime.setTime(110000);
        a.set(0, 90);
        ChartContext ctx = frame(c, 110300);
        double shown = a.animatedY(ctx, 0);
        assertTrue(shown > 10 && shown < 80, "positive control: mid transition " + shown);
        int x = Math.round(a.getPoint(ctx, 0).getScreenX());
        int inside = Math.round(ctx.screenY(a, 0, shown * 0.8) - ctx.getOriginY());
        int settledOnly = Math.round(ctx.screenY(a, 0, (shown + 90) / 2) - ctx.getOriginY());
        assertNotNull(a.hitTest(ctx, x, inside, 1), "a tap on the painted bar hits it");
        assertNull(a.hitTest(ctx, x, settledOnly, 1), "a tap where the bar only will be misses");
    }

    private static Axis fixedAxis() {
        com.codename1.ui.chart.axis.NumberAxis y = new com.codename1.ui.chart.axis.NumberAxis(Axis.LEFT);
        y.setRange(0.0, 100.0);
        return y;
    }

    @FormTest
    public void scatterInAStackGroupIsNotALayer() {
        ScatterSeries first = new ScatterSeries("s1");
        first.setValues(50, 50).setStackGroup("g");
        BarSeries bar = new BarSeries("b");
        bar.setValues(10, 10).setStackGroup("g");
        ScatterSeries second = new ScatterSeries("s2");
        second.setValues(50, 50).setStackGroup("g");
        new Chart().addSeries(first).addSeries(bar).addSeries(second);
        assertEquals(0, bar.getBase(0), 1e-9, "a scatter below does not lift the bars");
        double[] yb = second.getYBounds();
        assertEquals(50, yb[1], 1e-9, "a scatter above is not lifted onto the bars");
    }

    @FormTest
    public void rangeBarInAStackGroupTweensToItsOwnStart() {
        RangeBarSeries r = new RangeBarSeries("r");
        r.addRange(0, 5, 8);
        r.addRange(1, 5, 8);
        r.setStackGroup("g");
        Chart c = showing(new Chart().addSeries(r), 100000);
        AnimationTime.setTime(110000);
        r.set(0, 6);
        ChartContext ctx = frame(c, 110300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        assertEquals(5, r.getBase(0), 1e-9, "positive control: the settled base is the start");
        assertEquals(5, r.animatedBase(ctx, 0), 1e-6, "the start does not dip toward a stack");
    }

    @FormTest
    public void valueLineAtOrBelowZeroIsSkippedOnALogAxis() {
        BarSeries s = new BarSeries("s");
        s.setValues(1, 10, 100);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setYAxis(new LogAxis(Axis.LEFT));
        c.addAnnotation(Annotation.valueLine(0, "zeroline").setIncludeInRange(false));
        c.addAnnotation(Annotation.valueLine(10, "tenline"));
        c.addAnnotation(Annotation.valueBand(0, 10, "bandlabel").setIncludeInRange(false));
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c);
            boolean zero = false;
            boolean ten = false;
            boolean band = false;
            for (Object[] st : implementation.getDrawnStrings()) {
                zero |= "zeroline".equals(st[0]);
                ten |= "tenline".equals(st[0]);
                band |= "bandlabel".equals(st[0]);
            }
            assertTrue(ten, "positive control: a placeable line is labelled");
            assertTrue(band, "a band reaching below zero is clamped to the axis, not dropped");
            assertFalse(zero, "a line at zero has no place on a log axis");
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }
}
