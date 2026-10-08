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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Order;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.mfa.JdbcTotpRepository;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtClaimValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.JdbcRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.security.Base32;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The half of the test server that asks who is calling: an OAuth2 authorization
/// server with its sign-in, and a resource server that takes the tokens it issues.
/// The app's sign-in tests drive both the way a real application would.
///
/// ## What is under a chain, and what is not
///
/// Two chains, each limited to its own paths:
///
/// - [#signIn] guards `/oauth2/**`, `/.well-known/**`, `/userinfo`, `/login`,
///   `/login/**`, `/logout` and `/account/**`.
/// - [#secured] guards `/api/secure/**`.
///
/// Everything else -- the screenshot websocket at `/` and `/cn1ss`, `/api/pets`
/// and the rest of `/api` -- matches neither and is served exactly as it was
/// before this class existed, with no sign-in. Neither chain may ever be widened
/// to "any request": every device leg's screenshots arrive unauthenticated.
///
/// A request neither chain claims is not the security layer's at all: however
/// its path is written, it is answered as it was before. A path spelled to be
/// read two ways -- a `;`, `//`, a dot segment, an encoded separator -- is refused
/// with 400 only when it is, or could be read as, a path one of the two chains
/// guards.
///
/// ## The issuer is whatever address the client used
///
/// A device reaches this server as `127.0.0.1`, as `localhost`, as `10.0.2.2`
/// from the Android emulator or by a LAN address, and on whichever port the leg
/// chose, so no one issuer could be written down. The server therefore runs on
/// the `test` profile, and on a development profile an authorization server with
/// no issuer configured takes it from each request's `Host`: the metadata, the
/// `iss` of a redirect and the `iss` of every token name the address that client
/// used. The resource-server chain verifies with the signing keys themselves
/// rather than fetching `/oauth2/jwks` from itself. It still checks who issued a
/// token and whom it is for: the issuer must be the address the request carrying
/// the token came in on -- the one address that device knows this server by --
/// and the audience must be [#AUDIENCE], which is what the authorization server
/// here puts in every access token that asks for nothing else.
///
/// ## Every credential here is a fixture
///
/// The users, their passwords, the TOTP secret and the client id are public test
/// values, repeated in the app's tests. Nothing is a real secret: the signing
/// key and the key the TOTP secret is sealed with are made when the server
/// starts and are gone when it stops, with the in-memory database beside them.
@Configuration
public class SecurityConfig {
    /// The client the app is: public, so PKCE is required of it.
    public static final String CLIENT_ID = "hellocodenameone-app";
    /// The loopback redirect address. RFC 8252 lets it be asked for with any port.
    public static final String LOOPBACK_REDIRECT = "http://127.0.0.1/callback";
    /// The redirect address of a custom scheme, as an installed app registers one.
    public static final String APP_REDIRECT = "com.codenameone.examples.hellocodenameone:/oauth2redirect";
    /// The scope [SecureApi]'s notes are read with; the app's tests ask for it.
    public static final String SCOPE_READ = "notes:read";
    /// The scope the app's tests never ask for, so they can be refused for it.
    public static final String SCOPE_WRITE = "notes:write";

    /// What [SecureApi] is called in a token's `aud`: the authorization server's
    /// default audience, and the one audience the resource chain accepts. A name
    /// and not an address, because this server has no one address.
    public static final String AUDIENCE = "urn:hellocodenameone:secure-api";

    /// A user with a password and nothing else.
    public static final String USER = "ada";
    public static final String USER_PASSWORD = "ada-test-password";
    /// A user who also has an authenticator app.
    public static final String MFA_USER = "grace";
    public static final String MFA_USER_PASSWORD = "grace-test-password";
    /// That app's secret, in Base32: the twenty bytes `12345678901234567890`,
    /// which is the SHA-1 test secret of RFC 6238. Codes are six digits over
    /// SHA-1 on a 30 second step, so `com.codename1.security.Otp` computes them
    /// from this on the device.
    public static final String MFA_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /// The users, in the `cn1_users` table of the security schema. The database is
    /// new at every start, and a second server of one process -- a test's -- may
    /// find them there already.
    @Bean
    public UserDetailsService users(DataSource db, PasswordEncoder encoder) {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(db);
        if (!users.userExists(USER)) {
            users.createUser(User.withUsername(USER).password(encoder.encode(USER_PASSWORD))
                    .roles("USER").build());
        }
        if (!users.userExists(MFA_USER)) {
            users.createUser(User.withUsername(MFA_USER)
                    .password(encoder.encode(MFA_USER_PASSWORD)).roles("USER").build());
        }
        return users;
    }

