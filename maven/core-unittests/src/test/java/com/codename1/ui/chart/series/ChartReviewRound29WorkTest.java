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
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.Chart;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Counts work rather than time: the counters are package-private hooks in the series.
public class ChartReviewRound29WorkTest extends UITestBase {

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
    public void boxesAddedOutOfOrderIndexInLinearWork() {
        int n = 2000;
        BoxPlotSeries s = new BoxPlotSeries("b");
        for (int i = 0; i < n; i++) {
            // 7919 is coprime to 2000, so this visits every x once, out of order
            int x = (i * 7919) % n;
            s.addSamples(x, new double[]{1, 2, 3, 4, 100});
        }
        assertEquals(n, s.size());
        // replacing a box finds it by x
        s.addBox(5, 1, 2, 3, 4, 5);
        assertEquals(n, s.size(), "replaced, not appended");
        assertTrue(s.boxIndexBuilds <= 4 * n, "linear: " + s.boxIndexBuilds);
        int b = -1;
        for (int i = 0; i < n; i++) {
            if (s.getX(i) == 7) {
                b = i;
            }
        }
        assertEquals(1, s.getOutliers(b).length, "outliers still paired with their box");
        assertEquals(0, s.getOutliers(indexOf(s, 5)).length, "addBox dropped the replaced box's outliers");
    }

    private static int indexOf(BoxPlotSeries s, double x) {
        for (int i = 0; i < s.size(); i++) {
            if (s.getX(i) == x) {
                return i;
            }
        }
        return -1;
    }

    @FormTest
    public void zoomedHeatmapPaintsOnlyTheWindow() {
        int n = 500;
        double[][] m = new double[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                m[r][c] = r + c;
            }
        }
        HeatmapSeries h = new HeatmapSeries("h");
        h.setMatrix(m);
        Chart chart = new Chart().addSeries(h).setAnimationDuration(0);
        paint(chart);
        assertTrue(h.paintedCells >= n * n, "positive control: unzoomed paints every cell " + h.paintedCells);
        chart.getXAxis().setVisibleRange(100, 110);
        chart.getYAxis().setVisibleRange(100, 110);
        h.paintedCells = 0;
        paint(chart);
        assertTrue(h.paintedCells > 0 && h.paintedCells <= 144, "window only: " + h.paintedCells);
        assertEquals(100, chart.getXAxis().getVisibleMin(), 1, "the zoom held");
    }
}
