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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

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
        if (Double.compare(scale, cacheScale) != 0) {
            CACHE.clear();
            OVERHANG.clear();
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
        // A font file the application loaded answers to its names before
        // any face of the platform does.
        FontFiles.Face loaded = FontFiles.find(family, weight, italic);
        if (loaded != null) {
            Font base = loaded.base();
            if (base != null) {
                try {
                    font = base.derive(pixels, Font.STYLE_PLAIN);
                } catch (RuntimeException unavailable) {
                    font = null;
                }
            }
        }
        if (font == null && (Font.isNativeFontSchemeSupported() || Font.isTrueTypeFileSupported())) {
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

    /// Forgets every cached font, so a family is looked up again: a font
    /// file loaded after a text was first drawn in its family takes over.
    public static void flush() {
        CACHE.clear();
        OVERHANG.clear();
    }

    private static final HashMap<Font, HashMap<Character, Integer>> OVERHANG =
            new HashMap<Font, HashMap<Character, Integer>>();

    /// Returns how far the ink of the last character of a text reaches
    /// past the width the text measures, in device pixels; 0 for a font
    /// that is not italic.
    ///
    /// The width of a text is the sum of the advances of its characters,
    /// and an italic leans past its own: the top of an `l` or a `d` is
    /// drawn to the right of where the next character would start. A
    /// native label clips its text to the room it measured, so a label
    /// exactly as wide as its text lost the top of such a last letter. A
    /// font does not say how far its glyphs lean, so the character is
    /// drawn once and looked at; the answer is kept per font and
    /// character.
    public static int overhang(javafx.scene.text.Font font, String text) {
        if (font == null || text == null || font.cn1Posture() != javafx.scene.text.FontPosture.ITALIC) {
            return 0;
        }
        int end = text.length();
        if (end == 0 || text.charAt(end - 1) <= ' ') {
            return 0;
        }
        Font f = of(font);
        Character last = Character.valueOf(text.charAt(end - 1));
        HashMap<Character, Integer> known = OVERHANG.get(f);
        if (known == null) {
            known = new HashMap<Character, Integer>();
            OVERHANG.put(f, known);
        }
        Integer kept = known.get(last);
        if (kept == null) {
            kept = Integer.valueOf(inkPastAdvance(f, last.charValue()));
            known.put(last, kept);
        }
        return kept.intValue();
    }

    /// Draws a character in white on black and answers how many columns
    /// right of its advance hold any of it. A glyph leans by less than it
    /// is tall, which is the room it is given.
    private static int inkPastAdvance(Font f, char c) {
        int advance = f.charWidth(c);
        int height = f.getHeight();
        if (advance <= 0 || height <= 0) {
            return 0;
        }
        int width = advance + height;
        Image picture = Image.createImage(width, height, 0xff000000);
        Graphics g = picture.getGraphics();
        g.setClip(0, 0, width, height);
        g.setFont(f);
        g.setColor(0xffffff);
        g.drawString(String.valueOf(c), 0, 0);
        int[] rgb = picture.getRGB();
        if (rgb == null || rgb.length < width * height) {
            return 0;
        }
        for (int x = width - 1; x >= advance; x--) {
            for (int y = 0; y < height; y++) {
                // The faintest edge of an antialiased stroke is not worth
                // a column of its own.
                if ((rgb[y * width + x] & 0xff) > 0x30) {
                    return x + 1 - advance;
                }
            }
        }
        return 0;
    }

    /// Returns whether a native font came from the cache of the current
    /// scale, so it is still the right size.
    public static boolean isCurrent(Object nativeFont) {
        return Double.compare(cacheScale, Units.scale()) == 0 && nativeFont instanceof Font && CACHE.containsValue(nativeFont);
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
