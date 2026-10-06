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
package com.codename1.backend.security.ratelimit;

import com.codename1.backend.HttpServer;

/// Says which key a request counts under.
///
/// ```java
/// RateLimitKeyResolver tenant = request -> request.getHeader("X-Tenant");
/// ```
public interface RateLimitKeyResolver {
    /// The key, or null when the request has none -- nobody is signed in, it
    /// has no session -- and the limit does not apply to it.
    String resolve(HttpServer.Request request);

    /// Whether the key is known only once the request has been authenticated.
    /// A limit whose key needs that is applied after sign-in; any other is
    /// applied first of all, before the request costs anything.
    default boolean needsAuthentication() {
        return false;
    }
}
