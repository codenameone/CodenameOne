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
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.webauthn.InMemoryPublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.InMemoryUserCredentialRepository;
import com.codename1.backend.security.webauthn.PublicKeyCredentialRpEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codename1.backend.security.webauthn.WebAuthnRelyingPartyOperations;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Passkeys: a user who is signed in registers one, and from then on signs in
/// with it.
///
/// ```java
/// http.formLogin(Customizer.withDefaults())
///     .webAuthn(passkeys -> passkeys
///         .rpId("example.com")
///         .rpName("Example")
///         .allowedOrigins("https://example.com"));
/// ```
///
/// The relying party id is the domain the passkeys belong to, and the allowed
/// origins are where the ceremonies may run -- each exactly as a client
/// reports it, compared whole. An Android application reports
/// `android:apk-key-hash:` and the base64url SHA-256 of its signing
/// certificate; list it beside the web origin when the application is to use
/// the same passkeys.
///
/// ## The endpoints
///
/// They take and answer JSON, the forms the WebAuthn specification defines,
/// so a browser's `PublicKeyCredential.parseCreationOptionsFromJSON` /
/// `parseRequestOptionsFromJSON` / `toJSON()` and the Codename One client's
/// `WebAuthnClient` work with them as they are. No page is served: the
/// application has its own, or is an app.
///
/// | Request | Who | Does |
/// |---|---|---|
/// | `POST /webauthn/register/options` | signed in | answers the options to make a passkey with |
/// | `POST /webauthn/register` | signed in | takes the authenticator's answer; `{"success":true,"credentialId":"..."}`, or 400 and `{"success":false,"error":"..."}` |
/// | `DELETE /webauthn/register/{credentialId}` | signed in, the owner | removes a passkey; 204, or 404 |
/// | `POST /webauthn/authenticate/options` | anybody | answers the options to sign in with |
/// | `POST /login/webauthn` | anybody | takes the authenticator's answer and signs the user in; `{"authenticated":true,"redirectUrl":"/"}`, or 401 and `{"authenticated":false}` |
///
/// `POST /webauthn/register` takes the credential as the client has it, with
/// an optional `label` beside it or as a query parameter, or wrapped as Spring
/// Security wraps it: `{"publicKey": {"credential": {...}, "label": "..."}}`.
///
/// A ceremony's options wait in the session they were asked from, for five
/// minutes, and are taken out before the answer is looked at, so a challenge
/// is answered once. "Signed in" means in this session: somebody a
/// remember-me cookie brought back is asked to sign in before they may add or
/// remove a passkey.
///
/// A refused sign-in is answered 401 and nothing more; which check refused it
/// is in the [com.codename1.backend.security.webauthn.WebAuthnException] the
/// [#failureHandler] is given, for the server's log.
///
/// ## A second factor
///
/// A passkey whose authenticator verified the user -- a fingerprint, a face,
/// a PIN -- is two factors in one step, and signs the user in without the
/// chain's `mfa()` asking for a code. One that did not verify the user is one
/// factor, as a password is, and the second factor is then asked for as it
/// is after a password. To have every passkey sign-in be the first kind, call
/// `userVerification("required")`.
///
/// ## CSRF
///
/// On a chain that keeps a session the two sign-in requests and the three
/// registration requests are checked for the CSRF token like any other
/// `POST` and `DELETE`. A page sends it as it does for its other requests. An
/// app, which has no page to read it from, is served by a chain that leaves
/// the ceremony out of the check:
///
/// ```java
/// http.csrf(csrf -> csrf.ignoringRequestMatchers("/webauthn/**", "/login/webauthn"));
/// ```
///
/// The four `POST`s lose little by it: each answers, or is answered by, a
/// challenge that is 32 random bytes kept in the caller's own session, which
/// a page of another site can neither read nor guess. `DELETE` has no such
/// thing; leave it under the check where browsers sign in to the same chain.
///
/// ## Where things come from
///
/// Passkeys are kept in the application's
/// [UserCredentialRepository] bean and its
/// [PublicKeyCredentialUserEntityRepository] bean -- in the database with the
/// `Jdbc` implementation of each -- or in the ones given here. With neither
/// they are kept in memory and are gone when the server stops, which a server
/// outside a development profile says once when it starts. A user who signs
/// in is looked up in the application's [UserDetailsService], for their
/// authorities and for whether they may sign in at all.
public final class WebAuthnConfigurer extends SecurityConfigurer {
    private String rpId;
    private String rpName;
    private final List<String> allowedOrigins = new ArrayList<String>();
    private String userVerification;
    private String residentKey;
    private String authenticatorAttachment;
    private boolean authenticatorAttachmentSet;
    private boolean allowUnverifiedAttestation;
    private boolean allowCrossOrigin;
    private boolean usernameFirst;
    private int challengeValiditySeconds = 300;
    private UserCredentialRepository credentials;
    private PublicKeyCredentialUserEntityRepository userEntities;
    private UserDetailsService users;
    private WebAuthnRelyingPartyOperations operations;
    private WebAuthnRelyingPartyOperations.SignatureCounterListener counterListener;
    private String defaultSuccessUrl = "/";
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private Clock clock = Clock.SYSTEM;
    private WebAuthnRelyingPartyOperations resolved;

