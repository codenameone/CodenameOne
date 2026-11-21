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
package com.codenameone.examples.bazaar.content;

import com.codename1.backend.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

/// Small text helpers of the online store's content: comma lists, handles and
/// storefront paths -- by hand, since `String.split` is a regular expression.
public final class Lists {
    private Lists() {
    }

    /// The trimmed, non-empty parts of a comma separated list.
    public static List<String> split(String text) {
        List<String> out = new ArrayList<String>();
        String t = text == null ? "" : text;
        int start = 0;
        while (start <= t.length()) {
            int comma = t.indexOf(',', start);
            int end = comma < 0 ? t.length() : comma;
            String part = t.substring(start, end).trim();
            if (part.length() > 0) {
                out.add(part);
            }
            start = end + 1;
        }
        return out;
    }

    public static String join(List<String> items) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; items != null && iter < items.size(); iter++) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(items.get(iter));
        }
        return out.toString();
    }

    /// `in` as a handle (`a-z`, `0-9`, `-`, at most `max`), made from a title
    /// when `in` is empty; 400 when nothing is left.
    public static String handle(String in, String title, int max) {
        String src = in == null || in.trim().length() == 0 ? title : in;
        StringBuilder out = new StringBuilder();
        boolean dash = false;
        for (int iter = 0; src != null && iter < src.length() && out.length() < max; iter++) {
            char c = src.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                out.append(c);
                dash = false;
            } else if (out.length() > 0 && !dash) {
                out.append('-');
                dash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.setLength(out.length() - 1);
        }
        if (out.length() == 0) {
            throw new ResponseStatusException(400, "Give it a handle of letters and numbers");
        }
        return out.toString();
    }

    /// Text cut to `max` characters, trimmed, never null.
    public static String cut(String text, int max) {
        String t = text == null ? "" : text.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    /// A path on this site: starts with one `/`, no scheme, no backslash, no
    /// control characters or spaces; null when it is not one.
    public static String sitePath(String in) {
        String p = in == null ? "" : in.trim();
        if (p.length() < 1 || p.length() > 500 || p.charAt(0) != '/') {
            return null;
        }
        if (p.length() > 1 && (p.charAt(1) == '/' || p.charAt(1) == '\\')) {
            return null;
        }
        for (int iter = 0; iter < p.length(); iter++) {
            char c = p.charAt(iter);
            if (c <= ' ' || c == 0x7f || c == '\\' || c == '"' || c == '<' || c == '>') {
                return null;
            }
        }
        return p;
    }
}
