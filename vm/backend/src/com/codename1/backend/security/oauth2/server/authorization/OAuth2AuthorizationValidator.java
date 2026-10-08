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

import com.codename1.backend.security.Clock;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidatorResult;
import com.codename1.backend.security.oauth2.jwt.Jwt;

/// Checks a signed access token against its live authorization grant, including
/// client-credentials grants. Removing the grant makes subsequent checks fail.
/// Install alongside timestamp and audience validators on a JWT decoder that
/// verifies the issuer's signature. The resource server must share the issuer's
/// authorization store; signature-only verification cannot observe revocation.
public final class OAuth2AuthorizationValidator implements OAuth2TokenValidator<Jwt> {
    private final OAuth2AuthorizationService authorizations;
    private final String issuer;
    private Clock clock = Clock.SYSTEM;

    public OAuth2AuthorizationValidator(OAuth2AuthorizationService authorizations, String issuer) {
        if (authorizations == null || issuer == null || issuer.length() == 0) {
            throw new IllegalArgumentException("An authorization store and issuer are required");
        }
        this.authorizations = authorizations;
        this.issuer = issuer;
    }

    /// The clock used to check the grant's expiry.
    public void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String id = jwt.getId();
        int dot = id == null ? -1 : id.indexOf('.');
        OAuth2Authorization grant = !issuer.equals(jwt.getIssuer()) || dot <= 0 ? null
                : authorizations.findById(id.substring(0, dot));
        if (grant != null && OAuth2Authorization.ACTIVE.equals(grant.getStatus())
                && grant.getExpiresAt() > clock.currentTimeMillis()
                && grant.getPrincipalName().equals(jwt.getSubject())) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                "The access token's authorization is not active", null));
    }
}
