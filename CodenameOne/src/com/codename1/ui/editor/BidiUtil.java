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

package com.codename1.ui.editor;

/// A compact implementation of the core Unicode Bidirectional Algorithm (UAX #9) used by the pure text
/// editor to lay out, hit test and render a single line of mixed left-to-right / right-to-left text.
///
/// The editor renders each directional run as a substring so the platform font engine performs the actual
/// glyph shaping (Arabic joining, ligatures); this class only resolves the *embedding levels* of the
/// characters on a line and the *visual order* those characters appear in. That division keeps the editor
/// correct on every platform while remaining self contained and unit testable with no UI dependency.
///
/// A single paragraph line is assumed (no explicit embedding/override controls beyond the paragraph base
/// direction, and no line wrapping inside the algorithm). This covers the editor's model where each
/// document line is one paragraph. The implementation follows the resolution phases of UAX #9: character
/// typing, the weak type rules W1-W7, the neutral rules N1-N2, the implicit level rules I1-I2 and the
/// reordering rule L2.
final class BidiUtil {
    // Bidirectional character types (a subset of UAX #9 sufficient for the editor).
    static final byte L = 0;    // Left-to-Right (strong)
    static final byte R = 1;    // Right-to-Left (strong)
    static final byte AL = 2;   // Right-to-Left Arabic (strong)
    static final byte EN = 3;   // European Number
    static final byte ES = 4;   // European Number Separator
    static final byte ET = 5;   // European Number Terminator
    static final byte AN = 6;   // Arabic Number
    static final byte CS = 7;   // Common Number Separator
    static final byte NSM = 8;  // Nonspacing Mark
    static final byte BN = 9;   // Boundary Neutral
    static final byte ON = 10;  // Other Neutral
    static final byte WS = 11;  // Whitespace

    private BidiUtil() {
    }

