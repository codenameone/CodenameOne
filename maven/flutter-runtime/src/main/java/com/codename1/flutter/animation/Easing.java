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
 * The Material 3 named easing curves — Flutter's {@code Easing}. Each is a
 * static {@link Curve} (the {@code legacy}/{@code standard}/{@code emphasized}
 * families are Bezier {@link Cubic}s using the same control points as Flutter);
 * the reply study reads {@code Easing.legacy} and {@code Easing.legacy.flipped}.
 */
public final class Easing {

    private Easing() {
    }

    public static final Curve linear = Curves.linear;
    public static final Curve legacy = new Cubic(0.4, 0.0, 0.2, 1.0);
    public static final Curve legacyDecelerate = new Cubic(0.0, 0.0, 0.2, 1.0);
    public static final Curve legacyAccelerate = new Cubic(0.4, 0.0, 1.0, 1.0);
    public static final Curve standard = new Cubic(0.2, 0.0, 0.0, 1.0);
    public static final Curve standardAccelerate = new Cubic(0.3, 0.0, 1.0, 1.0);
    public static final Curve standardDecelerate = new Cubic(0.0, 0.0, 0.0, 1.0);
    public static final Curve emphasized = new Cubic(0.2, 0.0, 0.0, 1.0);
    public static final Curve emphasizedAccelerate = new Cubic(0.3, 0.0, 0.8, 0.15);
    public static final Curve emphasizedDecelerate = new Cubic(0.05, 0.7, 0.1, 1.0);
}
