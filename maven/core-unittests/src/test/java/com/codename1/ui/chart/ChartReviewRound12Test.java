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
import com.codename1.ui.chart.axis.TimeAxisAccess;
import com.codename1.ui.chart.layout.Squarify;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twelfth review round of the chart package: one test per finding.
public class ChartReviewRound12Test extends UITestBase {

    private void paint(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        c.paint(Image.createImage(w, h).getGraphics());
    }

    static float fieldOf(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return ((Number) f.get(o)).floatValue();
    }

    @FormTest
    public void drilledSunburstGoesUpOnlyFromTheCentreOrJustOutsideTheRings() throws Exception {
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        a.add(new TreeNode("a1", 1)).add(new TreeNode("a2", 2));
        root.add(a).add(new TreeNode("B", 3));
        SunburstSeries s = new SunburstSeries("s", root);
        Chart c = new Chart().setTitle("A title").addSeries(s);
        paint(c, 400, 600);
        s.setCurrent(a);
        paint(c, 400, 600);
        ChartContext ctx = c.getLastContext();
        float cx = fieldOf(s, "cx");
        float cy = fieldOf(s, "cy");
        float outer = fieldOf(s, "outer");
        assertTrue(cy - outer > 40, "positive control: the title area lies well outside the rings");
        // the title row, far above the rings
        assertNull(s.hitTest(ctx, Math.round(cx) - ctx.getOriginX(), 3 - ctx.getOriginY(), 2),
                "a tap on the title must not drill up");
        // the centre still goes up
        ChartPoint p = s.hitTest(ctx, Math.round(cx) - ctx.getOriginX(), Math.round(cy) - ctx.getOriginY(), 2);
        assertNotNull(p, "the centre goes back up");
        assertTrue(s.handleTap(p));
        assertEquals(root, s.getCurrent());
        // and so does a tap just past the outer ring
        s.setCurrent(a);
        paint(c, 400, 600);
        ctx = c.getLastContext();
        p = s.hitTest(ctx, Math.round(cx + outer + 2) - ctx.getOriginX(), Math.round(cy) - ctx.getOriginY(), 2);
        assertNotNull(p, "just outside the rings still goes up");
        assertEquals(-1, p.getIndex());
    }

    @FormTest
    public void subSecondSpansGetMillisecondTicks() {
        long t0 = 1700000000123L;
        int[] field = new int[1];
        double[] t = TimeAxisAccess.ticks(t0, t0 + 400, 6, field);
        assertTrue(t.length >= 2 && t.length <= 6, "400ms gives 2..6 ticks, got " + t.length);
        assertEquals(Calendar.MILLISECOND, field[0]);
        for (int i = 1; i < t.length; i++) {
            assertTrue(t[i] > t[i - 1]);
        }
        assertTrue(TimeAxisAccess.pattern(field[0]).indexOf("SSS") > 0);
        t = TimeAxisAccess.ticks(t0, t0 + 5, 6, field);
        assertTrue(t.length >= 2, "5ms gives at least two ticks, got " + t.length);
        assertTrue(t[0] >= t0 && t[t.length - 1] <= t0 + 5);
        // a span just under a step still has two ticks
        t = TimeAxisAccess.ticks(t0 + 1, t0 + 999, 2, field);
        assertTrue(t.length >= 2, "a near-second span still has two ticks");
    }

    @FormTest
    public void barBandNarrowsSmoothlyWhileASeriesFades() throws Exception {
        BarSeries wide = new BarSeries("wide");
        wide.setData(new double[]{0, 2, 4, 6}, new double[]{1, 2, 3, 4});
        BarSeries dense = new BarSeries("dense");
        dense.setData(new double[]{0, 1, 2, 3, 4, 5, 6}, new double[]{1, 1, 1, 1, 1, 1, 1});
        Chart c = new Chart().addSeries(wide).addSeries(dense);
        paint(c, 600, 400);
        ChartContext ctx = c.getLastContext();
        Method m = BarSeries.class.getDeclaredMethod("bandPixels", ChartContext.class);
        m.setAccessible(true);
        float with = ((Float) m.invoke(wide, ctx)).floatValue();
        ((Series) dense).visibility = 0;
        float without = ((Float) m.invoke(wide, ctx)).floatValue();
        ((Series) dense).visibility = 0.5f;
        float mid = ((Float) m.invoke(wide, ctx)).floatValue();
        ((Series) dense).visibility = 0.001f;
        float nearZero = ((Float) m.invoke(wide, ctx)).floatValue();
        ((Series) dense).visibility = 1;
        assertTrue(without > with, "positive control: the dense series narrows the band");
        assertTrue(mid > with && mid < without, "mid fade lies strictly between: " + with + " " + mid + " " + without);
        assertTrue(without - nearZero < (without - with) * 0.01f, "no snap as the fade reaches zero");
    }

    @FormTest
    public void infiniteValuesDoNotPoisonTreemapOrSparkline() {
        float[][] r = Squarify.layout(new double[]{Double.POSITIVE_INFINITY, 3, 1, Double.NaN}, 0, 0, 100, 100);
        for (float[] cell : r) {
            for (float v : cell) {
                assertFalse(Float.isNaN(v) || Float.isInfinite(v), "every rect is finite");
            }
        }
        assertEquals(0f, r[0][2] * r[0][3], 0.001f);
        assertEquals(7500f, r[1][2] * r[1][3], 1f);
        Sparkline sp = new Sparkline(new double[]{1, Double.POSITIVE_INFINITY, 3, Double.NEGATIVE_INFINITY, 2});
        implementation.setShapeSupported(true);
        sp.setX(0);
        sp.setY(0);
        sp.setWidth(200);
        sp.setHeight(40);
        sp.paint(Image.createImage(200, 40).getGraphics());
        assertEquals(3, sp.getLastSegments(), "infinities are gaps, leaving three runs");
    }

    @FormTest
    public void waterfallSetDataKeepsTotalsByPosition() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setSteps(new double[]{10, -3, 0}, new boolean[]{false, false, true});
        assertTrue(w.isTotal(2));
        w.setData(null, new double[]{20, 5, 0, 1});
        assertTrue(w.isTotal(2), "the marked total survives new data");
        assertFalse(w.isTotal(3));
        assertEquals(25, w.getY(2), 1e-9);
        w.set(2, 99);
        assertEquals(25, w.getY(2), 1e-9, "a total's value is computed");
    }

    @FormTest
    public void duplicateLabelsKeepTheirOwnHiddenState() {
        PieSeries p = new PieSeries("p");
        p.setData(new String[]{"X", "Y", "X"}, new double[]{1, 2, 3});
        p.toggleLegendEntry(2);
        assertFalse(p.isLegendEntryVisible(2));
        assertTrue(p.isLegendEntryVisible(0));
        // a new part pushed in front: the second X is still the hidden one
        p.setData(new String[]{"Z", "X", "Y", "X"}, new double[]{1, 1, 2, 3});
        assertTrue(p.isLegendEntryVisible(1), "the first X stays shown");
        assertFalse(p.isLegendEntryVisible(3), "the second X stays hidden");
    }
}
