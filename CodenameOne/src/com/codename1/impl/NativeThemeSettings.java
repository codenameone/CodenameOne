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
package com.codename1.impl;

import java.util.HashMap;
import java.util.Map;
import com.codename1.util.StringUtil;

/// Internal port snapshot of OS theme defaults. Font size is in CN1 device pixels,
/// before accessibility scaling. Missing values mean unsupported, never a guessed value.
public final class NativeThemeSettings {
    private final Map<String, String> colors = new HashMap<String, String>();
    private String fontFamily;
    private float fontSize;

    public NativeThemeSettings color(String token, int rgb) {
        colors.put(token, Integer.toHexString(0x1000000 | (rgb & 0xffffff)).substring(1));
        return this;
    }

    /// Compares only the categories the application opted into.
    public NativeThemeSettings forInheritance(boolean inheritColors, boolean inheritFonts) {
        NativeThemeSettings result = new NativeThemeSettings();
        if (inheritColors) {
            result.colors.putAll(colors);
        }
        if (inheritFonts) {
            result.font(fontFamily, fontSize);
        }
        return result;
    }

    public NativeThemeSettings copy() {
        NativeThemeSettings result = new NativeThemeSettings();
        result.colors.putAll(colors);
        result.fontFamily = fontFamily;
        result.fontSize = fontSize;
        return result;
    }

    public String getColor(String token) {
        return colors.get(token);
    }

    public NativeThemeSettings font(String family, float pixels) {
        if (family != null && family.length() > 0 && pixels > 0 && pixels < 10000) {
            fontFamily = family;
            fontSize = pixels;
        }
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public float getFontSize() {
        return fontSize;
    }

    /// Parses the native bridge's newline-separated key=value snapshot. Unknown
    /// and malformed entries are ignored so partially supported settings still work.
    public static NativeThemeSettings parse(String encoded) {
        NativeThemeSettings result = new NativeThemeSettings();
        if (encoded == null) {
            return result;
        }
        String family = null;
        float size = 0;
        for (String line : StringUtil.tokenize(encoded, '\n')) {
            int split = line.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = line.substring(0, split);
            String value = line.substring(split + 1);
            try {
                if ("fontFamily".equals(key)) {
                    family = value;
                } else if ("fontSize".equals(key)) {
                    size = Float.parseFloat(value);
                } else if (value.length() == 6) {
                    result.color(key, Integer.parseInt(value, 16));
                }
            } catch (IllegalArgumentException ignored) {
                // An unavailable or invalid setting keeps the bundled fallback.
            }
        }
        return result.font(family, size);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NativeThemeSettings)) {
            return false;
        }
        NativeThemeSettings that = (NativeThemeSettings) other;
        return colors.equals(that.colors) && Float.floatToIntBits(fontSize) == Float.floatToIntBits(that.fontSize)
                && (fontFamily == null ? that.fontFamily == null : fontFamily.equals(that.fontFamily));
    }

    @Override
    public int hashCode() {
        return colors.hashCode() * 31 + (fontFamily == null ? 0 : fontFamily.hashCode())
                + Float.floatToIntBits(fontSize);
    }
}
