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
 * The {@link Animatable} produced by {@code evaluatable.chain(parent)}:
 * {@code transform(t) == evaluatable.transform(parent.transform(t))}. Used to
 * compose a {@link CurveTween} in front of another tween.
 */
public class ChainedEvaluation<T> extends Animatable<T> {

    private final Animatable<Double> parent;
    private final Animatable<T> evaluatable;

    public ChainedEvaluation(Animatable<Double> parent, Animatable<T> evaluatable) {
        this.parent = parent;
        this.evaluatable = evaluatable;
    }

    @Override
    public T transform(double t) {
        return evaluatable.transform(parent.transform(t));
    }
}
