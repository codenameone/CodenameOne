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
package com.codename1.backend.web;

import com.codename1.backend.HttpServer;
import java.util.ArrayList;
import java.util.List;

/// An interceptor limited to some paths, as Spring's
/// `registry.addInterceptor(x).addPathPatterns(...).excludePathPatterns(...)`.
///
/// Declare it as the bean in place of the interceptor it wraps:
///
/// ```java
/// @Bean
/// HandlerInterceptor audit() {
///     return new MappedInterceptor(new AuditInterceptor())
///             .addPathPatterns("/api/**")
///             .excludePathPatterns("/api/health");
/// }
/// ```
///
/// A pattern is a path in which `*` matches within one segment and a final
/// `/**` matches the rest of the path, the prefix itself included. With no
/// include pattern every path is included. Paths are matched as the routers
/// match them, without the query and with unreserved escapes decoded.
public final class MappedInterceptor implements HandlerInterceptor {
    private final HandlerInterceptor delegate;
    private final List<String> includes = new ArrayList<String>();
    private final List<String> excludes = new ArrayList<String>();

    /// Limits `delegate` to the patterns added next.
    /// @param delegate the interceptor to run on matching paths
    public MappedInterceptor(HandlerInterceptor delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate is null");
        }
        this.delegate = delegate;
    }

    /// Runs the interceptor on these paths.
    /// @param patterns path patterns
    /// @return this interceptor
    public MappedInterceptor addPathPatterns(String... patterns) {
        add(includes, patterns);
        return this;
    }

    /// Never runs the interceptor on these paths, even when included.
    /// @param patterns path patterns
    /// @return this interceptor
    public MappedInterceptor excludePathPatterns(String... patterns) {
        add(excludes, patterns);
        return this;
    }

    private static void add(List<String> to, String[] patterns) {
        for (String p : patterns) {
            if (p == null || p.length() == 0 || p.charAt(0) != '/') {
                throw new IllegalArgumentException("a path pattern starts with '/': " + p);
            }
            to.add(p);
        }
    }

    /// Whether the interceptor runs for `path`.
    /// @param path a request path, without its query
    /// @return true when it is included and not excluded
    public boolean matches(String path) {
        for (String p : excludes) {
            if (matches(p, path)) {
                return false;
            }
        }
        if (includes.isEmpty()) {
            return true;
        }
        for (String p : includes) {
            if (matches(p, path)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String base = pattern.substring(0, pattern.length() - 3);
            if (path.length() == base.length() ? matchSegments(base, path)
                    : path.length() > base.length() && path.charAt(base.length()) == '/'
                    && matchSegments(base, path.substring(0, base.length()))) {
                return true;
            }
            // A wildcard in the base makes the boundary move: try each '/'.
            if (base.indexOf('*') >= 0) {
                for (int i = 1 ; i <= path.length() ; i++) {
                    if ((i == path.length() || path.charAt(i) == '/')
                            && matchSegments(base, path.substring(0, i))) {
                        return true;
                    }
                }
            }
            return base.length() == 0;
        }
        return matchSegments(pattern, path);
    }

    /// `*` matches any run of characters other than '/'. Iterative, with one
    /// backtrack point, so a pattern of many stars costs linear time per star.
    private static boolean matchSegments(String pattern, String path) {
        int p = 0;
        int t = 0;
        int star = -1;
        int mark = 0;
        while (t < path.length()) {
            if (p < pattern.length() && pattern.charAt(p) == '*') {
                star = p++;
                mark = t;
            } else if (p < pattern.length() && pattern.charAt(p) == path.charAt(t)) {
                p++;
                t++;
            } else if (star >= 0 && path.charAt(mark) != '/') {
                p = star + 1;
                t = ++mark;
            } else {
                return false;
            }
        }
        while (p < pattern.length() && pattern.charAt(p) == '*') {
            p++;
        }
        return p == pattern.length();
    }

    private boolean applies(HttpServer.Request request) {
        return matches(request.pathFrom(0));
    }

    @Override
    public HttpServer.Response preHandle(HttpServer.Request request, HandlerMethod handler)
            throws Exception {
        return applies(request) ? delegate.preHandle(request, handler) : null;
    }

    @Override
    public void postHandle(HttpServer.Request request, HandlerMethod handler,
                           HttpServer.Response response) throws Exception {
        if (applies(request)) {
            delegate.postHandle(request, handler, response);
        }
    }

    @Override
    public void afterCompletion(HttpServer.Request request, HandlerMethod handler,
                                HttpServer.Response response, Throwable failure) {
        if (applies(request)) {
            delegate.afterCompletion(request, handler, response, failure);
        }
    }
}
