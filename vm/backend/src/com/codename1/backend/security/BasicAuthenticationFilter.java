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

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;

/// Authenticates a request from its `Authorization: Basic` header.
///
/// The credentials are checked on every request that carries them and nothing
/// is kept: no session is started. A request without the header passes through
/// untouched, for the rules further on to judge; one whose credentials are
/// refused is answered 401 with a challenge.
public final class BasicAuthenticationFilter implements SecurityFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint entryPoint;

    BasicAuthenticationFilter(AuthenticationManager authenticationManager,
                              AuthenticationEntryPoint entryPoint) {
        this.authenticationManager = authenticationManager;
        this.entryPoint = entryPoint;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return chain.doFilter(request);
        }
        header = header.trim();
        // The scheme is compared without regard to case, character by character:
        // folding it with the locale would stop "BASIC" matching under a Turkish one.
        if (!header.regionMatches(true, 0, "Basic", 0, 5)
                || (header.length() > 5 && header.charAt(5) != ' ')) {
            return chain.doFilter(request);
        }
        try {
            String[] credentials = decode(header);
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            if (current != null && current.isAuthenticated()
                    && !(current instanceof AnonymousAuthenticationToken)
                    && current.getName().equals(credentials[0])) {
                // Already this user, from the session: nothing to check again.
                return chain.doFilter(request);
            }
            Authentication result = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(credentials[0],
                            credentials[1]));
            if (result == null) {
                throw new AuthenticationServiceException("The AuthenticationManager returned "
                        + "no authentication");
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(result);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException refused) {
            SecurityContextHolder.clearContext();
            return entryPoint.commence(request, refused);
        }
        return chain.doFilter(request);
    }

    /// The username and password of a Basic header.
    private static String[] decode(String header) {
        if (header.length() <= 6) {
            throw new BadCredentialsException("Empty basic authentication token");
        }
        byte[] decoded = Base64.decode(header.substring(6).trim());
        if (decoded == null) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token;
        try {
            token = new String(decoded, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
        int colon = token.indexOf(':');
        if (colon < 0) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[] {token.substring(0, colon), token.substring(colon + 1)};
    }
}
