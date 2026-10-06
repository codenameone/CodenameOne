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
package com.codename1.backend.security.oauth2.core;

/// How a client obtains a token: the `grant_type` of RFC 6749 and RFC 8628.
public final class AuthorizationGrantType {
    public static final AuthorizationGrantType AUTHORIZATION_CODE =
            new AuthorizationGrantType("authorization_code");
    public static final AuthorizationGrantType REFRESH_TOKEN =
            new AuthorizationGrantType("refresh_token");
    public static final AuthorizationGrantType CLIENT_CREDENTIALS =
            new AuthorizationGrantType("client_credentials");
    public static final AuthorizationGrantType DEVICE_CODE =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:device_code");

    private final String value;

    public AuthorizationGrantType(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("value cannot be empty");
        }
        this.value = value;
    }

    /// The value as it is written in a request.
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AuthorizationGrantType
                && value.equals(((AuthorizationGrantType) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
