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

/// Parses and formats CSS colors.
public final class Colors {
    private static final String[] NAMES = {
        "aliceblue", "f0f8ff", "antiquewhite", "faebd7", "aqua", "00ffff", "aquamarine", "7fffd4",
        "azure", "f0ffff", "beige", "f5f5dc", "bisque", "ffe4c4", "black", "000000",
        "blanchedalmond", "ffebcd", "blue", "0000ff", "blueviolet", "8a2be2", "brown", "a52a2a",
        "burlywood", "deb887", "cadetblue", "5f9ea0", "chartreuse", "7fff00", "chocolate", "d2691e",
        "coral", "ff7f50", "cornflowerblue", "6495ed", "cornsilk", "fff8dc", "crimson", "dc143c",
        "cyan", "00ffff", "darkblue", "00008b", "darkcyan", "008b8b", "darkgoldenrod", "b8860b",
        "darkgray", "a9a9a9", "darkgreen", "006400", "darkgrey", "a9a9a9", "darkkhaki", "bdb76b",
        "darkmagenta", "8b008b", "darkolivegreen", "556b2f", "darkorange", "ff8c00", "darkorchid", "9932cc",
        "darkred", "8b0000", "darksalmon", "e9967a", "darkseagreen", "8fbc8f", "darkslateblue", "483d8b",
        "darkslategray", "2f4f4f", "darkslategrey", "2f4f4f", "darkturquoise", "00ced1", "darkviolet", "9400d3",
        "deeppink", "ff1493", "deepskyblue", "00bfff", "dimgray", "696969", "dimgrey", "696969",
        "dodgerblue", "1e90ff", "firebrick", "b22222", "floralwhite", "fffaf0", "forestgreen", "228b22",
        "fuchsia", "ff00ff", "gainsboro", "dcdcdc", "ghostwhite", "f8f8ff", "gold", "ffd700",
        "goldenrod", "daa520", "gray", "808080", "grey", "808080", "green", "008000",
        "greenyellow", "adff2f", "honeydew", "f0fff0", "hotpink", "ff69b4", "indianred", "cd5c5c",
        "indigo", "4b0082", "ivory", "fffff0", "khaki", "f0e68c", "lavender", "e6e6fa",
        "lavenderblush", "fff0f5", "lawngreen", "7cfc00", "lemonchiffon", "fffacd", "lightblue", "add8e6",
        "lightcoral", "f08080", "lightcyan", "e0ffff", "lightgoldenrodyellow", "fafad2", "lightgray", "d3d3d3",
        "lightgreen", "90ee90", "lightgrey", "d3d3d3", "lightpink", "ffb6c1", "lightsalmon", "ffa07a",
        "lightseagreen", "20b2aa", "lightskyblue", "87cefa", "lightslategray", "778899", "lightslategrey", "778899",
        "lightsteelblue", "b0c4de", "lightyellow", "ffffe0", "lime", "00ff00", "limegreen", "32cd32",
        "linen", "faf0e6", "magenta", "ff00ff", "maroon", "800000", "mediumaquamarine", "66cdaa",
        "mediumblue", "0000cd", "mediumorchid", "ba55d3", "mediumpurple", "9370db", "mediumseagreen", "3cb371",
        "mediumslateblue", "7b68ee", "mediumspringgreen", "00fa9a", "mediumturquoise", "48d1cc", "mediumvioletred", "c71585",
        "midnightblue", "191970", "mintcream", "f5fffa", "mistyrose", "ffe4e1", "moccasin", "ffe4b5",
        "navajowhite", "ffdead", "navy", "000080", "oldlace", "fdf5e6", "olive", "808000",
        "olivedrab", "6b8e23", "orange", "ffa500", "orangered", "ff4500", "orchid", "da70d6",
        "palegoldenrod", "eee8aa", "palegreen", "98fb98", "paleturquoise", "afeeee", "palevioletred", "db7093",
        "papayawhip", "ffefd5", "peachpuff", "ffdab9", "peru", "cd853f", "pink", "ffc0cb",
        "plum", "dda0dd", "powderblue", "b0e0e6", "purple", "800080", "rebeccapurple", "663399",
        "red", "ff0000", "rosybrown", "bc8f8f", "royalblue", "4169e1", "saddlebrown", "8b4513",
        "salmon", "fa8072", "sandybrown", "f4a460", "seagreen", "2e8b57", "seashell", "fff5ee",
        "sienna", "a0522d", "silver", "c0c0c0", "skyblue", "87ceeb", "slateblue", "6a5acd",
        "slategray", "708090", "slategrey", "708090", "snow", "fffafa", "springgreen", "00ff7f",
        "steelblue", "4682b4", "tan", "d2b48c", "teal", "008080", "thistle", "d8bfd8",
        "tomato", "ff6347", "turquoise", "40e0d0", "violet", "ee82ee", "wheat", "f5deb3",
        "white", "ffffff", "whitesmoke", "f5f5f5", "yellow", "ffff00", "yellowgreen", "9acd32"
    };

