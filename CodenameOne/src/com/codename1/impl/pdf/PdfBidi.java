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
package com.codename1.impl.pdf;

/// The Unicode bidirectional algorithm (UAX #9) for one line of text:
/// resolves embedding levels and orders the line for display.
///
/// Implemented: the paragraph level (P2, P3), the weak types (W1-W7), the
/// neutrals (N1, N2), the implicit levels (I1, I2), trailing white space
/// (L1), reordering (L2) and mirroring (L4). Explicit embeddings,
/// overrides and isolates (U+202A-202E, U+2066-2069) are ignored, as if
/// removed (X9); the marks U+200E, U+200F and U+061C are honoured. That
/// covers text mixing right-to-left and left-to-right scripts and numbers
/// without explicit formatting characters.
@com.codename1.impl.SharedWithBackend
public final class PdfBidi {
    /// Pick the direction from the first strong character.
    public static final int AUTO = -1;
    /// Left to right.
    public static final int LTR = 0;
    /// Right to left.
    public static final int RTL = 1;

    static final byte L = 0;
    static final byte R = 1;
    static final byte AL = 2;
    static final byte EN = 3;
    static final byte ES = 4;
    static final byte ET = 5;
    static final byte AN = 6;
    static final byte CS = 7;
    static final byte NSM = 8;
    static final byte BN = 9;
    static final byte B = 10;
    static final byte S = 11;
    static final byte WS = 12;
    static final byte ON = 13;

    private static final String MIRRORS = "()<>[]{}\u00ab\u00bb\u2039\u203a\u2045\u2046\u207d\u207e\u208d\u208e"
            + "\u2208\u220b\u2264\u2265\u2266\u2267\u226a\u226b\u2282\u2283\u2286\u2287\u3008\u3009\u300a\u300b"
            + "\u300c\u300d\u300e\u300f\u3010\u3011\uff08\uff09\uff3b\uff3d\uff5b\uff5d";

    private PdfBidi() {
    }

