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
import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.util.List;

/// Answers 429 to a request that is over one of the chain's rate limits; see
/// [HttpSecurity#rateLimit].
///
/// A chain has up to two of these. The limits whose key a request has from the
/// start -- its client's address, its session -- are applied by the first
/// filter of the chain, before the session is read or a password hashed. The
/// ones whose key is who signed in are applied once that is known.
public final class RateLimitFilter implements SecurityFilter {
    /// {RequestMatcher, RateLimitKeyResolver, RateLimiter} per rule, in the
    /// order they were given.
    private final Object[][] rules;

    RateLimitFilter(List<Object[]> rules) {
        this.rules = rules.toArray(new Object[rules.size()][]);
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        for (Object[] rule : rules) {
            if (!((RequestMatcher) rule[0]).matches(request)) {
                continue;
            }
            String key = ((RateLimitKeyResolver) rule[1]).resolve(request);
            if (key == null) {
                continue;
            }
            RateLimiter limiter = (RateLimiter) rule[2];
            if (!limiter.tryAcquire(key)) {
                long wait = limiter.retryAfterSeconds(key);
                return Responses.status(429, "Too Many Requests")
                        .header("Retry-After", String.valueOf(wait < 1 ? 1 : wait));
            }
        }
        return chain.doFilter(request);
    }
}
