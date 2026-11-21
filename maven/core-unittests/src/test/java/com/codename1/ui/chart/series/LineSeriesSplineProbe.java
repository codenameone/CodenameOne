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
package com.codename1.ui.chart.series;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/// Exposes the spline segments a line series draws, for tests outside this package.
public final class LineSeriesSplineProbe {
    private LineSeriesSplineProbe() {
    }

    /// The cubic segments of the spline through the points, each as {c1x, c1y, c2x, c2y, x, y}.
    public static List<float[]> cubics(float[] px, float[] py, boolean rotated) {
        GeneralPath p = new GeneralPath();
        p.moveTo(px[0], py[0]);
        LineSeries.appendSpline(p, px, py, 0, px.length - 1, rotated);
        List<float[]> out = new ArrayList<float[]>();
        float[] c = new float[6];
        for (PathIterator it = p.getPathIterator(); !it.isDone(); it.next()) {
            if (it.currentSegment(c) == PathIterator.SEG_CUBICTO) {
                out.add(new float[]{c[0], c[1], c[2], c[3], c[4], c[5]});
            }
        }
        return out;
    }
}
