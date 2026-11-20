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
package com.codenameone.examples.bazaar.cart;

import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;

/// Small helpers the commerce slices share: codes as people type them, and
/// the caller when signing in is optional.
public final class Codes {
    private Codes() {
    }

    /// A code with spaces and dashes dropped and ASCII letters upper case.
    /// Folded by hand: `toUpperCase()` follows the device's locale, and a
    /// Turkish one turns `i` into a dotted capital no code contains.
    public static String normalize(String code) {
        if (code == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(code.length());
        for (int iter = 0; iter < code.length(); iter++) {
            char c = code.charAt(iter);
            if (c == ' ' || c == '-' || c == '\t') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            out.append(c);
        }
        return out.toString();
    }

    /// The signed-in caller, or null for a guest.
    public static String callerOrNull() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || !who.isAuthenticated() || who.getName() == null
                || "anonymousUser".equals(who.getName())) {
            return null;
        }
        return who.getName();
    }

    /// `text` trimmed and cut to `max` characters; empty for null.
    public static String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
