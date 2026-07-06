/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.webauthn;

import java.io.IOException;

/// Thrown for failures during a WebAuthn / passkey ceremony driven by
/// [WebAuthnClient]. The [#getError()] code mirrors the W3C exception names
/// returned by the platform authenticator (`"NotAllowedError"`,
/// `"InvalidStateError"`, `"NotSupportedError"`, `"SecurityError"`,
/// `"AbortError"`, `"ConstraintError"`) plus Codename One-specific values
/// for transport (`"transport_error"`), parsing
/// (`"invalid_options"`, `"invalid_response"`) and the platform not
/// implementing public-key credentials at all (`"not_supported"`).
///
public class WebAuthnException extends IOException {

    /// User dismissed the OS passkey sheet, or the OS denied the request
    /// because the authenticator was unavailable or no credential matched.
    public static final String NOT_ALLOWED = "NotAllowedError";

    /// The credential being created already exists on the authenticator
    /// (mapped from the W3C `InvalidStateError` for `create()`), or the
    /// requested credential is no longer valid.
    public static final String INVALID_STATE = "InvalidStateError";

    /// The platform does not support the requested public-key algorithm /
    /// transport / RP combination.
    public static final String NOT_SUPPORTED = "NotSupportedError";

    /// Origin / RP-ID validation failed. Most often this means the app's
    /// associated domain (iOS) or asset link (Android) is not configured
    /// for the relying-party identifier in the options JSON.
    public static final String SECURITY_ERROR = "SecurityError";

    /// The ceremony was cancelled by the caller (e.g. via
    /// [WebAuthnClient#cancel()]).
    public static final String ABORTED = "AbortError";

    /// One of the option constraints (resident key required, user verification
    /// required, etc.) could not be satisfied by the available authenticators.
    public static final String CONSTRAINT_ERROR = "ConstraintError";

    /// The platform lacks a public-key credential implementation. iOS &lt; 16,
    /// Android API &lt; 28, JavaSE / desktop, web fallback without a native
    /// WebAuthn implementation.
    public static final String NOT_IMPLEMENTED = "not_supported";

    /// Generic transport / network failure (e.g. while POSTing the registration
    /// or assertion response to your relying-party server).
    public static final String TRANSPORT_ERROR = "transport_error";

    /// The options JSON received from the relying party could not be parsed.
    public static final String INVALID_OPTIONS = "invalid_options";

    /// The response JSON returned by the authenticator could not be parsed
    /// back into a [PublicKeyCredential].
    public static final String INVALID_RESPONSE = "invalid_response";

    private final String error;
    private final String errorDescription;

    public WebAuthnException(String error, String message) {
        super(message != null ? message : error);
        this.error = error;
        this.errorDescription = message;
    }

    public WebAuthnException(String error, String message, Throwable cause) {
        super(message != null ? message : error);
        this.error = error;
        this.errorDescription = message;
        if (cause != null) {
            initCause(cause);
        }
    }

    /// The short error code (see constants on this class).
    public String getError() {
        return error;
    }

    /// Human-readable description supplied by the platform or the client.
    public String getErrorDescription() {
        return errorDescription;
    }
}
