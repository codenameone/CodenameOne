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

/// The error codes of RFC 6749 and RFC 6750 this layer answers with.
public final class OAuth2ErrorCodes {
    /// The request is missing a parameter, repeats one, or is otherwise
    /// malformed.
    public static final String INVALID_REQUEST = "invalid_request";
    /// The access token is expired, revoked, malformed or otherwise not valid.
    public static final String INVALID_TOKEN = "invalid_token";
    /// The request needs more than the access token grants.
    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";
    /// The server met something that stopped it answering.
    public static final String SERVER_ERROR = "server_error";

    private OAuth2ErrorCodes() {
    }
}
