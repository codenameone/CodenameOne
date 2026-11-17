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
package com.codename1.backend.security.saml2;

import com.codename1.backend.security.AuthenticationException;

/// A SAML sign-in that was refused. [#getErrorCode] is one of
/// [Saml2ErrorCodes]; the message says what was wrong, for the log, and is
/// never shown to the browser.
public class Saml2AuthenticationException extends AuthenticationException {
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public Saml2AuthenticationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public Saml2AuthenticationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /// One of [Saml2ErrorCodes].
    public String getErrorCode() {
        return errorCode;
    }

    static Saml2AuthenticationException malformed(String message, Throwable cause) {
        return new Saml2AuthenticationException(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA, message,
                cause);
    }

    static Saml2AuthenticationException malformed(String message) {
        return new Saml2AuthenticationException(Saml2ErrorCodes.MALFORMED_RESPONSE_DATA, message);
    }
}
