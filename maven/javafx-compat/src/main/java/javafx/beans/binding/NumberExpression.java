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

import javafx.beans.value.ObservableNumberValue;

/// The fluent operations of an observable number: arithmetic, comparison and
/// conversion to text, each returning a binding that follows this number.
///
/// The result of an arithmetic operation has the wider of the two operand
/// types, as the same expression would in Java.
public interface NumberExpression extends ObservableNumberValue {

    /// Returns a binding computing the negation of this number.
    NumberBinding negate();

    /// Returns a binding computing the sum of this number and another.
    NumberBinding add(final ObservableNumberValue other);

    /// Returns a binding computing the sum of this number and a constant.
    NumberBinding add(final double other);

    /// Returns a binding computing the sum of this number and a constant.
    NumberBinding add(final float other);

    /// Returns a binding computing the sum of this number and a constant.
    NumberBinding add(final long other);

    /// Returns a binding computing the sum of this number and a constant.
    NumberBinding add(final int other);

    /// Returns a binding computing the difference of this number and another.
    NumberBinding subtract(final ObservableNumberValue other);

    /// Returns a binding computing the difference of this number and a constant.
    NumberBinding subtract(final double other);

    /// Returns a binding computing the difference of this number and a constant.
    NumberBinding subtract(final float other);

    /// Returns a binding computing the difference of this number and a constant.
    NumberBinding subtract(final long other);

    /// Returns a binding computing the difference of this number and a constant.
    NumberBinding subtract(final int other);

    /// Returns a binding computing the product of this number and another.
    NumberBinding multiply(final ObservableNumberValue other);

    /// Returns a binding computing the product of this number and a constant.
    NumberBinding multiply(final double other);

    /// Returns a binding computing the product of this number and a constant.
    NumberBinding multiply(final float other);

    /// Returns a binding computing the product of this number and a constant.
    NumberBinding multiply(final long other);

    /// Returns a binding computing the product of this number and a constant.
    NumberBinding multiply(final int other);

    /// Returns a binding computing the quotient of this number and another.
    NumberBinding divide(final ObservableNumberValue other);

    /// Returns a binding computing the quotient of this number and a constant.
    NumberBinding divide(final double other);

    /// Returns a binding computing the quotient of this number and a constant.
    NumberBinding divide(final float other);

    /// Returns a binding computing the quotient of this number and a constant.
    NumberBinding divide(final long other);

    /// Returns a binding computing the quotient of this number and a constant.
    NumberBinding divide(final int other);

    /// Returns a binding telling whether this number and another are equal.
    BooleanBinding isEqualTo(final ObservableNumberValue other);

    /// Returns a binding telling whether this number and another are equal, within a tolerance.
    BooleanBinding isEqualTo(final ObservableNumberValue other, double epsilon);

    /// Returns a binding telling whether this number and a constant are equal, within a tolerance.
    BooleanBinding isEqualTo(final double other, double epsilon);

    /// Returns a binding telling whether this number and a constant are equal, within a tolerance.
    BooleanBinding isEqualTo(final float other, double epsilon);

    /// Returns a binding telling whether this number and a constant are equal.
    BooleanBinding isEqualTo(final long other);

    /// Returns a binding telling whether this number and a constant are equal, within a tolerance.
    BooleanBinding isEqualTo(final long other, double epsilon);

    /// Returns a binding telling whether this number and a constant are equal.
    BooleanBinding isEqualTo(final int other);

    /// Returns a binding telling whether this number and a constant are equal, within a tolerance.
    BooleanBinding isEqualTo(final int other, double epsilon);

    /// Returns a binding telling whether this number and another are not equal.
    BooleanBinding isNotEqualTo(final ObservableNumberValue other);

    /// Returns a binding telling whether this number and another are not equal, within a tolerance.
    BooleanBinding isNotEqualTo(final ObservableNumberValue other, double epsilon);

    /// Returns a binding telling whether this number and a constant are not equal, within a tolerance.
    BooleanBinding isNotEqualTo(final double other, double epsilon);

    /// Returns a binding telling whether this number and a constant are not equal, within a tolerance.
    BooleanBinding isNotEqualTo(final float other, double epsilon);

    /// Returns a binding telling whether this number and a constant are not equal.
    BooleanBinding isNotEqualTo(final long other);

    /// Returns a binding telling whether this number and a constant are not equal, within a tolerance.
    BooleanBinding isNotEqualTo(final long other, double epsilon);

    /// Returns a binding telling whether this number and a constant are not equal.
    BooleanBinding isNotEqualTo(final int other);

    /// Returns a binding telling whether this number and a constant are not equal, within a tolerance.
    BooleanBinding isNotEqualTo(final int other, double epsilon);

    /// Returns a binding telling whether this number is greater than another.
    BooleanBinding greaterThan(final ObservableNumberValue other);

    /// Returns a binding telling whether this number is greater than a constant.
    BooleanBinding greaterThan(final double other);

    /// Returns a binding telling whether this number is greater than a constant.
    BooleanBinding greaterThan(final float other);

    /// Returns a binding telling whether this number is greater than a constant.
    BooleanBinding greaterThan(final long other);

    /// Returns a binding telling whether this number is greater than a constant.
    BooleanBinding greaterThan(final int other);

    /// Returns a binding telling whether this number is less than another.
    BooleanBinding lessThan(final ObservableNumberValue other);

    /// Returns a binding telling whether this number is less than a constant.
    BooleanBinding lessThan(final double other);

    /// Returns a binding telling whether this number is less than a constant.
    BooleanBinding lessThan(final float other);

    /// Returns a binding telling whether this number is less than a constant.
    BooleanBinding lessThan(final long other);

    /// Returns a binding telling whether this number is less than a constant.
    BooleanBinding lessThan(final int other);

    /// Returns a binding telling whether this number is greater than or equal to another.
    BooleanBinding greaterThanOrEqualTo(final ObservableNumberValue other);

    /// Returns a binding telling whether this number is greater than or equal to a constant.
    BooleanBinding greaterThanOrEqualTo(final double other);

    /// Returns a binding telling whether this number is greater than or equal to a constant.
    BooleanBinding greaterThanOrEqualTo(final float other);

    /// Returns a binding telling whether this number is greater than or equal to a constant.
    BooleanBinding greaterThanOrEqualTo(final long other);

    /// Returns a binding telling whether this number is greater than or equal to a constant.
    BooleanBinding greaterThanOrEqualTo(final int other);

    /// Returns a binding telling whether this number is less than or equal to another.
    BooleanBinding lessThanOrEqualTo(final ObservableNumberValue other);

    /// Returns a binding telling whether this number is less than or equal to a constant.
    BooleanBinding lessThanOrEqualTo(final double other);

    /// Returns a binding telling whether this number is less than or equal to a constant.
    BooleanBinding lessThanOrEqualTo(final float other);

    /// Returns a binding telling whether this number is less than or equal to a constant.
    BooleanBinding lessThanOrEqualTo(final long other);

    /// Returns a binding telling whether this number is less than or equal to a constant.
    BooleanBinding lessThanOrEqualTo(final int other);

    /// Returns a binding holding this number as text.
    StringBinding asString();

    /// Returns a binding holding this number formatted with a `String.format` pattern.
    StringBinding asString(String format);
}
