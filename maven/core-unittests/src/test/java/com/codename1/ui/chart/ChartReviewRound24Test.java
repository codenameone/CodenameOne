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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.layouts.BorderLayout;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Twenty-fourth review round of the chart package.
public class ChartReviewRound24Test extends UITestBase {

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
            return new ArrayList<Object[]>(implementation.getDrawCalls());
        } finally {
            implementation.setRecordDrawCalls(false);
        }
    }

    private Chart show(Chart c, long now) {
        AnimationTime.setTime(now);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        AnimationTime.setTime(now + 3000);
        c.animate();
        paint(c);
        return c;
    }

    private ChartContext frame(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        paint(c);
        return c.getLastContext();
    }

    @FormTest
    public void dashedSegmentFromFarOffThePlotEnds() {
        final LineSeries s = new LineSeries("l");
        s.setValues(1e15, 0);
        s.setDashed(true);
        final Chart c = new Chart().addSeries(s).setAnimationDuration(0);
        c.getYAxis().setRange(0.0, 1.0);
        List<Object[]> calls = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> paint(c));
        int dashes = 0;
        for (Object[] call : calls) {
            if ("drawShape".equals(call[0]) && ((Integer) call[3] > 0 || (Integer) call[4] > 0)) {
                dashes++;
            }
        }
        assertTrue(dashes > 0, "the visible part is dashed");
    }

    @FormTest
    public void shrinkingRadarStaysInsideItsRadius() throws Exception {
        RadarSeries s = new RadarSeries("r", "a", "b", "c");
        s.setValues(1000, 1000, 1000);
        Chart c = show(new Chart().addSeries(s), 100000);
        s.setValues(10, 10, 10);
        ChartContext ctx = frame(c, 103300);
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        float r = ChartReviewRound12Test.fieldOf(s, "lastR");
        double mx = 0;
        double my = 0;
        for (int i = 0; i < 3; i++) {
            mx += s.getPoint(ctx, i).getScreenX() / 3.0;
            my += s.getPoint(ctx, i).getScreenY() / 3.0;
        }
        for (int i = 0; i < 3; i++) {
            double dx = s.getPoint(ctx, i).getScreenX() - mx;
            double dy = s.getPoint(ctx, i).getScreenY() - my;
            double d = Math.sqrt(dx * dx + dy * dy);
            assertTrue(d > 1, "positive control: a polygon " + d);
            assertTrue(d <= r + 1, "vertex " + i + " at " + d + " inside radius " + r);
        }
    }

    @FormTest
    public void scatterHitsWhatIsPainted() {
        ScatterSeries s = new ScatterSeries("s");
        s.setData(new double[]{1, 5, 20}, new double[]{0, 0, 5});
        Chart c = new Chart().addSeries(s);
        c.getXAxis().setRange(0.0, 10.0);
        c.getYAxis().setRange(0.0, 10.0);
        show(c, 200000);
        s.setData(new double[]{1, 5, 20}, new double[]{0, 10, 5});
        ChartContext ctx = frame(c, 203150);
        double painted = s.animatedY(ctx, 1);
        assertTrue(painted > 1 && painted < 8, "positive control: mid transition " + painted + " at " + ctx.getProgress());
        int px = (int) (ctx.screenX(s, 5, painted) - ctx.getOriginX());
        int py = (int) (ctx.screenY(s, 5, painted) - ctx.getOriginY());
        ChartPoint hit = s.hitTest(ctx, px, py, 1);
        assertNotNull(hit, "the painted marker is hit");
        assertEquals(1, hit.getIndex());
        int ox = (int) (ctx.screenX(s, 20, 5) - ctx.getOriginX());
        int oy = (int) (ctx.screenY(s, 20, 5) - ctx.getOriginY());
        assertNull(s.hitTest(ctx, ox, oy, 1), "a point outside the visible x range is not hit");
    }

    @FormTest
    public void categoryOutlierIsIgnored() {
        CategoryAxis a = new CategoryAxis(Axis.BOTTOM, "a", "b", "c");
        a.setDataBounds(0, 1e9);
        assertEquals(2.5, a.getVisibleMax(), 1e-9, "an outlier keeps the labels' bands");
        a.setDataBounds(0, 6);
        assertEquals(6.5, a.getVisibleMax(), 1e-9, "a near point still gets its band");
    }

    @FormTest
    public void sunburstOfHugeValuesDrawsFiniteSectors() {
        TreeNode root = new TreeNode("root");
        root.add(new TreeNode("a", 1e308));
        root.add(new TreeNode("b", 5e307));
        final Chart c = new Chart().addSeries(new SunburstSeries("s", root)).setAnimationDuration(0);
        List<Object[]> calls = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> paint(c));
        int sectors = 0;
        for (Object[] call : calls) {
            if ("fillShape".equals(call[0])) {
                int w = (Integer) call[3];
                int h = (Integer) call[4];
                assertTrue(w > 0 && w <= W && h > 0 && h <= H, "sector bounds " + w + "x" + h);
                sectors++;
            }
        }
        assertEquals(2, sectors, "both sectors drawn");
    }
}
