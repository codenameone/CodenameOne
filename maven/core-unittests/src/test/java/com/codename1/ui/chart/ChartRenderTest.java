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
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.chart.series.WaterfallSeries;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Renders every chart type off screen and checks the accessible description and data table.
public class ChartRenderTest extends UITestBase {

    private Chart render(Chart c) {
        c.setAnimationDuration(0);
        fills(c);
        if (!c.getSeries().isEmpty()) {
            // each series must draw. Hiding every series would compare against the chart's empty
            // state, which draws less for reasons of its own, so a series that painted nothing
            // passed; instead a companion that always paints one rectangle keeps the chart
            // non-empty, and only the series under test is hidden.
            Series[] all = c.getSeries().toArray(new Series[0]);
            Companion companion = new Companion(all[0].isCartesian());
            c.addSeries(companion);
            // no legend, and the companion spans the same data, so hiding the series changes
            // neither the axes nor a legend entry: what disappears is what the series painted
            c.setLegendPosition(Chart.LEGEND_NONE);
            // the bounds of the whole chart with everything shown: a stacked layer hidden lowers
            // the layers above it, and the companion must keep the axes where they were
            companion.xb = union(all, true);
            companion.yb = union(all, false);
            for (Series s : all) {
                int shown = fills(c);
                s.setVisible(false);
                // reset per render, so the assertion speaks for the render with the series hidden
                companion.painted = false;
                int hidden = fills(c);
                s.setVisible(true);
                assertTrue(companion.painted, "the companion keeps the chart non-empty");
                assertTrue(shown > hidden, s.getName() + " drew nothing: " + shown + " calls shown, " + hidden + " with it hidden");
            }
            c.removeSeries(companion);
        }
        return c;
    }

    private static double[] union(Series[] all, boolean x) {
        double[] u = null;
        for (Series s : all) {
            double[] b = x ? s.getXBounds() : s.getYBounds();
            if (b == null) {
                continue;
            }
            if (u == null) {
                u = new double[]{b[0], b[1]};
            } else {
                u[0] = Math.min(u[0], b[0]);
                u[1] = Math.max(u[1], b[1]);
            }
        }
        return u;
    }

    // paints one rectangle in every frame, so hiding the series under test leaves a chart that is
    // still drawing data; a series whose paint does nothing then fails the comparison above
    private static final class Companion extends Series {
        private final boolean cartesian;
        boolean painted;
        double[] xb;
        double[] yb;

        Companion(boolean cartesian) {
            super("companion");
            this.cartesian = cartesian;
        }

        @Override
        public boolean isCartesian() {
            return cartesian;
        }

        @Override
        public double[] getXBounds() {
            return xb;
        }

        @Override
        public double[] getYBounds() {
            return yb;
        }

        @Override
        public void paint(ChartContext ctx) {
            painted = true;
            ctx.getGraphics().fillRect(ctx.getPlotX(), ctx.getPlotY(), 2, 2);
        }
    }

