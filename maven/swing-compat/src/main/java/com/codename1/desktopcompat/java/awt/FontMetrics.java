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

import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;

/// The measurements of a font, in logical pixels.
///
/// This base class estimates from the font size alone; the metrics a
/// component or a graphics hands out measure the platform font.
public abstract class FontMetrics {

    protected Font font;

    protected FontMetrics(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public int getLeading() {
        return 0;
    }

    public int getAscent() {
        return font.getSize();
    }

    public int getDescent() {
        return 0;
    }

    public int getHeight() {
        return getLeading() + getAscent() + getDescent();
    }

    public int getMaxAscent() {
        return getAscent();
    }

    public int getMaxDescent() {
        return getDescent();
    }

    public int getMaxAdvance() {
        return -1;
    }

    public int charWidth(int codePoint) {
        return charWidth((char) codePoint);
    }

    public int charWidth(char ch) {
        return stringWidth(String.valueOf(ch));
    }

    public int stringWidth(String str) {
        return Math.round(str.length() * font.getSize2D() * 0.6f);
    }

    public int charsWidth(char[] data, int off, int len) {
        return stringWidth(new String(data, off, len));
    }

    public int[] getWidths() {
        int[] widths = new int[256];
        for (char ch = 0; ch < 256; ch++) {
            widths[ch] = charWidth(ch);
        }
        return widths;
    }

    public boolean hasUniformLineMetrics() {
        return true;
    }

    public Rectangle2D getStringBounds(String str, Graphics context) {
        return new Rectangle2D.Float(0, -getAscent(), stringWidth(str), getHeight());
    }

    public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
        return getStringBounds(str.substring(beginIndex, limit), context);
    }

    @Override
    public String toString() {
        return "FontMetrics[font=" + getFont() + "ascent=" + getAscent() + ", descent=" + getDescent()
                + ", height=" + getHeight() + "]";
    }
}
