/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.transitions.SeriesTransition;
import com.codename1.charts.transitions.XYSeriesTransition;

class ChartTransitionSnippet {

    ChartComponent chart;
    XYSeries readings;

    void snippet() {
        // tag::chart-transition[]
        XYSeriesTransition transition = new XYSeriesTransition(chart, readings);
        transition.setEasing(SeriesTransition.EASING_IN_OUT);
        transition.setDuration(600);

        // The buffer starts out empty. Write the NEW shape of the series into
        // it -- every point, not only the ones that changed -- because the
        // buffer is what the series is tweened towards.
        XYSeries next = transition.getBuffer();
        next.add(0, 12);
        next.add(1, 19);
        next.add(2, 7);

        transition.animateChart();
        // end::chart-transition[]
    }

    void immediate() {
        // tag::chart-transition-immediate[]
        XYSeriesTransition transition = new XYSeriesTransition(chart, readings);
        transition.getBuffer().add(3, 22);
        transition.updateChart();
        // end::chart-transition-immediate[]
    }
}