    WebAuthnConfigurer() {
    }

    /// The relying party id: the domain passkeys are bound to, such as
    /// `example.com`. Required.
    public WebAuthnConfigurer rpId(String rpId) {
        this.rpId = rpId;
        return this;
    }

    /// What the user is shown as the site's name; the id unless set.
    public WebAuthnConfigurer rpName(String rpName) {
        this.rpName = rpName;
        return this;
    }

    /// The origins a ceremony may run on: `https://example.com`, with a port
    /// when it is not the default and nothing after it; and
    /// `android:apk-key-hash:...` for an Android application. Required.
    public WebAuthnConfigurer allowedOrigins(String... origins) {
        allowedOrigins.clear();
        if (origins != null) {
            for (String origin : origins) {
                allowedOrigins.add(origin);
            }
        }
        return this;
    }

    /// Whether the authenticator must verify the user: `required`,
    /// `preferred` -- the default -- or `discouraged`. See the class for what
    /// it means for a second factor.
    public WebAuthnConfigurer userVerification(String userVerification) {
        this.userVerification = userVerification;
        return this;
    }

    /// Whether a new credential must be one a sign-in can find without being
    /// told the user: `required` -- the default -- `preferred` or
    /// `discouraged`.
    public WebAuthnConfigurer residentKey(String residentKey) {
        this.residentKey = residentKey;
        return this;
    }

    /// `platform` for the device's own authenticator, `cross-platform` for a
    /// security key, null -- the default -- for either.
    public WebAuthnConfigurer authenticatorAttachment(String authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
        this.authenticatorAttachmentSet = true;
        return this;
    }

    /// Whether a registration whose attestation this server cannot verify is
    /// accepted as if it carried none; see
    /// [WebAuthnRelyingPartyOperations#setAllowUnverifiedAttestation]. Off
    /// unless set, and such a registration is refused with a message naming
    /// the format.
    public WebAuthnConfigurer allowUnverifiedAttestation(boolean allow) {
        this.allowUnverifiedAttestation = allow;
        return this;
    }

    /// Whether a ceremony may run in a frame of another origin. Off unless set.
    public WebAuthnConfigurer allowCrossOrigin(boolean allow) {
        this.allowCrossOrigin = allow;
        return this;
    }

    /// Whether `POST /webauthn/authenticate/options` may be told a `username`
    /// -- in a JSON body or as a parameter -- and then lists that user's
    /// credentials, so that a security key which holds no discoverable
    /// credential can answer. Off unless set, because the list tells whoever
    /// asks that the user exists and has passkeys; a passkey needs no name.
    public WebAuthnConfigurer usernameFirst(boolean usernameFirst) {
        this.usernameFirst = usernameFirst;
        return this;
    }

