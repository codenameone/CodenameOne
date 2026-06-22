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
package com.codename1.ui;

import com.codename1.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Parses the CSS color forms into a packed `0xAARRGGBB` integer: `#rgb`,
/// `#rgba`, `#rrggbb`, `#rrggbbaa`, `rgb(...)` / `rgba(...)` with integer or
/// percentage components, and the CSS named-color subset.
///
/// This is the single home for CSS color-string parsing in the framework. The
/// runtime CSS gradient parser and the vector map style engine both delegate
/// here so the grammar -- and the named-color table -- lives in exactly one
/// place.
public final class CSSColor {

    private CSSColor() {
    }

    private static final Map<String, Integer> NAMED_COLORS = buildNamedColors();

    /// Parses a CSS color string into `0xAARRGGBB`, throwing
    /// `IllegalArgumentException` when the value is not a recognised color so
    /// the caller can choose between a default and a propagated failure.
    public static int parse(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException("Empty color");
        }
        if (trimmed.charAt(0) == '#') {
            return parseHexColor(trimmed.substring(1));
        }
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("rgba(") || lower.startsWith("rgb(")) {
            int open = trimmed.indexOf('(');
            int close = trimmed.lastIndexOf(')');
            if (open < 0 || close < open) {
                throw new IllegalArgumentException("Unrecognised color: " + value);
            }
            List<String> comps = StringUtil.tokenize(trimmed.substring(open + 1, close), ',');
            if (comps.size() < 3) {
                throw new IllegalArgumentException("Unrecognised color: " + value);
            }
            int r = parseColorComponent(comps.get(0));
            int g = parseColorComponent(comps.get(1));
            int b = parseColorComponent(comps.get(2));
            int a = 255;
            if (comps.size() >= 4) {
                a = clamp(Math.round(parseFloat(comps.get(3).trim()) * 255f));
            }
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
        Integer named = NAMED_COLORS.get(lower);
        if (named != null) {
            return named.intValue();
        }
        throw new IllegalArgumentException("Unrecognised color: " + value);
    }

    /// Parses a CSS color string into `0xAARRGGBB`, returning `defaultColor`
    /// when the value is `null` or not a recognised color. The lenient variant
    /// for styling input that should degrade gracefully rather than fail.
    public static int parse(String value, int defaultColor) {
        if (value == null) {
            return defaultColor;
        }
        try {
            return parse(value);
        } catch (RuntimeException ex) {
            return defaultColor;
        }
    }

    private static int parseColorComponent(String s) {
        String t = s.trim();
        if (t.endsWith("%")) {
            return Math.round(parseFloat(t.substring(0, t.length() - 1)) * 2.55f);
        }
        return Integer.parseInt(t);
    }

    private static int parseHexColor(String hex) {
        int len = hex.length();
        int r;
        int g;
        int b;
        int a = 255;
        if (len == 3) {
            r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
            g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
            b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
        } else if (len == 4) {
            r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
            g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
            b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
            a = Integer.parseInt(hex.substring(3, 4), 16) * 17;
        } else if (len == 6) {
            r = Integer.parseInt(hex.substring(0, 2), 16);
            g = Integer.parseInt(hex.substring(2, 4), 16);
            b = Integer.parseInt(hex.substring(4, 6), 16);
        } else if (len == 8) {
            r = Integer.parseInt(hex.substring(0, 2), 16);
            g = Integer.parseInt(hex.substring(2, 4), 16);
            b = Integer.parseInt(hex.substring(4, 6), 16);
            a = Integer.parseInt(hex.substring(6, 8), 16);
        } else {
            throw new IllegalArgumentException("Bad hex color: #" + hex);
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
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

    private static float parseFloat(String s) {
        return Float.parseFloat(s.trim());
    }

    private static Map<String, Integer> buildNamedColors() {
        // CSS named-color subset matching CSSTheme's build-time mapping. Keep
        // this list in sync if the compiler adds more names.
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("transparent", Integer.valueOf(0x00000000));
        m.put("black", Integer.valueOf(0xff000000));
        m.put("white", Integer.valueOf(0xffffffff));
        m.put("red", Integer.valueOf(0xffff0000));
        m.put("green", Integer.valueOf(0xff008000));
        m.put("blue", Integer.valueOf(0xff0000ff));
        m.put("yellow", Integer.valueOf(0xffffff00));
        m.put("cyan", Integer.valueOf(0xff00ffff));
        m.put("magenta", Integer.valueOf(0xffff00ff));
        m.put("gray", Integer.valueOf(0xff808080));
        m.put("grey", Integer.valueOf(0xff808080));
        m.put("silver", Integer.valueOf(0xffc0c0c0));
        m.put("maroon", Integer.valueOf(0xff800000));
        m.put("olive", Integer.valueOf(0xff808000));
        m.put("purple", Integer.valueOf(0xff800080));
        m.put("teal", Integer.valueOf(0xff008080));
        m.put("navy", Integer.valueOf(0xff000080));
        m.put("orange", Integer.valueOf(0xffffa500));
        m.put("pink", Integer.valueOf(0xffffc0cb));
        return m;
    }
}
