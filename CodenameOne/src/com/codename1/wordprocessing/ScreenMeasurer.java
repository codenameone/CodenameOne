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
package com.codename1.wordprocessing;

import com.codename1.ui.Font;

import java.util.HashMap;
import java.util.Map;

/// Measures text with the platform fonts `DocumentView` draws it in.
///
/// The view's default. A layout measured with one font and drawn with another puts every
/// run where the first font would have ended the previous one, so wider glyphs run into the
/// next word. `PdfMeasurer` measures the PDF's fonts; use it on a view only when its pages
/// must break exactly where the exported PDF's do.
///
/// Widths are measured at [#PIXELS_PER_POINT] pixels per point and scaled back, so they do
/// not depend on the size the view happens to draw at.
public class ScreenMeasurer implements Measurer {
    /// The pixel size per point text is measured at.
    static final int PIXELS_PER_POINT = 4;

    private final Map<String, Font> fonts = new HashMap<String, Font>();

    /// The platform font a style is drawn in, at a pixel size.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// - `px`: the size in pixels
    ///
    /// #### Returns
    ///
    /// the font
    public Font font(TextStyle style, int px) {
        String key = style.font + (style.bold ? "|b" : "|") + (style.italic ? "i" : "") + px;
        Font f = fonts.get(key);
        if (f == null) {
            f = create(style, px);
            fonts.put(key, f);
        }
        return f;
    }

    private static Font create(TextStyle style, int px) {
        int s = (style.bold ? Font.STYLE_BOLD : 0) | (style.italic ? Font.STYLE_ITALIC : 0);
        String fam = style.font == null ? "" : Ids.asciiLower(style.font);
        boolean mono = fam.indexOf("mono") >= 0 || fam.indexOf("courier") >= 0;
        if (mono) {
            // the system monospace font comes in three sizes only: use the nearest, unless
            // even that is far off, where a proportional font of the right size reads better
            Font best = null;
            int[] sizes = {Font.SIZE_SMALL, Font.SIZE_MEDIUM, Font.SIZE_LARGE};
            for (int size : sizes) {
                Font c = Font.createSystemFont(Font.FACE_MONOSPACE, s, size);
                if (best == null || Math.abs(c.getHeight() - px) < Math.abs(best.getHeight() - px)) {
                    best = c;
                }
            }
            if (!Font.isNativeFontSchemeSupported() || Math.abs(best.getHeight() - px) * 4 <= px) {
                return best;
            }
        }
        if (Font.isNativeFontSchemeSupported()) {
            String name = style.italic ? style.bold ? "native:ItalicBold" : "native:ItalicRegular"
                    : style.bold ? "native:MainBold" : "native:MainRegular";
            return Font.createTrueTypeFont(name, name).derive(px, s);
        }
        return Font.createSystemFont(Font.FACE_SYSTEM, s, px < 12 ? Font.SIZE_SMALL
                : px > 20 ? Font.SIZE_LARGE : Font.SIZE_MEDIUM);
    }

    private Font measuring(TextStyle style, float points) {
        return font(style, Math.max(1, Math.round(points * PIXELS_PER_POINT)));
    }

    @Override
    public float width(String text, TextStyle style) {
        return measuring(style, PdfMeasurer.drawnSize(style)).stringWidth(text) / (float) PIXELS_PER_POINT;
    }

    @Override
    public float ascent(TextStyle style) {
        return measuring(style, style.size).getAscent() / (float) PIXELS_PER_POINT;
    }

    @Override
    public float descent(TextStyle style) {
        Font f = measuring(style, style.size);
        return (f.getHeight() - f.getAscent()) / (float) PIXELS_PER_POINT;
    }
}
