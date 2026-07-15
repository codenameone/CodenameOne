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

package com.codename1.ui.editor;

/// An immutable inline text style (bold / italic / underline / strike, foreground color, highlight color
/// and relative font size) used by the pure rich text editor. Instances are value objects; the
/// `#with*` methods return a possibly new instance leaving the original untouched, which makes them safe
/// to share across runs.
public final class TextStyle {
    /// The default (unstyled) text style.
    public static final TextStyle DEFAULT = new TextStyle(false, false, false, false, false, -1, -1, 0);

    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strike;
    private final boolean monospace;
    private final int foreColor;
    private final int highlight;
    private final int fontSizeLevel;

    private TextStyle(boolean bold, boolean italic, boolean underline, boolean strike, boolean monospace,
                      int foreColor, int highlight, int fontSizeLevel) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strike = strike;
        this.monospace = monospace;
        this.foreColor = foreColor;
        this.highlight = highlight;
        this.fontSizeLevel = fontSizeLevel;
    }

    /// True when bold.
    public boolean isBold() {
        return bold;
    }

    /// True when italic.
    public boolean isItalic() {
        return italic;
    }

    /// True when underlined.
    public boolean isUnderline() {
        return underline;
    }

    /// True when struck through.
    public boolean isStrike() {
        return strike;
    }

    /// True when the run represents inline code / monospaced text.
    public boolean isMonospace() {
        return monospace;
    }

    /// The foreground color as 0xRRGGBB, or -1 to inherit.
    public int getForeColor() {
        return foreColor;
    }

    /// The highlight (background) color as 0xRRGGBB, or -1 for none.
    public int getHighlight() {
        return highlight;
    }

    /// The relative font size level (1..7), or 0 for the default size.
    public int getFontSizeLevel() {
        return fontSizeLevel;
    }

    /// Returns a style with bold set to the given value.
    public TextStyle withBold(boolean v) {
        return new TextStyle(v, italic, underline, strike, monospace, foreColor, highlight, fontSizeLevel);
    }

    /// Returns a style with italic set to the given value.
    public TextStyle withItalic(boolean v) {
        return new TextStyle(bold, v, underline, strike, monospace, foreColor, highlight, fontSizeLevel);
    }

    /// Returns a style with underline set to the given value.
    public TextStyle withUnderline(boolean v) {
        return new TextStyle(bold, italic, v, strike, monospace, foreColor, highlight, fontSizeLevel);
    }

    /// Returns a style with strike-through set to the given value.
    public TextStyle withStrike(boolean v) {
        return new TextStyle(bold, italic, underline, v, monospace, foreColor, highlight, fontSizeLevel);
    }

    /// Returns a style with inline-code / monospaced rendering enabled or disabled.
    public TextStyle withMonospace(boolean v) {
        return new TextStyle(bold, italic, underline, strike, v, foreColor, highlight, fontSizeLevel);
    }

    /// Returns a style with the given foreground color (or -1 to inherit).
    public TextStyle withForeColor(int v) {
        return new TextStyle(bold, italic, underline, strike, monospace, v, highlight, fontSizeLevel);
    }

    /// Returns a style with the given highlight color (or -1 for none).
    public TextStyle withHighlight(int v) {
        return new TextStyle(bold, italic, underline, strike, monospace, foreColor, v, fontSizeLevel);
    }

    /// Returns a style with the given font size level (1..7, or 0 for default).
    public TextStyle withFontSizeLevel(int v) {
        return new TextStyle(bold, italic, underline, strike, monospace, foreColor, highlight, v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextStyle)) {
            return false;
        }
        TextStyle t = (TextStyle) o;
        return bold == t.bold && italic == t.italic && underline == t.underline && strike == t.strike
                && monospace == t.monospace
                && foreColor == t.foreColor && highlight == t.highlight && fontSizeLevel == t.fontSizeLevel;
    }

    @Override
    public int hashCode() {
        int h = 0;
        h = h * 31 + (bold ? 1 : 0);
        h = h * 31 + (italic ? 1 : 0);
        h = h * 31 + (underline ? 1 : 0);
        h = h * 31 + (strike ? 1 : 0);
        h = h * 31 + (monospace ? 1 : 0);
        h = h * 31 + foreColor;
        h = h * 31 + highlight;
        h = h * 31 + fontSizeLevel;
        return h;
    }
}
