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

import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// What a token endpoint answered a successful exchange with.
public final class OAuth2AccessTokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final String refreshToken;
    private final Set<String> scopes;
    private final Map<String, Object> additionalParameters;

    /// @param fields the fields of the answer, by name
    public OAuth2AccessTokenResponse(Map fields) {
        this.accessToken = text(fields.get("access_token"));
        this.tokenType = text(fields.get("token_type"));
        Object expires = fields.get("expires_in");
        long seconds = 0;
        if (expires instanceof Number) {
            seconds = ((Number) expires).longValue();
        } else if (expires instanceof String) {
            try {
                seconds = Long.parseLong((String) expires);
            } catch (NumberFormatException notANumber) {
                seconds = 0;
            }
        }
        this.expiresIn = seconds;
        this.refreshToken = text(fields.get("refresh_token"));
        this.scopes = Collections.unmodifiableSet(OAuth2Parameters.scopes(
                text(fields.get("scope"))));
        Map<String, Object> rest = new LinkedHashMap<String, Object>();
        for (Object entry : fields.entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            String name = String.valueOf(e.getKey());
            if (!"access_token".equals(name) && !"token_type".equals(name)
                    && !"expires_in".equals(name) && !"refresh_token".equals(name)
                    && !"scope".equals(name)) {
                rest.put(name, e.getValue());
            }
        }
        this.additionalParameters = Collections.unmodifiableMap(rest);
    }

    private static String text(Object value) {
        return value instanceof String && ((String) value).length() > 0 ? (String) value : null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    /// The seconds the access token is good for, or 0 when the answer did not
    /// say.
    public long getExpiresIn() {
        return expiresIn;
    }

    /// The refresh token, or null.
    public String getRefreshToken() {
        return refreshToken;
    }

    /// The scopes granted, when the answer named them; empty otherwise, which
    /// means what was asked for.
    public Set<String> getScopes() {
        return scopes;
    }

    /// Everything else the answer carried: `id_token` among it.
    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    /// The ID token as it was sent, or null.
    public String getIdToken() {
        return text(additionalParameters.get("id_token"));
    }
}
