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

/// One step of a [SecurityFilterChain]. A filter answers the request itself, or
/// asks the rest of the chain and returns -- or replaces -- what comes back.
///
/// ```java
/// SecurityFilter audit = (request, chain) -> {
///     log(request.getMethod() + " " + request.pathFrom(0));
///     return chain.doFilter(request);
/// };
/// http.addFilterBefore(audit, AuthorizationFilter.class);
/// ```
///
/// The response the chain returns may be one a handler shares between requests,
/// so a filter does not write into it: a header for this request's answer goes
/// through [SecurityExchange#setResponseHeader].
public interface SecurityFilter {
    /// The answer to `request`; null when nothing under the chain routes it,
    /// which the server answers 404.
    HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain) throws Exception;
}
