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
package com.codename1.desktopcompat.javax.swing;

/// A spinner model over numbers: a value, a step, and an optional minimum
/// and maximum. The value keeps its type: an `Integer` steps to an
/// `Integer`, a `Double` to a `Double`.
///
/// A bound that is a number is compared with the value by magnitude
/// whatever the two types are, where the desktop fails on a bound of
/// another type than the value. `setValue` does not check the bounds, as
/// on the desktop.
@SuppressWarnings("rawtypes")
public class SpinnerNumberModel extends AbstractSpinnerModel {

    private Number stepSize;
    private Number value;
    private Comparable minimum;
    private Comparable maximum;

    public SpinnerNumberModel(Number value, Comparable minimum, Comparable maximum, Number stepSize) {
        if (value == null || stepSize == null) {
            throw new IllegalArgumentException("value and stepSize must be non-null");
        }
        if (!((minimum == null || compare(minimum, value) <= 0)
                && (maximum == null || compare(maximum, value) >= 0))) {
            throw new IllegalArgumentException("(minimum <= value <= maximum) is false");
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }

    public SpinnerNumberModel(int value, int minimum, int maximum, int stepSize) {
        this(Integer.valueOf(value), Integer.valueOf(minimum), Integer.valueOf(maximum), Integer.valueOf(stepSize));
    }

    public SpinnerNumberModel(double value, double minimum, double maximum, double stepSize) {
        this(Double.valueOf(value), Double.valueOf(minimum), Double.valueOf(maximum), Double.valueOf(stepSize));
    }

    public SpinnerNumberModel() {
        this(Integer.valueOf(0), null, null, Integer.valueOf(1));
    }

    private static boolean integral(Object o) {
        return o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte;
    }

    /// Compares a bound with a number: numbers by magnitude, anything else
    /// by the bound's own `compareTo`.
    @SuppressWarnings("unchecked")
    static int compare(Comparable bound, Number n) {
        if (bound instanceof Number) {
            Number b = (Number) bound;
            if (integral(b) && integral(n)) {
                long x = b.longValue();
                long y = n.longValue();
                return x < y ? -1 : x == y ? 0 : 1;
            }
            double x = b.doubleValue();
            double y = n.doubleValue();
            return x < y ? -1 : x == y ? 0 : 1;
        }
        return bound.compareTo(n);
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public void setMinimum(Comparable minimum) {
        if (!same(minimum, this.minimum)) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }

    public Comparable getMinimum() {
        return minimum;
    }

    public void setMaximum(Comparable maximum) {
        if (!same(maximum, this.maximum)) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }

    public Comparable getMaximum() {
        return maximum;
    }

    public void setStepSize(Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("null stepSize");
        }
        if (!stepSize.equals(this.stepSize)) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    public Number getStepSize() {
        return stepSize;
    }

    private Number step(int dir) {
        Number next;
        if (value instanceof Float || value instanceof Double) {
            double v = value.doubleValue() + stepSize.doubleValue() * dir;
            if (value instanceof Double) {
                next = Double.valueOf(v);
            } else {
                next = Float.valueOf((float) v);
            }
        } else {
            long v = value.longValue() + stepSize.longValue() * dir;
            if (value instanceof Long) {
                next = Long.valueOf(v);
            } else if (value instanceof Integer) {
                next = Integer.valueOf((int) v);
            } else if (value instanceof Short) {
                next = Short.valueOf((short) v);
            } else {
                next = Byte.valueOf((byte) v);
            }
        }
        if (maximum != null && compare(maximum, next) < 0) {
            return null;
        }
        if (minimum != null && compare(minimum, next) > 0) {
            return null;
        }
        return next;
    }

    @Override
    public Object getNextValue() {
        return step(1);
    }

    @Override
    public Object getPreviousValue() {
        return step(-1);
    }

    public Number getNumber() {
        return value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value)) {
            this.value = (Number) value;
            fireStateChanged();
        }
    }
}
