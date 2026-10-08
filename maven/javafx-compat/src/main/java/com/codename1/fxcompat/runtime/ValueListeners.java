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

import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/// The listeners of an observable value, and the last value its change
/// listeners were told about.
///
/// The first change listener makes the owner eager: its value is read when
/// the listener is added and again on every invalidation, because that is
/// the only way to know the old and the new value.
public final class ValueListeners<T> {

    private final ObservableValue<T> observable;
    private ListenerSet<ChangeListener<? super T>> set;
    private T currentValue;

    private ValueListeners(ObservableValue<T> observable) {
        this.observable = observable;
    }

    /// Adds an invalidation listener.
    public static <T> ValueListeners<T> add(ValueListeners<T> helper, ObservableValue<T> observable,
            InvalidationListener listener) {
        if (observable == null || listener == null) {
            throw new NullPointerException();
        }
        ValueListeners<T> result = helper == null ? new ValueListeners<T>(observable) : helper;
        result.set = ListenerSet.addInvalidation(result.set, listener);
        return result;
    }

    /// Removes an invalidation listener; returns `null` once nothing is left.
    public static <T> ValueListeners<T> remove(ValueListeners<T> helper, InvalidationListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (helper == null) {
            return null;
        }
        helper.set = ListenerSet.removeInvalidation(helper.set, listener);
        return helper.set == null ? null : helper;
    }

    /// Adds a change listener, reading the value if it is the first one.
    public static <T> ValueListeners<T> add(ValueListeners<T> helper, ObservableValue<T> observable,
            ChangeListener<? super T> listener) {
        if (observable == null || listener == null) {
            throw new NullPointerException();
        }
        ValueListeners<T> result = helper == null ? new ValueListeners<T>(observable) : helper;
        boolean first = ListenerSet.changeListeners(result.set).isEmpty();
        result.set = ListenerSet.addChange(result.set, listener);
        if (first) {
            result.currentValue = observable.getValue();
        }
        return result;
    }

    /// Removes a change listener; returns `null` once nothing is left.
    public static <T> ValueListeners<T> remove(ValueListeners<T> helper, ChangeListener<? super T> listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (helper == null) {
            return null;
        }
        helper.set = ListenerSet.removeChange(helper.set, listener);
        if (ListenerSet.changeListeners(helper.set).isEmpty()) {
            helper.currentValue = null;
        }
        return helper.set == null ? null : helper;
    }

    /// Tells the invalidation listeners the value is stale and, when the
    /// value really differs from the last one reported, the change listeners.
    public static <T> void fire(ValueListeners<T> helper) {
        if (helper == null) {
            return;
        }
        ListenerSet<ChangeListener<? super T>> set = helper.set;
        ListenerSet.fireInvalidation(set, helper.observable);
        List<ChangeListener<? super T>> listeners = ListenerSet.changeListeners(set);
        if (!listeners.isEmpty()) {
            T oldValue = helper.currentValue;
            T newValue = helper.observable.getValue();
            helper.currentValue = newValue;
            boolean changed = newValue == null ? oldValue != null : !newValue.equals(oldValue);
            if (changed) {
                for (int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).changed(helper.observable, oldValue, newValue);
                }
            }
        }
    }
}
