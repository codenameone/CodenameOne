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
import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.saml2.RelyingPartyRegistration;
import com.codename1.backend.security.saml2.RelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.Saml2AuthenticationRequest;
import com.codename1.backend.security.saml2.Saml2Metadata;
import java.io.IOException;

/// Starts a SAML sign-in at `GET {authenticateBase}/{registrationId}`: makes an
/// AuthnRequest, remembers its ID in a signed cookie, and sends the browser to
/// the identity provider. Also serves this server's metadata at
/// `GET {metadataBase}/{registrationId}`.
///
/// The request's ID is kept in a cookie rather than the session because the
/// identity provider's answer is a cross-site POST: a `SameSite=Lax` session
/// cookie does not come with it, and the response could not be held to the
/// request. The cookie is `__Host-`, signed, `SameSite=None` and short lived.
public final class Saml2WebSsoAuthenticationRequestFilter implements SecurityFilter {
    static final String COOKIE = "__Host-cn1_saml2_authn_request";
    static final String PURPOSE = "saml2-authn-request";
    static final long SECONDS = 600;

    private final String authenticatePrefix;
    private final String metadataPrefix;
    private final RelyingPartyRegistrationRepository registrations;
    private final SignedTokens tokens;
    private final AuthenticationFailureHandler failureHandler;
    private final boolean singleLogout;

    Saml2WebSsoAuthenticationRequestFilter(String authenticatePrefix, String metadataPrefix,
            RelyingPartyRegistrationRepository registrations,
            SignedTokens tokens, AuthenticationFailureHandler failureHandler,
            boolean singleLogout) {
        this.singleLogout = singleLogout;
        this.authenticatePrefix = authenticatePrefix;
        this.metadataPrefix = metadataPrefix;
        this.registrations = registrations;
        this.tokens = tokens;
        this.failureHandler = failureHandler;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        if (!"GET".equals(request.getMethod())) {
            return chain.doFilter(request);
        }
        String path = SecurityExchange.path(request);
        if (path.startsWith(metadataPrefix) && path.indexOf('/', metadataPrefix.length()) < 0) {
            RelyingPartyRegistration r = registrations.findByRegistrationId(
                    path.substring(metadataPrefix.length()));
            if (r == null) {
                return chain.doFilter(request);
            }
            String xml = Saml2Metadata.forServiceProvider(r,
                    Saml2Addresses.entityId(request, r),
                    Saml2Addresses.acs(request, r),
                    singleLogout && r.getSigningKey() != null ? Saml2Addresses.slo(request, r)
                            : null);
            return new HttpServer.Response(200, "application/samlmetadata+xml",
                    Responses.utf8(xml));
        }
        if (!path.startsWith(authenticatePrefix)
                || path.indexOf('/', authenticatePrefix.length()) >= 0) {
            return chain.doFilter(request);
        }
        String id = path.substring(authenticatePrefix.length());
        RelyingPartyRegistration r = registrations.findByRegistrationId(id);
        if (r == null) {
            return chain.doFilter(request);
        }
        Saml2AuthenticationRequest authn;
        try {
            authn = Saml2AuthenticationRequest.create(r, Saml2Addresses.acs(request, r),
                    Saml2Addresses.entityId(request, r), null,
                    System.currentTimeMillis());
        } catch (IOException err) {
            return failureHandler.onAuthenticationFailure(request,
                    new com.codename1.backend.security.saml2.Saml2AuthenticationException(
                            com.codename1.backend.security.saml2.Saml2ErrorCodes
                                    .INTERNAL_VALIDATION_ERROR,
                            "The AuthnRequest could not be made: " + err.getMessage(), err));
        } catch (IllegalArgumentException err) {
            return failureHandler.onAuthenticationFailure(request,
                    new com.codename1.backend.security.saml2.Saml2AuthenticationException(
                            com.codename1.backend.security.saml2.Saml2ErrorCodes
                                    .INTERNAL_VALIDATION_ERROR, err.getMessage(), err));
        }
        String cookie = tokens.create(PURPOSE, id + " " + authn.getId(), SECONDS);
        HttpServer.Response out = RelyingPartyRegistration.POST.equals(authn.getBinding())
                ? Responses.html(200, authn.getPostPage())
                : Responses.redirect(authn.getRedirectUrl());
        return out.header("Set-Cookie", COOKIE + "=" + cookie + "; Path=/; Max-Age=" + SECONDS
                + "; Secure; HttpOnly; SameSite=None")
                .header("Cache-Control", "no-store");
    }
}
