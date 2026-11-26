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

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Font;

import java.util.HashMap;
import java.util.Map;

/// Maps the font families an artwork names to Codename One fonts.
///
/// Register a TrueType font for a family with `register`; any other family falls back to
/// the native main font (or the system font where native fonts are missing), in the right
/// weight and style.
public final class ArtFonts {
    private static final Map<String, Font> REGISTERED = new HashMap<String, Font>();

    private ArtFonts() {
    }

    /// Registers a font for a family name.
    ///
    /// #### Parameters
    ///
    /// - `family`: the name an artwork uses, compared ignoring case
    /// - `bold`: the weight it is for
    /// - `italic`: the style it is for
    /// - `font`: the font, at any size; it is derived to each size needed
    public static void register(String family, boolean bold, boolean italic, Font font) {
        REGISTERED.put(key(family, bold, italic), font);
    }

    private static String key(String family, boolean bold, boolean italic) {
        StringBuilder sb = new StringBuilder();
        String f = family == null ? "" : family;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.append(bold ? "/b" : "/r").append(italic ? "i" : "").toString();
    }

    /// True when fonts can be measured and drawn, which needs an initialized display.
    static boolean available() {
        return Display.isInitialized();
    }

    /// The font for a run at a size in pixels; null without a display.
    static Font font(TextRun run, float px) {
        if (!available()) {
            return null;
        }
        Font f = REGISTERED.get(key(run.getFontFamily(), run.isBold(), run.isItalic()));
        int weight = (run.isBold() ? CN.STYLE_BOLD : 0) | (run.isItalic() ? CN.STYLE_ITALIC : 0);
        if (f != null) {
            return f.derive(px, weight);
        }
        if (Font.isNativeFontSchemeSupported()) {
            String n;
            if (run.isBold()) {
                n = run.isItalic() ? "native:ItalicBold" : "native:MainBold";
            } else {
                n = run.isItalic() ? "native:ItalicRegular" : "native:MainRegular";
            }
            return Font.createTrueTypeFont(n, n).derive(px, weight);
        }
        return Font.createSystemFont(CN.FACE_SYSTEM, weight, CN.SIZE_MEDIUM);
    }
}
