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
package com.codenameone.examples.reel;


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
/// The roles are VIEWER, CREATOR, MODERATOR and ADMIN ([Roles]).
/// `/api/admin` is for admins and `/api/moderation` for moderators and admins;
/// the studio checks the creator role itself, because a feature flag can open
/// uploads to everyone. The rest is for anyone signed in and acts on the
/// caller's own account, profiles, list and subscription.
@Configuration
public class SecurityConfig {
    public static final String CLIENT_ID = "reel-app";
    public static final String AUDIENCE = "urn:reel:api";

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
        return AccountSecurity.clients(db, "reel", CLIENT_ID, "Reel", issuer);
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
    /// `reel.signin.per-minute` says otherwise: the end-to-end tests are many
    /// people signing in from one address, and their profile raises it.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            @Value("${reel.signin.per-minute:10}") int signInsPerMinute) throws IOException {
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
            // Opening accounts is the one thing a stranger can ask for that
            // writes a row each time. One limiter per rule: a shared one would
            // share its count.
            .rateLimit("/api/signup", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(20, 3600))
            // Guessing the current password with a token in hand.
            .rateLimit("/api/me/password",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 3600))
            // Certificate codes are short enough to type; guessing them in a
            // loop is what this slows down.
            .rateLimit("/api/verify/**", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(60, 60))
            // Every playback start mints tokens and may raise a sharing alert;
            // a person starts a few a minute at most.
            // Profile PINs are four digits.
            .rateLimit("/api/profiles/*/unlock",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 900))
            .rateLimit("/api/playback/start",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(30, 60))
            // tag::reel-api-rules[]
            // The media origin, the license server, the worker API, the
            // certificate page and the shared-title page are outside this
            // chain: each checks its own signed token (MediaTokens, the
            // worker's bearer secret) or shows only public facts, and none of
            // them takes the user's access token.
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST", "/api/signup")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("GET", "/api/config")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("GET", "/api/plans")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("GET", "/api/verify/**")).permitAll()
                    .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                    .requestMatchers("/api/moderation/**").hasAnyRole(Roles.MODERATOR, Roles.ADMIN)
                    .anyRequest().authenticated())
            // end::reel-api-rules[]
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                    AccountSecurity.tokenDecoder(signingKeys, issuer, AUDIENCE))
                    .jwtAuthenticationConverter(AccountSecurity.accountRoles(users))));
        return http.build();
    }
}
