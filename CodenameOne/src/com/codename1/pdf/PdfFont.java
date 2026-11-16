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
package com.codename1.pdf;

import com.codename1.impl.pdf.FontState;
import com.codename1.impl.pdf.StandardFont;
import com.codename1.impl.pdf.TrueTypeFont;
import java.io.IOException;

/// A font text is drawn and measured with.
///
/// The 14 standard PDF fonts ([#HELVETICA], [#TIMES_ROMAN], [#COURIER] and
/// their styles, plus [#SYMBOL] and [#ZAPF_DINGBATS]) are built into every
/// reader and add nothing to the file, but they cover only the Western
/// European characters of WinAnsiEncoding; any other character is drawn as
/// `?`. For any other script, load a TrueType font with
/// [#loadTrueType(byte[])]: only the glyphs a document uses are embedded.
///
/// ```java
/// PdfFont body = PdfFont.loadTrueType(Util.readInputStream(
///         Display.getInstance().getResourceAsStream(getClass(), "/NotoSans-Regular.ttf")));
/// canvas.setFont(body, 12);
/// canvas.drawText("Grusse, Jalapeno", 72, 72);
/// ```
///
/// A font object holds no per-document state and may be shared by any
/// number of documents, including from different threads on a server.
@com.codename1.impl.SharedWithBackend
public abstract class PdfFont {
    /// Helvetica, the standard sans serif.
    public static final PdfFont HELVETICA = new StandardFont(0);
    /// Helvetica Bold.
    public static final PdfFont HELVETICA_BOLD = new StandardFont(1);
    /// Helvetica Oblique.
    public static final PdfFont HELVETICA_OBLIQUE = new StandardFont(2);
    /// Helvetica Bold Oblique.
    public static final PdfFont HELVETICA_BOLD_OBLIQUE = new StandardFont(3);
    /// Times Roman, the standard serif.
    public static final PdfFont TIMES_ROMAN = new StandardFont(4);
    /// Times Bold.
    public static final PdfFont TIMES_BOLD = new StandardFont(5);
    /// Times Italic.
    public static final PdfFont TIMES_ITALIC = new StandardFont(6);
    /// Times Bold Italic.
    public static final PdfFont TIMES_BOLD_ITALIC = new StandardFont(7);
    /// Courier, the standard monospaced font.
    public static final PdfFont COURIER = new StandardFont(8);
    /// Courier Bold.
    public static final PdfFont COURIER_BOLD = new StandardFont(9);
    /// Courier Oblique.
    public static final PdfFont COURIER_OBLIQUE = new StandardFont(10);
    /// Courier Bold Oblique.
    public static final PdfFont COURIER_BOLD_OBLIQUE = new StandardFont(11);
    /// Symbol: Greek letters and mathematical signs, by their code in the
    /// font's own encoding (a char `'a'` draws alpha).
    public static final PdfFont SYMBOL = new StandardFont(12);
    /// ZapfDingbats: ornaments and arrows, by their code in the font's own
    /// encoding.
    public static final PdfFont ZAPF_DINGBATS = new StandardFont(13);

    /// Subclassed only inside Codename One.
    protected PdfFont() {
    }

    /// Loads a TrueType (`.ttf`) font, or the TrueType outlines of an
    /// OpenType font. Fonts whose outlines are CFF (`.otf` with a `CFF `
    /// table) and collections (`.ttc`) are rejected with an
    /// [IOException], as is a font whose license bits forbid embedding.
    public static PdfFont loadTrueType(byte[] data) throws IOException {
        return new TrueTypeFont(data);
    }

    /// The PostScript name of the font.
    public abstract String getName();

    /// The advance width of `text` at `size` points, in points.
    public abstract float getStringWidth(String text, float size);

    /// The height above the baseline of the font's tallest letters at
    /// `size` points, in points.
    public abstract float getAscent(float size);

    /// The depth below the baseline at `size` points, as a positive number
    /// of points.
    public abstract float getDescent(float size);

    /// The recommended distance between two baselines at `size` points.
    public float getLineHeight(float size) {
        return (getAscent(size) + getDescent(size)) * 1.15f;
    }

    /// True when the font can draw `c` with a glyph of its own.
    public abstract boolean canDisplay(char c);

    /// Creates the per-document state that tracks which glyphs a document
    /// uses and writes the font objects. Internal.
    protected abstract FontState createState();
}
