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

import com.codename1.ui.chart.Chart;

/// The entry animation of a bar and line chart when it is first shown: bars grow from the
/// baseline and the line draws in. The entry runs 1.6 times the animation duration, so a 625ms
/// duration gives the grid's 1000ms.
public class Chart2GrowInAnimationScreenshotTest extends AbstractChart2AnimationScreenshotTest {
    @Override
    protected Chart buildChart() {
        Chart c = new Chart();
        c.setAnimationDuration(625);
        return AbstractChart2ScreenshotTest.monthlySales(c);
    }

    @Override
    protected int getAnimationDurationMillis() {
        return 1000;
    }

    @Override
    protected int framesThatMustMove() {
        // the 625 ms entry spans the first four frames of the 1000 ms strip
        return 3;
    }
}
