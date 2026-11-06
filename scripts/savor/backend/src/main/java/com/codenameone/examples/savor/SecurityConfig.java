/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.savor;


import com.codename1.appkit.server.Grants;
import com.codename1.appkit.server.security.AccountSecurity;
import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Order;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimitKeys;

import java.io.IOException;

/// Who may call what.
///
/// This server is two things at once, and has a filter chain for each:
///
/// - **An authorization server.** The app signs a user in here -- e-mail and
///   password, then the authorization-code flow with PKCE -- and gets a
///   short-lived access token and a refresh token. Nothing but this server ever
///   sees the password.
/// - **An API.** Everything under `/api` takes that access token as a bearer
///   token, keeps no session, and decides from the account's roles what the
///   caller may do.
///
/// The roles are CUSTOMER, COURIER and ADMIN ([Roles]). `/api/courier` is for
/// couriers and `/api/admin` for admins; the rest is for anyone signed in, and
/// acts on the caller's own account, cards, receipts and onboarding application.
/// `/api/onboarding` is that last one, and is deliberately not under
/// `/api/courier`: applying to deliver is what someone does before they may.
@Configuration
public class SecurityConfig {
    public static final String CLIENT_ID = "savor-app";
    public static final String AUDIENCE = "urn:savor:api";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return AccountSecurity.passwordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager users(DataSource db) {
        return AccountSecurity.users(db);
    }

    @Bean
    public RegisteredClientRepository clients(DataSource db,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer) throws IOException {
        return AccountSecurity.clients(db, "savor", CLIENT_ID, "Savor", issuer);
    }

    @Bean
    public OAuth2AuthorizationService authorizations(DataSource db) {
        return AccountSecurity.authorizations(db);
    }

    /// The sign-ins an account has open. The kit declares no beans of its
    /// own, so the server that runs the authorization server declares it here.
    @Bean
    public Grants grants(DataSource db, OAuth2AuthorizationService authorizations) {
        return new Grants(db, authorizations);
    }

    @Bean
    public JwkSource signingKeys(Config config) throws IOException {
        return AccountSecurity.signingKeys(config);
    }

    /// Signing in. Ten tries a minute from one address, unless
    /// `savor.signin.per-minute` says otherwise: the end-to-end tests are many
    /// people signing in from one address, and their profile raises it.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            @Value("${savor.signin.per-minute:10}") int signInsPerMinute) throws IOException {
        return AccountSecurity.signIn(http, issuer, signInsPerMinute, AUDIENCE);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain api(HttpSecurity http, JwkSource signingKeys,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            JdbcUserDetailsManager users) throws IOException {
        http.securityMatcher("/api/**")
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Opening accounts and sending text messages are the two things
            // here that cost something each time and that a stranger can ask
            // for. One limiter per rule: a shared one would share its count.
            .rateLimit("/api/account/register", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(20, 3600))
            .rateLimit("/api/account/phone/**",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 600))
            // Guessing the current password with a token in hand, and filling
            // the database with pictures: an hour's allowance of each is more
            // than anyone honest needs.
            .rateLimit("/api/account/password",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 3600))
            // Every search and every dropped pin is a request this server
            // makes of somebody else's geocoder, in the service's name. A
            // person typing an address makes a few a second at most; a client
            // in a loop would get the whole service refused by the provider.
            .rateLimit("/api/geo/**",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(120, 60))
            .rateLimit("/api/onboarding/application/documents",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(60, 3600))
            // Restaurant photos are the other way to fill the database with
            // pictures: an application's own and a listed menu's. Each route
            // gets its own limiter, for the reason given above.
            .rateLimit("/api/restaurants/application/photo",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(60, 3600))
            .rateLimit("/api/merchant/photos",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(60, 3600))
            // Every card setup is a session made at the payment processor.
            // `Payments` caps how many an account has open; this caps how
            // often it may start one, so abandoning setups in a loop is slow.
            .rateLimit("/api/payments/methods/setup",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(30, 3600))
            // tag::savor-api-rules[]
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST",
                            "/api/account/register")).permitAll()
                    .requestMatchers("/api/courier/**").hasRole(Roles.COURIER)
                    .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                    .requestMatchers("/api/merchant/**").hasRole(Roles.MERCHANT)
                    .anyRequest().authenticated())
            // end::savor-api-rules[]
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                    AccountSecurity.tokenDecoder(signingKeys, issuer, AUDIENCE))
                    .jwtAuthenticationConverter(AccountSecurity.accountRoles(users))));
        return http.build();
    }
}
