/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.play;

import com.codename1.ui.CN;
import com.codename1.ui.Font;

/// The game's type and colours: Rajdhani (SIL Open Font License, see
/// `ART.md`) in capitals, over near-black and gunmetal, with one amber accent.
/// Friendly is always blue and hostile always red, whatever colour a team's
/// paint is.
public final class Type {
    public static final int AMBER = 0xf2b632;
    public static final int FRIENDLY = 0x4aa8ff;
    public static final int HOSTILE = 0xf0443a;
    public static final int INK = 0x0b0d10;
    public static final int GUNMETAL = 0x1c2127;
    public static final int STEEL = 0x8a949e;
    public static final int WHITE = 0xf2f4f5;
    public static final int STAMP = 0xe0141e;

    private static Font boldBase;
    private static Font mediumBase;

    private Type() {
    }

    /// Rajdhani Bold at `mm` millimetres.
    public static Font bold(float mm) {
        if (boldBase == null) {
            boldBase = load("Rajdhani-Bold");
        }
        return boldBase.derive(CN.convertToPixels(mm), Font.STYLE_BOLD);
    }

    /// Rajdhani Medium at `mm` millimetres.
    public static Font medium(float mm) {
        if (mediumBase == null) {
            mediumBase = load("Rajdhani-Medium");
        }
        return mediumBase.derive(CN.convertToPixels(mm), Font.STYLE_PLAIN);
    }

    private static Font load(String name) {
        if (Font.isTrueTypeFileSupported()) {
            try {
                return Font.createTrueTypeFont(name, name + ".ttf");
            } catch (RuntimeException e) {
                com.codename1.io.Log.e(e);
            }
        }
        return Font.createTrueTypeFont("native:MainBold", "native:MainBold");
    }

    /// `s` in capitals; the rules' names and operation titles are ASCII.
    public static String caps(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }
}
