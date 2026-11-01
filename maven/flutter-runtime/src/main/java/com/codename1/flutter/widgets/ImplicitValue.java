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
package com.codename1.flutter.widgets;

import com.codename1.flutter.State;
import com.codename1.flutter.animation.AnimationController;
import com.codename1.flutter.animation.AnimationStatus;
import com.codename1.flutter.animation.Curve;
import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * One animated double behind an implicitly animated widget: when the target changes the
 * value runs from wherever it is now to the new target over the widget's duration and
 * curve, rebuilding its state on every frame, as Flutter's ImplicitlyAnimatedWidget does.
 */
public final class ImplicitValue {
    private static final Funcs.VoidFunc0 NOOP = new Funcs.VoidFunc0() {
        @Override
        public void call() {
        }
    };
    private final AnimationController controller = new AnimationController();
    private double from;
    private double to;
    private Curve curve;
    private Funcs.VoidFunc0 onEnd;

    public ImplicitValue(double initial, final State<?> state) {
        from = initial;
        to = initial;
        controller.value(1.0);
        controller.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (state.mounted()) {
                    state.setState(NOOP);
                }
            }
        });
        controller.addStatusListener(new Funcs.VoidFunc1<AnimationStatus>() {
            @Override
            public void call(AnimationStatus s) {
                if (s == AnimationStatus.completed && onEnd != null) {
                    onEnd.call();
                }
            }
        });
    }

    public void onEnd(Object callback) {
        this.onEnd = callback instanceof Funcs.VoidFunc0 ? (Funcs.VoidFunc0) callback : null;
    }

    /** Starts a run to {@code target} unless that is already where the value is headed. */
    public void retarget(double target, Duration duration, Curve c) {
        if (target == to) {
            return;
        }
        from = value();
        to = target;
        curve = c;
        if (duration == null || duration.inMilliseconds() <= 0) {
            from = to;
            controller.value(1.0);
            return;
        }
        controller.duration(duration);
        controller.forward(Double.valueOf(0.0));
    }

    public double value() {
        Double v = controller.value();
        double t = v == null ? 1.0 : v.doubleValue();
        if (curve != null && t > 0 && t < 1) {
            t = curve.transform(t);
        }
        return from + (to - from) * t;
    }

    public boolean isAnimating() {
        return controller.status() == AnimationStatus.forward;
    }

    public void dispose() {
        controller.dispose();
    }
}
