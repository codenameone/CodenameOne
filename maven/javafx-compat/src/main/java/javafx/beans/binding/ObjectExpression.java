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

import javafx.beans.value.ObservableObjectValue;

/// The fluent operations of an observable object reference.
public abstract class ObjectExpression<T> implements ObservableObjectValue<T> {

    /// Creates the expression.
    public ObjectExpression() {
    }

    @Override
    public T getValue() {
        return get();
    }

    /// Returns the value as an expression: the value itself when it already
    /// is one, otherwise a binding that follows it.
    public static <T> ObjectExpression<T> objectExpression(final ObservableObjectValue<T> value) {
        if (value == null) {
            throw new NullPointerException("Value must be specified.");
        }
        if (value instanceof ObjectExpression) {
            return (ObjectExpression<T>) value;
        }
        return new Fn.ObjectFn<T>(() -> value.get(), value);
    }

    /// Returns a binding telling whether this object equals another.
    public BooleanBinding isEqualTo(final ObservableObjectValue<?> other) {
        return Bindings.equal(this, other);
    }

    /// Returns a binding telling whether this object equals a constant.
    public BooleanBinding isEqualTo(final Object other) {
        return Bindings.equal(this, other);
    }

    /// Returns a binding telling whether this object differs from another.
    public BooleanBinding isNotEqualTo(final ObservableObjectValue<?> other) {
        return Bindings.notEqual(this, other);
    }

    /// Returns a binding telling whether this object differs from a constant.
    public BooleanBinding isNotEqualTo(final Object other) {
        return Bindings.notEqual(this, other);
    }

    /// Returns a binding telling whether the value is `null`.
    public BooleanBinding isNull() {
        return Bindings.isNull(this);
    }

    /// Returns a binding telling whether the value is not `null`.
    public BooleanBinding isNotNull() {
        return Bindings.isNotNull(this);
    }

    /// Returns a binding holding the text of the object; `"null"` for none.
    public StringBinding asString() {
        return Bindings.text(this);
    }

    /// Returns a binding holding the object formatted with a `String.format`
    /// pattern.
    public StringBinding asString(String format) {
        return Bindings.formatted(format, this);
    }
}