    /// How long a ceremony's options wait for their answer; 300 seconds
    /// unless set.
    public WebAuthnConfigurer challengeValiditySeconds(int seconds) {
        if (seconds < 1) {
            throw new IllegalArgumentException("challengeValiditySeconds must be positive");
        }
        this.challengeValiditySeconds = seconds;
        return this;
    }

    /// Where credentials are kept, in place of the application's bean.
    public WebAuthnConfigurer userCredentialRepository(UserCredentialRepository repository) {
        this.credentials = repository;
        return this;
    }

    /// Where users' handles are kept, in place of the application's bean.
    public WebAuthnConfigurer userEntityRepository(
            PublicKeyCredentialUserEntityRepository repository) {
        this.userEntities = repository;
        return this;
    }

    /// The users a passkey signs in, in place of the application's bean.
    public WebAuthnConfigurer userDetailsService(UserDetailsService userDetailsService) {
        this.users = userDetailsService;
        return this;
    }

    /// The ceremonies themselves, made by the application: everything set
    /// here about the relying party and the repositories is then its.
    public WebAuthnConfigurer relyingPartyOperations(WebAuthnRelyingPartyOperations operations) {
        this.operations = operations;
        return this;
    }

    /// What is told of a signature counter that did not advance: a credential
    /// that may have been copied. The sign-in is refused either way.
    public WebAuthnConfigurer signatureCounterListener(
            WebAuthnRelyingPartyOperations.SignatureCounterListener listener) {
        this.counterListener = listener;
        return this;
    }

    /// The `redirectUrl` a sign-in is answered with when no page asked for it.
    public WebAuthnConfigurer defaultSuccessUrl(String defaultSuccessUrl) {
        this.defaultSuccessUrl = Responses.path(defaultSuccessUrl, "defaultSuccessUrl");
        return this;
    }

    /// Answers a sign-in itself, instead of the JSON.
    public WebAuthnConfigurer successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /// Answers a refused sign-in itself, instead of the 401; it is handed the
    /// exception, which says why.
    public WebAuthnConfigurer failureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    /// The clock a challenge's lifetime is read from; for tests.
    public WebAuthnConfigurer clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    @Override
    public void init(HttpSecurity http) {
        resolved = operations != null ? operations
                : http.getSharedObject(WebAuthnRelyingPartyOperations.class);
        if (resolved == null) {
            resolved = make(http);
        }
        if (users == null) {
            users = http.chosenUserDetailsService();
        }
        if (users == null) {
            users = http.getSharedObject(UserDetailsService.class);
        }
        if (users == null) {
            throw new IllegalStateException("webAuthn() signs in users of the application, and "
                    + "this application has no UserDetailsService to find them in. Declare one "
                    + "as a bean, or call userDetailsService(...) on the webAuthn() configurer.");
        }
        // A later request sees who signed in with which passkey.
        http.authenticationCodec(new WebAuthnAuthenticationCodec());
        // Whoever signs in is nobody yet.
        http.permit(AntPathRequestMatcher.antMatcher("POST", "/webauthn/authenticate/options"));
        http.permit(AntPathRequestMatcher.antMatcher("POST", "/login/webauthn"));
    }

