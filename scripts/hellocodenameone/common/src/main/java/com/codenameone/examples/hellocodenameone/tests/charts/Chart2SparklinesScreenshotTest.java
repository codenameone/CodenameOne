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
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.chart.Sparkline;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// A list of rows, each with an inline sparkline of a different type.
public class Chart2SparklinesScreenshotTest extends AbstractChart2ScreenshotTest {

    @Override
    protected Component buildChart() {
        Container list = new Container(BoxLayout.y());
        String[] names = {"ACME", "Globex", "Initech", "Hooli"};
        int[] types = {Sparkline.LINE, Sparkline.AREA, Sparkline.BARS, Sparkline.LINE};
        for (int i = 0; i < names.length; i++) {
            double[] v = new double[20];
            for (int k = 0; k < v.length; k++) {
                v[k] = ((k * (i + 3) * 17) % 23) - (i == 2 ? 11 : 0);
            }
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.CENTER, new Label(names[i]));
            row.add(BorderLayout.EAST, new Sparkline(v).setType(types[i]).setShowExtremes(i == 3));
            list.add(row);
        }
        return list;
    }

    @Override
    protected String screenshotName() {
        return "chart2-sparklines";
    }
}
