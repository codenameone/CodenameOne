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

/// A data point the user tapped, hovered or selected.
public final class ChartPoint {
    private final Series series;
    private final int index;
    private final double x;
    private final double y;
    private final int screenX;
    private final int screenY;
    private final String label;

    /// Creates a point reference.
    ///
    /// #### Parameters
    ///
    /// - `series`: the series the point belongs to
    ///
    /// - `index`: the point's index in the series
    ///
    /// - `x`: its x value (or category index)
    ///
    /// - `y`: its value
    ///
    /// - `screenX`: where it is drawn, relative to the chart
    ///
    /// - `screenY`: where it is drawn, relative to the chart
    ///
    /// - `label`: a name for the point (a pie slice or treemap cell), or null
    public ChartPoint(Series series, int index, double x, double y, int screenX, int screenY, String label) {
        this.series = series;
        this.index = index;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.label = label;
    }

    /// The series.
    public Series getSeries() {
        return series;
    }

    /// The point's index within its series.
    public int getIndex() {
        return index;
    }

    /// The x value.
    public double getX() {
        return x;
    }

    /// The value.
    public double getY() {
        return y;
    }

    /// Screen x relative to the chart component.
    public int getScreenX() {
        return screenX;
    }

    /// Screen y relative to the chart component.
    public int getScreenY() {
        return screenY;
    }

    /// The point's own name, or null.
    public String getLabel() {
        return label;
    }
}
