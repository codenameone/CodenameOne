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

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoder;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2TokenContext;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.TokenSettings;
import java.io.IOException;

/** The Backend security chapter's authorization server examples. */
@Configuration
public class AuthorizationServerConfig {
// tag::backend-security-authserver[]
    @Bean
    SecurityFilterChain web(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .authorizationServer(Customizer.withDefaults());
        return http.build();
    }
// end::backend-security-authserver[]

// tag::backend-security-authserver-clients[]
    @Bean
    RegisteredClientRepository clients(PasswordEncoder encoder, Config config)
            throws IOException {
        RegisteredClient app = RegisteredClient.withId("mobile")
                .clientId("acme-app")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)   // public: PKCE
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                .redirectUri("com.acme.app:/oauth2redirect")
                .scope("openid").scope("profile").scope("orders:read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(300)
                        .refreshTokenTimeToLive(30L * 24 * 3600)
                        .build())
                .build();
        RegisteredClient reports = RegisteredClient.withId("reports")
                .clientId("nightly-reports")
                .clientSecret(encoder.encode(config.get("reports.client-secret")))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("orders:read")
                .build();
        return new InMemoryRegisteredClientRepository(app, reports);
    }
// end::backend-security-authserver-clients[]

// tag::backend-security-authserver-store[]
    @Bean
    OAuth2AuthorizationService authorizations(DataSource dataSource) {
        return new JdbcOAuth2AuthorizationService(dataSource);
    }
// end::backend-security-authserver-store[]

// tag::backend-security-authserver-keys[]
    @Bean
    JwkSource signingKeys(Config config) throws IOException {
        return AuthorizationServerKeys.load(config);
    }

    @Bean
    JwtEncoder jwtEncoder(JwkSource keys) {
        return new DefaultJwtEncoder(keys);
    }
// end::backend-security-authserver-keys[]

    static void customize(HttpSecurity http) {
// tag::backend-security-authserver-customize[]
http.authorizationServer(server -> server
        .settings(AuthorizationServerSettings.builder()
                .issuer("https://id.example.com")
                .build())
        .tokenCustomizer(context -> {
            if (OAuth2TokenContext.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().claim("tenant", "acme");
            }
        }));
// end::backend-security-authserver-customize[]
    }
}
