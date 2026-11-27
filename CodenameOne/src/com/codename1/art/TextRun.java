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
package com.codename1.art;

/// A stretch of text with one style.
public final class TextRun {
    private String text;
    private String fontFamily;
    private float size;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private ArtPaint fill;
    private float letterSpacing;

    /// Creates a run.
    ///
    /// #### Parameters
    ///
    /// - `text`: the characters; `\n` breaks a line
    /// - `fontFamily`: the family name, or null for the default
    /// - `size`: the font size in the node's units
    /// - `fill`: the text color or paint
    public TextRun(String text, String fontFamily, float size, ArtPaint fill) {
        this.text = text == null ? "" : text;
        this.fontFamily = fontFamily;
        this.size = size;
        this.fill = fill;
    }

    /// A deep copy.
    public TextRun copy() {
        TextRun r = new TextRun(text, fontFamily, size, fill == null ? null : fill.copy());
        r.bold = bold;
        r.italic = italic;
        r.underline = underline;
        r.strikethrough = strikethrough;
        r.letterSpacing = letterSpacing;
        return r;
    }

    /// The characters.
    public String getText() {
        return text;
    }

    /// Sets the characters.
    ///
    /// #### Parameters
    ///
    /// - `text`: the characters
    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    /// The family name, or null.
    public String getFontFamily() {
        return fontFamily;
    }

    /// Sets the family.
    ///
    /// #### Parameters
    ///
    /// - `fontFamily`: the family name
    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    /// The size in the node's units.
    public float getSize() {
        return size;
    }

    /// Sets the size.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size
    public void setSize(float size) {
        this.size = size;
    }

    /// True for bold.
    public boolean isBold() {
        return bold;
    }

    /// Sets bold.
    ///
    /// #### Parameters
    ///
    /// - `bold`: true for bold
    public void setBold(boolean bold) {
        this.bold = bold;
    }

    /// True for italic.
    public boolean isItalic() {
        return italic;
    }

    /// Sets italic.
    ///
    /// #### Parameters
    ///
    /// - `italic`: true for italic
    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    /// True for underline.
    public boolean isUnderline() {
        return underline;
    }

    /// Sets underline.
    ///
    /// #### Parameters
    ///
    /// - `underline`: true to underline
    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    /// True for strikethrough.
    public boolean isStrikethrough() {
        return strikethrough;
    }

    /// Sets strikethrough.
    ///
    /// #### Parameters
    ///
    /// - `strikethrough`: true to strike through
    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    /// The text paint.
    public ArtPaint getFill() {
        return fill;
    }

    /// Sets the text paint.
    ///
    /// #### Parameters
    ///
    /// - `fill`: the paint
    public void setFill(ArtPaint fill) {
        this.fill = fill;
    }

    /// Extra space between characters, in the node's units.
    public float getLetterSpacing() {
        return letterSpacing;
    }

    /// Sets the letter spacing.
    ///
    /// #### Parameters
    ///
    /// - `letterSpacing`: extra space between characters
    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
    }
}
