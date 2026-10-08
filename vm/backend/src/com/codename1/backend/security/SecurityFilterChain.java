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

/// The filters that guard some of a server's requests. An application declares
/// one as a bean, built from the [HttpSecurity] it is handed:
///
/// ```java
/// @Configuration
/// public class SecurityConfig {
///     @Bean
///     @Order(1)
///     SecurityFilterChain api(HttpSecurity http) {
///         http.securityMatcher("/api/**")
///             .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("API"))
///             .httpBasic(Customizer.withDefaults())
///             .csrf(csrf -> csrf.disable());
///         return http.build();
///     }
///
///     @Bean
///     SecurityFilterChain pages(HttpSecurity http) {
///         http.authorizeHttpRequests(auth -> auth
///                 .requestMatchers("/", "/css/**").permitAll()
///                 .anyRequest().authenticated())
///             .formLogin(Customizer.withDefaults());
///         return http.build();
///     }
/// }
/// ```
///
/// A request is put to the chains in `@Order`, and the first whose
/// [#matches] answers true is the only one that sees it. A request no chain
/// matches is not guarded at all. The server's own endpoints -- management, MCP,
/// the telemetry relay -- are not put to any chain: they keep their own tokens.
public interface SecurityFilterChain {
    /// Whether this chain guards `request`.
    boolean matches(HttpServer.Request request);

    /// The filters, in the order they run.
    List<SecurityFilter> getFilters();
}
