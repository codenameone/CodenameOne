/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for restorable state values, mirroring Flutter's
 * {@code RestorableProperty<T>} (which extends {@code ChangeNotifier}). User
 * code subclasses this directly to restore custom values, overriding
 * {@link #createDefaultValue()}, {@link #fromPrimitives(Object)},
 * {@link #toPrimitives()} and {@link #initWithValue(Object)} and calling
 * {@link #notifyListeners()}.
 *
 * <p>Codename One does not persist restoration data, so the serialization hooks
 * are inert; what matters at runtime is the {@code ChangeNotifier} behaviour and
 * the value held by the {@link RestorableValue} subtypes.</p>
 *
 * @param <T> the restored value type
 */
public class RestorableProperty<T> {

    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
    private boolean registered;
    private boolean disposed;

    /** The value used when no restoration data is available. */
    public T createDefaultValue() {
        return null;
    }

    /** Adopt {@code value} as the current value (no persistence side effects). Returns the
     *  adopted value — Flutter's {@code initWithValue} returns {@code T}. */
    public void initWithValue(T value) {
    }

    /** Serialize the current value; inert because nothing is persisted. */
    public Object toPrimitives() {
        return null;
    }

    /** Deserialize a previously persisted value; never called (no persistence). */
    public T fromPrimitives(Object data) {
        return createDefaultValue();
    }

    /** Whether this property has been registered with a {@link RestorationMixin}. */
    public boolean isRegistered() {
        return registered;
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        com.codename1.flutter.foundation.Listeners.notify(listeners);
    }

    public void dispose() {
        disposed = true;
        listeners.clear();
    }

    // ------------------------------------------------------------------
    // Framework plumbing (used by RestorationMixin)
    // ------------------------------------------------------------------

    void markRegistered() {
        this.registered = true;
    }

    void markUnregistered() {
        this.registered = false;
    }
}
