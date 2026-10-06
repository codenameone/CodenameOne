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
package com.codename1.backend.security;

import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoder;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationServer;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import com.codename1.backend.security.oauth2.server.authorization.OidcUserInfoMapper;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.io.IOException;

/// Makes the server an OAuth2 authorization server and OpenID Connect
/// provider.
///
/// ```java
/// @Bean
/// SecurityFilterChain web(HttpSecurity http) {
///     http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
///         .formLogin(Customizer.withDefaults())
///         .authorizationServer(Customizer.withDefaults());
///     return http.build();
/// }
///
/// @Bean
/// RegisteredClientRepository clients() {
///     return new InMemoryRegisteredClientRepository(RegisteredClient.withId("app")
///             .clientId("acme-app")
///             .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
///             .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
///             .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
///             .redirectUri("com.acme.app:/oauth2redirect")
///             .scope("openid").scope("profile").build());
/// }
/// ```
///
/// ## Endpoints
///
/// | Path | |
/// |---|---|
/// | `GET /oauth2/authorize` | the authorization endpoint, for the signed-in user |
/// | `POST /oauth2/token` | the token endpoint |
/// | `GET /oauth2/jwks` | the public halves of the signing keys |
/// | `POST /oauth2/revoke` | revocation (RFC 7009) |
/// | `POST /oauth2/device_authorization` | starts a device grant (RFC 8628) |
/// | `/oauth2/device_verification` | the page where the signed-in user types the device's code |
/// | `GET /userinfo` | the user's claims, for an access token granted `openid` |
/// | `GET /.well-known/openid-configuration`, `/.well-known/oauth-authorization-server` | the metadata |
///
/// The paths can be changed, and the issuer must be given outside a
/// development profile; see [AuthorizationServerSettings].
///
/// ## Signing in
///
/// The authorization endpoint and the device verification page are for a
/// signed-in user, and how a user signs in is whatever else the chain
/// declares: `formLogin`, `oauth2Login`, a second factor. A browser that
/// arrives signed out is sent to sign in and comes back to the request it
/// made. A client that is not a browser -- its `Accept` does not name
/// `text/html` -- is answered `401` with `login_required` instead.
///
/// ## Grants
///
/// - `authorization_code`, with PKCE: required of a public client, and S256
///   only. A code works once, for five minutes; presenting one again later
///   revokes what its use was issued. (Presented twice within ten seconds --
///   a retry, or two processes handed one request -- one is answered and the
///   other refused, and nothing is revoked.) The redirect address must be one the
///   client registered, exactly -- see
///   [com.codename1.backend.security.oauth2.server.authorization.RegisteredClient]
///   -- and a request whose client or address is wrong is answered with a 400
///   and never a redirect.
/// - `refresh_token`: opaque, stored as a hash, replaced on every use; using
///   one that was replaced revokes the grant, with the same ten seconds'
///   allowance for a client that refreshes twice at once. A public client is issued one
///   too -- the rotation is what protects it there.
/// - `client_credentials`, for a client with a secret.
/// - `urn:ietf:params:oauth:grant-type:device_code`: the device shows a code
///   of eight letters, the user types it at the verification page, is asked
///   -- by name -- whether to let that client in, and approves; the answer
///   counts only from the question this server put to that session. The
///   device's polling is answered `authorization_pending`,
///   `slow_down`, `access_denied` or `expired_token` until it is.
///
/// A client is never granted a scope it was not registered with, and there is
/// no consent page: registering a client with a scope is the consent.
///
/// ## Tokens and keys
///
/// An access token is a JWT of type `at+jwt` (RFC 9068). Its `aud` is the
/// resource server it is for: what the request named with the `resource`
/// parameter (RFC 8707) -- at the authorization endpoint, the device
/// authorization endpoint or the token endpoint, where it may only narrow what
/// the grant was made for -- or else the server's default audience, which is
/// the issuer unless
/// [AuthorizationServerSettings.Builder#defaultAudience] or
/// `cn1.security.authorizationserver.audience` says otherwise. A client
/// registered with resources may ask for those only; `invalid_target` answers
/// anything else. The client the token was issued to is its `client_id`
/// claim. An ID token is for the client, and its `aud` is the client id: it
/// carries
/// `nonce`, `auth_time`, `azp` and `at_hash`, and both are signed RS256 -- or
/// ES256 when the signing key is a P-256 one. The keys are the application's
/// [JwkSource] bean, or the files named by
/// `cn1.security.authorizationserver.jwk.keys`; see [AuthorizationServerKeys],
/// which is also how an application signs tokens of its own with them.
///
/// Access tokens are verified by their signature alone, anywhere: revoking a
/// grant stops its refresh token and its answers at `/userinfo`, and an
/// access token already issued stays good until it expires. Keep them short.
///
/// ## What is kept, and where
///
/// The clients are the application's [RegisteredClientRepository] bean. The
/// grants are its [OAuth2AuthorizationService] bean; without one they are kept
/// in this process, which a line at start-up says, and are lost when it stops
/// and unknown to any other process. Client secrets are compared through the
/// application's [PasswordEncoder] bean, or the one given to
/// [#clientSecretEncoder]. With neither, a server whose clients are all known
/// when it starts -- an
/// [com.codename1.backend.security.oauth2.server.authorization.InMemoryRegisteredClientRepository]
/// -- does not start if one of them has a secret. One whose clients are in a
/// table starts, says once that it cannot check secrets, and refuses a client
/// that presents one with `invalid_client`.
///
/// ## Not here
///
/// A consent page, dynamic client registration, token introspection, opaque
/// access tokens, `private_key_jwt` and mutual TLS client authentication,
/// pushed authorization requests, DPoP, encrypted tokens, the implicit and
/// password grants, and back-channel or RP-initiated logout.
public final class AuthorizationServerConfigurer extends SecurityConfigurer {
    /// The setting that holds how many device codes one user may try at the
    /// verification page in one window; 10 unless set.
    public static final String DEVICE_VERIFICATION_ATTEMPTS =
            "cn1.security.authorizationserver.device.verificationAttempts";
    /// The setting that holds the length of that window in seconds; 300
    /// unless set.
    public static final String DEVICE_VERIFICATION_WINDOW =
            "cn1.security.authorizationserver.device.verificationWindowSeconds";

