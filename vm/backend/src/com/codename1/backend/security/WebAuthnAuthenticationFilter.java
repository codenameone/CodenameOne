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

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.Json;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.backend.security.webauthn.WebAuthnAuthentication;
import com.codename1.backend.security.webauthn.WebAuthnException;
import com.codename1.backend.security.webauthn.WebAuthnRelyingPartyOperations;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// Signs a user in with a passkey: hands out the options of a sign-in at
/// `POST /webauthn/authenticate/options`, and takes the authenticator's answer
/// at `POST /login/webauthn`.
///
/// The options are kept in the session they were asked from, for five minutes
/// unless set otherwise, and are taken out of it before the answer is looked
/// at: a challenge is answered once, in the session it was given to, in time,
/// or not at all.
public final class WebAuthnAuthenticationFilter implements SecurityFilter {
    /// The session attribute a sign-in's options wait under.
    public static final String PENDING = "CN1_WEBAUTHN_AUTHENTICATION";
    /// The most characters of a request body that are read.
    static final int MAX_BODY = 262144;

    private final WebAuthnRelyingPartyOperations operations;
    private final String optionsUrl;
    private final String loginUrl;
    private final boolean usernameFirst;
    private final long validityMillis;
    private final UserDetailsService users;
    private final SessionSignIn signIn;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final Clock clock;

    WebAuthnAuthenticationFilter(WebAuthnRelyingPartyOperations operations, String optionsUrl,
            String loginUrl, boolean usernameFirst, long validityMillis, UserDetailsService users,
            SessionSignIn signIn, AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler, Clock clock) {
        this.operations = operations;
        this.optionsUrl = optionsUrl;
        this.loginUrl = loginUrl;
        this.usernameFirst = usernameFirst;
        this.validityMillis = validityMillis;
        this.users = users;
        this.signIn = signIn;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.clock = clock;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!"POST".equals(request.getMethod())) {
            return chain.doFilter(request);
        }
        String path = SecurityExchange.path(request);
        if (optionsUrl.equals(path)) {
            return options(request);
        }
        if (!loginUrl.equals(path)) {
            return chain.doFilter(request);
        }
        Authentication authentication;
        boolean verified;
        try {
            // Out of the session first: whatever the answer turns out to be,
            // the challenge has been used.
            Map pending = take(request, PENDING, clock);
            Map answer = body(request);
            if (answer == null) {
                throw new WebAuthnException(WebAuthnException.MALFORMED,
                        "The request body is not the JSON of a credential");
            }
            WebAuthnRelyingPartyOperations.Assertion assertion = operations.authenticate(
                    PublicKeyCredentialRequestOptions.fromMap(pending), answer);
            UserDetails user = user(assertion.getUser().getName());
            authentication = new WebAuthnAuthentication(assertion.getUser(),
                    user.getAuthorities(), assertion.getCredential().getCredentialId(),
                    assertion.isUserVerified());
            verified = assertion.isUserVerified();
        } catch (AuthenticationException refused) {
            signIn.failure(request);
            return failureHandler.onAuthenticationFailure(request, refused);
        }
        // Two factors in one when the authenticator verified the user; one
        // when it did not, and the chain's second factor then applies.
        return signIn.success(request, authentication, successHandler, verified);
    }

    private HttpServer.Response options(HttpServer.Request request) {
        String username = null;
        if (usernameFirst) {
            Map asked = body(request);
            Object named = asked == null ? null : asked.get("username");
            username = named instanceof String ? (String) named : Responses.param(request,
                    "username");
        }
        PublicKeyCredentialRequestOptions options =
                operations.createCredentialRequestOptions(username);
        keep(request, PENDING, options.toStoredMap(), clock.currentTimeMillis() + validityMillis);
        return json(200, options.toMap());
    }

    /// The local user a passkey's owner is, who must be able to sign in.
    private UserDetails user(String username) {
        UserDetails user;
        try {
            user = users.loadUserByUsername(username);
        } catch (UsernameNotFoundException gone) {
            user = null;
        }
        if (user == null || !user.isEnabled() || !user.isAccountNonLocked()
                || !user.isAccountNonExpired()) {
            throw new WebAuthnException(WebAuthnException.ACCOUNT_UNAVAILABLE,
                    "The user this passkey belongs to cannot sign in");
        }
        return user;
    }

    // ------------------------------------ what both passkey filters share

    /// Keeps a ceremony's options in the session until `expires`.
    static void keep(HttpServer.Request request, String attribute, Map<String, Object> options,
                     long expires) {
        Map<String, Object> pending = new LinkedHashMap<String, Object>();
        pending.put("options", options);
        pending.put("expires", Long.valueOf(expires));
        request.getSession(true).setAttribute(attribute, pending);
    }

    /// Takes a ceremony's options out of the session, so that they are
    /// answered once.
    ///
    /// - [WebAuthnException]: when none were waiting, or they waited too long
    static Map take(HttpServer.Request request, String attribute, Clock clock) throws IOException {
        HttpSession session = request.getSession(false);
        Object stored = session == null ? null : session.consumeAttribute(attribute);
        if (!(stored instanceof Map)) {
            throw new WebAuthnException(WebAuthnException.NO_CHALLENGE, "No ceremony was "
                    + "waiting in this session: it was not started here, or its challenge has "
                    + "been answered already");
        }
        Object options = ((Map) stored).get("options");
        Object expires = ((Map) stored).get("expires");
        if (!(options instanceof Map) || !(expires instanceof Number)) {
            throw new WebAuthnException(WebAuthnException.NO_CHALLENGE,
                    "What waited in this session is not a ceremony");
        }
        if (((Number) expires).longValue() < clock.currentTimeMillis()) {
            throw new WebAuthnException(WebAuthnException.CHALLENGE_EXPIRED,
                    "The ceremony was started too long ago; start it again");
        }
        return (Map) options;
    }

    /// The request body as a JSON object, or null when it is not one.
    static Map body(HttpServer.Request request) {
        String text;
        try {
            text = request.getBody();
        } catch (RuntimeException binary) {
            return null;
        }
        if (text == null || text.length() == 0 || text.length() > MAX_BODY) {
            return null;
        }
        Object parsed;
        try {
            parsed = Json.parse(text);
        } catch (IOException malformed) {
            return null;
        } catch (RuntimeException malformed) {
            return null;
        }
        return parsed instanceof Map ? (Map) parsed : null;
    }

    /// A JSON answer nothing may keep: it carries a challenge, or says who
    /// signed in.
    static HttpServer.Response json(int status, Map<String, Object> value) {
        return HttpServer.Response.json(status, Json.write(value))
                .header("Cache-Control", "no-store");
    }
}
