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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.oauth2.client.AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.ClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistrations;
import com.codename1.backend.security.oauth2.client.DefaultAuthorizationCodeTokenResponseClient;
import com.codename1.backend.security.oauth2.client.OAuth2AccessTokenResponse;
import com.codename1.backend.security.oauth2.client.OAuth2AccessTokenResponseClient;
import com.codename1.backend.security.oauth2.client.OAuth2AuthenticationToken;
import com.codename1.backend.security.oauth2.client.OAuth2AuthorizationRequest;
import com.codename1.backend.security.oauth2.client.OAuth2User;
import com.codename1.backend.security.oauth2.client.OAuth2UserRequest;
import com.codename1.backend.security.oauth2.client.OAuth2UserService;
import com.codename1.backend.security.oauth2.client.OidcIdTokenDecoderFactory;
import com.codename1.backend.security.oauth2.client.OidcUser;
import com.codename1.backend.security.oauth2.client.OidcUserRequest;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtException;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;
import java.util.Map;

/// Ends a sign-in at an identity provider: takes the provider's answer at
/// `/login/oauth2/code/{registrationId}`, holds it to the request this browser
/// was sent away with, exchanges the code, verifies the ID token and signs the
/// user in.
public final class OAuth2LoginAuthenticationFilter implements SecurityFilter {
    private final String callbackPrefix;
    private final ClientRegistrationRepository registrations;
    private final AuthorizationRequestRepository session;
    private final AuthorizationRequestRepository posted;
    private final OAuth2AccessTokenResponseClient tokenClient;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> userService;
    private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    private final OidcIdTokenDecoderFactory decoders;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final SessionSignIn signIn;
    private final Map<String, ClientRegistration> resolved =
            new java.util.HashMap<String, ClientRegistration>();

    OAuth2LoginAuthenticationFilter(String callbackPrefix,
            ClientRegistrationRepository registrations, AuthorizationRequestRepository session,
            AuthorizationRequestRepository posted, OAuth2AccessTokenResponseClient tokenClient,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            OidcIdTokenDecoderFactory decoders, AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler, SessionSignIn signIn) {
        this.callbackPrefix = callbackPrefix;
        this.registrations = registrations;
        this.session = session;
        this.posted = posted;
        this.tokenClient = tokenClient;
        this.userService = userService;
        this.oidcUserService = oidcUserService;
        this.decoders = decoders;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.signIn = signIn;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        boolean post = "POST".equals(request.getMethod());
        if (!path.startsWith(callbackPrefix) || path.indexOf('/', callbackPrefix.length()) >= 0
                || !(post || "GET".equals(request.getMethod()))) {
            return chain.doFilter(request);
        }
        String state = Responses.param(request, "state");
        String code = Responses.param(request, "code");
        String error = Responses.param(request, "error");
        if (state == null || (code == null && error == null)) {
            // Not an answer from a provider at all.
            return chain.doFilter(request);
        }
        Authentication authentication;
        try {
            authentication = authenticate(request, path.substring(callbackPrefix.length()), post,
                    state, code, error);
        } catch (AuthenticationException refused) {
            signIn.failure(request);
            return failureHandler.onAuthenticationFailure(request, refused);
        }
        return signIn.success(request, authentication, successHandler);
    }

    private Authentication authenticate(HttpServer.Request request, String registrationId,
            boolean post, String state, String code, String error) {
        // Taken back whatever follows: a request answers one callback.
        OAuth2AuthorizationRequest sent = (post ? posted : session)
                .removeAuthorizationRequest(request);
        if (sent == null) {
            throw refused(OAuth2ErrorCodes.AUTHORIZATION_REQUEST_NOT_FOUND,
                    "No sign-in was waiting for this answer");
        }
        if (!OAuth2Parameters.equalsConstantTime(sent.getState(), state)) {
            throw refused(OAuth2ErrorCodes.INVALID_STATE_PARAMETER,
                    "The state is not the one this browser was given");
        }
        ClientRegistration registration = registrations.findByRegistrationId(registrationId);
        if (registration == null || !registrationId.equals(sent.getRegistrationId())) {
            throw refused(OAuth2ErrorCodes.AUTHORIZATION_REQUEST_NOT_FOUND,
                    "The answer is for another registration than the sign-in that waited");
        }
        if (post != ClientRegistration.FORM_POST.equals(registration.getResponseMode())) {
            throw refused(OAuth2ErrorCodes.INVALID_REQUEST,
                    "The answer did not arrive the way the registration asks for it");
        }
        registration = resolved(registration);
        // Before the error as much as before the code: an answer from another
        // issuer is not this provider refusing anything.
        answeredBy(registration, Responses.param(request, "iss"));
        if (error != null) {
            // The code, and nothing else the provider wrote beside it.
            throw refused(DefaultAuthorizationCodeTokenResponseClient.sanitizeErrorCode(error),
                    "The provider refused the sign-in");
        }
        OAuth2AccessTokenResponse tokens = tokenClient.getTokenResponse(registration, sent, code);
        OAuth2User user;
        if (registration.getScopes().contains("openid")) {
            user = oidcUserService.loadUser(new OidcUserRequest(registration, tokens,
                    idToken(registration, sent, tokens)));
        } else {
            user = userService.loadUser(new OAuth2UserRequest(registration, tokens));
        }
        if (user == null) {
            throw refused(OAuth2ErrorCodes.INVALID_USER_INFO_RESPONSE, "No user was made");
        }
        // Kept in the session as what it is by the codec http.oauth2Login()
        // registers; see OAuth2AuthenticationCodec.
        return new OAuth2AuthenticationToken(user, user.getAuthorities(),
                registration.getRegistrationId());
    }

