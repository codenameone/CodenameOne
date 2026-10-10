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
package javafx.beans.property;

import com.codename1.fxcompat.runtime.PropertyText;
import com.codename1.fxcompat.runtime.ValueListeners;
import java.lang.ref.WeakReference;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableValue;

/// The full implementation of a `float` property except for its bean and name.
///
/// Invalidation is lazy: after a change, listeners are told once and not
/// again until the value has been read. A change listener reads the value on
/// every invalidation, so registering one makes the property eager.
public abstract class FloatPropertyBase extends FloatProperty {

    private float value;
    private ObservableValue<? extends Number> observable;
    private InvalidationListener listener;
    private boolean valid = true;
    private ValueListeners<Number> helper;

    /// Creates the property with the default value.
    public FloatPropertyBase() {
    }

    /// Creates the property with an initial value.
    public FloatPropertyBase(float initialValue) {
        this.value = initialValue;
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
    public void addListener(ChangeListener<? super Number> listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    /// Tells the listeners the value may have changed.
    protected void fireValueChangedEvent() {
        ValueListeners.fire(helper);
    }

    private void markInvalid() {
        if (valid) {
            valid = false;
            invalidated();
            fireValueChangedEvent();
        }
    }

    /// Called when the property turns invalid, before listeners are told.
    protected void invalidated() {
    }

    @Override
    public float get() {
        valid = true;
        return observable == null ? value : boundValue();
    }

    private float boundValue() {
        if (observable instanceof ObservableFloatValue) {
            return ((ObservableFloatValue) observable).get();
        }
        Number v = observable.getValue();
        return v == null ? 0 : v.floatValue();
    }

    /// Sets the value.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: when the property is bound
    @Override
    public void set(float newValue) {
        if (isBound()) {
            throw new RuntimeException(PropertyText.owner(this) + "A bound value cannot be set.");
        }
        if (value != newValue) {
            value = newValue;
            markInvalid();
        }
    }

    @Override
    public boolean isBound() {
        return observable != null;
    }

    /// Makes this property follow another value until [#unbind()]. The
    /// property observes it weakly, so being bound does not keep it alive.
    @Override
    public void bind(final ObservableValue<? extends Number> newObservable) {
        if (newObservable == null) {
            throw new NullPointerException("Cannot bind to null");
        }
        if (!newObservable.equals(observable)) {
            unbind();
            observable = newObservable;
            if (listener == null) {
                listener = new Listener(this);
            }
            observable.addListener(listener);
            markInvalid();
        }
    }

    /// Stops following the bound value and keeps the value it had.
    @Override
    public void unbind() {
        if (observable != null) {
            value = boundValue();
            observable.removeListener(listener);
            observable = null;
        }
    }

    @Override
    public String toString() {
        String tail;
        if (isBound()) {
            tail = valid ? "bound, value: " + get() : "bound, invalid";
        } else {
            tail = "value: " + get();
        }
        return PropertyText.describe("FloatProperty", this, tail);
    }

    /// Forwards invalidations of the bound value without keeping the
    /// property reachable from it.
    private static final class Listener implements InvalidationListener, WeakListener {
        private final WeakReference ref;

        Listener(FloatPropertyBase property) {
            this.ref = new WeakReference(property);
        }

        @Override
        public void invalidated(Observable source) {
            Object property = ref.get();
            if (property instanceof FloatPropertyBase) {
                ((FloatPropertyBase) property).markInvalid();
            } else {
                source.removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return ref.get() == null;
        }
    }
}
