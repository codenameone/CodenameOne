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
import com.codename1.ui.Form;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Shared scaffolding for the static screenshots of the `com.codename1.ui.chart` package, the
/// counterpart of `AbstractChartScreenshotTest` for the legacy `com.codename1.charts` package.
/// Each subclass returns the component it wants captured and its screenshot name; data is fixed
/// (no random numbers) and `staticChart()` turns animation off, so every capture is deterministic.
/// The animations themselves are covered by the `Chart2*AnimationScreenshotTest` frame grids.
abstract class AbstractChart2ScreenshotTest extends BaseTest {
    private Component built;

    protected abstract Component buildChart();

    protected abstract String screenshotName();

    /// The component `buildChart()` returned, once the form is built.
    protected final Component getBuiltComponent() {
        return built;
    }

    /// A chart that applies everything immediately, for a still capture.
    protected static Chart staticChart() {
        Chart c = new Chart();
        c.setAnimationDuration(0);
        return c;
    }

    @Override
    public boolean runTest() throws Exception {
        String shot = screenshotName();
        Form form = createForm(shot, new BorderLayout(), shot);
        built = buildChart();
        form.add(BorderLayout.CENTER, built);
        form.show();
        return true;
    }

    /// The tree the treemap and sunburst captures draw.
    static TreeNode portfolio() {
        TreeNode root = new TreeNode("Portfolio");
        TreeNode tech = root.add(new TreeNode("Tech"));
        tech.add(new TreeNode("ACME", 120));
        tech.add(new TreeNode("Globex", 80));
        tech.add(new TreeNode("Initech", 35));
        TreeNode energy = root.add(new TreeNode("Energy"));
        energy.add(new TreeNode("Solar", 60));
        energy.add(new TreeNode("Wind", 40));
        TreeNode health = root.add(new TreeNode("Health"));
        health.add(new TreeNode("Pharma", 50));
        health.add(new TreeNode("Devices", 20));
        root.add(new TreeNode("Cash", 30));
        return root;
    }

    /// Twelve months of bars and a line, the data the interaction and animation captures share.
    static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    static final double[] SALES = {42, 55, 48, 63, 71, 66, 80, 77, 69, 85, 92, 88};
    static final double[] TARGET = {50, 52, 55, 58, 61, 64, 67, 70, 73, 76, 79, 82};

    /// Fills `c` with the shared monthly sales bars and target line.
    static Chart monthlySales(Chart c) {
        c.setTitle("Monthly sales").setXAxis(new CategoryAxis(Axis.BOTTOM, MONTHS));
        c.addSeries(new BarSeries("Sales").setData(null, SALES));
        c.addSeries(new LineSeries("Target").setData(null, TARGET));
        return c;
    }
}
