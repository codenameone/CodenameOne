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
package com.codename1.impl.backend.security;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.CsrfToken;
import com.codename1.backend.security.HttpSecurity;

/// What generated code calls: one static method per thing it needs, so a
/// generated line stays a line.
public final class SecuritySupport {
    private SecuritySupport() {
    }

    /// The [HttpSecurity] a `SecurityFilterChain` bean method is handed. A new
    /// one for every method: each builds its own chain.
    public static HttpSecurity http(Config config, Object[] beans) {
        return SecurityAccess.get().httpSecurity(config, beans);
    }

    /// A controller's `Authentication` parameter: who signed in, or null.
    public static Authentication authentication() {
        return SecurityAccess.get().authentication();
    }

    /// A controller's `@AuthenticationPrincipal` parameter, before its type is
    /// checked.
    public static Object principal() {
        return SecurityAccess.get().principal();
    }

    /// A controller's `CsrfToken` parameter.
    public static CsrfToken csrfToken(HttpServer.Request request) {
        return SecurityAccess.get().csrfToken(request);
    }
}
