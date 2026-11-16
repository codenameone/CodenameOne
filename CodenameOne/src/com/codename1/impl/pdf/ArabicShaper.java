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

/// Arabic joining: which contextual form each letter takes, the Unicode
/// presentation forms for fonts that map them, and the mapping back from
/// presentation forms to letters for text extraction.
@com.codename1.impl.SharedWithBackend
public final class ArabicShaper {
    /// The letter stands alone.
    public static final int ISOLATED = 0;
    /// The letter joins the one before it (in reading order).
    public static final int FINAL = 1;
    /// The letter joins the one after it.
    public static final int INITIAL = 2;
    /// The letter joins on both sides.
    public static final int MEDIAL = 3;

    // letter, isolated presentation form, number of forms (2: isolated and
    // final, the letter joins only the one before it; 4: all four).
    private static final int[] TABLE = {
        0x0622, 0xfe81, 2, 0x0623, 0xfe83, 2, 0x0624, 0xfe85, 2, 0x0625, 0xfe87, 2,
        0x0626, 0xfe89, 4, 0x0627, 0xfe8d, 2, 0x0628, 0xfe8f, 4, 0x0629, 0xfe93, 2,
        0x062a, 0xfe95, 4, 0x062b, 0xfe99, 4, 0x062c, 0xfe9d, 4, 0x062d, 0xfea1, 4,
        0x062e, 0xfea5, 4, 0x062f, 0xfea9, 2, 0x0630, 0xfeab, 2, 0x0631, 0xfead, 2,
        0x0632, 0xfeaf, 2, 0x0633, 0xfeb1, 4, 0x0634, 0xfeb5, 4, 0x0635, 0xfeb9, 4,
        0x0636, 0xfebd, 4, 0x0637, 0xfec1, 4, 0x0638, 0xfec5, 4, 0x0639, 0xfec9, 4,
        0x063a, 0xfecd, 4, 0x0641, 0xfed1, 4, 0x0642, 0xfed5, 4, 0x0643, 0xfed9, 4,
        0x0644, 0xfedd, 4, 0x0645, 0xfee1, 4, 0x0646, 0xfee5, 4, 0x0647, 0xfee9, 4,
        0x0648, 0xfeed, 2, 0x0649, 0xfeef, 2, 0x064a, 0xfef1, 4,
        0x0671, 0xfb50, 2, 0x0679, 0xfb66, 4, 0x067a, 0xfb5e, 4, 0x067b, 0xfb52, 4,
        0x067e, 0xfb56, 4, 0x067f, 0xfb62, 4, 0x0680, 0xfb5a, 4, 0x0683, 0xfb76, 4,
        0x0684, 0xfb72, 4, 0x0686, 0xfb7a, 4, 0x0687, 0xfb7e, 4, 0x0688, 0xfb88, 2,
        0x068c, 0xfb84, 2, 0x068d, 0xfb82, 2, 0x068e, 0xfb86, 2, 0x0691, 0xfb8c, 2,
        0x0698, 0xfb8a, 2, 0x06a4, 0xfb6a, 4, 0x06a6, 0xfb6e, 4, 0x06a9, 0xfb8e, 4,
        0x06ad, 0xfbd3, 4, 0x06af, 0xfb92, 4, 0x06b1, 0xfb9a, 4, 0x06b3, 0xfb96, 4,
        0x06ba, 0xfb9e, 2, 0x06bb, 0xfba0, 4, 0x06be, 0xfbaa, 4, 0x06c0, 0xfba4, 2,
        0x06c1, 0xfba6, 4, 0x06c5, 0xfbe0, 2, 0x06c6, 0xfbd9, 2, 0x06c7, 0xfbd7, 2,
        0x06c8, 0xfbdb, 2, 0x06c9, 0xfbe2, 2, 0x06cb, 0xfbde, 2, 0x06cc, 0xfbfc, 4,
        0x06d0, 0xfbe4, 4, 0x06d2, 0xfbae, 2, 0x06d3, 0xfbb0, 2,
    };

    /// Lam followed by these alefs forms a ligature; isolated forms of
    /// the ligatures (the final form is the next code).
    private static final int[] LAM_ALEF = {0x0622, 0xfef5, 0x0623, 0xfef7, 0x0625, 0xfef9, 0x0627, 0xfefb};

    private ArabicShaper() {
    }

    private static int find(char c) {
        if (c < 0x0622 || c > 0x06d3) {
            return -1;
        }
        for (int i = 0; i < TABLE.length; i += 3) {
            if (TABLE[i] == c) {
                return i;
            }
        }
        return -1;
    }

    /// True for marks that do not take part in joining.
    static boolean transparent(char c) {
        return PdfBidi.type(c) == PdfBidi.NSM;
    }

    private static boolean joinsAfter(char c) {
        if (c == 0x0640 || c == 0x200d) {
            return true;
        }
        int i = find(c);
        return i >= 0 && TABLE[i + 2] == 4;
    }

    private static boolean joinsBefore(char c) {
        return c == 0x0640 || c == 0x200d || find(c) >= 0;
    }

    /// True when `text` has letters that join.
    public static boolean needsShaping(String text) {
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c >= 0x0620 && c <= 0x06ff || c >= 0x0750 && c <= 0x077f || c >= 0x08a0 && c <= 0x08ff) {
                return true;
            }
        }
        return false;
    }

    /// The form of every character of `text`, which is in logical order.
    public static int[] forms(String text) {
        int n = text.length();
        int[] f = new int[n];
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (transparent(c) || !joinsBefore(c)) {
                continue;
            }
            int p = i - 1;
            while (p >= 0 && transparent(text.charAt(p))) {
                p--;
            }
            int q = i + 1;
            while (q < n && transparent(text.charAt(q))) {
                q++;
            }
            boolean before = p >= 0 && joinsAfter(text.charAt(p));
            boolean after = q < n && joinsAfter(c) && joinsBefore(text.charAt(q));
            f[i] = before ? (after ? MEDIAL : FINAL) : (after ? INITIAL : ISOLATED);
        }
        return f;
    }

    /// The presentation form of `c` in form `form`, or `c` itself.
    public static char presentation(char c, int form) {
        int i = find(c);
        if (i < 0) {
            return c;
        }
        int forms = TABLE[i + 2];
        int f = form;
        if (forms == 2) {
            f = form == FINAL || form == MEDIAL ? FINAL : ISOLATED;
        }
        return (char) (TABLE[i + 1] + f);
    }

    /// The lam-alef ligature for `alef` after a lam in form `lamForm`
    /// (isolated or final), or 0 when `alef` is not one that ligates.
    public static char lamAlef(char alef, int lamForm) {
        for (int i = 0; i < LAM_ALEF.length; i += 2) {
            if (LAM_ALEF[i] == alef) {
                boolean joined = lamForm == FINAL || lamForm == MEDIAL;
                return (char) (LAM_ALEF[i + 1] + (joined ? 1 : 0));
            }
        }
        return 0;
    }

    /// The letters a presentation form stands for, or null when `c` is
    /// not one this class produces.
    public static String letters(char c) {
        if (c >= 0xfef5 && c <= 0xfefc) {
            return "\u0644" + (char) LAM_ALEF[((c - 0xfef5) / 2) * 2];
        }
        if (c < 0xfb50 || c > 0xfefc) {
            return null;
        }
        for (int i = 0; i < TABLE.length; i += 3) {
            int base = TABLE[i + 1];
            if (c >= base && c < base + TABLE[i + 2]) {
                return String.valueOf((char) TABLE[i]);
            }
        }
        if (c == 0xfe80) {
            return "\u0621";
        }
        return null;
    }
}
