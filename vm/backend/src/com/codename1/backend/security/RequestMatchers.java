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

/// Combines matchers: all of them, any of them, or not one.
///
/// ```java
/// RequestMatcher writes = RequestMatchers.allOf(
///         AntPathRequestMatcher.antMatcher("/api/**"),
///         RequestMatchers.not(RequestMatchers.method("GET")));
/// ```
public final class RequestMatchers {
    private RequestMatchers() {
    }

    /// Matches when any of `matchers` does; never, given none.
    public static RequestMatcher anyOf(final RequestMatcher... matchers) {
        check(matchers);
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServer.Request request) {
                for (RequestMatcher matcher : matchers) {
                    if (matcher.matches(request)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public MatchResult matcher(HttpServer.Request request) {
                for (RequestMatcher matcher : matchers) {
                    MatchResult result = matcher.matcher(request);
                    if (result.isMatch()) {
                        return result;
                    }
                }
                return MatchResult.notMatch();
            }

            @Override
            public String toString() {
                return "Or " + java.util.Arrays.asList(matchers);
            }
        };
    }

    /// Matches when every one of `matchers` does; always, given none.
    public static RequestMatcher allOf(final RequestMatcher... matchers) {
        check(matchers);
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServer.Request request) {
                for (RequestMatcher matcher : matchers) {
                    if (!matcher.matches(request)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "And " + java.util.Arrays.asList(matchers);
            }

            @Override
            public MatchResult matcher(HttpServer.Request request) {
                java.util.Map<String, String> variables = new java.util.LinkedHashMap<String, String>();
                for (RequestMatcher matcher : matchers) {
                    MatchResult result = matcher.matcher(request);
                    if (!result.isMatch()) {
                        return MatchResult.notMatch();
                    }
                    variables.putAll(result.getVariables());
                }
                return MatchResult.match(variables);
            }
        };
    }

    /// Matches when `matcher` does not.
    public static RequestMatcher not(final RequestMatcher matcher) {
        check(new RequestMatcher[] {matcher});
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServer.Request request) {
                return !matcher.matches(request);
            }

            @Override
            public String toString() {
                return "Not [" + matcher + "]";
            }
        };
    }

    /// Matches requests of one method, written as HTTP writes it: `DELETE`.
    public static RequestMatcher method(final String method) {
        if (method == null || method.length() == 0) {
            throw new IllegalArgumentException("A method is required");
        }
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServer.Request request) {
                return method.equals(request.getMethod());
            }

            @Override
            public String toString() {
                return method;
            }
        };
    }

    /// Matches requests carrying a header; with a value given, that value.
    public static RequestMatcher header(final String name, final String value) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A header name is required");
        }
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServer.Request request) {
                String actual = request.getHeader(name);
                return actual != null && (value == null || value.equals(actual));
            }

            @Override
            public String toString() {
                return "Header [" + name + (value == null ? "" : "=" + value) + "]";
            }
        };
    }

    private static void check(RequestMatcher[] matchers) {
        if (matchers == null) {
            throw new IllegalArgumentException("matchers cannot be null");
        }
        for (RequestMatcher matcher : matchers) {
            if (matcher == null) {
                throw new IllegalArgumentException("matchers cannot contain null values");
            }
        }
    }
}
