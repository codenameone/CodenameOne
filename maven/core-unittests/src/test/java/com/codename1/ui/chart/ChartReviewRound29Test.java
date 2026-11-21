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
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.layout.Squarify;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound29Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private List<Object[]> paint(com.codename1.ui.Component c) {
        implementation.setShapeSupported(true);
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        implementation.setRecordDrawCalls(true);
        try {
            c.paint(Image.createImage(W, H).getGraphics());
            return new java.util.ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private static int count(List<Object[]> calls, String name) {
        int n = 0;
        for (Object[] call : calls) {
            if (name.equals(call[0])) {
                n++;
            }
        }
        return n;
    }

    @FormTest
    public void pieSelectionBeforeFirstPaintIsAppliedAfterLayout() {
        PieSeries s = new PieSeries("p");
        s.setData(new String[]{"a", "b", "c"}, new double[]{1, 2, 3});
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        c.setSelection(s, 2);
        paint(c);
        ChartPoint p = c.getSelection();
        assertNotNull(p, "the pending selection survives the paint that lays the pie out");
        assertEquals(2, p.getIndex());
    }

    @FormTest
    public void sankeySelectionOfANodeAddedSincePaintWaitsForTheNextPaint() {
        SankeySeries s = new SankeySeries("s");
        s.addLink("A", "B", 5).addLink("B", "C", 3);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        s.addLink("C", "D", 2);
        assertNull(s.getPoint(c.getLastContext(), 3), "the cached layout has no node D");
        c.setSelection(s, 3);
        paint(c);
        ChartPoint p = c.getSelection();
        assertNotNull(p, "applied once the layout knows D");
        assertEquals(3, p.getIndex());
    }

    @FormTest
    public void sharedRootNotifiesEverySeries() {
        TreeNode root = new TreeNode("root");
        TreeNode x = root.add(new TreeNode("x"));
        x.add(new TreeNode("leaf", 3));
        root.add(new TreeNode("y", 2));
        TreemapSeries tm = new TreemapSeries("t", root);
        tm.setCurrent(x);
        SunburstSeries sb = new SunburstSeries("s", root);
        sb.setCurrent(x);
        final int[] told = new int[1];
        TreeNode.ChangeListener counter = new TreeNode.ChangeListener() {
            @Override
            public void treeChanging(TreeNode node) {
            }

            @Override
            public void treeChanged(TreeNode node) {
                told[0]++;
            }
        };
        root.addChangeListener(counter);
        new TreeNode("elsewhere").add(x);
        assertEquals(1, told[0], "the extra listener is told");
        assertSame(root, tm.getCurrent(), "the treemap is told too");
        assertSame(root, sb.getCurrent(), "and the sunburst");
        root.removeChangeListener(counter);
        root.getChild(0).setValue(4);
        assertEquals(1, told[0], "a removed listener is not told");
    }

    @FormTest
    public void currentMovedOutOfTheTreeFallsBackToRoot() {
        TreeNode root = new TreeNode("root");
        TreeNode x = root.add(new TreeNode("x"));
        x.add(new TreeNode("leaf", 3));
        TreemapSeries tm = new TreemapSeries("t", root);
        tm.setCurrent(x);
        assertSame(x, tm.getCurrent());
        new TreeNode("other").add(x);
        assertSame(root, tm.getCurrent());
    }

    @FormTest
    public void squarifyKeepsSiblingsNearMaxValue() {
        float[][] r = Squarify.layout(new double[]{Double.MAX_VALUE, Double.MAX_VALUE}, 0, 0, 100, 100);
        float a0 = r[0][2] * r[0][3];
        float a1 = r[1][2] * r[1][3];
        assertTrue(a0 > 0 && a1 > 0, a0 + " " + a1);
        assertEquals(a0, a1, 1f);
    }

    @FormTest
    public void treemapDrawsABranchWhoseChildrenOverflow() {
        TreeNode root = new TreeNode("root");
        TreeNode big = root.add(new TreeNode("big"));
        big.add(new TreeNode("a", Double.MAX_VALUE));
        big.add(new TreeNode("b", Double.MAX_VALUE));
        root.add(new TreeNode("small", 1));
        Chart c = new Chart().addSeries(new TreemapSeries("t", root)).setAnimationDuration(0);
        List<Object[]> calls = paint(c);
        int cells = 0;
        for (Object[] call : calls) {
            if ("fillRect".equals(call[0]) && (Integer) call[3] > 1 && (Integer) call[4] > 1
                    && (Integer) call[3] < W && (Integer) call[4] < H) {
                cells++;
            }
        }
        assertTrue(cells >= 2, "the overflowed branch's children are drawn: " + cells);
    }

    @FormTest
    public void sunburstDrawsABranchWhoseChildrenOverflow() {
        TreeNode root = new TreeNode("root");
        TreeNode big = root.add(new TreeNode("big"));
        big.add(new TreeNode("a", Double.MAX_VALUE));
        big.add(new TreeNode("b", Double.MAX_VALUE));
        root.add(new TreeNode("small", 1));
        Chart c = new Chart().addSeries(new SunburstSeries("s", root)).setAnimationDuration(0);
        assertTrue(count(paint(c), "fillShape") >= 3, "the branch and both children are drawn");
    }

    @FormTest
    public void radarPolygonsUseTheGridSpokes() {
        RadarSeries s = new RadarSeries("r", "A", "B", "C", "D", "E");
        s.setValues(5, 5, 5, 5, 5, 5);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertNull(s.getPoint(ctx, 5), "a sixth value has no spoke");
        double cx = ctx.getPlotX() + ctx.getPlotWidth() / 2.0 - ctx.getOriginX();
        double cy = ctx.getPlotY() + ctx.getPlotHeight() / 2.0 - ctx.getOriginY();
        ChartPoint p0 = s.getPoint(ctx, 0);
        ChartPoint p1 = s.getPoint(ctx, 1);
        double a0 = Math.atan2(p0.getScreenY() - cy, p0.getScreenX() - cx);
        double a1 = Math.atan2(p1.getScreenY() - cy, p1.getScreenX() - cx);
        double d = Math.toDegrees(Math.abs(a1 - a0));
        if (d > 180) {
            d = 360 - d;
        }
        assertEquals(72, d, 2, "vertices sit on the five spokes");
    }

    @FormTest
    public void sessionTicksSurviveAHugeNegativeVisibleMin() {
        SessionTimeAxis a = new SessionTimeAxis(Axis.BOTTOM, new long[0]);
        a.setMaxTimes(3);
        for (int i = 0; i < 6; i++) {
            a.addTime(86400000L * (i + 1));
        }
        assertTrue(a.getFirstIndex() > 0, "positive control: indices are offset");
        a.setDisplayRange(-1e18, 6);
        assertTrue(a.ticks(5).length > 0, "ticks for the stored bars");
    }
}
