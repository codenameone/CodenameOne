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

import com.codename1.flutter.Color;
import com.codename1.flutter.Colors;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;

/**
 * The set of geometry-specific {@link TextTheme}s for a Material design
 * language — Flutter's {@code Typography}. A ThemeData is built from
 * {@code Typography.material2018(...)}; its {@code englishLike}/{@code dense}/
 * {@code tall} themes are merged by script. This pass records the supplied
 * themes; when none are given the getters return {@code null} and the caller's
 * ThemeData falls back to its own defaults.
 */
public class Typography {

    private Object platform;
    private TextTheme black;
    private TextTheme white;
    private TextTheme englishLike;
    private TextTheme dense;
    private TextTheme tall;

    private Typography() {
    }

    /**
     * Dart's {@code Typography.material2018(...)} factory.
     *
     * <p>Called from Dart as {@code Typography.material2018(platform: ...)} and
     * nothing else, so every text theme arrives null and this used to record
     * five nulls and change nothing. That is not what the caller asked for: a
     * theme naming this typography is asking for the 2018 (Material 2) type
     * scale, which is a different set of sizes, weights and INKS from the
     * Material 3 defaults -- the gallery's demos are framed in it, so its
     * typography page rendered every sample at roughly 60% of its size and in
     * full black where the design is grey.</p>
     */
    public static Typography material2018(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        return build(platform,
                black != null ? black : blackMountainView(),
                white != null ? white : whiteMountainView(),
                englishLike != null ? englishLike : englishLike2018(),
                dense, tall);
    }

    /**
     * Flutter's {@code englishLike2018} geometry: size, weight and tracking per
     * role, with no colour (the colour comes from the black/white theme this is
     * merged with).
     */
    public static TextTheme englishLike2018() {
        TextTheme t = new TextTheme();
        t.displayLarge(style(96, FontWeight.w300, -1.5));
        t.displayMedium(style(60, FontWeight.w300, -0.5));
        t.displaySmall(style(48, FontWeight.w400, 0));
        t.headlineLarge(style(40, FontWeight.w400, 0.25));
        t.headlineMedium(style(34, FontWeight.w400, 0.25));
        t.headlineSmall(style(24, FontWeight.w400, 0));
        t.titleLarge(style(20, FontWeight.w500, 0.15));
        t.titleMedium(style(16, FontWeight.w400, 0.15));
        t.titleSmall(style(14, FontWeight.w500, 0.1));
        t.bodyLarge(style(16, FontWeight.w400, 0.5));
        t.bodyMedium(style(14, FontWeight.w400, 0.25));
        t.bodySmall(style(12, FontWeight.w400, 0.4));
        t.labelLarge(style(14, FontWeight.w500, 1.25));
        t.labelMedium(style(12, FontWeight.w400, 1.5));
        t.labelSmall(style(10, FontWeight.w400, 1.5));
        return t;
    }

    /** Flutter's {@code blackMountainView} inks: display roles grey, body roles near-black. */
    public static TextTheme blackMountainView() {
        return inks(Colors.black54, Colors.black87, Colors.black);
    }

    /** Flutter's {@code whiteMountainView} inks, for a dark theme. */
    public static TextTheme whiteMountainView() {
        return inks(Colors.white70, Colors.white, Colors.white);
    }

    private static TextTheme inks(Color display, Color body, Color emphasis) {
        TextTheme t = new TextTheme();
        t.displayLarge(ink(display));
        t.displayMedium(ink(display));
        t.displaySmall(ink(display));
        t.headlineLarge(ink(display));
        t.headlineMedium(ink(display));
        t.headlineSmall(ink(body));
        t.titleLarge(ink(body));
        t.titleMedium(ink(body));
        t.titleSmall(ink(emphasis));
        t.bodyLarge(ink(body));
        t.bodyMedium(ink(body));
        t.bodySmall(ink(display));
        t.labelLarge(ink(body));
        t.labelMedium(ink(body));
        t.labelSmall(ink(emphasis));
        return t;
    }

    private static TextStyle style(double size, FontWeight weight, double tracking) {
        TextStyle t = new TextStyle();
        t.fontSize(size);
        t.fontWeight(weight);
        t.letterSpacing(tracking);
        return t;
    }

    private static TextStyle ink(Color c) {
        TextStyle t = new TextStyle();
        t.color(c);
        return t;
    }

    /**
     * The text theme a {@link ThemeData} should use when it names this
     * typography and no explicit textTheme: the geometry, with the ink for the
     * requested brightness layered on top — Flutter's
     * {@code defaultTextTheme.merge(...)}.
     */
    public TextTheme resolve(boolean dark) {
        TextTheme geometry = englishLike != null ? englishLike : englishLike2018();
        TextTheme colours = dark
                ? (white != null ? white : whiteMountainView())
                : (black != null ? black : blackMountainView());
        return geometry.merge(colours);
    }

    /** Dart's {@code Typography.material2014(...)} factory. */
    public static Typography material2014(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        return build(platform, black, white, englishLike, dense, tall);
    }

    private static Typography build(Object platform, TextTheme black, TextTheme white,
            TextTheme englishLike, TextTheme dense, TextTheme tall) {
        Typography t = new Typography();
        t.platform = platform;
        t.black = black;
        t.white = white;
        t.englishLike = englishLike;
        t.dense = dense;
        t.tall = tall;
        return t;
    }

    public TextTheme black() {
        return black;
    }

    public TextTheme white() {
        return white;
    }

    public TextTheme englishLike() {
        return englishLike;
    }

    public TextTheme dense() {
        return dense;
    }

    public TextTheme tall() {
        return tall;
    }
}
