/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.surfaces;

/// A color used on an external surface. Because surfaces render outside the app (potentially while
/// the app process is dead) they cannot resolve theme constants at render time, so a surface color
/// is either an explicit ARGB value with an optional dark-mode counterpart, or a semantic role the
/// operating system resolves natively (label, secondary label, background, accent).
///
/// ```java
/// SurfaceColor.rgb(0xff333333, 0xffeeeeee)   // dark text in light mode, light text in dark mode
/// SurfaceColor.LABEL                         // whatever the OS considers primary label color
/// ```
public final class SurfaceColor {
    /// The platform's primary label (body text) color.
    public static final SurfaceColor LABEL = new SurfaceColor("label");

    /// The platform's secondary (dimmed) label color.
    public static final SurfaceColor SECONDARY_LABEL = new SurfaceColor("secondaryLabel");

    /// The platform's standard surface background color.
    public static final SurfaceColor BACKGROUND = new SurfaceColor("background");

    /// The platform accent / tint color.
    public static final SurfaceColor ACCENT = new SurfaceColor("accent");

    private final int light;
    private final int dark;
    private final String role;

    private SurfaceColor(int light, int dark) {
        this.light = light;
        this.dark = dark;
        this.role = null;
    }

    private SurfaceColor(String role) {
        this.light = 0;
        this.dark = 0;
        this.role = role;
    }

    /// Creates a color used in both light and dark appearance.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color as `0xAARRGGBB`
    ///
    /// #### Returns
    ///
    /// the color
    public static SurfaceColor rgb(int argb) {
        return new SurfaceColor(argb, argb);
    }

    /// Creates a color with distinct light and dark appearance values. The surface renderer picks
    /// the value matching the system appearance.
    ///
    /// #### Parameters
    ///
    /// - `lightArgb`: the color used in light mode as `0xAARRGGBB`
    /// - `darkArgb`: the color used in dark mode as `0xAARRGGBB`
    ///
    /// #### Returns
    ///
    /// the color
    public static SurfaceColor rgb(int lightArgb, int darkArgb) {
        return new SurfaceColor(lightArgb, darkArgb);
    }

    /// Returns the light-mode ARGB value; meaningless for role colors.
    public int getLight() {
        return light;
    }

    /// Returns the dark-mode ARGB value; meaningless for role colors.
    public int getDark() {
        return dark;
    }

    /// Returns the semantic role name (`label`, `secondaryLabel`, `background`, `accent`), or null
    /// for explicit ARGB colors.
    public String getRole() {
        return role;
    }
}
