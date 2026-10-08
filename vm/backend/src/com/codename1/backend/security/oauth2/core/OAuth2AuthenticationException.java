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

import com.codename1.backend.security.AuthenticationException;

/// An authentication that failed for a reason OAuth 2.0 has a code for.
public class OAuth2AuthenticationException extends AuthenticationException {
    private final OAuth2Error error;

    public OAuth2AuthenticationException(OAuth2Error error) {
        this(error, error.getDescription());
    }

    public OAuth2AuthenticationException(OAuth2Error error, String message) {
        super(message == null ? error.getErrorCode() : message);
        this.error = error;
    }

    public OAuth2AuthenticationException(OAuth2Error error, String message, Throwable cause) {
        super(message == null ? error.getErrorCode() : message, cause);
        this.error = error;
    }

    /// The error, with its code.
    public OAuth2Error getError() {
        return error;
    }
}
