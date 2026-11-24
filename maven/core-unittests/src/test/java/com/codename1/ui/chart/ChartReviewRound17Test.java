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
import com.codename1.ui.chart.series.CategoricalSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.HierarchySeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Seventeenth review round of the chart package.
public class ChartReviewRound17Test extends UITestBase {

    private static final double INF = Double.POSITIVE_INFINITY;

    private List<Object[]> calls(Chart c, int w, int h) {
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

    private static int fills(List<Object[]> drawn) {
        int n = 0;
        for (Object[] call : drawn) {
            if (((String) call[0]).startsWith("fill") && ((Integer) call[3]).intValue() > 0 && ((Integer) call[4]).intValue() > 0) {
                n++;
            }
        }
        return n;
    }

    private static void assertFiniteTooltip(Series s, ChartContext ctx, int i) {
        ChartPoint p = s.getPoint(ctx, i);
        assertNotNull(p, "point " + i);
        String t = s.formatTooltip(ctx, p);
        assertFalse(t == null || t.indexOf("NaN") >= 0 || t.indexOf("Infinity") >= 0, "tooltip " + t);
    }

    private void assertCategoricalSurvivesInfinity(CategoricalSeries s) {
        s.setData(new String[]{"a", "b", "c"}, new double[]{1, INF, 2});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> drawn = calls(c, 400, 300);
        assertTrue(fills(drawn) >= 2, "the finite values still render: " + fills(drawn));
        assertFiniteTooltip(s, c.getLastContext(), 0);
        assertFiniteTooltip(s, c.getLastContext(), 2);
    }

    @FormTest
    public void pieIgnoresAnInfiniteValue() {
        assertCategoricalSurvivesInfinity(new PieSeries("p"));
    }

    @FormTest
    public void funnelIgnoresAnInfiniteValue() {
        assertCategoricalSurvivesInfinity(new FunnelSeries("f"));
    }

    private static TreeNode treeWithInfinity() {
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        a.add(new TreeNode("a1", 1));
        a.add(new TreeNode("a2", INF));
        root.add(a);
        root.add(new TreeNode("B", 3));
        return root;
    }

    private void assertHierarchySurvivesInfinity(HierarchySeries s) {
        assertEquals(4, s.getRoot().getValue(), 0, "an infinite leaf counts as nothing");
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> drawn = calls(c, 400, 300);
        assertTrue(fills(drawn) >= 2, "the finite nodes still render: " + fills(drawn));
        assertFiniteTooltip(s, c.getLastContext(), 0);
    }

    @FormTest
    public void sunburstIgnoresAnInfiniteLeaf() {
        assertHierarchySurvivesInfinity(new SunburstSeries("s", treeWithInfinity()));
    }

    @FormTest
    public void treemapIgnoresAnInfiniteLeaf() {
        assertHierarchySurvivesInfinity(new TreemapSeries("t", treeWithInfinity()));
    }

    @FormTest
    public void sankeyIgnoresAnInfiniteLink() {
        SankeySeries s = new SankeySeries("k");
        s.addLink("a", "b", 5).addLink("a", "c", INF).addLink("b", "d", 3);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        List<Object[]> drawn = calls(c, 400, 300);
        assertTrue(fills(drawn) >= 3, "the finite nodes still render: " + fills(drawn));
        for (Object[] call : drawn) {
            for (int k = 1; k < 5; k++) {
                assertTrue(Math.abs(((Integer) call[k]).intValue()) < 100000, Arrays.toString(call));
            }
        }
        assertFiniteTooltip(s, c.getLastContext(), 0);
        assertEquals(5, s.getPoint(c.getLastContext(), 0).getY(), 0, "the infinite link adds nothing to its node");
    }

    @Test
    public void descendingBarsAreAsWideAsAscending() {
        BarSeries up = new BarSeries("u");
        up.setData(new double[]{0, 0.5, 1}, new double[]{1, 2, 3});
        BarSeries down = new BarSeries("d");
        down.setData(new double[]{1, 0.5, 0}, new double[]{3, 2, 1});
        BarSeries mixed = new BarSeries("m");
        mixed.setData(new double[]{1, 0, Double.NaN, 0.5}, new double[]{3, 1, 1, 2});
        assertEquals(0.5, up.getMinXSpacing(), 1e-9, "positive control");
        assertEquals(0.5, down.getMinXSpacing(), 1e-9, "descending x");
        assertEquals(0.5, mixed.getMinXSpacing(), 1e-9, "unsorted x with a gap");
    }

    @FormTest
    public void sunburstHoleCornersAreTheFirstRing() throws Exception {
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        for (int i = 0; i < 4; i++) {
            a.add(new TreeNode("a" + i, 1));
        }
        root.add(a);
        root.add(new TreeNode("B", 3));
        SunburstSeries s = new SunburstSeries("s", root);
        s.setCurrent(a);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        calls(c, 400, 400);
        ChartContext ctx = c.getLastContext();
        Field f = HierarchySeries.class.getDeclaredField("crumb");
        f.setAccessible(true);
        int[] crumb = (int[]) f.get(s);
        assertNotNull(crumb);
        float r = crumb[2] / 2f;
        float cx = crumb[0] + r;
        float cy = crumb[1] + r;
        ChartPoint centre = s.hitTest(ctx, Math.round(cx) - ctx.getOriginX(), Math.round(cy) - ctx.getOriginY(), 1);
        assertTrue(centre != null && centre.getIndex() < 0, "positive control: the hole goes up");
        int x = Math.round(cx + r * 0.95f) - ctx.getOriginX();
        int y = Math.round(cy + r * 0.95f) - ctx.getOriginY();
        ChartPoint corner = s.hitTest(ctx, x, y, 1);
        assertTrue(corner == null || corner.getIndex() >= 0, "the corner of the hole's square is on the ring");
    }

    @FormTest
    public void removingAnotherChartsSeriesLeavesItAlone() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2}, new double[]{1, 2, 3});
        Chart owner = new Chart().addSeries(s).setAnimationDuration(0);
        Chart other = new Chart();
        other.removeSeries(s);
        assertSame(owner, s.getChart());
        assertTrue(fills(calls(owner, 300, 200)) >= 0);
    }

    @FormTest
    public void infiniteHeatmapCellIsAGap() {
        HeatmapSeries s = new HeatmapSeries("h");
        s.setMatrix(new double[][]{{1, INF}, {2, 3}});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        calls(c, 400, 300);
        ChartContext ctx = c.getLastContext();
        assertNotNull(s.getPoint(ctx, 0), "positive control");
        assertNull(s.getPoint(ctx, 1), "the infinite cell has no point");
    }

    @FormTest
    public void nanAnnotationIsNotDrawn() {
        LineSeries s = new LineSeries("s");
        s.setData(new double[]{0, 1, 2}, new double[]{1, 2, 3});
        Chart plain = new Chart().addSeries(s).setAnimationDuration(0);
        int base = calls(plain, 400, 300).size();
        LineSeries s2 = new LineSeries("s");
        s2.setData(new double[]{0, 1, 2}, new double[]{1, 2, 3});
        Chart annotated = new Chart().addSeries(s2).setAnimationDuration(0);
        annotated.addAnnotation(Annotation.valueLine(Double.NaN, null));
        annotated.addAnnotation(Annotation.valueBand(Double.NaN, 2, null));
        List<Object[]> drawn = calls(annotated, 400, 300);
        assertEquals(base, drawn.size(), "a NaN line draws nothing");
        LineSeries s3 = new LineSeries("s");
        s3.setData(new double[]{0, 1, 2}, new double[]{1, 2, 3});
        Chart control = new Chart().addSeries(s3).setAnimationDuration(0);
        control.addAnnotation(Annotation.valueLine(2, null));
        assertTrue(calls(control, 400, 300).size() > base, "positive control: a finite line draws");
    }

    @FormTest
    public void hierarchyRejectsAForeignNode() {
        TreeNode big = new TreeNode("big");
        TreeNode root = new TreeNode("root");
        TreeNode a = new TreeNode("A");
        a.add(new TreeNode("a1", 1));
        a.add(new TreeNode("a2", 2));
        root.add(a);
        root.add(new TreeNode("B", 3));
        big.add(root);
        big.add(new TreeNode("C", 4));
        TreemapSeries s = new TreemapSeries("t", root);
        s.setCurrent(a);
        assertSame(a, s.getCurrent(), "positive control");
        s.setCurrent(new TreeNode("stranger", 1));
        assertSame(root, s.getCurrent(), "a node outside the tree falls back to the root");
        s.setCurrent(big);
        assertSame(root, s.getCurrent(), "a node above the root falls back to the root");
        s.handleTap(new ChartPoint(s, -1, 0, 0, 0, 0, null));
        assertSame(root, s.getCurrent(), "going up from the root stays at the root");
    }

    @FormTest
    public void emptySankeyLinkIsNotHit() {
        SankeySeries s = new SankeySeries("k");
        s.addLink("a", "b", 0).addLink("a", "c", 5);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        calls(c, 400, 300);
        ChartContext ctx = c.getLastContext();
        boolean liveHit = false;
        for (int y = 0; y < 300; y += 2) {
            for (int x = 0; x < 400; x += 2) {
                ChartPoint p = s.hitTest(ctx, x - ctx.getOriginX(), y - ctx.getOriginY(), 2);
                if (p != null) {
                    assertTrue(p.getIndex() != 3, "the empty link is hit at " + x + "," + y);
                    liveHit |= p.getIndex() == 4;
                }
            }
        }
        assertTrue(liveHit, "positive control: the live link is hit");
    }

    @Test
    public void colorScaleRejectsBadPositions() {
        final int[] cols = {0, 0xffffff};
        assertThrows(IllegalArgumentException.class, () -> new ColorScale(new double[]{0, Double.NaN}, cols));
        assertThrows(IllegalArgumentException.class, () -> new ColorScale(new double[]{0, INF}, cols));
        assertThrows(IllegalArgumentException.class, () -> new ColorScale(new double[]{1, 0}, cols));
        assertNotNull(new ColorScale(new double[]{0, 0, 1}, new int[]{0, 1, 2}));
    }
}