    /// One-time codes, with [#MFA_USER] already enrolled under [#MFA_SECRET]. The
    /// secret is stored sealed, under a key made here: it only has to last as
    /// long as the in-memory database it seals rows of.
    @Bean
    public TotpService totp(DataSource db) throws IOException {
        JdbcTotpRepository secrets = new JdbcTotpRepository(db, Crypto.randomBytes(32));
        TotpService totp = new TotpService(secrets, "Hello Codename One");
        if (!totp.isEnabled(MFA_USER)) {
            secrets.save(MFA_USER, Base32.decode(MFA_SECRET));
            secrets.confirm(MFA_USER);
        }
        return totp;
    }

    /// The one client, in `cn1_oauth2_registered_client`.
    @Bean
    public RegisteredClientRepository clients(DataSource db) {
        JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(db);
        if (clients.findByClientId(CLIENT_ID) == null) {
            clients.save(RegisteredClient.withId("hellocodenameone").clientId(CLIENT_ID)
                    .clientName("Hello Codename One")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                    .redirectUri(LOOPBACK_REDIRECT).redirectUri(APP_REDIRECT)
                    .scope("openid").scope("profile").scope(SCOPE_READ).scope(SCOPE_WRITE)
                    .build());
        }
        return clients;
    }

    /// Grants, codes and refresh tokens, in `cn1_oauth2_authorization` and
    /// `cn1_oauth2_token`.
    @Bean
    public OAuth2AuthorizationService authorizations(DataSource db) {
        return new JdbcOAuth2AuthorizationService(db);
    }

    /// The signing key: an RSA key made at start, never written anywhere. Every
    /// restart invalidates every token, which is right for a fixture, and the
    /// same call makes the key on the JVM and in the native server.
    @Bean
    public JwkSource signingKeys() throws IOException {
        List<Jwk> keys = new ArrayList<Jwk>();
        keys.add(AuthorizationServerKeys.usable(Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)),
                "the test server's generated key"));
        return AuthorizationServerKeys.of(keys);
    }

    /// The authorization server and how its users sign in: a form, HTTP Basic on
    /// the request itself, and a one-time code for a user who has enrolled.
    ///
    /// CSRF protection is off for this chain, deliberately. Its clients are the
    /// app's tests -- programs posting `/login`, `/login/mfa` and the device
    /// verification form with `ConnectionRequest` -- and a token they would have
    /// to scrape out of a page first protects nothing in a fixture that holds no
    /// real account. The endpoints a client calls as itself (`/oauth2/token`,
    /// `/oauth2/device_authorization`, `/oauth2/revoke`, `/userinfo`) are exempt
    /// whatever this says. The device verification form keeps its own one-use
    /// `ticket`, which no setting removes.
    ///
    /// The two limits the layer keeps itself are raised in `application.properties`
    /// from a handful in five minutes to ten a second: the device codes one user
    /// may try, and the attempts at a one-time code. Both are counted per user,
    /// every test user here is `ada` or `grace`, and a suite run again against a
    /// server left up would otherwise be answered 429 by the run before it.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http) {
        http.securityMatcher("/oauth2/**", "/.well-known/**", "/userinfo", "/login", "/login/**",
                    "/logout", "/account/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.defaultSuccessUrl("/account/me"))
            .httpBasic(basic -> basic.realmName("hellocodenameone"))
            .mfa(mfa -> mfa.defaultSuccessUrl("/account/me"))
            .authorizationServer(server -> server
                    .settings(AuthorizationServerSettings.builder()
                            .defaultAudience(AUDIENCE).build()));
        return http.build();
    }

    /// Whether `issuer` is this server as the request being served names it: the
    /// scheme it arrived over and its `Host`. A token carried to another name for
    /// the same server is refused, as a real resource server refuses one from an
    /// issuer it was not told about.
    static boolean issuedAtThisAddress(Object issuer) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null || !(issuer instanceof String)) {
            return false;
        }
        String host = exchange.getRequest().getHeader("Host");
        return host != null && ((exchange.isSecure() ? "https://" : "http://") + host)
                .equals(issuer);
    }

    /// The routes of [SecureApi]: a bearer token on every request and no session.
    /// The token is verified against [#signingKeys] directly; see the class
    /// comment for why not through the issuer's address, and for what is asked
    /// of its issuer and audience instead.
    @Bean
    @Order(2)
    public SecurityFilterChain secured(HttpSecurity http, JwkSource signingKeys) {
        DefaultJwtDecoder decoder = DefaultJwtDecoder.withJwkSource(signingKeys).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtAudienceValidator(AUDIENCE),
                new JwtClaimValidator("iss", SecurityConfig::issuedAtThisAddress)));
        http.securityMatcher("/api/secure/**")
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("GET", "/api/secure/notes"))
                        .hasAuthority("SCOPE_" + SCOPE_READ)
                    .requestMatchers("/api/secure/notes", "/api/secure/notes/**",
                            "/api/secure/admin")
                        .hasAuthority("SCOPE_" + SCOPE_WRITE)
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)));
        return http.build();
    }
}
