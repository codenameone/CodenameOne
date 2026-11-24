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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Fourteenth review round of the chart package: one test per finding.
public class ChartReviewRound14Test extends UITestBase {

    private void place(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    @FormTest
    public void infiniteLinePointIsAGapAndKeepsTheAxis() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2, 3}, new double[]{1, Double.POSITIVE_INFINITY, 3, 5});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        Axis y = c.getYAxis();
        assertTrue(finite(y.getDataMax()) && y.getDataMax() <= 10, "axis max " + y.getDataMax());
        assertTrue(finite(y.getDataMin()), "axis min " + y.getDataMin());
        for (int i : new int[]{0, 2, 3}) {
            ChartPoint p = s.getPoint(c.getLastContext(), i);
            assertNotNull(p, "point " + i);
            assertTrue(p.getScreenY() >= 0 && p.getScreenY() <= 300, "point " + i + " on the plot");
        }
    }

    @FormTest
    public void infiniteWaterfallStepIsAGap() {
        WaterfallSeries s = new WaterfallSeries("w");
        s.add(5);
        s.add(Double.POSITIVE_INFINITY);
        s.add(3);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        assertTrue(finite(c.getYAxis().getDataMax()) && c.getYAxis().getDataMax() <= 10,
                "axis max " + c.getYAxis().getDataMax());
    }

    @FormTest
    public void valueBandToInfinityKeepsTheAxisFinite() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1}, new double[]{1, 3});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0)
                .addAnnotation(Annotation.valueBand(2, Double.POSITIVE_INFINITY, "band"));
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        assertTrue(finite(c.getYAxis().getDataMax()) && c.getYAxis().getDataMax() <= 10,
                "axis max " + c.getYAxis().getDataMax());
    }

    @Test
    public void oneNaNXDoesNotPoisonTheXBounds() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, Double.NaN, 4}, new double[]{1, 2, 3});
        assertArrayEquals(new double[]{0, 4}, s.getXBounds(), 0);
    }

    @Test
    public void logAxisHonoursSetRange() {
        LogAxis a = new LogAxis(Axis.LEFT);
        a.setRange(5.0, 500.0);
        a.setDataBounds(1, 1000);
        assertEquals(5, a.getDataMin(), 1e-9);
        assertEquals(500, a.getDataMax(), 1e-9);
    }

    @FormTest
    public void exportDuringAViewportAnimationShowsTheTarget() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, new double[]{1, 4, 2, 8, 5, 7, 3, 9, 6, 2});
        Chart settled = new Chart().addSeries(s).setAnimationDuration(0);
        place(settled, 400, 300);
        settled.paint(Image.createImage(400, 300).getGraphics());
        List<String> expected = exportCalls(settled);

        LineSeries s2 = new LineSeries("s");
        s2.setData(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, new double[]{1, 4, 2, 8, 5, 7, 3, 9, 6, 2});
        Chart c = new Chart().addSeries(s2).setAnimationDuration(0);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        c.getXAxis().setVisibleRange(2, 4);
        c.paint(Image.createImage(400, 300).getGraphics());
        c.resetZoom();
        assertTrue(c.getXAxis().isZoomed(), "positive control: the zoom-out is still animating");
        List<String> got = exportCalls(c);
        assertEquals(2, c.getXAxis().getVisibleMin(), 1e-9, "the on-screen viewport is restored");
        assertEquals(4, c.getXAxis().getVisibleMax(), 1e-9);
        // toImage records the export paint and then a paint at screen size that restores the
        // on-screen geometry; only the export half is compared, the screen is still mid-zoom
        int exportCalls = expected.size() / 2;
        assertEquals(expected.subList(0, exportCalls), got.subList(0, exportCalls),
                "the export shows the viewport the animation ends at");
    }

    private List<String> exportCalls(Chart c) {
        implementation.setShapeSupported(true);
        implementation.setRecordDrawCalls(true);
        try {
            c.toImage(400, 300);
            List<String> out = new ArrayList<String>();
            for (Object[] call : implementation.getDrawCalls()) {
                StringBuilder sb = new StringBuilder();
                for (Object o : call) {
                    sb.append(o instanceof float[] ? Arrays.toString((float[]) o) : String.valueOf(o)).append(',');
                }
                out.add(sb.toString());
            }
            return out;
        } finally {
            implementation.setRecordDrawCalls(false);
            implementation.setShapeSupported(false);
        }
    }

    @FormTest
    public void arrowAtTheLastPointPansAZoomedChart() {
        LineSeries a = new LineSeries("a");
        a.setData(new double[]{0, 1, 2, 3, 4}, new double[]{1, 2, 3, 2, 1});
        LineSeries b = new LineSeries("b");
        b.setData(new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, new double[]{1, 2, 3, 2, 1, 2, 3, 2, 1, 2});
        Chart c = new Chart().addSeries(a).addSeries(b).setAnimationDuration(0);
        place(c, 400, 300);
        c.paint(Image.createImage(400, 300).getGraphics());
        c.getXAxis().setVisibleRange(2, 5);
        c.setSelection(a, 4);
        c.paint(Image.createImage(400, 300).getGraphics());
        assertNotNull(c.getSelection(), "positive control: the last point of a is selected");
        double before = c.getXAxis().getVisibleMin();
        c.keyReleased(Display.getInstance().getKeyCode(Display.GAME_RIGHT));
        assertEquals(4, c.getSelection().getIndex(), "nothing to step to");
        assertTrue(c.getXAxis().getVisibleMin() > before, "the arrow pans instead");
    }

    @Test
    public void treeNodeMovesAndRejectsCycles() {
        TreeNode p1 = new TreeNode("p1");
        TreeNode p2 = new TreeNode("p2");
        TreeNode leaf = new TreeNode("leaf", 3);
        p1.add(leaf);
        p2.add(leaf);
        assertEquals(0, p1.getChildCount());
        assertSame(p2, leaf.getParent());
        assertEquals(3, p2.getValue(), 0);
        assertEquals(0, p1.getValue(), 0);
        assertThrows(IllegalArgumentException.class, () -> p1.add(p1));
        p1.add(p2);
        assertThrows(IllegalArgumentException.class, () -> leaf.add(p1));
        assertFalse(leaf.getChildCount() > 0);
    }

    @Test
    public void colorScaleRejectsBadStops() {
        assertThrows(IllegalArgumentException.class, () -> new ColorScale(new double[0], new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new ColorScale(new double[]{0, 1}, new int[]{0}));
        assertNotNull(new ColorScale(new double[]{0, 1}, new int[]{0, 0xffffff}));
    }
}
