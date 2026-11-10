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
package com.codename1.backend;

import java.util.LinkedHashMap;
import java.util.Map;

/// Routes answered without a controller method, as Spring's
/// `addRedirectViewController` and `addStatusController`.
///
/// ```java
/// @Bean
/// HttpServer.Handler shortcuts() {
///     return new ViewControllers()
///             .redirect("/docs", "/docs/index.html")
///             .status("/favicon.ico", 204);
/// }
/// ```
///
/// Spring's `addViewController("/", "home")` -- a template rendered for a
/// path -- has no counterpart: a view here is compiled into the controller
/// method that returns it, so there is nothing to look a view name up in at
/// run time. A one-line `@GetMapping` method returning the name does the same
/// and keeps the template checked at build time.
public final class ViewControllers implements HttpServer.Handler {
    /// Canonical path -> {status, location or null}.
    private final Map<String, Object[]> routes = new LinkedHashMap<String, Object[]>();

    /// Answers `path` with a 302 to `location`.
    /// @param path the path, exactly
    /// @param location where the client is sent; relative or absolute
    /// @return this
    public ViewControllers redirect(String path, String location) {
        return redirect(path, location, 302);
    }

    /// Answers `path` with `status` (a 3xx) and `location`.
    /// @param path the path, exactly
    /// @param location where the client is sent
    /// @param status 301, 302, 303, 307 or 308
    /// @return this
    public ViewControllers redirect(String path, String location, int status) {
        if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
            throw new IllegalArgumentException("a redirect status is 301, 302, 303, 307 or 308");
        }
        if (location == null || location.length() == 0 || location.indexOf('\r') >= 0
                || location.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("not a location: " + location);
        }
        put(path, new Object[] {Integer.valueOf(status), location});
        return this;
    }

    /// Answers `path` with `status` and no body.
    /// @param path the path, exactly
    /// @param status 100 to 599
    /// @return this
    public ViewControllers status(String path, int status) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("a status is between 100 and 599");
        }
        put(path, new Object[] {Integer.valueOf(status), null});
        return this;
    }

    private void put(String path, Object[] answer) {
        if (path == null || path.length() == 0 || path.charAt(0) != '/') {
            throw new IllegalArgumentException("a path starts with '/': " + path);
        }
        routes.put(HttpServer.canonicalDeclaredPath(path), answer);
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return null;
        }
        Object[] answer = routes.get(request.pathFrom(0));
        if (answer == null) {
            return null;
        }
        int status = ((Integer) answer[0]).intValue();
        HttpServer.Response r = HttpServer.Response.empty(status, null, null);
        if (answer[1] != null) {
            r.header("Location", (String) answer[1]);
        }
        return r;
    }
}
