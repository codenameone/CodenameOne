/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish;

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
    /// when they are ASCII letters: "Dana Courier" is "DD".
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
