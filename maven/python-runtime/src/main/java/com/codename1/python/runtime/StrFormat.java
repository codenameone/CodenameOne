/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

/// `str.format` and `str.format_map`: replacement fields with indices,
/// names, `.attr` and `[key]` lookups, `!r`/`!s`/`!a` conversions and nested
/// format specs.
final class StrFormat {

    private StrFormat() {
    }

    static String format(String fmt, Object[] pos, Object named) {
        StringBuilder sb = new StringBuilder();
        int auto = 0;
        boolean manual = false;
        int n = fmt.length();
        for (int i = 0; i < n; i++) {
            char c = fmt.charAt(i);
            if (c == '}') {
                if (i + 1 < n && fmt.charAt(i + 1) == '}') {
                    sb.append('}');
                    i++;
                    continue;
                }
                throw Py.valueError("Single '}' encountered in format string");
            }
            if (c != '{') {
                sb.append(c);
                continue;
            }
            if (i + 1 < n && fmt.charAt(i + 1) == '{') {
                sb.append('{');
                i++;
                continue;
            }
            int depth = 1;
            int j = i + 1;
            while (j < n && depth > 0) {
                char d = fmt.charAt(j);
                if (d == '{') {
                    depth++;
                } else if (d == '}') {
                    depth--;
                }
                j++;
            }
            if (depth != 0) {
                throw Py.valueError("expected '}' before end of string");
            }
            String field = fmt.substring(i + 1, j - 1);
            i = j - 1;
            String spec = "";
            char conv = 0;
            int colon = indexOutsideBrackets(field, ':');
            if (colon >= 0) {
                spec = field.substring(colon + 1);
                field = field.substring(0, colon);
            }
            int bang = field.indexOf('!');
            if (bang >= 0) {
                conv = field.charAt(bang + 1);
                field = field.substring(0, bang);
            }
            int end = 0;
            while (end < field.length() && field.charAt(end) != '.' && field.charAt(end) != '[') {
                end++;
            }
            String head = field.substring(0, end);
            Object v;
            if (head.length() == 0) {
                if (manual) {
                    throw Py.valueError("cannot switch from manual field specification to automatic field numbering");
                }
                v = posArg(pos, auto++);
            } else if (Character.isDigit(head.charAt(0))) {
                manual = true;
                v = posArg(pos, Integer.parseInt(head));
            } else {
                v = Py.obj(named).getItem(Py.str(head));
            }
            while (end < field.length()) {
                if (field.charAt(end) == '.') {
                    int k = end + 1;
                    while (k < field.length() && field.charAt(k) != '.' && field.charAt(k) != '[') {
                        k++;
                    }
                    v = Py.obj(v).getAttr(field.substring(end + 1, k));
                    end = k;
                } else {
                    int k = field.indexOf(']', end);
                    String key = field.substring(end + 1, k);
                    boolean digits = key.length() > 0;
                    for (int q = 0; q < key.length() && digits; q++) {
                        digits = Character.isDigit(key.charAt(q));
                    }
                    v = Py.obj(v).getItem(digits ? PyInt.of(Long.parseLong(key)) : Py.str(key));
                    end = k + 1;
                }
            }
            if (spec.indexOf('{') >= 0) {
                spec = format(spec, pos, named);
            }
            if (conv == 'r') {
                v = Py.str(Py.toRepr(v));
            } else if (conv == 's') {
                v = Py.str(Py.toStr(v));
            } else if (conv == 'a') {
                v = Py.obj(Builtins.ASCII).call(new Object[] {v}, Py.NO_KW);
            }
            sb.append(Formatting.format(v, spec));
        }
        return sb.toString();
    }

    private static Object posArg(Object[] pos, int i) {
        if (i >= pos.length) {
            throw Py.indexError("Replacement index " + i + " out of range for positional args tuple");
        }
        return pos[i];
    }

    private static int indexOutsideBrackets(String s, char c) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char d = s.charAt(i);
            if (d == '[') {
                depth++;
            } else if (d == ']') {
                depth--;
            } else if (d == c && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