    private AuthorizationServerSettings settings;
    private RegisteredClientRepository clients;
    private OAuth2AuthorizationService authorizations;
    private JwkSource keys;
    private JwtEncoder encoder;
    private PasswordEncoder secrets;
    private OAuth2TokenCustomizer customizer;
    private OidcUserInfoMapper userInfo;
    private RateLimiter verificationLimiter;
    private Clock clock;

    AuthorizationServerConfigurer() {
    }

    /// The issuer and the paths; see [AuthorizationServerSettings].
    public AuthorizationServerConfigurer settings(AuthorizationServerSettings settings) {
        this.settings = settings;
        return this;
    }

    /// The clients, in place of the application's bean.
    public AuthorizationServerConfigurer registeredClientRepository(
            RegisteredClientRepository registeredClientRepository) {
        this.clients = registeredClientRepository;
        return this;
    }

    /// Where grants are kept, in place of the application's bean.
    public AuthorizationServerConfigurer authorizationService(
            OAuth2AuthorizationService authorizationService) {
        this.authorizations = authorizationService;
        return this;
    }

    /// The signing keys, the first of them signing, in place of the
    /// application's bean and the configuration.
    public AuthorizationServerConfigurer jwkSource(JwkSource jwkSource) {
        this.keys = jwkSource;
        return this;
    }

    /// What signs, in place of the application's bean. It must sign with the
    /// keys of [#jwkSource].
    public AuthorizationServerConfigurer jwtEncoder(JwtEncoder jwtEncoder) {
        this.encoder = jwtEncoder;
        return this;
    }

    /// What client secrets were encoded with, in place of the application's
    /// [PasswordEncoder] bean.
    public AuthorizationServerConfigurer clientSecretEncoder(PasswordEncoder passwordEncoder) {
        this.secrets = passwordEncoder;
        return this;
    }

    /// Changes the claims of every token before it is signed.
    public AuthorizationServerConfigurer tokenCustomizer(OAuth2TokenCustomizer tokenCustomizer) {
        this.customizer = tokenCustomizer;
        return this;
    }

