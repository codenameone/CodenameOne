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
package com.codename1.backend.security.oauth2.server.authorization;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Json;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.InsufficientAuthenticationException;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.backend.security.oauth2.jwt.JwtException;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The endpoints of an authorization server, each a method that takes a
/// request and answers it. The chain's filters decide which request goes to
/// which; see [com.codename1.backend.security.AuthorizationServerConfigurer],
/// which is also where every rule these endpoints apply is described.
public final class OAuth2AuthorizationServer {
    /// The letters of a user code: twenty consonants, so that a code spells no
    /// word and none of its letters is mistaken for another or for a digit.
    static final String USER_CODE_ALPHABET = "BCDFGHJKLMNPQRSTVWXZ";
    private static final int USER_CODE_LENGTH = 8;
    private static final long DEVICE_POLL_INTERVAL_SECONDS = 5;
    private static final long PURGE_INTERVAL_MILLIS = 60000;
    private static final long PURGE_GRACE_MILLIS = 600000;
    private static final long REUSE_GRACE_MILLIS = 10000;
    /// Session attribute holding the last completed credential check, in epoch seconds.
    public static final String AUTH_TIME = "cn1.security.oauth2.authTime";
    private static final String DEVICE_TICKET = "cn1.security.oauth2.deviceTicket";
    /// The attribute of a grant that holds the resource servers it is for.
    private static final String RESOURCE = "resource";

    private final AuthorizationServerSettings settings;
    private final String fixedIssuer;
    private final String defaultAudience;
    private final RegisteredClientRepository clients;
    private final OAuth2AuthorizationService authorizations;
    private final JwkSource keys;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final PasswordEncoder secrets;
    private final OAuth2TokenCustomizer customizer;
    private final OidcUserInfoMapper userInfo;
    private final RateLimiter verificationLimiter;
    private final Clock clock;
    private final Attempts attempts;
    private long purgedAt;

    /// @param fixedIssuer the issuer, or null to read it off each request: a
    /// development profile only
    /// @param defaultAudience the `aud` of an access token whose request named
    /// no `resource`; null for the issuer
    /// @param secrets what client secrets were encoded with; null when no
    /// client has one
    /// @param verificationLimiter what bounds wrong user codes; null to count
    /// in this process
    /// @param verificationAttempts how many user codes one user may try in a
    /// window, when this process counts
    /// @param verificationWindowSeconds the length of that window
    public OAuth2AuthorizationServer(AuthorizationServerSettings settings, String fixedIssuer,
            String defaultAudience, RegisteredClientRepository clients, OAuth2AuthorizationService authorizations,
            JwkSource keys, JwtEncoder encoder, PasswordEncoder secrets,
            OAuth2TokenCustomizer customizer, OidcUserInfoMapper userInfo,
            RateLimiter verificationLimiter, int verificationAttempts,
            long verificationWindowSeconds, Clock clock) {
        if (settings == null || clients == null || authorizations == null || keys == null
                || encoder == null) {
            throw new IllegalArgumentException("The settings, the clients, the authorizations, "
                    + "the keys and an encoder are required");
        }
        this.settings = settings;
        this.fixedIssuer = fixedIssuer;
        this.defaultAudience = defaultAudience != null ? defaultAudience
                : settings.getDefaultAudience();
        this.clients = clients;
        this.authorizations = authorizations;
        this.keys = keys;
        this.encoder = encoder;
        this.secrets = secrets;
        this.customizer = customizer;
        this.userInfo = userInfo;
        this.verificationLimiter = verificationLimiter;
        this.attempts = new Attempts(verificationAttempts, verificationWindowSeconds * 1000L);
        this.clock = clock == null ? Clock.SYSTEM : clock;
        // Tokens of this server's own, read back at the user info and
        // revocation endpoints: under the two algorithms it signs with, and
        // only with a key of the algorithm's kind.
        this.decoder = DefaultJwtDecoder.withJwkSource(keys).jwsAlgorithms(
                new JwsAlgorithm[] {SignatureAlgorithm.RS256, SignatureAlgorithm.ES256}).build();
    }

    public AuthorizationServerSettings getSettings() {
        return settings;
    }

    // ------------------------------------------------------------ dispatching

    /// Answers a request to one of the endpoints that need no signed-in user:
    /// the token, revocation, device authorization, JWK Set, user info and
    /// metadata endpoints.
    ///
    /// @param path the request's path
    /// @return the answer, or null when `path` is none of them
    public HttpServer.Response handle(HttpServer.Request request, String path) {
        try {
            if (path.equals(settings.getTokenEndpoint())) {
                return token(request);
            }
            if (path.equals(settings.getJwkSetEndpoint())) {
                return jwks(request);
            }
            if (path.equals(settings.getTokenRevocationEndpoint())) {
                return revoke(request);
            }
            if (path.equals(settings.getDeviceAuthorizationEndpoint())) {
                return deviceAuthorization(request);
            }
            if (path.equals(settings.getOidcUserInfoEndpoint())) {
                return userInfo(request);
            }
            if ("/.well-known/openid-configuration".equals(path)
                    || "/.well-known/oauth-authorization-server".equals(path)) {
                return metadata(request);
            }
            return null;
        } catch (Refusal refusal) {
            return refusal.response();
        } catch (IllegalStateException failed) {
            // The store, or the keys: nothing the client can do anything about,
            // and nothing it should read.
            System.err.println("cn1: the authorization server could not answer " + path + ": "
                    + failed.getMessage());
            return error(500, OAuth2ErrorCodes.SERVER_ERROR, "The server could not answer", null);
        }
    }

    /// Whether `path` is an endpoint a user must be signed in for: the
    /// authorization endpoint or the device verification page.
    public boolean isUserEndpoint(String path) {
        return path.equals(settings.getAuthorizationEndpoint())
                || path.equals(settings.getDeviceVerificationEndpoint());
    }

