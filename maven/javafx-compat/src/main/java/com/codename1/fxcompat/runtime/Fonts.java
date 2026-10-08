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
package com.codename1.fxcompat.runtime;

import java.util.HashMap;

import com.codename1.ui.Font;

/// Turns a JavaFX font description into the Codename One font that draws
/// it, and measures text with it in logical pixels.
///
/// A font size is converted to device pixels here rather than scaled by a
/// transform when drawing, so glyphs are rasterised at their real size.
/// Fonts are cached per description and scale.
public final class Fonts {

    private static final HashMap<String, Font> CACHE = new HashMap<String, Font>();
    private static double cacheScale;

    private Fonts() {
    }

    private static boolean isAsciiName(String family, String expected) {
        return family != null && family.equalsIgnoreCase(expected);
    }

    /// Returns the native font of a family, a weight from 100 to 900, a
    /// posture and a size in logical pixels.
    public static Font create(String family, int weight, boolean italic, double size) {
        double scale = Units.scale();
        if (scale != cacheScale) {
            CACHE.clear();
            cacheScale = scale;
        }
        String key = family + '|' + weight + '|' + italic + '|' + size;
        Font cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        float pixels = (float) Math.max(1, size * scale);
        Font font = null;
        int style = (weight >= 600 ? Font.STYLE_BOLD : Font.STYLE_PLAIN) | (italic ? Font.STYLE_ITALIC : 0);
        if (Font.isNativeFontSchemeSupported() || Font.isTrueTypeFileSupported()) {
            String face;
            if (isAsciiName(family, "Monospaced") || isAsciiName(family, "Monospace")
                    || isAsciiName(family, "Courier New")) {
                face = null;
            } else if (weight >= 800) {
                face = italic ? "native:ItalicBlack" : "native:MainBlack";
            } else if (weight >= 600) {
                face = italic ? "native:ItalicBold" : "native:MainBold";
            } else if (weight <= 200) {
                face = italic ? "native:ItalicThin" : "native:MainThin";
            } else if (weight <= 300) {
                face = italic ? "native:ItalicLight" : "native:MainLight";
            } else {
                face = italic ? "native:ItalicRegular" : "native:MainRegular";
            }
            if (face != null) {
                try {
                    Font base = Font.createTrueTypeFont(face, face);
                    if (base != null) {
                        font = base.derive(pixels, Font.STYLE_PLAIN);
                    }
                } catch (RuntimeException unavailable) {
                    font = null;
                }
            }
        }
        if (font == null) {
            int face = isAsciiName(family, "Monospaced") || isAsciiName(family, "Monospace")
                    || isAsciiName(family, "Courier New") ? Font.FACE_MONOSPACE : Font.FACE_SYSTEM;
            int sizeClass = pixels < 14 * scale ? Font.SIZE_SMALL
                    : (pixels > 20 * scale ? Font.SIZE_LARGE : Font.SIZE_MEDIUM);
            font = Font.createSystemFont(face, style, sizeClass);
        }
        CACHE.put(key, font);
        return font;
    }

    /// Returns whether a native font came from the cache of the current
    /// scale, so it is still the right size.
    public static boolean isCurrent(Object nativeFont) {
        return cacheScale == Units.scale() && nativeFont instanceof Font && CACHE.containsValue(nativeFont);
    }

    /// Returns the native font of a JavaFX font, the default font for
    /// `null`.
    public static Font of(javafx.scene.text.Font font) {
        javafx.scene.text.Font f = font == null ? javafx.scene.text.Font.getDefault() : font;
        Object n = f.cn1Native();
        return n instanceof Font ? (Font) n : Font.getDefaultFont();
    }

    /// Returns the width of a text in logical pixels.
    public static double width(javafx.scene.text.Font font, String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        return Units.toLogical(of(font).stringWidth(text));
    }

    /// Returns the height of a line in logical pixels.
    public static double lineHeight(javafx.scene.text.Font font) {
        return Units.toLogical(of(font).getHeight());
    }

    /// Returns the distance from the top of a line to its baseline in
    /// logical pixels.
    public static double ascent(javafx.scene.text.Font font) {
        Font f = of(font);
        int ascent = f.getAscent();
        if (ascent <= 0) {
            ascent = f.getHeight() - Math.max(0, f.getDescent());
        }
        return Units.toLogical(ascent);
    }
}
