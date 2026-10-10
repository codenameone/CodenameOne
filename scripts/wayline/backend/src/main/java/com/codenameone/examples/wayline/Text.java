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
package com.codenameone.examples.wayline;

import com.codename1.backend.ResponseStatusException;

/// What the app sends, made safe to store: trimmed, bounded, and one of the
/// words a field allows.
public final class Text {
    private Text() {
    }

    /// `value` trimmed, or empty for null; answers 400 when longer than `max`.
    public static String optional(String value, int max, String what) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max) {
            throw new ResponseStatusException(400, "That is too long for " + what);
        }
        return text;
    }

    /// `value` trimmed; answers 400 when it is empty or longer than `max`.
    public static String required(String value, int max, String what) {
        String text = optional(value, max, what);
        if (text.length() == 0) {
            throw new ResponseStatusException(400, "Tell us " + what);
        }
        return text;
    }

    /// `value` when it is one of `allowed`, and `fallback` when it is empty;
    /// answers 400 for anything else. The words are compared as written: they
    /// are the app's own constants, not something a person types.
    public static String oneOf(String value, String[] allowed, String fallback, String what) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return fallback;
        }
        for (int iter = 0; iter < allowed.length; iter++) {
            if (allowed[iter].equals(text)) {
                return text;
            }
        }
        throw new ResponseStatusException(400, "That is not a value for " + what);
    }

    /// The first letters of the first and last words of a name, in upper case
    /// when they are ASCII letters: "Dana Driver" is "DD".
    public static String initials(String name) {
        String text = name == null ? "" : name.trim();
        if (text.length() == 0) {
            return "";
        }
        int last = text.lastIndexOf(' ');
        StringBuilder out = new StringBuilder(2);
        out.append(upper(text.charAt(0)));
        if (last > 0 && last + 1 < text.length()) {
            out.append(upper(text.charAt(last + 1)));
        }
        return out.toString();
    }

    private static char upper(char c) {
        // By hand: toUpperCase() follows the server's locale.
        return c >= 'a' && c <= 'z' ? (char) (c - ('a' - 'A')) : c;
    }
}
