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

import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/// A chart that joins the points of each series with a line.
///
/// The points are joined in the order of their X values by default, and
/// in the order of the data when the sorting policy is
/// [SortingPolicy#NONE]; a policy of [SortingPolicy#Y_AXIS] orders them
/// by their Y values. A symbol is drawn at each point unless
/// `createSymbols` is off. See [Chart] for what the charts of this layer
/// leave out.
public class LineChart<X, Y> extends XYChart<X, Y> {

    /// The order the points of a series are joined in.
    public enum SortingPolicy {
        /// The order of the data.
        NONE,
        /// The order of the X values.
        X_AXIS,
        /// The order of the Y values.
        Y_AXIS
    }

    private final BooleanProperty createSymbols = new FxBoolean(this, "createSymbols", true, Dirty.PAINT);
    private final ObjectProperty<SortingPolicy> axisSortingPolicy = new FxObject<SortingPolicy>(this,
            "axisSortingPolicy", SortingPolicy.X_AXIS, Dirty.PAINT);

    /// Creates a line chart.
    public LineChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
    }

    /// Creates a line chart of the given series.
    public LineChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
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

    /// Returns the order the points of a series are joined in.
    public final SortingPolicy getAxisSortingPolicy() {
        return axisSortingPolicy.get();
    }

    /// Sets the order the points of a series are joined in.
    public final void setAxisSortingPolicy(SortingPolicy value) {
        axisSortingPolicy.set(value);
    }

    /// The order the points of a series are joined in.
    public final ObjectProperty<SortingPolicy> axisSortingPolicyProperty() {
        return axisSortingPolicy;
    }

    /// The places of the points of a series that the axes have a place
    /// for, ordered along one axis: 0 for X, 1 for Y, anything else for
    /// the order of the data.
    static <X, Y> List<double[]> places(Series<X, Y> series, Scale sx, Scale sy, double x, double y, double w,
            double h, int orderBy) {
        List<double[]> out = new ArrayList<double[]>();
        ObservableList<Data<X, Y>> items = series.getData();
        for (int i = 0; i < items.size(); i++) {
            Data<X, Y> item = items.get(i);
            if (item == null) {
                continue;
            }
            double[] p = place(item, sx, sy, x, y, w, h);
            if (p.length < 2) {
                continue;
            }
            int at = out.size();
            if (orderBy == 0) {
                while (at > 0 && out.get(at - 1)[0] > p[0]) {
                    at--;
                }
            } else if (orderBy == 1) {
                // A greater Y value is higher up, so its pixel is smaller.
                while (at > 0 && out.get(at - 1)[1] < p[1]) {
                    at--;
                }
            }
            out.add(at, p);
        }
        return out;
    }

    static void polyline(Renderer renderer, List<double[]> points, Color color, double width) {
        if (points.size() < 2) {
            return;
        }
        FxPath path = new FxPath();
        path.moveTo(points.get(0)[0], points.get(0)[1]);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i)[0], points.get(i)[1]);
        }
        renderer.stroke(path, color, width, StrokeLineCap.BUTT, StrokeLineJoin.ROUND, 10, null, 0);
    }

    @Override
    void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y, double w,
            double h) {
        SortingPolicy policy = getAxisSortingPolicy();
        int orderBy = policy == SortingPolicy.X_AXIS ? 0 : policy == SortingPolicy.Y_AXIS ? 1 : 2;
        for (int s = 0; s < series.size(); s++) {
            List<double[]> points = places(series.get(s), sx, sy, x, y, w, h, orderBy);
            polyline(renderer, points, color(s), 3);
            if (getCreateSymbols()) {
                for (int i = 0; i < points.size(); i++) {
                    symbol(renderer, points.get(i)[0], points.get(i)[1], color(s));
                }
            }
        }
    }
}
