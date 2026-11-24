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

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.chart.series.ScatterSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// A dot or bubble at the edge of the data is drawn whole. The axes used to end exactly at
/// the extreme points, so those were drawn on the plot's border with half of them clipped.
public class ScatterEdgeMarginTest extends UITestBase {

    private static void paint(Chart c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(400);
        c.setHeight(400);
        c.paint(Image.createImage(400, 400).getGraphics());
    }

    @Test
    public void theAxesLeaveRoomAroundTheExtremePoints() {
        Chart c = new Chart().setAnimationDuration(0);
        c.addSeries(new ScatterSeries("People").setData(
                new double[]{158, 170, 190}, new double[]{52, 66, 90}, null));
        paint(c);
        double xr = 190 - 158;
        assertTrue(c.getXAxis().getVisibleMin() <= 158 - xr * 0.03, "x min " + c.getXAxis().getVisibleMin());
        assertTrue(c.getXAxis().getVisibleMax() >= 190 + xr * 0.03, "x max " + c.getXAxis().getVisibleMax());
        double yr = 90 - 52;
        assertTrue(c.getYAxis().getVisibleMax() >= 90 + yr * 0.03, "y max " + c.getYAxis().getVisibleMax());
    }

    @Test
    public void bubblesGetAWiderMargin() {
        Chart c = new Chart().setAnimationDuration(0);
        c.addSeries(new ScatterSeries("Regions").setData(
                new double[]{2, 6, 10}, new double[]{12, 9, 22}, new double[]{30, 45, 120}));
        paint(c);
        assertTrue(c.getXAxis().getVisibleMin() <= 2 - 8 * 0.07, "x min " + c.getXAxis().getVisibleMin());
        assertTrue(c.getXAxis().getVisibleMax() >= 10 + 8 * 0.07, "x max " + c.getXAxis().getVisibleMax());
    }
}
