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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.oauth2.jwt.Jwt;
import java.util.Collection;

/// An [OAuth2UserRequest] for a provider that also issued an ID token, which
/// has been verified by the time a service sees it.
public class OidcUserRequest extends OAuth2UserRequest {
    private final Jwt idToken;

    public OidcUserRequest(ClientRegistration clientRegistration,
                           OAuth2AccessTokenResponse tokenResponse, Jwt idToken) {
        this(clientRegistration, tokenResponse, idToken, clientRegistration.getScopes());
    }

    /// Includes the scopes actually sent, after authorization request customization.
    public OidcUserRequest(ClientRegistration clientRegistration,
                           OAuth2AccessTokenResponse tokenResponse, Jwt idToken,
                           Collection<String> requestedScopes) {
        super(clientRegistration, tokenResponse, requestedScopes);
        this.idToken = idToken;
    }

    /// The verified ID token.
    public Jwt getIdToken() {
        return idToken;
    }
}
