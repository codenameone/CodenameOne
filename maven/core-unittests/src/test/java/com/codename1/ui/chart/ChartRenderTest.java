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
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.chart.series.WaterfallSeries;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Renders every chart type off screen and checks the accessible description and data table.
public class ChartRenderTest extends UITestBase {

    private static Chart render(Chart c) {
        c.setAnimationDuration(0);
        Image img = c.toImage(600, 400);
        assertNotNull(img);
        assertEquals(600, img.getWidth());
        return c;
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
