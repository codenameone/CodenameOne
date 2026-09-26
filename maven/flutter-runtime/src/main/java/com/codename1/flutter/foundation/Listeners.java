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
import java.util.Collection;

/**
 * Listener dispatch shared by every notifier in the runtime -- Animation's
 * value and status listeners, ChangeNotifier, ValueNotifier and the
 * controllers.
 *
 * <p>Each call is isolated, as Flutter's own notifyListeners does: a listener
 * that throws is reported and the rest still run. Dispatching in a bare loop
 * let one faulty listener starve every listener after it; on an animation it
 * was worse, because the throw reached the frame clock, which drops the whole
 * controller -- a failing analytics listener froze the visual animation.</p>
 *
 * <p>The list is copied first, so a listener may add or remove listeners
 * during dispatch -- one added is not called until the next notification, as in
 * Flutter. One REMOVED is not called either: each is checked against the live
 * collection before it runs. Calling the copy blindly ran a listener after an
 * earlier one had disposed its owner, and its state or controller code ran on
 * a disposed object.</p>
 */
public final class Listeners {

    private Listeners() {
    }

    /** Calls every listener in {@code listeners}, reporting any that throws. */
    public static void notify(Collection<Funcs.VoidFunc0> listeners) {
        for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
            if (!listeners.contains(l)) {
                continue;
            }
            try {
                l.call();
            } catch (Throwable t) {
                com.codename1.flutter.FlutterErrorReport.record(t);
            }
        }
    }

    /** Calls every listener in {@code listeners} with {@code value}, reporting any that throws. */
    public static <T> void notify(Collection<Funcs.VoidFunc1<T>> listeners, T value) {
        for (Funcs.VoidFunc1<T> l : new ArrayList<Funcs.VoidFunc1<T>>(listeners)) {
            if (!listeners.contains(l)) {
                continue;
            }
            try {
                l.call(value);
            } catch (Throwable t) {
                com.codename1.flutter.FlutterErrorReport.record(t);
            }
        }
    }
}
