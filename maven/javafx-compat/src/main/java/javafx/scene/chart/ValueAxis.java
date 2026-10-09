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

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.StringConverter;

/// An axis of numbers between a lower and an upper bound.
///
/// While the axis is auto ranging, the chart writes the range it chose
/// into the bounds each time it draws. Minor ticks are not drawn; their
/// count is recorded.
public abstract class ValueAxis<T extends Number> extends Axis<T> {

    private final DoubleProperty lowerBound = new FxDouble(this, "lowerBound", 0, Dirty.PAINT);
    private final DoubleProperty upperBound = new FxDouble(this, "upperBound", 100, Dirty.PAINT);
    private final IntegerProperty minorTickCount = new SimpleIntegerProperty(this, "minorTickCount", 5);
    private final ObjectProperty<StringConverter<T>> tickLabelFormatter = new FxObject<StringConverter<T>>(this,
            "tickLabelFormatter", null, Dirty.PAINT);
    private boolean writing;

    /// Creates an auto ranging axis.
    public ValueAxis() {
    }

    /// Creates an axis with a fixed range.
    public ValueAxis(double lowerBound, double upperBound) {
        setAutoRanging(false);
        this.lowerBound.set(lowerBound);
        this.upperBound.set(upperBound);
    }

    /// Writes the range the chart chose, without asking for a repaint.
    void range(double lower, double upper) {
        writing = true;
        try {
            lowerBound.set(lower);
            upperBound.set(upper);
        } finally {
            writing = false;
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if (!writing) {
            super.cn1Invalidated(what);
        }
    }

    /// Returns the value at the start of the axis.
    public final double getLowerBound() {
        return lowerBound.get();
    }

    /// Sets the value at the start of the axis.
    public final void setLowerBound(double value) {
        lowerBound.set(value);
    }

    /// The value at the start of the axis.
    public final DoubleProperty lowerBoundProperty() {
        return lowerBound;
    }

    /// Returns the value at the end of the axis.
    public final double getUpperBound() {
        return upperBound.get();
    }

    /// Sets the value at the end of the axis.
    public final void setUpperBound(double value) {
        upperBound.set(value);
    }

    /// The value at the end of the axis.
    public final DoubleProperty upperBoundProperty() {
        return upperBound;
    }

    /// Returns the number of minor ticks asked for.
    public final int getMinorTickCount() {
        return minorTickCount.get();
    }

    /// Records a number of minor ticks; none are drawn.
    public final void setMinorTickCount(int value) {
        minorTickCount.set(value);
    }

    /// The number of minor ticks asked for.
    public final IntegerProperty minorTickCountProperty() {
        return minorTickCount;
    }

    /// Returns what turns a tick value into its label, `null` for the
    /// plain number.
    public final StringConverter<T> getTickLabelFormatter() {
        return tickLabelFormatter.get();
    }

    /// Sets what turns a tick value into its label.
    public final void setTickLabelFormatter(StringConverter<T> value) {
        tickLabelFormatter.set(value);
    }

    /// What turns a tick value into its label.
    public final ObjectProperty<StringConverter<T>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    /// The distance between two ticks when the range is fixed.
    abstract double unit();

    /// Whether zero is kept in a range taken from the data.
    abstract boolean zeroInRange();
}
