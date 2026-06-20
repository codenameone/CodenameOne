/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

/// Parses the CSS color forms found in MapLibre style sheets into a packed
/// `0xAARRGGBB` integer: `#rgb`, `#rrggbb`, `#rrggbbaa`, `rgb(r,g,b)` and
/// `rgba(r,g,b,a)`. Unsupported forms (named colors, `hsl(...)`) return the
/// supplied default.
final class ColorParser {

    private ColorParser() {
    }

    static int parse(String value, int def) {
        if (value == null) {
            return def;
        }
        String s = value.trim();
        try {
            if (s.length() > 0 && s.charAt(0) == '#') {
                return parseHex(s.substring(1), def);
            }
            if (s.startsWith("rgba(") || s.startsWith("rgb(")) {
                return parseRgb(s, def);
            }
        } catch (Throwable t) {
            return def;
        }
        return def;
    }

    private static int parseHex(String hex, int def) {
        int r;
        int g;
        int b;
        int a = 255;
        if (hex.length() == 3) {
            r = hexPair("" + hex.charAt(0) + hex.charAt(0));
            g = hexPair("" + hex.charAt(1) + hex.charAt(1));
            b = hexPair("" + hex.charAt(2) + hex.charAt(2));
        } else if (hex.length() == 6) {
            r = hexPair(hex.substring(0, 2));
            g = hexPair(hex.substring(2, 4));
            b = hexPair(hex.substring(4, 6));
        } else if (hex.length() == 8) {
            r = hexPair(hex.substring(0, 2));
            g = hexPair(hex.substring(2, 4));
            b = hexPair(hex.substring(4, 6));
            a = hexPair(hex.substring(6, 8));
        } else {
            return def;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int parseRgb(String s, int def) {
        int open = s.indexOf('(');
        int close = s.indexOf(')');
        if (open < 0 || close < 0 || close < open) {
            return def;
        }
        String inner = s.substring(open + 1, close);
        String[] parts = split(inner, ',');
        if (parts.length < 3) {
            return def;
        }
        int r = clamp(parseIntSafe(parts[0]));
        int g = clamp(parseIntSafe(parts[1]));
        int b = clamp(parseIntSafe(parts[2]));
        int a = 255;
        if (parts.length >= 4) {
            double af = parseDoubleSafe(parts[3]);
            a = clamp((int) (af * 255 + 0.5));
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int hexPair(String pair) {
        return Integer.parseInt(pair, 16);
    }

    private static int parseIntSafe(String s) {
        return (int) parseDoubleSafe(s);
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static int clamp(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }

    private static String[] split(String s, char sep) {
        java.util.List parts = new java.util.ArrayList();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        String[] out = new String[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (String) parts.get(i);
        }
        return out;
    }
}
