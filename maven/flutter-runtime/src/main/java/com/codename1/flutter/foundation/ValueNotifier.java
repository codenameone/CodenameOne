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
package com.codename1.flutter.foundation;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code ChangeNotifier} that holds a single value ({@code ValueNotifier<T>}
 * in Flutter); assigning {@link #value(Object)} notifies listeners when the value
 * actually changes. new_gallery drives {@code ValueListenableBuilder<bool>} from
 * these (the settings sheet's open flag, the extended nav-rail flag).
 *
 * <p>Transpiler surface: the Dart {@code value} getter maps to {@link #value()},
 * {@code notifier.value = v} to {@link #value(Object)}.</p>
 *
 * @param <T> the value type
 */
public class ValueNotifier<T> extends ValueListenable<T> {

    private T current;
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();

    public ValueNotifier(T value) {
        this.current = value;
    }

    @Override
    public T value() {
        return current;
    }

    public void value(T newValue) {
        // Dart's ==, not Java equals: 1 and 1.0 are the same value and NaN is never
        // equal to itself, so a num notifier notified for 1 -> 1.0 and stayed quiet
        // for NaN -> NaN.
        boolean changed = !dart.runtime.DartRuntime.eq(current, newValue);
        if (changed) {
            current = newValue;
            notifyListeners();
        }
    }

    @Override
    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        Listeners.notify(listeners);
    }

    public void dispose() {
        listeners.clear();
    }
}
