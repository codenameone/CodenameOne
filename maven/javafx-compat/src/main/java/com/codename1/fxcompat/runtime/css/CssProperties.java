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
package com.codename1.fxcompat.runtime.css;

import java.util.HashMap;

/// The style properties the scene graph takes, and what kind of value each
/// one is written as. It is the list in the description of
/// `com.codename1.fxcompat.runtime.StyleTarget`, as data: the parser reads
/// it to know how to read a value, and the engine to know what object to
/// make of one.
///
/// A property that is not in the table is not an error. JavaFX lets a rule
/// define any name as a colour and other rules use it (`-fx-base`,
/// `-my-accent`), so an unknown name whose value reads as a paint is such a
/// definition, [#LOOKUP_DEFINITION]; only an unknown name with any other
/// value is reported and dropped.
public final class CssProperties {

    /// Not a property of the scene graph.
    public static final int UNKNOWN = 0;
    /// A colour or a gradient.
    public static final int PAINT = 1;
    /// One length.
    public static final int LENGTH = 2;
    /// A number without a unit; an angle in degrees for `-fx-rotate`.
    public static final int NUMBER = 3;
    /// One to four lengths: top, right, bottom, left.
    public static final int INSETS = 4;
    /// One to four radii, clockwise from the top left.
    public static final int RADII = 5;
    /// `true` or `false`.
    public static final int BOOLEAN = 6;
    /// One of a fixed set of words.
    public static final int KEYWORD = 7;
    /// One quoted string, taken as it is: the path of `-fx-shape`.
    public static final int TEXT = 16;
    /// The `-fx-font` shorthand.
    public static final int FONT = 8;
    /// A font size: a length, a percentage or a size keyword.
    public static final int FONT_SIZE = 9;
    /// A font family name.
    public static final int FONT_FAMILY = 10;
    /// A font weight: a keyword or 100 to 900.
    public static final int FONT_WEIGHT = 11;
    /// A font posture keyword.
    public static final int FONT_STYLE = 12;
    /// An alignment keyword such as `center-left`.
    public static final int POS = 13;
    /// A cursor name.
    public static final int CURSOR = 14;
    /// What an unknown name with a paint for a value is: a colour other
    /// rules look up by that name.
    public static final int LOOKUP_DEFINITION = 15;
    /// `-fx-effect`: `dropshadow(...)`, `innershadow(...)` or `none`.
    public static final int EFFECT = 17;

    private static final HashMap<String, Integer> KINDS = new HashMap<String, Integer>();
    private static final HashMap<String, String[]> KEYWORDS = new HashMap<String, String[]>();

    static {
        add(PAINT, "-fx-background-color", "-fx-border-color", "-fx-text-fill", "-fx-prompt-text-fill", "-fx-fill",
                "-fx-stroke");
        add(LENGTH, "-fx-min-width", "-fx-pref-width", "-fx-max-width", "-fx-min-height", "-fx-pref-height",
                "-fx-max-height", "-fx-spacing", "-fx-hgap", "-fx-vgap", "-fx-graphic-text-gap", "-fx-stroke-width",
                "-fx-stroke-dash-offset", "-fx-line-spacing", "-fx-translate-x", "-fx-translate-y", "-fx-size");
        add(NUMBER, "-fx-opacity", "-fx-rotate", "-fx-scale-x", "-fx-scale-y", "-fx-stroke-miter-limit");
        add(INSETS, "-fx-padding", "-fx-background-insets", "-fx-border-width", "-fx-border-insets");
        add(RADII, "-fx-background-radius", "-fx-border-radius");
        add(BOOLEAN, "-fx-managed", "-fx-snap-to-pixel", "-fx-fill-height", "-fx-fill-width", "-fx-wrap-text",
                "-fx-underline", "-fx-strikethrough", "-fx-fit-to-width", "-fx-fit-to-height", "-fx-pannable",
                "-fx-smooth", "-fx-scale-shape", "-fx-position-shape");
        add(TEXT, "-fx-shape");
        add(FONT, "-fx-font");
        add(FONT_SIZE, "-fx-font-size");
        add(FONT_FAMILY, "-fx-font-family");
        add(FONT_WEIGHT, "-fx-font-weight");
        add(FONT_STYLE, "-fx-font-style");
        add(POS, "-fx-alignment");
        add(CURSOR, "-fx-cursor");
        add(EFFECT, "-fx-effect");
        keyword("visibility", "visible", "hidden", "collapse");
        keyword("-fx-border-style", "none", "solid", "dashed", "dotted");
        keyword("-fx-orientation", "horizontal", "vertical");
        keyword("-fx-text-alignment", "left", "center", "right", "justify");
        keyword("-fx-content-display", "top", "right", "bottom", "left", "center", "graphic-only", "text-only");
        keyword("-fx-hbar-policy", "never", "always", "as-needed");
        keyword("-fx-vbar-policy", "never", "always", "as-needed");
        keyword("-fx-stroke-line-cap", "square", "butt", "round");
        keyword("-fx-stroke-line-join", "miter", "bevel", "round");
        keyword("-fx-stroke-type", "inside", "outside", "centered");
        keyword("-fx-text-origin", "baseline", "top", "center", "bottom");
    }

