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

/**
 * An object exposing a value that changes over time and can be listened to
 * ({@code ValueListenable<T>} in Flutter). {@code ValueListenableBuilder}
 * rebuilds whenever the value changes. Implemented by {@link ValueNotifier}.
 *
 * <p>It IS a {@link Listenable}, as in Flutter, so a notifier can drive anything that takes
 * one — {@code AnimatedWidget(listenable: notifier)}, {@code AnimatedBuilder(animation:
 * notifier)}. Leaving the two hierarchies unrelated made those a compile error in transpiled
 * code for no reason the Dart could explain.</p>
 *
 * @param <T> the value type
 */
public abstract class ValueListenable<T> implements Listenable {

    public abstract T value();

    public abstract void addListener(Funcs.VoidFunc0 listener);

    public abstract void removeListener(Funcs.VoidFunc0 listener);
}
