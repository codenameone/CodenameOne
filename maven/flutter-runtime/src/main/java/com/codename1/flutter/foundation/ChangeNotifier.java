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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

    /** Identity-keyed listener lists for every ChangeNotifier instance. */
    Map<ChangeNotifier, List<Funcs.VoidFunc0>> LISTENERS =
            new IdentityHashMap<ChangeNotifier, List<Funcs.VoidFunc0>>();

    static List<Funcs.VoidFunc0> listenersOf(ChangeNotifier self) {
        List<Funcs.VoidFunc0> l = LISTENERS.get(self);
        if (l == null) {
            l = new ArrayList<Funcs.VoidFunc0>();
            LISTENERS.put(self, l);
        }
        return l;
    }

    default void addListener(Funcs.VoidFunc0 listener) {
        listenersOf(this).add(listener);
    }

    default void removeListener(Funcs.VoidFunc0 listener) {
        listenersOf(this).remove(listener);
    }

    default void notifyListeners() {
        Listeners.notify(listenersOf(this));
    }

    default void dispose() {
        LISTENERS.remove(this);
    }

    default boolean hasListeners() {
        List<Funcs.VoidFunc0> l = LISTENERS.get(this);
        return l != null && !l.isEmpty();
    }
}