    private CssProperties() {
    }

    /// The colours the standard JavaFX themes define and look up, which a
    /// style sheet redefines to recolour an application. They are
    /// definitions like any `-name: colour`, and the only names starting
    /// with `-fx-` that are.
    private static final String[] THEME_COLOURS = {
        "-fx-base", "-fx-background", "-fx-control-inner-background", "-fx-control-inner-background-alt",
        "-fx-accent", "-fx-default-button", "-fx-focus-color", "-fx-faint-focus-color", "-fx-color",
        "-fx-text-base-color", "-fx-text-background-color", "-fx-text-inner-color", "-fx-dark-text-color",
        "-fx-mid-text-color", "-fx-light-text-color", "-fx-body-color", "-fx-outer-border", "-fx-inner-border",
        "-fx-inner-border-horizontal", "-fx-inner-border-bottomup", "-fx-shadow-highlight-color",
        "-fx-box-border", "-fx-text-box-border", "-fx-hover-base", "-fx-pressed-base", "-fx-selection-bar",
        "-fx-selection-bar-non-focused", "-fx-selection-bar-text", "-fx-cell-hover-color", "-fx-mark-color",
        "-fx-mark-highlight-color", "-fx-cell-focus-inner-border", "-fx-focused-text-base-color",
        "-fx-focused-mark-color", "-fx-table-cell-border-color", "-fx-table-header-border-color",
        "-fx-progress-color",
    };

    /// Whether a name that is not a property may define a looked-up
    /// colour: it starts with `-` and is not a misspelt or unsupported
    /// `-fx-` property.
    public static boolean isDefinitionName(String property) {
        if (property.length() < 2 || property.charAt(0) != '-') {
            return false;
        }
        if (!property.startsWith("-fx-")) {
            return true;
        }
        for (String known : THEME_COLOURS) {
            if (known.equals(property)) {
                return true;
            }
        }
        return false;
    }

    private static void add(int kind, String... names) {
        for (int i = 0; i < names.length; i++) {
            KINDS.put(names[i], Integer.valueOf(kind));
        }
    }

    private static void keyword(String name, String... words) {
        KINDS.put(name, Integer.valueOf(KEYWORD));
        KEYWORDS.put(name, words);
    }

    /// The kind of value `property` takes, or [#UNKNOWN]. The name is in
    /// lower case, as a style sheet's names are once read.
    public static int kind(String property) {
        Integer k = KINDS.get(property);
        return k == null ? UNKNOWN : k.intValue();
    }

    /// Whether `word`, in lower case, is one `property` accepts. Only for a
    /// property of the kind [#KEYWORD].
    public static boolean accepts(String property, String word) {
        String[] words = KEYWORDS.get(property);
        if (words == null) {
            return false;
        }
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(word)) {
                return true;
            }
        }
        return false;
    }

    /// Whether a value of `property` is written in layers, separated by
    /// commas: the fills of a background and the strokes of a border, each
    /// with its own colour, insets, radii, widths and style.
    public static boolean isLayered(String property) {
        return "-fx-background-color".equals(property) || "-fx-background-insets".equals(property)
                || "-fx-background-radius".equals(property) || "-fx-border-color".equals(property)
                || "-fx-border-width".equals(property) || "-fx-border-radius".equals(property)
                || "-fx-border-insets".equals(property) || "-fx-border-style".equals(property);
    }

    /// Whether `property` is one of the five font names, which a node takes
    /// together: a child inherits them, and withdrawing one restores the
    /// whole font.
    public static boolean isFont(String property) {
        return property.startsWith("-fx-font");
    }

    /// `s` with the ASCII capitals folded to lower case, and nothing else
    /// touched. A style sheet's names and keywords are ASCII by
    /// specification, and the locale's own folding would turn an `I` into
    /// a dotless one on a device set to Turkish.
    public static String lower(String s) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                char[] out = s.toCharArray();
                for (int j = i; j < n; j++) {
                    char d = out[j];
                    if (d >= 'A' && d <= 'Z') {
                        out[j] = (char) (d + ('a' - 'A'));
                    }
                }
                return new String(out);
            }
        }
        return s;
    }
}
