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
package com.codename1.flutter.util;

/**
 * ASCII string helpers — package:collection's {@code compareAsciiUpperCase}.
 * The settings page sorts locale display names with it.
 */
public final class AsciiUtil {

    private AsciiUtil() {
    }

    /**
     * Dart's {@code compareAsciiUpperCase(a, b)}: compares two strings by
     * upper-casing ASCII letters only (a-z -&gt; A-Z), leaving all other code
     * units untouched. Returns a negative, zero, or positive int like
     * {@link String#compareTo}.
     */
    public static long compareAsciiUpperCase(String a, String b) {
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }
        int len = Math.min(a.length(), b.length());
        for (int i = 0; i < len; i++) {
            int ca = toUpperAscii(a.charAt(i));
            int cb = toUpperAscii(b.charAt(i));
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    private static int toUpperAscii(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - ('a' - 'A');
        }
        return c;
    }
}
