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
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Golden review: a category axis with room for a single label drew none.
public class ChartReviewRound33Test extends UITestBase {

    @FormTest
    public void aLoneCategoryLabelKeepsTheWholeAxis() {
        String[] cats = new String[24];
        double[] v = new double[24];
        for (int i = 0; i < cats.length; i++) {
            cats[i] = "Department number " + (i + 1);
            v[i] = i + 1;
        }
        Chart c = new Chart().setXAxis(new CategoryAxis(Axis.BOTTOM, cats));
        c.addSeries(new BarSeries("b").setData(null, v));
        c.setAnimationDuration(0);
        c.setX(0);
        c.setY(0);
        // wide enough for one label, not for two
        c.setWidth(260);
        c.setHeight(300);
        implementation.setRecordDrawnStrings(true);
        try {
            c.paint(Image.createImage(260, 300).getGraphics());
            List<Object[]> drawn = implementation.getDrawnStrings();
            boolean found = false;
            for (Object[] d : drawn) {
                found |= ((String) d[0]).startsWith("Department");
            }
            assertTrue(found, "no category label was drawn");
        } finally {
            implementation.setRecordDrawnStrings(false);
        }
    }
}
