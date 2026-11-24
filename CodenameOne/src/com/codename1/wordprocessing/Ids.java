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
package com.codename1.wordprocessing;

/// Hands out block ids while a reader builds a document, and the folding helpers the text
/// formats share.
final class Ids {
    private long next = 1;

    String next(char prefix) {
        return String.valueOf(prefix) + next++;
    }

    long peek() {
        return next;
    }

    /// Lower-cases ASCII letters only. `String.toLowerCase` follows the device locale, and on a
    /// Turkish device the `I` of a tag name would not become `i`.
    static String asciiLower(String s) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                char[] chars = s.toCharArray();
                for (int j = i; j < n; j++) {
                    char d = chars[j];
                    if (d >= 'A' && d <= 'Z') {
                        chars[j] = (char) (d + ('a' - 'A'));
                    }
                }
                return new String(chars);
            }
        }
        return s;
    }

    /// The bytes of text that is ASCII by definition, such as Base64.
    static byte[] ascii(String s) {
        byte[] out = new byte[s.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) s.charAt(i);
        }
        return out;
    }

    /// Six lowercase hex digits for an RGB color.
    static String hex(int rgb) {
        return Integer.toHexString(0x1000000 | rgb & 0xffffff).substring(1);
    }

    static String fmt(float f) {
        if (f == (int) f) {
            return String.valueOf((int) f);
        }
        return String.valueOf(f);
    }
}
