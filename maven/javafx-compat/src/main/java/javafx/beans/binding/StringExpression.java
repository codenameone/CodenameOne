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

import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;

/// The fluent operations of an observable string. Comparisons treat `null`
/// as the empty string.
public abstract class StringExpression implements ObservableStringValue {

    /// Creates the expression.
    public StringExpression() {
    }

    @Override
    public String getValue() {
        return get();
    }

    /// Returns the value, or the empty string when it is `null`.
    public final String getValueSafe() {
        final String value = get();
        return value == null ? "" : value;
    }

    /// Returns any observable value as a string expression: the value itself
    /// when it already is one, otherwise a binding holding its text.
    public static StringExpression stringExpression(final ObservableValue<?> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        return Bindings.convert(value);
    }

    /// Returns an expression holding this string followed by the text of
    /// another object, which is observed when it is an observable value.
    public StringExpression concat(Object other) {
        return Bindings.concat(this, other);
    }

    /// Returns a binding telling whether this string equals another.
    public BooleanBinding isEqualTo(final ObservableStringValue other) {
        return Bindings.equal(this, other);
    }

    /// Returns a binding telling whether this string equals a constant.
    public BooleanBinding isEqualTo(final String other) {
        return Bindings.equal(this, other);
    }

    /// Returns a binding telling whether this string differs from another.
    public BooleanBinding isNotEqualTo(final ObservableStringValue other) {
        return Bindings.notEqual(this, other);
    }

    /// Returns a binding telling whether this string differs from a constant.
    public BooleanBinding isNotEqualTo(final String other) {
        return Bindings.notEqual(this, other);
    }

    /// Returns a binding telling whether this string equals another,
    /// ignoring case.
    public BooleanBinding isEqualToIgnoreCase(final ObservableStringValue other) {
        return Bindings.equalIgnoreCase(this, other);
    }

    /// Returns a binding telling whether this string equals a constant,
    /// ignoring case.
    public BooleanBinding isEqualToIgnoreCase(final String other) {
        return Bindings.equalIgnoreCase(this, other);
    }

    /// Returns a binding telling whether this string differs from another
    /// even ignoring case.
    public BooleanBinding isNotEqualToIgnoreCase(final ObservableStringValue other) {
        return Bindings.notEqualIgnoreCase(this, other);
    }

    /// Returns a binding telling whether this string differs from a constant
    /// even ignoring case.
    public BooleanBinding isNotEqualToIgnoreCase(final String other) {
        return Bindings.notEqualIgnoreCase(this, other);
    }

    /// Returns a binding telling whether this string sorts after another.
    public BooleanBinding greaterThan(final ObservableStringValue other) {
        return Bindings.greaterThan(this, other);
    }

    /// Returns a binding telling whether this string sorts after a constant.
    public BooleanBinding greaterThan(final String other) {
        return Bindings.greaterThan(this, other);
    }

    /// Returns a binding telling whether this string sorts before another.
    public BooleanBinding lessThan(final ObservableStringValue other) {
        return Bindings.lessThan(this, other);
    }

    /// Returns a binding telling whether this string sorts before a constant.
    public BooleanBinding lessThan(final String other) {
        return Bindings.lessThan(this, other);
    }

    /// Returns a binding telling whether this string does not sort before
    /// another.
    public BooleanBinding greaterThanOrEqualTo(final ObservableStringValue other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    /// Returns a binding telling whether this string does not sort before a
    /// constant.
    public BooleanBinding greaterThanOrEqualTo(final String other) {
        return Bindings.greaterThanOrEqual(this, other);
    }

    /// Returns a binding telling whether this string does not sort after
    /// another.
    public BooleanBinding lessThanOrEqualTo(final ObservableStringValue other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    /// Returns a binding telling whether this string does not sort after a
    /// constant.
    public BooleanBinding lessThanOrEqualTo(final String other) {
        return Bindings.lessThanOrEqual(this, other);
    }

    /// Returns a binding telling whether the value is `null`.
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /// Returns a binding telling whether the value is not `null`.
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /// Returns a binding holding the length of the string; zero for `null`.
    public IntegerBinding length() {
        return Bindings.length(this);
    }

    /// Returns a binding telling whether the string is `null` or empty.
    public BooleanBinding isEmpty() {
        return Bindings.isEmpty(this);
    }

    /// Returns a binding telling whether the string has any character.
    public BooleanBinding isNotEmpty() {
        return Bindings.isNotEmpty(this);
    }
}
