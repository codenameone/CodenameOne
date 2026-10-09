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
import java.util.List;

/// Gives a request nobody signed in for an [AnonymousAuthenticationToken], so
/// the rules that follow always have an authentication to judge.
public final class AnonymousAuthenticationFilter implements SecurityFilter {
    private final String key;
    private final Object principal;
    private final List<GrantedAuthority> authorities;

    AnonymousAuthenticationFilter(String key, Object principal, List<GrantedAuthority> authorities) {
        this.key = key;
        this.principal = principal;
        this.authorities = authorities;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null) {
            context.setAuthentication(new AnonymousAuthenticationToken(key, principal, authorities));
        }
        return chain.doFilter(request);
    }
}
