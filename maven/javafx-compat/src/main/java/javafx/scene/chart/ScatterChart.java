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
import javafx.scene.paint.Color;

/// A chart that draws a symbol at each point and joins nothing.
///
/// Each series has a symbol of its own shape as well as its own colour,
/// the eight of JavaFX in turn: a disc, a square, a diamond, a cross, a
/// triangle, and then a ring, a square and a diamond with white middles.
/// See [Chart] for what the charts of this layer leave out.
public class ScatterChart<X, Y> extends XYChart<X, Y> {

    private static final double[] CROSS = {2, 0, 5, 4, 8, 0, 10, 0, 10, 2, 6, 5, 10, 8, 10, 10, 8, 10, 5, 6, 2, 10,
        0, 10, 0, 8, 4, 5, 0, 2, 0, 0};

    /// Creates a scatter chart.
    public ScatterChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    /// Creates a scatter chart of the given series.
    public ScatterChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis);
        setData(data);
    }

    private static int shape(int series) {
        return (series < 0 ? 0 : series) % 8;
    }

    private static void diamond(Renderer renderer, double cx, double cy, double halfWidth, double halfHeight,
            Color color) {
        FxPath path = new FxPath();
        path.moveTo(cx, cy - halfHeight);
        path.lineTo(cx + halfWidth, cy);
        path.lineTo(cx, cy + halfHeight);
        path.lineTo(cx - halfWidth, cy);
        path.closePath();
        renderer.fill(path, color, cx - halfWidth, cy - halfHeight, halfWidth * 2, halfHeight * 2);
    }

    /// Draws the symbol of a series about a point.
    static void mark(Renderer renderer, int series, double cx, double cy) {
        Color color = color(series);
        int shape = shape(series);
        if (shape == 0) {
            ring(renderer, cx, cy, 5, 0, color);
        } else if (shape == 1) {
            renderer.fillRect(cx - 5, cy - 5, 10, 10, color);
        } else if (shape == 2) {
            diamond(renderer, cx, cy, 5, 7, color);
        } else if (shape == 3) {
            FxPath path = new FxPath();
            path.moveTo(cx - 5 + CROSS[0], cy - 5 + CROSS[1]);
            for (int i = 2; i + 1 < CROSS.length; i += 2) {
                path.lineTo(cx - 5 + CROSS[i], cy - 5 + CROSS[i + 1]);
            }
            path.closePath();
            renderer.fill(path, color, cx - 5, cy - 5, 10, 10);
        } else if (shape == 4) {
            FxPath path = new FxPath();
            path.moveTo(cx, cy - 5);
            path.lineTo(cx + 5, cy + 5);
            path.lineTo(cx - 5, cy + 5);
            path.closePath();
            renderer.fill(path, color, cx - 5, cy - 5, 10, 10);
        } else if (shape == 5) {
            ring(renderer, cx, cy, 5, 3, color);
        } else if (shape == 6) {
            renderer.fillRect(cx - 5, cy - 5, 10, 10, color);
            renderer.fillRect(cx - 3, cy - 3, 6, 6, Color.WHITE);
        } else {
            diamond(renderer, cx, cy, 5, 7, color);
            diamond(renderer, cx, cy, 2.5, 4.5, Color.WHITE);
        }
    }

    @Override
    double[] legendSymbolSize(int index) {
        int shape = shape(index);
        return new double[] {10, shape == 2 || shape == 7 ? 14 : 10};
    }

    @Override
    void legendSymbol(Renderer renderer, int index, double x, double y) {
        double[] size = legendSymbolSize(index);
        mark(renderer, index, x + size[0] / 2, y + size[1] / 2);
    }

    @Override
    void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y, double w,
            double h) {
        for (int s = 0; s < series.size(); s++) {
            List<double[]> points = LineChart.places(series.get(s), sx, sy, x, y, 2);
            for (int i = 0; i < points.size(); i++) {
                mark(renderer, s, points.get(i)[0], points.get(i)[1]);
            }
        }
    }
}
