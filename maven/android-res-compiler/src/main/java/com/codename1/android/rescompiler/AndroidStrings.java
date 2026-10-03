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
package com.codename1.android.rescompiler;

/// Android's string resource syntax: surrounding double quotes keep
/// whitespace, otherwise runs of whitespace collapse to one space and leading
/// and trailing whitespace is trimmed; a backslash escapes quotes, `@`, `?`,
/// `n`, `t` and four-digit unicode escapes.
public final class AndroidStrings {

    private AndroidStrings() {
    }

    public static String unescape(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(raw.length());
        boolean quoted = false;
        boolean pendingSpace = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' && i + 1 < raw.length()) {
                if (pendingSpace) {
                    out.append(' ');
                    pendingSpace = false;
                }
                char n = raw.charAt(++i);
                switch (n) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        if (i + 4 < raw.length() + 0 && isHex(raw, i + 1, i + 5)) {
                            out.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16));
                            i += 4;
                        } else {
                            out.append('u');
                        }
                        break;
                    default:
                        out.append(n);
                        break;
                }
                continue;
            }
            if (ch == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t')) {
                if (out.length() > 0) {
                    pendingSpace = true;
                }
                continue;
            }
            if (pendingSpace) {
                out.append(' ');
                pendingSpace = false;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static boolean isHex(String s, int from, int to) {
        if (to > s.length()) {
            return false;
        }
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
