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
import java.util.function.Predicate;

/// Refuses a token unless one of its claims passes a test.
///
/// ```java
/// // An ID token must carry the nonce this server sent with the request.
/// OAuth2TokenValidator<Jwt> nonce = new JwtClaimValidator("nonce", expected::equals);
/// ```
///
/// The test is handed the claim as the JSON had it -- a String, a Long or
/// Double, a Boolean, a List or a Map -- and null when the token has none of
/// that name. It is handed an Object and not something narrower on purpose:
/// what type a claim has is the issuer's choice, and a test that assumed one
/// would be casting a value it has not looked at. Ask with `instanceof`.
public final class JwtClaimValidator implements OAuth2TokenValidator<Jwt> {
    private final String claim;
    private final Predicate<Object> test;

    public JwtClaimValidator(String claim, Predicate<Object> test) {
        if (claim == null || test == null) {
            throw new IllegalArgumentException("A claim and a test are required");
        }
        this.claim = claim;
        this.test = test;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (test.test(jwt.getClaim(claim))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                "The " + claim + " claim is not valid",
                "https://tools.ietf.org/html/rfc6750#section-3.1"));
    }
}
