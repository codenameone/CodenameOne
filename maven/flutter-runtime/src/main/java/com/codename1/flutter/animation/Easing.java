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
