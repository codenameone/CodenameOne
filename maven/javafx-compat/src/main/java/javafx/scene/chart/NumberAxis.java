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
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.util.StringConverter;

/// An axis of plain numbers, with a tick every `tickUnit`.
///
/// Auto ranging rounds the range of the data outwards to a tick unit of
/// 1, 2, 2.5 or 5 times a power of ten that gives about ten ticks or
/// fewer; JavaFX pads the range by a fraction of itself first, so the
/// two can choose bounds that differ by a tick.
public final class NumberAxis extends ValueAxis<Number> {

    private final BooleanProperty forceZeroInRange = new FxBoolean(this, "forceZeroInRange", true, Dirty.PAINT);
    private final DoubleProperty tickUnit = new FxDouble(this, "tickUnit", 5, Dirty.PAINT);

    /// Creates an auto ranging axis.
    public NumberAxis() {
    }

    /// Creates an axis with a fixed range and tick unit.
    public NumberAxis(double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        this.tickUnit.set(tickUnit);
    }

    /// Creates a labelled axis with a fixed range and tick unit.
    public NumberAxis(String axisLabel, double lowerBound, double upperBound, double tickUnit) {
        super(lowerBound, upperBound);
        this.tickUnit.set(tickUnit);
        setLabel(axisLabel);
    }

    /// Returns whether zero is kept in a range taken from the data.
    public final boolean isForceZeroInRange() {
        return forceZeroInRange.get();
    }

    /// Sets whether zero is kept in a range taken from the data.
    public final void setForceZeroInRange(boolean value) {
        forceZeroInRange.set(value);
    }

    /// Whether zero is kept in a range taken from the data.
    public final BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    /// Returns the distance between two ticks of a fixed range.
    public final double getTickUnit() {
        return tickUnit.get();
    }

    /// Sets the distance between two ticks of a fixed range.
    public final void setTickUnit(double value) {
        tickUnit.set(value);
    }

    /// The distance between two ticks of a fixed range.
    public final DoubleProperty tickUnitProperty() {
        return tickUnit;
    }

    @Override
    double unit() {
        return getTickUnit();
    }

    @Override
    boolean zeroInRange() {
        return isForceZeroInRange();
    }

    /// Writes a number with as many decimals as the tick unit has, and no
    /// more than six.
    static String plain(double value, double unit) {
        int decimals = 0;
        double u = Math.abs(unit);
        while (decimals < 6 && u > 0 && Math.abs(u - Math.round(u)) > 1e-9) {
            u *= 10;
            decimals++;
        }
        double scale = 1;
        for (int i = 0; i < decimals; i++) {
            scale *= 10;
        }
        long scaled = Math.round(Math.abs(value) * scale);
        boolean negative = value < 0 && scaled != 0;
        long whole = (long) (scaled / scale);
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        out.append(whole);
        if (decimals > 0) {
            String fraction = String.valueOf(scaled - (long) (whole * scale));
            out.append('.');
            for (int i = fraction.length(); i < decimals; i++) {
                out.append('0');
            }
            out.append(fraction);
        }
        return out.toString();
    }

    @Override
    String text(Object value) {
        if (!(value instanceof Number)) {
            return String.valueOf(value);
        }
        Number n = (Number) value;
        StringConverter<Number> format = getTickLabelFormatter();
        if (format != null) {
            return format.toString(n);
        }
        return plain(n.doubleValue(), cn1Unit);
    }

    /// The tick unit the chart is drawing with, which decides how many
    /// decimals a label has.
    double cn1Unit = 1;
}
