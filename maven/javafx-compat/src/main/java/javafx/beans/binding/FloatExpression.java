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

import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableValue;

/// A [NumberExpression] whose value is a `float`, with the arithmetic results typed
/// the way Java would type them.
public abstract class FloatExpression extends NumberExpressionBase implements ObservableFloatValue {

    /// Creates the expression.
    public FloatExpression() {
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
    public Float getValue() {
        return Float.valueOf(get());
    }

    /// Returns the value as an expression: the value itself when it already
    /// is one, otherwise a binding that follows it.
    public static FloatExpression floatExpression(final ObservableFloatValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof FloatExpression) {
            return (FloatExpression) value;
        }
        return new Fn.FloatFn(new Fn.ToFloat() {
            @Override
            public float get() {
                return value.get();
            }
        }, value);
    }

    /// Returns an expression following a boxed numeric value; a `null` value
    /// reads as zero.
    public static <T extends Number> FloatExpression floatExpression(final ObservableValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof FloatExpression) {
            return (FloatExpression) value;
        }
        return new Fn.FloatFn(new Fn.ToFloat() {
            @Override
            public float get() {
                T current = value.getValue();
                return current == null ? 0 : current.floatValue();
            }
        }, value);
    }

    /// Returns an object expression holding the boxed value.
    public ObjectExpression<Float> asObject() {
        return new Fn.ObjectFn<Float>(new java.util.function.Supplier<Float>() {
            @Override
            public Float get() {
                return getValue();
            }
        }, this);
    }

    @Override
    public FloatBinding negate() {
        return (FloatBinding) Bindings.negate(this);
    }

    @Override
    public DoubleBinding add(final double other) {
        return (DoubleBinding) Bindings.add(this, other);
    }

    @Override
    public FloatBinding add(final float other) {
        return (FloatBinding) Bindings.add(this, other);
    }

    @Override
    public FloatBinding add(final long other) {
        return (FloatBinding) Bindings.add(this, other);
    }

    @Override
    public FloatBinding add(final int other) {
        return (FloatBinding) Bindings.add(this, other);
    }

    @Override
    public DoubleBinding subtract(final double other) {
        return (DoubleBinding) Bindings.subtract(this, other);
    }

    @Override
    public FloatBinding subtract(final float other) {
        return (FloatBinding) Bindings.subtract(this, other);
    }

    @Override
    public FloatBinding subtract(final long other) {
        return (FloatBinding) Bindings.subtract(this, other);
    }

    @Override
    public FloatBinding subtract(final int other) {
        return (FloatBinding) Bindings.subtract(this, other);
    }

    @Override
    public DoubleBinding multiply(final double other) {
        return (DoubleBinding) Bindings.multiply(this, other);
    }

    @Override
    public FloatBinding multiply(final float other) {
        return (FloatBinding) Bindings.multiply(this, other);
    }

    @Override
    public FloatBinding multiply(final long other) {
        return (FloatBinding) Bindings.multiply(this, other);
    }

    @Override
    public FloatBinding multiply(final int other) {
        return (FloatBinding) Bindings.multiply(this, other);
    }

    @Override
    public DoubleBinding divide(final double other) {
        return (DoubleBinding) Bindings.divide(this, other);
    }

    @Override
    public FloatBinding divide(final float other) {
        return (FloatBinding) Bindings.divide(this, other);
    }

    @Override
    public FloatBinding divide(final long other) {
        return (FloatBinding) Bindings.divide(this, other);
    }

    @Override
    public FloatBinding divide(final int other) {
        return (FloatBinding) Bindings.divide(this, other);
    }
}
