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
