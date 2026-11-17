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
import com.codename1.backend.security.saml2.Saml2AuthenticatedPrincipal;
import com.codename1.backend.security.saml2.Saml2Authentication;
import com.codename1.backend.security.saml2.Saml2AuthenticationConverter;
import com.codename1.backend.security.saml2.Saml2AuthenticationException;
import com.codename1.backend.security.saml2.Saml2ErrorCodes;
import com.codename1.backend.security.saml2.Saml2ResponseValidator;

/// Ends a SAML sign-in: takes the identity provider's posted `SAMLResponse`
/// at `POST {acsBase}/{registrationId}`, holds it to the AuthnRequest this
/// browser was sent with (see [Saml2WebSsoAuthenticationRequestFilter]),
/// validates it with [Saml2ResponseValidator] and signs the user in.
public final class Saml2WebSsoAuthenticationFilter implements SecurityFilter {
    /// The largest `SAMLResponse` read, base64 encoded.
    static final int MAX_RESPONSE = 512 * 1024;

    private final String acsPrefix;
    private final RelyingPartyRegistrationRepository registrations;
    private final SignedTokens tokens;
    private final Saml2ResponseValidator validator;
    private final Saml2AuthenticationConverter converter;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final SessionSignIn signIn;

    Saml2WebSsoAuthenticationFilter(String acsPrefix,
            RelyingPartyRegistrationRepository registrations, SignedTokens tokens,
            Saml2ResponseValidator validator, Saml2AuthenticationConverter converter,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler, SessionSignIn signIn) {
        this.acsPrefix = acsPrefix;
        this.registrations = registrations;
        this.tokens = tokens;
        this.validator = validator;
        this.converter = converter;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.signIn = signIn;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        if (!"POST".equals(request.getMethod()) || !path.startsWith(acsPrefix)
                || path.indexOf('/', acsPrefix.length()) >= 0) {
            return chain.doFilter(request);
        }
        String encoded = Responses.param(request, "SAMLResponse");
        if (encoded == null) {
            return chain.doFilter(request);
        }
        Authentication authentication;
        try {
            authentication = authenticate(request, path.substring(acsPrefix.length()), encoded);
        } catch (Saml2AuthenticationException refused) {
            signIn.failure(request);
            return clear(failureHandler.onAuthenticationFailure(request, refused));
        }
        return clear(signIn.success(request, authentication, successHandler));
    }

    private static HttpServer.Response clear(HttpServer.Response response) {
        return response.header("Set-Cookie", Saml2WebSsoAuthenticationRequestFilter.COOKIE
                + "=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None");
    }

    private Authentication authenticate(HttpServer.Request request, String registrationId,
                                        String encoded) {
        RelyingPartyRegistration r = registrations.findByRegistrationId(registrationId);
        if (r == null) {
            throw new Saml2AuthenticationException(
                    Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                    "No registration is called " + registrationId);
        }
        if (encoded.length() > MAX_RESPONSE) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA,
                    "The response is too large");
        }
        byte[] xml = base64(encoded);
        if (xml == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA,
                    "The SAMLResponse is not base64");
        }
        String sent = null;
        String cookie = request.getCookie(Saml2WebSsoAuthenticationRequestFilter.COOKIE);
        if (cookie != null && cookie.length() > 0) {
            String subject = tokens.verify(Saml2WebSsoAuthenticationRequestFilter.PURPOSE, cookie);
            int space = subject == null ? -1 : subject.indexOf(' ');
            if (space > 0 && registrationId.equals(subject.substring(0, space))) {
                sent = subject.substring(space + 1);
            }
        }
        Saml2AuthenticatedPrincipal principal;
        try {
            principal = validator.validate(xml, r, Saml2Addresses.acs(request, r),
                    Saml2Addresses.entityId(request, r), sent, System.currentTimeMillis());
        } catch (IllegalArgumentException err) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INTERNAL_VALIDATION_ERROR,
                    err.getMessage(), err);
        }
        Saml2Authentication authentication = converter.convert(principal);
        if (authentication == null) {
            throw new Saml2AuthenticationException(Saml2ErrorCodes.INVALID_ASSERTION,
                    "The application refused " + principal.getName());
        }
        return authentication;
    }

    private static byte[] base64(String encoded) {
        StringBuilder b = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                b.append(c);
            }
        }
        return com.codename1.backend.Base64.decode(b.toString());
    }
}
