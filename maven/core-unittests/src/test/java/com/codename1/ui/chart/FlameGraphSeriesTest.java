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
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.series.FlameGraphSeries;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Widths, stacking direction, drilling and the rise-in animation of the flame graph.
public class FlameGraphSeriesTest extends UITestBase {

    private static ChartContext paint(Chart c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(600);
        c.setHeight(400);
        c.paint(Image.createImage(600, 400).getGraphics());
        ChartContext ctx = c.getLastContext();
        assertNotNull(ctx);
        return ctx;
    }

    private static TreeNode profile() {
        TreeNode main = new TreeNode("main");
        TreeNode load = main.add(new TreeNode("load"));
        load.add(new TreeNode("parse", 30));
        load.add(new TreeNode("decode", 10));
        TreeNode render = main.add(new TreeNode("render"));
        render.add(new TreeNode("layout", 45));
        render.add(new TreeNode("paint", 15));
        return main;
    }

    private static ChartPoint find(Chart c, ChartContext ctx, FlameGraphSeries s, String label) {
        for (int i = 0; i < s.getPointCount(); i++) {
            ChartPoint p = s.getPoint(ctx, i);
            if (label.equals(s.getNode(p).getLabel())) {
                return p;
            }
        }
        return null;
    }

    @FormTest
    public void barsAreAsWideAsTheirShareAndStackOnTheirParent() {
        FlameGraphSeries s = new FlameGraphSeries("CPU", profile());
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        c.setLegendPosition(Chart.LEGEND_NONE);
        ChartContext ctx = paint(c);
        assertEquals(7, s.getPointCount(), "every node is drawn");
        ChartPoint main = find(c, ctx, s, "main");
        ChartPoint render = find(c, ctx, s, "render");
        ChartPoint layout = find(c, ctx, s, "layout");
        assertNotNull(main);
        assertNotNull(render);
        assertNotNull(layout);
        // render is 60 of 100, layout 45 of 100
        int w = ctx.getPlotWidth();
        ChartPoint hitRender = c.hitTest(Math.round(ctx.getPlotX() + w * 0.7f), render.getScreenY());
        assertSame(s.getNode(render), s.getNode(hitRender));
        ChartPoint hitLoad = c.hitTest(Math.round(ctx.getPlotX() + w * 0.3f), render.getScreenY());
        assertEquals("load", s.getNode(hitLoad).getLabel());
        assertTrue(main.getScreenY() > render.getScreenY() && render.getScreenY() > layout.getScreenY(),
                "the root is the bottom row and children sit above their parent");
    }

    @FormTest
    public void invertedHangsTheChildrenBelowTheRoot() {
        FlameGraphSeries s = new FlameGraphSeries("CPU", profile()).setInverted(true);
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        ChartContext ctx = paint(c);
        assertTrue(find(c, ctx, s, "main").getScreenY() < find(c, ctx, s, "layout").getScreenY());
    }

    @FormTest
    public void tappingABarZoomsIntoIt() {
        FlameGraphSeries s = new FlameGraphSeries("CPU", profile());
        Chart c = new Chart().addSeries(s);
        c.setAnimationDuration(0);
        ChartContext ctx = paint(c);
        ChartPoint render = find(c, ctx, s, "render");
        c.pointerPressed(render.getScreenX(), render.getScreenY());
        c.pointerReleased(render.getScreenX(), render.getScreenY());
        assertEquals("render", s.getCurrent().getLabel());
        ctx = paint(c);
        ChartPoint layout = find(c, ctx, s, "layout");
        // layout is 45 of render's 60 once zoomed: it reaches past the middle of the plot
        assertNotNull(c.hitTest(ctx.getPlotX() + ctx.getPlotWidth() * 2 / 3, layout.getScreenY()));
        assertEquals("layout", s.getNode(c.hitTest(ctx.getPlotX() + ctx.getPlotWidth() * 2 / 3, layout.getScreenY())).getLabel());
    }

    @FormTest
    public void theStacksRiseRowByRowWhenTheChartAppears() {
        AnimationTime.setTime(30000);
        try {
            FlameGraphSeries s = new FlameGraphSeries("CPU", profile());
            Chart c = new Chart().addSeries(s);
            c.setAnimationDuration(600);
            Form f = new Form(new BorderLayout());
            f.add(BorderLayout.CENTER, c);
            f.show();
            paint(c);
            AnimationTime.setTime(30100);
            c.animate();
            ChartContext ctx = paint(c);
            int early = s.getPointCount();
            assertTrue(early > 0 && early < 7, "early on only the lower rows are up: " + early);
            AnimationTime.setTime(31000);
            c.animate();
            paint(c);
            assertEquals(7, s.getPointCount());
        } finally {
            AnimationTime.reset();
        }
    }
}
