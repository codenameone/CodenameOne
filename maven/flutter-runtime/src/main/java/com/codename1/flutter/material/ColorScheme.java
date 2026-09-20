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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * A Material 3 color scheme. Two ways to build one:
 * <ul>
 *   <li>{@link #fromSeed(Color, Brightness)} derives the full role set from a
 *       seed color with an HSL approximation of the M3 tonal palettes;</li>
 *   <li>the write-once named constructor ({@code ColorScheme(primary: ...,
 *       brightness: ...)}) sets roles explicitly — unset roles fall back to a
 *       related role so callers that specify only a subset still read sensibly.</li>
 * </ul>
 * The HSL derivation (not the full HCT algorithm) matches the earlier M1 scope.
 */
public class ColorScheme {

    private Brightness brightness;
    private Color primary;
    private Color onPrimary;
    private Color primaryContainer;
    private Color onPrimaryContainer;
    private Color inversePrimary;
    private Color secondary;
    private Color onSecondary;
    private Color secondaryContainer;
    private Color onSecondaryContainer;
    private Color tertiary;
    private Color onTertiary;
    private Color tertiaryContainer;
    private Color onTertiaryContainer;
    private Color error;
    private Color onError;
    private Color errorContainer;
    private Color onErrorContainer;
    private Color surface;
    private Color onSurface;
    private Color surfaceVariant;
    private Color onSurfaceVariant;
    private Color background;
    private Color onBackground;
    private Color outline;
    private Color outlineVariant;
    private Color surfaceContainerLowest;
    private Color surfaceContainerLow;
    private Color surfaceContainer;
    private Color surfaceContainerHigh;
    private Color surfaceContainerHighest;
    private Color shadow;
    private Color scrim;
    private Color inverseSurface;
    private Color onInverseSurface;

    public ColorScheme() {
    }

    public static ColorScheme fromSeed(Color seedColor) {
        return fromSeed(seedColor, null);
    }

    /**
     * Canonical two-parameter form: a null brightness means light.
     */
    public static ColorScheme fromSeed(Color seedColor, Brightness brightness) {
        double[] hsl = toHsl(seedColor.argb());
        double h = hsl[0];
        double s = hsl[1];
        ColorScheme c = new ColorScheme();
        c.brightness = brightness;
        if (brightness == Brightness.dark) {
            c.primary = fromHsl(h, Math.min(1, s + 0.15), 0.80);
            c.inversePrimary = fromHsl(h, s, 0.40);
            c.onPrimary = fromHsl(h, s, 0.20);
            c.surface = fromHsl(h, Math.min(0.25, s), 0.06);
            c.onSurface = new Color(0xFFE6E1E5);
            c.secondary = fromHsl(h, s * 0.35, 0.70);
        } else {
            c.primary = fromHsl(h, s, 0.40);
            c.inversePrimary = fromHsl(h, Math.min(1, s + 0.15), 0.80);
            c.onPrimary = new Color(0xFFFFFFFF);
            c.surface = fromHsl(h, Math.min(0.35, s), 0.98);
            c.onSurface = new Color(0xFF1C1B1F);
            c.secondary = fromHsl(h, s * 0.35, 0.45);
        }
        return c;
    }

    private static final Color DEFAULT_SEED = new Color(0xFF6750A4);

    /**
     * {@code ColorScheme.light}/{@code .dark} — the seeded defaults with any explicitly
     * named role applied over them.
     *
     * <p>These took no parameters at all, so a scheme written out role by role (which is
     * how all four studies define their palettes) was built entirely from the default seed
     * and every colour the app asked for was dropped. The roles are applied over the seeded
     * base rather than replacing it, so naming one does not blank the rest.</p>
     */
    public static ColorScheme light(Color primary, Color onPrimary, Color primaryContainer,
            Color onPrimaryContainer, Color secondary, Color onSecondary,
            Color secondaryContainer, Color onSecondaryContainer, Color tertiary,
            Color onTertiary, Color error, Color onError, Color errorContainer,
            Color onErrorContainer, Color surface, Color onSurface, Color surfaceVariant,
            Color onSurfaceVariant, Color background, Color onBackground, Color outline,
            Color shadow, Color inverseSurface, Color onInverseSurface, Color inversePrimary,
            Brightness brightness) {
        return applyRoles(fromSeed(DEFAULT_SEED, Brightness.light), primary, onPrimary,
                primaryContainer, onPrimaryContainer, secondary, onSecondary,
                secondaryContainer, onSecondaryContainer, tertiary, onTertiary, error, onError,
                errorContainer, onErrorContainer, surface, onSurface, surfaceVariant,
                onSurfaceVariant, background, onBackground, outline, shadow, inverseSurface,
                onInverseSurface, inversePrimary, brightness);
    }

    public static ColorScheme dark(Color primary, Color onPrimary, Color primaryContainer,
            Color onPrimaryContainer, Color secondary, Color onSecondary,
            Color secondaryContainer, Color onSecondaryContainer, Color tertiary,
            Color onTertiary, Color error, Color onError, Color errorContainer,
            Color onErrorContainer, Color surface, Color onSurface, Color surfaceVariant,
            Color onSurfaceVariant, Color background, Color onBackground, Color outline,
            Color shadow, Color inverseSurface, Color onInverseSurface, Color inversePrimary,
            Brightness brightness) {
        return applyRoles(fromSeed(DEFAULT_SEED, Brightness.dark), primary, onPrimary,
                primaryContainer, onPrimaryContainer, secondary, onSecondary,
                secondaryContainer, onSecondaryContainer, tertiary, onTertiary, error, onError,
                errorContainer, onErrorContainer, surface, onSurface, surfaceVariant,
                onSurfaceVariant, background, onBackground, outline, shadow, inverseSurface,
                onInverseSurface, inversePrimary, brightness);
    }

    private static ColorScheme applyRoles(ColorScheme c, Color primary, Color onPrimary,
            Color primaryContainer, Color onPrimaryContainer, Color secondary,
            Color onSecondary, Color secondaryContainer, Color onSecondaryContainer,
            Color tertiary, Color onTertiary, Color error, Color onError,
            Color errorContainer, Color onErrorContainer, Color surface, Color onSurface,
            Color surfaceVariant, Color onSurfaceVariant, Color background,
            Color onBackground, Color outline, Color shadow, Color inverseSurface,
            Color onInverseSurface, Color inversePrimary, Brightness brightness) {
        if (primary != null) { c.primary(primary); }
        if (onPrimary != null) { c.onPrimary(onPrimary); }
        if (primaryContainer != null) { c.primaryContainer(primaryContainer); }
        if (onPrimaryContainer != null) { c.onPrimaryContainer(onPrimaryContainer); }
        if (secondary != null) { c.secondary(secondary); }
        if (onSecondary != null) { c.onSecondary(onSecondary); }
        if (secondaryContainer != null) { c.secondaryContainer(secondaryContainer); }
        if (onSecondaryContainer != null) { c.onSecondaryContainer(onSecondaryContainer); }
        if (tertiary != null) { c.tertiary(tertiary); }
        if (onTertiary != null) { c.onTertiary(onTertiary); }
        if (error != null) { c.error(error); }
        if (onError != null) { c.onError(onError); }
        if (errorContainer != null) { c.errorContainer(errorContainer); }
        if (onErrorContainer != null) { c.onErrorContainer(onErrorContainer); }
        if (surface != null) { c.surface(surface); }
        if (onSurface != null) { c.onSurface(onSurface); }
        if (surfaceVariant != null) { c.surfaceVariant(surfaceVariant); }
        if (onSurfaceVariant != null) { c.onSurfaceVariant(onSurfaceVariant); }
        if (background != null) { c.background(background); }
        if (onBackground != null) { c.onBackground(onBackground); }
        if (outline != null) { c.outline(outline); }
        if (shadow != null) { c.shadow(shadow); }
        if (inverseSurface != null) { c.inverseSurface(inverseSurface); }
        if (onInverseSurface != null) { c.onInverseSurface(onInverseSurface); }
        if (inversePrimary != null) { c.inversePrimary(inversePrimary); }
        if (brightness != null) { c.brightness(brightness); }
        return c;
    }

    // ------------------------------------------------------------------
    // Named-parameter setters
    // ------------------------------------------------------------------

    public void brightness(Brightness v) { this.brightness = v; }
    public void primary(Color v) { this.primary = v; }
    public void onPrimary(Color v) { this.onPrimary = v; }
    public void primaryContainer(Color v) { this.primaryContainer = v; }
    public void onPrimaryContainer(Color v) { this.onPrimaryContainer = v; }
    public void inversePrimary(Color v) { this.inversePrimary = v; }
    public void secondary(Color v) { this.secondary = v; }
    public void onSecondary(Color v) { this.onSecondary = v; }
    public void secondaryContainer(Color v) { this.secondaryContainer = v; }
    public void onSecondaryContainer(Color v) { this.onSecondaryContainer = v; }
    public void tertiary(Color v) { this.tertiary = v; }
    public void onTertiary(Color v) { this.onTertiary = v; }
    public void tertiaryContainer(Color v) { this.tertiaryContainer = v; }
    public void onTertiaryContainer(Color v) { this.onTertiaryContainer = v; }
    public void error(Color v) { this.error = v; }
    public void onError(Color v) { this.onError = v; }
    public void errorContainer(Color v) { this.errorContainer = v; }
    public void onErrorContainer(Color v) { this.onErrorContainer = v; }
    public void surface(Color v) { this.surface = v; }
    public void onSurface(Color v) { this.onSurface = v; }
    public void surfaceVariant(Color v) { this.surfaceVariant = v; }
    public void onSurfaceVariant(Color v) { this.onSurfaceVariant = v; }
    public void background(Color v) { this.background = v; }
    public void onBackground(Color v) { this.onBackground = v; }
    public void outline(Color v) { this.outline = v; }
    public void outlineVariant(Color v) { this.outlineVariant = v; }
    public void surfaceContainerLowest(Color v) { this.surfaceContainerLowest = v; }
    public void surfaceContainerLow(Color v) { this.surfaceContainerLow = v; }
    public void surfaceContainer(Color v) { this.surfaceContainer = v; }
    public void surfaceContainerHigh(Color v) { this.surfaceContainerHigh = v; }
    public void surfaceContainerHighest(Color v) { this.surfaceContainerHighest = v; }
    public void shadow(Color v) { this.shadow = v; }
    public void scrim(Color v) { this.scrim = v; }
    public void inverseSurface(Color v) { this.inverseSurface = v; }
    public void onInverseSurface(Color v) { this.onInverseSurface = v; }

    // ------------------------------------------------------------------
    // Getters (with role fallbacks for the unset subset)
    // ------------------------------------------------------------------

    private static Color or(Color a, Color b) {
        return a != null ? a : b;
    }

    public Brightness brightness() { return brightness == null ? Brightness.light : brightness; }
    public Color primary() { return primary; }
    public Color onPrimary() { return onPrimary; }
    public Color primaryContainer() { return or(primaryContainer, primary); }
    public Color onPrimaryContainer() { return or(onPrimaryContainer, onPrimary); }
    public Color inversePrimary() { return or(inversePrimary, primary); }
    public Color secondary() { return or(secondary, primary); }
    public Color onSecondary() { return or(onSecondary, onPrimary); }
    public Color secondaryContainer() { return or(secondaryContainer, secondary()); }
    public Color onSecondaryContainer() { return or(onSecondaryContainer, onSecondary()); }
    public Color tertiary() { return or(tertiary, secondary()); }
    public Color onTertiary() { return or(onTertiary, onSecondary()); }
    public Color tertiaryContainer() { return or(tertiaryContainer, tertiary()); }
    public Color onTertiaryContainer() { return or(onTertiaryContainer, onTertiary()); }
    public Color error() { return or(error, new Color(0xFFB00020)); }
    public Color onError() { return or(onError, new Color(0xFFFFFFFF)); }
    public Color errorContainer() { return or(errorContainer, error()); }
    public Color onErrorContainer() { return or(onErrorContainer, onError()); }
    public Color surface() { return surface; }
    public Color onSurface() { return onSurface; }
    public Color surfaceVariant() { return or(surfaceVariant, surface); }
    public Color onSurfaceVariant() { return or(onSurfaceVariant, onSurface); }
    public Color background() { return or(background, surface); }
    public Color onBackground() { return or(onBackground, onSurface); }
    /// Material 3's tonal surface containers. Flutter falls each of them back to
    /// {@link #surface()} when the scheme names none, and several component
    /// defaults are written in terms of them -- an elevated button's face is
    /// surfaceContainerLow, not primary.
    public Color surfaceContainerLowest() { return or(surfaceContainerLowest, surface); }
    public Color surfaceContainerLow() { return or(surfaceContainerLow, surface); }
    public Color surfaceContainer() { return or(surfaceContainer, surface); }
    public Color surfaceContainerHigh() { return or(surfaceContainerHigh, surface); }
    public Color surfaceContainerHighest() { return or(surfaceContainerHighest, surface); }

    public Color outline() { return or(outline, new Color(0xFF79747E)); }
    /// Falls back to {@link #onBackground()}, which is what Flutter's ColorScheme does --
    /// and it is load bearing rather than a detail, because an M3 Divider takes its colour
    /// from this role. A scheme written out by hand names neither, so falling back to
    /// `outline` painted the compose page's rules in a pale grey where the reference draws
    /// them almost black.
    public Color outlineVariant() { return or(outlineVariant, onBackground()); }
    public Color shadow() { return or(shadow, new Color(0xFF000000)); }
    public Color scrim() { return or(scrim, new Color(0xFF000000)); }
    public Color inverseSurface() { return or(inverseSurface, onSurface); }
    public Color onInverseSurface() { return or(onInverseSurface, surface); }

    /**
     * Returns a copy with the supplied (non-null) roles overridden. Parameter
     * order matches the Dart stub.
     */
    public ColorScheme copyWith(Brightness brightness, Color primary, Color onPrimary,
                                Color primaryContainer, Color onPrimaryContainer, Color secondary,
                                Color onSecondary, Color secondaryContainer, Color tertiary,
                                Color error, Color onError, Color surface, Color onSurface,
                                Color surfaceVariant, Color onSurfaceVariant, Color background,
                                Color onBackground, Color outline, Color inversePrimary,
                                Color inverseSurface, Color shadow) {
        ColorScheme c = new ColorScheme();
        c.brightness = brightness != null ? brightness : this.brightness;
        c.primary = primary != null ? primary : this.primary;
        c.onPrimary = onPrimary != null ? onPrimary : this.onPrimary;
        c.primaryContainer = primaryContainer != null ? primaryContainer : this.primaryContainer;
        c.onPrimaryContainer = onPrimaryContainer != null ? onPrimaryContainer : this.onPrimaryContainer;
        c.secondary = secondary != null ? secondary : this.secondary;
        c.onSecondary = onSecondary != null ? onSecondary : this.onSecondary;
        c.secondaryContainer = secondaryContainer != null ? secondaryContainer : this.secondaryContainer;
        c.tertiary = tertiary != null ? tertiary : this.tertiary;
        c.error = error != null ? error : this.error;
        c.onError = onError != null ? onError : this.onError;
        c.surface = surface != null ? surface : this.surface;
        c.onSurface = onSurface != null ? onSurface : this.onSurface;
        c.surfaceVariant = surfaceVariant != null ? surfaceVariant : this.surfaceVariant;
        c.onSurfaceVariant = onSurfaceVariant != null ? onSurfaceVariant : this.onSurfaceVariant;
        c.background = background != null ? background : this.background;
        c.onBackground = onBackground != null ? onBackground : this.onBackground;
        c.outline = outline != null ? outline : this.outline;
        c.inversePrimary = inversePrimary != null ? inversePrimary : this.inversePrimary;
        c.inverseSurface = inverseSurface != null ? inverseSurface : this.inverseSurface;
        c.shadow = shadow != null ? shadow : this.shadow;
        // carry the rest unchanged
        c.onSecondaryContainer = this.onSecondaryContainer;
        c.onTertiary = this.onTertiary;
        c.tertiaryContainer = this.tertiaryContainer;
        c.onTertiaryContainer = this.onTertiaryContainer;
        c.errorContainer = this.errorContainer;
        c.onErrorContainer = this.onErrorContainer;
        c.outlineVariant = this.outlineVariant;
        c.scrim = this.scrim;
        c.onInverseSurface = this.onInverseSurface;
        return c;
    }

    // ------------------------------------------------------------------
    // HSL helpers
    // ------------------------------------------------------------------

    static double[] toHsl(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        double h;
        double s;
        if (max == min) {
            h = 0;
            s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) * 60;
            } else if (max == g) {
                h = ((b - r) / d + 2) * 60;
            } else {
                h = ((r - g) / d + 4) * 60;
            }
        }
        return new double[]{h, s, l};
    }

    static Color fromHsl(double h, double s, double l) {
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double hh = (h % 360) / 60;
        double x = c * (1 - Math.abs(hh % 2 - 1));
        double r = 0;
        double g = 0;
        double b = 0;
        if (hh < 1) {
            r = c;
            g = x;
        } else if (hh < 2) {
            r = x;
            g = c;
        } else if (hh < 3) {
            g = c;
            b = x;
        } else if (hh < 4) {
            g = x;
            b = c;
        } else if (hh < 5) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        double m = l - c / 2;
        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);
        ri = Math.max(0, Math.min(255, ri));
        gi = Math.max(0, Math.min(255, gi));
        bi = Math.max(0, Math.min(255, bi));
        return new Color(0xFF000000 | (ri << 16) | (gi << 8) | bi);
    }
}
