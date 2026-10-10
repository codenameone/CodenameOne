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
package com.codename1.compat.jdk;

/// The glob syntax of `Files.newDirectoryStream(dir, glob)`, matched against
/// one file name.
final class Glob {

    private Glob() {
    }

    /// Whether `name` from `ni` on matches `glob` between `gi` and `gEnd`.
    static boolean matches(String glob, int gi, int gEnd, String name, int ni) {
        int n = name.length();
        while (gi < gEnd) {
            char g = glob.charAt(gi);
            if (g == '*') {
                while (gi < gEnd && glob.charAt(gi) == '*') {
                    gi++;
                }
                if (gi == gEnd) {
                    return true;
                }
                for (int k = ni; k <= n; k++) {
                    if (matches(glob, gi, gEnd, name, k)) {
                        return true;
                    }
                }
                return false;
            }
            if (g == '{') {
                int close = closing(glob, gi, gEnd);
                if (close < 0) {
                    throw new IllegalArgumentException("Missing '}' in glob: " + glob);
                }
                String rest = glob.substring(close + 1, gEnd);
                int start = gi + 1;
                for (int i = start; i <= close; i++) {
                    char c = glob.charAt(i);
                    if (c == '\\') {
                        i++;
                    } else if (c == ',' || i == close) {
                        String alternative = glob.substring(start, i) + rest;
                        if (matches(alternative, 0, alternative.length(), name, ni)) {
                            return true;
                        }
                        start = i + 1;
                    }
                }
                return false;
            }
            if (ni >= n) {
                return false;
            }
            char c = name.charAt(ni);
            if (g == '?') {
                gi++;
            } else if (g == '[') {
                int close = gi + 1;
                boolean negate = close < gEnd && glob.charAt(close) == '!';
                if (negate) {
                    close++;
                }
                int first = close;
                boolean found = false;
                while (close < gEnd && (glob.charAt(close) != ']' || close == first)) {
                    char lo = glob.charAt(close);
                    char hi = lo;
                    if (close + 2 < gEnd && glob.charAt(close + 1) == '-' && glob.charAt(close + 2) != ']') {
                        hi = glob.charAt(close + 2);
                        close += 2;
                    }
                    found = found || (c >= lo && c <= hi);
                    close++;
                }
                if (close >= gEnd) {
                    throw new IllegalArgumentException("Missing ']' in glob: " + glob);
                }
                if (found == negate) {
                    return false;
                }
                gi = close + 1;
            } else {
                if (g == '\\') {
                    gi++;
                    if (gi >= gEnd) {
                        throw new IllegalArgumentException("No character to escape in glob: " + glob);
                    }
                    g = glob.charAt(gi);
                }
                if (g != c) {
                    return false;
                }
                gi++;
            }
            ni++;
        }
        return ni == n;
    }

    private static int closing(String glob, int open, int end) {
        for (int i = open + 1; i < end; i++) {
            char c = glob.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '}') {
                return i;
            }
        }
        return -1;
    }
}
