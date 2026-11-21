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
package com.codenameone.examples.bazaar;


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
import com.codenameone.examples.bazaar.web.security.Throttle;

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
/// The roles are CUSTOMER, MERCHANT and ADMIN ([Roles]). `/api/merchant` is for
/// store staff and `/api/admin` for admins; the rest is for anyone signed in,
/// and acts on the caller's own account, cards and receipts.
@Configuration
public class SecurityConfig {
    public static final String CLIENT_ID = "bazaar-app";
    public static final String AUDIENCE = "urn:bazaar:api";
    /// A storefront signup form's submission; open to guests.
    static final String SIGNUP_SUBMIT = "/api/shop/stores/*/signup-forms/*/submit";
    /// A back-in-stock alert; open to guests, who give an e-mail address.
    static final String STOCK_ALERT = "/api/shop/stores/*/stock-alerts";

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
        return AccountSecurity.clients(db, "bazaar", CLIENT_ID, "Bazaar", issuer);
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
    /// `bazaar.signin.per-minute` says otherwise: the end-to-end tests are many
    /// people signing in from one address, and their profile raises it.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            @Value("${bazaar.signin.per-minute:10}") int signInsPerMinute) throws IOException {
        return AccountSecurity.signIn(http, issuer, signInsPerMinute, AUDIENCE);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain api(HttpSecurity http, JwkSource signingKeys,
            @Value("${" + AccountSecurity.ISSUER_KEY + ":}") String issuer,
            JdbcUserDetailsManager users, Throttle throttle) throws IOException {
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
            // Guessing the current password with a token in hand: an hour's
            // allowance is more than anyone honest needs.
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
            // Every card setup is a session made at the payment processor.
            // `Payments` caps how many an account has open; this caps how
            // often it may start one, so abandoning setups in a loop is slow.
            // A gift card's code is its money. Looking one up, or putting
            // one on a cart, answers whether a code exists, so both are slow
            // enough that guessing 16 characters from 32 is hopeless.
            .rateLimit("/api/shop/gift-cards/**",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 60))
            .rateLimit("/api/shop/cart/*/gift-card",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 60))
            .rateLimit("/api/payments/methods/setup",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(30, 3600))
            // The two things a stranger may post: a newsletter signup and a
            // back-in-stock alert. Both write a row and may send a mail, so
            // they share the web forms' bucket (`bazaar.limit.form`) per
            // address; each also drops a filled honeypot and refuses a
            // malformed address before anything is written.
            .rateLimit(SIGNUP_SUBMIT, RateLimitKeys.clientAddress(), throttle.limiter("form"))
            .rateLimit(STOCK_ALERT, RateLimitKeys.clientAddress(), throttle.limiter("form"))
            // tag::bazaar-api-rules[]
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST",
                            "/api/account/register")).permitAll()
                    // The storefront: stores, products and pictures anyone may
                    // browse. Only reads, and only what is ACTIVE.
                    .requestMatchers(AntPathRequestMatcher.antMatcher("GET",
                            "/api/shop/**")).permitAll()
                    // A cart is usable before signing in: a guest's is found
                    // by the token its first answer carried.
                    .requestMatchers("/api/shop/cart/**").permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST", SIGNUP_SUBMIT)).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST", STOCK_ALERT)).permitAll()
                    .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                    .requestMatchers("/api/merchant/**").hasRole(Roles.MERCHANT)
                    .anyRequest().authenticated())
            // end::bazaar-api-rules[]
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(
                    AccountSecurity.tokenDecoder(signingKeys, issuer, AUDIENCE))
                    .jwtAuthenticationConverter(AccountSecurity.accountRoles(users))));
        return http.build();
    }
}
