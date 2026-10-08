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

import com.codename1.backend.security.Clock;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2ErrorCodes;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidatorResult;

/// Refuses a token past its `exp` or ahead of its `nbf`, with a minute's
/// allowance either way for two machines whose clocks disagree.
///
/// A token with neither claim passes: whether a token must expire is for the
/// issuer to decide and for another validator to demand.
public final class JwtTimestampValidator implements OAuth2TokenValidator<Jwt> {
    private final long skewSeconds;
    private Clock clock = Clock.SYSTEM;

    /// With the allowance of 60 seconds.
    public JwtTimestampValidator() {
        this(60);
    }

    /// @param clockSkewSeconds how far past `exp` or ahead of `nbf` still passes
    public JwtTimestampValidator(long clockSkewSeconds) {
        if (clockSkewSeconds < 0) {
            throw new IllegalArgumentException("clockSkew cannot be negative");
        }
        this.skewSeconds = clockSkewSeconds;
    }

    /// Reads the time from `clock` instead of the machine.
    public void setClock(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock cannot be null");
        }
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        long now = clock.currentTimeMillis() / 1000L;
        Long expires = jwt.getExpiresAt();
        if (expires != null && now - skewSeconds > expires.longValue()) {
            return OAuth2TokenValidatorResult.failure(error("Jwt expired at " + expires));
        }
        Long notBefore = jwt.getNotBefore();
        if (notBefore != null && now + skewSeconds < notBefore.longValue()) {
            return OAuth2TokenValidatorResult.failure(error("Jwt used before " + notBefore));
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2Error error(String reason) {
        return new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, reason,
                "https://tools.ietf.org/html/rfc6750#section-3.1");
    }
}
