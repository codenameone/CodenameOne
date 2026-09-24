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
package com.codename1.flutter.material;

import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AnimationController;
import com.codename1.flutter.animation.TickerProvider;
import com.codename1.flutter.foundation.ChangeNotifier;

import dart.core.Duration;

/**
 * Coordinates tab selection between a {@link TabBar} and a {@link TabBarView} —
 * Flutter's {@code TabController}. Holds the selected {@code index} over a fixed
 * {@code length}, notifies listeners on change (it is a {@link ChangeNotifier}),
 * and exposes an {@link Animation} whose value tracks the selected index
 * (0..length-1) so index-driven animations resolve. Tab-change gestures and the
 * cross-fade flight are not yet wired; {@link #animateTo} sets the index
 * immediately.
 */
public class TabController implements ChangeNotifier {

    /** Held here, not in the shared map, so this goes when the object does (see ChangeNotifier). */
    private final java.util.List<dart.runtime.Funcs.VoidFunc0> changeNotifierListeners =
            new java.util.ArrayList<dart.runtime.Funcs.VoidFunc0>();

    @Override
    public java.util.List<dart.runtime.Funcs.VoidFunc0> changeNotifierListeners$() {
        return changeNotifierListeners;
    }


    private long length;
    private long index;
    private long previousIndex;
    private final AnimationController controller = new AnimationController();

    public TabController() {
        controller.lowerBound(0.0);
        controller.upperBound(Double.MAX_VALUE);
    }

    // Named-parameter setters.

    public void length(long v) {
        this.length = v;
        controller.upperBound(v <= 1 ? 1.0 : (double) (v - 1));
    }

    public void initialIndex(long v) {
        this.index = v;
        this.previousIndex = v;
        controller.value((double) v);
    }

    public void animationDuration(Duration v) {
        if (v != null) {
            controller.duration(v);
        }
    }

    public void vsync(TickerProvider v) {
        // self-driven; provider unused
    }

    // Dart getters / setters.

    public long index() {
        return index;
    }

    public void index(long v) {
        if (v == index) {
            return;
        }
        previousIndex = index;
        index = v;
        controller.value((double) v);
        notifyListeners();
    }

    public long length() {
        return length;
    }

    public long previousIndex() {
        return previousIndex;
    }

    public boolean indexIsChanging() {
        return false;
    }

    public double offset() {
        return 0.0;
    }

    public Animation<Double> animation() {
        return controller;
    }

    public void animateTo(long value, Duration duration, com.codename1.flutter.animation.Curve curve) {
        index(value);
    }
}
