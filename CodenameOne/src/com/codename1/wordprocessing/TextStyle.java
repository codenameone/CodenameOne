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

/// Immutable character formatting for a run of text. Sizes are in points and colors are 24 bit
/// RGB values.
///
/// A style is changed by deriving a new one, never in place:
///
/// ```java
/// // com.codename1.ui.editor has a TextStyle too, hence the full name
/// com.codename1.wordprocessing.TextStyle heading =
///         com.codename1.wordprocessing.TextStyle.DEFAULT.withSize(18).withBold(true);
/// Log.p("heading at " + heading.size + "pt");
/// ```
public final class TextStyle {
    /// Marks the absence of a highlight color.
    public static final int NO_BACKGROUND = -1;

    /// Normal baseline position.
    public static final String BASELINE = "baseline";

    /// Raised and reduced, as in an exponent.
    public static final String SUPERSCRIPT = "superscript";

    /// Lowered and reduced, as in a chemical formula.
    public static final String SUBSCRIPT = "subscript";

    /// The style of text that names no formatting of its own.
    public static final TextStyle DEFAULT = new TextStyle("Serif", 12, 0x202833, false, false,
            false, false, NO_BACKGROUND, "", BASELINE);

    /// The font family name.
    public final String font;

    /// The hyperlink target, or the empty string.
    public final String link;

    /// The text color.
    public final int color;

    /// The highlight color, or `NO_BACKGROUND`.
    public final int background;

    /// The font size in points.
    public final float size;

    /// Whether the text is bold.
    public final boolean bold;

    /// Whether the text is italic.
    public final boolean italic;

    /// Whether the text is underlined.
    public final boolean underline;

    /// Whether the text is struck through.
    public final boolean strike;

    /// One of `BASELINE`, `SUPERSCRIPT` or `SUBSCRIPT`.
    public final String script;

    /// The tracked change this run belongs to, or null.
    public final Revision revision;

    /// Creates a style.
    ///
    /// #### Parameters
    ///
    /// - `font`: the font family
    ///
    /// - `size`: the size in points, greater than zero
    ///
    /// - `color`: the RGB text color
    ///
    /// - `bold`: bold text
    ///
    /// - `italic`: italic text
    ///
    /// - `underline`: underlined text
    ///
    /// - `strike`: struck-through text
    ///
    /// - `background`: the RGB highlight, or `NO_BACKGROUND`
    ///
    /// - `link`: the hyperlink target, or null or empty for none
    ///
    /// - `script`: `BASELINE`, `SUPERSCRIPT` or `SUBSCRIPT`; null means `BASELINE`
    public TextStyle(String font, float size, int color, boolean bold, boolean italic,
            boolean underline, boolean strike, int background, String link, String script) {
        this(font, size, color, bold, italic, underline, strike, background, link, script, null);
    }

    /// Creates a style with a tracked change.
    ///
    /// #### Parameters
    ///
    /// - `font`: the font family, or null for `Serif`
    ///
    /// - `size`: the size in points
    ///
    /// - `color`: the text color
    ///
    /// - `bold`: bold
    ///
    /// - `italic`: italic
    ///
    /// - `underline`: underline
    ///
    /// - `strike`: strikethrough
    ///
    /// - `background`: the highlight color, or `NO_BACKGROUND`
    ///
    /// - `link`: the hyperlink target, or null or empty for none
    ///
    /// - `script`: `BASELINE`, `SUPERSCRIPT` or `SUBSCRIPT`; null means `BASELINE`
    ///
    /// - `revision`: the tracked change the run belongs to, or null
    public TextStyle(String font, float size, int color, boolean bold, boolean italic,
            boolean underline, boolean strike, int background, String link, String script,
            Revision revision) {
        if (Float.isNaN(size) || size <= 0 || size > 1638) {
            throw new IllegalArgumentException("Invalid font size " + size);
        }
        this.font = font == null || font.length() == 0 ? "Serif" : font;
        this.size = size;
        this.color = color & 0xffffff;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strike = strike;
        this.background = background < 0 ? NO_BACKGROUND : background & 0xffffff;
        this.link = link == null ? "" : link;
        this.script = SUPERSCRIPT.equals(script) || SUBSCRIPT.equals(script) ? script : BASELINE;
        this.revision = revision;
    }

