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
/// Auto ranging does what JavaFX does: the range of the data is padded
/// by a hundredth of itself at each end, never across zero, and rounded
/// outwards to a tick unit of 1, 2.5 or 5 times a power of ten - the
/// smallest that gives a tick about every two label heights, no more
/// than twenty ticks, and labels that do not touch. The unit it chose is
/// written into `tickUnit`, and decides the decimals of the labels.
///
/// A label of an axis with a fixed range has its thousands grouped and
/// up to three decimals, as `3,000` and `0.25`.
public final class NumberAxis extends ValueAxis<Number> {

    private final BooleanProperty forceZeroInRange = new FxBoolean(this, "forceZeroInRange", true, Dirty.PAINT);
    private final DoubleProperty tickUnit = new FxDouble(this, "tickUnit", 5, Dirty.PAINT);
    private int decimals = -1;
    private boolean grouping = true;

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

    /// Returns the distance between two ticks.
    public final double getTickUnit() {
        return tickUnit.get();
    }

    /// Sets the distance between two ticks of a fixed range.
    public final void setTickUnit(double value) {
        tickUnit.set(value);
    }

    /// The distance between two ticks.
    public final DoubleProperty tickUnitProperty() {
        return tickUnit;
    }

    @Override
    double unit() {
        return getTickUnit();
    }

    @Override
    void unit(double value) {
        tickUnit.set(value);
    }

    @Override
    void format(int decimals, boolean grouping) {
        this.decimals = decimals;
        this.grouping = grouping;
    }

    @Override
    boolean zeroInRange() {
        return isForceZeroInRange();
    }

    /// Writes a number with a fixed count of decimals, or with up to
    /// three and no trailing zeros when the count is negative.
    static String plain(double value, int decimals, boolean grouping) {
        if (value != value || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        int places = decimals < 0 ? 3 : Math.min(decimals, 9);
        long scale = 1;
        for (int i = 0; i < places; i++) {
            scale *= 10;
        }
        double magnitude = Math.abs(value) * scale;
        if (magnitude > 9.0e17) {
            return String.valueOf(value);
        }
        long scaled = Math.round(magnitude);
        String whole = String.valueOf(scaled / scale);
        StringBuilder out = new StringBuilder();
        if (value < 0 && scaled != 0) {
            out.append('-');
        }
        for (int i = 0; i < whole.length(); i++) {
            if (grouping && i > 0 && (whole.length() - i) % 3 == 0) {
                out.append(',');
            }
            out.append(whole.charAt(i));
        }
        if (places > 0) {
            StringBuilder fraction = new StringBuilder(String.valueOf(scaled % scale));
            while (fraction.length() < places) {
                fraction.insert(0, '0');
            }
            int keep = fraction.length();
            if (decimals < 0) {
                while (keep > 0 && fraction.charAt(keep - 1) == '0') {
                    keep--;
                }
            }
            if (keep > 0) {
                out.append('.');
                for (int i = 0; i < keep; i++) {
                    out.append(fraction.charAt(i));
                }
            }
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
        return plain(n.doubleValue(), decimals, grouping);
    }

    /// A formatter of tick labels that writes a number the way the axis
    /// does, between a prefix and a suffix.
    public static class DefaultFormatter extends StringConverter<Number> {

        private final String prefix;
        private final String suffix;

        /// Creates a formatter for an axis.
        public DefaultFormatter(NumberAxis axis) {
            this(axis, null, null);
        }

        /// Creates a formatter that puts a text before and after a number.
        public DefaultFormatter(NumberAxis axis, String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public String toString(Number object) {
            if (object == null) {
                return "";
            }
            double v = object.doubleValue();
            String number = v == Math.rint(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.valueOf(v);
            return (prefix == null ? "" : prefix) + number + (suffix == null ? "" : suffix);
        }

        @Override
        public Number fromString(String string) {
            if (string == null) {
                return null;
            }
            int from = prefix == null || !string.startsWith(prefix) ? 0 : prefix.length();
            int to = suffix == null || suffix.length() == 0 || !string.endsWith(suffix) ? string.length()
                    : string.length() - suffix.length();
            if (to <= from) {
                return null;
            }
            try {
                return Double.valueOf(string.substring(from, to).trim());
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }
    }
}
