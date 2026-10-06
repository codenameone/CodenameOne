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

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.apikey.ApiKey;
import com.codename1.backend.security.apikey.ApiKeyAuthenticationToken;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.ApiKeyRepository;
import com.codename1.backend.security.oauth2.server.resource.InvalidBearerTokenException;

/// Authenticates a request from the API key it presents, in `X-API-Key` or as
/// `Authorization: Bearer <key>`.
///
/// A bearer value is taken for an API key only when it starts with the
/// configured prefix; any other is left for whatever verifies tokens. So one
/// chain can accept both, and the prefix is what tells them apart. A value in
/// `X-API-Key` is always a key, and one without the prefix is refused.
///
/// The key is checked on every request and nothing is kept. A request without
/// one passes through untouched; one whose key is unknown or revoked is
/// answered 401 at once, and is not told which.
public final class ApiKeyAuthenticationFilter implements SecurityFilter {
    /// Longer than any key this runtime makes, and short enough to hash freely.
    private static final int MAX_KEY_CHARS = 256;

    private final ApiKeyRepository repository;
    private final String prefix;
    private final String headerName;
    private final AuthenticationEntryPoint entryPoint;

    ApiKeyAuthenticationFilter(ApiKeyRepository repository, String prefix, String headerName,
                               AuthenticationEntryPoint entryPoint) {
        this.repository = repository;
        this.prefix = prefix;
        this.headerName = headerName;
        this.entryPoint = entryPoint;
    }

    /// The API key a request presents, or null: the value of the key header,
    /// or a bearer value that starts with the prefix.
    static String presented(HttpServer.Request request, String headerName, String prefix) {
        String fromHeader = request.getHeader(headerName);
        if (fromHeader != null) {
            return fromHeader.trim();
        }
        String bearer = bearer(request);
        return bearer != null && bearer.startsWith(prefix) ? bearer : null;
    }

    private static String bearer(HttpServer.Request request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        header = header.trim();
        if (header.length() <= 7 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return header.substring(7).trim();
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String key = presented(request, headerName, prefix);
        if (key == null) {
            return chain.doFilter(request);
        }
        String bearer = bearer(request);
        if (bearer != null && bearer.startsWith(prefix)) {
            // Ours to judge, whatever comes of it: not a token for the filter
            // that verifies tokens.
            SecurityExchange exchange = SecurityExchange.of(request);
            if (exchange != null) {
                exchange.setAttribute(SecurityExchange.BEARER_CLAIMED, Boolean.TRUE);
            }
        }
        ApiKey found = null;
        if (key.startsWith(prefix) && key.length() > prefix.length()
                && key.length() <= MAX_KEY_CHARS) {
            String hash = ApiKeyGenerator.hash(key);
            found = repository.findByHash(hash);
            // The repository was asked for this hash; believe it only if that
            // is what it returned.
            if (found != null && !Crypto.equalsConstantTime(Responses.utf8(hash),
                    Responses.utf8(found.getHash()))) {
                found = null;
            }
        }
        if (found == null || found.isRevoked()) {
            SecurityContextHolder.clearContext();
            // One answer for unknown, revoked and malformed alike.
            return entryPoint.commence(request,
                    new InvalidBearerTokenException("The API key is not valid"));
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ApiKeyAuthenticationToken(found));
        SecurityContextHolder.setContext(context);
        return chain.doFilter(request);
    }
}
