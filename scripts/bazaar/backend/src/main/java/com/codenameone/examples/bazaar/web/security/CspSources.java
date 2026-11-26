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
package com.codenameone.examples.bazaar.web.security;

import java.util.ArrayList;
import java.util.List;

/// Hosts a page may load from beyond its own origin, per directive: what the
/// app slots on that page were allowed ([com.codenameone.examples.bazaar.web.slots.Slots]).
///
/// A source is accepted only as `https://host[:port]` with a host of letters,
/// digits, dots and dashes -- never a wildcard, a scheme alone, `'unsafe-*'`,
/// a path or anything with a quote, semicolon or space in it -- so an owner's
/// allow-list can widen the policy by named hosts and cannot rewrite it.
public final class CspSources {
    public static final int SCRIPT = 0;
    public static final int IMG = 1;
    public static final int CONNECT = 2;
    public static final int FRAME = 3;

    private final List<List<String>> lists = new ArrayList<List<String>>();

    public CspSources() {
        for (int iter = 0; iter < 4; iter++) {
            lists.add(new ArrayList<String>());
        }
    }

    /// Adds `source` to `directive` when it is a valid https origin; answers
    /// whether it was.
    public boolean add(int directive, String source) {
        String clean = clean(source);
        if (clean == null) {
            return false;
        }
        List<String> list = lists.get(directive);
        if (!list.contains(clean)) {
            list.add(clean);
        }
        return true;
    }

    public void addAll(CspSources other) {
        for (int d = 0; d < 4; d++) {
            List<String> from = other.lists.get(d);
            for (int iter = 0; iter < from.size(); iter++) {
                add(d, from.get(iter));
            }
        }
    }

    public List<String> get(int directive) {
        return lists.get(directive);
    }

    public boolean isEmpty() {
        for (int d = 0; d < 4; d++) {
            if (!lists.get(d).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static void append(CspSources sources, StringBuilder b, int directive) {
        if (sources == null) {
            return;
        }
        List<String> list = sources.lists.get(directive);
        for (int iter = 0; iter < list.size(); iter++) {
            b.append(' ').append(list.get(iter));
        }
    }

    /// `https://host[:port]` in lower case, or null for anything else.
    public static String clean(String source) {
        if (source == null) {
            return null;
        }
        String s = source.trim();
        if (!s.regionMatches(true, 0, "https://", 0, 8)) {
            return null;
        }
        String rest = s.substring(8);
        if (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        if (rest.length() == 0 || rest.length() > 200) {
            return null;
        }
        StringBuilder out = new StringBuilder("https://");
        boolean port = false;
        boolean dot = false;
        for (int iter = 0; iter < rest.length(); iter++) {
            char c = rest.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            if (port) {
                if (c < '0' || c > '9') {
                    return null;
                }
            } else if (c == ':') {
                port = true;
            } else if (c == '.') {
                dot = true;
            } else if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-')) {
                return null;
            }
            out.append(c);
        }
        if (!dot || out.charAt(out.length() - 1) == ':' || out.charAt(8) == '.') {
            return null;
        }
        return out.toString();
    }
}
