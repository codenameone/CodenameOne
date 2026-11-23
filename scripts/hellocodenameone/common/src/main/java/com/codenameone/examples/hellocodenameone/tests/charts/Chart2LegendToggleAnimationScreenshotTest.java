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

/// Tapping a legend entry hides its series with an animation: a bar series shrinks away and
/// the other one widens into its room, a stacked area's upper band collapses, a pie slice
/// closes and the others share its angle, and a radar polygon fades.
public class Chart2LegendToggleAnimationScreenshotTest extends AbstractChart2GridAnimationScreenshotTest {
    @Override
    protected Chart[] buildCharts() {
        return new Chart[]{Chart2Gallery.groupedBars(new Chart()), Chart2Gallery.stackedArea(new Chart()),
                Chart2Gallery.pie(new Chart()), Chart2Gallery.radar(new Chart())};
    }

    @Override
    protected void startAnimation(Chart[] c) {
        long start = getAnimationStartTime();
        AnimationTime.setTime(start + 10000);
        for (Chart ch : c) {
            ch.animate();
        }
        AnimationTime.setTime(start);
        // what a tap on each chart's first legend entry does
        for (Chart ch : c) {
            ch.getSeries().get(0).toggleLegendEntry(0);
        }
    }
}
