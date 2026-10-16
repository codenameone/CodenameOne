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

import dart.core.Duration;

/**
 * Top-level {@code const} values of Flutter's {@code package:flutter/material.dart}
 * that new_gallery references directly, mirrored as Java statics. The transpiler
 * routes the bare identifiers ({@code kToolbarHeight}, ...) to these fields.
 */
public final class MaterialConstants {

    private MaterialConstants() {
    }

    /** Flutter's {@code kToolbarHeight}: the default AppBar height (logical px). */
    public static final double kToolbarHeight = 56.0;

    /** Flutter's {@code kFloatingActionButtonMargin}: default FAB margin (logical px). */
    public static final double kFloatingActionButtonMargin = 16.0;

    /** Flutter's {@code kThemeAnimationDuration}: theme cross-fade duration. */
    public static final Duration kThemeAnimationDuration = Duration.of(0, 0, 0, 0, 200, 0);

    /** Flutter's {@code kBottomNavigationBarHeight}: the default bottom nav bar height (logical px). */
    public static final double kBottomNavigationBarHeight = 56.0;
}