    private WebAuthnRelyingPartyOperations make(HttpSecurity http) {
        if (rpId == null || rpId.length() == 0) {
            throw new IllegalStateException("webAuthn() needs the relying party id: the domain "
                    + "passkeys are bound to. Call rpId(\"example.com\") on the configurer.");
        }
        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException("webAuthn() needs the origins a ceremony may run "
                    + "on. Call allowedOrigins(\"https://" + rpId + "\") on the configurer.");
        }
        UserCredentialRepository keys = credentials != null ? credentials
                : http.getSharedObject(UserCredentialRepository.class);
        PublicKeyCredentialUserEntityRepository handles = userEntities != null ? userEntities
                : http.getSharedObject(PublicKeyCredentialUserEntityRepository.class);
        if ((keys == null || handles == null) && !http.getConfig().isDevelopmentProfile()) {
            System.err.println("cn1: webAuthn() was given no "
                    + (keys == null ? "UserCredentialRepository" : "")
                    + (keys == null && handles == null ? " and no " : "")
                    + (handles == null ? "PublicKeyCredentialUserEntityRepository" : "")
                    + ", so passkeys are kept in memory: they are gone when this server stops "
                    + "and unknown to any other. Declare the Jdbc one as a bean.");
        }
        WebAuthnRelyingPartyOperations made = new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity(rpId, rpName), allowedOrigins,
                handles != null ? handles : new InMemoryPublicKeyCredentialUserEntityRepository(),
                keys != null ? keys : new InMemoryUserCredentialRepository());
        if (userVerification != null) {
            made.setUserVerification(userVerification);
        }
        if (residentKey != null) {
            made.setResidentKey(residentKey);
        }
        if (authenticatorAttachmentSet) {
            made.setAuthenticatorAttachment(authenticatorAttachment);
        }
        made.setAllowUnverifiedAttestation(allowUnverifiedAttestation);
        made.setAllowCrossOrigin(allowCrossOrigin);
        made.setTimeoutMillis(challengeValiditySeconds * 1000L);
        made.setClock(clock);
        if (counterListener != null) {
            made.setSignatureCounterListener(counterListener);
        }
        return made;
    }

    @Override
    public void configure(HttpSecurity http) {
        long validity = challengeValiditySeconds * 1000L;
        AuthenticationSuccessHandler success = successHandler != null ? successHandler
                : new Answer(http, defaultSuccessUrl);
        AuthenticationFailureHandler failure = failureHandler != null ? failureHandler
                : new Refusal();
        http.addFilter(new WebAuthnAuthenticationFilter(resolved,
                "/webauthn/authenticate/options", "/login/webauthn", usernameFirst, validity,
                users, http.signIn(), success, failure, clock), HttpSecurity.ORDER_WEBAUTHN_LOGIN);
        http.addFilter(new WebAuthnRegistrationFilter(resolved, "/webauthn/register/options",
                "/webauthn/register", validity, clock), HttpSecurity.ORDER_WEBAUTHN_REGISTRATION);
    }

    /// The answer to a sign-in: where the user was going, for the client to
    /// go there.
    private static final class Answer implements AuthenticationSuccessHandler {
        private final HttpSecurity http;
        private final String defaultUrl;

        Answer(HttpSecurity http, String defaultUrl) {
            this.http = http;
            this.defaultUrl = defaultUrl;
        }

        @Override
        public HttpServer.Response onAuthenticationSuccess(HttpServer.Request request,
                                                           Authentication authentication) {
            // Asked for when it is first needed: at init() the chain has not
            // settled which request cache it has.
            RequestCache cache = http.resolveRequestCache();
            String saved = cache.getRequest(request);
            if (saved != null) {
                cache.removeRequest(request);
            }
            Map<String, Object> answer = new LinkedHashMap<String, Object>();
            answer.put("authenticated", Boolean.TRUE);
            answer.put("redirectUrl", saved != null ? saved : defaultUrl);
            return WebAuthnAuthenticationFilter.json(200, answer);
        }
    }

    /// The answer to a refused sign-in: that it was refused, and not why.
    private static final class Refusal implements AuthenticationFailureHandler {
        @Override
        public HttpServer.Response onAuthenticationFailure(HttpServer.Request request,
                                                           AuthenticationException exception) {
            Map<String, Object> answer = new LinkedHashMap<String, Object>();
            answer.put("authenticated", Boolean.FALSE);
            return WebAuthnAuthenticationFilter.json(401, answer);
        }
    }
}
