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
 * A mapping from a {@code double} (typically an animation's 0..1 value) to a
 * value of type {@code T} — Flutter's {@code Animatable<T>}, the supertype of
 * {@link Tween} and {@link CurveTween}.
 */
public abstract class Animatable<T> {

    /** Maps the parametric value {@code t} to a {@code T}. */
    public abstract T transform(double t);

    /** {@code transform(animation.value)}. */
    public T evaluate(Animation<Double> animation) {
        return transform(animation.value());
    }

    /** Returns an {@link Animation} whose value is {@code transform(parent.value)}. */
    public Animation<T> animate(Animation<Double> parent) {
        return new AnimatedEvaluation<T>(parent, this);
    }

    /** Chains this after {@code parent}: {@code transform(parent.transform(t))}. */
    public Animatable<T> chain(Animatable<Double> parent) {
        return new ChainedEvaluation<T>(parent, this);
    }
}
