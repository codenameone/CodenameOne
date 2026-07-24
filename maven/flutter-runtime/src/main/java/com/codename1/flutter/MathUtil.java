package com.codename1.flutter;

/**
 * Small math helpers from dart:ui that new_gallery reaches by bare name.
 * Currently only {@code lerpDouble}, used by the cut-corners input border to
 * animate its notch.
 */
public final class MathUtil {

    private MathUtil() {
    }

    /**
     * dart:ui's top-level {@code lerpDouble(a, b, t)}: linearly interpolate
     * between two nullable numbers. Returns null when both {@code a} and
     * {@code b} are null; treats a lone null endpoint as 0. {@code a}/{@code b}
     * are declared {@code Object} to mirror the {@code num?} stub (they arrive
     * as boxed {@link Double}/{@link Long}).
     */
    public static Double lerpDouble(Object a, Object b, double t) {
        if (a == null && b == null) {
            return null;
        }
        double da = toDouble(a);
        double db = toDouble(b);
        return da + (db - da) * t;
    }

    private static double toDouble(Object n) {
        if (n instanceof Number) {
            return ((Number) n).doubleValue();
        }
        return 0.0;
    }
}
