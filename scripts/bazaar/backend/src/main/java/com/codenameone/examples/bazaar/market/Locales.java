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
package com.codenameone.examples.bazaar.market;

import com.codename1.backend.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

/// Language tags as the storefront uses them: `de` or `de-CH`, folded by
/// hand (a locale-sensitive fold would turn the `I` of `IT` into a dotless
/// i on a Turkish server), and the languages an `Accept-Language` header
/// asks for, in its order of preference.
public final class Locales {
    private Locales() {
    }

    /// `text` as `ll` or `ll-CC`; anything else answers 400.
    public static String normalize(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() != 2 && !(t.length() == 5 && (t.charAt(2) == '-' || t.charAt(2) == '_'))) {
            throw new ResponseStatusException(400, "A language is like de or de-CH");
        }
        StringBuilder out = new StringBuilder(5);
        for (int iter = 0; iter < 2; iter++) {
            out.append(letter(t.charAt(iter), false));
        }
        if (t.length() == 5) {
            out.append('-').append(letter(t.charAt(3), true)).append(letter(t.charAt(4), true));
        }
        return out.toString();
    }

    /// [#normalize], or null for a tag that is not one.
    public static String orNull(String text) {
        try {
            return normalize(text);
        } catch (ResponseStatusException bad) {
            return null;
        }
    }

    private static char letter(char c, boolean upper) {
        char l = c >= 'A' && c <= 'Z' ? (char) (c - 'A' + 'a') : c;
        if (l < 'a' || l > 'z') {
            throw new ResponseStatusException(400, "A language is like de or de-CH");
        }
        return upper ? (char) (l - 'a' + 'A') : l;
    }

    /// The language part: `de` of `de-CH`.
    public static String language(String locale) {
        return locale.length() > 2 ? locale.substring(0, 2) : locale;
    }

    /// The tags of an `Accept-Language` header, best first (ties keep their
    /// order); a `*`, a malformed tag or a `q=0` one is left out.
    public static List<String> accepted(String header) {
        List<String> tags = new ArrayList<String>();
        List<Integer> weights = new ArrayList<Integer>();
        if (header == null) {
            return tags;
        }
        int start = 0;
        while (start < header.length() && tags.size() < 20) {
            int comma = header.indexOf(',', start);
            int end = comma < 0 ? header.length() : comma;
            String part = header.substring(start, end).trim();
            start = end + 1;
            int semi = part.indexOf(';');
            String tag = (semi < 0 ? part : part.substring(0, semi)).trim();
            int q = 1000;
            if (semi >= 0) {
                q = quality(part.substring(semi + 1).trim());
            }
            String norm = orNull(tag);
            if (norm == null && tag.length() > 2 && tag.charAt(2) == '-') {
                // de-Latn-CH and the like: the language alone.
                norm = orNull(tag.substring(0, 2));
            }
            if (norm == null || q <= 0) {
                continue;
            }
            int at = tags.size();
            while (at > 0 && weights.get(at - 1).intValue() < q) {
                at--;
            }
            tags.add(at, norm);
            weights.add(at, Integer.valueOf(q));
        }
        return tags;
    }

    /// `q=0.8` as 800; a malformed weight is 0.
    static int quality(String param) {
        if (!param.startsWith("q=") && !param.startsWith("Q=")) {
            return 1000;
        }
        String v = param.substring(2).trim();
        if (v.length() == 0 || v.length() > 5) {
            return 0;
        }
        int whole = v.charAt(0) - '0';
        if (whole < 0 || whole > 1) {
            return 0;
        }
        int frac = 0;
        int digits = 0;
        if (v.length() > 1) {
            if (v.charAt(1) != '.') {
                return 0;
            }
            for (int iter = 2; iter < v.length(); iter++) {
                char c = v.charAt(iter);
                if (c < '0' || c > '9') {
                    return 0;
                }
                frac = frac * 10 + (c - '0');
                digits++;
            }
        }
        while (digits < 3) {
            frac *= 10;
            digits++;
        }
        return Math.min(1000, whole * 1000 + frac);
    }
}
