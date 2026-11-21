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

import com.codename1.pdf.PdfFont;

import java.util.HashMap;
import java.util.Map;

/// Measures with the fonts a PDF is drawn in, so a document paginates identically on screen and
/// on paper.
///
/// Out of the box every style maps to one of the standard PDF fonts: serif families to Times,
/// monospaced ones to Courier and everything else to Helvetica. Those cover only Western
/// European characters, so a document in any other script should register a TrueType font for
/// each family it uses:
///
/// ```java
/// PdfMeasurer m = new PdfMeasurer();
/// m.register("Serif", false, false, PdfFont.loadTrueType(regularBytes));
/// m.register("Serif", true, false, PdfFont.loadTrueType(boldBytes));
/// DocumentLayout layout = new DocumentLayout(doc, m);
/// ```
public final class PdfMeasurer implements Measurer {
    private final Map<String, PdfFont> fonts = new HashMap<String, PdfFont>();

    /// Registers a font for a family and face. A face without its own registration falls back
    /// to the family's regular font, and a family without any to a standard font.
    ///
    /// #### Parameters
    ///
    /// - `family`: the family name a `TextStyle` uses, such as `Serif` or `Calibri`
    ///
    /// - `bold`: whether this is the bold face
    ///
    /// - `italic`: whether this is the italic face
    ///
    /// - `font`: the font
    public void register(String family, boolean bold, boolean italic, PdfFont font) {
        if (family == null || font == null) {
            throw new IllegalArgumentException("Family and font are required");
        }
        fonts.put(key(family, bold, italic), font);
    }

    private static String key(String family, boolean bold, boolean italic) {
        return Ids.asciiLower(family) + (bold ? "|b" : "|") + (italic ? "i" : "");
    }

    /// The font a style is drawn in.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// #### Returns
    ///
    /// the font, never null
    public PdfFont font(TextStyle style) {
        String family = style.font == null ? "Serif" : style.font;
        PdfFont f = fonts.get(key(family, style.bold, style.italic));
        if (f == null) {
            f = fonts.get(key(family, false, false));
        }
        return f != null ? f : standard(family, style.bold, style.italic);
    }

    /// The standard PDF font closest to a family.
    static PdfFont standard(String family, boolean bold, boolean italic) {
        String f = Ids.asciiLower(family);
        if (f.indexOf("mono") >= 0 || f.indexOf("courier") >= 0 || f.indexOf("consol") >= 0
                || f.indexOf("menlo") >= 0 || f.indexOf("code") >= 0) {
            return bold ? italic ? PdfFont.COURIER_BOLD_OBLIQUE : PdfFont.COURIER_BOLD
                    : italic ? PdfFont.COURIER_OBLIQUE : PdfFont.COURIER;
        }
        boolean serif = f.indexOf("sans") < 0 && (f.indexOf("serif") >= 0
                || f.indexOf("times") >= 0 || f.indexOf("georgia") >= 0
                || f.indexOf("cambria") >= 0 || f.indexOf("garamond") >= 0
                || f.indexOf("roman") >= 0 || f.indexOf("book") >= 0);
        if (serif) {
            return bold ? italic ? PdfFont.TIMES_BOLD_ITALIC : PdfFont.TIMES_BOLD
                    : italic ? PdfFont.TIMES_ITALIC : PdfFont.TIMES_ROMAN;
        }
        return bold ? italic ? PdfFont.HELVETICA_BOLD_OBLIQUE : PdfFont.HELVETICA_BOLD
                : italic ? PdfFont.HELVETICA_OBLIQUE : PdfFont.HELVETICA;
    }

    /// The size text of a style is drawn at: smaller for superscript and subscript.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// #### Returns
    ///
    /// the size in points
    public static float drawnSize(TextStyle style) {
        return TextStyle.BASELINE.equals(style.script) ? style.size : style.size * 0.65f;
    }

    @Override
    public float width(String text, TextStyle style) {
        return font(style).getStringWidth(text, drawnSize(style));
    }

    @Override
    public float ascent(TextStyle style) {
        return font(style).getAscent(style.size);
    }

    @Override
    public float descent(TextStyle style) {
        return Math.abs(font(style).getDescent(style.size));
    }
}