    /// What the server says of a user; see [OidcUserInfoMapper].
    public AuthorizationServerConfigurer userInfoMapper(OidcUserInfoMapper userInfoMapper) {
        this.userInfo = userInfoMapper;
        return this;
    }

    /// What bounds how many device codes one user may try at the verification
    /// page, in place of what the application's [RateLimiter] bean would
    /// count and of the count kept in this process.
    ///
    /// Ten tries in five minutes are allowed unless
    /// `cn1.security.authorizationserver.device.verificationAttempts` and
    /// `cn1.security.authorizationserver.device.verificationWindowSeconds`
    /// say otherwise. With no limiter they are counted in this process. With
    /// one [RateLimiter] bean they are counted wherever it counts -- a
    /// [com.codename1.backend.security.ratelimit.JdbcRateLimiter] makes that
    /// every process -- in a limiter the bean derives, with that limit; see
    /// [RateLimiter#derive]. A limiter given here, or a bean that derives
    /// none, counts by its own limit, and setting the two keys as well is
    /// then refused when the chain is built, since they would decide nothing.
    public AuthorizationServerConfigurer deviceVerificationRateLimiter(RateLimiter rateLimiter) {
        this.verificationLimiter = rateLimiter;
        return this;
    }

    /// The clock every lifetime is measured on; for tests.
    public AuthorizationServerConfigurer clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    private AuthorizationServerSettings settings() {
        if (settings == null) {
            settings = AuthorizationServerSettings.builder().build();
        }
        return settings;
    }

