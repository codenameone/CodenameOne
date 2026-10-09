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
///
/// On a chain with a second factor, the right password of a user who has one
/// is refused too, with a 401 that says why: see
/// [HttpBasicConfigurer#secondFactorExempt].
public final class BasicAuthenticationFilter implements SecurityFilter {
    /// What a user with a second factor is told when they present a password
    /// alone.
    static final String SECOND_FACTOR_REQUIRED = "This account has a second factor, which "
            + "HTTP Basic credentials cannot present. Sign in through the login page.";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint entryPoint;
    private final SecondFactorPolicy secondFactor;

    /// @param secondFactor the chain's second factor, or null when it has
    /// none or exempts this mechanism from it
    BasicAuthenticationFilter(AuthenticationManager authenticationManager,
                              AuthenticationEntryPoint entryPoint,
                              SecondFactorPolicy secondFactor) {
        this.authenticationManager = authenticationManager;
        this.entryPoint = entryPoint;
        this.secondFactor = secondFactor;
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
            if (secondFactor != null && secondFactor.requires(result)) {
                // After the password was checked, so this says nothing to a
                // caller who does not know it.
                return secondFactorRequired(request);
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

    /// The refusal of a right password from a user who has a second factor: a
    /// 401 with why in the challenge and the body. The challenge stays a Basic
    /// one -- it is what the request used -- and the extra parameter is one a
    /// client that does not know it ignores. An entry point of the
    /// application's own answers instead, and is handed the reason.
    private HttpServer.Response secondFactorRequired(HttpServer.Request request) throws Exception {
        SecurityContextHolder.clearContext();
        if (!(entryPoint instanceof BasicAuthenticationEntryPoint)) {
            return entryPoint.commence(request,
                    new InsufficientAuthenticationException(SECOND_FACTOR_REQUIRED));
        }
        return Responses.status(401, SECOND_FACTOR_REQUIRED).header("WWW-Authenticate",
                "Basic realm=\"" + ((BasicAuthenticationEntryPoint) entryPoint).getRealmName()
                        + "\", error=\"second_factor_required\"");
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
