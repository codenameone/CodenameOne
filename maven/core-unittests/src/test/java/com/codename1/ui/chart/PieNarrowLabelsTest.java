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
import com.codename1.ui.chart.series.PieSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// A pie on a phone held upright still names its slices. One line labels ("Samsung 0.6%")
/// leave such a pie too small, and the pie used to drop every outside label and rely on the
/// legend; it now stacks each name over its percentage, which takes half the width.
public class PieNarrowLabelsTest extends UITestBase {

    @Test
    public void aNarrowPieLabelsEverySliceOutside() {
        Chart c = new Chart().setTitle("Mobile OS share").setAnimationDuration(0);
        c.addSeries(new PieSeries("Share").setData(
                new String[]{"Android", "iOS", "Samsung", "KaiOS", "Other"},
                new double[]{70.1, 28.5, 0.6, 0.3, 0.5}));
        int w = 340;
        int h = 860;
        c.setX(0);
        c.setY(0);
        c.setWidth(w);
        c.setHeight(h);
        c.paint(Image.createImage(w, h).getGraphics());
        ChartContext ctx = c.getLastContext();
        float cx = ctx.getPlotX() + ctx.getPlotWidth() / 2f;
        float cy = ctx.getPlotY() + ctx.getPlotHeight() / 2f;
        float half = Math.min(ctx.getPlotWidth(), ctx.getPlotHeight()) / 2f;
        int outside = 0;
        for (int i = 0; i < ctx.getLabels().size(); i++) {
            int[] r = ctx.getLabels().getRect(i);
            float dx = Math.max(Math.abs(r[0] + r[2] / 2f - cx) - r[2] / 2f, 0);
            float dy = r[1] + r[3] / 2f - cy;
            // a label wholly to one side of a pie smaller than the plot is an outside label
            if (dx > 0 && Math.abs(dy) < half && (r[0] > cx || r[0] + r[2] < cx)) {
                double reach = Math.sqrt(dx * dx + dy * dy);
                if (reach > half * 0.3) {
                    outside++;
                }
            }
        }
        assertTrue(outside >= 5, "expected all five slices labelled outside the pie, got " + outside);
    }
}
