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
import com.codename1.ui.chart.axis.TimeAxisAccess;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import java.lang.reflect.Method;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Thirteenth review round of the chart package: one test per finding.
public class ChartReviewRound13Test extends UITestBase {

    private void place(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    @FormTest
    public void everyBarSeriesComputesTheSameBandWhileOneFades() throws Exception {
        BarSeries wide = new BarSeries("wide");
        wide.setData(new double[]{0, 2, 4, 6}, new double[]{1, 2, 3, 4});
        BarSeries dense = new BarSeries("dense");
        dense.setData(new double[]{0, 1, 2, 3, 4, 5, 6}, new double[]{1, 1, 1, 1, 1, 1, 1});
        Chart c = new Chart().addSeries(wide).addSeries(dense).setAnimationDuration(0);
        place(c, 600, 400);
        c.paint(Image.createImage(600, 400).getGraphics());
        ChartContext ctx = c.getLastContext();
        Method m = BarSeries.class.getDeclaredMethod("bandPixels", ChartContext.class);
        m.setAccessible(true);
        float full = ((Float) m.invoke(wide, ctx)).floatValue();
        ((Series) dense).visibility = 0.5f;
        float a = ((Float) m.invoke(wide, ctx)).floatValue();
        float b = ((Float) m.invoke(dense, ctx)).floatValue();
        ((Series) dense).visibility = 1;
        assertTrue(a > full, "positive control: the fade widens the band");
        assertEquals(a, b, 0.0001f, "the fading series must use the same band as the others");
    }

    @FormTest
    public void hitsAfterExportUseScreenGeometry() throws Exception {
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        a.add(new TreeNode("a1", 1)).add(new TreeNode("a2", 2));
        root.add(a).add(new TreeNode("B", 3));
        SunburstSeries s = new SunburstSeries("s", root);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        place(c, 300, 500);
        c.paint(Image.createImage(300, 500).getGraphics());
        float cx = ChartReviewRound12Test.fieldOf(s, "cx");
        float cy = ChartReviewRound12Test.fieldOf(s, "cy");
        float outer = ChartReviewRound12Test.fieldOf(s, "outer");
        ChartContext ctx = c.getLastContext();
        int px = Math.round(cx + outer * 0.8f);
        int py = Math.round(cy);
        ChartPoint before = s.hitTest(ctx, px - ctx.getOriginX(), py - ctx.getOriginY(), 2);
        assertNotNull(before, "positive control: the ring is hit");
        assertNotNull(c.toImage(1080, 1080));
        assertEquals(cx, ChartReviewRound12Test.fieldOf(s, "cx"), 0.01f);
        assertEquals(cy, ChartReviewRound12Test.fieldOf(s, "cy"), 0.01f);
        assertEquals(outer, ChartReviewRound12Test.fieldOf(s, "outer"), 0.01f);
        ctx = c.getLastContext();
        ChartPoint after = s.hitTest(ctx, px - ctx.getOriginX(), py - ctx.getOriginY(), 2);
        assertNotNull(after);
        assertEquals(before.getIndex(), after.getIndex());
        assertEquals(before.getLabel(), after.getLabel());
    }

    private static int[] pixels(Image img) {
        return img.getRGB();
    }

    @FormTest
    public void exportMidTransitionRendersTheSettledChart() {
        BarSeries settledS = new BarSeries("s");
        settledS.setData(new double[]{0, 1, 2}, new double[]{5, 9, 2});
        Chart settled = new Chart().addSeries(settledS).setAnimationDuration(0);
        place(settled, 400, 300);
        settled.paint(Image.createImage(400, 300).getGraphics());
        int[] expected = pixels(settled.toImage(400, 300));

        BarSeries moving = new BarSeries("s");
        moving.setData(new double[]{0, 1, 2}, new double[]{1, 1, 1});
        Chart c = new Chart().addSeries(moving).setAnimationDuration(100000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        moving.setData(new double[]{0, 1, 2}, new double[]{5, 9, 2});
        c.paint(Image.createImage(400, 300).getGraphics());
        assertTrue(c.getLastContext().getProgress() < 1, "positive control: a transition is running");
        int[] got = pixels(c.toImage(400, 300));
        assertTrue(c.getLastContext().getProgress() < 1, "the on-screen transition keeps running");
        assertTrue(Arrays.equals(expected, got), "the export shows the settled chart");
    }

    @FormTest
    public void steppedDownTimeTicksSpanTheRange() {
        long day = 24L * 3600 * 1000;
        long t0 = 1700000000000L;
        int[] field = new int[1];
        for (int maxCount = 2; maxCount <= 3; maxCount++) {
            for (long span = day / 2; span < 400 * day; span = span * 3 / 2) {
                double[] t = TimeAxisAccess.ticks(t0, t0 + span, maxCount, field);
                assertTrue(t.length >= 2, "two ticks at span " + span);
                double gapRight = t0 + span - t[t.length - 1];
                double step = t[1] - t[0];
                assertTrue(gapRight <= step * 1.2 + 1, "ticks reach the right side at span " + span
                        + ": " + Arrays.toString(t));
            }
        }
    }

    @FormTest
    public void infiniteWaterfallDeltaIsAGap() {
        WaterfallSeries w = new WaterfallSeries("w");
        w.setSteps(new double[]{5, Double.POSITIVE_INFINITY, 2, 0}, new boolean[]{false, false, false, true});
        assertEquals(5, w.getBase(2), 0);
        assertEquals(7, w.getY(3), 0);
        assertFalse(Double.isInfinite(w.getBase(3)));
        assertArrayEquals(new double[]{0, 5}, new double[]{w.getBase(0), w.getBase(1)}, 0);
    }
}
