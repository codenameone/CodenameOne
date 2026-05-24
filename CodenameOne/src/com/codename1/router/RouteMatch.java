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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Compiled route pattern paired with its handler, plus matching logic.
///
/// Patterns support:
/// - **Literals** — `/about` matches only `/about`.
/// - **Named params** — `/users/:id` matches `/users/42` (`:id` → `"42"`).
/// - **Single-segment wildcard** — `/files/*` matches `/files/x` but not `/files/x/y`.
/// - **Catch-all wildcard** — `/files/**` matches `/files/`, `/files/x`, `/files/x/y/...`.
///   The matched suffix is exposed as the special `*` param value.
///
/// Internally each pattern is compiled into a regex once at registration time;
/// matches are O(path length).
///
/// #### Since 8.0
final class RouteMatch {
    private final String pattern;
    private final Pattern regex;
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
                // `/` we already emitted and replace it with an optional
                // group so the bare prefix matches too.
                if (regex.length() > 0 && regex.charAt(regex.length() - 1) == '/') {
                    regex.setLength(regex.length() - 1);
                }
                regex.append("(?:/(.*))?");
                wildcard = true;
                names.add("*");
            } else if (seg.equals("*")) {
                names.add("*");
                regex.append("([^/]+)");
            } else if (seg.length() > 1 && seg.charAt(0) == ':') {
                names.add(seg.substring(1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(seg));
            }
            i = end;
        }
        regex.append("/?$");
        this.regex = Pattern.compile(regex.toString());
        this.paramNames = names.toArray(new String[names.size()]);
        this.isWildcard = wildcard;
    }

    String getPattern() { return pattern; }

    RouteBuilder getBuilder() { return builder; }

    /// Returns the param map on a match, or null on no match.
    Map<String, String> match(String path) {
        if (path == null) return null;
        Matcher m = regex.matcher(path);
        if (!m.matches()) return null;
        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        for (int i = 0; i < paramNames.length && i < m.groupCount(); i++) {
            String value = m.group(i + 1);
            // Catch-all `**` produces an optional group: when the input ends
            // at the prefix (e.g. `/admin` against `/admin/**`) the group is
            // null. Normalize to empty string so callers don't NPE.
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
}
