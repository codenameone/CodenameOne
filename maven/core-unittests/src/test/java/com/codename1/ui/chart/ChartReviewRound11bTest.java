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
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.SunburstSeries;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Eleventh review round of the chart package, second half: one test per finding.
public class ChartReviewRound11bTest extends UITestBase {

    private List<Object[]> calls(Chart c, int w, int h) {
        c.setAnimationDuration(0);
        return callsAnimated(c, w, h);
    }

    private List<Object[]> callsAnimated(Chart c, int w, int h) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(w, h).getGraphics());
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private static int v(Object[] call, int k) {
        return ((Integer) call[k]).intValue();
    }

    @FormTest
    public void heatmapCellsCanBeSelectedAndSurviveADataChange() {
        HeatmapSeries s = new HeatmapSeries("h");
        s.setMatrix(new double[][]{{1, 2, 3}, {4, 5, 6}});
        Chart c = new Chart().addSeries(s);
        calls(c, 400, 300);
        assertEquals(6, s.getPointCount());
        c.setSelection(s, 4);
        ChartPoint p = c.getSelection();
        assertNotNull(p, "a cell can be selected by its id");
        assertEquals(4, p.getIndex());
        assertEquals(5, p.getY(), 0);
        s.setMatrix(new double[][]{{1, 2, 3}, {4, 50, 6}});
        calls(c, 400, 300);
        assertNotNull(c.getSelection(), "the selection survives new data");
        assertEquals(4, c.getSelection().getIndex());
    }

    @FormTest
    public void radarWithAnInfiniteValueDoesNotHang() {
        RadarSeries s = new RadarSeries("r", "a", "b", "c");
        s.setValues(1, Double.POSITIVE_INFINITY, 3);
        final Chart c = new Chart().addSeries(s);
        List<Object[]> drawn = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> calls(c, 400, 400));
        assertFalse(drawn.isEmpty(), "positive control: the radar painted");
        for (Object[] call : drawn) {
            if ("fillShape".equals(call[0]) || "drawShape".equals(call[0])) {
                assertTrue(v(call, 3) <= 400 && v(call, 4) <= 400, "every shape stays on the chart");
            }
        }
    }

    @FormTest
    public void fadingRadarIsScaledWithTheOthers() {
        RadarSeries a = new RadarSeries("a", "x", "y", "z");
        a.setValues(10, 10, 10);
        RadarSeries b = new RadarSeries("b", "x", "y", "z");
        b.setValues(1000, 1000, 1000);
        Chart c = new Chart().addSeries(a).addSeries(b);
        com.codename1.ui.Form f = new com.codename1.ui.Form();
        f.add(c);
        f.show();
        calls(c, 400, 400);
        c.setAnimationDuration(100000);
        b.setVisible(false); // starts fading out, and stays near fully drawn for the next frame
        int shapes = 0;
        for (Object[] call : callsAnimated(c, 400, 400)) {
            if ("fillShape".equals(call[0])) {
                shapes++;
                assertTrue(v(call, 1) >= 0 && v(call, 2) >= 0 && v(call, 1) + v(call, 3) <= 400
                        && v(call, 2) + v(call, 4) <= 400, "the fading radar stays inside the chart");
            }
        }
        assertEquals(2, shapes, "positive control: both radars painted");
    }

    @FormTest
    public void gaugeRepaintsIdentically() {
        GaugeSeries s = new GaugeSeries("g", 0, 100);
        s.setValue(42);
        Chart c = new Chart().addSeries(s);
        List<Object[]> first = calls(c, 300, 300);
        List<Object[]> second = calls(c, 300, 300);
        assertFalse(first.isEmpty());
        assertEquals(first.size(), second.size(), "the cached value font paints the same frame");
    }

    private static List<Object[]> rects(List<Object[]> calls, int minH) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (Object[] call : calls) {
            if (("fillRect".equals(call[0]) || "fillRoundRect".equals(call[0])) && v(call, 4) >= minH) {
                out.add(call);
            }
        }
        return out;
    }

    @FormTest
    public void groupedBarsShareOneBandWhenSpacingsDiffer() {
        BarSeries a = new BarSeries("a");
        a.setData(new double[]{0, 1, 2, 3, 4}, new double[]{10, 10, 10, 10, 10});
        BarSeries b = new BarSeries("b");
        b.setData(new double[]{0, 2, 4}, new double[]{10, 10, 10});
        Chart c = new Chart().addSeries(a).addSeries(b);
        List<Object[]> bars = rects(calls(c, 600, 400), 20);
        assertTrue(bars.size() >= 8, "positive control: the bars painted, got " + bars.size());
        int w = v(bars.get(0), 3);
        for (Object[] bar : bars) {
            assertTrue(Math.abs(v(bar, 3) - w) <= 1, "every bar of the group has the same width");
        }
    }

    @FormTest
    public void singleBarIsNotOneUnitWide() {
        BarSeries a = new BarSeries("a");
        a.setData(new double[]{1700000000000.0}, new double[]{10});
        Chart c = new Chart().addSeries(a);
        List<Object[]> bars = rects(calls(c, 600, 400), 20);
        assertFalse(bars.isEmpty(), "positive control: the bar painted");
        assertTrue(v(bars.get(0), 3) >= 20, "a lone bar gets a visible band, got " + v(bars.get(0), 3));
    }

    @FormTest
    public void rightLegendIsBoundedByTheHeight() {
        Chart c = new Chart().setLegendPosition(Chart.LEGEND_RIGHT);
        for (int i = 0; i < 60; i++) {
            BarSeries s = new BarSeries("series " + i);
            s.setData(new double[]{0, 1}, new double[]{1, 2});
            c.addSeries(s);
        }
        calls(c, 600, 200);
        assertFalse(c.legendHits.isEmpty(), "positive control: the legend was laid out");
        assertTrue(c.legendHits.size() < 60, "entries that do not fit are summed up, not hit tested");
        for (int[] hit : c.legendHits) {
            assertTrue(hit[1] + hit[3] <= 200, "every recorded hit is on screen");
        }
    }

    @FormTest
    public void hiddenPieSliceFollowsItsLabel() {
        PieSeries p = new PieSeries("p");
        p.setData(new String[]{"A", "B", "C"}, new double[]{1, 2, 3});
        new Chart().addSeries(p);
        p.toggleLegendEntry(0);
        assertFalse(p.isLegendEntryVisible(0));
        p.setData(new String[]{"C", "A", "B"}, new double[]{3, 1, 2});
        assertTrue(p.isLegendEntryVisible(0), "C was never hidden");
        assertFalse(p.isLegendEntryVisible(1), "A stays hidden where it moved");
    }

    @FormTest
    public void sankeyBackLinkIsHittable() {
        SankeySeries s = new SankeySeries("s");
        s.addLink("a", "b", 10).addLink("b", "c", 10).addLink("c", "a", 4);
        Chart c = new Chart().addSeries(s);
        calls(c, 600, 400);
        ChartContext ctx = c.getLastContext();
        int back = 3 + 2;
        boolean found = false;
        boolean forward = false;
        for (int y = 0; y < 400 && !found; y += 2) {
            for (int x = 0; x < 600; x += 2) {
                ChartPoint p = s.hitTest(ctx, x - ctx.getOriginX(), y - ctx.getOriginY(), 2);
                if (p != null && p.getIndex() == back) {
                    found = true;
                    break;
                }
                forward |= p != null && p.getIndex() == 3;
            }
        }
        assertTrue(forward || found, "positive control: links are hittable");
        assertTrue(found, "the back link can be hit");
    }

    @FormTest
    public void smallSunburstCanDrillOut() throws Exception {
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        a.add(new TreeNode("a1", 1)).add(new TreeNode("a2", 2));
        root.add(a).add(new TreeNode("B", 3));
        SunburstSeries s = new SunburstSeries("s", root);
        Chart c = new Chart().addSeries(s);
        calls(c, 60, 60);
        s.setCurrent(a);
        calls(c, 60, 60);
        ChartContext ctx = c.getLastContext();
        // just past the outer ring (round 12 limits going up to a narrow band there, so a tap
        // on the title or legend does not drill out)
        int x = Math.round(ChartReviewRound12Test.fieldOf(s, "cx") + ChartReviewRound12Test.fieldOf(s, "outer") + 1);
        int y = Math.round(ChartReviewRound12Test.fieldOf(s, "cy"));
        ChartPoint p = s.hitTest(ctx, x - ctx.getOriginX(), y - ctx.getOriginY(), 2);
        assertNotNull(p, "a tap just outside the rings goes back up");
        assertEquals(-1, p.getIndex());
        assertTrue(s.handleTap(p));
        assertEquals(root, s.getCurrent());
    }
}
