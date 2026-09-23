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

import java.util.List;

import dart.runtime.Funcs;

/**
 * Flutter's ChangeNotifier. Applied in Dart as a mixin
 * ({@code class EmailStore with ChangeNotifier}); the transpiler maps a stub
 * mixin to an implemented Java interface, so the notifier state (the listener
 * list) lives in an identity-keyed side table rather than in an instance field.
 * Listeners are {@code VoidCallback}s ({@link Funcs.VoidFunc0}).
 *
 * <p>A ChangeNotifier IS a {@link Listenable}, as in Flutter — so a model mixing it in can
 * drive an {@code AnimatedBuilder} or an {@code AnimatedWidget} directly.</p>
 */
public interface ChangeNotifier extends Listenable {

    /** Listener lists, one per notifier that currently has listeners. */
    NotifierListeners LISTENERS = new NotifierListeners();

    default void addListener(Funcs.VoidFunc0 listener) {
        LISTENERS.forAdding(this).add(listener);
    }

    default void removeListener(Funcs.VoidFunc0 listener) {
        LISTENERS.remove(this, listener);
    }

    default void notifyListeners() {
        List<Funcs.VoidFunc0> l = LISTENERS.get(this);
        if (l != null) {
            Listeners.notify(l);
        }
    }

    default void dispose() {
        LISTENERS.clear(this);
    }

    default boolean hasListeners() {
        List<Funcs.VoidFunc0> l = LISTENERS.get(this);
        return l != null && !l.isEmpty();
    }
}
