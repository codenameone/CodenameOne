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
package com.codenameone.developerguide.backend.security;

import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationConverter;
import com.codename1.backend.security.oauth2.server.resource.JwtGrantedAuthoritiesConverter;

/** The Backend security chapter's resource server examples. */
@Configuration
public class ResourceServerConfig {
// tag::backend-security-jwt-decoder[]
    @Bean
    JwtDecoder jwtDecoder() {
        DefaultJwtDecoder decoder = DefaultJwtDecoder
                .withJwkSetUri("https://id.example.com/oauth2/jwks")
                .jwsAlgorithms(SignatureAlgorithm.RS256, SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator("https://id.example.com"),
                new JwtAudienceValidator("orders-api")));
        return decoder;
    }
// end::backend-security-jwt-decoder[]

    @Bean
    SecurityFilterChain roles(HttpSecurity http) {
// tag::backend-security-jwt-converter[]
JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
roles.setAuthoritiesClaimName("roles");     // ["ADMIN", "USER"]
roles.setAuthorityPrefix("ROLE_");          // so hasRole("ADMIN") matches

JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
converter.setJwtGrantedAuthoritiesConverter(roles);
converter.setPrincipalClaimName("preferred_username");

http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
        jwt.jwtAuthenticationConverter(converter)));
// end::backend-security-jwt-converter[]
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
