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
import com.codename1.ui.chart.Annotation;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;

/// Bars and a dashed target line on one category axis, with a value annotation.
public class Chart2MixedScreenshotTest extends AbstractChart2ScreenshotTest {

    @Override
    protected Component buildChart() {
        Chart c = staticChart();
        c.setTitle("Revenue vs target").setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
        c.addSeries(new BarSeries("2024").setData(null, new double[]{120, 150, 140, 190}));
        c.addSeries(new BarSeries("2025").setData(null, new double[]{135, 170, 165, 230}));
        c.addSeries(new LineSeries("Target").setDashed(true).setData(null, new double[]{130, 160, 170, 200}));
        c.addAnnotation(Annotation.valueLine(180, "Stretch"));
        return c;
    }

    @Override
    protected String screenshotName() {
        return "chart2-mixed";
    }
}
