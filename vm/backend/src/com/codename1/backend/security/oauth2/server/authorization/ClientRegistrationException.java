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

/// A registration refused by a [ClientRegistrationPolicy], answered 400 with
/// the RFC 7591 error it names.
public class ClientRegistrationException extends RuntimeException {
    /// RFC 7591 3.2.2: a redirect address is not acceptable.
    public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";
    /// RFC 7591 3.2.2: another piece of the metadata is not acceptable.
    public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";

    private final String error;

    /// @param error the RFC 7591 error code, such as
    /// [#INVALID_CLIENT_METADATA]
    /// @param description what is wrong, shown to the client's developer
    public ClientRegistrationException(String error, String description) {
        super(description);
        this.error = error == null ? INVALID_CLIENT_METADATA : error;
    }

    /// The RFC 7591 error code.
    public String getError() {
        return error;
    }
}