    // the drawing calls of one render (lines, rects, arcs, shapes); text is not counted
    private int fills(Chart c) {
        // without shape support Graphics drops fillShape/drawShape before the implementation sees them
        implementation.setShapeSupported(true);
        implementation.setRecordDrawCalls(true);
        try {
            Image img = c.toImage(600, 400);
            assertNotNull(img);
            assertEquals(600, img.getWidth());
            c.setX(0);
            c.setY(0);
            c.setWidth(600);
            c.setHeight(400);
            implementation.getDrawCalls().clear();
            c.paint(Image.createImage(600, 400).getGraphics());
            return implementation.getDrawCalls().size();
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    @FormTest
    public void everyTypeRenders() {
        render(new Chart().addSeries(new LineSeries("l").setData(null, new double[]{1, 3, 2})));
        render(new Chart().addSeries(new BarSeries("b").setData(null, new double[]{1, -3, 2})));
        CandlestickSeries cs = new CandlestickSeries("c");
        cs.setOhlc(null, new double[]{1, 2}, new double[]{3, 4}, new double[]{0, 1}, new double[]{2, 3});
        render(new Chart().addSeries(cs));
        render(new Chart().addSeries(new WaterfallSeries("w").setSteps(new double[]{5, -2, 0}, new boolean[]{false, false, true})));
        render(new Chart().addSeries(new HistogramSeries("h").setSamples(new double[]{1, 2, 2, 3, 3, 3, 4}, 0)));
        BoxPlotSeries bp = new BoxPlotSeries("bp");
        bp.addSamples(0, new double[]{1, 2, 3, 4, 5, 9});
        render(new Chart().addSeries(bp));
        render(new Chart().addSeries(new HeatmapSeries("hm").setMatrix(new double[][]{{1, 2}, {3, Double.NaN}})));
        render(new Chart().addSeries(new PieSeries("p").setData(new String[]{"a", "b", "c"}, new double[]{1, 2, 3})));
        render(new Chart().addSeries(new FunnelSeries("f").setData(new String[]{"a", "b"}, new double[]{10, 4})));
        render(new Chart().addSeries(new GaugeSeries("g", 0, 100).setValue(40)));
        render(new Chart().addSeries(new RadarSeries("r", "x", "y", "z").setValues(1, 2, 3)));
        TreeNode root = new TreeNode("root");
        TreeNode a = root.add(new TreeNode("a"));
        a.add(new TreeNode("a1", 3));
        a.add(new TreeNode("a2", 1));
        root.add(new TreeNode("b", 2));
        assertEquals(6, root.getValue(), 1e-9);
        render(new Chart().addSeries(new TreemapSeries("t", root)));
        render(new Chart().addSeries(new SunburstSeries("s", root)));
        render(new Chart().addSeries(new SankeySeries("k").addLink("a", "b", 3).addLink("b", "c", 2)));
        render(new Chart().addSeries(new ScatterSeries("sc").setData(null, new double[]{1, 2}, new double[]{3, 9})));
        render(new Chart().addSeries(new ErrorBarSeries("e").setIntervals(null, new double[]{2, 3}, new double[]{1, 2}, new double[]{3, 4})));
        render(new Chart());
    }

    @FormTest
    public void tableAndDescriptionCarryTheData() {
        Chart c = new Chart().setTitle("Sales").setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2"));
        c.addSeries(new BarSeries("2025").setData(null, new double[]{10, 20}));
        c.addSeries(new PieSeries("Mix").setData(new String[]{"x", "y"}, new double[]{1, 3}));
        List<String[]> rows = c.getTableRows();
        assertEquals(4, rows.size());
        assertEquals("Q1", rows.get(0)[1]);
        assertEquals("20", rows.get(1)[2]);
        assertEquals("y", rows.get(3)[1]);
        String d = c.getAccessibilityText();
        assertTrue(d.startsWith("Sales."), d);
        assertTrue(d.contains("2025: 2 values from 10 to 20"), d);
        assertEquals(4, c.toTableModel().getRowCount());
    }

    @FormTest
    public void tappingAPieSliceSelectsIt() {
        Chart c = new Chart();
        c.addSeries(new PieSeries("p").setLabelMode(PieSeries.LABELS_NONE).setData(new String[]{"a", "b"}, new double[]{1, 1}));
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        c.setWidth(600);
        c.setHeight(400);
        c.paint(Image.createImage(600, 400).getGraphics());
        ChartContext ctx = c.getLastContext();
        assertNotNull(ctx);
        // right of the centre is the first slice (it starts at twelve o'clock, clockwise)
        int cx = ctx.getPlotX() + ctx.getPlotWidth() / 2 - ctx.getOriginX();
        int cy = ctx.getPlotY() + ctx.getPlotHeight() / 2 - ctx.getOriginY();
        ChartPoint p = c.hitTest(cx + ctx.getPlotHeight() / 4, cy);
        assertNotNull(p);
        assertEquals(0, p.getIndex());
        p = c.hitTest(cx - ctx.getPlotHeight() / 4, cy);
        assertNotNull(p);
        assertEquals(1, p.getIndex());
    }
}
