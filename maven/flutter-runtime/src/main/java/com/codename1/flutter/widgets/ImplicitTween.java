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

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Offset;
import com.codename1.flutter.State;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.animation.Curve;
import com.codename1.flutter.animation.Tween;
import dart.core.Duration;

/**
 * An implicitly animated value of any type: the object counterpart of {@link ImplicitValue}.
 * A progress value runs 0..1 whenever the target changes, and {@link #value()} interpolates
 * from wherever the value was when the target moved to the new target. Numbers, colors,
 * insets, alignments, offsets and text styles interpolate; anything else, or a pair of
 * different kinds, switches to the target at the end, as Flutter's Tween does for a type
 * it cannot lerp. A {@link Tween} given to {@link #lerpWith} does the interpolation instead.
 */
public final class ImplicitTween {
    private final ImplicitValue progress;
    private Object from;
    private Object to;
    private int generation;
    private Tween<Object> tween;

    public ImplicitTween(Object initial, State<?> state) {
        progress = new ImplicitValue(0, state);
        from = initial;
        to = initial;
    }

    /** Makes {@code t} interpolate the values (TweenAnimationBuilder's own tween). */
    @SuppressWarnings("unchecked")
    public void lerpWith(Tween<?> t) {
        this.tween = (Tween<Object>) t;
    }

    public void onEnd(dart.runtime.Funcs.VoidFunc0 callback) {
        progress.onEnd(callback);
    }

    /** Starts a run to {@code target} unless that is already where the value is headed. */
    public void retarget(Object target, Duration duration, Curve curve) {
        if (same(target, to)) {
            return;
        }
        from = value();
        to = target;
        generation++;
        progress.retarget(generation, duration, curve);
    }

    public Object value() {
        double p = progress.value() - (generation - 1);
        if (generation == 0 || p >= 1) {
            return to;
        }
        if (p <= 0) {
            return from;
        }
        if (tween != null) {
            Object b = tween.begin();
            Object e = tween.end();
            tween.begin(from);
            tween.end(to);
            try {
                return tween.lerp(p);
            } finally {
                tween.begin(b);
                tween.end(e);
            }
        }
        return lerp(from, to, p);
    }

    public boolean isAnimating() {
        return progress.isAnimating();
    }

    public void dispose() {
        progress.dispose();
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static double num(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Interpolates two values of one kind; a null end stands for that kind's zero. */
    public static Object lerp(Object a, Object b, double t) {
        if (a instanceof Number && b instanceof Number) {
            return Double.valueOf(num(((Number) a).doubleValue(), ((Number) b).doubleValue(), t));
        }
        if (a instanceof Color && b instanceof Color) {
            Color x = (Color) a;
            Color y = (Color) b;
            long al = Math.round(num(x.alpha(), y.alpha(), t));
            long r = Math.round(num(x.red(), y.red(), t));
            long g = Math.round(num(x.green(), y.green(), t));
            long bl = Math.round(num(x.blue(), y.blue(), t));
            return new Color((al << 24) | (r << 16) | (g << 8) | bl);
        }
        if (a instanceof EdgeInsets && b instanceof EdgeInsets) {
            return EdgeInsets.lerp((EdgeInsets) a, (EdgeInsets) b, t);
        }
        if (a instanceof Alignment && b instanceof Alignment) {
            Alignment x = (Alignment) a;
            Alignment y = (Alignment) b;
            return new Alignment(num(x.x(), y.x(), t), num(x.y(), y.y(), t));
        }
        if (a instanceof Offset && b instanceof Offset) {
            Offset x = (Offset) a;
            Offset y = (Offset) b;
            return new Offset(num(x.dx(), y.dx(), t), num(x.dy(), y.dy(), t));
        }
        if (a instanceof TextStyle && b instanceof TextStyle) {
            TextStyle x = (TextStyle) a;
            TextStyle y = (TextStyle) b;
            TextStyle r = (t < 0.5 ? x : y).merge(new TextStyle());
            if (x.fontSize() != null && y.fontSize() != null) {
                r.fontSize(num(x.fontSize().doubleValue(), y.fontSize().doubleValue(), t));
            }
            if (x.color() != null && y.color() != null) {
                r.color((Color) lerp(x.color(), y.color(), t));
            }
            return r;
        }
        if (a instanceof com.codename1.flutter.BoxDecoration && b instanceof com.codename1.flutter.BoxDecoration) {
            com.codename1.flutter.BoxDecoration x = (com.codename1.flutter.BoxDecoration) a;
            com.codename1.flutter.BoxDecoration y = (com.codename1.flutter.BoxDecoration) b;
            com.codename1.flutter.BoxDecoration base = t < 0.5 ? x : y;
            Object c = x.getColor() != null && y.getColor() != null ? lerp(x.getColor(), y.getColor(), t)
                    : base.getColor();
            Object r = base.getBorderRadius();
            if (x.getBorderRadius() instanceof com.codename1.flutter.BorderRadius
                    && y.getBorderRadius() instanceof com.codename1.flutter.BorderRadius) {
                r = com.codename1.flutter.BorderRadius.lerp((com.codename1.flutter.BorderRadius) x.getBorderRadius(),
                        (com.codename1.flutter.BorderRadius) y.getBorderRadius(), t);
            }
            return base.withColorAndRadius((Color) c, r);
        }
        return t < 1 ? a : b;
    }
}
