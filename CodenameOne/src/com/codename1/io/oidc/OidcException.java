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
package com.codename1.io.oidc;

import java.io.IOException;

/// Thrown for failures during an OpenID Connect / OAuth 2.0 flow driven by
/// [OidcClient]. The [#getError()] code mirrors the `error` field from RFC 6749
/// for authorization-server responses (e.g. `"access_denied"`, `"invalid_grant"`)
/// and uses Codename One-specific values for transport or client-side problems
/// (`"transport_error"`, `"state_mismatch"`, `"nonce_mismatch"`, `"user_cancelled"`,
/// `"discovery_failed"`, `"invalid_id_token"`, `"issuer_mismatch"`,
/// `"storage_unavailable"`).
///
public class OidcException extends IOException {

    /// Authorization server returned `error=access_denied`.
    public static final String ACCESS_DENIED = "access_denied";

    /// User cancelled the system browser / native sign-in sheet.
    public static final String USER_CANCELLED = "user_cancelled";

    /// `state` returned by the authorization server did not match the one we sent.
    public static final String STATE_MISMATCH = "state_mismatch";

    /// `nonce` claim on the returned ID token did not match the one we sent.
    public static final String NONCE_MISMATCH = "nonce_mismatch";

    /// The discovery document could not be fetched or parsed.
    public static final String DISCOVERY_FAILED = "discovery_failed";

    /// Token-endpoint response was missing or malformed.
    public static final String INVALID_GRANT = "invalid_grant";

    /// The ID token was not accepted: it is malformed, it is for another client or from
    /// another issuer, it has expired, it does not belong to the access token it came
    /// with, or its signature does not verify against the provider's keys -- which
    /// includes a platform that cannot check a signature of that kind. See
    /// [OidcClient#setVerifyIdTokenSignature(boolean)].
    public static final String INVALID_ID_TOKEN = "invalid_id_token";

    /// The authorization response names another issuer than the provider the request was
    /// sent to, or none where the provider says it always names one (RFC 9207).
    public static final String ISSUER_MISMATCH = "issuer_mismatch";

    /// Generic transport / network failure.
    public static final String TRANSPORT_ERROR = "transport_error";

    /// A [TokenStore] could not read, write or remove the tokens -- the platform has no
    /// secure storage, or the store failed.
    public static final String STORAGE_UNAVAILABLE = "storage_unavailable";

    /// Device grant: the user has not finished approving the device yet. Polling continues.
    public static final String AUTHORIZATION_PENDING = "authorization_pending";

    /// Device grant: the device asked too often; it must wait longer between requests.
    public static final String SLOW_DOWN = "slow_down";

    /// Device grant: the device code ran out before the user approved it.
    public static final String EXPIRED_TOKEN = "expired_token";

    private final String error;
    private final String errorDescription;

    public OidcException(String error, String message) {
        super(message != null ? message : error);
        this.error = error;
        this.errorDescription = message;
    }

    public OidcException(String error, String message, Throwable cause) {
        super(message != null ? message : error);
        this.error = error;
        this.errorDescription = message;
        if (cause != null) {
            initCause(cause);
        }
    }

    /// The short error code (see the constants on this class).
    public String getError() {
        return error;
    }

    /// Human-readable description supplied by the server or the client.
    public String getErrorDescription() {
        return errorDescription;
    }
}
