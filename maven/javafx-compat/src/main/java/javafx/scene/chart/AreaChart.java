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

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/// A chart that fills the area between the line of each series and zero.
///
/// The points of a series are joined in the order of their X values; the
/// fill is the colour of the series at a fifth of its strength, with a
/// line of one pixel over it and a small ring at each point. See [Chart]
/// for what the charts of this layer leave out.
public class AreaChart<X, Y> extends XYChart<X, Y> {

    private final BooleanProperty createSymbols = new FxBoolean(this, "createSymbols", true, Dirty.PAINT);

    /// Creates an area chart.
    public AreaChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    /// Creates an area chart of the given series.
    public AreaChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis);
        setData(data);
    }

    /// Returns whether a symbol is drawn at each point.
    public final boolean getCreateSymbols() {
        return createSymbols.get();
    }

    /// Sets whether a symbol is drawn at each point.
    public final void setCreateSymbols(boolean value) {
        createSymbols.set(value);
    }

    /// Whether a symbol is drawn at each point.
    public final BooleanProperty createSymbolsProperty() {
        return createSymbols;
    }

    @Override
    double[] legendSymbolSize(int index) {
        return new double[] {12, 12};
    }

    @Override
    void legendSymbol(Renderer renderer, int index, double x, double y) {
        ring(renderer, x + 6, y + 6, 6, 3, color(index));
    }

    @Override
    void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y, double w,
            double h) {
        double base = y + (sy.category ? h : sy.base());
        for (int s = 0; s < series.size(); s++) {
            List<double[]> points = LineChart.places(series.get(s), sx, sy, x, y, 0);
            if (points.isEmpty()) {
                continue;
            }
            Color color = color(s);
            if (points.size() > 1) {
                FxPath area = new FxPath();
                area.moveTo(points.get(0)[0], base);
                for (int i = 0; i < points.size(); i++) {
                    area.lineTo(points.get(i)[0], points.get(i)[1]);
                }
                area.lineTo(points.get(points.size() - 1)[0], base);
                area.closePath();
                renderer.fill(area, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.2), x, y, w,
                        h);
            }
            LineChart.polyline(renderer, points, color, 1);
            if (getCreateSymbols()) {
                for (int i = 0; i < points.size(); i++) {
                    ring(renderer, points.get(i)[0], points.get(i)[1], 3, 2, color);
                }
            }
        }
    }
}
