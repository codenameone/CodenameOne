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
import com.codename1.flutter.TextStyle;

/**
 * Material 3 default text styles. Each getter returns the configured override
 * when {@link #copyWith} (or a builder) supplied one, otherwise a fresh
 * {@link TextStyle} carrying the M3 default logical size for that role. Fresh
 * instances are returned for the defaults because TextStyle is a mutable
 * write-once config object; sharing would leak one call site's mutation.
 */
public class TextTheme {

    private TextStyle displayLarge;
    private TextStyle displayMedium;
    private TextStyle displaySmall;
    private TextStyle headlineLarge;
    private TextStyle headlineMedium;
    private TextStyle headlineSmall;
    private TextStyle titleLarge;
    private TextStyle titleMedium;
    private TextStyle titleSmall;
    private TextStyle bodyLarge;
    private TextStyle bodyMedium;
    private TextStyle bodySmall;
    private TextStyle labelLarge;
    private TextStyle labelMedium;
    private TextStyle labelSmall;

    /**
     * This theme with {@code other}'s properties layered on top, role by role —
     * Flutter's {@code TextTheme.merge}. Used to combine a type GEOMETRY (sizes,
     * weights, tracking) with an INK theme (colours), which is how
     * {@link Typography} composes its two halves.
     */
    public TextTheme merge(TextTheme other) {
        if (other == null) {
            return this;
        }
        TextTheme t = new TextTheme();
        t.displayLarge = displayLarge().merge(other.displayLarge);
        t.displayMedium = displayMedium().merge(other.displayMedium);
        t.displaySmall = displaySmall().merge(other.displaySmall);
        t.headlineLarge = headlineLarge().merge(other.headlineLarge);
        t.headlineMedium = headlineMedium().merge(other.headlineMedium);
        t.headlineSmall = headlineSmall().merge(other.headlineSmall);
        t.titleLarge = titleLarge().merge(other.titleLarge);
        t.titleMedium = titleMedium().merge(other.titleMedium);
        t.titleSmall = titleSmall().merge(other.titleSmall);
        t.bodyLarge = bodyLarge().merge(other.bodyLarge);
        t.bodyMedium = bodyMedium().merge(other.bodyMedium);
        t.bodySmall = bodySmall().merge(other.bodySmall);
        t.labelLarge = labelLarge().merge(other.labelLarge);
        t.labelMedium = labelMedium().merge(other.labelMedium);
        t.labelSmall = labelSmall().merge(other.labelSmall);
        return t;
    }

    private static TextStyle sized(double size) {
        TextStyle t = new TextStyle();
        t.fontSize(size);
        return t;
    }

    // ------------------------------------------------------------------
    // Named-parameter setters (the Dart constructor's named args and any
    // {@code textTheme.copyWith(role: style)}-style overrides land here).
    // ------------------------------------------------------------------

    public void displayLarge(TextStyle v) {
        this.displayLarge = v;
    }

    public void displayMedium(TextStyle v) {
        this.displayMedium = v;
    }

    public void displaySmall(TextStyle v) {
        this.displaySmall = v;
    }

    public void headlineLarge(TextStyle v) {
        this.headlineLarge = v;
    }

    public void headlineMedium(TextStyle v) {
        this.headlineMedium = v;
    }

    public void headlineSmall(TextStyle v) {
        this.headlineSmall = v;
    }

    public void titleLarge(TextStyle v) {
        this.titleLarge = v;
    }

    public void titleMedium(TextStyle v) {
        this.titleMedium = v;
    }

    public void titleSmall(TextStyle v) {
        this.titleSmall = v;
    }

    public void bodyLarge(TextStyle v) {
        this.bodyLarge = v;
    }

    public void bodyMedium(TextStyle v) {
        this.bodyMedium = v;
    }

    public void bodySmall(TextStyle v) {
        this.bodySmall = v;
    }

    public void labelLarge(TextStyle v) {
        this.labelLarge = v;
    }

    public void labelMedium(TextStyle v) {
        this.labelMedium = v;
    }

    public void labelSmall(TextStyle v) {
        this.labelSmall = v;
    }

    public TextStyle displayLarge() {
        return displayLarge != null ? displayLarge : sized(57);
    }

    public TextStyle displayMedium() {
        return displayMedium != null ? displayMedium : sized(45);
    }

    public TextStyle displaySmall() {
        return displaySmall != null ? displaySmall : sized(36);
    }

    public TextStyle headlineLarge() {
        return headlineLarge != null ? headlineLarge : sized(32);
    }

    public TextStyle headlineMedium() {
        return headlineMedium != null ? headlineMedium : sized(28);
    }

