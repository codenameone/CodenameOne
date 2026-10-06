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

import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.util.ArrayList;
import java.util.List;

/// The rate limits of a chain; filled in by [HttpSecurity#rateLimit].
public final class RateLimitConfigurer extends SecurityConfigurer {

    /// {RequestMatcher, RateLimitKeyResolver, RateLimiter or null}.
    private final List<Object[]> rules = new ArrayList<Object[]>();

    RateLimitConfigurer() {
    }

    void add(RequestMatcher matcher, RateLimitKeyResolver keyResolver, RateLimiter limiter) {
        if (matcher == null || keyResolver == null) {
            throw new IllegalArgumentException("A rate limit needs a matcher and a key resolver");
        }
        rules.add(new Object[] {matcher, keyResolver, limiter});
    }

    @Override
    public void configure(HttpSecurity http) {
        List<Object[]> first = new ArrayList<Object[]>();
        List<Object[]> authenticated = new ArrayList<Object[]>();
        for (Object[] rule : rules) {
            Object limiter = rule[2];
            if (limiter == null) {
                limiter = http.getSharedObject(RateLimiter.class);
            }
            if (limiter == null) {
                throw new IllegalStateException("rateLimit() was given no RateLimiter, and this "
                        + "application has no single RateLimiter bean to use instead. Pass one: "
                        + "new InMemoryRateLimiter(permits, periodSeconds).");
            }
            Object[] resolved = {rule[0], rule[1], limiter};
            if (((RateLimitKeyResolver) rule[1]).needsAuthentication()) {
                authenticated.add(resolved);
            } else {
                first.add(resolved);
            }
        }
        if (!first.isEmpty()) {
            http.addFilter(new RateLimitFilter(first), HttpSecurity.ORDER_RATE_LIMIT);
        }
        if (!authenticated.isEmpty()) {
            http.addFilter(new RateLimitFilter(authenticated),
                    HttpSecurity.ORDER_AUTHENTICATED_RATE_LIMIT);
        }
    }
}