    private Colors() {
    }

    /// Parses `#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`, `rgb()`, `rgba()`, `hsl()`, `hsla()`,
    /// a named color or `transparent`.
    ///
    /// #### Parameters
    ///
    /// - `s`: the color text
    /// - `def`: returned when `s` is null or not a color
    ///
    /// #### Returns
    ///
    /// the color as `0xAARRGGBB`
    public static int parse(String s, int def) {
        if (s == null) {
            return def;
        }
        s = s.trim();
        if (s.length() == 0) {
            return def;
        }
        if (s.charAt(0) == '#') {
            return hex(s.substring(1), def);
        }
        if ("transparent".equalsIgnoreCase(s)) {
            return 0;
        }
        int paren = s.indexOf('(');
        if (paren > 0 && s.endsWith(")")) {
            String fn = s.substring(0, paren).trim();
            String[] parts = split(s.substring(paren + 1, s.length() - 1));
            if ("rgb".equalsIgnoreCase(fn) || "rgba".equalsIgnoreCase(fn)) {
                if (parts.length < 3) {
                    return def;
                }
                int r = channel(parts[0]);
                int g = channel(parts[1]);
                int b = channel(parts[2]);
                int a = parts.length > 3 ? alpha(parts[3]) : 255;
                if (r < 0 || g < 0 || b < 0 || a < 0) {
                    return def;
                }
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            if ("hsl".equalsIgnoreCase(fn) || "hsla".equalsIgnoreCase(fn)) {
                if (parts.length < 3) {
                    return def;
                }
                try {
                    float h = Float.parseFloat(strip(parts[0], "deg")) / 360f;
                    float sat = Float.parseFloat(strip(parts[1], "%")) / 100f;
                    float l = Float.parseFloat(strip(parts[2], "%")) / 100f;
                    int a = parts.length > 3 ? alpha(parts[3]) : 255;
                    if (a < 0) {
                        return def;
                    }
                    h = h - (float) Math.floor(h);
                    float[] rgb = hslToRgb(h, clamp(sat), clamp(l));
                    return (a << 24) | (to8(rgb[0]) << 16) | (to8(rgb[1]) << 8) | to8(rgb[2]);
                } catch (NumberFormatException e) {
                    return def;
                }
            }
            return def;
        }
        for (int i = 0; i < NAMES.length; i += 2) {
            if (NAMES[i].equalsIgnoreCase(s)) {
                return 0xff000000 | Integer.parseInt(NAMES[i + 1], 16);
            }
        }
        return def;
    }

    /// Formats a color as `#rrggbb`, ignoring alpha.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color
    public static String toHex(int argb) {
        String h = Integer.toHexString(argb & 0xffffff);
        StringBuilder sb = new StringBuilder("#");
        for (int i = h.length(); i < 6; i++) {
            sb.append('0');
        }
        return sb.append(h).toString();
    }

    private static String strip(String s, String suffix) {
        s = s.trim();
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length()).trim();
        }
        return s;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static int to8(float v) {
        int i = (int) (v * 255 + 0.5f);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private static String[] split(String s) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        int st = 0;
        for (int i = 0; i <= s.length(); i++) {
            char c = i == s.length() ? ',' : s.charAt(i);
            if (c == ',' || c == ' ' || c == '/') {
                String p = s.substring(st, i).trim();
                if (p.length() > 0) {
                    out.add(p);
                }
                st = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static int channel(String p) {
        try {
            if (p.endsWith("%")) {
                return to8(Float.parseFloat(p.substring(0, p.length() - 1)) / 100f);
            }
            int v = Math.round(Float.parseFloat(p));
            return v < 0 ? 0 : (v > 255 ? 255 : v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int alpha(String p) {
        try {
            if (p.endsWith("%")) {
                return to8(Float.parseFloat(p.substring(0, p.length() - 1)) / 100f);
            }
            return to8(clamp(Float.parseFloat(p)));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int hex(String h, int def) {
        try {
            if (h.length() == 3 || h.length() == 4) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < h.length(); i++) {
                    sb.append(h.charAt(i)).append(h.charAt(i));
                }
                h = sb.toString();
            }
            if (h.length() == 6) {
                return 0xff000000 | Integer.parseInt(h, 16);
            }
            if (h.length() == 8) {
                int rgb = Integer.parseInt(h.substring(0, 6), 16);
                int a = Integer.parseInt(h.substring(6), 16);
                return (a << 24) | rgb;
            }
        } catch (NumberFormatException e) {
            return def;
        }
        return def;
    }

    static float[] hslToRgb(float h, float s, float l) {
        if (s == 0) {
            return new float[]{l, l, l};
        }
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        return new float[]{hue(p, q, h + 1f / 3), hue(p, q, h), hue(p, q, h - 1f / 3)};
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1f / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 0.5f) {
            return q;
        }
        if (t < 2f / 3) {
            return p + (q - p) * (2f / 3 - t) * 6;
        }
        return p;
    }
}
