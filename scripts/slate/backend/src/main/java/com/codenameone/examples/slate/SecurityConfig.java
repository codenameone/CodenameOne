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
package com.codenameone.examples.slate;

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
/// The roles are CLIENT, PROVIDER and OWNER ([Roles]). `/api/provider` is for
/// providers, `/api/owner` and `/api/admin` for owners -- and an owner may do
/// what a provider does, since in a small business they are often both. The
/// rest is for anyone signed in, and acts on the caller's own account,
/// appointments, waiting list and cards.
@Configuration
public class SecurityConfig {
    /// The id the app signs in as.
    public static final String CLIENT_ID = "slate-app";
    /// Whom the access tokens are for: the API accepts no token issued for
    /// anything else.
    public static final String AUDIENCE = "urn:slate:api";

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
        return AccountSecurity.clients(db, "slate", CLIENT_ID, "Slate", issuer);
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
    /// `slate.signin.per-minute` says otherwise: the end-to-end tests are many
    /// people signing in from one address, and their profile raises it.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            @Value("${slate.signin.per-minute:10}") int signInsPerMinute) throws IOException {
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
            // A hold takes a slot from everyone else for minutes. A client
            // books a few at a time; this caps how fast a script could take a
            // whole day's diary out of circulation.
            .rateLimit("/api/appointments/hold",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(60, 3600))
            // A device calendar pushes its busy times whenever it changes.
            .rateLimit("/api/provider/busy",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(120, 3600))
            // Every card setup is a session made at the payment processor.
            // `Payments` caps how many an account has open; this caps how
            // often it may start one, so abandoning setups in a loop is slow.
            .rateLimit("/api/payments/methods/setup",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(30, 3600))
            // tag::slate-api-rules[]
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST",
                            "/api/account/register")).permitAll()
                    .requestMatchers("/api/provider/**").hasRole(Roles.PROVIDER)
                    .requestMatchers("/api/owner/**").hasRole(Roles.OWNER)
                    .requestMatchers("/api/admin/**").hasRole(Roles.OWNER)
                    .anyRequest().authenticated())
            // end::slate-api-rules[]
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                    AccountSecurity.tokenDecoder(signingKeys, issuer, AUDIENCE))
                    .jwtAuthenticationConverter(AccountSecurity.accountRoles(users))));
        return http.build();
    }
}