    /// The request's issuer: the configured one, or on a development profile
    /// without one, the address the request was made to.
    String issuer(HttpServer.Request request) {
        if (fixedIssuer != null) {
            return fixedIssuer;
        }
        String host = request.getHeader("Host");
        if (host == null || host.length() == 0 || host.length() > 255) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "The request names no host");
        }
        for (int iter = 0 ; iter < host.length() ; iter++) {
            char c = host.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == ':' || c == '[' || c == ']')) {
                throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST,
                        "The request names no host");
            }
        }
        return (request.isSecure() ? "https://" : "http://") + host;
    }

    // --------------------------------------------------------------- metadata

    private HttpServer.Response metadata(HttpServer.Request request) {
        requireMethod(request, "GET");
        String issuer = issuer(request);
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("issuer", issuer);
        m.put("authorization_endpoint", issuer + settings.getAuthorizationEndpoint());
        m.put("token_endpoint", issuer + settings.getTokenEndpoint());
        m.put("jwks_uri", issuer + settings.getJwkSetEndpoint());
        m.put("userinfo_endpoint", issuer + settings.getOidcUserInfoEndpoint());
        m.put("revocation_endpoint", issuer + settings.getTokenRevocationEndpoint());
        m.put("device_authorization_endpoint", issuer + settings.getDeviceAuthorizationEndpoint());
        m.put("response_types_supported", list("code"));
        m.put("response_modes_supported", list("query"));
        m.put("grant_types_supported", list("authorization_code", "refresh_token",
                "client_credentials", AuthorizationGrantType.DEVICE_CODE.getValue()));
        m.put("subject_types_supported", list("public"));
        List<Object> algorithms = new ArrayList<Object>();
        for (Jwk key : signingKeys()) {
            String algorithm = AuthorizationServerKeys.algorithm(key);
            if (algorithm != null && !algorithms.contains(algorithm)) {
                algorithms.add(algorithm);
            }
        }
        m.put("id_token_signing_alg_values_supported", algorithms);
        m.put("scopes_supported", list("openid", "profile", "email"));
        m.put("token_endpoint_auth_methods_supported", list("client_secret_basic",
                "client_secret_post", "none"));
        m.put("revocation_endpoint_auth_methods_supported", list("client_secret_basic",
                "client_secret_post", "none"));
        m.put("code_challenge_methods_supported", list("S256"));
        m.put("claims_supported", list("sub", "iss", "aud", "exp", "iat", "auth_time", "nonce",
                "at_hash", "preferred_username", "name", "email", "email_verified"));
        m.put("authorization_response_iss_parameter_supported", Boolean.TRUE);
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        return new HttpServer.Response(200, "application/json", OAuth2Parameters.utf8(
                Json.write(m)), headers);
    }

    private static List<Object> list(String... values) {
        List<Object> out = new ArrayList<Object>();
        for (String value : values) {
            out.add(value);
        }
        return out;
    }

    private List<Jwk> signingKeys() {
        try {
            return keys.getKeys();
        } catch (IOException err) {
            throw new IllegalStateException("The signing keys could not be read: "
                    + err.getMessage(), err);
        }
    }

    private HttpServer.Response jwks(HttpServer.Request request) {
        requireMethod(request, "GET");
        List<Jwk> all = signingKeys();
        return new HttpServer.Response(200, "application/json", OAuth2Parameters.utf8(
                JwkSet.of(all.toArray(new Jwk[all.size()])).toJson()));
    }

    // ---------------------------------------------------- the client, proven

    /// Who a request to the token, revocation or device endpoint is from.
    private RegisteredClient authenticateClient(HttpServer.Request request) {
        String header = request.getHeader("Authorization");
        String id;
        String secret = null;
        ClientAuthenticationMethod method;
        boolean basic = header != null && header.regionMatches(true, 0, "Basic ", 0, 6);
        if (basic) {
            byte[] decoded = Base64.decode(header.substring(6).trim());
            String pair = decoded == null ? "" : OAuth2Parameters.string(decoded);
            int colon = pair.indexOf(':');
            id = colon < 0 ? null : OAuth2Parameters.decode(pair.substring(0, colon));
            secret = colon < 0 ? null : OAuth2Parameters.decode(pair.substring(colon + 1));
            if (id == null || secret == null || id.length() == 0) {
                throw invalidClient(true);
            }
            method = ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        } else {
            id = param(request, "client_id");
            secret = param(request, "client_secret");
            if (id == null || id.length() == 0) {
                throw invalidClient(false);
            }
            method = secret == null ? ClientAuthenticationMethod.NONE
                    : ClientAuthenticationMethod.CLIENT_SECRET_POST;
        }
        RegisteredClient client = clients.findByClientId(id);
        if (client == null || !client.getClientAuthenticationMethods().contains(method)) {
            throw invalidClient(basic);
        }
        if (!ClientAuthenticationMethod.NONE.equals(method)) {
            if (secrets == null) {
                // Nothing to check a secret with, which the start-up said. A
                // secret that cannot be checked is one that was not proven.
                throw invalidClient(basic);
            }
            if (client.getClientSecret() == null
                    || !secrets.matches(secret, client.getClientSecret())) {
                throw invalidClient(basic);
            }
        }
        return client;
    }

    private static Refusal invalidClient(boolean basic) {
        Refusal refusal = new Refusal(401, OAuth2ErrorCodes.INVALID_CLIENT,
                "Client authentication failed");
        if (basic) {
            refusal.header("WWW-Authenticate", "Basic realm=\"oauth2\"");
        }
        return refusal;
    }

    // ------------------------------------------------- the authorization endpoint

    /// Answers the authorization endpoint for a browser whose user is
    /// `authentication`.
    ///
    /// The client and the redirect address are checked before anything else,
    /// and a request that fails either is answered here, with a 400: an error
    /// is sent to a redirect address only once that address is known to be the
    /// client's own.
    ///
    /// - `InsufficientAuthenticationException`: when nobody is signed in and
    /// the request is a browser's, so that the chain sends it to sign in and
    /// back
    ///
    /// @param authentication who is signed in; null for nobody
    public HttpServer.Response authorize(HttpServer.Request request,
                                         Authentication authentication) {
        try {
            return authorizeChecked(request, authentication);
        } catch (Refusal refusal) {
            return refusal.response();
        } catch (IllegalStateException failed) {
            System.err.println("cn1: the authorization endpoint could not answer: "
                    + failed.getMessage());
            return error(500, OAuth2ErrorCodes.SERVER_ERROR, "The server could not answer", null);
        }
    }

    private HttpServer.Response authorizeChecked(HttpServer.Request request,
                                                 Authentication authentication) {
        requireMethod(request, "GET");
        String clientId = request.queryParam("client_id");
        RegisteredClient client = clientId == null ? null : clients.findByClientId(clientId);
        if (client == null) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "Unknown client_id");
        }
        String asked = request.queryParam("redirect_uri");
        String redirectUri = asked;
        if (redirectUri == null && client.getRedirectUris().size() == 1) {
            redirectUri = client.getRedirectUris().iterator().next();
        }
        if (redirectUri == null || !registered(client, redirectUri)) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST,
                    "The redirect_uri is not one the client registered");
        }
        // From here on the address is the client's: errors go back to it.
        String state = request.queryParam("state");
        String issuer = issuer(request);
        if (!client.getAuthorizationGrantTypes().contains(
                AuthorizationGrantType.AUTHORIZATION_CODE)) {
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                    "The client may not use the authorization code grant");
        }
        if (!"code".equals(request.queryParam("response_type"))) {
            return redirectError(redirectUri, state, issuer,
                    OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE, "response_type must be code");
        }
        Set<String> scopes = OAuth2Parameters.scopes(request.queryParam("scope"));
        if (!client.getScopes().containsAll(scopes)) {
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_SCOPE,
                    "A scope asked for is not one the client was registered with");
        }
        List<String> resources;
        try {
            resources = resources(client, OAuth2Parameters.values(query(request), "resource"));
        } catch (Refusal target) {
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_TARGET,
                    target.description);
        }
        String challenge = request.queryParam("code_challenge");
        String challengeMethod = request.queryParam("code_challenge_method");
        if (challenge == null) {
            if (client.isPublic() || client.getClientSettings().isRequireProofKey()) {
                return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_REQUEST,
                        "This client must send a code_challenge (PKCE)");
            }
        } else if (!"S256".equals(challengeMethod) || !isChallenge(challenge)) {
            // "plain" is refused, and so is leaving the method out, which
            // RFC 7636 reads as plain.
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_REQUEST,
                    "code_challenge_method must be S256, with a 43 character challenge");
        }
        Set<String> prompts = OAuth2Parameters.scopes(request.queryParam("prompt"));
        if (prompts.contains("none") && prompts.size() > 1) {
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_REQUEST,
                    "prompt=none cannot be combined with another prompt");
        }
        long maxAge = -1;
        String maxAgeParameter = request.queryParam("max_age");
        if (maxAgeParameter != null) {
            try {
                maxAge = Long.parseLong(maxAgeParameter);
                if (maxAge < 0) {
                    throw new NumberFormatException("negative max_age");
                }
            } catch (NumberFormatException invalid) {
                return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.INVALID_REQUEST,
                        "max_age must be a nonnegative integer");
            }
        }
        long now = clock.currentTimeMillis();
        HttpSession session = request.getSession(false);
        Object authenticatedAt = session == null ? null : session.getAttribute(AUTH_TIME);
        boolean tooOld = maxAge >= 0 && (maxAge == 0 || !(authenticatedAt instanceof Number)
                || now / 1000L - ((Number) authenticatedAt).longValue() > maxAge);
        if (prompts.contains("login") || tooOld) {
            // The endpoint cannot silently reuse the current authentication, nor
            // redirect the same prompt=login request in an endless login loop.
            // Tell the RP that it must arrange a fresh credential check.
            return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.LOGIN_REQUIRED,
                    "Fresh authentication is required");
        }
        if (authentication == null) {
            if (prompts.contains("none")) {
                return redirectError(redirectUri, state, issuer, OAuth2ErrorCodes.LOGIN_REQUIRED,
                        "Nobody is signed in");
            }
            String accept = request.getHeader("Accept");
            if (accept == null || accept.indexOf("text/html") < 0) {
                // A program, which cannot use a login page: told so directly.
                throw new Refusal(401, OAuth2ErrorCodes.LOGIN_REQUIRED, "Nobody is signed in");
            }
            throw new InsufficientAuthenticationException(
                    "Full authentication is required to authorize a client");
        }
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("redirect_uri", redirectUri);
        attributes.put("redirect_uri_sent", Boolean.valueOf(asked != null));
        if (challenge != null) {
            attributes.put("code_challenge", challenge);
        }
        String nonce = request.queryParam("nonce");
        if (nonce != null) {
            attributes.put("nonce", nonce);
        }
        attributes.put("auth_time", Long.valueOf(authTime(request, now)));
        attributes.put("authorities", authorities(authentication));
        if (!resources.isEmpty()) {
            attributes.put(RESOURCE, new ArrayList<Object>(resources));
        }
        TokenSettings tokens = client.getTokenSettings();
        long codeExpires = now + tokens.getAuthorizationCodeTimeToLive() * 1000L;
        OAuth2Authorization authorization = new OAuth2Authorization(OAuth2Parameters.random(16),
                client.getId(), authentication.getName(),
                AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), scopes,
                OAuth2Authorization.ACTIVE, attributes, now, codeExpires);
        authorizations.save(authorization);
        String code = OAuth2Parameters.random(32);
        authorizations.addToken(authorization.getId(), OAuth2AuthorizationService.CODE,
                OAuth2Parameters.sha256(code), codeExpires);
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        answer.put("code", code);
        answer.put("state", state);
        answer.put("iss", issuer);
        return redirect(OAuth2Parameters.append(redirectUri, answer));
    }

    /// When this session completed a credential check, in epoch seconds.
    /// Authentication supplied without a session has no stored sign-in time.
    private static long authTime(HttpServer.Request request, long now) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return now / 1000L;
        }
        Object known = session.getAttribute(AUTH_TIME);
        if (known instanceof Number) {
            return ((Number) known).longValue();
        }
        return now / 1000L;
    }

    private static List<Object> authorities(Authentication authentication) {
        List<Object> out = new ArrayList<Object>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            out.add(authority.getAuthority());
        }
        return out;
    }

    /// Whether `uri` is one of the client's redirect addresses: the same text,
    /// or -- RFC 8252 -- the same `http` loopback address on another port.
    static boolean registered(RegisteredClient client, String uri) {
        if (uri.indexOf('#') >= 0) {
            return false;
        }
        for (String known : client.getRedirectUris()) {
            if (known.equals(uri)) {
                return true;
            }
        }
        String portless = loopbackWithoutPort(uri);
        if (portless != null) {
            for (String known : client.getRedirectUris()) {
                if (portless.equals(loopbackWithoutPort(known))) {
                    return true;
                }
            }
        }
        return false;
    }

    /// An `http://127.0.0.1...` or `http://[::1]...` address with its port
    /// taken out; null for any other address.
    private static String loopbackWithoutPort(String uri) {
        String[] hosts = {"http://127.0.0.1", "http://[::1]"};
        for (String host : hosts) {
            if (uri.startsWith(host)) {
                int at = host.length();
                if (at == uri.length()) {
                    return uri;
                }
                char next = uri.charAt(at);
                if (next == '/' || next == '?') {
                    return uri;
                }
                if (next != ':') {
                    return null;
                }
                int end = at + 1;
                while (end < uri.length() && uri.charAt(end) >= '0' && uri.charAt(end) <= '9') {
                    end++;
                }
                if (end < uri.length() && uri.charAt(end) != '/' && uri.charAt(end) != '?') {
                    return null;
                }
                return host + uri.substring(end);
            }
        }
        return null;
    }

    private static boolean isChallenge(String challenge) {
        if (challenge.length() < 43 || challenge.length() > 128) {
            return false;
        }
        for (int iter = 0 ; iter < challenge.length() ; iter++) {
            char c = challenge.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~')) {
                return false;
            }
        }
        return true;
    }

    private static HttpServer.Response redirectError(String redirectUri, String state,
            String issuer, String code, String description) {
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        answer.put("error", code);
        answer.put("error_description", description);
        answer.put("state", state);
        answer.put("iss", issuer);
        return redirect(OAuth2Parameters.append(redirectUri, answer));
    }

    private static HttpServer.Response redirect(String location) {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("Location", location);
        headers.put("Cache-Control", "no-store");
        headers.put("Referrer-Policy", "no-referrer");
        return new HttpServer.Response(302, "text/plain; charset=utf-8", new byte[0], headers);
    }

    // --------------------------------------------------------- the token endpoint

    private HttpServer.Response token(HttpServer.Request request) {
        requireMethod(request, "POST");
        RegisteredClient client = authenticateClient(request);
        String grantType = param(request, "grant_type");
        if (grantType == null) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "grant_type is required");
        }
        long now = clock.currentTimeMillis();
        purge(now);
        Map<String, Object> answer;
        if ("authorization_code".equals(grantType)) {
            answer = redeemCode(request, client, now);
        } else if ("refresh_token".equals(grantType)) {
            answer = refresh(request, client, now);
        } else if ("client_credentials".equals(grantType)) {
            answer = clientCredentials(request, client, now);
        } else if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(grantType)) {
            answer = redeemDeviceCode(request, client, now);
        } else {
            throw new Refusal(400, OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE,
                    "This server does not issue tokens for that grant_type");
        }
        return json(200, answer);
    }

    private static void requireGrant(RegisteredClient client, AuthorizationGrantType grant) {
        if (!client.getAuthorizationGrantTypes().contains(grant)) {
            throw new Refusal(400, OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                    "The client may not use this grant");
        }
    }

    /// Whether a secret that was used up is being presented long enough after
    /// its use to be a replay, rather than the same request arriving twice.
    ///
    /// A client that retries, or refreshes from two threads at once, presents
    /// one secret twice within moments, and so do two processes handed the
    /// same request: one is answered, the other refused, and that is all.
    /// Ending the grant for it would also race the request that won, whose
    /// answer is still being written. Ten seconds on, a second presentation
    /// is somebody else's.
    private static boolean replayed(OAuth2AuthorizationService.StoredToken stored, long now) {
        return now - stored.getPolledAt() >= REUSE_GRACE_MILLIS;
    }

    private static Refusal invalidGrant() {
        // One answer for every way a grant can be wrong: which way it was is
        // not something whoever holds a bad one should learn.
        return new Refusal(400, OAuth2ErrorCodes.INVALID_GRANT,
                "The grant is invalid, expired, revoked or was issued to another client");
    }

    private Map<String, Object> redeemCode(HttpServer.Request request, RegisteredClient client,
                                           long now) {
        requireGrant(client, AuthorizationGrantType.AUTHORIZATION_CODE);
        String code = param(request, "code");
        if (code == null || code.length() == 0) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "code is required");
        }
        String hash = OAuth2Parameters.sha256(code);
        OAuth2AuthorizationService.StoredToken stored = authorizations.findToken(
                OAuth2AuthorizationService.CODE, hash);
        if (stored == null) {
            throw invalidGrant();
        }
        if (stored.isUsed()) {
            // A code presented again was seen by two parties, and one of them
            // is not the client: everything issued from it goes.
            if (replayed(stored, now)) {
                authorizations.remove(stored.getAuthorizationId());
            }
            throw invalidGrant();
        }
        OAuth2Authorization authorization = stored.getExpiresAt() <= now ? null
                : authorizations.findById(stored.getAuthorizationId());
        if (authorization == null) {
            throw invalidGrant();
        }
        String sentRedirect = param(request, "redirect_uri");
        Object challenge = authorization.getAttribute("code_challenge");
        String verifier = param(request, "code_verifier");
        boolean good = authorization.getRegisteredClientId().equals(client.getId());
        // The address must be repeated exactly when the request named one.
        if (Boolean.TRUE.equals(authorization.getAttribute("redirect_uri_sent"))
                || sentRedirect != null) {
            good &= sentRedirect != null
                    && sentRedirect.equals(authorization.getAttribute("redirect_uri"));
        }
        if (challenge instanceof String) {
            good &= verifier != null && verifier.length() >= 43 && verifier.length() <= 128
                    && OAuth2Parameters.equalsConstantTime((String) challenge,
                            OAuth2Parameters.sha256(verifier));
        } else {
            // A verifier for a request that had no challenge: the challenge
            // was stripped on the way, which is the attack PKCE exists for.
            good &= verifier == null;
        }
        if (!good) {
            // A wrong guess spends the code, and the grant it stood for goes
            // with it: there is no second try at a verifier.
            authorizations.consumeToken(OAuth2AuthorizationService.CODE, hash, now);
            authorizations.remove(authorization.getId());
            throw invalidGrant();
        }
        // Everything above was reading. This one statement is what decides
        // between two requests that present the same code at the same moment,
        // in this process or in two: exactly one of them changes the row. The
        // other is refused and takes nothing away from the one that won --
        // revoking here would race the winner's own writes, and a code
        // presented after this moment is caught above, by its mark.
        if (!authorizations.consumeToken(OAuth2AuthorizationService.CODE, hash, now)) {
            throw invalidGrant();
        }
        return issue(request, client, authorization, authorization.getPrincipalName(),
                "authorization_code", authorization.getScopes(), now, null);
    }

    private Map<String, Object> refresh(HttpServer.Request request, RegisteredClient client,
                                        long now) {
        requireGrant(client, AuthorizationGrantType.REFRESH_TOKEN);
        String presented = param(request, "refresh_token");
        if (presented == null || presented.length() == 0) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "refresh_token is required");
        }
        String hash = OAuth2Parameters.sha256(presented);
        boolean reuse = client.getTokenSettings().isReuseRefreshTokens();
        OAuth2AuthorizationService.StoredToken stored = authorizations.findToken(
                OAuth2AuthorizationService.REFRESH_TOKEN, hash);
        if (stored == null) {
            throw invalidGrant();
        }
        OAuth2Authorization authorization = authorizations.findById(stored.getAuthorizationId());
        if (authorization == null || !authorization.getRegisteredClientId().equals(client.getId())
                || !OAuth2Authorization.ACTIVE.equals(authorization.getStatus())) {
            throw invalidGrant();
        }
        if (stored.isUsed()) {
            // A token that was already replaced: whoever presents it is not
            // the holder of the one that replaced it. Which of the two is the
            // thief cannot be told, so neither keeps anything.
            if (replayed(stored, now)) {
                authorizations.remove(authorization.getId());
            }
            throw invalidGrant();
        }
        if (stored.getExpiresAt() <= now) {
            throw invalidGrant();
        }
        Set<String> scopes = authorization.getScopes();
        String asked = param(request, "scope");
        if (asked != null) {
            Set<String> narrowed = OAuth2Parameters.scopes(asked);
            if (!scopes.containsAll(narrowed)) {
                throw new Refusal(400, OAuth2ErrorCodes.INVALID_SCOPE,
                        "A scope asked for is not one the grant has");
            }
            scopes = narrowed;
        }
        if (!reuse && !authorizations.consumeToken(OAuth2AuthorizationService.REFRESH_TOKEN,
                hash, now)) {
            // Another request used it between the read and here: of two that
            // refresh at the same moment one is answered, and the other is
            // refused without ending the grant under the one that won. The
            // token presented again after this is caught above, by its mark.
            throw invalidGrant();
        }
        return issue(request, client, authorization, authorization.getPrincipalName(),
                "refresh_token", scopes, now, reuse ? presented : null);
    }

    private Map<String, Object> clientCredentials(HttpServer.Request request,
                                                  RegisteredClient client, long now) {
        requireGrant(client, AuthorizationGrantType.CLIENT_CREDENTIALS);
        if (client.getClientSecret() == null) {
            throw invalidClient(false);
        }
        Set<String> scopes = OAuth2Parameters.scopes(param(request, "scope"));
        if (!client.getScopes().containsAll(scopes)) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_SCOPE,
                    "A scope asked for is not one the client was registered with");
        }
        scopes.remove("openid");
        return issue(request, client, null, client.getClientId(), "client_credentials", scopes,
                now, null, resources(client, OAuth2Parameters.values(form(request), RESOURCE)));
    }

    /// Signs what a grant yields and answers with it.
    ///
    /// @param authorization the grant, or null for `client_credentials`
    /// @param keptRefreshToken the refresh token to hand back unchanged, or
    /// null to issue a new one where the client may have one
    private Map<String, Object> issue(HttpServer.Request request, RegisteredClient client,
            OAuth2Authorization authorization, String principal, String grantType,
            Set<String> scopes, long now, String keptRefreshToken) {
        return issue(request, client, authorization, principal, grantType, scopes, now,
                keptRefreshToken, granted(request, client, authorization));
    }

    /// The resource servers a token issued under `authorization` is for: what
    /// this request to the token endpoint names, which must be among those
    /// the grant was made for when it was made for any (RFC 8707 section
    /// 2.2); otherwise the grant's own. Empty for the default audience.
    private List<String> granted(HttpServer.Request request, RegisteredClient client,
                                 OAuth2Authorization authorization) {
        List<String> of = new ArrayList<String>();
        Object kept = authorization.getAttribute(RESOURCE);
        if (kept instanceof List) {
            for (Object one : (List) kept) {
                if (one instanceof String) {
                    of.add((String) one);
                }
            }
        }
        List<String> asked = resources(client, OAuth2Parameters.values(form(request), RESOURCE));
        if (asked.isEmpty()) {
            return of;
        }
        if (!of.isEmpty() && !of.containsAll(asked)) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_TARGET,
                    "A resource asked for is not one the grant was made for");
        }
        return asked;
    }

    /// `asked`, checked: each an absolute address with no fragment, and one
    /// the client was registered with when it was registered with any.
    private static List<String> resources(RegisteredClient client, List<String> asked) {
        List<String> out = new ArrayList<String>();
        for (String resource : asked) {
            if (!RegisteredClient.isResource(resource)) {
                throw new Refusal(400, OAuth2ErrorCodes.INVALID_TARGET,
                        "A resource is an absolute address with no fragment");
            }
            if (!client.getResources().isEmpty() && !client.getResources().contains(resource)) {
                throw new Refusal(400, OAuth2ErrorCodes.INVALID_TARGET,
                        "A resource asked for is not one the client was registered with");
            }
            if (!out.contains(resource)) {
                out.add(resource);
            }
        }
        return out;
    }

    /// The request's query, without the `?`; empty when it has none.
    private static String query(HttpServer.Request request) {
        String target = request.getTarget();
        int mark = target == null ? -1 : target.indexOf('?');
        return mark < 0 ? "" : target.substring(mark + 1);
    }

    /// The request's form body; empty when it sent another kind.
    private static String form(HttpServer.Request request) {
        String type = request.getHeader("Content-Type");
        if (type == null || !type.trim().regionMatches(true, 0,
                "application/x-www-form-urlencoded", 0, 33)) {
            return "";
        }
        String body = request.getBody();
        return body == null ? "" : body;
    }

    /// @param resources the resource servers the access token is for; empty
    /// for the default audience
    private Map<String, Object> issue(HttpServer.Request request, RegisteredClient client,
            OAuth2Authorization authorization, String principal, String grantType,
            Set<String> scopes, long now, String keptRefreshToken, List<String> resources) {
        String issuer = issuer(request);
        TokenSettings settings = client.getTokenSettings();
        long nowSeconds = now / 1000L;
        Jwk signing = null;
        for (Jwk key : signingKeys()) {
            if (key.isPrivate() && AuthorizationServerKeys.algorithm(key) != null) {
                signing = key;
                break;
            }
        }
        if (signing == null) {
            throw new IllegalStateException("There is no private key to sign with");
        }
        SignatureAlgorithm algorithm = SignatureAlgorithm.from(
                AuthorizationServerKeys.algorithm(signing));
        // The id of the grant leads the token's own, which is how the user
        // info and revocation endpoints find the grant a token was issued under.
        String jti = (authorization == null ? "" : authorization.getId() + ".")
                + OAuth2Parameters.random(12);
        // Who the token is for is the resource server (RFC 9068 section 3):
        // what the request named, or this server's default. Who it was issued
        // to is the client_id claim. An ID token, below, is the other way
        // round -- it is for the client.
        List<String> audience = new ArrayList<String>(resources);
        if (audience.isEmpty()) {
            audience.add(defaultAudience != null ? defaultAudience : issuer);
        }
        JwtClaimsSet.Builder access = JwtClaimsSet.builder().issuer(issuer).subject(principal)
                .audience(audience).issuedAt(nowSeconds).notBefore(nowSeconds)
                .expiresAt(nowSeconds + settings.getAccessTokenTimeToLive()).id(jti)
                .claim("client_id", client.getClientId());
        if (!scopes.isEmpty()) {
            access.claim("scope", OAuth2Parameters.scopes(scopes));
        }
        if (customizer != null) {
            customizer.customize(new OAuth2TokenContext(OAuth2TokenContext.ACCESS_TOKEN, client,
                    authorization, principal, grantType, scopes, access));
        }
        String accessToken = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(algorithm).keyId(signing.getKeyId()).type("at+jwt").build(),
                access.build())).getTokenValue();
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        answer.put("access_token", accessToken);
        answer.put("token_type", "Bearer");
        answer.put("expires_in", Long.valueOf(settings.getAccessTokenTimeToLive()));
        if (!scopes.isEmpty()) {
            answer.put("scope", OAuth2Parameters.scopes(scopes));
        }
        if (authorization == null) {
            return answer;
        }
        if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            long refreshExpires = now + settings.getRefreshTokenTimeToLive() * 1000L;
            String refreshToken = keptRefreshToken;
            if (refreshToken == null) {
                refreshToken = OAuth2Parameters.random(32);
                authorizations.addToken(authorization.getId(),
                        OAuth2AuthorizationService.REFRESH_TOKEN,
                        OAuth2Parameters.sha256(refreshToken), refreshExpires);
            } else if (!authorizations.extendToken(OAuth2AuthorizationService.REFRESH_TOKEN,
                    OAuth2Parameters.sha256(refreshToken), now, refreshExpires)) {
                throw invalidGrant();
            }
            // Both rotated and reused tokens extend the lifetime of their grant.
            authorizations.save(authorization.withExpiresAt(Math.max(authorization.getExpiresAt(), refreshExpires)));
            answer.put("refresh_token", refreshToken);
        } else {
            authorizations.save(authorization.withExpiresAt(
                    now + settings.getAccessTokenTimeToLive() * 1000L));
        }
        if (scopes.contains("openid")) {
            JwtClaimsSet.Builder id = JwtClaimsSet.builder();
            for (Map.Entry<String, Object> claim : userClaims(principal, scopes).entrySet()) {
                id.claim(claim.getKey(), claim.getValue());
            }
            id.issuer(issuer).subject(principal).audience(client.getClientId())
                    .issuedAt(nowSeconds).expiresAt(nowSeconds + settings.getIdTokenTimeToLive())
                    .claim("azp", client.getClientId())
                    .claim("at_hash", leftHalf(accessToken));
            Object authTime = authorization.getAttribute("auth_time");
            if (authTime instanceof Number) {
                id.claim("auth_time", Long.valueOf(((Number) authTime).longValue()));
            }
            Object nonce = authorization.getAttribute("nonce");
            if (nonce instanceof String) {
                id.claim("nonce", nonce);
            }
            if (customizer != null) {
                customizer.customize(new OAuth2TokenContext(OAuth2TokenContext.ID_TOKEN, client,
                        authorization, principal, grantType, scopes, id));
            }
            answer.put("id_token", encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(algorithm).keyId(signing.getKeyId()).type("JWT").build(),
                    id.build())).getTokenValue());
        }
        return answer;
    }

    /// The left half of the SHA-256 of a token, as OpenID Connect's `at_hash`
    /// has it. Both algorithms this server signs with hash with SHA-256.
    static String leftHalf(String token) {
        byte[] digest = Crypto.sha256(OAuth2Parameters.utf8(token));
        byte[] half = new byte[digest.length / 2];
        System.arraycopy(digest, 0, half, 0, half.length);
        return Base64Url.encode(half);
    }

    private Map<String, Object> userClaims(String principal, Set<String> scopes) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (userInfo != null) {
            Map<String, Object> mapped = userInfo.getClaims(principal, scopes);
            if (mapped != null) {
                out.putAll(mapped);
            }
        } else if (scopes.contains("profile")) {
            out.put("preferred_username", principal);
        }
        out.put("sub", principal);
        return out;
    }

    private synchronized void purge(long now) {
        if (now - purgedAt < PURGE_INTERVAL_MILLIS) {
            return;
        }
        purgedAt = now;
        try {
            // Ten minutes after they expired, not at once: a device that polls
            // just too late is owed "expired_token", and a code presented twice
            // "this was used", and neither can be said of a row that is gone.
            authorizations.purgeExpired(now - PURGE_GRACE_MILLIS, 200);
        } catch (RuntimeException failed) {
            System.err.println("cn1: expired authorizations could not be purged: "
                    + failed.getMessage());
        }
    }

    // ---------------------------------------------------------- revocation

    private HttpServer.Response revoke(HttpServer.Request request) {
        requireMethod(request, "POST");
        RegisteredClient client = authenticateClient(request);
        String token = param(request, "token");
        if (token == null || token.length() == 0) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "token is required");
        }
        String authorizationId = null;
        OAuth2AuthorizationService.StoredToken stored = authorizations.findToken(
                OAuth2AuthorizationService.REFRESH_TOKEN, OAuth2Parameters.sha256(token));
        if (stored != null) {
            authorizationId = stored.getAuthorizationId();
        } else if (token.indexOf('.') > 0) {
            try {
                authorizationId = grantOf(decoder.decode(token));
            } catch (JwtException notOurs) {
                authorizationId = null;
            }
        }
        OAuth2Authorization authorization = authorizationId == null ? null
                : authorizations.findById(authorizationId);
        // Only the client a token was issued to may revoke it; and whatever
        // the token was, the answer is the same (RFC 7009).
        if (authorization != null && authorization.getRegisteredClientId().equals(client.getId())) {
            authorizations.remove(authorization.getId());
        }
        return json(200, new LinkedHashMap<String, Object>());
    }

    /// The id of the grant an access token was issued under, or null.
    private static String grantOf(Jwt token) {
        String jti = token.getId();
        int dot = jti == null ? -1 : jti.indexOf('.');
        return dot <= 0 ? null : jti.substring(0, dot);
    }

    // ----------------------------------------------------------- user info

    private HttpServer.Response userInfo(HttpServer.Request request) {
        if (!"GET".equals(request.getMethod()) && !"POST".equals(request.getMethod())) {
            throw new Refusal(405, OAuth2ErrorCodes.INVALID_REQUEST, "GET or POST");
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new Refusal(401, null, null).header("WWW-Authenticate", "Bearer");
        }
        Jwt token;
        try {
            token = decoder.decode(header.substring(7).trim());
        } catch (JwtException bad) {
            Refusal refusal = bearer(401, OAuth2ErrorCodes.INVALID_TOKEN,
                    "The access token is not valid");
            // Why, for the server's log; the answer says only that it is not.
            refusal.initCause(bad);
            throw refusal;
        }
        String grant = grantOf(token);
        OAuth2Authorization authorization = grant == null ? null : authorizations.findById(grant);
        if (!issuer(request).equals(token.getIssuer()) || authorization == null
                || !OAuth2Authorization.ACTIVE.equals(authorization.getStatus())
                || !authorization.getPrincipalName().equals(token.getSubject())) {
            // Another issuer's token, or one whose grant was revoked.
            throw bearer(401, OAuth2ErrorCodes.INVALID_TOKEN, "The access token is not valid");
        }
        if (!forThisServer(token, issuer(request))) {
            throw bearer(401, OAuth2ErrorCodes.INVALID_TOKEN, "The access token is not valid");
        }
        Set<String> scopes = OAuth2Parameters.scopes(token.getClaimAsString("scope"));
        if (!scopes.contains("openid")) {
            throw bearer(403, OAuth2ErrorCodes.INSUFFICIENT_SCOPE,
                    "The access token was not granted openid");
        }
        return json(200, userClaims(token.getSubject(), scopes));
    }

    /// Whether an access token names this server among those it is for: the issuer, the
    /// default audience -- what a token gets when its request named no `resource` -- or
    /// the address of the user info endpoint, which a client that wants one token for a
    /// resource server and for this endpoint names as a `resource` beside the other.
    ///
    /// A token whose request named resource servers is for those and nobody else; that is
    /// the point of naming them (RFC 8707). This endpoint used to answer it all the same,
    /// so any resource server handed a token that carried `openid` could bring it here
    /// and read the user's profile and email with it. A token customizer that replaces
    /// `aud` outright decides this too: it keeps one of the three in the claim, or its
    /// tokens are not for this endpoint.
    private boolean forThisServer(Jwt token, String issuer) {
        // Never null: a token with no `aud` has an empty list, and is for nobody.
        List<String> audience = token.getAudience();
        return audience.contains(issuer)
                || defaultAudience != null && audience.contains(defaultAudience)
                || audience.contains(issuer + settings.getOidcUserInfoEndpoint());
    }

    private static Refusal bearer(int status, String code, String description) {
        return new Refusal(status, code, description).header("WWW-Authenticate",
                "Bearer error=\"" + code + "\", error_description=\"" + description + "\"");
    }

    // ---------------------------------------------------- the device grant

    private HttpServer.Response deviceAuthorization(HttpServer.Request request) {
        requireMethod(request, "POST");
        RegisteredClient client = authenticateClient(request);
        requireGrant(client, AuthorizationGrantType.DEVICE_CODE);
        Set<String> scopes = OAuth2Parameters.scopes(param(request, "scope"));
        if (!client.getScopes().containsAll(scopes)) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_SCOPE,
                    "A scope asked for is not one the client was registered with");
        }
        List<String> resources = resources(client,
                OAuth2Parameters.values(form(request), RESOURCE));
        Map<String, Object> asked = null;
        if (!resources.isEmpty()) {
            asked = new LinkedHashMap<String, Object>();
            asked.put(RESOURCE, new ArrayList<Object>(resources));
        }
        long now = clock.currentTimeMillis();
        purge(now);
        long ttl = client.getTokenSettings().getDeviceCodeTimeToLive();
        long expires = now + ttl * 1000L;
        OAuth2Authorization authorization = new OAuth2Authorization(OAuth2Parameters.random(16),
                client.getId(), "", AuthorizationGrantType.DEVICE_CODE.getValue(), scopes,
                OAuth2Authorization.PENDING, asked, now, expires);
        authorizations.save(authorization);
        String deviceCode = OAuth2Parameters.random(32);
        String userCode = newUserCode();
        authorizations.addToken(authorization.getId(), OAuth2AuthorizationService.DEVICE_CODE,
                OAuth2Parameters.sha256(deviceCode), expires);
        authorizations.addToken(authorization.getId(), OAuth2AuthorizationService.USER_CODE,
                OAuth2Parameters.sha256(userCode), expires);
        String shown = userCode.substring(0, 4) + "-" + userCode.substring(4);
        String page = issuer(request) + settings.getDeviceVerificationEndpoint();
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        answer.put("device_code", deviceCode);
        answer.put("user_code", shown);
        answer.put("verification_uri", page);
        answer.put("verification_uri_complete", page + "?user_code=" + shown);
        answer.put("expires_in", Long.valueOf(ttl));
        answer.put("interval", Long.valueOf(DEVICE_POLL_INTERVAL_SECONDS));
        return json(200, answer);
    }

    /// Eight letters of the user code alphabet, each chosen evenly: a byte is
    /// used only when it is below the largest multiple of twenty, so that no
    /// letter is likelier than another.
    static String newUserCode() {
        StringBuilder code = new StringBuilder(USER_CODE_LENGTH);
        int letters = USER_CODE_ALPHABET.length();
        int bound = 256 - 256 % letters;
        try {
            while (code.length() < USER_CODE_LENGTH) {
                byte[] random = Crypto.randomBytes(16);
                for (int iter = 0 ; iter < random.length && code.length() < USER_CODE_LENGTH ; iter++) {
                    int b = random[iter] & 0xff;
                    if (b < bound) {
                        code.append(USER_CODE_ALPHABET.charAt(b % letters));
                    }
                }
            }
        } catch (IOException err) {
            throw new IllegalStateException("The system has no source of random bytes: "
                    + err.getMessage(), err);
        }
        return code.toString();
    }

    /// A user code as it is stored: its letters, in upper case, and nothing a
    /// person might type between or around them.
    static String normalizeUserCode(String typed) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0 ; typed != null && iter < typed.length() && out.length() < 64 ; iter++) {
            char c = typed.charAt(iter);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - ('a' - 'A'));
            }
            if (c >= 'A' && c <= 'Z') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private Map<String, Object> redeemDeviceCode(HttpServer.Request request,
                                                 RegisteredClient client, long now) {
        requireGrant(client, AuthorizationGrantType.DEVICE_CODE);
        String deviceCode = param(request, "device_code");
        if (deviceCode == null || deviceCode.length() == 0) {
            throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST, "device_code is required");
        }
        String hash = OAuth2Parameters.sha256(deviceCode);
        OAuth2AuthorizationService.StoredToken stored = authorizations.findToken(
                OAuth2AuthorizationService.DEVICE_CODE, hash);
        OAuth2Authorization authorization = stored == null ? null
                : authorizations.findById(stored.getAuthorizationId());
        if (authorization == null || stored.isUsed()
                || !authorization.getRegisteredClientId().equals(client.getId())) {
            throw invalidGrant();
        }
        if (stored.getExpiresAt() <= now) {
            throw new Refusal(400, OAuth2ErrorCodes.EXPIRED_TOKEN,
                    "The device code has expired; start again");
        }
        long sincePoll = now - stored.getPolledAt();
        authorizations.touchToken(OAuth2AuthorizationService.DEVICE_CODE, hash, now);
        if (stored.getPolledAt() > 0 && sincePoll < DEVICE_POLL_INTERVAL_SECONDS * 1000L) {
            throw new Refusal(400, OAuth2ErrorCodes.SLOW_DOWN,
                    "Polling too often; wait five seconds longer between requests");
        }
        if (OAuth2Authorization.PENDING.equals(authorization.getStatus())) {
            throw new Refusal(400, OAuth2ErrorCodes.AUTHORIZATION_PENDING,
                    "The user has not answered yet");
        }
        if (OAuth2Authorization.DENIED.equals(authorization.getStatus())) {
            authorizations.remove(authorization.getId());
            throw new Refusal(400, OAuth2ErrorCodes.ACCESS_DENIED, "The user refused");
        }
        if (!authorizations.consumeToken(OAuth2AuthorizationService.DEVICE_CODE, hash, now)) {
            throw invalidGrant();
        }
        return issue(request, client, authorization, authorization.getPrincipalName(),
                AuthorizationGrantType.DEVICE_CODE.getValue(), authorization.getScopes(), now, null);
    }

    /// Answers the device verification page for the signed-in user
    /// `authentication`: the form that takes a user code, the question that
    /// follows a right one, and the answer to that question.
    ///
    /// The chain's CSRF protection covers the two posts; the token to put in
    /// the forms is passed in.
    ///
    /// - `InsufficientAuthenticationException`: when nobody is signed in, so
    /// that the chain sends the browser to sign in and back
    ///
    /// @param authentication who is signed in; null for nobody
    /// @param csrfParameter the name of the CSRF form field, or null
    /// @param csrfToken its value
    public HttpServer.Response deviceVerification(HttpServer.Request request,
            Authentication authentication, String csrfParameter, String csrfToken) {
        return deviceVerification(request, authentication, csrfParameter, null, csrfToken);
    }

    /// [#deviceVerification(HttpServer.Request, Authentication, String, String)],
    /// told the header a CSRF token may come back in as well.
    ///
    /// The endpoint has two forms, and the request chooses. A browser gets the
    /// pages. A request that asks for JSON -- its `Accept` names
    /// `application/json` and not `text/html`, or its body is
    /// `application/json` -- gets the same three steps as data, so that an
    /// application that signed the user in itself can put its own screen in
    /// front of them:
    ///
    /// 1. `GET`: `{"csrf": {"headerName", "parameterName", "token"}}`, the
    ///    token a post must carry when the chain protects against CSRF.
    /// 2. `POST {"user_code": "..."}`: the question, as
    ///    `{"user_code", "client_id", "client_name", "scope", "principal",
    ///    "ticket"}`.
    /// 3. `POST {"user_code": "...", "ticket": "...", "decision": "approve"}`
    ///    or `"deny"`: `{"status": "approved"}` or `{"status": "denied"}`.
    ///
    /// Nothing is relaxed for it. The user must be signed in -- a request that
    /// is not is answered 401 `login_required` rather than sent to a login
    /// page -- the chain's CSRF protection covers both posts, each try at a
    /// code is counted, and an answer counts only with the ticket this server
    /// put in that session with the question, once. A code that is not valid
    /// is 400 `invalid_grant`, an answer without its ticket 400
    /// `invalid_request`, and too many tries 429 `slow_down`.
    ///
    /// @param csrfHeader the name of the header a CSRF token may be sent in,
    /// or null
    public HttpServer.Response deviceVerification(HttpServer.Request request,
            Authentication authentication, String csrfParameter, String csrfHeader,
            String csrfToken) {
        boolean json = asksForJson(request);
        if (authentication == null) {
            if (json) {
                // A program, which cannot use a login page: told so directly.
                return error(401, OAuth2ErrorCodes.LOGIN_REQUIRED, "Nobody is signed in", null);
            }
            throw new InsufficientAuthenticationException(
                    "Full authentication is required to approve a device");
        }
        try {
            return json ? verificationJson(request, authentication, csrfParameter, csrfHeader,
                    csrfToken) : verificationChecked(request, authentication, csrfParameter,
                    csrfToken);
        } catch (Refusal refusal) {
            return refusal.response();
        } catch (IllegalStateException failed) {
            System.err.println("cn1: the device verification page could not answer: "
                    + failed.getMessage());
            return json ? error(500, OAuth2ErrorCodes.SERVER_ERROR, "The server could not answer",
                    null) : page(500, "<p role=\"alert\">The server could not answer. Try "
                    + "again.</p>");
        }
    }

    /// Whether a request to the verification endpoint wants its JSON form.
    private static boolean asksForJson(HttpServer.Request request) {
        if (isJson(request.getHeader("Content-Type"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && contains(accept, "application/json")
                && !contains(accept, "text/html");
    }

    private static boolean isJson(String contentType) {
        return contentType != null && contentType.trim().regionMatches(true, 0,
                "application/json", 0, 16);
    }

    /// Whether `header` names `type`, in any case: a media type is ASCII, and
    /// is compared without folding either side.
    private static boolean contains(String header, String type) {
        for (int at = 0 ; at + type.length() <= header.length() ; at++) {
            if (header.regionMatches(true, at, type, 0, type.length())) {
                return true;
            }
        }
        return false;
    }

    /// What one step at the verification endpoint comes to, whichever form
    /// asked: the pages and the JSON are two ways of writing these down.
    private static final class Verification {
        static final int LIMITED = 0;
        static final int INVALID = 1;
        static final int QUESTION = 2;
        static final int NO_TICKET = 3;
        static final int APPROVED = 4;
        static final int DENIED = 5;

        final int outcome;
        long retryAfter;
        String typed;
        String ticket;
        String who;
        RegisteredClient client;
        OAuth2Authorization authorization;

        Verification(int outcome) {
            this.outcome = outcome;
        }
    }

    /// One post to the verification endpoint: a code looked up, or a question
    /// answered.
    ///
    /// @param typed the user code as sent
    /// @param decision `approve`, `deny`, or null for a lookup
    /// @param ticket what came back with the decision
    /// @param reask whether an answer without its ticket is turned into the
    /// question again, as the page does, rather than refused
    private Verification verify(HttpServer.Request request, Authentication authentication,
            String typed, String decision, String ticket, boolean reask) {
        typed = normalizeUserCode(typed);
        String who = authentication.getName();
        String hash = OAuth2Parameters.sha256(typed);
        HttpSession session = request.getSession(false);
        if (decision != null) {
            // An answer counts only from the question this server put to this
            // session: the ticket that went out with it comes back, once. The
            // chain's CSRF protection covers this form too, but a device handed
            // to whoever asks is not left to a setting an application may have
            // turned off.
            Object issued = session == null ? null : session.getAttribute(DEVICE_TICKET);
            if (session != null) {
                session.removeAttribute(DEVICE_TICKET);
            }
            if (!(issued instanceof String) || ticket == null
                    || !OAuth2Parameters.equalsConstantTime((String) issued, ticket + "." + hash)) {
                if (!reask) {
                    return new Verification(Verification.NO_TICKET);
                }
                decision = null;
            }
        }
        // Counted before the code is looked at, wrong or right: what is being
        // bounded is how many codes one user may try.
        boolean allowed = decision != null || (verificationLimiter != null
                ? verificationLimiter.tryAcquire("device-verification:" + who)
                : attempts.tryAcquire(who, clock.currentTimeMillis()));
        if (!allowed) {
            Verification limited = new Verification(Verification.LIMITED);
            long wait = verificationLimiter != null
                    ? verificationLimiter.retryAfterSeconds("device-verification:" + who)
                    : attempts.windowMillis / 1000L;
            limited.retryAfter = wait < 1 ? 1 : wait;
            return limited;
        }
        long now = clock.currentTimeMillis();
        OAuth2AuthorizationService.StoredToken stored = typed.length() == USER_CODE_LENGTH
                ? authorizations.findToken(OAuth2AuthorizationService.USER_CODE, hash) : null;
        OAuth2Authorization authorization = stored == null || stored.isUsed()
                || stored.getExpiresAt() <= now ? null
                : authorizations.findById(stored.getAuthorizationId());
        RegisteredClient client = authorization == null ? null
                : clients.findById(authorization.getRegisteredClientId());
        if (client == null || !OAuth2Authorization.PENDING.equals(authorization.getStatus())) {
            return new Verification(Verification.INVALID);
        }
        if (decision == null) {
            if (session == null) {
                // An answer that arrived with no session at all -- a user signed
                // in by credentials on the request itself, or one whose session
                // ended while the question was on their screen. It was discarded
                // above like any answer without its ticket; the question it is
                // asked instead needs somewhere to keep the new one.
                session = request.getSession(true);
            }
            Verification question = new Verification(Verification.QUESTION);
            question.ticket = OAuth2Parameters.random(32);
            session.setAttribute(DEVICE_TICKET, question.ticket + "." + hash);
            question.typed = typed;
            question.who = who;
            question.client = client;
            question.authorization = authorization;
            return question;
        }
        boolean approve = "approve".equals(decision);
        // The code is used up by one statement, and only the request that used
        // it up answers the grant.
        if (!authorizations.consumeToken(OAuth2AuthorizationService.USER_CODE, hash, now)) {
            return new Verification(Verification.INVALID);
        }
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("auth_time", Long.valueOf(authTime(request, now)));
        attributes.put("authorities", authorities(authentication));
        Object resource = authorization.getAttribute(RESOURCE);
        if (resource != null) {
            // What the device asked the tokens to be for is part of the grant.
            attributes.put(RESOURCE, resource);
        }
        if (!authorizations.decide(authorization.getId(), approve, who, attributes)) {
            return new Verification(Verification.INVALID);
        }
        return new Verification(approve ? Verification.APPROVED : Verification.DENIED);
    }

    private HttpServer.Response verificationChecked(HttpServer.Request request,
            Authentication authentication, String csrfParameter, String csrfToken) {
        String hidden = csrfParameter == null || csrfToken == null ? ""
                : "<input type=\"hidden\" name=\"" + escape(csrfParameter) + "\" value=\""
                        + escape(csrfToken) + "\">\n";
        String action = escape(settings.getDeviceVerificationEndpoint());
        if ("GET".equals(request.getMethod())) {
            return page(200, codeForm(action, hidden,
                    normalizeUserCode(request.queryParam("user_code")), null));
        }
        requireMethod(request, "POST");
        Verification step = verify(request, authentication, param(request, "user_code"),
                param(request, "decision"), param(request, "ticket"), true);
        switch (step.outcome) {
            case Verification.LIMITED:
                return page(429, "<p role=\"alert\">Too many codes were tried. "
                        + "Wait a few minutes and try again.</p>")
                        .header("Retry-After", String.valueOf(step.retryAfter));
            case Verification.QUESTION:
                return page(200, question(step, action, hidden));
            case Verification.APPROVED:
                return page(200, "<h2>Device approved</h2>\n<p>You can go back to it now.</p>\n");
            case Verification.DENIED:
                return page(200, "<h2>Device refused</h2>\n<p>Nothing was given access.</p>\n");
            default:
                return page(200, codeForm(action, hidden, "",
                        "That code is not valid, or has expired. Check the device and try "
                        + "again."));
        }
    }

    /// The question, naming who is asking: a code somebody else sent the user
    /// is for a device of theirs, and this is where the user sees it.
    private static String question(Verification step, String action, String hidden) {
        StringBuilder body = new StringBuilder();
        body.append("<h2>Sign in a device</h2>\n<p><strong>").append(escape(
                step.client.getClientName())).append("</strong> is asking to act as <strong>")
            .append(escape(step.who)).append("</strong>");
        if (!step.authorization.getScopes().isEmpty()) {
            body.append(", with: ").append(escape(OAuth2Parameters.scopes(
                    step.authorization.getScopes())));
        }
        body.append(".</p>\n<p>Approve only if you started this on a device of your own, and "
                + "the code it shows is ").append(step.typed.substring(0, 4)).append('-')
                .append(step.typed.substring(4)).append(".</p>\n<form method=\"post\" action=\"")
                .append(action).append("\">\n").append(hidden)
                .append("<input type=\"hidden\" name=\"ticket\" value=\"").append(step.ticket)
                .append("\">\n")
                .append("<input type=\"hidden\" name=\"user_code\" value=\"").append(step.typed)
                .append("\">\n<button type=\"submit\" name=\"decision\" value=\"approve\">Approve"
                    + "</button>\n<button type=\"submit\" name=\"decision\" value=\"deny\">Deny"
                    + "</button>\n</form>\n");
        return body.toString();
    }

    /// The verification endpoint for a request that asked for JSON; see
    /// [#deviceVerification(HttpServer.Request, Authentication, String, String, String)].
    private HttpServer.Response verificationJson(HttpServer.Request request,
            Authentication authentication, String csrfParameter, String csrfHeader,
            String csrfToken) {
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        if ("GET".equals(request.getMethod())) {
            if (csrfToken != null) {
                Map<String, Object> csrf = new LinkedHashMap<String, Object>();
                if (csrfHeader != null) {
                    csrf.put("headerName", csrfHeader);
                }
                if (csrfParameter != null) {
                    csrf.put("parameterName", csrfParameter);
                }
                csrf.put("token", csrfToken);
                answer.put("csrf", csrf);
            }
            return frameless(json(200, answer));
        }
        requireMethod(request, "POST");
        Map sent = null;
        if (isJson(request.getHeader("Content-Type"))) {
            try {
                sent = Json.parseObject(request.getBody());
            } catch (IOException malformed) {
                sent = null;
            } catch (RuntimeException malformed) {
                sent = null;
            }
            if (sent == null) {
                throw new Refusal(400, OAuth2ErrorCodes.INVALID_REQUEST,
                        "The body is not a JSON object");
            }
        }
        Verification step = verify(request, authentication, text(request, sent, "user_code"),
                text(request, sent, "decision"), text(request, sent, "ticket"), false);
        switch (step.outcome) {
            case Verification.LIMITED:
                return frameless(error(429, OAuth2ErrorCodes.SLOW_DOWN,
                        "Too many codes were tried; wait and try again", null)
                        .header("Retry-After", String.valueOf(step.retryAfter)));
            case Verification.QUESTION:
                answer.put("user_code", step.typed.substring(0, 4) + "-"
                        + step.typed.substring(4));
                answer.put("client_id", step.client.getClientId());
                answer.put("client_name", step.client.getClientName());
                answer.put("scope", OAuth2Parameters.scopes(step.authorization.getScopes()));
                answer.put("principal", step.who);
                answer.put("ticket", step.ticket);
                return frameless(json(200, answer));
            case Verification.NO_TICKET:
                return frameless(error(400, OAuth2ErrorCodes.INVALID_REQUEST, "The answer does "
                        + "not carry the ticket of the question it answers; look the code up "
                        + "again", null));
            case Verification.APPROVED:
                answer.put("status", "approved");
                return frameless(json(200, answer));
            case Verification.DENIED:
                answer.put("status", "denied");
                return frameless(json(200, answer));
            default:
                return frameless(error(400, OAuth2ErrorCodes.INVALID_GRANT,
                        "That code is not valid, or has expired", null));
        }
    }

    /// A field of a JSON body, as text; of the form or the query when the
    /// request sent no JSON. Null when it is absent or is not text.
    private static String text(HttpServer.Request request, Map sent, String name) {
        if (sent == null) {
            return param(request, name);
        }
        Object value = sent.get(name);
        return value instanceof String ? (String) value : null;
    }

    /// `response`, never to be shown in a frame: it is an answer about a
    /// signed-in user, as the pages are.
    private static HttpServer.Response frameless(HttpServer.Response response) {
        return response.header("Content-Security-Policy", "frame-ancestors 'none'");
    }

    private static String codeForm(String action, String hidden, String code, String problem) {
        return "<h2>Sign in a device</h2>\n"
                + (problem == null ? "" : "<p role=\"alert\">" + problem + "</p>\n")
                + "<form method=\"post\" action=\"" + action + "\">\n" + hidden
                + "<p><label for=\"user_code\">The code shown on the device</label>\n"
                + "<input type=\"text\" id=\"user_code\" name=\"user_code\" value=\""
                + escape(code) + "\" required autofocus autocomplete=\"off\" "
                + "autocapitalize=\"characters\"></p>\n"
                + "<button type=\"submit\">Continue</button>\n</form>\n";
    }

    private static HttpServer.Response page(int status, String body) {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("Cache-Control", "no-store");
        // Beside the chain's own X-Frame-Options, and there even if the chain
        // turned that off: this page asks a signed-in user to press a button.
        headers.put("Content-Security-Policy", "frame-ancestors 'none'");
        return new HttpServer.Response(status, "text/html; charset=utf-8", OAuth2Parameters.utf8(
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>Sign in a device</title>\n</head>\n<body>\n" + body
                + "</body>\n</html>\n"), headers);
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Wrong codes one user may try: ten in five minutes, counted here.
    private static final class Attempts {
        private final int limit;
        private final long windowMillis;
        private final Map<String, long[]> windows = new HashMap<String, long[]>();

        Attempts(int limit, long windowMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
        }

        synchronized boolean tryAcquire(String key, long now) {
            if (windows.size() > 10000) {
                // Bounded: under a flood, forgetting everybody's count is
                // better than keeping one per name ever seen.
                windows.clear();
            }
            long[] window = windows.get(key);
            if (window == null || now - window[0] >= windowMillis) {
                window = new long[] {now, 0};
                windows.put(key, window);
            }
            window[1]++;
            return window[1] <= limit;
        }
    }

    // ------------------------------------------------------------ answering

    private static String param(HttpServer.Request request, String name) {
        try {
            return request.param(name);
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static void requireMethod(HttpServer.Request request, String method) {
        if (!method.equals(request.getMethod())) {
            throw new Refusal(405, OAuth2ErrorCodes.INVALID_REQUEST, "This endpoint takes "
                    + method).header("Allow", method);
        }
    }

    private static HttpServer.Response json(int status, Map<String, Object> body) {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        // RFC 6749 5.1: nothing that holds a token may be kept by a cache.
        headers.put("Cache-Control", "no-store");
        headers.put("Pragma", "no-cache");
        return new HttpServer.Response(status, "application/json", OAuth2Parameters.utf8(
                Json.write(body)), headers);
    }

    private static HttpServer.Response error(int status, String code, String description,
                                             Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        if (code != null) {
            body.put("error", code);
            if (description != null) {
                body.put("error_description", description);
            }
        }
        HttpServer.Response response = json(status, body);
        if (extra != null) {
            for (Map.Entry<String, Object> header : extra.entrySet()) {
                response = response.header(header.getKey(), String.valueOf(header.getValue()));
            }
        }
        return response;
    }

    /// An endpoint's refusal, on its way out as an OAuth2 error.
    private static final class Refusal extends RuntimeException {
        private final int status;
        private final String code;
        private final String description;
        private final Map<String, Object> headers = new LinkedHashMap<String, Object>();

        Refusal(int status, String code, String description) {
            super(code);
            this.status = status;
            this.code = code;
            this.description = description;
        }

        Refusal header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        HttpServer.Response response() {
            return error(status, code, description, headers);
        }
    }
}
