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
package com.codenameone.examples.bastion;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Order;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.DisabledException;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.HeadersConfigurer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.HttpStatusReturningLogoutSuccessHandler;
import com.codename1.backend.security.LogoutFilter;
import com.codename1.backend.security.RequestMatcher;
import com.codename1.backend.security.SecondFactorAuthenticationFilter;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.UsernamePasswordAuthenticationFilter;
import com.codename1.backend.security.WebAuthnRegistrationFilter;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.mfa.JdbcRecoveryCodeRepository;
import com.codename1.backend.security.mfa.JdbcTotpRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtClaimValidator;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.JdbcRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2TokenContext;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.TokenSettings;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationConverter;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codename1.backend.security.webauthn.JdbcPublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.JdbcUserCredentialRepository;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codenameone.examples.bastion.account.SignInGuard;
import com.codenameone.examples.bastion.audit.Audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/// Who may call what, and how they prove who they are.
///
/// This server is two things at once, with a filter chain for each, as Savor's
/// is:
///
/// - **An authorization server.** The app signs a customer in here -- a
///   password, then a code from their authenticator app if they have one, or
///   a passkey instead of both -- and then runs the authorization-code flow
///   with PKCE for a five-minute access token and a refresh token. Nothing but
///   this server ever sees the password.
/// - **An API.** Everything under `/api` takes that access token as a bearer
///   token, keeps no session, and decides from the account's roles, read
///   fresh on every request, what the caller may do.
///
/// What a bank adds over Savor, each a few lines below: a second factor
/// (`mfa`), passkeys (`webAuthn`), a lockout per account on top of the rate
/// limit per address ([SignInGuard]), stricter headers, shorter tokens that
/// rotate, and an audit record of every sign-in, refused or not.
@Configuration
public class SecurityConfig {
    /// The id the app signs in as. A public client: it holds no secret, since
    /// anything shipped inside an app can be read out of it, and proves itself
    /// with PKCE instead.
    public static final String CLIENT_ID = "bastion-app";
    /// Where the authorization code is sent back to. The app makes the
    /// requests of the flow itself and reads the code out of the redirect, so
    /// this is never opened; it is the loopback address because that is the
    /// one address nobody else can register a listener on.
    public static final String LOOPBACK_REDIRECT = "http://127.0.0.1/callback";
    /// Where the code is sent back to when the app runs in a browser: a path on
    /// this server, which answers with the code it was given. See
    /// [#withWebRedirect].
    public static final String WEB_REDIRECT_PATH = "/signin/code";
    /// Whom the access tokens are for. The API accepts no token issued for
    /// anything else.
    public static final String AUDIENCE = "urn:bastion:api";
    public static final String ISSUER_KEY = "cn1.security.authorizationserver.issuer";
    /// Five minutes. An access token cannot be withdrawn once issued -- the
    /// API checks its signature, not a list -- so its lifetime is how long a
    /// stolen one is good for. The roles behind it are read on every request,
    /// so a suspension still takes effect at once.
    public static final long ACCESS_TOKEN_SECONDS = 300;
    /// Twelve hours of being signed in without the password, a banking app's
    /// usual day. Each refresh hands back a new refresh token and retires the
    /// old one, so a copied refresh token is good until its owner next uses
    /// theirs.
    public static final long REFRESH_TOKEN_SECONDS = 12 * 3600;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // A hash that names its algorithm, `{bcrypt}...`, so a stronger one
        // can be phased in later without anyone's password being reset.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /// The accounts, in the tables `cn1.security.schema.enabled` creates.
    @Bean
    public JdbcUserDetailsManager users(DataSource db) {
        return new JdbcUserDetailsManager(db);
    }

