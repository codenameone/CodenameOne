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
package com.codename1.backend.security.oauth2.client.credentials;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/// An access token this server obtained to call another one.
///
/// The value is a credential: [#toString()] never prints it, so a token that
/// ends up in a log line or an exception message names its type, scopes and
/// expiry and nothing anyone could replay.
public final class AccessToken {
    private final String tokenValue;
    private final String tokenType;
    private final long expiresAtMillis;
    private final Set<String> scopes;

    /// @param tokenValue the token, as the endpoint issued it
    /// @param tokenType the type the endpoint named, normally `Bearer`
    /// @param expiresAtMillis when it stops being accepted, in epoch milliseconds
    /// @param scopes what it was issued for; empty when the endpoint did not say
    public AccessToken(String tokenValue, String tokenType, long expiresAtMillis,
                       Set<String> scopes) {
        if (tokenValue == null || tokenValue.length() == 0) {
            throw new IllegalArgumentException("An access token has a value");
        }
        this.tokenValue = tokenValue;
        this.tokenType = tokenType == null ? "Bearer" : tokenType;
        this.expiresAtMillis = expiresAtMillis;
        this.scopes = scopes == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(scopes));
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    /// True once `nowMillis` has reached the expiry.
    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    /// The value of an `Authorization` header that presents this token.
    public String toAuthorizationHeader() {
        return tokenType + " " + tokenValue;
    }

    @Override
    public String toString() {
        return "AccessToken[type=" + tokenType + ", scopes=" + scopes + ", expiresAt="
                + expiresAtMillis + ", value=<redacted>]";
    }
}
