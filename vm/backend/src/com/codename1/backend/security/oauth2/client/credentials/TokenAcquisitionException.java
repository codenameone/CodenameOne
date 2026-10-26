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

/// A token endpoint did not issue a token.
///
/// The message names the endpoint, the HTTP status and the OAuth `error` code,
/// and nothing else. The request carried a client secret or a signed assertion
/// and the response may carry a token, so neither body is ever copied into it:
/// this exception is written to logs and error pages by code that has no idea
/// what it holds.
public class TokenAcquisitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int status;
    private final String errorCode;
    private final boolean retryable;

    /// @param message what failed, with no secret in it
    /// @param status the HTTP status, or -1 when the endpoint was not reached
    /// @param errorCode the OAuth error code the endpoint gave, or null
    /// @param retryable whether trying again later can succeed
    /// @param cause the failure underneath, or null
    public TokenAcquisitionException(String message, int status, String errorCode,
                                     boolean retryable, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /// The HTTP status, or -1 when the endpoint was not reached.
    public int getStatus() {
        return status;
    }

    /// The OAuth `error` code, such as `invalid_grant`, or null.
    public String getErrorCode() {
        return errorCode;
    }

    /// True for a failure that can go away by itself: the endpoint could not be
    /// reached, answered 5xx, or asked to slow down with 429. A refused
    /// credential (`invalid_client`, `invalid_grant`) is not.
    public boolean isRetryable() {
        return retryable;
    }
}
