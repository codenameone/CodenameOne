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
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.series.FlameGraphSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.TreemapSeries;

/// New data arriving in the chart types that are not plotted on axes: pie slices turn to
/// their new angles, the funnel's stages resize, the gauge needle swings, the radar polygon
/// reshapes, and the treemap's cells and the flame graph's bars move to their new places.
/// The axis charts' data changes are `Chart2DataChangeAnimationScreenshotTest`.
public class Chart2PartsDataChangeAnimationScreenshotTest extends AbstractChart2GridAnimationScreenshotTest {
    private PieSeries pie;
    private FunnelSeries funnel;
    private GaugeSeries gauge;
    private RadarSeries radar;
    private TreeNode tree;
    private TreeNode profile;

    @Override
    protected Chart[] buildCharts() {
        Chart p = new Chart().setTitle("Pie");
        pie = new PieSeries("Share");
        pie.setData(new String[]{"A", "B", "C"}, new double[]{70, 20, 10});
        p.addSeries(pie);
        Chart f = new Chart().setTitle("Funnel");
        funnel = new FunnelSeries("Funnel");
        funnel.setData(new String[]{"Visited", "Trial", "Paid"}, new double[]{1000, 300, 60});
        f.addSeries(funnel);
        Chart g = new Chart().setTitle("Gauge");
        gauge = new GaugeSeries("CPU", 0, 100).setValue(20).setUnit("%");
        g.addSeries(gauge);
        Chart r = new Chart().setTitle("Radar");
        radar = new RadarSeries("Alice", "Speed", "Power", "Range", "Defense", "Stamina").setValues(8, 6, 7, 5, 9);
        r.addSeries(radar);
        Chart t = new Chart().setTitle("Treemap");
        tree = AbstractChart2ScreenshotTest.portfolio();
        t.addSeries(new TreemapSeries("Portfolio", tree));
        Chart fg = new Chart().setTitle("Flame graph").setLegendPosition(Chart.LEGEND_NONE);
        profile = Chart2Gallery.profile();
        fg.addSeries(new FlameGraphSeries("CPU", profile));
        return new Chart[]{p, f, g, r, t, fg};
    }

    @Override
    protected void startAnimation(Chart[] c) {
        long start = getAnimationStartTime();
        // let the entry animations finish, then change the data at the strip's start time
        AnimationTime.setTime(start + 10000);
        for (Chart ch : c) {
            ch.animate();
        }
        AnimationTime.setTime(start);
        pie.setData(new String[]{"A", "B", "C"}, new double[]{25, 35, 40});
        funnel.setData(new String[]{"Visited", "Trial", "Paid"}, new double[]{1000, 700, 450});
        gauge.setValue(85);
        radar.setValues(4, 9, 3, 8, 5);
        // the largest holding shrinks and the cash grows
        tree.getChild(0).getChild(0).setValue(20);
        tree.getChild(3).setValue(140);
        // rendering got faster and the collector slower
        profile.getChild(0).getChild(1).getChild(0).getChild(0).setValue(4);
        profile.getChild(1).getChild(0).setValue(30);
    }

    @Override
    protected void finishCapture() {
        pie = null;
        funnel = null;
        gauge = null;
        radar = null;
        tree = null;
        profile = null;
        super.finishCapture();
    }
}