    @Bean
    public RegisteredClientRepository clients(DataSource db,
            @Value("${" + ISSUER_KEY + ":}") final String issuer) throws IOException {
        final JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(db);
        // Written once. A database from before a change to these settings
        // keeps its old row; drop it (or the database, on a development
        // profile) to pick the change up.
        if (clients.findByClientId(CLIENT_ID) == null) {
            clients.save(RegisteredClient.withId("bastion").clientId(CLIENT_ID)
                    .clientName("Bastion")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(LOOPBACK_REDIRECT)
                    .scope("openid").scope("profile")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(ACCESS_TOKEN_SECONDS)
                            .refreshTokenTimeToLive(REFRESH_TOKEN_SECONDS)
                            .reuseRefreshTokens(false)
                            .build())
                    .build());
        }
        // The app in a browser is the same client with one more address: this
        // server's own. It is worked out and not stored, because on a
        // development profile the server has no one address to store -- and a
        // row written by an earlier version would not have it either.
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient client) {
                clients.save(client);
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                return withWebRedirect(clients.findByClientId(clientId), issuer);
            }

            @Override
            public RegisteredClient findById(String id) {
                return withWebRedirect(clients.findById(id), issuer);
            }
        };
    }

    /// The app's registration with [#WEB_REDIRECT_PATH] on this server added
    /// to the addresses a code may be sent back to.
    ///
    /// A browser will not show a page the answer to a request that was
    /// redirected to another origin, so the app running in one cannot read its
    /// code off a redirect to the loopback address the way the installed app
    /// does. It asks for the code to be sent to this server instead, which
    /// hands it straight back ([SignInCodePage]).
    ///
    /// "This server" is the configured issuer where there is one, and is then
    /// the only web address accepted. With none -- a development profile -- it
    /// is the address the request arrived at, the same rule
    /// [#issuedAtThisAddress] holds a token to: a code is only ever sent back
    /// to the server the browser was already talking to.
    static RegisteredClient withWebRedirect(RegisteredClient client, String issuer) {
        if (client == null || !CLIENT_ID.equals(client.getClientId())) {
            return client;
        }
        String here = here(issuer);
        if (here == null) {
            return client;
        }
        RegisteredClient.Builder copy = RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientSettings(client.getClientSettings())
                .tokenSettings(client.getTokenSettings())
                .redirectUri(here + WEB_REDIRECT_PATH);
        for (ClientAuthenticationMethod method : client.getClientAuthenticationMethods()) {
            copy.clientAuthenticationMethod(method);
        }
        for (AuthorizationGrantType grant : client.getAuthorizationGrantTypes()) {
            copy.authorizationGrantType(grant);
        }
        for (String uri : client.getRedirectUris()) {
            copy.redirectUri(uri);
        }
        for (String scope : client.getScopes()) {
            copy.scope(scope);
        }
        for (String resource : client.getResources()) {
            copy.resource(resource);
        }
        return copy.build();
    }


    /// Kept in the database, so a refresh token outlives a restart and is good
    /// on every instance of the server.
    @Bean
    public OAuth2AuthorizationService authorizations(DataSource db) {
        return new JdbcOAuth2AuthorizationService(db);
    }

    /// The keys the tokens are signed with: the files named by
    /// `cn1.security.authorizationserver.jwk.keys`. On a development profile
    /// with none configured, a key made at start-up.
    @Bean
    public JwkSource signingKeys(Config config) throws IOException {
        return AuthorizationServerKeys.load(config);
    }

    /// Authenticator-app secrets, encrypted under
    /// `cn1.security.mfa.encryptionKey` before they reach the database, so a
    /// copy of the database alone does not give anyone the codes. A server
    /// outside a development profile refuses to start without the key.
    @Bean
    public TotpService totp(DataSource db, Config config) {
        return new TotpService(JdbcTotpRepository.fromConfig(db, config), "Bastion");
    }

    /// Recovery codes, kept as salted hashes: shown once, never again.
    @Bean
    public RecoveryCodeService recoveryCodes(DataSource db) {
        return new RecoveryCodeService(new JdbcRecoveryCodeRepository(db));
    }

    /// Passkeys: the public keys, and the random handle each account is known
    /// by to the authenticator (never the e-mail address).
    @Bean
    public UserCredentialRepository passkeys(DataSource db) {
        return new JdbcUserCredentialRepository(db);
    }

    @Bean
    public PublicKeyCredentialUserEntityRepository passkeyHolders(DataSource db) {
        return new JdbcPublicKeyCredentialUserEntityRepository(db);
    }

    /// The headers both chains send. The defaults already include
    /// `nosniff`, `X-Frame-Options: DENY` and `Cache-Control: no-store`; they
    /// are spelled out here so that a reader of this file sees them, and so a
    /// change to the defaults does not change a bank. HSTS is sent only on a
    /// response over TLS this server terminated: two years, subdomains too.
    static void hardened(HeadersConfigurer headers) {
        headers.contentTypeOptions(nosniff -> { })
                .frameOptions(frame -> frame.deny())
                .cacheControl(cache -> { })
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(2 * 365 * 24 * 3600L).includeSubDomains(true));
    }

    /// Signing in. A session exists here and only here, for the few requests
    /// between the password being accepted and the code being handed over;
    /// the app ends it with `POST /logout` once it has its tokens.
    @Bean
    @Order(1)
    public SecurityFilterChain signIn(HttpSecurity http, SignInGuard guard, Audit audit,
            @Value("${" + ISSUER_KEY + ":}") final String issuer,
            @Value("${bastion.signin.per-minute:10}") int signInsPerMinute,
            @Value("${bastion.webauthn.rp-id:localhost}") String rpId,
            @Value("${bastion.webauthn.origins:http://localhost:8080}") String origins)
            throws IOException {
        http.securityMatcher("/oauth2/**", "/.well-known/**", "/userinfo", "/login", "/login/**",
                    "/logout", "/webauthn/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .headers(SecurityConfig::hardened)
            // No CSRF token, for the reason Savor gives: the app posts the
            // forms itself and has no page to read a token from. The session
            // cookie is SameSite=Lax and [#sameOrigin] refuses a change sent by
            // another site's page, which together do the token's work. The
            // passkey ceremonies answer a 32-byte challenge kept in the
            // caller's session, which no other site can read.
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(sameOrigin(issuer), LogoutFilter.class)
            .logout(logout -> logout
                    .logoutRequestMatcher(AntPathRequestMatcher.antMatcher("POST", "/logout"))
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(204)))
            // The answers are statuses, not redirects to pages: the app reads
            // how the request ended. A password accepted for an account with
            // an authenticator app is not a sign-in yet -- `mfa` answers it
            // with a redirect to /login/mfa, and these handlers run only when
            // the code is in.
            .formLogin(form -> form
                    .successHandler(guard.signedIn("password"))
                    .failureHandler(guard.refused("password")))
            .addFilterBefore(guard.lockout(), UsernamePasswordAuthenticationFilter.class)
            .mfa(mfa -> mfa.successHandler(guard.signedIn("password+totp")))
            // The second-factor filter answers a wrong code with a redirect
            // back to its page and offers no failure handler, so a wrong code
            // is recorded from what came back.
            .addFilterBefore(audited(AntPathRequestMatcher.antMatcher("POST", "/login/mfa"),
                    302, "SIGNIN_FAILED", "totp", audit),
                    SecondFactorAuthenticationFilter.class)
            // Passkeys belong to the domain in rpId, and a ceremony is
            // accepted only from the listed origins. The installed Android app
            // reports `android:apk-key-hash:<hash>` as its origin; a release
            // adds that beside the web origin.
            .webAuthn(passkeys -> passkeys
                    .rpId(rpId)
                    .rpName("Bastion")
                    .allowedOrigins(split(origins))
                    .successHandler(guard.signedIn("passkey"))
                    .failureHandler(guard.refused("passkey")))
            .addFilterBefore(audited(AntPathRequestMatcher.antMatcher("POST", "/webauthn/register"),
                    200, "PASSKEY_ADDED", "", audit), WebAuthnRegistrationFilter.class)
            .addFilterBefore(audited(AntPathRequestMatcher.antMatcher("DELETE",
                    "/webauthn/register/**"), 204, "PASSKEY_REMOVED", "", audit),
                    WebAuthnRegistrationFilter.class)
            // Guessing is the attack on this chain: passwords, codes. A limit
            // per route, from one address, unless `bastion.signin.per-minute`
            // says otherwise -- the tests are many people signing in from one
            // address, and their profile raises it. One limiter per rule: a
            // shared one would share its count. The second factor also counts
            // wrong codes per account on its own (cn1.security.mfa.attempts).
            .rateLimit("/login", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(signInsPerMinute, 60))
            .rateLimit("/login/mfa", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(signInsPerMinute, 60))
            .rateLimit("/login/webauthn", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(signInsPerMinute, 60))
            // The access token carries `auth_time`, when the person behind it
            // last signed in. The server records it on the sign-in; a refresh
            // keeps the original, so a token kept alive for twelve hours still
            // says the sign-in was twelve hours ago. Devices reads it: adding a
            // signing key needs a sign-in minutes old, not just any token.
            .authorizationServer(server -> server
                    .settings(AuthorizationServerSettings.builder()
                            .defaultAudience(AUDIENCE).build())
                    .tokenCustomizer(context -> {
                        if (OAuth2TokenContext.ACCESS_TOKEN.equals(context.getTokenType())
                                && context.getAuthorization() != null) {
                            Object at = context.getAuthorization().getAttribute("auth_time");
                            if (at instanceof Number) {
                                context.getClaims().claim("auth_time",
                                        Long.valueOf(((Number) at).longValue()));
                            }
                            // And how: `amr`, which the server records on
                            // the grant. A passkey sign-in (`hwk`) may add a
                            // device without retyping a password.
                            Object amr = context.getAuthorization().getAttribute("amr");
                            if (amr instanceof java.util.List) {
                                context.getClaims().claim("amr",
                                        new java.util.ArrayList<Object>((java.util.List) amr));
                            }
                        }
                    }));
        return http.build();
    }

    /// The API. Stateless: every request stands on its own bearer token.
    @Bean
    @Order(2)
    public SecurityFilterChain api(HttpSecurity http, JwkSource signingKeys,
            @Value("${" + ISSUER_KEY + ":}") final String issuer,
            @Value("${bastion.register.per-hour:20}") int registrationsPerHour,
            @Value("${bastion.transfer.per-minute:20}") int movementsPerMinute,
            JdbcUserDetailsManager users) throws IOException {
        DefaultJwtDecoder decoder = DefaultJwtDecoder.withJwkSource(signingKeys).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtAudienceValidator(AUDIENCE),
                issuer.length() > 0 ? new JwtIssuerValidator(issuer)
                        : new JwtClaimValidator("iss", SecurityConfig::issuedAtThisAddress)));

        // The token says who the caller is and nothing about what they may do.
        // The roles are read from the account on every request, so an admin
        // who suspends an account means now, not when the token runs out.
        JwtAuthenticationConverter roles = new JwtAuthenticationConverter();
        roles.setJwtGrantedAuthoritiesConverter(jwt -> {
            String name = jwt.getSubject();
            if (name == null || !users.userExists(name)) {
                throw new DisabledException("No such account");
            }
            UserDetails account = users.loadUserByUsername(name);
            if (!account.isEnabled() || !account.isAccountNonLocked()) {
                throw new DisabledException("This account is suspended");
            }
            Collection<GrantedAuthority> held = new ArrayList<GrantedAuthority>();
            held.addAll(account.getAuthorities());
            return held;
        });

        http.securityMatcher("/api/**")
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // A JSON API is never framed and never a document: nothing it
            // answers may load anything.
            .headers(headers -> {
                hardened(headers);
                headers.contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'none'; frame-ancestors 'none'"));
            })
            // Opening accounts costs a ledger row and a hash each, and a
            // stranger may ask: per address.
            .rateLimit("/api/account/register", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(registrationsPerHour, 3600))
            // Guessing the current password, or a code, with a token in hand:
            // an hour's allowance is more than anyone honest needs.
            .rateLimit("/api/account/password",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 3600))
            .rateLimit("/api/account/mfa/**",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(10, 600))
            // Moving money, and the steps before it: a script with a stolen
            // token and device key still moves it no faster than a person.
            .rateLimit("/api/transfers",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(movementsPerMinute, 60))
            .rateLimit("/api/payees",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(movementsPerMinute, 60))
            .rateLimit("/api/devices",
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    new InMemoryRateLimiter(movementsPerMinute, 60))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AntPathRequestMatcher.antMatcher("POST",
                            "/api/account/register")).permitAll()
                    .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)
                    .jwtAuthenticationConverter(roles)));
        return http.build();
    }

    /// Records `action` when a request `matcher` takes is answered `status`:
    /// for the steps whose filters have no handler of the application's.
    static SecurityFilter audited(final RequestMatcher matcher, final int status,
            final String action, final String detail, final Audit audit) {
        return (request, chain) -> {
            HttpServer.Response response = chain.doFilter(request);
            if (response != null && response.getStatus() == status && matcher.matches(request)) {
                String who = signedIn();
                audit.append(who, action, who, detail);
            }
            return response;
        };
    }

    /// Who the request is from, or `unknown`: the second-factor step is
    /// nobody yet as far as the chain knows.
    private static String signedIn() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        return who == null || !who.isAuthenticated() || who.getName() == null ? "unknown"
                : who.getName();
    }

    private static String[] split(String list) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        while (start <= list.length()) {
            int comma = list.indexOf(',', start);
            int end = comma < 0 ? list.length() : comma;
            String one = list.substring(start, end).trim();
            if (one.length() > 0) {
                out.add(one);
            }
            start = end + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    /// Refuses a request that changes something and was sent by a page of
    /// another site. A browser names the site a request came from in `Origin`
    /// on every request that is not a plain read, and a page cannot change or
    /// drop it. The installed app is not a browser and sends none.
    static SecurityFilter sameOrigin(final String issuer) {
        return (request, chain) -> {
            String method = request.getMethod();
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                return chain.doFilter(request);
            }
            String origin = request.getHeader("Origin");
            if (origin == null || origin.length() == 0) {
                return chain.doFilter(request);
            }
            String here = here(issuer);
            if (here != null && sameOrigin(origin, here)) {
                return chain.doFilter(request);
            }
            return new HttpServer.Response(403, "application/json",
                    "{\"error\":\"cross_origin\"}".getBytes("UTF-8"))
                    .header("Cache-Control", "no-store");
        };
    }

    /// This server's address: the configured issuer, and with none -- a
    /// development profile -- the address the request in hand arrived at.
    static String here(String issuer) {
        if (issuer != null && issuer.length() > 0) {
            return issuer;
        }
        SecurityExchange exchange = SecurityExchange.current();
        String host = exchange == null ? null : exchange.getRequest().getHeader("Host");
        if (host == null || host.length() == 0) {
            return null;
        }
        return (exchange.isSecure() ? "https://" : "http://") + host;
    }

    /// Whether `origin` is the origin of `address`, compared without regard to
    /// case and without folding it, which would depend on the server's locale.
    static boolean sameOrigin(String origin, String address) {
        int path = address.indexOf('/', address.indexOf("://") + 3);
        String root = path < 0 ? address : address.substring(0, path);
        return origin.equalsIgnoreCase(root);
    }

    /// Whether a token's issuer is the address this request came to; used
    /// only when no issuer is configured, which only a development profile
    /// allows.
    static boolean issuedAtThisAddress(Object issuer) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null || !(issuer instanceof String)) {
            return false;
        }
        String host = exchange.getRequest().getHeader("Host");
        return host != null && ((exchange.isSecure() ? "https://" : "http://") + host)
                .equals(issuer);
    }
}