    public TextStyle headlineSmall() {
        return headlineSmall != null ? headlineSmall : sized(24);
    }

    public TextStyle titleLarge() {
        return titleLarge != null ? titleLarge : sized(22);
    }

    public TextStyle titleMedium() {
        return titleMedium != null ? titleMedium : sized(16);
    }

    public TextStyle titleSmall() {
        return titleSmall != null ? titleSmall : sized(14);
    }

    public TextStyle bodyLarge() {
        return bodyLarge != null ? bodyLarge : sized(16);
    }

    public TextStyle bodyMedium() {
        return bodyMedium != null ? bodyMedium : sized(14);
    }

    public TextStyle bodySmall() {
        return bodySmall != null ? bodySmall : sized(12);
    }

    public TextStyle labelLarge() {
        return labelLarge != null ? labelLarge : sized(14);
    }

    public TextStyle labelMedium() {
        return labelMedium != null ? labelMedium : sized(12);
    }

    public TextStyle labelSmall() {
        return labelSmall != null ? labelSmall : sized(11);
    }

    /**
     * Returns a copy with the supplied (non-null) roles overridden. Parameters
     * follow the M3 role order declared in the Dart stub.
     */
    public TextTheme copyWith(TextStyle displayLarge, TextStyle displayMedium, TextStyle displaySmall,
                              TextStyle headlineLarge, TextStyle headlineMedium, TextStyle headlineSmall,
                              TextStyle titleLarge, TextStyle titleMedium, TextStyle titleSmall,
                              TextStyle bodyLarge, TextStyle bodyMedium, TextStyle bodySmall,
                              TextStyle labelLarge, TextStyle labelMedium, TextStyle labelSmall) {
        TextTheme c = new TextTheme();
        c.displayLarge = displayLarge != null ? displayLarge : this.displayLarge;
        c.displayMedium = displayMedium != null ? displayMedium : this.displayMedium;
        c.displaySmall = displaySmall != null ? displaySmall : this.displaySmall;
        c.headlineLarge = headlineLarge != null ? headlineLarge : this.headlineLarge;
        c.headlineMedium = headlineMedium != null ? headlineMedium : this.headlineMedium;
        c.headlineSmall = headlineSmall != null ? headlineSmall : this.headlineSmall;
        c.titleLarge = titleLarge != null ? titleLarge : this.titleLarge;
        c.titleMedium = titleMedium != null ? titleMedium : this.titleMedium;
        c.titleSmall = titleSmall != null ? titleSmall : this.titleSmall;
        c.bodyLarge = bodyLarge != null ? bodyLarge : this.bodyLarge;
        c.bodyMedium = bodyMedium != null ? bodyMedium : this.bodyMedium;
        c.bodySmall = bodySmall != null ? bodySmall : this.bodySmall;
        c.labelLarge = labelLarge != null ? labelLarge : this.labelLarge;
        c.labelMedium = labelMedium != null ? labelMedium : this.labelMedium;
        c.labelSmall = labelSmall != null ? labelSmall : this.labelSmall;
        return c;
    }

    /**
     * Returns a copy in which every role's style has {@code bodyColor} applied
     * to the body/label/title roles and {@code displayColor} to the
     * display/headline roles, with an optional {@code fontFamily} and font-size
     * scaling applied uniformly. Mirrors Flutter's {@code TextTheme.apply}.
     * Parameter order matches the Dart stub.
     */
    public TextTheme apply(String fontFamily, Double fontSizeFactor, Double fontSizeDelta,
                           Color displayColor, Color bodyColor, Object decoration, Object decorationColor) {
        TextTheme c = new TextTheme();
        c.displayLarge = applyOne(displayLarge(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.displayMedium = applyOne(displayMedium(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.displaySmall = applyOne(displaySmall(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.headlineLarge = applyOne(headlineLarge(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.headlineMedium = applyOne(headlineMedium(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.headlineSmall = applyOne(headlineSmall(), displayColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.titleLarge = applyOne(titleLarge(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.titleMedium = applyOne(titleMedium(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.titleSmall = applyOne(titleSmall(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.bodyLarge = applyOne(bodyLarge(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.bodyMedium = applyOne(bodyMedium(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.bodySmall = applyOne(bodySmall(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.labelLarge = applyOne(labelLarge(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.labelMedium = applyOne(labelMedium(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        c.labelSmall = applyOne(labelSmall(), bodyColor, fontFamily, fontSizeFactor, fontSizeDelta);
        return c;
    }

    private static TextStyle applyOne(TextStyle base, Color color, String fontFamily,
                                      Double fontSizeFactor, Double fontSizeDelta) {
        return base.apply(color, null, fontFamily, fontSizeFactor, fontSizeDelta, null);
    }
}
