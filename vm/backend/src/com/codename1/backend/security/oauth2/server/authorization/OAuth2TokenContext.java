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

import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import java.util.Set;

/// One token about to be signed, as an [OAuth2TokenCustomizer] sees it.
public final class OAuth2TokenContext {
    /// [#getTokenType] of an access token.
    public static final String ACCESS_TOKEN = "access_token";
    /// [#getTokenType] of an ID token.
    public static final String ID_TOKEN = "id_token";

    private final String tokenType;
    private final RegisteredClient registeredClient;
    private final OAuth2Authorization authorization;
    private final String principalName;
    private final String authorizationGrantType;
    private final Set<String> authorizedScopes;
    private final JwtClaimsSet.Builder claims;

    OAuth2TokenContext(String tokenType, RegisteredClient registeredClient,
                       OAuth2Authorization authorization, String principalName,
                       String authorizationGrantType, Set<String> authorizedScopes,
                       JwtClaimsSet.Builder claims) {
        this.tokenType = tokenType;
        this.registeredClient = registeredClient;
        this.authorization = authorization;
        this.principalName = principalName;
        this.authorizationGrantType = authorizationGrantType;
        this.authorizedScopes = authorizedScopes;
        this.claims = claims;
    }

    /// [#ACCESS_TOKEN] or [#ID_TOKEN].
    public String getTokenType() {
        return tokenType;
    }

    public RegisteredClient getRegisteredClient() {
        return registeredClient;
    }

    /// The grant the token is issued under; null for `client_credentials`,
    /// which keeps none. Its attribute `authorities` lists what the user had
    /// been granted when they approved.
    public OAuth2Authorization getAuthorization() {
        return authorization;
    }

    /// The user, or for `client_credentials` the client's id.
    public String getPrincipalName() {
        return principalName;
    }

    /// The `grant_type` of the request the token answers.
    public String getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    public Set<String> getAuthorizedScopes() {
        return authorizedScopes;
    }

    /// The claims so far, to add to or change.
    public JwtClaimsSet.Builder getClaims() {
        return claims;
    }
}
