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
package javafx.scene.chart;

import java.util.List;

import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.collections.ObservableList;

/// A chart that draws a symbol at each point and joins nothing.
///
/// Every series is drawn with a filled circle of its colour; JavaFX
/// gives each series a symbol of its own shape. See [Chart] for what the
/// charts of this layer leave out.
public class ScatterChart<X, Y> extends XYChart<X, Y> {

    /// Creates a scatter chart.
    public ScatterChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    /// Creates a scatter chart of the given series.
    public ScatterChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis);
        setData(data);
    }

    @Override
    void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y, double w,
            double h) {
        for (int s = 0; s < series.size(); s++) {
            List<double[]> points = LineChart.places(series.get(s), sx, sy, x, y, w, h, 2);
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                FxPath dot = new FxPath();
                dot.addEllipse(p[0], p[1], 5, 5);
                renderer.fill(dot, color(s), p[0] - 5, p[1] - 5, 10, 10);
            }
        }
    }
}
