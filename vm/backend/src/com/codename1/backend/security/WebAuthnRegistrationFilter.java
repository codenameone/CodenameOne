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
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.WebAuthnException;
import com.codename1.backend.security.webauthn.WebAuthnRelyingPartyOperations;
import java.util.LinkedHashMap;
import java.util.Map;

/// Lets a signed-in user register a passkey, and remove one of theirs:
///
/// - `POST /webauthn/register/options` answers the options to make one with;
/// - `POST /webauthn/register` takes the authenticator's answer and stores
///   the credential;
/// - `DELETE /webauthn/register/{credentialId}` removes a credential, the id
///   in base64url.
///
/// All three are for a user who signed in during this session. Somebody a
/// remember-me cookie brought back is sent to sign in first: a stolen cookie
/// must not be able to leave a passkey of the thief's behind. The filter runs
/// after the chain's authorization rules, which say who may reach it at all.
///
/// The options are kept in the session, for five minutes unless set
/// otherwise, and taken out of it before the answer is looked at.
public final class WebAuthnRegistrationFilter implements SecurityFilter {
    /// The session attribute a registration's options wait under.
    public static final String PENDING = "CN1_WEBAUTHN_REGISTRATION";

    private final WebAuthnRelyingPartyOperations operations;
    private final String optionsUrl;
    private final String registerUrl;
    private final long validityMillis;
    private final Clock clock;

    WebAuthnRegistrationFilter(WebAuthnRelyingPartyOperations operations, String optionsUrl,
                               String registerUrl, long validityMillis, Clock clock) {
        this.operations = operations;
        this.optionsUrl = optionsUrl;
        this.registerUrl = registerUrl;
        this.validityMillis = validityMillis;
        this.clock = clock;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String method = request.getMethod();
        String path = SecurityExchange.path(request);
        if ("POST".equals(method) && optionsUrl.equals(path)) {
            PublicKeyCredentialCreationOptions options =
                    operations.createPublicKeyCredentialCreationOptions(user());
            WebAuthnAuthenticationFilter.keep(request, PENDING, options.toMap(),
                    clock.currentTimeMillis() + validityMillis);
            return WebAuthnAuthenticationFilter.json(200, options.toMap());
        }
        if ("POST".equals(method) && registerUrl.equals(path)) {
            return register(request, user());
        }
        if ("DELETE".equals(method) && path.startsWith(registerUrl + "/")
                && path.indexOf('/', registerUrl.length() + 1) < 0) {
            return delete(user(), path.substring(registerUrl.length() + 1));
        }
        return chain.doFilter(request);
    }

    /// The name of the user this request is from, who must have signed in
    /// during this session. Anybody else is refused the way the rules refuse:
    /// the chain then asks them to sign in.
    private static String user() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || !who.isAuthenticated() || who instanceof AnonymousAuthenticationToken
                || who instanceof RememberMeAuthenticationToken || who.getName() == null
                || who.getName().length() == 0) {
            throw new AccessDeniedException("Managing passkeys takes a sign-in made in this "
                    + "session");
        }
        return who.getName();
    }

    private HttpServer.Response register(HttpServer.Request request, String username) {
        Map<String, Object> answer = new LinkedHashMap<String, Object>();
        try {
            Map pending = WebAuthnAuthenticationFilter.take(request, PENDING, clock);
            PublicKeyCredentialCreationOptions options =
                    PublicKeyCredentialCreationOptions.fromMap(pending);
            // Options made for whoever this session was then are not this
            // user's to answer: they carry that user's handle.
            PublicKeyCredentialUserEntity user = operations.getUserEntities()
                    .findByUsername(username);
            if (options == null || user == null || !Base64Url.encode(user.getId()).equals(
                    Base64Url.encode(options.getUser().getId()))) {
                throw new WebAuthnException(WebAuthnException.NO_CHALLENGE,
                        "The registration that waited in this session was started by another "
                        + "user");
            }
            Map body = WebAuthnAuthenticationFilter.body(request);
            if (body == null) {
                throw new WebAuthnException(WebAuthnException.MALFORMED,
                        "The request body is not the JSON of a credential");
            }
            // Either the credential itself, as a client's toJSON() gives it, or
            // Spring Security's envelope: {"publicKey": {"credential": ..., "label": ...}}.
            Map credential = body;
            Object label = body.get("label");
            Object envelope = body.get("publicKey");
            if (envelope instanceof Map) {
                Object inner = ((Map) envelope).get("credential");
                if (!(inner instanceof Map)) {
                    throw new WebAuthnException(WebAuthnException.MALFORMED,
                            "The publicKey of the request has no credential");
                }
                credential = (Map) inner;
                label = ((Map) envelope).get("label");
            }
            if (!(label instanceof String)) {
                label = request.queryParam("label");
            }
            CredentialRecord record = operations.registerCredential(options, credential,
                    label instanceof String ? (String) label : null);
            answer.put("success", Boolean.TRUE);
            answer.put("credentialId", Base64Url.encode(record.getCredentialId()));
            return WebAuthnAuthenticationFilter.json(200, answer);
        } catch (WebAuthnException refused) {
            // The user is signed in and it is their own registration: they are
            // told which check refused it. The message, which may quote what
            // was sent, stays in the exception.
            answer.put("success", Boolean.FALSE);
            answer.put("error", refused.getReason());
            return WebAuthnAuthenticationFilter.json(400, answer);
        }
    }

    private HttpServer.Response delete(String username, String encoded) {
        byte[] credentialId = Base64Url.decode(encoded);
        PublicKeyCredentialUserEntity user = operations.getUserEntities().findByUsername(username);
        CredentialRecord record = credentialId == null || credentialId.length == 0 || user == null
                ? null : operations.getUserCredentials().findByCredentialId(credentialId);
        // Somebody else's credential is, to this user, no credential: the
        // answer does not say that it exists.
        if (record == null || !Base64Url.encode(user.getId()).equals(
                Base64Url.encode(record.getUserEntityUserId()))) {
            return Responses.status(404, "Not Found");
        }
        operations.getUserCredentials().delete(credentialId);
        return new HttpServer.Response(204, "text/plain; charset=utf-8", new byte[0]);
    }
}
