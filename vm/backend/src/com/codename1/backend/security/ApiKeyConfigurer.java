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
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.ApiKeyRepository;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAccessDeniedHandler;
import com.codename1.backend.security.oauth2.server.resource.BearerTokenAuthenticationEntryPoint;

/// Sign-in with an API key on each request.
///
/// ```java
/// @Bean
/// SecurityFilterChain api(HttpSecurity http) {
///     http.securityMatcher("/api/**")
///         .authorizeHttpRequests(auth -> auth
///                 .requestMatchers("/api/deploy/**").hasAuthority("SCOPE_deploy")
///                 .anyRequest().authenticated())
///         .apiKey(Customizer.withDefaults());
///     return http.build();
/// }
/// ```
///
/// A client sends its key as `X-API-Key: cn1_...` or as
/// `Authorization: Bearer cn1_...`. Keys are looked up in the application's
/// [ApiKeyRepository] bean unless [#repository] names another, and are made
/// with [ApiKeyGenerator].
///
/// On a chain that also has `oauth2ResourceServer(...)`, a bearer value that
/// starts with the key prefix is an API key and any other is a token; that is
/// the whole rule, and the reason the prefix here has to be the one the keys
/// were generated with.
///
/// A request authenticated by its key is not asked for a CSRF token, and no
/// session is started for it. A key's scopes are the authorities `SCOPE_x`.
public final class ApiKeyConfigurer extends SecurityConfigurer {
    private ApiKeyRepository repository;
    private String prefix = ApiKeyGenerator.DEFAULT_PREFIX;
    private String headerName = "X-API-Key";
    private AuthenticationEntryPoint authenticationEntryPoint =
            new BearerTokenAuthenticationEntryPoint();

    ApiKeyConfigurer() {
    }

    /// Where keys are looked up, in place of the application's bean.
    public ApiKeyConfigurer repository(ApiKeyRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        this.repository = repository;
        return this;
    }

    /// What every key starts with; `cn1_` unless set. It must be the prefix
    /// the keys were generated with.
    public ApiKeyConfigurer prefix(String prefix) {
        this.prefix = ApiKeyGenerator.checkPrefix(prefix);
        return this;
    }

    /// The header a key is sent in, beside `Authorization: Bearer`; `X-API-Key`
    /// unless set.
    public ApiKeyConfigurer headerName(String headerName) {
        if (headerName == null || headerName.length() == 0) {
            throw new IllegalArgumentException("A header name is required");
        }
        this.headerName = headerName;
        return this;
    }

    /// What answers a request whose key is refused, and -- when this is the
    /// chain's only way in -- one that sent none.
    public ApiKeyConfigurer authenticationEntryPoint(AuthenticationEntryPoint entryPoint) {
        if (entryPoint == null) {
            throw new IllegalArgumentException("authenticationEntryPoint cannot be null");
        }
        this.authenticationEntryPoint = entryPoint;
        return this;
    }

    @Override
    public void init(HttpSecurity http) {
        RequestMatcher carriesKey = new CarriesKey(headerName, prefix);
        CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            csrf.ignoringRequestMatchers(carriesKey);
        }
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            handling.defaultAuthenticationEntryPointFor(authenticationEntryPoint, carriesKey);
            handling.defaultAccessDeniedHandlerFor(new BearerTokenAccessDeniedHandler(), carriesKey);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        ApiKeyRepository keys = repository != null ? repository
                : http.getSharedObject(ApiKeyRepository.class);
        if (keys == null) {
            throw new IllegalStateException("apiKey() needs somewhere to look keys up, and this "
                    + "application has none. Declare an ApiKeyRepository bean, or call "
                    + "repository(...) on the apiKey() configurer.");
        }
        http.addFilter(new ApiKeyAuthenticationFilter(keys, prefix, headerName,
                authenticationEntryPoint), HttpSecurity.ORDER_API_KEY);
    }

    /// Matches a request that presents an API key.
    private static final class CarriesKey implements RequestMatcher {
        private final String headerName;
        private final String prefix;

        CarriesKey(String headerName, String prefix) {
            this.headerName = headerName;
            this.prefix = prefix;
        }

        @Override
        public boolean matches(HttpServer.Request request) {
            return ApiKeyAuthenticationFilter.presented(request, headerName, prefix) != null;
        }

        @Override
        public String toString() {
            return "ApiKey";
        }
    }
}
