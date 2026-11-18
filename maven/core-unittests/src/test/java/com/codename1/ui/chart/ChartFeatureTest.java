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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Brushing, the loading state, axis variants and label legibility on painted charts.
public class ChartFeatureTest extends UITestBase {

    private static ChartContext paint(Chart c) {
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(600);
        c.setHeight(400);
        c.paint(Image.createImage(600, 400).getGraphics());
        ChartContext ctx = c.getLastContext();
        assertNotNull(ctx);
        return ctx;
    }

    private static void assertLabelsDisjoint(ChartContext ctx) {
        LabelLayout l = ctx.getLabels();
        int n = l.size();
        for (int i = 0; i < n; i++) {
            int[] a = l.getRect(i);
            for (int j = i + 1; j < n; j++) {
                int[] b = l.getRect(j);
                boolean overlap = a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
                assertFalse(overlap, "labels " + i + " and " + j + " overlap");
            }
        }
    }

    @FormTest
    public void denseLabelsNeverOverlap() {
        String[] names = new String[40];
        double[] v = new double[40];
        for (int i = 0; i < names.length; i++) {
            names[i] = "Category with a long name " + i;
            v[i] = (i * 37) % 23 + 1;
        }
        BarSeries bars = new BarSeries("b");
        bars.setShowValues(true);
        bars.setData(null, v);
        Chart c = new Chart().setXAxis(new CategoryAxis(Axis.BOTTOM, names)).addSeries(bars);
        assertLabelsDisjoint(paint(c));
    }

    @FormTest
    public void tinyPieSlicesNeverOverlap() {
        PieSeries p = new PieSeries("p");
        String[] names = new String[14];
        double[] v = new double[14];
        for (int i = 0; i < names.length; i++) {
            names[i] = "Slice " + i;
            v[i] = i < 10 ? 0.4 : 30;
        }
        p.setData(names, v);
        assertLabelsDisjoint(paint(new Chart().addSeries(p)));
    }

    @FormTest
    public void brushSelectsAndZooms() {
        double[] y = new double[100];
        for (int i = 0; i < y.length; i++) {
            y[i] = i % 7;
        }
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, y));
        c.setBrushEnabled(true);
        ChartContext ctx = paint(c);
        final double[] got = new double[2];
        c.addRangeListener((chart, from, to) -> {
            got[0] = from;
            got[1] = to;
        });
        int x0 = ctx.getPlotX() + ctx.getPlotWidth() / 4;
        int x1 = ctx.getPlotX() + ctx.getPlotWidth() / 2;
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        c.pointerPressed(x0, py);
        c.pointerDragged(new int[]{x0 + 20}, new int[]{py});
        c.pointerDragged(new int[]{x1}, new int[]{py});
        c.pointerReleased(x1, py);
        assertTrue(got[1] > got[0]);
        assertTrue(got[0] > 15 && got[0] < 35, "from " + got[0]);
        assertTrue(got[1] > 40 && got[1] < 60, "to " + got[1]);
    }

    @FormTest
    public void loadingStateIsDescribed() {
        Chart c = new Chart().setLoading("Loading prices");
        assertTrue(c.isLoading());
        paint(c);
        assertTrue(c.describe().contains("Loading prices"));
        c.setLoading(null);
        assertFalse(c.isLoading());
    }

    @FormTest
    public void logAxisTicksArePowers() {
        LogAxis a = new LogAxis(Axis.LEFT);
        a.setDataBounds(1, 100000);
        double[] t = a.ticks(8);
        assertTrue(t.length >= 3);
        for (double v : t) {
            double e = Math.log10(v);
            assertEquals(Math.rint(e), e, 1e-9, "tick " + v);
        }
    }

    @FormTest
    public void categoryAxisCoversEveryBand() {
        CategoryAxis a = new CategoryAxis(Axis.BOTTOM, "a", "b", "c");
        assertEquals(3, a.getCategoryCount());
        a.setDataBounds(0, 2);
        double[] t = a.ticks(10);
        assertEquals(3, t.length);
        assertTrue(a.toNormalized(0) > 0 && a.toNormalized(2) < 1);
    }
}
