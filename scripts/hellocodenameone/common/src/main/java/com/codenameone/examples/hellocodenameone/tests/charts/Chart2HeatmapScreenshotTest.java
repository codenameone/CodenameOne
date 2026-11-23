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
package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.ui.Component;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.HeatmapSeries;

/// A 5x7 heatmap with category axes on both sides and value labels.
public class Chart2HeatmapScreenshotTest extends AbstractChart2ScreenshotTest {

    @Override
    protected Component buildChart() {
        Chart c = staticChart();
        double[][] m = new double[5][7];
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 7; k++) {
                m[r][k] = (r * 13 + k * 7) % 40 + 5;
            }
        }
        c.setTitle("Visits").setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
                .setYAxis(new CategoryAxis(Axis.LEFT, "Night", "Morning", "Noon", "Afternoon", "Evening"));
        c.addSeries(new HeatmapSeries("Visits").setMatrix(m).setShowValues(true));
        return c;
    }

    @Override
    protected String screenshotName() {
        return "chart2-heatmap";
    }
}
