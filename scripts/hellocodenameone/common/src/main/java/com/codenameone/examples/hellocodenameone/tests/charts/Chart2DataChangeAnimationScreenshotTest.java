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

import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;

/// A data change animating: `setData` on both series of a shown chart tweens the bars and the
/// line from the old values to the new ones, and the value axis rescales with them.
public class Chart2DataChangeAnimationScreenshotTest extends AbstractChart2AnimationScreenshotTest {
    private static final double[] NEXT_SALES = {90, 30, 75, 20, 110, 45, 60, 100, 35, 70, 50, 120};
    private static final double[] NEXT_TARGET = {60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82};

    private BarSeries sales;
    private LineSeries target;

    @Override
    protected Chart buildChart() {
        Chart c = new Chart();
        c.setAnimationDuration(600);
        c.setTitle("Monthly sales").setXAxis(new CategoryAxis(Axis.BOTTOM, AbstractChart2ScreenshotTest.MONTHS));
        sales = new BarSeries("Sales");
        sales.setData(null, AbstractChart2ScreenshotTest.SALES);
        target = new LineSeries("Target");
        target.setData(null, AbstractChart2ScreenshotTest.TARGET);
        c.addSeries(sales);
        c.addSeries(target);
        return c;
    }

    @Override
    protected void startAnimation(Chart c) {
        long start = getAnimationStartTime();
        // let the entry animation finish, then change the data at the grid's start time
        AnimationTime.setTime(start + 10000);
        c.animate();
        AnimationTime.setTime(start);
        sales.setData(null, NEXT_SALES);
        target.setData(null, NEXT_TARGET);
    }

    @Override
    protected int getAnimationDurationMillis() {
        return 600;
    }

    @Override
    protected void finishCapture() {
        sales = null;
        target = null;
        super.finishCapture();
    }
}
