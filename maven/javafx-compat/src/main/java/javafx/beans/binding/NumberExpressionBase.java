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
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;

/// Implements every operation of [NumberExpression] on top of [Bindings],
/// leaving only the value itself to the typed subclasses.
public abstract class NumberExpressionBase implements NumberExpression {

    /// Creates the expression.
    public NumberExpressionBase() {
    }

    /// Returns a number as an expression: the value itself when it already
    /// is one, otherwise a binding of the matching type that follows it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is of none of the four
    ///   typed kinds
    public static <S extends Number> NumberExpressionBase numberExpression(final ObservableNumberValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof NumberExpressionBase) {
            return (NumberExpressionBase) value;
        }
        if (value instanceof ObservableIntegerValue) {
            return IntegerExpression.integerExpression((ObservableIntegerValue) value);
        }
        if (value instanceof ObservableDoubleValue) {
            return DoubleExpression.doubleExpression((ObservableDoubleValue) value);
        }
        if (value instanceof ObservableFloatValue) {
            return FloatExpression.floatExpression((ObservableFloatValue) value);
        }
        if (value instanceof ObservableLongValue) {
            return LongExpression.longExpression((ObservableLongValue) value);
        }
        throw new IllegalArgumentException("Unsupported Type");
    }

    @Override
    public NumberBinding negate() {
        return Bindings.negate(this);
    }

    @Override
    public NumberBinding add(final ObservableNumberValue other) {
        return Bindings.add(this, other);
    }

    @Override
    public NumberBinding add(final double other) {
        return Bindings.add(this, other);
    }

    @Override
    public NumberBinding add(final float other) {
        return Bindings.add(this, other);
    }

    @Override
    public NumberBinding add(final long other) {
        return Bindings.add(this, other);
    }

    @Override
    public NumberBinding add(final int other) {
        return Bindings.add(this, other);
    }

    @Override
    public NumberBinding subtract(final ObservableNumberValue other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public NumberBinding subtract(final double other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public NumberBinding subtract(final float other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public NumberBinding subtract(final long other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public NumberBinding subtract(final int other) {
        return Bindings.subtract(this, other);
    }

    @Override
    public NumberBinding multiply(final ObservableNumberValue other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public NumberBinding multiply(final double other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public NumberBinding multiply(final float other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public NumberBinding multiply(final long other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public NumberBinding multiply(final int other) {
        return Bindings.multiply(this, other);
    }

    @Override
    public NumberBinding divide(final ObservableNumberValue other) {
        return Bindings.divide(this, other);
    }

    @Override
    public NumberBinding divide(final double other) {
        return Bindings.divide(this, other);
    }

    @Override
    public NumberBinding divide(final float other) {
        return Bindings.divide(this, other);
    }

    @Override
    public NumberBinding divide(final long other) {
        return Bindings.divide(this, other);
    }

    @Override
    public NumberBinding divide(final int other) {
        return Bindings.divide(this, other);
    }

    @Override
    public BooleanBinding isEqualTo(final ObservableNumberValue other) {
        return Bindings.equal(this, other);
    }

    @Override
    public BooleanBinding isEqualTo(final ObservableNumberValue other, double epsilon) {
        return Bindings.equal(this, other, epsilon);
    }

    @Override
    public BooleanBinding isEqualTo(final double other, double epsilon) {
        return Bindings.equal(this, other, epsilon);
    }

    @Override
    public BooleanBinding isEqualTo(final float other, double epsilon) {
        return Bindings.equal(this, other, epsilon);
    }

    @Override
    public BooleanBinding isEqualTo(final long other) {
        return Bindings.equal(this, other);
    }

    @Override
    public BooleanBinding isEqualTo(final long other, double epsilon) {
        return Bindings.equal(this, other, epsilon);
    }

    @Override
    public BooleanBinding isEqualTo(final int other) {
        return Bindings.equal(this, other);
    }

    @Override
    public BooleanBinding isEqualTo(final int other, double epsilon) {
        return Bindings.equal(this, other, epsilon);
    }

    @Override
    public BooleanBinding isNotEqualTo(final ObservableNumberValue other) {
        return Bindings.notEqual(this, other);
    }

    @Override
    public BooleanBinding isNotEqualTo(final ObservableNumberValue other, double epsilon) {
        return Bindings.notEqual(this, other, epsilon);
    }

    @Override
    public BooleanBinding isNotEqualTo(final double other, double epsilon) {
        return Bindings.notEqual(this, other, epsilon);
    }

    @Override
    public BooleanBinding isNotEqualTo(final float other, double epsilon) {
        return Bindings.notEqual(this, other, epsilon);
    }

    @Override
    public BooleanBinding isNotEqualTo(final long other) {
        return Bindings.notEqual(this, other);
    }

    @Override
    public BooleanBinding isNotEqualTo(final long other, double epsilon) {
        return Bindings.notEqual(this, other, epsilon);
    }

    @Override
    public BooleanBinding isNotEqualTo(final int other) {
        return Bindings.notEqual(this, other);
    }

    @Override
    public BooleanBinding isNotEqualTo(final int other, double epsilon) {
        return Bindings.notEqual(this, other, epsilon);
    }

    @Override
    public BooleanBinding greaterThan(final ObservableNumberValue other) {
        return Bindings.greaterThan(this, other);
    }

    @Override
    public BooleanBinding greaterThan(final double other) {
        return Bindings.greaterThan(this, other);
    }

    @Override
    public BooleanBinding greaterThan(final float other) {
        return Bindings.greaterThan(this, other);
    }

    @Override
    public BooleanBinding greaterThan(final long other) {
        return Bindings.greaterThan(this, other);
    }

    @Override
    public BooleanBinding greaterThan(final int other) {
        return Bindings.greaterThan(this, other);
    }

    @Override
    public BooleanBinding lessThan(final ObservableNumberValue other) {
        return Bindings.lessThan(this, other);
    }

    @Override
    public BooleanBinding lessThan(final double other) {
        return Bindings.lessThan(this, other);
    }

    @Override
    public BooleanBinding lessThan(final float other) {
        return Bindings.lessThan(this, other);
    }

    @Override
    public BooleanBinding lessThan(final long other) {
        return Bindings.lessThan(this, other);
    }

    @Override
    public BooleanBinding lessThan(final int other) {
        return Bindings.lessThan(this, other);
    }

    @Override
    public BooleanBinding greaterThanOrEqualTo(final ObservableNumberValue other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding greaterThanOrEqualTo(final double other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding greaterThanOrEqualTo(final float other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding greaterThanOrEqualTo(final long other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding greaterThanOrEqualTo(final int other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding lessThanOrEqualTo(final ObservableNumberValue other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding lessThanOrEqualTo(final double other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding lessThanOrEqualTo(final float other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding lessThanOrEqualTo(final long other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    @Override
    public BooleanBinding lessThanOrEqualTo(final int other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    @Override
    public StringBinding asString() {
        return Bindings.text(this);
    }

    @Override
    public StringBinding asString(String format) {
        return Bindings.formatted(format, this);
    }
}
