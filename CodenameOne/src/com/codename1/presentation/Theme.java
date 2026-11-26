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
package com.codename1.presentation;

/// The colors and fonts a presentation is designed with. Formats that name theme colors
/// instead of values (PPTX's `accent1`, Keynote's styles) resolve them through this when read,
/// so every element in the model holds plain colors; the theme is kept so a writer can name
/// them again and a new slide picks matching colors.
public final class Theme {
    /// Index of the dark text color in `colors`.
    public static final int DARK1 = 0;

    /// Index of the light background color.
    public static final int LIGHT1 = 1;

    /// Index of the second dark color.
    public static final int DARK2 = 2;

    /// Index of the second light color.
    public static final int LIGHT2 = 3;

    /// Index of the first of six accent colors; the others follow it.
    public static final int ACCENT1 = 4;

    /// Index of the hyperlink color.
    public static final int HYPERLINK = 10;

    /// Index of the followed hyperlink color.
    public static final int FOLLOWED = 11;

    /// The names of the twelve colors, as OOXML's color scheme spells them.
    static final String[] NAMES = {"dk1", "lt1", "dk2", "lt2", "accent1", "accent2", "accent3",
        "accent4", "accent5", "accent6", "hlink", "folHlink"};

    /// The colors of the default theme, which also fill in a short color list.
    private static final int[] DEFAULT_COLORS = {0x1f2933,
        0xffffff, 0x334e68, 0xf0f4f8, 0x2f6fde, 0xe8590c, 0x2b9348, 0xb5179e, 0xf2a900,
        0x0b7285, 0x1c64f2, 0x7048e8};

    /// A neutral theme: dark slate on white, six distinguishable accents, Sans fonts.
    public static final Theme DEFAULT = new Theme("Codename One", DEFAULT_COLORS, "Sans", "Sans");

    /// The theme's name.
    public final String name;

    private final int[] colors;

    /// The font of titles.
    public final String majorFont;

    /// The font of body text.
    public final String minorFont;

    /// A theme.
    ///
    /// #### Parameters
    ///
    /// - `name`: its name
    ///
    /// - `colors`: twelve colors as 0xRRGGBB in the order of the index constants; missing ones
    ///   come from `DEFAULT`
    ///
    /// - `majorFont`: the title font
    ///
    /// - `minorFont`: the body font
    public Theme(String name, int[] colors, String majorFont, String minorFont) {
        this.name = name == null ? "" : name;
        this.colors = new int[12];
        for (int i = 0; i < 12; i++) {
            this.colors[i] = colors != null && i < colors.length ? colors[i] & 0xffffff
                    : DEFAULT_COLORS[i];
        }
        this.majorFont = majorFont == null || majorFont.length() == 0 ? "Sans" : majorFont;
        this.minorFont = minorFont == null || minorFont.length() == 0 ? "Sans" : minorFont;
    }

    /// A color of the scheme.
    ///
    /// #### Parameters
    ///
    /// - `index`: one of the index constants, 0 to 11
    ///
    /// #### Returns
    ///
    /// the color as 0xRRGGBB
    public int color(int index) {
        return colors[index < 0 || index > 11 ? 0 : index];
    }

    /// A color by its OOXML scheme name (`accent1`, `dk1`, `tx1`, `bg1`...).
    ///
    /// #### Parameters
    ///
    /// - `name`: the name
    ///
    /// #### Returns
    ///
    /// the color, or -1 for a name the scheme does not have
    public int color(String name) {
        if (name == null) {
            return -1;
        }
        if ("tx1".equals(name)) {
            return colors[DARK1];
        }
        if ("bg1".equals(name)) {
            return colors[LIGHT1];
        }
        if ("tx2".equals(name)) {
            return colors[DARK2];
        }
        if ("bg2".equals(name)) {
            return colors[LIGHT2];
        }
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return colors[i];
            }
        }
        return -1;
    }

    /// This theme with one color replaced.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index constant
    ///
    /// - `value`: 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// the theme
    public Theme withColor(int index, int value) {
        int[] c = new int[12];
        System.arraycopy(colors, 0, c, 0, 12);
        if (index >= 0 && index < 12) {
            c[index] = value;
        }
        return new Theme(name, c, majorFont, minorFont);
    }

    /// This theme with other fonts.
    ///
    /// #### Parameters
    ///
    /// - `major`: the title font
    ///
    /// - `minor`: the body font
    ///
    /// #### Returns
    ///
    /// the theme
    public Theme withFonts(String major, String minor) {
        return new Theme(name, colors, major, minor);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Theme)) {
            return false;
        }
        Theme t = (Theme) o;
        if (!name.equals(t.name) || !majorFont.equals(t.majorFont)
                || !minorFont.equals(t.minorFont)) {
            return false;
        }
        for (int i = 0; i < 12; i++) {
            if (colors[i] != t.colors[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = name.hashCode();
        for (int i = 0; i < 12; i++) {
            h = h * 31 + colors[i];
        }
        return h;
    }
}
