/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.foundation;

/**
 * User-perceived characters (grapheme clusters), which is what Flutter counts for a
 * text field's {@code maxLength} and its counter -- not UTF-16 units. Counting units
 * cut an emoji in half at a limit and reported a flag as four characters.
 *
 * <p>The segmentation covers what a text field meets in practice: surrogate pairs,
 * combining marks, variation selectors, emoji skin-tone modifiers, zero-width-joiner
 * sequences (families, professions), regional-indicator flag pairs, tag sequences
 * (subdivision flags) and CR LF. It is not the full Unicode UAX #29 table.</p>
 */
public final class Characters {

    private Characters() {
    }

    /** How many user-perceived characters {@code s} holds. */
    public static int count(String s) {
        if (s == null) {
            return 0;
        }
        int n = 0;
        int i = 0;
        while (i < s.length()) {
            i = next(s, i);
            n++;
        }
        return n;
    }

    /** {@code s} cut to at most {@code max} user-perceived characters, never mid-character. */
    public static String take(String s, long max) {
        if (s == null || max < 0) {
            return s;
        }
        int i = 0;
        long n = 0;
        while (i < s.length() && n < max) {
            i = next(s, i);
            n++;
        }
        return s.substring(0, i);
    }

    /** The index just past the cluster that starts at {@code i}. */
    static int next(String s, int i) {
        int len = s.length();
        int cp = codePointAt(s, i);
        int j = i + Character.charCount(cp);
        if (cp == '\r' && j < len && s.charAt(j) == '\n') {
            return j + 1;
        }
        boolean regional = isRegionalIndicator(cp);
        if (regional && j < len && isRegionalIndicator(codePointAt(s, j))) {
            j += Character.charCount(codePointAt(s, j));   // a flag is two indicators
        }
        while (j < len) {
            int c = codePointAt(s, j);
            if (isExtender(c)) {
                j += Character.charCount(c);
            } else if (c == 0x200D && j + 1 < len) {
                // Zero-width joiner: the next character belongs to this cluster too.
                j += 1;
                j += Character.charCount(codePointAt(s, j));
            } else {
                break;
            }
        }
        return j;
    }

    /**
     * The code point at {@code i}, decoding a surrogate pair by hand: String.codePointAt
     * is not in ParparVM's JavaAPI or CLDC11.
     */
    private static int codePointAt(String s, int i) {
        char c = s.charAt(i);
        if (c >= 0xD800 && c <= 0xDBFF && i + 1 < s.length()) {
            char d = s.charAt(i + 1);
            if (d >= 0xDC00 && d <= 0xDFFF) {
                return ((c - 0xD800) << 10) + (d - 0xDC00) + 0x10000;
            }
        }
        return c;
    }

    private static boolean isRegionalIndicator(int c) {
        return c >= 0x1F1E6 && c <= 0x1F1FF;
    }

    /** Characters that never start a cluster: they modify the one before them. */
    private static boolean isExtender(int c) {
        return (c >= 0x0300 && c <= 0x036F)        // combining diacritical marks
                || (c >= 0x1AB0 && c <= 0x1AFF)
                || (c >= 0x1DC0 && c <= 0x1DFF)
                || (c >= 0x20D0 && c <= 0x20FF)
                || (c >= 0xFE20 && c <= 0xFE2F)
                || (c >= 0xFE00 && c <= 0xFE0F)    // variation selectors
                || (c >= 0xE0100 && c <= 0xE01EF)
                || (c >= 0x1F3FB && c <= 0x1F3FF)  // emoji skin-tone modifiers
                || (c >= 0xE0020 && c <= 0xE007F); // tag characters (subdivision flags)
    }
}
