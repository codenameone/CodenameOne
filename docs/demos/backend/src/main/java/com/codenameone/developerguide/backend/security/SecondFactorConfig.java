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
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.JdbcTotpRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.ratelimit.JdbcRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimiter;
import com.codename1.backend.security.webauthn.JdbcPublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.JdbcUserCredentialRepository;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;

/** The Backend security chapter's second factor and passkey examples. */
@Configuration
public class SecondFactorConfig {
// tag::backend-security-mfa[]
    @Bean
    TotpService totp(DataSource dataSource, Config config) {
        return new TotpService(JdbcTotpRepository.fromConfig(dataSource, config), "Acme");
    }

    @Bean
    RecoveryCodeService recoveryCodes(DataSource dataSource) {
        return new RecoveryCodeService(new JdbcRecoveryCodeRepository(dataSource));
    }

    @Bean
    SecurityFilterChain web(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .mfa(Customizer.withDefaults());
        return http.build();
    }
// end::backend-security-mfa[]

// tag::backend-security-limiter-bean[]
    @Bean
    RateLimiter limits(DataSource dataSource) {
        return new JdbcRateLimiter(dataSource, "api", 600, 60);
    }
// end::backend-security-limiter-bean[]

// tag::backend-security-webauthn-store[]
    @Bean
    UserCredentialRepository passkeys(DataSource dataSource) {
        return new JdbcUserCredentialRepository(dataSource);
    }

    @Bean
    PublicKeyCredentialUserEntityRepository passkeyUsers(DataSource dataSource) {
        return new JdbcPublicKeyCredentialUserEntityRepository(dataSource);
    }
// end::backend-security-webauthn-store[]
}
