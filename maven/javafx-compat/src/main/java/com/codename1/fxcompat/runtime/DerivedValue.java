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
package com.codename1.fxcompat.runtime;

import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/// The value behind `ObservableValue.map`, `flatMap` and `orElse`: computed
/// from its source when asked, and invalid whenever the source (or, for
/// `flatMap`, the value the source maps to) is.
public final class DerivedValue<S, U> implements ObservableValue<U> {

    private static final int MAP = 0;
    private static final int FLAT = 1;
    private static final int OR_ELSE = 2;

    private final ObservableValue<S> source;
    private final Function<? super S, ?> function;
    private final U other;
    private final int kind;
    private ValueListeners<U> helper;
    private ObservableValue<?> inner;
    private final InvalidationListener relay = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            sourceChanged();
        }
    };

    private DerivedValue(ObservableValue<S> source, Function<? super S, ?> function, U other, int kind) {
        if (source == null || (kind != OR_ELSE && function == null)) {
            throw new NullPointerException();
        }
        this.source = source;
        this.function = function;
        this.other = other;
        this.kind = kind;
        source.addListener(relay);
        if (kind == FLAT) {
            follow();
        }
    }

    /// The value `function` maps the source's to, null for null.
    public static <S, U> ObservableValue<U> map(ObservableValue<S> source, Function<? super S, ? extends U> function) {
        return new DerivedValue<S, U>(source, function, null, MAP);
    }

    /// The value of the observable value `function` maps the source's to.
    public static <S, U> ObservableValue<U> flatMap(ObservableValue<S> source,
            Function<? super S, ? extends ObservableValue<? extends U>> function) {
        return new DerivedValue<S, U>(source, function, null, FLAT);
    }

    /// The source's value, or `other` while it is null.
    public static <S> ObservableValue<S> orElse(ObservableValue<S> source, S other) {
        return new DerivedValue<S, S>(source, null, other, OR_ELSE);
    }

    private void follow() {
        ObservableValue<?> next = null;
        S v = source.getValue();
        if (v != null) {
            Object o = function.apply(v);
            if (o instanceof ObservableValue) {
                next = (ObservableValue<?>) o;
            }
        }
        if (next != inner) {
            if (inner != null) {
                inner.removeListener(relay);
            }
            inner = next;
            if (inner != null) {
                inner.addListener(relay);
            }
        }
    }

    private void sourceChanged() {
        if (kind == FLAT) {
            follow();
        }
        ValueListeners.fire(helper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public U getValue() {
        S v = source.getValue();
        switch (kind) {
            case OR_ELSE:
                return v == null ? other : (U) v;
            case FLAT:
                follow();
                return inner == null ? null : (U) inner.getValue();
            default:
                return v == null ? null : (U) function.apply(v);
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super U> listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super U> listener) {
        helper = ValueListeners.remove(helper, listener);
    }
}
