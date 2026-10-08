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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidatorResult;

/// Refuses a token whose `iss` is not exactly the issuer this server trusts.
/// A token that names no issuer is refused with the rest.
public final class JwtIssuerValidator implements OAuth2TokenValidator<Jwt> {
    private final String issuer;

    public JwtIssuerValidator(String issuer) {
        if (issuer == null || issuer.length() == 0) {
            throw new IllegalArgumentException("issuer cannot be empty");
        }
        this.issuer = issuer;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (issuer.equals(jwt.getIssuer())) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                "The iss claim is not valid", "https://tools.ietf.org/html/rfc6750#section-3.1"));
    }
}
