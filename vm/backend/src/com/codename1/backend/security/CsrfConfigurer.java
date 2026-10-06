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

import java.util.ArrayList;
import java.util.List;

/// Protection against cross-site request forgery. On by default; see
/// [CsrfFilter] for what it requires of a request.
///
/// ```java
/// http.csrf(csrf -> csrf
///         .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
///         .ignoringRequestMatchers("/webhooks/**"));
/// ```
///
/// A chain whose clients are not browsers -- an API reached with a bearer token
/// or HTTP Basic from a program -- has nothing to protect and turns it off:
/// `http.csrf(csrf -> csrf.disable())`.
public final class CsrfConfigurer extends SecurityConfigurer {
    private CsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
    private RequestMatcher requireProtection = CsrfFilter.DEFAULT_REQUIRE_PROTECTION;
    private final List<RequestMatcher> ignored = new ArrayList<RequestMatcher>();

    CsrfConfigurer() {
    }

    /// Where the token is kept; the session unless set.
    public CsrfConfigurer csrfTokenRepository(CsrfTokenRepository csrfTokenRepository) {
        if (csrfTokenRepository == null) {
            throw new IllegalArgumentException("csrfTokenRepository cannot be null");
        }
        this.repository = csrfTokenRepository;
        return this;
    }

    /// Which requests must carry the token; every one that is not a GET, HEAD,
    /// TRACE or OPTIONS unless set.
    public CsrfConfigurer requireCsrfProtectionMatcher(RequestMatcher requireCsrfProtectionMatcher) {
        if (requireCsrfProtectionMatcher == null) {
            throw new IllegalArgumentException("requireCsrfProtectionMatcher cannot be null");
        }
        this.requireProtection = requireCsrfProtectionMatcher;
        return this;
    }

    /// Requests left alone whatever their method, by Ant pattern: an endpoint
    /// another server calls, which authenticates some other way.
    public CsrfConfigurer ignoringRequestMatchers(String... patterns) {
        for (String pattern : patterns) {
            ignored.add(new AntPathRequestMatcher(pattern));
        }
        return this;
    }

    /// Requests left alone whatever their method.
    public CsrfConfigurer ignoringRequestMatchers(RequestMatcher... requestMatchers) {
        for (RequestMatcher matcher : requestMatchers) {
            if (matcher == null) {
                throw new IllegalArgumentException("requestMatchers cannot contain null values");
            }
            ignored.add(matcher);
        }
        return this;
    }

    @Override
    public void configure(HttpSecurity http) {
        if (http.sessionCreationPolicy() == SessionCreationPolicy.STATELESS && !http.csrfAsked()) {
            // Nothing is kept between requests for a forged one to ride on, and
            // the token itself would have to live in a session the chain was
            // told not to have. A chain that wants it anyway says so.
            return;
        }
        RequestMatcher protect = requireProtection;
        if (!ignored.isEmpty()) {
            protect = RequestMatchers.allOf(requireProtection, RequestMatchers.not(
                    RequestMatchers.anyOf(ignored.toArray(new RequestMatcher[ignored.size()]))));
        }
        http.addFilter(new CsrfFilter(repository, protect, http.resolveAccessDeniedHandler()),
                HttpSecurity.ORDER_CSRF);
    }
}
