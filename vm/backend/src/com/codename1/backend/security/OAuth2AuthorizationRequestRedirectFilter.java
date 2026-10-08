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
import com.codename1.backend.security.oauth2.client.AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.OAuth2AuthorizationRequest;
import com.codename1.backend.security.oauth2.client.OAuth2AuthorizationRequestResolver;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import java.util.LinkedHashMap;
import java.util.Map;

/// Starts a sign-in at an identity provider: keeps the request this browser
/// is sent away with, and redirects it to the provider.
public final class OAuth2AuthorizationRequestRedirectFilter implements SecurityFilter {
    private final OAuth2AuthorizationRequestResolver resolver;
    private final AuthorizationRequestRepository session;
    private final AuthorizationRequestRepository posted;
    private final AuthenticationFailureHandler failureHandler;

    OAuth2AuthorizationRequestRedirectFilter(OAuth2AuthorizationRequestResolver resolver,
            AuthorizationRequestRepository session, AuthorizationRequestRepository posted,
            AuthenticationFailureHandler failureHandler) {
        this.resolver = resolver;
        this.session = session;
        this.posted = posted;
        this.failureHandler = failureHandler;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        OAuth2AuthorizationRequest start;
        try {
            start = resolver.resolve(request);
        } catch (RuntimeException err) {
            // The provider's metadata could not be read, or the registration
            // is unusable: the server's fault, and not the browser's to hear.
            System.err.println("cn1: a sign-in could not be started: " + err.getMessage());
            return failureHandler.onAuthenticationFailure(request,
                    new OAuth2AuthenticationException(new OAuth2Error(
                            OAuth2ErrorCodes.SERVER_ERROR), "The sign-in could not be started"));
        }
        if (start == null) {
            return chain.doFilter(request);
        }
        // A posted answer arrives without the session's cookie; see
        // CookieOAuth2AuthorizationRequestRepository.
        boolean formPost = ClientRegistration.FORM_POST.equals(
                start.getAdditionalParameters().get("response_mode"));
        (formPost ? posted : session).saveAuthorizationRequest(start, request);
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("Location", start.getAuthorizationRequestUri());
        headers.put("Cache-Control", "no-store");
        return new HttpServer.Response(302, "text/plain; charset=utf-8", new byte[0], headers);
    }
}