    /// The bidirectional class of `c`.
    static byte type(char c) {
        if (c < 0x80) {
            if (c >= '0' && c <= '9') {
                return EN;
            }
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
                return L;
            }
            switch (c) {
                case ' ':
                    return WS;
                case '\t':
                case 0x0b:
                case 0x1f:
                    return S;
                case '\n':
                case '\r':
                case 0x1c:
                case 0x1d:
                case 0x1e:
                    return B;
                case '+':
                case '-':
                    return ES;
                case '#':
                case '$':
                case '%':
                    return ET;
                case ',':
                case '.':
                case '/':
                case ':':
                    return CS;
                default:
                    return c < 0x20 || c == 0x7f ? BN : ON;
            }
        }
        if (c < 0x0300) {
            if (c == 0xa0) {
                return CS;
            }
            if (c >= 0xa2 && c <= 0xa5 || c == 0xb0 || c == 0xb1) {
                return ET;
            }
            if (c == 0xad) {
                return BN;
            }
            if (c >= 0xa1 && c <= 0xbf && c != 0xaa && c != 0xb5 && c != 0xba || c == 0xd7 || c == 0xf7) {
                return ON;
            }
            return c < 0xa0 ? BN : L;
        }
        if (c <= 0x036f || c >= 0x0483 && c <= 0x0489 || c >= 0x20d0 && c <= 0x20ff
                || c >= 0xfe00 && c <= 0xfe0f || c >= 0xfe20 && c <= 0xfe2f) {
            return NSM;
        }
        if (c >= 0x0590 && c <= 0x05ff) {
            if (c >= 0x0591 && c <= 0x05bd || c == 0x05bf || c == 0x05c1 || c == 0x05c2 || c == 0x05c4
                    || c == 0x05c5 || c == 0x05c7) {
                return NSM;
            }
            return R;
        }
        if (c >= 0x0600 && c <= 0x07bf) {
            if (c >= 0x0660 && c <= 0x0669 || c == 0x066b || c == 0x066c || c <= 0x0605
                    || c == 0x06dd) {
                return AN;
            }
            if (c >= 0x06f0 && c <= 0x06f9) {
                return EN;
            }
            if (c == 0x060c) {
                return CS;
            }
            if (c == 0x066a) {
                return ET;
            }
            if (c >= 0x0610 && c <= 0x061a || c >= 0x064b && c <= 0x065f || c == 0x0670
                    || c >= 0x06d6 && c <= 0x06dc || c >= 0x06df && c <= 0x06e4 || c == 0x06e7 || c == 0x06e8
                    || c >= 0x06ea && c <= 0x06ed || c == 0x0711 || c >= 0x0730 && c <= 0x074a
                    || c >= 0x07a6 && c <= 0x07b0) {
                return NSM;
            }
            return AL;
        }
        if (c >= 0x07c0 && c <= 0x085f) {
            return R;
        }
        if (c >= 0x0860 && c <= 0x08ff) {
            return c >= 0x08d3 ? NSM : AL;
        }
        if (c >= 0x2000 && c <= 0x206f) {
            if (c <= 0x200a || c == 0x2028) {
                return WS;
            }
            if (c == 0x2029) {
                return B;
            }
            if (c == 0x200e) {
                return L;
            }
            if (c == 0x200f) {
                return R;
            }
            if (c >= 0x2030 && c <= 0x2034) {
                return ET;
            }
            if (c == 0x202f) {
                return CS;
            }
            if (c <= 0x200d || c >= 0x202a && c <= 0x202e || c >= 0x2060) {
                return BN;
            }
            return ON;
        }
        if (c >= 0x20a0 && c <= 0x20cf) {
            return ET;
        }
        if (c >= 0x2190 && c <= 0x2bff || c >= 0x3001 && c <= 0x3003 || c >= 0x3008 && c <= 0x3011
                || c >= 0xfe50 && c <= 0xfe6f && c != 0xfe52 && c != 0xfe55 || c >= 0xff01 && c <= 0xff0f
                && c != 0xff03 && c != 0xff04 && c != 0xff05) {
            return c == 0x2212 ? ES : ON;
        }
        if (c == 0x3000) {
            return WS;
        }
        if (c >= 0xff10 && c <= 0xff19) {
            return EN;
        }
        if (c >= 0xfb1d && c <= 0xfb4f) {
            return c == 0xfb1e ? NSM : R;
        }
        if (c >= 0xfb50 && c <= 0xfdff || c >= 0xfe70 && c <= 0xfefe) {
            return AL;
        }
        if (c == 0xfeff) {
            return BN;
        }
        return L;
    }

    /// True when `text` has a right-to-left character, so it needs
    /// reordering.
    public static boolean hasRtl(String text) {
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c >= 0x0590 && c <= 0x08ff || c >= 0xfb1d && c <= 0xfdff || c >= 0xfe70 && c <= 0xfefe
                    || c == 0x200f) {
                return true;
            }
        }
        return false;
    }

    /// The direction of the first strong character of `text`, `def` when
    /// there is none (P2, P3).
    public static int direction(String text, int def) {
        int n = text.length();
        for (int i = 0; i < n; i++) {
            byte t = type(text.charAt(i));
            if (t == L) {
                return LTR;
            }
            if (t == R || t == AL) {
                return RTL;
            }
        }
        return def;
    }

    /// [#RTL] when most strong characters of `text` are right to left,
    /// else [#LTR].
    public static int majority(String text) {
        int balance = 0;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            byte t = type(text.charAt(i));
            if (t == L) {
                balance--;
            } else if (t == R || t == AL) {
                balance++;
            }
        }
        return balance > 0 ? RTL : LTR;
    }

    /// The resolved embedding level of every character of the line `text`
    /// in a paragraph of direction `dir` ([#AUTO], [#LTR] or [#RTL]).
    public static byte[] levels(String text, int dir) {
        int n = text.length();
        int base = dir == AUTO ? direction(text, LTR) : dir;
        byte[] t = new byte[n];
        byte[] orig = new byte[n];
        for (int i = 0; i < n; i++) {
            t[i] = type(text.charAt(i));
            orig[i] = t[i];
        }
        byte sos = base == RTL ? R : L;
        // X9: embedding controls and BN are removed; treat them as the
        // type before them so they never break a sequence.
        // W1
        byte prev = sos;
        for (int i = 0; i < n; i++) {
            if (t[i] == NSM || t[i] == BN) {
                t[i] = prev;
            } else {
                prev = t[i];
            }
        }
        // W2, W3
        byte strong = sos;
        for (int i = 0; i < n; i++) {
            byte c = t[i];
            if (c == EN && strong == AL) {
                t[i] = AN;
            }
            if (c == L || c == R || c == AL) {
                strong = c;
            }
        }
        for (int i = 0; i < n; i++) {
            if (t[i] == AL) {
                t[i] = R;
            }
        }
        // W4
        for (int i = 1; i < n - 1; i++) {
            if (t[i] == ES && t[i - 1] == EN && t[i + 1] == EN) {
                t[i] = EN;
            } else if (t[i] == CS && t[i - 1] == EN && t[i + 1] == EN) {
                t[i] = EN;
            } else if (t[i] == CS && t[i - 1] == AN && t[i + 1] == AN) {
                t[i] = AN;
            }
        }
        // W5
        for (int i = 0; i < n; i++) {
            if (t[i] == ET) {
                int end = i;
                while (end < n && t[end] == ET) {
                    end++;
                }
                boolean en = i > 0 && t[i - 1] == EN || end < n && t[end] == EN;
                if (en) {
                    for (int k = i; k < end; k++) {
                        t[k] = EN;
                    }
                }
                i = end - 1;
            }
        }
        // W6
        for (int i = 0; i < n; i++) {
            if (t[i] == ES || t[i] == ET || t[i] == CS) {
                t[i] = ON;
            }
        }
        // W7
        strong = sos;
        for (int i = 0; i < n; i++) {
            if (t[i] == EN && strong == L) {
                t[i] = L;
            }
            if (t[i] == L || t[i] == R) {
                strong = t[i];
            }
        }
        // N1, N2
        for (int i = 0; i < n; i++) {
            if (isNeutral(t[i])) {
                int end = i;
                while (end < n && isNeutral(t[end])) {
                    end++;
                }
                byte before = i == 0 ? sos : strongOf(t[i - 1]);
                byte after = end == n ? sos : strongOf(t[end]);
                byte v = before == after ? before : sos;
                for (int k = i; k < end; k++) {
                    t[k] = v;
                }
                i = end - 1;
            }
        }
        // I1, I2
        byte[] lv = new byte[n];
        for (int i = 0; i < n; i++) {
            byte c = t[i];
            if (base == LTR) {
                lv[i] = (byte) (c == R ? 1 : c == AN || c == EN ? 2 : 0);
            } else {
                lv[i] = (byte) (c == L || c == EN || c == AN ? 2 : 1);
            }
        }
        // L1: separators and trailing white space go to the paragraph level.
        boolean trailing = true;
        for (int i = n - 1; i >= 0; i--) {
            byte o = orig[i];
            if (o == S || o == B) {
                lv[i] = (byte) base;
                trailing = true;
            } else if (o == WS || o == BN) {
                if (trailing) {
                    lv[i] = (byte) base;
                }
            } else {
                trailing = false;
            }
        }
        return lv;
    }

    private static boolean isNeutral(byte t) {
        return t == ON || t == WS || t == S || t == B || t == BN;
    }

    private static byte strongOf(byte t) {
        return t == L ? L : R;
    }

    /// The display order of `levels` (L2): element i of the result is the
    /// logical index of the i-th character from the left.
    public static int[] order(byte[] levels) {
        int n = levels.length;
        int[] idx = new int[n];
        int max = 0;
        int minOdd = 127;
        for (int i = 0; i < n; i++) {
            idx[i] = i;
            max = Math.max(max, levels[i]);
            if ((levels[i] & 1) != 0) {
                minOdd = Math.min(minOdd, levels[i]);
            }
        }
        for (int level = max; level >= minOdd && level > 0; level--) {
            int i = 0;
            while (i < n) {
                if (levels[idx[i]] >= level) {
                    int end = i;
                    while (end < n && levels[idx[end]] >= level) {
                        end++;
                    }
                    for (int a = i, b = end - 1; a < b; a++, b--) {
                        int tmp = idx[a];
                        idx[a] = idx[b];
                        idx[b] = tmp;
                    }
                    i = end;
                } else {
                    i++;
                }
            }
        }
        return idx;
    }

    /// The mirror image of `c` for right-to-left display (L4), or `c`.
    public static char mirror(char c) {
        int i = MIRRORS.indexOf(c);
        if (i < 0) {
            return c;
        }
        return MIRRORS.charAt(i ^ 1);
    }

    /// `text` in display order for a paragraph of direction `dir`, with
    /// mirrored characters in right-to-left runs. Used to restore the
    /// reading order of text extracted in display order, since the
    /// reordering is its own inverse for a line without explicit controls.
    public static String reorder(String text, int dir) {
        if (!hasRtl(text)) {
            return text;
        }
        byte[] lv = levels(text, dir);
        int[] idx = order(lv);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < idx.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            char c = text.charAt(idx[i]);
            sb.append((lv[idx[i]] & 1) != 0 ? mirror(c) : c);
        }
        return sb.toString();
    }
}
