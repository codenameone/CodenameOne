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
package com.codename1.ui.chart;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A chart whose value axis does not reach zero (a line between 12 and 24 on an axis from 10)
/// grows in from the bottom of the axis. It grew out of zero, below the plot, so the first part
/// of the entry drew nothing and the screenshot suite saw two identical frames.
public class EntryAboveAxisFloorTest extends UITestBase {

    @FormTest
    public void entryStartsOnTheVisibleAxis() {
        AnimationTime.setTime(10000);
        try {
            LineSeries s = new LineSeries("l");
            s.setData(null, new double[]{12, 19, 15, 24, 21});
            Chart c = new Chart().addSeries(s);
            c.setAnimationDuration(600);
            Form f = new Form(new BorderLayout());
            f.add(BorderLayout.CENTER, c);
            f.show();
            c.setX(0);
            c.setY(0);
            c.setWidth(600);
            c.setHeight(400);
            AnimationTime.setTime(10060);
            c.paint(Image.createImage(600, 400).getGraphics());
            ChartContext ctx = c.getLastContext();
            assertNotNull(ctx);
            Axis a = ctx.getValueAxis(s);
            double floor = Math.min(a.getVisibleMin(), a.getVisibleMax());
            assertTrue(floor > 0, "the axis starts above zero: " + floor);
            assertTrue(ctx.getProgress() < 1, "the entry is still running");
            for (int i = 0; i < s.size(); i++) {
                double y = s.animatedY(ctx, i);
                assertTrue(y > floor, "point " + i + " is above the bottom of the plot early in the entry: "
                        + y + " vs " + floor);
                assertTrue(y < s.getY(i), "point " + i + " is still growing: " + y);
            }
        } finally {
            AnimationTime.reset();
        }
    }
}
