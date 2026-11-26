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

import com.codename1.impl.xml.XmlScanner;
import com.codename1.wordprocessing.TextStyle;
import java.util.Map;

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

    /// Parses an ODF length (`pt`, `cm`, `mm`, `in`, `pc` or `px`; bare numbers are points)
    /// to points.
    ///
    /// #### Parameters
    ///
    /// - `v`: the attribute value; null answers the fallback
    /// - `fallback`: the answer for a missing or malformed value
    ///
    /// #### Returns
    ///
    /// the length in points
    public static float length(String v, float fallback) {
        if (v == null) {
            return fallback;
        }
        String s = v.trim();
        float scale = 1;
        int cut = s.length();
        if (s.endsWith("pt")) {
            cut -= 2;
        } else if (s.endsWith("cm")) {
            cut -= 2;
            scale = 72 / 2.54f;
        } else if (s.endsWith("mm")) {
            cut -= 2;
            scale = 72 / 25.4f;
        } else if (s.endsWith("in")) {
            cut -= 2;
            scale = 72;
        } else if (s.endsWith("pc")) {
            cut -= 2;
            scale = 12;
        } else if (s.endsWith("px")) {
            cut -= 2;
            scale = 0.75f;
        }
        try {
            float f = Float.parseFloat(s.substring(0, cut).trim()) * scale;
            return Float.isNaN(f) || Float.isInfinite(f) ? fallback : f;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// Decoded `style:text-properties`; a null field is a property the element leaves to
    /// its parent style. The ODT and ODP readers share this one parser.
    public static final class TextProps {
        private static final String[] NAMES = {"font-name", "font-family", "font-weight",
            "font-style", "text-underline-style", "text-line-through-style", "color",
            "background-color", "font-size", "text-position", "display"};

        /// The font family, unquoted.
        public String font;
        /// The size in points.
        public Float size;
        /// The RGB color.
        public Integer color;
        /// The RGB highlight, or `TextStyle.NO_BACKGROUND`.
        public Integer background;
        /// Bold.
        public Boolean bold;
        /// Italic.
        public Boolean italic;
        /// Underlined.
        public Boolean underline;
        /// Struck through.
        public Boolean strike;
        /// Hidden text (`text:display="none"`).
        public Boolean hidden;
        /// One of the `TextStyle` script constants.
        public String script;

        /// Decodes the text properties of the element a scanner is at. `style:font-name`
        /// wins over `fo:font-family`.
        ///
        /// #### Parameters
        ///
        /// - `x`: the scanner, at a `style:text-properties` start tag
        ///
        /// #### Returns
        ///
        /// the decoded properties
        public static TextProps read(XmlScanner x) {
            TextProps t = new TextProps();
            for (String n : NAMES) {
                String v = x.attr(n);
                if (v != null) {
                    t.put(n, v);
                }
            }
            return t;
        }

        /// Decodes text properties from attributes keyed by qualified or local name;
        /// `fo:font-family` wins over `style:font-name`, as in a resolved style.
        ///
        /// #### Parameters
        ///
        /// - `attributes`: the attributes; unknown names are ignored
        ///
        /// #### Returns
        ///
        /// the decoded properties
        public static TextProps of(Map<String, String> attributes) {
            TextProps t = new TextProps();
            String family = null;
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                String k = e.getKey();
                String local = k.substring(k.indexOf(':') + 1);
                if ("font-family".equals(local)) {
                    family = e.getValue();
                } else {
                    t.put(local, e.getValue());
                }
            }
            if (family != null) {
                t.font = unquote(family);
            }
            return t;
        }

        private void put(String n, String v) {
            if ("font-name".equals(n) || "font-family".equals(n)) {
                if (font == null) {
                    font = unquote(v);
                }
            } else if ("font-weight".equals(n)) {
                bold = Boolean.valueOf("bold".equals(v) || v.length() == 3 && v.charAt(0) >= '6');
            } else if ("font-style".equals(n)) {
                italic = Boolean.valueOf("italic".equals(v) || "oblique".equals(v));
            } else if ("text-underline-style".equals(n)) {
                underline = Boolean.valueOf(!"none".equals(v));
            } else if ("text-line-through-style".equals(n)) {
                strike = Boolean.valueOf(!"none".equals(v));
            } else if ("color".equals(n)) {
                int c = OdfText.color(v, -1);
                if (c >= 0) {
                    color = Integer.valueOf(c);
                }
            } else if ("background-color".equals(n)) {
                background = Integer.valueOf(OdfText.color(v, TextStyle.NO_BACKGROUND));
            } else if ("font-size".equals(n)) {
                float f = v.endsWith("%") ? 0 : length(v, 0);
                if (f > 0 && f < 1638) {
                    size = Float.valueOf(f);
                }
            } else if ("text-position".equals(n)) {
                script = scriptOf(v);
            } else if ("display".equals(n) && "none".equals(v)) {
                hidden = Boolean.TRUE;
            }
        }

        /// Applies these properties on top of a run style, mapping the families the writers
        /// emit for the logical fonts (`Arial`, `Times New Roman`, `Courier New`) back.
        ///
        /// #### Parameters
        ///
        /// - `base`: the inherited formatting
        ///
        /// #### Returns
        ///
        /// the combined formatting
        public TextStyle apply(TextStyle base) {
            TextStyle s = base;
            if (font != null && font.length() > 0) {
                String f = font;
                if ("Times New Roman".equals(f)) {
                    f = "Serif";
                } else if ("Courier New".equals(f)) {
                    f = "Monospace";
                } else if ("Arial".equals(f)) {
                    f = "SansSerif";
                }
                s = s.withFont(f);
            }
            if (size != null) {
                s = s.withSize(size.floatValue());
            }
            if (color != null) {
                s = s.withColor(color.intValue());
            }
            if (bold != null) {
                s = s.withBold(bold.booleanValue());
            }
            if (italic != null) {
                s = s.withItalic(italic.booleanValue());
            }
            if (underline != null) {
                s = s.withUnderline(underline.booleanValue());
            }
            if (strike != null) {
                s = s.withStrike(strike.booleanValue());
            }
            if (background != null) {
                s = s.withBackground(background.intValue());
            }
            if (script != null) {
                s = s.withScript(script);
            }
            return s;
        }
    }

    private static String unquote(String v) {
        String f = v.trim();
        if (f.length() > 1 && (f.charAt(0) == '\'' || f.charAt(0) == '"')) {
            return f.substring(1, f.length() - 1);
        }
        return f;
    }

    /// `style:text-position` to a `TextStyle` script: `super` or a positive percentage
    /// (`33% 58%`, as LibreOffice writes it), `sub` or a negative one.
    private static String scriptOf(String pos) {
        if (pos.startsWith("super") || pos.length() > 0 && pos.charAt(0) >= '1'
                && pos.charAt(0) <= '9') {
            return TextStyle.SUPERSCRIPT;
        }
        return pos.startsWith("sub") || pos.startsWith("-") ? TextStyle.SUBSCRIPT
                : TextStyle.BASELINE;
    }

    /// The intrinsic size of a PNG, GIF or JPEG in points at 96 dpi, which is what
    /// `fo:clip` is measured against.
    ///
    /// #### Parameters
    ///
    /// - `d`: the encoded image
    ///
    /// #### Returns
    ///
    /// width and height, or null for a format or header it cannot read
    public static float[] imageSize(byte[] d) {
        if (d == null) {
            return null;
        }
        int w = 0;
        int h = 0;
        if (d.length >= 24 && (d[0] & 0xff) == 0x89 && d[1] == 'P') {
            w = u16(d, 18);
            h = u16(d, 22);
        } else if (d.length >= 10 && d[0] == 'G' && d[1] == 'I') {
            w = (d[7] & 0xff) << 8 | d[6] & 0xff;
            h = (d[9] & 0xff) << 8 | d[8] & 0xff;
        } else if (d.length > 4 && (d[0] & 0xff) == 0xff && (d[1] & 0xff) == 0xd8) {
            int i = 2;
            while (i + 9 < d.length && (d[i] & 0xff) == 0xff) {
                int marker = d[i + 1] & 0xff;
                if (marker >= 0xc0 && marker <= 0xc3) {
                    h = u16(d, i + 5);
                    w = u16(d, i + 7);
                    break;
                }
                i += 2 + u16(d, i + 2);
            }
        }
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new float[] {w * 0.75f, h * 0.75f};
    }

    private static int u16(byte[] d, int i) {
        return (d[i] & 0xff) << 8 | d[i + 1] & 0xff;
    }

    /// Parses a `#rrggbb` color.
    ///
    /// #### Parameters
    ///
    /// - `v`: the attribute value
    /// - `fallback`: the answer for anything else, `transparent` included
    ///
    /// #### Returns
    ///
    /// the RGB value
    public static int color(String v, int fallback) {
        if (v == null || v.length() != 7 || v.charAt(0) != '#') {
            return fallback;
        }
        try {
            return Integer.parseInt(v.substring(1), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
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
            } else if (c < 0x20 && c != '\t' && c != '\n' && c != '\r'
                    || c == 0xfffe || c == 0xffff) {
                // not allowed in XML 1.0 at all, not even escaped: dropped, as XmlOut does
                continue;
            } else {
                b.append(c);
            }
        }
    }
}
