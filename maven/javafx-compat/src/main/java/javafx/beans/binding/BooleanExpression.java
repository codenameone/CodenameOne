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

import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

/// The fluent operations of an observable boolean.
public abstract class BooleanExpression implements ObservableBooleanValue {

    /// Creates the expression.
    public BooleanExpression() {
    }

    @Override
    public Boolean getValue() {
        return Boolean.valueOf(get());
    }

    /// Returns the value as an expression: the value itself when it already
    /// is one, otherwise a binding that follows it.
    public static BooleanExpression booleanExpression(final ObservableBooleanValue value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof BooleanExpression) {
            return (BooleanExpression) value;
        }
        return new Fn.BooleanFn(() -> value.get(), value);
    }

    /// Returns an expression following a boxed boolean; `null` reads as
    /// `false`.
    public static BooleanExpression booleanExpression(final ObservableValue<Boolean> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof BooleanExpression) {
            return (BooleanExpression) value;
        }
        return new Fn.BooleanFn(() -> {
            Boolean current = value.getValue();
            return current != null && current.booleanValue();
        }, value);
    }

    /// Returns a binding that is true while this value and the other are.
    public BooleanBinding and(final ObservableBooleanValue other) {
        return Bindings.and(this, other);
    }

    /// Returns a binding that is true while this value or the other is.
    public BooleanBinding or(final ObservableBooleanValue other) {
        return Bindings.or(this, other);
    }

    /// Returns a binding holding the opposite of this value.
    public BooleanBinding not() {
        return Bindings.not(this);
    }

    /// Returns a binding telling whether this value equals another.
    public BooleanBinding isEqualTo(final ObservableBooleanValue other) {
        return Bindings.equal(this, other);
    }

    /// Returns a binding telling whether this value differs from another.
    public BooleanBinding isNotEqualTo(final ObservableBooleanValue other) {
        return Bindings.notEqual(this, other);
    }

    /// Returns a binding holding this value as `"true"` or `"false"`.
    public StringBinding asString() {
        return Bindings.text(this);
    }

    /// Returns an object expression holding the boxed value.
    public ObjectExpression<Boolean> asObject() {
        return new Fn.ObjectFn<Boolean>(() -> getValue(), this);
    }
}
