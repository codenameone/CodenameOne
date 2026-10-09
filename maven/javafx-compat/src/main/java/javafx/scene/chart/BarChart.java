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
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.DoubleProperty;
import javafx.collections.ObservableList;

/// A chart of bars, one for each data item, grouped by category.
///
/// One axis is a [CategoryAxis] and the other a [ValueAxis]: the bars
/// stand upright when the categories are on the X axis and lie on their
/// side when they are on the Y axis. A bar grows from zero, so the value
/// axis always holds zero when it takes its range from the data. See
/// [Chart] for what the charts of this layer leave out.
public class BarChart<X, Y> extends XYChart<X, Y> {

    private final DoubleProperty barGap = new FxDouble(this, "barGap", 4, Dirty.PAINT);
    private final DoubleProperty categoryGap = new FxDouble(this, "categoryGap", 10, Dirty.PAINT);

    /// Creates a bar chart.
    ///
    /// @throws IllegalArgumentException unless one axis is a value axis
    ///     and the other a category axis
    public BarChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
        boolean upright = xAxis instanceof CategoryAxis && yAxis instanceof ValueAxis;
        boolean sideways = yAxis instanceof CategoryAxis && xAxis instanceof ValueAxis;
        if (!upright && !sideways) {
            throw new IllegalArgumentException("Axis type incorrect, one of X,Y should be CategoryAxis and the "
                    + "other NumberAxis");
        }
    }

    /// Creates a bar chart of the given series.
    public BarChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        this(xAxis, yAxis);
        setData(data);
    }

    /// Creates a bar chart of the given series with a gap between the
    /// categories.
    public BarChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data, double categoryGap) {
        this(xAxis, yAxis);
        setData(data);
        setCategoryGap(categoryGap);
    }

    /// Returns the gap between two bars of one category.
    public final double getBarGap() {
        return barGap.get();
    }

    /// Sets the gap between two bars of one category.
    public final void setBarGap(double value) {
        barGap.set(value);
    }

    /// The gap between two bars of one category.
    public final DoubleProperty barGapProperty() {
        return barGap;
    }

    /// Returns the gap between the bars of two categories.
    public final double getCategoryGap() {
        return categoryGap.get();
    }

    /// Sets the gap between the bars of two categories.
    public final void setCategoryGap(double value) {
        categoryGap.set(value);
    }

    /// The gap between the bars of two categories.
    public final DoubleProperty categoryGapProperty() {
        return categoryGap;
    }

    @Override
    boolean zeroBased() {
        return true;
    }

    @Override
    void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y, double w,
            double h) {
        boolean upright = sx.category;
        Scale names = upright ? sx : sy;
        Scale values = upright ? sy : sx;
        int count = names.names.size();
        int groups = series.size();
        if (count == 0 || groups == 0 || values.category) {
            return;
        }
        double band = (upright ? w : h) / count;
        double room = Math.max(1, band - Math.max(0, getCategoryGap()));
        double gap = Math.max(0, getBarGap());
        double bar = (room - gap * (groups - 1)) / groups;
        if (bar < 1) {
            gap = 0;
            bar = room / groups;
        }
        double zero = values.zero();
        for (int s = 0; s < groups; s++) {
            ObservableList<Data<X, Y>> items = series.get(s).getData();
            for (int i = 0; i < items.size(); i++) {
                Data<X, Y> item = items.get(i);
                if (item == null) {
                    continue;
                }
                double at = names.at(upright ? (Object) item.getXValue() : (Object) item.getYValue());
                double value = values.at(upright ? (Object) item.getYValue() : (Object) item.getXValue());
                if (at != at || value != value) {
                    continue;
                }
                value = Math.max(0, Math.min(1, value));
                double offset = -room / 2 + s * (bar + gap);
                double from = Math.min(zero, value);
                double length = Math.abs(value - zero);
                if (upright) {
                    renderer.fillRect(x + at * w + offset, y + h - (from + length) * h, bar, length * h, color(s));
                } else {
                    // The first category is at the bottom of a Y axis.
                    renderer.fillRect(x + from * w, y + h - at * h + offset, length * w, bar, color(s));
                }
            }
        }
    }
}
