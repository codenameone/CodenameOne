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

/// How a client proves which client it is at the token endpoint.
public final class ClientAuthenticationMethod {
    /// The id and secret in an `Authorization: Basic` header.
    public static final ClientAuthenticationMethod CLIENT_SECRET_BASIC =
            new ClientAuthenticationMethod("client_secret_basic");
    /// The id and secret as the form fields `client_id` and `client_secret`.
    public static final ClientAuthenticationMethod CLIENT_SECRET_POST =
            new ClientAuthenticationMethod("client_secret_post");
    /// No secret: a public client, which proves itself with PKCE.
    public static final ClientAuthenticationMethod NONE = new ClientAuthenticationMethod("none");

    private final String value;

    public ClientAuthenticationMethod(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("value cannot be empty");
        }
        this.value = value;
    }

    /// The value as metadata and settings write it.
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClientAuthenticationMethod
                && value.equals(((ClientAuthenticationMethod) other).value);
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
