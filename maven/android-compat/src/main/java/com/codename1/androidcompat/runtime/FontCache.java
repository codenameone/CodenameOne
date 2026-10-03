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
package com.codename1.androidcompat.runtime;

import android.graphics.Typeface;
import com.codename1.ui.Font;

import java.util.HashMap;
import java.util.Map;

/// Maps a typeface and a pixel size onto a Codename One font, caching the
/// result: deriving a native font is not free and text is measured often.
public final class FontCache {

    private static final Map<String, Font> CACHE = new HashMap<String, Font>();

    private FontCache() {
    }

    public static Font font(Typeface tf, float sizePx) {
        if (tf == null) {
            tf = Typeface.DEFAULT;
        }
        int size = Math.max(1, Math.round(sizePx));
        String nativeName = nativeName(tf);
        String key = (tf.getFile() == null ? nativeName : tf.getFile()) + "@" + size;
        Font f = CACHE.get(key);
        if (f != null) {
            return f;
        }
        f = create(tf, nativeName, size);
        CACHE.put(key, f);
        return f;
    }

    private static Font create(Typeface tf, String nativeName, int size) {
        try {
            if (tf.getFile() != null && Font.isTrueTypeFileSupported()) {
                String file = tf.getFile();
                String name = file.endsWith(".ttf") || file.endsWith(".otf") ? file.substring(0, file.length() - 4) : file;
                return Font.createTrueTypeFont(name, file).derive(size, Font.STYLE_PLAIN);
            }
            if (Font.isNativeFontSchemeSupported()) {
                return Font.createTrueTypeFont(nativeName, nativeName).derive(size, Font.STYLE_PLAIN);
            }
        } catch (RuntimeException e) {
            // Fall through to a system font: a missing bundled font should not
            // take the screen down with it.
            com.codename1.io.Log.p("Font " + tf.getFamily() + " unavailable, using the system font: " + e);
        }
        int style = (tf.isBold() ? Font.STYLE_BOLD : 0) | (tf.isItalic() ? Font.STYLE_ITALIC : 0);
        int face = "monospace".equals(tf.getFamily()) ? Font.FACE_MONOSPACE : Font.FACE_SYSTEM;
        Font small = Font.createSystemFont(face, style, Font.SIZE_SMALL);
        Font medium = Font.createSystemFont(face, style, Font.SIZE_MEDIUM);
        Font large = Font.createSystemFont(face, style, Font.SIZE_LARGE);
        if (size <= small.getHeight()) {
            return small;
        }
        if (size >= large.getHeight()) {
            return large;
        }
        return medium;
    }

    static String nativeName(Typeface tf) {
        int w = tf.getWeight();
        boolean italic = tf.isItalic();
        String base;
        if (w >= 800) {
            base = "Black";
        } else if (w >= 600) {
            // Medium (500, Android's sans-serif-medium: titles, buttons) has no
            // native face. CSS font matching falls back from 500 to 400 before
            // anything heavier, and Codename One's bold is condensed on Android,
            // which made every medium title narrow and heavy.
            base = "Bold";
        } else if (w <= 150) {
            base = "Thin";
        } else if (w <= 350) {
            base = "Light";
        } else {
            base = "Regular";
        }
        return (italic ? "native:Italic" : "native:Main") + base;
    }
}
