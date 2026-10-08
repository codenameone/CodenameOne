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

/// Loads who is signed in from the chain's [SecurityContextRepository] into
/// [SecurityContextHolder] before anything else looks. First in every chain.
///
/// It only loads. A filter that signs a user in saves the context itself, and
/// the server clears the holder when the request ends.
public final class SecurityContextHolderFilter implements SecurityFilter {
    private final SecurityContextRepository repository;
    private final boolean alwaysSession;

    SecurityContextHolderFilter(SecurityContextRepository repository, boolean alwaysSession) {
        this.repository = repository;
        this.alwaysSession = alwaysSession;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (alwaysSession) {
            request.getSession(true);
        }
        // A test that said who its requests are from is believed over the session.
        SecurityContext test = SecurityContextHolder.testContext();
        SecurityContext context = test != null ? new SecurityContextImpl(test.getAuthentication())
                : repository.loadContext(request);
        SecurityContextHolder.setContext(context == null
                ? SecurityContextHolder.createEmptyContext() : context);
        return chain.doFilter(request);
    }
}