    /// A copy with a different font family.
    ///
    /// #### Parameters
    ///
    /// - `value`: the family
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withFont(String value) {
        return new TextStyle(value, size, color, bold, italic, underline, strike, background, link,
                script, revision);
    }

    /// A copy with a different size.
    ///
    /// #### Parameters
    ///
    /// - `value`: the size in points
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withSize(float value) {
        return new TextStyle(font, value, color, bold, italic, underline, strike, background, link,
                script, revision);
    }

    /// A copy with a different text color.
    ///
    /// #### Parameters
    ///
    /// - `value`: the RGB color
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withColor(int value) {
        return new TextStyle(font, size, value, bold, italic, underline, strike, background, link,
                script, revision);
    }

    /// A copy with a different highlight.
    ///
    /// #### Parameters
    ///
    /// - `value`: the RGB color, or `NO_BACKGROUND`
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withBackground(int value) {
        return new TextStyle(font, size, color, bold, italic, underline, strike, value, link,
                script, revision);
    }

    /// A copy with bold switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: bold or not
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withBold(boolean value) {
        return new TextStyle(font, size, color, value, italic, underline, strike, background, link,
                script, revision);
    }

    /// A copy with italic switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: italic or not
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withItalic(boolean value) {
        return new TextStyle(font, size, color, bold, value, underline, strike, background, link,
                script, revision);
    }

    /// A copy with underline switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: underlined or not
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withUnderline(boolean value) {
        return new TextStyle(font, size, color, bold, italic, value, strike, background, link,
                script, revision);
    }

    /// A copy with strike-through switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: struck through or not
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withStrike(boolean value) {
        return new TextStyle(font, size, color, bold, italic, underline, value, background, link,
                script, revision);
    }

    /// A copy with a different hyperlink.
    ///
    /// #### Parameters
    ///
    /// - `value`: the target, or null or empty for none
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withLink(String value) {
        return new TextStyle(font, size, color, bold, italic, underline, strike, background, value,
                script, revision);
    }

    /// A copy with a different baseline position.
    ///
    /// #### Parameters
    ///
    /// - `value`: `BASELINE`, `SUPERSCRIPT` or `SUBSCRIPT`
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withScript(String value) {
        return new TextStyle(font, size, color, bold, italic, underline, strike, background, link,
                value, revision);
    }

    /// A copy marked as a tracked change, or with the mark removed.
    ///
    /// #### Parameters
    ///
    /// - `value`: the revision, or null
    ///
    /// #### Returns
    ///
    /// the derived style
    public TextStyle withRevision(Revision value) {
        return new TextStyle(font, size, color, bold, italic, underline, strike, background, link,
                script, value);
    }

    /// Two styles are equal when every attribute is.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextStyle)) {
            return false;
        }
        TextStyle t = (TextStyle) o;
        return font.equals(t.font) && Float.floatToIntBits(size) == Float.floatToIntBits(t.size)
                && color == t.color && background == t.background && bold == t.bold
                && italic == t.italic && underline == t.underline && strike == t.strike
                && link.equals(t.link) && script.equals(t.script)
                && (revision == null ? t.revision == null : revision.equals(t.revision));
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        int h = font.hashCode();
        h = h * 31 + Float.floatToIntBits(size);
        h = h * 31 + color;
        h = h * 31 + background;
        h = h * 31 + (bold ? 1 : 0) + (italic ? 2 : 0) + (underline ? 4 : 0) + (strike ? 8 : 0);
        h = h * 31 + link.hashCode();
        h = h * 31 + script.hashCode();
        return h * 31 + (revision == null ? 0 : revision.hashCode());
    }
}
