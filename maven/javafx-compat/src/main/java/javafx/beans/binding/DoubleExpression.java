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
package javafx.beans.binding;

import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;

/// A [NumberExpression] whose value is a `double`, with the arithmetic results typed
/// the way Java would type them.
public abstract class DoubleExpression extends NumberExpressionBase implements ObservableDoubleValue {

    /// Creates the expression.
    public DoubleExpression() {
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    @Override
    public double doubleValue() {
        return (double) get();
    }

    @Override
    public Double getValue() {
        return Double.valueOf(get());
    }

    /// Returns the value as an expression: the value itself when it already
    /// is one, otherwise a binding that follows it.
    public static DoubleExpression doubleExpression(final ObservableDoubleValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof DoubleExpression) {
            return (DoubleExpression) value;
        }
        return new Fn.DoubleFn(new Fn.ToDouble() {
            @Override
            public double get() {
                return value.get();
            }
        }, value);
    }

    /// Returns an expression following a boxed numeric value; a `null` value
    /// reads as zero.
    public static <T extends Number> DoubleExpression doubleExpression(final ObservableValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof DoubleExpression) {
            return (DoubleExpression) value;
        }
        return new Fn.DoubleFn(new Fn.ToDouble() {
            @Override
            public double get() {
                T current = value.getValue();
                return current == null ? 0 : current.doubleValue();
            }
        }, value);
    }

    /// Returns an object expression holding the boxed value.
    public ObjectExpression<Double> asObject() {
        return new Fn.ObjectFn<Double>(new java.util.function.Supplier<Double>() {
            @Override
            public Double get() {
                return getValue();
            }
        }, this);
    }

    @Override
    public DoubleBinding negate() {
        return (DoubleBinding) Bindings.negate(this);
    }

    @Override
    public DoubleBinding add(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final double other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final float other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final long other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding add(final int other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding subtract(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final double other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final float other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final long other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding subtract(final int other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding multiply(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final double other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final float other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final long other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding multiply(final int other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding divide(final ObservableNumberValue other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final double other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final float other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final long other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public DoubleBinding divide(final int other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }
}
