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
package com.codename1.desktopcompat.java.awt;

/// A typeface, a style and a size in logical pixels.
///
/// The family is a request, not a file: every font is drawn with the
/// platform's own typeface in the asked weight and slant, so a 12 pixel
/// bold "Arial" is the device's 12 pixel bold font. Sizes follow the
/// desktop convention of one point per pixel.
public class Font {

    public static final String DIALOG = "Dialog";
    public static final String DIALOG_INPUT = "DialogInput";
    public static final String SANS_SERIF = "SansSerif";
    public static final String SERIF = "Serif";
    public static final String MONOSPACED = "Monospaced";
    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int ROMAN_BASELINE = 0;
    public static final int CENTER_BASELINE = 1;
    public static final int HANGING_BASELINE = 2;
    public static final int TRUETYPE_FONT = 0;
    public static final int TYPE1_FONT = 1;

    protected String name;
    protected int style;
    protected int size;
    protected float pointSize;

    public Font(String name, int style, int size) {
        this.name = name != null ? name : "Default";
        this.style = (style & ~0x03) == 0 ? style : 0;
        this.size = size;
        this.pointSize = size;
    }

    protected Font(Font font) {
        this.name = font.name;
        this.style = font.style;
        this.size = font.size;
        this.pointSize = font.pointSize;
    }

    private Font(String name, int style, float sizePts) {
        this.name = name;
        this.style = (style & ~0x03) == 0 ? style : 0;
        this.size = (int) (sizePts + 0.5);
        this.pointSize = sizePts;
    }

    public String getFamily() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getFontName() {
        return name;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public float getSize2D() {
        return pointSize;
    }

    public boolean isPlain() {
        return style == 0;
    }

    public boolean isBold() {
        return (style & BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public Font deriveFont(int style, float size) {
        return new Font(name, style, size);
    }

    public Font deriveFont(float size) {
        return new Font(name, style, size);
    }

    public Font deriveFont(int style) {
        return new Font(name, style, pointSize);
    }

    /// Whether the platform font can draw the character; the layer cannot
    /// ask, so it answers true.
    public boolean canDisplay(char c) {
        return true;
    }

    /// Parses `family-style-size`, `family style size` and their shorter
    /// forms; a missing style is plain and a missing size is 12.
    public static Font decode(String str) {
        if (str == null) {
            return new Font(DIALOG, PLAIN, 12);
        }
        String s = str.trim().replace('-', ' ');
        String fontName = s;
        int fontStyle = PLAIN;
        int fontSize = 12;
        int last = s.lastIndexOf(' ');
        if (last > 0) {
            String tail = s.substring(last + 1);
            try {
                fontSize = Integer.parseInt(tail);
                s = s.substring(0, last).trim();
                fontName = s;
                last = s.lastIndexOf(' ');
            } catch (NumberFormatException e) {
                fontSize = 12;
            }
        }
        if (last > 0) {
            String tail = s.substring(last + 1);
            int parsed = -1;
            if (tail.equalsIgnoreCase("bold")) {
                parsed = BOLD;
            } else if (tail.equalsIgnoreCase("italic")) {
                parsed = ITALIC;
            } else if (tail.equalsIgnoreCase("bolditalic")) {
                parsed = BOLD | ITALIC;
            } else if (tail.equalsIgnoreCase("plain")) {
                parsed = PLAIN;
            }
            if (parsed >= 0) {
                fontStyle = parsed;
                fontName = s.substring(0, last).trim();
            }
        }
        return new Font(fontName.length() == 0 ? DIALOG : fontName, fontStyle, fontSize);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ style ^ Float.floatToIntBits(pointSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Font)) {
            return false;
        }
        Font font = (Font) obj;
        return size == font.size && style == font.style
                && Float.floatToIntBits(pointSize) == Float.floatToIntBits(font.pointSize) && name.equals(font.name);
    }

    @Override
    public String toString() {
        String strStyle = isBold() ? (isItalic() ? "bolditalic" : "bold") : (isItalic() ? "italic" : "plain");
        return "java.awt.Font[family=" + getFamily() + ",name=" + name + ",style=" + strStyle + ",size=" + size + "]";
    }
}