    /// The registration with its provider's endpoints known: read from the
    /// issuer's metadata the first time, and kept.
    private ClientRegistration resolved(ClientRegistration registration) {
        synchronized (resolved) {
            ClientRegistration known = resolved.get(registration.getRegistrationId());
            if (known != null) {
                return known;
            }
        }
        ClientRegistration complete;
        try {
            complete = ClientRegistrations.resolve(registration, RemoteJwkSet.WEB);
        } catch (RuntimeException err) {
            throw refused(OAuth2ErrorCodes.SERVER_ERROR, "The provider's metadata could not be "
                    + "read");
        }
        synchronized (resolved) {
            resolved.put(registration.getRegistrationId(), complete);
        }
        return complete;
    }

    /// Holds the answer to the provider the browser was sent to (RFC 9207). A
    /// client registered with several providers is otherwise open to one of
    /// them answering a sign-in that was started at another, and having its
    /// code sent to the wrong token endpoint.
    ///
    /// An `iss` that is there must be the registration's issuer. One that is
    /// missing is refused when the provider says it always sends it. A
    /// registration that names no issuer has nothing to hold an `iss` to, and
    /// it is ignored.
    private static void answeredBy(ClientRegistration registration, String iss) {
        ClientRegistration.ProviderDetails p = registration.getProviderDetails();
        if (iss == null) {
            if (p.isAuthorizationResponseIssParameterSupported()) {
                throw refused(OAuth2ErrorCodes.INVALID_ISSUER, "The provider names itself in "
                        + "every answer, and this one has no iss");
            }
            return;
        }
        if (p.getIssuerUri() == null && p.getIssuerTemplate() == null) {
            return;
        }
        if (!p.isIssuer(iss)) {
            throw refused(OAuth2ErrorCodes.INVALID_ISSUER, "The answer is from another issuer "
                    + "than the provider this sign-in was sent to");
        }
    }

    /// Holds an ID token's `at_hash`, when it has one, to the access token it
    /// came with: the left half of the token's hash under the digest of the
    /// algorithm that signed the ID token. An access token swapped for
    /// another on the way has another hash.
    private static void accessTokenHash(Jwt idToken, String accessToken) {
        if (!idToken.hasClaim("at_hash")) {
            return;
        }
        Object claimed = idToken.getClaim("at_hash");
        Object alg = idToken.getHeaders().get("alg");
        String expected = null;
        if (accessToken != null && alg instanceof String) {
            byte[] token = OAuth2Parameters.utf8(accessToken);
            String name = (String) alg;
            byte[] digest = name.endsWith("384") ? Crypto.sha384(token)
                    : name.endsWith("512") ? Crypto.sha512(token) : Crypto.sha256(token);
            byte[] half = new byte[digest.length / 2];
            System.arraycopy(digest, 0, half, 0, half.length);
            expected = Base64Url.encode(half);
        }
        if (expected == null || !(claimed instanceof String)
                || !OAuth2Parameters.equalsConstantTime(expected, (String) claimed)) {
            throw refused(OAuth2ErrorCodes.INVALID_ID_TOKEN, "The at_hash of the ID token is "
                    + "not the hash of the access token it came with");
        }
    }

    private Jwt idToken(ClientRegistration registration, OAuth2AuthorizationRequest sent,
                        OAuth2AccessTokenResponse tokens) {
        String encoded = tokens.getIdToken();
        if (encoded == null) {
            throw refused(OAuth2ErrorCodes.INVALID_ID_TOKEN, "The provider sent no ID token");
        }
        Jwt idToken;
        try {
            idToken = decoders.createDecoder(registration).decode(encoded);
        } catch (JwtException err) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_ID_TOKEN), "The ID token did not verify: "
                    + err.getMessage(), err);
        } catch (IllegalArgumentException err) {
            throw refused(OAuth2ErrorCodes.INVALID_ID_TOKEN, "The ID token cannot be verified");
        }
        // The nonce ties the token to this browser's request: a token issued
        // for another sign-in, replayed here, has another one.
        if (sent.getNonce() == null || !OAuth2Parameters.equalsConstantTime(sent.getNonce(),
                idToken.getClaimAsString("nonce"))) {
            throw refused(OAuth2ErrorCodes.INVALID_NONCE,
                    "The nonce of the ID token is not the one that was sent");
        }
        accessTokenHash(idToken, tokens.getAccessToken());
        return idToken;
    }

    private static OAuth2AuthenticationException refused(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
