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
import com.codename1.ui.chart.axis.TimeAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    /// Paints the chart and asserts that no two drawn strings overlap and every one is inside it.
    private void assertDrawnTextLegible(Chart c) {
        implementation.setRecordDrawnStrings(true);
        try {
            paint(c);
            List<Object[]> drawn = implementation.getDrawnStrings();
            List<int[]> boxes = new ArrayList<int[]>();
            for (Object[] d : drawn) {
                String str = (String) d[0];
                if (str.trim().length() == 0) {
                    continue;
                }
                int w = implementation.stringWidth(d[3], str);
                int h = implementation.getHeight(d[3]);
                int x = ((Integer) d[1]).intValue();
                int y = ((Integer) d[2]).intValue();
                assertTrue(x >= 0 && y >= 0 && x + w <= 600 && y + h <= 400, "outside: " + str);
                for (int[] b : boxes) {
                    boolean overlap = x < b[0] + b[2] && b[0] < x + w && y < b[1] + b[3] && b[1] < y + h;
                    assertFalse(overlap, "'" + str + "' overlaps another label");
                }
                boxes.add(new int[]{x, y, w, h});
            }
            assertTrue(boxes.size() > 3, "too few labels drawn: " + boxes.size());
        } finally {
            implementation.setRecordDrawnStrings(false);
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
        assertDrawnTextLegible(c);
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
        assertDrawnTextLegible(new Chart().addSeries(p));
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
        // the zoom animates to the selection
        long end = System.currentTimeMillis() + 2000;
        while (Math.abs(c.getXAxis().getVisibleMax() - got[1]) > 1e-6 && System.currentTimeMillis() < end) {
            c.animate();
        }
        assertEquals(got[0], c.getXAxis().getVisibleMin(), 1e-6);
        assertEquals(got[1], c.getXAxis().getVisibleMax(), 1e-6);
    }

    @FormTest
    public void brushWithoutZoomLeavesTheViewport() {
        Chart c = new Chart().addSeries(new LineSeries("l").setData(null, new double[]{1, 4, 2, 5, 3, 6, 2, 7}));
        c.setBrushEnabled(true).setBrushZooms(false);
        ChartContext ctx = paint(c);
        double min = c.getXAxis().getVisibleMin();
        double max = c.getXAxis().getVisibleMax();
        final int[] calls = new int[1];
        c.addRangeListener((chart, from, to) -> calls[0]++);
        int py = ctx.getPlotY() + ctx.getPlotHeight() / 2;
        c.pointerPressed(ctx.getPlotX() + 10, py);
        c.pointerDragged(new int[]{ctx.getPlotX() + 60}, new int[]{py});
        c.pointerDragged(new int[]{ctx.getPlotX() + 200}, new int[]{py});
        c.pointerReleased(ctx.getPlotX() + 200, py);
        for (int i = 0; i < 20; i++) {
            c.animate();
        }
        assertEquals(1, calls[0]);
        assertEquals(min, c.getXAxis().getVisibleMin(), 1e-9);
        assertEquals(max, c.getXAxis().getVisibleMax(), 1e-9);
    }

    @FormTest
    public void timeAxisMonthTicksNameDistinctMonths() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.MARCH, 3, 0, 0, 0);
        double from = cal.getTime().getTime();
        cal.set(2025, Calendar.SEPTEMBER, 20, 0, 0, 0);
        double to = cal.getTime().getTime();
        // the base manager, whose long date style is Date.toString(): the month must not be read
        // from the weekday that string opens with
        com.codename1.l10n.L10NManager previous = implementation.getLocalizationManager();
        implementation.setLocalizationManager(new com.codename1.l10n.L10NManager("en", "US") {
        });
        try {
            assertMonthTicks(from, to);
        } finally {
            implementation.setLocalizationManager(previous);
        }
    }

    private static void assertMonthTicks(double from, double to) {
        TimeAxis a = new TimeAxis(Axis.BOTTOM);
        a.setDataBounds(from, to);
        double[] t = a.ticks(8);
        assertTrue(t.length >= 4, "ticks " + t.length);
        String prev = null;
        for (double v : t) {
            String label = a.format(v, 1);
            assertTrue(label.endsWith("2025"), label);
            assertFalse(label.equals(prev), "repeated label " + label);
            prev = label;
        }
        assertTrue(a.format(t[0], 1).startsWith("Apr"), a.format(t[0], 1));
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

    @FormTest
    public void partUIIDsOverrideThePalette() {
        ChartTheme plain = new Chart().getTheme();
        java.util.Hashtable<String, Object> props = new java.util.Hashtable<String, Object>();
        props.put("ChartAxis.fgColor", "ff0000");
        props.put("ChartTooltip.bgColor", "00ff00");
        com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(props);
        ChartTheme t = new Chart().getTheme();
        assertEquals(0xff0000, t.getAxisColor());
        assertEquals(0x00ff00, t.getTooltipBackground());
        // parts the theme leaves out keep the palette's colours
        assertEquals(plain.getGridColor(), t.getGridColor());
        assertEquals(plain.getLegendTextColor(), t.getLegendTextColor());
    }

    @FormTest
    public void rtlLegendRunsFromTheRightAndStillToggles() {
        LineSeries alpha = new LineSeries("Alpha");
        alpha.setData(null, new double[]{1, 2, 3});
        LineSeries beta = new LineSeries("Beta");
        beta.setData(null, new double[]{3, 2, 1});
        Chart c = new Chart().addSeries(alpha).addSeries(beta);
        c.setRTL(true);
        implementation.setRecordDrawnStrings(true);
        int ax = -1;
        int ay = -1;
        int bx = -1;
        try {
            paint(c);
            for (Object[] d : implementation.getDrawnStrings()) {
                if ("Alpha".equals(d[0])) {
                    ax = ((Integer) d[1]).intValue();
                    ay = ((Integer) d[2]).intValue();
                } else if ("Beta".equals(d[0])) {
                    bx = ((Integer) d[1]).intValue();
                }
            }
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
        assertTrue(ax > bx && bx >= 0, "Alpha at " + ax + ", Beta at " + bx);
        c.pointerPressed(ax + 2, ay + 2);
        c.pointerReleased(ax + 2, ay + 2);
        assertFalse(alpha.isLegendEntryVisible(0));
        assertTrue(beta.isLegendEntryVisible(0));
    }
}
