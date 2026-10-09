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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Matches a request's path against an Ant-style pattern, and optionally its
/// method.
///
/// - `?` is one character and `*` any run of characters within one segment:
///   `/files/*.txt`.
/// - `**` is any number of whole segments, none included: `/api/**` matches
///   `/api`, `/api/` and `/api/users/7`.
/// - `{name}` is one segment, or the rest of one, bound to that name:
///   `/users/{id}`.
///
/// The path compared is the one the routers compare -- the query removed and
/// percent-escapes of unreserved characters resolved -- so a request cannot be
/// spelled to match a rule here and a different route there. Matching is case
/// sensitive, as routing is, and a trailing slash is part of the path.
public final class AntPathRequestMatcher implements RequestMatcher {
    private static final String MATCH_ALL = "**";

    private final String pattern;
    private final String method;
    private final String[] segments;
    private final boolean all;
    private final boolean variables;

    public AntPathRequestMatcher(String pattern) {
        this(pattern, null);
    }

    /// @param method the method to match exactly, or null for any
    public AntPathRequestMatcher(String pattern, String method) {
        if (pattern == null || pattern.length() == 0) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        if (!MATCH_ALL.equals(pattern) && pattern.charAt(0) != '/') {
            throw new IllegalArgumentException("The pattern \"" + pattern + "\" must start "
                    + "with /: a request path always does");
        }
        this.pattern = pattern;
        this.method = method == null || method.length() == 0 ? null : method;
        this.all = MATCH_ALL.equals(pattern) || "/**".equals(pattern);
        this.segments = split(pattern);
        boolean named = false;
        for (String segment : segments) {
            int open = segment.indexOf('{');
            if (open >= 0) {
                int close = segment.indexOf('}', open);
                if (close < 0) {
                    throw new IllegalArgumentException("The pattern \"" + pattern
                            + "\" opens a { it never closes");
                }
                if (segment.substring(open, close).indexOf(':') >= 0) {
                    throw new IllegalArgumentException("The pattern \"" + pattern
                            + "\" gives a variable a regular expression, which is not "
                            + "supported; match the segment and check it in the rule");
                }
                named = true;
            }
            if (segment.indexOf(MATCH_ALL) >= 0 && !MATCH_ALL.equals(segment)) {
                throw new IllegalArgumentException("In the pattern \"" + pattern
                        + "\", ** must be a whole segment");
            }
        }
        this.variables = named;
    }

    /// A matcher for `pattern`, any method.
    public static AntPathRequestMatcher antMatcher(String pattern) {
        return new AntPathRequestMatcher(pattern);
    }

    /// A matcher for `pattern` and one method, written as HTTP writes it: `POST`.
    public static AntPathRequestMatcher antMatcher(String method, String pattern) {
        return new AntPathRequestMatcher(pattern, method);
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean matches(HttpServer.Request request) {
        if (method != null && !method.equals(request.getMethod())) {
            return false;
        }
        if (all) {
            return true;
        }
        return matchSegments(0, split(SecurityExchange.path(request)), 0, null);
    }

    @Override
    public MatchResult matcher(HttpServer.Request request) {
        if (method != null && !method.equals(request.getMethod())) {
            return MatchResult.notMatch();
        }
        if (all) {
            return MatchResult.match();
        }
        if (!variables) {
            return matches(request) ? MatchResult.match() : MatchResult.notMatch();
        }
        Map<String, String> bound = new LinkedHashMap<String, String>();
        return matchSegments(0, split(SecurityExchange.path(request)), 0, bound)
                ? MatchResult.match(bound) : MatchResult.notMatch();
    }

    /// Whether the pattern from segment `pi` matches the path from segment `si`.
    private boolean matchSegments(int pi, String[] path, int si, Map<String, String> bound) {
        if (pi == segments.length) {
            return si == path.length;
        }
        String segment = segments[pi];
        if (MATCH_ALL.equals(segment)) {
            if (pi == segments.length - 1) {
                return true;
            }
            for (int skip = si ; skip <= path.length ; skip++) {
                if (matchSegments(pi + 1, path, skip, bound)) {
                    return true;
                }
            }
            return false;
        }
        if (si == path.length) {
            return false;
        }
        return matchText(segment, 0, path[si], 0, bound)
                && matchSegments(pi + 1, path, si + 1, bound);
    }

    /// Whether one pattern segment from `pi` matches one path segment from `ti`.
    private static boolean matchText(String pattern, int pi, String text, int ti,
                                     Map<String, String> bound) {
        while (pi < pattern.length()) {
            char c = pattern.charAt(pi);
            if (c == '*') {
                for (int end = ti ; end <= text.length() ; end++) {
                    if (matchText(pattern, pi + 1, text, end, bound)) {
                        return true;
                    }
                }
                return false;
            }
            if (c == '{') {
                int close = pattern.indexOf('}', pi);
                String name = pattern.substring(pi + 1, close);
                // The longest run first, so {name} alone takes the whole segment.
                for (int end = text.length() ; end > ti ; end--) {
                    if (matchText(pattern, close + 1, text, end, bound)) {
                        if (bound != null) {
                            bound.put(name, text.substring(ti, end));
                        }
                        return true;
                    }
                }
                return false;
            }
            if (ti >= text.length() || (c != '?' && c != text.charAt(ti))) {
                return false;
            }
            pi++;
            ti++;
        }
        return ti == text.length();
    }

    /// The segments between the slashes of a path: `/a/b` is two, `/a/` is `a`
    /// and an empty one, and `/` is none. By hand: this runtime's String has no
    /// split.
    static String[] split(String path) {
        if (path == null || path.length() == 0 || "/".equals(path)) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        int start = path.charAt(0) == '/' ? 1 : 0;
        while (start <= path.length()) {
            int slash = path.indexOf('/', start);
            if (slash < 0) {
                out.add(path.substring(start));
                break;
            }
            out.add(path.substring(start, slash));
            start = slash + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public String toString() {
        return "Ant [pattern='" + pattern + "'" + (method == null ? "" : ", " + method) + "]";
    }
}
