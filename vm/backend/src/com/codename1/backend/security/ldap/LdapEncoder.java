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
package com.codename1.backend.security.ldap;

/// Escaping for what a user types that ends up inside a filter or a
/// distinguished name. Without it a user name of `*` matches the first entry
/// of the directory, and `admin)(|(uid=*` rewrites the query.
public final class LdapEncoder {
    private LdapEncoder() {
    }

    /// `value` as an assertion value of a filter (RFC 4515 section 3): `*`,
    /// `(`, `)`, `\` and NUL as `\XX`.
    public static String filterEncode(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '*' || c == '(' || c == ')' || c == '\\' || c == 0) {
                out.append('\\').append(HEX.charAt((c >> 4) & 0xf)).append(HEX.charAt(c & 0xf));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// `value` as one attribute value of a distinguished name (RFC 4514
    /// section 2.4).
    public static String nameEncode(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean edge = (i == 0 && (c == ' ' || c == '#'))
                    || (i == value.length() - 1 && c == ' ');
            if (c == 0) {
                out.append("\\00");
            } else if (edge || c == '"' || c == '+' || c == ',' || c == ';' || c == '<'
                    || c == '>' || c == '\\' || c == '=') {
                out.append('\\').append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final String HEX = "0123456789abcdef";

    /// ASCII-only lower case; attribute names are ASCII, and
    /// `String.toLowerCase` folds `I` to a dotless i on a Turkish device.
    static String asciiLower(String s) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = new StringBuilder(s.length());
                    out.append(s, 0, i);
                }
                out.append((char) (c + 32));
            } else if (out != null) {
                out.append(c);
            }
        }
        return out == null ? s : out.toString();
    }

    /// ASCII-only upper case, for role names made from group names.
    static String asciiUpper(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return out.toString();
    }

    /// `pattern` with `{0}`, `{1}`... replaced by `values` (already escaped).
    static String format(String pattern, String... values) {
        StringBuilder out = new StringBuilder(pattern.length() + 32);
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            int close = c == '{' ? pattern.indexOf('}', i) : -1;
            if (close == i + 2 && pattern.charAt(i + 1) >= '0'
                    && pattern.charAt(i + 1) - '0' < values.length) {
                out.append(values[pattern.charAt(i + 1) - '0']);
                i = close + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /// `value` cut at every `sep`; String.split is a regex the device lacks.
    static String[] split(String value, char sep) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        int from = 0;
        int at;
        while ((at = value.indexOf(sep, from)) >= 0) {
            out.add(value.substring(from, at));
            from = at + 1;
        }
        out.add(value.substring(from));
        return out.toArray(new String[out.size()]);
    }
}