    @Override
    public void init(HttpSecurity http) {
        AuthorizationServerSettings s = settings();
        // What a client calls as itself carries no session to ride on: its
        // credentials, or a bearer token, are in the request.
        CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            csrf.ignoringRequestMatchers(
                    AntPathRequestMatcher.antMatcher("POST", s.getTokenEndpoint()),
                    AntPathRequestMatcher.antMatcher("POST", s.getTokenRevocationEndpoint()),
                    AntPathRequestMatcher.antMatcher("POST", s.getDeviceAuthorizationEndpoint()),
                    AntPathRequestMatcher.antMatcher("POST", s.getOidcUserInfoEndpoint()));
        }
    }

    /// What a server with no [PasswordEncoder] does about its clients'
    /// secrets. Clients that are all known now are checked now, and one with a
    /// secret stops the start: every request it made would be refused, and a
    /// log line per request is a poor way to learn that. Clients kept
    /// somewhere they can be added to later cannot be checked, so the start is
    /// told once what will happen to one that has a secret.
    ///
    /// No encoder is assumed in either case: one named here would be in every
    /// authorization server, whether or not a client of it has a secret.
    private static void requireNoSecrets(RegisteredClientRepository clients) {
        java.util.List<
                com.codename1.backend.security.oauth2.server.authorization.RegisteredClient> all =
                clients.findAll();
        if (all == null) {
            System.err.println("cn1: the authorization server has no PasswordEncoder, so a "
                    + "registered client that authenticates with a secret will be refused with "
                    + "invalid_client. Declare a PasswordEncoder bean, or call "
                    + "clientSecretEncoder(...) on the authorizationServer() configurer; a "
                    + "server whose clients are all public needs neither.");
            return;
        }
        for (com.codename1.backend.security.oauth2.server.authorization.RegisteredClient client
                : all) {
            if (client.getClientSecret() != null) {
                throw new IllegalStateException("The registered client \""
                        + client.getClientId() + "\" has a secret, and the authorization "
                        + "server has nothing to check a secret with. Declare a "
                        + "PasswordEncoder bean -- the one the secret was encoded with -- or "
                        + "call clientSecretEncoder(...) on the authorizationServer() "
                        + "configurer.");
            }
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthorizationServerSettings s = settings();
        String issuer = s.getIssuer();
        String audience = s.getDefaultAudience();
        int verificationAttempts;
        int verificationWindow;
        try {
            if (issuer == null) {
                String configured = http.getConfig().get(AuthorizationServerSettings.ISSUER);
                issuer = configured == null ? null
                        : AuthorizationServerSettings.validIssuer(configured);
            }
            if (issuer == null && !http.getConfig().isDevelopmentProfile()) {
                throw new IllegalStateException("The authorization server needs to be told its "
                        + "issuer: set " + AuthorizationServerSettings.ISSUER + " to the address "
                        + "clients reach this server at, such as https://id.example.com");
            }
            if (audience == null) {
                audience = http.getConfig().get(AuthorizationServerSettings.AUDIENCE);
                if (audience != null && !com.codename1.backend.security.oauth2.server
                        .authorization.RegisteredClient.isResource(audience)) {
                    throw new IllegalStateException(AuthorizationServerSettings.AUDIENCE
                            + " must be an absolute address with no fragment: " + audience);
                }
            }
            if (clients == null) {
                clients = http.getSharedObject(RegisteredClientRepository.class);
            }
            if (clients == null) {
                throw new IllegalStateException("authorizationServer() needs to be told its "
                        + "clients: declare a RegisteredClientRepository bean");
            }
            if (authorizations == null) {
                authorizations = http.getSharedObject(OAuth2AuthorizationService.class);
            }
            if (authorizations == null) {
                System.err.println("cn1: the authorization server keeps its grants in this "
                        + "process: refresh tokens will not survive a restart and are not shared "
                        + "between processes. Declare an OAuth2AuthorizationService bean -- a "
                        + "JdbcOAuth2AuthorizationService over the DataSource -- to keep them in "
                        + "the database.");
                authorizations = new InMemoryOAuth2AuthorizationService();
            }
            if (keys == null) {
                keys = http.getSharedObject(JwkSource.class);
            }
            if (keys == null) {
                keys = AuthorizationServerKeys.load(http.getConfig());
            }
            if (encoder == null) {
                encoder = http.getSharedObject(JwtEncoder.class);
            }
            if (encoder == null) {
                encoder = new DefaultJwtEncoder(keys);
            }
            if (secrets == null) {
                secrets = http.getSharedObject(PasswordEncoder.class);
            }
            if (secrets == null) {
                requireNoSecrets(clients);
            }
            boolean configured = http.getConfig().get(DEVICE_VERIFICATION_ATTEMPTS, null) != null
                    || http.getConfig().get(DEVICE_VERIFICATION_WINDOW, null) != null;
            verificationAttempts = http.getConfig().getInt(DEVICE_VERIFICATION_ATTEMPTS, 10);
            verificationWindow = http.getConfig().getInt(DEVICE_VERIFICATION_WINDOW, 300);
            if (verificationAttempts < 1 || verificationWindow < 1) {
                throw new IllegalStateException(DEVICE_VERIFICATION_ATTEMPTS + " and "
                        + DEVICE_VERIFICATION_WINDOW + " must each be at least 1");
            }
            String counted = "the limiter given to deviceVerificationRateLimiter(...)";
            if (verificationLimiter == null) {
                RateLimiter bean = http.getSharedObject(RateLimiter.class);
                RateLimiter derived = bean == null ? null : bean.derive("device-verification",
                        verificationAttempts, verificationWindow);
                if (derived != null) {
                    verificationLimiter = derived;
                    configured = false;
                } else {
                    verificationLimiter = bean;
                    counted = "the application's RateLimiter bean, which derives no limiter";
                }
            }
            if (verificationLimiter != null && configured) {
                throw new IllegalStateException(DEVICE_VERIFICATION_ATTEMPTS + " and "
                        + DEVICE_VERIFICATION_WINDOW + " would decide nothing: tries are "
                        + "counted by " + counted + ", by a limit of its own. Remove the "
                        + "settings, or give that limiter the numbers.");
            }
        } catch (IOException err) {
            throw new IllegalStateException("The authorization server could not be set up: "
                    + err.getMessage(), err);
        }
        OAuth2AuthorizationServer server = new OAuth2AuthorizationServer(s, issuer, audience,
                clients,
                authorizations, keys, encoder, secrets, customizer, userInfo, verificationLimiter,
                verificationAttempts, verificationWindow, clock);
        http.addFilter(new OAuth2AuthorizationServerFilter(server),
                HttpSecurity.ORDER_AUTHORIZATION_SERVER);
        http.addFilter(new OAuth2AuthorizationEndpointFilter(server),
                HttpSecurity.ORDER_AUTHORIZATION_SERVER_USER);
    }
}
