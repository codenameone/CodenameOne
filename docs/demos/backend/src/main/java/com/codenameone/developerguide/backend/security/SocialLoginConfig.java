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
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.oauth2.client.AppleClientSecret;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.ClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistrations;
import com.codename1.backend.security.oauth2.client.CommonOAuth2Provider;
import com.codename1.backend.security.oauth2.client.FederatedIdentityRepository;
import com.codename1.backend.security.oauth2.client.InMemoryClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.JdbcFederatedIdentityRepository;
import com.codename1.backend.security.oauth2.client.LinkingOAuth2UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The Backend security chapter's examples of signing in through another provider. */
@Configuration
public class SocialLoginConfig {
// tag::backend-security-oauth2-apple[]
    @Bean
    ClientRegistrationRepository providers(Config config) throws IOException {
        List<ClientRegistration> all =
                new ArrayList<ClientRegistration>(ClientRegistrations.fromConfig(config));
        all.add(CommonOAuth2Provider.APPLE.getBuilder("apple")
                .clientId("com.example.web")          // the Services ID
                .clientSecretSupplier(AppleClientSecret.fromFile(
                        config.get("apple.team-id"), config.get("apple.key-id"),
                        config.get("apple.key-file")))
                .build());
        return new InMemoryClientRegistrationRepository(all);
    }
// end::backend-security-oauth2-apple[]

// tag::backend-security-oauth2-linking[]
    @Bean
    FederatedIdentityRepository identities(DataSource dataSource) {
        return new JdbcFederatedIdentityRepository(dataSource);
    }

    @Bean
    SecurityFilterChain web(HttpSecurity http, FederatedIdentityRepository identities,
                            UserDetailsService users) {
        LinkingOAuth2UserService linking = new LinkingOAuth2UserService(identities, users);
        linking.setCreateUsers(true);

        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                    .userService(linking)
                    .oidcUserService(linking.oidc()));
        return http.build();
    }
// end::backend-security-oauth2-linking[]
}
