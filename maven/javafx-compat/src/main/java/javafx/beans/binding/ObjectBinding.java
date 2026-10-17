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

import com.codename1.fxcompat.runtime.BindingObserver;
import com.codename1.fxcompat.runtime.ValueListeners;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Base class for a lazily evaluated binding that computes an object.
///
/// A subclass registers what it depends on with [#bind(Observable...)] and
/// implements [#computeValue()]. The value is computed on the first read after
/// an invalidation and cached until a dependency changes again; listeners are
/// told about an invalidation once, until the value is read.
public abstract class ObjectBinding<T> extends ObjectExpression<T> implements Binding<T> {

    private T value;
    private boolean valid;
    private BindingObserver observer;
    private ValueListeners<T> helper;

    /// Creates an invalid binding with no dependencies.
    public ObjectBinding() {
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
    public void addListener(ChangeListener<? super T> listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    /// Starts observing the dependencies; each one invalidates this binding.
    protected final void bind(Observable... dependencies) {
        if (dependencies != null && dependencies.length > 0) {
            if (observer == null) {
                observer = new BindingObserver(this);
            }
            for (Observable dependency : dependencies) {
                dependency.addListener(observer);
            }
        }
    }

    /// Stops observing the dependencies.
    protected final void unbind(Observable... dependencies) {
        if (observer != null && dependencies != null) {
            for (Observable dependency : dependencies) {
                dependency.removeListener(observer);
            }
        }
    }

    /// Releases the dependencies. The default implementation does nothing.
    @Override
    public void dispose() {
    }

    /// Returns the dependencies; the default implementation reports none.
    @Override
    public ObservableList<?> getDependencies() {
        return FXCollections.emptyObservableList();
    }

    /// Returns the value, computing it when the binding is invalid.
    @Override
    public final T get() {
        if (!valid) {
            value = computeValue();
            valid = true;
        }
        return value;
    }

    /// Called when the binding turns invalid, before listeners are told.
    protected void onInvalidating() {
    }

    @Override
    public final void invalidate() {
        if (valid) {
            valid = false;
            onInvalidating();
            ValueListeners.fire(helper);
        }
    }

    @Override
    public final boolean isValid() {
        return valid;
    }

    /// Computes the current value from the dependencies.
    protected abstract T computeValue();

    @Override
    public String toString() {
        return valid ? "ObjectBinding [value: " + get() + "]" : "ObjectBinding [invalid]";
    }
}
