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
package com.codename1.flutter.animation;

/**
 * Linearly interpolates between a {@code begin} and {@code end} value —
 * Flutter's {@code Tween<T>}. The base class handles the numeric case
 * ({@code begin + (end - begin) * t}) for {@link Number} values; typed
 * subclasses ({@link ColorTween}, {@link IntTween}) override {@link #lerp} for
 * their own interpolation. For opaque value types (border radius, matrices)
 * where no interpolation is wired this pass, {@link #lerp} steps at the
 * midpoint — a correct-shape, minimal fallback.
 */
public class Tween<T> extends Animatable<T> {

    private T beginValue;
    private T endValue;

    public Tween() {
    }

    /** Named-parameter setter for the Dart {@code begin:} argument. */
    public void begin(T v) {
        this.beginValue = v;
    }

    /** Named-parameter setter for the Dart {@code end:} argument. */
    public void end(T v) {
        this.endValue = v;
    }

    /** Dart getter {@code tween.begin}. */
    public T begin() {
        return beginValue;
    }

    /** Dart getter {@code tween.end}. */
    public T end() {
        return endValue;
    }

    /** Interpolates at {@code t} (0..1). Override for typed interpolation. */
    @SuppressWarnings("unchecked")
    public T lerp(double t) {
        if (beginValue instanceof Number && endValue instanceof Number) {
            double b = ((Number) beginValue).doubleValue();
            double e = ((Number) endValue).doubleValue();
            return (T) Double.valueOf(b + (e - b) * t);
        }
        if (t < 0.5) {
            return beginValue;
        }
        return endValue;
    }

    @Override
    public T transform(double t) {
        if (t == 0.0) {
            return beginValue;
        }
        if (t == 1.0) {
            return endValue;
        }
        return lerp(t);
    }
}
