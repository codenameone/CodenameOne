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
package com.codename1.impl.office;

import com.codename1.wordprocessing.TextStyle;

/// OpenDocument character formatting shared by the ODT and ODP writers: the
/// `style:text-properties` element of a run style, so both formats map fonts, sizes and
/// decorations the same way.
public final class OdfText {
    private OdfText() {
    }

    /// Appends the `style:text-properties` element for a run style.
    ///
    /// #### Parameters
    ///
    /// - `b`: the buffer to append to
    /// - `s`: the run formatting
    public static void textProperties(StringBuilder b, TextStyle s) {
        b.append("<style:text-properties fo:font-family=\"");
        String font = s.font;
        if (font == null || font.length() == 0 || "SansSerif".equals(font)) {
            font = "Arial";
        } else if ("Serif".equals(font)) {
            font = "Times New Roman";
        } else if ("Monospace".equals(font)) {
            font = "Courier New";
        }
        escape(b, font.indexOf(' ') >= 0 ? "'" + font + "'" : font);
        b.append("\" fo:font-size=\"").append(length(s.size)).append("\" fo:color=\"#")
                .append(hex(s.color)).append("\" fo:font-weight=\"")
                .append(s.bold ? "bold" : "normal").append("\" fo:font-style=\"")
                .append(s.italic ? "italic" : "normal").append('"');
        if (s.underline) {
            b.append(" style:text-underline-style=\"solid\" style:text-underline-width=\"auto\"")
                    .append(" style:text-underline-color=\"font-color\"");
        }
        if (s.strike) {
            b.append(" style:text-line-through-style=\"solid\"");
        }
        if (s.background != TextStyle.NO_BACKGROUND) {
            b.append(" fo:background-color=\"#").append(hex(s.background)).append('"');
        }
        if (TextStyle.SUPERSCRIPT.equals(s.script)) {
            b.append(" style:text-position=\"super 58%\"");
        } else if (TextStyle.SUBSCRIPT.equals(s.script)) {
            b.append(" style:text-position=\"sub 58%\"");
        }
        b.append("/>");
    }

    /// A length in points as ODF writes it, such as `12pt` or `10.5pt`.
    ///
    /// #### Parameters
    ///
    /// - `points`: the length
    ///
    /// #### Returns
    ///
    /// the attribute value
    public static String length(float points) {
        if (points == (int) points) {
            return String.valueOf((int) points) + "pt";
        }
        return String.valueOf(points) + "pt";
    }

    /// Six lowercase hex digits of an RGB color, without the `#`.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the color; the alpha byte is ignored
    ///
    /// #### Returns
    ///
    /// the digits
    public static String hex(int rgb) {
        return Integer.toHexString(0x1000000 | rgb & 0xffffff).substring(1);
    }

    /// Appends text escaped for an XML attribute value or element content.
    ///
    /// #### Parameters
    ///
    /// - `b`: the buffer to append to
    /// - `s`: the text; null appends nothing
    public static void escape(StringBuilder b, String s) {
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                b.append("&amp;");
            } else if (c == '<') {
                b.append("&lt;");
            } else if (c == '>') {
                b.append("&gt;");
            } else if (c == '"') {
                b.append("&quot;");
            } else {
                b.append(c);
            }
        }
    }
}
