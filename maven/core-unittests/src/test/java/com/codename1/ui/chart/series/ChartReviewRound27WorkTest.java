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
package com.codename1.ui.chart.series;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Counts work rather than time: the counters are package-private hooks in the series.
public class ChartReviewRound27WorkTest extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private void paint(com.codename1.ui.Component c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
    }

    @FormTest
    public void trimRebuildsTheSlotMapOnce() {
        final int n = 1000;
        ScatterSeries s = new ScatterSeries("s");
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = (i * 7919L) % n;
            ys[i] = i;
        }
        s.setData(xs, ys);
        Chart c = new Chart().addSeries(s);
        c.getXAxis().setRange(0.0, (double) n);
        c.getYAxis().setRange(0.0, (double) n);
        AnimationTime.setTime(500000);
        c.setAnimationDuration(1000);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        paint(c);
        AnimationTime.setTime(503000);
        c.animate();
        paint(c);
        // a change starts a transition, so the trim below runs while points are mid-animation
        s.set(0, 1);
        int before = s.slotMapRebuilds;
        s.setMaxPoints(10);
        assertEquals(1, s.slotMapRebuilds - before, "one slot map rebuild for the whole trim");
        AnimationTime.setTime(503300);
        c.animate();
        paint(c);
        ChartContext ctx = c.getLastContext();
        assertTrue(ctx.getProgress() > 0 && ctx.getProgress() < 1, "positive control: mid transition");
        for (int i = 0; i < 10; i++) {
            assertEquals(ys[n - 10 + i], s.animatedY(ctx, i), 1e-9, "point " + i + " tweens from its own value");
        }
    }

    @FormTest
    public void pieScansItsSlicesOncePerPaint() {
        final int n = 200;
        String[] labels = new String[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "s" + i;
            values[i] = 1;
        }
        PieSeries s = new PieSeries("p");
        s.setData(labels, values);
        Chart c = new Chart().addSeries(s).setAnimationDuration(0).setLegendPosition(Chart.LEGEND_NONE);
        int before = s.percentBaseScans;
        paint(c);
        assertEquals(1, s.percentBaseScans - before, "one scan per paint whatever the slice count");
        PieSeries q = new PieSeries("q");
        q.setData(new String[]{"a", "b", "c"}, new double[]{1, 1, 2});
        Chart small = new Chart().addSeries(q).setAnimationDuration(0);
        implementation.setRecordDrawnStrings(true);
        try {
            paint(small);
            List<Object[]> strings = implementation.getDrawnStrings();
            boolean quarter = false;
            boolean half = false;
            for (Object[] str : strings) {
                String t = (String) str[0];
                quarter |= t.indexOf("25%") >= 0;
                half |= t.indexOf("50%") >= 0;
            }
            assertTrue(quarter && half, "25% and 50% labels drawn: " + strings.size());
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }

    @FormTest
    public void wideHeatmapIsClippedToTheLimit() {
        HeatmapSeries h = new HeatmapSeries("h");
        h.setMatrix(new double[][]{new double[50]});
        double[] b = h.getXBoundsWithin(9.7);
        assertTrue(b != null, "the columns within reach still count");
        assertEquals(-0.5, b[0], 1e-9);
        assertEquals(9.5, b[1], 1e-9);
        double[] all = h.getXBoundsWithin(100);
        assertEquals(49.5, all[1], 1e-9, "a limit past the matrix keeps every column");
        assertNull(h.getXBoundsWithin(-1), "no column at or below a negative limit");
    }
}
