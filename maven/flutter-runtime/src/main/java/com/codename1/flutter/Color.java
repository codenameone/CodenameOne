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
package com.codename1.flutter;

/**
 * An immutable 32-bit ARGB color, mirroring Flutter's {@code Color}.
 * {@code new Color(0xFF2196F3)} is fully opaque material blue.
 */
public class Color {

    private final int value;

    public Color(long argb) {
        // Dart `int` maps to Java `long` in the transpiler, and opaque ARGB
        // literals (0xFF......) exceed the signed-int range; truncate to the
        // 32-bit ARGB word. `new Color(intLiteral)` still widens in.
        this.value = (int) argb;
    }

    /**
     * {@code Color.fromRGBO}: red/green/blue channels (0..255) with a
     * floating-point opacity (0.0..1.0) that becomes the alpha channel.
     */
    public static Color fromRGBO(long r, long g, long b, double opacity) {
        long a = Math.round(opacity * 255.0) & 0xFF;
        return new Color((a << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    /**
     * The full ARGB value, as Dart sees it — {@code Color.value}.
     *
     * <p>Unsigned, and a {@code long}, because Dart's {@code int} is 64-bit and
     * the gallery prints this: {@code color.value.toRadixString(16)}. Returning
     * the signed 32-bit word made every opaque colour negative, so the colors
     * demo listed "#000-1412" beside each swatch instead of "#FFFFEBEE".</p>
     */
    public long value() {
        return value & 0xFFFFFFFFL;
    }

    /** The same word as a signed 32-bit int, for Codename One's style API. */
    public int argb() {
        return value;
    }

    public int alpha() {
        return (value >> 24) & 0xFF;
    }

    public int red() {
        return (value >> 16) & 0xFF;
    }

    public int green() {
        return (value >> 8) & 0xFF;
    }

    public int blue() {
        return value & 0xFF;
    }

    /**
     * The 24-bit RGB portion — the form CN1 style colors use.
     */
    public int rgb() {
        return value & 0xFFFFFF;
    }

    /**
     * A copy of this color with the alpha channel replaced so it is {@code
     * opacity} (0..1) of fully opaque; RGB is unchanged.
     */
    public Color withOpacity(double opacity) {
        int a = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255.0);
        return withAlpha(a);
    }

    /**
     * A copy of this color with the alpha channel set to {@code a} (0..255).
     * The parameter is a {@code long} because the transpiler maps Dart
     * {@code int} to Java {@code long}.
     */
    public Color withAlpha(long a) {
        int alpha = (int) (a & 0xFF);
        return new Color(((long) alpha << 24) | (value & 0xFFFFFFL));
    }

    /**
     * A copy with the supplied (non-null) 8-bit channels overridden; unset
     * channels keep this color's value. Mirrors the older component form of
     * Flutter's {@code Color.copyWith}. The boxed parameters are {@code Long}
     * because the transpiler maps Dart {@code int} to Java {@code long}.
     */
    public Color copyWith(Long alpha, Long red, Long green, Long blue) {
        int a = alpha != null ? (int) (alpha & 0xFF) : alpha();
        int r = red != null ? (int) (red & 0xFF) : red();
        int g = green != null ? (int) (green & 0xFF) : green();
        int b = blue != null ? (int) (blue & 0xFF) : blue();
        return new Color(((long) a << 24) | (r << 16) | (g << 8) | b);
    }

    /**
     * Composites {@code foreground} over {@code background} using
     * source-over alpha blending (Flutter's {@code Color.alphaBlend}); the
     * result is fully opaque when {@code background} is opaque.
     */
    public static Color alphaBlend(Color foreground, Color background) {
        int fa = foreground.alpha();
        if (fa == 0xFF) {
            return foreground;
        }
        if (fa == 0) {
            return background;
        }
        double af = fa / 255.0;
        double ab = background.alpha() / 255.0;
        double ao = af + ab * (1 - af);
        if (ao == 0) {
            return new Color(0);
        }
        int r = blendChannel(foreground.red(), af, background.red(), ab, ao);
        int g = blendChannel(foreground.green(), af, background.green(), ab, ao);
        int b = blendChannel(foreground.blue(), af, background.blue(), ab, ao);
        int a = (int) Math.round(ao * 255.0);
        return new Color(((long) a << 24) | (r << 16) | (g << 8) | b);
    }

    private static int blendChannel(int cf, double af, int cb, double ab, double ao) {
        double v = (cf * af + cb * ab * (1 - af)) / ao;
        return Math.max(0, Math.min(255, (int) Math.round(v)));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Color && ((Color) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "Color(0x" + String.format("%08X", value) + ")";
    }
}
