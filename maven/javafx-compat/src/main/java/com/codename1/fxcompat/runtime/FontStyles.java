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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/// What nodes that show text share when they are styled and measured:
/// the `-fx-font...` names, keyword values and the breaking of text into
/// lines.
public final class FontStyles {

    private FontStyles() {
    }

    /// Returns whether a style name is one of `-fx-font`,
    /// `-fx-font-size`, `-fx-font-family`, `-fx-font-weight` and
    /// `-fx-font-style`.
    public static boolean isFontProperty(String property) {
        return "-fx-font".equals(property) || "-fx-font-size".equals(property)
                || "-fx-font-family".equals(property) || "-fx-font-weight".equals(property)
                || "-fx-font-style".equals(property);
    }

    private static FontWeight weight(Object value) {
        if (value instanceof FontWeight) {
            return (FontWeight) value;
        } else if (value instanceof Number) {
            return FontWeight.findByWeight(((Number) value).intValue());
        } else if (value instanceof String) {
            return FontWeight.findByName(((String) value).trim());
        }
        return null;
    }

    private static FontPosture posture(Object value) {
        if (value instanceof FontPosture) {
            return (FontPosture) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            if ("italic".equalsIgnoreCase(s) || "oblique".equalsIgnoreCase(s)) {
                return FontPosture.ITALIC;
            } else if ("normal".equalsIgnoreCase(s) || "regular".equalsIgnoreCase(s)) {
                return FontPosture.REGULAR;
            }
        }
        return null;
    }

    /// Returns a font with one styled attribute replaced: the font itself
    /// for `-fx-font` (a `Font`), or the base font with another size
    /// (`Number`), family (`String`), weight (`FontWeight`, a `Number`
    /// from 100 to 900 or a keyword) or posture (`FontPosture` or a
    /// keyword). Answers `null` when the value does not suit the name.
    public static Font apply(Font base, String property, Object value) {
        if (value instanceof Font) {
            return (Font) value;
        }
        Font from = base == null ? Font.getDefault() : base;
        FontWeight w = from.cn1Weight();
        FontPosture p = from.cn1Posture();
        String family = from.getFamily();
        double size = from.getSize();
        if ("-fx-font-size".equals(property) && value instanceof Number) {
            size = ((Number) value).doubleValue();
        } else if ("-fx-font-family".equals(property) && value instanceof String) {
            family = (String) value;
        } else if ("-fx-font-weight".equals(property) && weight(value) != null) {
            w = weight(value);
        } else if ("-fx-font-style".equals(property) && posture(value) != null) {
            p = posture(value);
        } else {
            return null;
        }
        return Font.font(family, w, p, size);
    }

    /// Returns the constant a style value stands for: the value itself
    /// when it is one of the constants, or the constant whose name is the
    /// keyword, compared without regard to ASCII case and with `-` for
    /// `_` (`"round"`, `"space-between"`). Answers `null` otherwise.
    public static Object keyword(Enum<?>[] constants, Object value) {
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] == value) {
                return constants[i];
            }
        }
        if (value instanceof String) {
            String wanted = ((String) value).trim().replace('-', '_');
            for (int i = 0; i < constants.length; i++) {
                if (constants[i].name().equalsIgnoreCase(wanted)) {
                    return constants[i];
                }
            }
        }
        return null;
    }

    private static void wrap(String paragraph, Font font, double width, ArrayList<String> out) {
        if (!(width > 0) || Fonts.width(font, paragraph) <= width) {
            out.add(paragraph);
            return;
        }
        int n = paragraph.length();
        int lineStart = 0;
        while (lineStart < n) {
            // The longest run of whole words that fits; a word that does
            // not fit on a line of its own is cut where it does.
            int end = lineStart;
            int lastBreak = -1;
            while (end < n) {
                char c = paragraph.charAt(end);
                if (c == ' ' && end > lineStart) {
                    lastBreak = end;
                }
                if (end > lineStart && Fonts.width(font, paragraph.substring(lineStart, end + 1)) > width) {
                    break;
                }
                end++;
            }
            if (end >= n) {
                out.add(paragraph.substring(lineStart));
                return;
            }
            int cut = lastBreak > lineStart ? lastBreak : end;
            out.add(paragraph.substring(lineStart, cut));
            lineStart = cut;
            while (lineStart < n && paragraph.charAt(lineStart) == ' ') {
                lineStart++;
            }
        }
    }

    /// Breaks text into lines: at every line feed, and, with a wrapping
    /// width above zero, between words wherever a line would be wider.
    /// There is always at least one line; `null` text is one empty line.
    public static String[] lines(String text, Font font, double wrappingWidth) {
        ArrayList<String> out = new ArrayList<String>();
        String t = text == null ? "" : text;
        int start = 0;
        while (true) {
            int feed = t.indexOf('\n', start);
            String paragraph = feed < 0 ? t.substring(start) : t.substring(start, feed);
            if (paragraph.length() > 0 && paragraph.charAt(paragraph.length() - 1) == '\r') {
                paragraph = paragraph.substring(0, paragraph.length() - 1);
            }
            wrap(paragraph, font, wrappingWidth, out);
            if (feed < 0) {
                break;
            }
            start = feed + 1;
        }
        String[] result = new String[out.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = out.get(i);
        }
        return result;
    }
}
