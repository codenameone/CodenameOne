/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.mail.EmailVerificationService;
import com.codename1.backend.security.mail.JdbcOneTimeTokenRepository;
import com.codename1.backend.security.mail.JdbcVerifiedEmailRepository;
import com.codename1.backend.security.mail.OneTimeTokenService;
import com.codename1.backend.security.mail.PasswordResetService;
import com.codename1.backend.security.mfa.EmailCodeService;
import com.codename1.backend.security.mfa.JdbcEmailCodeRepository;

import java.io.IOException;

/** The Backend security chapter's examples of the flows that send mail. */
@Configuration
public class MailSecurityConfig {
// tag::backend-security-reset[]
    @Bean
    OneTimeTokenService oneTimeTokens(DataSource dataSource) {
        return new OneTimeTokenService(new JdbcOneTimeTokenRepository(dataSource));
    }

    @Bean
    PasswordResetService passwordReset(OneTimeTokenService tokens, MailSender mail,
            JdbcUserDetailsManager users, PasswordEncoder encoder) {
        return new PasswordResetService(tokens, mail, users, users, encoder);
    }

    @Bean
    SecurityFilterChain web(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .passwordReset(Customizer.withDefaults());
        return http.build();
    }
// end::backend-security-reset[]

// tag::backend-security-verify[]
    @Bean
    EmailVerificationService emailVerification(OneTimeTokenService tokens, MailSender mail,
            DataSource dataSource) {
        return new EmailVerificationService(tokens, mail,
                new JdbcVerifiedEmailRepository(dataSource));
    }

    @Bean
    SecurityFilterChain shop(HttpSecurity http, EmailVerificationService verification) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/checkout/**").access(verification.verified())
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .emailVerification(Customizer.withDefaults());
        return http.build();
    }
// end::backend-security-verify[]

// tag::backend-security-verify-send[]
    void afterSignup(EmailVerificationService verification, String username, String email)
            throws IOException {
        verification.send(username, email, "https://example.com/verify-email");
    }
// end::backend-security-verify-send[]

// tag::backend-security-email-code[]
    @Bean
    EmailCodeService emailCodes(DataSource dataSource, MailSender mail) {
        return new EmailCodeService(new JdbcEmailCodeRepository(dataSource), mail);
    }

    @Bean
    SecurityFilterChain account(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .mfa(Customizer.withDefaults());
        return http.build();
    }
// end::backend-security-email-code[]
}