    /// Returns the bidirectional character type of a single UTF-16 code unit. Uses coarse Unicode ranges
    /// that cover the scripts an editor realistically handles (Latin, Arabic, Hebrew, Syriac, Thaana,
    /// common digits, punctuation and whitespace).
    static byte typeOf(char c) {
        if (c == '\t' || c == ' ' || c == 0x00A0 || c == 0x2007 || c == 0x202F) {
            return WS;
        }
        if (c == '\n' || c == '\r' || c == 0x0085 || c == 0x2028 || c == 0x2029) {
            return WS;
        }
        // European digits
        if (c >= '0' && c <= '9') {
            return EN;
        }
        // Arabic-Indic and Extended Arabic-Indic digits
        if ((c >= 0x0660 && c <= 0x0669) || (c >= 0x06F0 && c <= 0x06F9)) {
            return AN;
        }
        if (c == '+' || c == '-') {
            return ES;
        }
        if (c == '#' || c == '$' || c == '%' || c == 0x00A2 || c == 0x00A3 || c == 0x00A5
                || c == 0x00B0 || c == 0x00B1 || c == 0x066A) {
            return ET;
        }
        if (c == ',' || c == '.' || c == ':' || c == '/') {
            return CS;
        }
        // Combining marks
        if ((c >= 0x0300 && c <= 0x036F) || (c >= 0x0483 && c <= 0x0489)
                || (c >= 0x0591 && c <= 0x05BD) || (c >= 0x0610 && c <= 0x061A)
                || (c >= 0x064B && c <= 0x065F) || c == 0x0670
                || (c >= 0x06D6 && c <= 0x06DC) || (c >= 0x06DF && c <= 0x06E4)) {
            return NSM;
        }
        // Hebrew and Hebrew presentation forms (strong R)
        if ((c >= 0x0590 && c <= 0x05FF) || (c >= 0xFB1D && c <= 0xFB4F)) {
            return R;
        }
        // Arabic, Arabic Supplement/Extended, Syriac, Thaana, and Arabic presentation forms (strong AL)
        if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0x0700 && c <= 0x074F)
                || (c >= 0x0750 && c <= 0x077F) || (c >= 0x0780 && c <= 0x07BF)
                || (c >= 0x08A0 && c <= 0x08FF)
                || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) {
            return AL;
        }
        // Latin letters and the vast majority of the BMP letters an editor sees are strong L.
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
            return L;
        }
        if (c >= 0x00C0 && c <= 0x024F) {
            return L;
        }
        // CJK, Kana, Hangul and the like are strong L for bidi purposes.
        if (c >= 0x2E80 && c <= 0xD7FF) {
            return L;
        }
        if (c >= 0xF900 && c <= 0xFAFF) {
            return L;
        }
        // Everything else (ASCII punctuation, symbols) is a neutral.
        return ON;
    }

    /// Returns true if the paragraph's automatically detected base direction is right-to-left, per rules
    /// P2/P3: the direction of the first strong (L, R or AL) character, defaulting to left-to-right.
    static boolean autoBaseRtl(String text) {
        for (int i = 0; i < text.length(); i++) {
            byte t = typeOf(text.charAt(i));
            if (t == L) {
                return false;
            }
            if (t == R || t == AL) {
                return true;
            }
        }
        return false;
    }

    /// Resolves the bidi embedding level of every character on a line. `baseRtl` is the paragraph base
    /// direction (embedding level 1 when true, 0 when false).
    ///
    /// #### Parameters
    ///
    /// - `text`: the line text (a single paragraph, no line breaks)
    ///
    /// - `baseRtl`: true for a right-to-left base direction
    ///
    /// #### Returns
    ///
    /// a `byte[]` the same length as `text`, holding the resolved level of each character
    static byte[] resolveLevels(String text, boolean baseRtl) {
        int n = text.length();
        byte baseLevel = (byte) (baseRtl ? 1 : 0);
        byte[] levels = new byte[n];
        if (n == 0) {
            return levels;
        }
        byte[] types = new byte[n];
        for (int i = 0; i < n; i++) {
            types[i] = typeOf(text.charAt(i));
        }
        // W1: NSM takes the type of the previous character, or the base type (treated as ON) at the start.
        for (int i = 0; i < n; i++) {
            if (types[i] == NSM) {
                types[i] = (i == 0) ? ON : types[i - 1];
            }
        }
        // W2: EN becomes AN if the previous strong type is AL.
        byte prevStrong = baseRtl ? R : L;
        for (int i = 0; i < n; i++) {
            byte t = types[i];
            if (t == L || t == R || t == AL) {
                prevStrong = t;
            } else if (t == EN && prevStrong == AL) {
                types[i] = AN;
            }
        }
        // W3: AL becomes R.
        for (int i = 0; i < n; i++) {
            if (types[i] == AL) {
                types[i] = R;
            }
        }
        // W4: a single ES between two EN becomes EN; a single CS between two numbers of the same type
        // becomes that type.
        for (int i = 1; i < n - 1; i++) {
            if (types[i] == ES && types[i - 1] == EN && types[i + 1] == EN) {
                types[i] = EN;
            } else if (types[i] == CS && types[i - 1] == types[i + 1]
                    && (types[i - 1] == EN || types[i - 1] == AN)) {
                types[i] = types[i - 1];
            }
        }
        // W5: a sequence of ET adjacent to EN becomes EN.
        for (int i = 0; i < n; i++) {
            if (types[i] == ET) {
                int start = i;
                while (i < n && types[i] == ET) {
                    i++;
                }
                boolean en = (start > 0 && types[start - 1] == EN) || (i < n && types[i] == EN);
                if (en) {
                    for (int j = start; j < i; j++) {
                        types[j] = EN;
                    }
                }
                i--;
            }
        }
        // W6: remaining ES, ET, CS become ON.
        for (int i = 0; i < n; i++) {
            if (types[i] == ES || types[i] == ET || types[i] == CS) {
                types[i] = ON;
            }
        }
        // W7: EN becomes L if the previous strong type is L.
        prevStrong = baseRtl ? R : L;
        for (int i = 0; i < n; i++) {
            byte t = types[i];
            if (t == L || t == R) {
                prevStrong = t;
            } else if (t == EN && prevStrong == L) {
                types[i] = L;
            }
        }
        // N1/N2: resolve neutrals. A neutral run between two strongs of the same direction takes that
        // direction (numbers count as R); otherwise it takes the base direction. Whitespace and boundary
        // neutrals are treated as neutrals here.
        byte baseDir = baseRtl ? R : L;
        for (int i = 0; i < n; i++) {
            if (isNeutral(types[i])) {
                int start = i;
                while (i < n && isNeutral(types[i])) {
                    i++;
                }
                byte before = start == 0 ? baseDir : dirOf(types[start - 1]);
                byte after = i >= n ? baseDir : dirOf(types[i]);
                byte resolved = (before == after) ? before : baseDir;
                for (int j = start; j < i; j++) {
                    types[j] = resolved;
                }
                i--;
            }
        }
        // I1/I2: implicit levels.
        for (int i = 0; i < n; i++) {
            byte t = types[i];
            if (baseLevel % 2 == 0) {
                // even (LTR) embedding level
                if (t == R) {
                    levels[i] = (byte) (baseLevel + 1);
                } else if (t == AN || t == EN) {
                    levels[i] = (byte) (baseLevel + 2);
                } else {
                    levels[i] = baseLevel;
                }
            } else {
                // odd (RTL) embedding level
                if (t == L || t == EN || t == AN) {
                    levels[i] = (byte) (baseLevel + 1);
                } else {
                    levels[i] = baseLevel;
                }
            }
        }
        return levels;
    }

    private static boolean isNeutral(byte t) {
        return t == ON || t == WS || t == BN;
    }

    // Maps a resolved type to its directional class for neutral resolution (numbers behave as R).
    private static byte dirOf(byte t) {
        if (t == L) {
            return L;
        }
        return R;
    }

    /// Applies reordering rule L2 to produce the visual order of the characters. Returns an array
    /// `visualToLogical` where element `v` is the logical index of the character drawn at visual position
    /// `v` (left to right on screen).
    static int[] reorderVisual(byte[] levels) {
        int n = levels.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        if (n == 0) {
            return order;
        }
        byte highest = 0;
        byte lowestOdd = Byte.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            byte lv = levels[i];
            if (lv > highest) {
                highest = lv;
            }
            if ((lv & 1) != 0 && lv < lowestOdd) {
                lowestOdd = lv;
            }
        }
        if (lowestOdd == Byte.MAX_VALUE) {
            // all even levels: already in logical == visual order
            return order;
        }
        for (int level = highest; level >= lowestOdd; level--) {
            int i = 0;
            while (i < n) {
                if (levels[order[i]] >= level) {
                    int start = i;
                    while (i < n && levels[order[i]] >= level) {
                        i++;
                    }
                    reverse(order, start, i - 1);
                } else {
                    i++;
                }
            }
        }
        return order;
    }

    private static void reverse(int[] a, int from, int to) {
        while (from < to) {
            int t = a[from];
            a[from] = a[to];
            a[to] = t;
            from++;
            to--;
        }
    }

    /// True if the line, laid out with the given base direction, is purely left-to-right (no reordering
    /// needed). This lets the editor keep its fast LTR path for the common case.
    static boolean isTrivialLtr(String text, boolean baseRtl) {
        if (baseRtl) {
            return text.length() == 0;
        }
        for (int i = 0; i < text.length(); i++) {
            byte t = typeOf(text.charAt(i));
            if (t == R || t == AL || t == AN) {
                return false;
            }
        }
        return true;
    }
}
