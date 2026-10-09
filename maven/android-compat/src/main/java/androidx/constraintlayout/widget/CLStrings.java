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
package androidx.constraintlayout.widget;

import java.util.ArrayList;

/// Codename One: the string operations ConstraintLayout's sources use that
/// the Codename One runtime does not provide (no regular-expression split,
/// no `Integer.decode`, no locale-free case folding).
final class CLStrings {

    private CLStrings() {
    }

    /// `s.split(String.valueOf(c))` for a plain character: trailing empty
    /// strings are dropped, as `String.split` drops them.
    static String[] split(String s, char c) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        int n = out.size();
        while (n > 0 && out.get(n - 1).length() == 0) {
            n--;
        }
        if (n == 0) {
            return new String[] {""};
        }
        String[] r = new String[n];
        for (int i = 0; i < n; i++) {
            r[i] = out.get(i);
        }
        return r;
    }

    /// `Integer.decode`: decimal, `0x`/`#` hexadecimal or `0` octal, with an
    /// optional sign.
    static int decode(String s) {
        String t = s.trim();
        int i = 0;
        boolean negative = false;
        if (t.startsWith("-") || t.startsWith("+")) {
            negative = t.charAt(0) == '-';
            i = 1;
        }
        int radix = 10;
        if (t.regionMatches(true, i, "0x", 0, 2)) {
            radix = 16;
            i += 2;
        } else if (t.startsWith("#", i)) {
            radix = 16;
            i += 1;
        } else if (t.startsWith("0", i) && t.length() > i + 1) {
            radix = 8;
            i += 1;
        }
        int v = Integer.parseInt(t.substring(i), radix);
        return negative ? -v : v;
    }

    /// Lower-cases ASCII letters only, independent of the device locale.
    static String asciiLower(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + ('a' - 'A')));
            }
        }
        return sb == null ? s : sb.toString();
    }

    /// `s.matches(regex)`: the whole string must match.
    static boolean matches(String s, String regex) {
        com.codename1.util.regex.RE re = new com.codename1.util.regex.RE("^(" + regex + ")$");
        return re.match(s);
    }
}
