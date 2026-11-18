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
import com.codename1.ui.chart.series.SankeySeries;

/// A three-column sankey with a node that is both a target and a source.
public class Chart2SankeyScreenshotTest extends AbstractChart2ScreenshotTest {

    @Override
    protected Component buildChart() {
        Chart c = staticChart();
        c.setTitle("Energy flow");
        c.addSeries(new SankeySeries("Energy").addLink("Solar", "Grid", 40).addLink("Wind", "Grid", 30)
                .addLink("Gas", "Grid", 60).addLink("Gas", "Industry", 20).addLink("Grid", "Homes", 55)
                .addLink("Grid", "Industry", 45).addLink("Grid", "Losses", 30));
        return c;
    }

    @Override
    protected String screenshotName() {
        return "chart2-sankey";
    }
}
