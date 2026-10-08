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

import com.codename1.backend.security.oauth2.core.DelegatingOAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import java.util.ArrayList;
import java.util.List;

/// The validators a decoder is usually given.
///
/// ```java
/// decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
///         new JwtIssuerValidator(issuer), new JwtAudienceValidator("orders-api")));
/// ```
public final class JwtValidators {
    private JwtValidators() {
    }

    /// `exp` and `nbf`, with a minute's allowance; see [JwtTimestampValidator].
    public static OAuth2TokenValidator<Jwt> createDefault() {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<OAuth2TokenValidator<Jwt>>();
        validators.add(new JwtTimestampValidator());
        return new DelegatingOAuth2TokenValidator<Jwt>(validators);
    }

    /// [#createDefault], and the token's `iss` must be `issuer`.
    public static OAuth2TokenValidator<Jwt> createDefaultWithIssuer(String issuer) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<OAuth2TokenValidator<Jwt>>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator(issuer));
        return new DelegatingOAuth2TokenValidator<Jwt>(validators);
    }

    /// [#createDefault] and these as well. Giving a [JwtTimestampValidator]
    /// of one's own replaces the default one.
    @SafeVarargs
    public static OAuth2TokenValidator<Jwt> createDefaultWithValidators(
            OAuth2TokenValidator<Jwt>... more) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<OAuth2TokenValidator<Jwt>>();
        boolean timestamp = false;
        for (OAuth2TokenValidator<Jwt> validator : more) {
            timestamp |= validator instanceof JwtTimestampValidator;
        }
        if (!timestamp) {
            validators.add(new JwtTimestampValidator());
        }
        for (OAuth2TokenValidator<Jwt> validator : more) {
            validators.add(validator);
        }
        return new DelegatingOAuth2TokenValidator<Jwt>(validators);
    }
}
