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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.ui.Display;

import java.util.HashMap;

/// Maps AWT fonts onto the platform's fonts and measures them.
public final class Fonts {

    private static final HashMap<Integer, com.codename1.ui.Font> CACHE = new HashMap<Integer, com.codename1.ui.Font>();

    private Fonts() {
    }

    /// The Codename One font that draws `font` at `devicePixels` high.
    public static com.codename1.ui.Font nativeFont(Font font, float devicePixels) {
        int quarter = Math.max(4, Math.round(devicePixels * 4));
        boolean mono = monospaced(font);
        Integer key = Integer.valueOf((quarter * 4 + (font.getStyle() & 3)) * 2 + (mono ? 1 : 0));
        com.codename1.ui.Font f = CACHE.get(key);
        if (f != null) {
            return f;
        }
        if (mono) {
            f = fixedWidth(font, quarter / 4f);
        } else if (com.codename1.ui.Font.isNativeFontSchemeSupported()) {
            String face;
            if (font.isBold()) {
                face = font.isItalic() ? "native:ItalicBold" : "native:MainBold";
            } else {
                face = font.isItalic() ? "native:ItalicRegular" : "native:MainRegular";
            }
            f = com.codename1.ui.Font.createTrueTypeFont(face, face)
                    .derive(quarter / 4f, com.codename1.ui.Font.STYLE_PLAIN);
        } else {
            int style = (font.isBold() ? com.codename1.ui.Font.STYLE_BOLD : 0)
                    | (font.isItalic() ? com.codename1.ui.Font.STYLE_ITALIC : 0);
            f = com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, style,
                    com.codename1.ui.Font.SIZE_MEDIUM);
        }
        CACHE.put(key, f);
        return f;
    }

    /// Whether `font` asks for a family whose characters are all one
    /// width: text set in one is lined up in columns, which no other
    /// family can stand in for.
    static boolean monospaced(Font font) {
        String family = font.getFamily();
        return family != null && (family.equalsIgnoreCase(Font.MONOSPACED) || family.equalsIgnoreCase("Courier")
                || family.equalsIgnoreCase("Courier New"));
    }

    /// The platform's fixed width font. It comes in three sizes and no
    /// more, so this is the one nearest in height to what was asked for.
    private static com.codename1.ui.Font fixedWidth(Font font, float devicePixels) {
        int style = (font.isBold() ? com.codename1.ui.Font.STYLE_BOLD : 0)
                | (font.isItalic() ? com.codename1.ui.Font.STYLE_ITALIC : 0);
        int[] sizes = {com.codename1.ui.Font.SIZE_SMALL, com.codename1.ui.Font.SIZE_MEDIUM,
            com.codename1.ui.Font.SIZE_LARGE};
        // A font is about a fifth taller than its size says.
        float want = devicePixels * 1.2f;
        com.codename1.ui.Font best = null;
        float off = 0;
        for (int i = 0; i < sizes.length; i++) {
            com.codename1.ui.Font c = com.codename1.ui.Font.createSystemFont(
                    com.codename1.ui.Font.FACE_MONOSPACE, style, sizes[i]);
            float d = Math.abs(c.getHeight() - want);
            if (best == null || d < off) {
                best = c;
                off = d;
            }
        }
        return best;
    }

    /// The AWT font nearest a Codename One one: its height in logical pixels.
    public static Font fromNative(com.codename1.ui.Font f) {
        float px = f.getPixelSize();
        if (px <= 0) {
            px = f.getHeight() * 0.8f;
        }
        return new Font(Font.DIALOG, Font.PLAIN, Math.max(1, Math.round(px / Units.scale())));
    }

    /// The font of a component that was given none: the theme's label font,
    /// or 12 pixel Dialog before a display exists.
    public static Font defaultFont() {
        if (!Display.isInitialized()) {
            return new Font(Font.DIALOG, Font.PLAIN, 12);
        }
        return fromNative(com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle("Label").getFont());
    }

    public static FontMetrics metrics(Font font) {
        return new Metrics(font);
    }

    /// Measures the platform font at the current scale and answers in
    /// logical pixels, rounding widths to the nearest and heights up.
    private static final class Metrics extends FontMetrics {

        private final com.codename1.ui.Font cn1;
        private final float scale;

        Metrics(Font font) {
            super(font);
            scale = Units.scale();
            cn1 = Display.isInitialized() ? nativeFont(font, font.getSize2D() * scale) : null;
        }

        @Override
        public int getAscent() {
            if (cn1 == null) {
                return Math.round(font.getSize2D());
            }
            int a = cn1.getAscent();
            return Math.round((a > 0 ? a : cn1.getHeight() * 0.8f) / scale);
        }

        @Override
        public int getDescent() {
            return getHeight() - getAscent();
        }

        @Override
        public int getHeight() {
            if (cn1 == null) {
                return Math.round(font.getSize2D() * 1.25f);
            }
            return (int) Math.ceil(cn1.getHeight() / scale - 0.001f);
        }

        @Override
        public int getMaxAdvance() {
            return charWidth('W');
        }

        @Override
        public int stringWidth(String str) {
            if (cn1 == null) {
                return super.stringWidth(str);
            }
            return Math.round(cn1.stringWidth(str) / scale);
        }

        @Override
        public int charWidth(char ch) {
            if (cn1 == null) {
                return super.charWidth(ch);
            }
            return Math.round(cn1.charWidth(ch) / scale);
        }
    }
}
