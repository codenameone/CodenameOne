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
import com.codename1.ui.chart.ChartContext;

/// The shared tooltip and crosshair: a pointer hovering inside the plot shows one tooltip listing
/// every series at the hovered x.
public class Chart2SharedTooltipScreenshotTest extends AbstractChart2ScreenshotTest {
    @Override
    protected Component buildChart() {
        Chart c = staticChart();
        c.setTooltipMode(Chart.TOOLTIP_SHARED);
        return monthlySales(c);
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        super.registerReadyCallback(parent, new Runnable() {
            @Override
            public void run() {
                // the chart has painted by now, so its plot is laid out
                // aimed at the plot itself, so a change to the title, legend or axis gutters
                // cannot move the pointer outside it
                Chart c = (Chart) getBuiltComponent();
                ChartContext ctx = c.getLastContext();
                int x = c.getAbsoluteX() - c.getX() + ctx.getPlotX() + ctx.getPlotWidth() * 3 / 5;
                int y = c.getAbsoluteY() - c.getY() + ctx.getPlotY() + ctx.getPlotHeight() / 2;
                c.pointerHover(new int[]{x}, new int[]{y});
                c.repaint();
                run.run();
            }
        });
    }

    @Override
    protected String screenshotName() {
        return "chart2-shared-tooltip";
    }
}
