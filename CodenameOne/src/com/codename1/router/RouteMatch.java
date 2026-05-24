/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

import com.codename1.util.regex.RE;
import com.codename1.util.regex.RESyntaxException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Compiled route pattern paired with its handler, plus matching logic.
///
/// Patterns support:
/// - **Literals** — `/about` matches only `/about`.
/// - **Named params** — `/users/:id` matches `/users/42` (`:id` → `"42"`).
/// - **Single-segment wildcard** — `/files/*` matches `/files/x` but not `/files/x/y`.
/// - **Catch-all wildcard** — `/files/**` matches `/files`, `/files/`, and
///   `/files/x/y/...`. The matched suffix is exposed as the special `*` param value.
///
/// Internally each pattern is compiled into a regex once at registration time using
/// `com.codename1.util.regex.RE`. The framework deliberately uses CN1's regex rather
/// than `java.util.regex` because the latter is not part of the CLDC11 surface the
/// core framework's Ant build compiles against.
///
/// #### Since 8.0
final class RouteMatch {

    /// Regex metacharacters we escape when emitting a literal segment.
    private static final String REGEX_META = "\\.^$|?*+()[]{}";

    private final String pattern;
    private final String compiledRegex;
    private final RE regex;
    private final String[] paramNames;
    private final RouteBuilder builder;
    private final boolean isWildcard;

    RouteMatch(String pattern, RouteBuilder builder) {
        if (pattern == null || pattern.length() == 0) {
            throw new IllegalArgumentException("Route pattern cannot be empty");
        }
        String normalized = pattern.charAt(0) == '/' ? pattern : "/" + pattern;
        this.pattern = normalized;
        this.builder = builder;

        StringBuilder regex = new StringBuilder();
        regex.append('^');
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        boolean wildcard = false;
        int i = 0;
        while (i < normalized.length()) {
            char c = normalized.charAt(i);
            if (c == '/') {
                regex.append('/');
                i++;
                continue;
            }
            // Take one segment.
            int end = normalized.indexOf('/', i);
            if (end < 0) end = normalized.length();
            String seg = normalized.substring(i, end);
            if (seg.equals("**")) {
                // Ant-style catch-all: `/admin/**` must match `/admin`,
                // `/admin/`, and `/admin/foo/bar`. We absorb the preceding
                // `/` we already emitted and replace it with an alternation
                // — either an empty tail OR `/<suffix>` with the suffix
                // captured. Using alternation rather than `(?:/(.*))?` keeps
                // us compatible with CN1's RE engine, which can drop the
                // inner capture group when an optional non-capturing wrapper
                // skips its body.
                if (regex.length() > 0 && regex.charAt(regex.length() - 1) == '/') {
                    regex.setLength(regex.length() - 1);
                }
                regex.append("(?:|/(.*))");
                wildcard = true;
                names.add("*");
            } else if (seg.equals("*")) {
                names.add("*");
                regex.append("([^/]+)");
            } else if (seg.length() > 1 && seg.charAt(0) == ':') {
                names.add(seg.substring(1));
                regex.append("([^/]+)");
            } else {
                regex.append(escape(seg));
            }
            i = end;
        }
        regex.append("/?$");
        this.compiledRegex = regex.toString();
        try {
            this.regex = new RE(this.compiledRegex);
        } catch (RESyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid route pattern \"" + pattern + "\" produced bad regex: " + e.getMessage(), e);
        }
        this.paramNames = names.toArray(new String[names.size()]);
        this.isWildcard = wildcard;
    }

    String getPattern() { return pattern; }

    RouteBuilder getBuilder() { return builder; }

    /// Returns the param map on a match, or null on no match.
    Map<String, String> match(String path) {
        if (path == null) return null;
        // `RE.match` finds the pattern anywhere in `path`; the leading `^` and
        // trailing `$` we emit anchor that find to the full string. We also
        // assert the matched span covers the input as belt-and-braces against
        // any anchoring quirks in the engine.
        if (!regex.match(path, 0)) return null;
        if (regex.getParenStart(0) != 0 || regex.getParenEnd(0) != path.length()) {
            return null;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        int parens = regex.getParenCount();
        for (int i = 0; i < paramNames.length; i++) {
            // CN1's RE reports `getParenCount()` based on the groups the
            // matcher actually visited, so an unvisited alternation branch
            // (e.g. the suffix capture inside `(?:|/(.*))` for `/admin/**`
            // against bare `/admin`) shows up as "fewer parens than the
            // pattern has". Treat any missing group as an empty match and
            // always set the key so callers can look up the param without
            // null-checking.
            String value = null;
            if (i + 1 < parens && regex.getParenStart(i + 1) >= 0) {
                value = regex.getParen(i + 1);
            }
            params.put(paramNames[i], value == null ? "" : value);
        }
        return params;
    }

    /// Returns whether this pattern uses a catch-all `**`.
    boolean isCatchAll() { return isWildcard; }

    /// Helper used by guard matching where patterns may be path-prefix globs.
    static boolean simpleMatch(String pattern, String path) {
        return new RouteMatch(pattern, null).match(path) != null;
    }

    /// Specificity score: more literal segments = more specific. Used to deterministically
    /// pick a winner when multiple routes match.
    int specificity() {
        int score = 0;
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.charAt(i) == '/') { i++; continue; }
            int end = pattern.indexOf('/', i);
            if (end < 0) end = pattern.length();
            String seg = pattern.substring(i, end);
            if (seg.equals("**")) {
                score -= 100;
            } else if (seg.equals("*") || (seg.length() > 0 && seg.charAt(0) == ':')) {
                score += 1;
            } else {
                score += 10;
            }
            i = end;
        }
        return score;
    }

    static String joinSegments(List<String> segs) {
        if (segs == null || segs.isEmpty()) return "/";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segs.size(); i++) {
            sb.append('/').append(segs.get(i));
        }
        return sb.toString();
    }

    /// Manually escape regex metacharacters in a literal path segment. CN1's
    /// `RE` doesn't expose a `Pattern.quote` equivalent; this covers the
    /// metachars that show up in valid URL path segments (mainly `.`).
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (REGEX_META.indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }
}
