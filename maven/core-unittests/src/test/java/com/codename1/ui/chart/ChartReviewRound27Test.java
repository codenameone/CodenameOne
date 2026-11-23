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
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.axis.TimeAxis;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound27Test extends UITestBase {

    private static final int W = 400;
    private static final int H = 300;

    @AfterEach
    public void resetTime() {
        AnimationTime.reset();
    }

    private void paint(com.codename1.ui.Component c) {
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        c.paint(Image.createImage(W, H).getGraphics());
    }

    @FormTest
    public void fixedMinAtMaxValueKeepsARange() {
        NumberAxis a = new NumberAxis(Axis.LEFT);
        a.setRange(Double.MAX_VALUE, null);
        a.setDataBounds(1, 2);
        assertTrue(a.getVisibleMax() > a.getVisibleMin(), a.getVisibleMin() + ".." + a.getVisibleMax());
        assertTrue(!Double.isInfinite(a.getVisibleMax()), "finite");
        NumberAxis b = new NumberAxis(Axis.LEFT);
        b.setRange(null, -Double.MAX_VALUE);
        b.setDataBounds(1, 2);
        assertTrue(b.getVisibleMax() > b.getVisibleMin(), b.getVisibleMin() + ".." + b.getVisibleMax());
        assertTrue(!Double.isInfinite(b.getVisibleMin()), "finite");
    }

    @FormTest
    public void hugeTimestampPadIsNotLost() {
        TimeAxis t = new TimeAxis(Axis.BOTTOM);
        t.setDataBounds(1e25, 1e25);
        assertTrue(t.getVisibleMin() < t.getVisibleMax(), t.getVisibleMin() + ".." + t.getVisibleMax());
    }

}
