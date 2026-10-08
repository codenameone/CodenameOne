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
import java.util.ArrayList;
import java.util.List;

/// Refuses a token that is not for this server: one whose `aud` names none of
/// the audiences given here. A token with no audience at all is refused.
///
/// Without this a token an identity provider issued for some other application
/// is as good here as one issued for this one.
public final class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final List<String> audiences = new ArrayList<String>();

    /// @param audiences what this server is called in a token's `aud`; any one
    /// of them will do
    public JwtAudienceValidator(String... audiences) {
        if (audiences == null || audiences.length == 0) {
            throw new IllegalArgumentException("At least one audience is required");
        }
        for (String audience : audiences) {
            if (audience == null || audience.length() == 0) {
                throw new IllegalArgumentException("An audience cannot be empty");
            }
            this.audiences.add(audience);
        }
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        for (String audience : jwt.getAudience()) {
            if (audiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
                "The aud claim is not valid", "https://tools.ietf.org/html/rfc6750#section-3.1"));
    }
}
