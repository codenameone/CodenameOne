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

import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Migrations;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.CookieCsrfTokenRepository;
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.HttpStatusEntryPoint;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecuritySchema;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.apikey.ApiKeyGenerator;
import com.codename1.backend.security.apikey.GeneratedApiKey;
import com.codename1.backend.security.apikey.JdbcApiKeyRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpEnrollment;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.resource.JwtIssuerAuthenticationManagerResolver;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.JdbcRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codename1.backend.security.rememberme.JdbcTokenRepository;
import java.io.IOException;
import java.util.List;

/** The Backend security chapter's shorter examples, compiled so they cannot drift. */
public final class SecuritySnippets {

    private SecuritySnippets() {
    }

    public static void rules(HttpSecurity http) {
// tag::backend-security-rules[]
http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/css/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers(AntPathRequestMatcher.antMatcher("DELETE", "/api/**"))
                .hasAuthority("orders:delete")
        .requestMatchers("/account/password").fullyAuthenticated()
        .anyRequest().authenticated());
// end::backend-security-rules[]
    }

    public static void formLogin(HttpSecurity http) {
// tag::backend-security-form[]
http.formLogin(form -> form
        .loginPage("/signin")
        .defaultSuccessUrl("/home")
        .permitAll())
    .logout(logout -> logout
        .logoutUrl("/signout")
        .logoutSuccessUrl("/")
        .permitAll());
// end::backend-security-form[]
    }

    public static void basic(HttpSecurity http) {
// tag::backend-security-basic[]
http.securityMatcher("/internal/**")
    .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SERVICE"))
    .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .httpBasic(basic -> basic.realmName("Internal API"));
// end::backend-security-basic[]
    }

    public static void csrf(HttpSecurity http) {
// tag::backend-security-csrf[]
http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringRequestMatchers("/webhooks/**"));
// end::backend-security-csrf[]
    }

    public static void sessions(HttpSecurity http) {
// tag::backend-security-session[]
http.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        .sessionFixation(fixation -> fixation.newSession()));
// end::backend-security-session[]
    }

    public static void headers(HttpSecurity http) {
// tag::backend-security-headers[]
http.headers(headers -> headers
        .frameOptions(frame -> frame.sameOrigin())
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")));
// end::backend-security-headers[]
    }

    public static void handling(HttpSecurity http) {
// tag::backend-security-handling[]
http.exceptionHandling(handling -> handling
        .authenticationEntryPoint(new HttpStatusEntryPoint(401))
        .accessDeniedHandler((request, denied) ->
                HttpServer.Response.json(403, "{\"error\":\"forbidden\"}")));
// end::backend-security-handling[]
    }

    public static String currentUser() {
// tag::backend-security-current[]
Authentication who = SecurityContextHolder.getContext().getAuthentication();
String name = who == null ? null : who.getName();
// end::backend-security-current[]
        return name;
    }

    public static void issuers(HttpSecurity http) {
// tag::backend-security-jwt-issuers[]
http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(
        JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(
                "https://login.example.com", "https://partners.example.com")));
// end::backend-security-jwt-issuers[]
    }

    public static void apiKeys(HttpSecurity http) {
// tag::backend-security-apikey[]
http.securityMatcher("/api/**")
    .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/deploy/**").hasAuthority("SCOPE_deploy")
            .anyRequest().authenticated())
    .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .apiKey(Customizer.withDefaults());
// end::backend-security-apikey[]
    }

    public static String issueKey(JdbcApiKeyRepository repository) throws IOException {
// tag::backend-security-apikey-generate[]
GeneratedApiKey made = new ApiKeyGenerator().generate("ci-bot", "deploy", "read");
repository.save(made.getApiKey());        // the hash, the owner and the scopes
String shownOnce = made.getPlaintext();   // cn1_...; it can't be read back later
// end::backend-security-apikey-generate[]
        return shownOnce;
    }

    public static void rateLimits(HttpSecurity http, DataSource dataSource) {
// tag::backend-security-ratelimit[]
http.rateLimit(AntPathRequestMatcher.antMatcher("POST", "/login"),
        RateLimitKeys.clientAddress(), new JdbcRateLimiter(dataSource, "login", 5, 60));
http.rateLimit("/api/**",
        RateLimitKeys.firstOf(RateLimitKeys.apiKeyId(), RateLimitKeys.principal()),
        new InMemoryRateLimiter(600, 60));
// end::backend-security-ratelimit[]
    }

    public static void oauth2Login(HttpSecurity http) {
// tag::backend-security-oauth2-login[]
http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
    .oauth2Login(Customizer.withDefaults());
// end::backend-security-oauth2-login[]
    }

    public static void rememberMe(HttpSecurity http, DataSource dataSource) {
// tag::backend-security-remember[]
http.formLogin(Customizer.withDefaults())
    .rememberMe(remember -> remember
            .tokenRepository(new JdbcTokenRepository(dataSource))
            .tokenValiditySeconds(30 * 24 * 3600));
// end::backend-security-remember[]
    }

    public static String enrol(TotpService totp, String username) {
// tag::backend-security-totp-begin[]
TotpEnrollment enrollment = totp.beginEnrollment(username);
String forQrCode = enrollment.getOtpauthUri();   // otpauth://totp/...
String forTyping = enrollment.getSecret();       // the same secret, in Base32
// end::backend-security-totp-begin[]
        return forQrCode + forTyping;
    }

    public static List<String> confirm(TotpService totp, RecoveryCodeService recoveryCodes,
                                       String username, String code) {
// tag::backend-security-totp-confirm[]
if (!totp.confirmEnrollment(username, code)) {
    return null;                                  // a wrong code: nothing changed
}
List<String> shownOnce = recoveryCodes.generate(username);
// end::backend-security-totp-confirm[]
        return shownOnce;
    }

    public static void webAuthn(HttpSecurity http) {
// tag::backend-security-webauthn[]
http.formLogin(Customizer.withDefaults())
    .webAuthn(passkeys -> passkeys
            .rpId("example.com")
            .rpName("Example")
            .allowedOrigins("https://example.com",
                    "android:apk-key-hash:Zm9vYmFyZm9vYmFyZm9vYmFyZm9vYmFyZm9vYmFyZm9"));
// end::backend-security-webauthn[]
    }

    public static void webAuthnForApps(HttpSecurity http) {
// tag::backend-security-webauthn-csrf[]
http.csrf(csrf -> csrf.ignoringRequestMatchers(
        AntPathRequestMatcher.antMatcher("POST", "/webauthn/**"),
        AntPathRequestMatcher.antMatcher("POST", "/login/webauthn")));
// end::backend-security-webauthn-csrf[]
    }

    public static void audience(HttpSecurity http) {
// tag::backend-security-authserver-audience[]
http.authorizationServer(as -> as.settings(AuthorizationServerSettings.builder()
        .issuer("https://id.example.com")
        .defaultAudience("https://api.example.com")
        .build()));
// end::backend-security-authserver-audience[]
    }

    public static void schema() {
// tag::backend-security-schema-register[]
Migrations.register(SecuritySchema.migrations());
// end::backend-security-schema-register[]
    }
}
