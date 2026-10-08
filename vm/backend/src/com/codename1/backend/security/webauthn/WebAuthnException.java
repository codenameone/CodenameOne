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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.security.AuthenticationException;

/// A passkey ceremony that was refused, with which check refused it.
///
/// [#getReason] is one of the constants here: a short name a test, a log line
/// or an alert can match. The message says the same for a person, and may name
/// what was received -- an origin, an attestation format -- so it is for the
/// server's own log and is not sent to whoever made the request.
public final class WebAuthnException extends AuthenticationException {
    /// The request is not the JSON a ceremony takes, or a field of it is
    /// missing or is not what it should be.
    public static final String MALFORMED = "malformed";
    /// A CBOR value could not be read: truncated, nested too deep, too large,
    /// or of a kind the ceremonies have no use for.
    public static final String MALFORMED_CBOR = "malformed_cbor";
    /// No ceremony was waiting in this session, or it had been used already.
    public static final String NO_CHALLENGE = "no_challenge";
    /// The ceremony was started too long ago.
    public static final String CHALLENGE_EXPIRED = "challenge_expired";
    /// The client signed another challenge than the one this session was given.
    public static final String CHALLENGE_MISMATCH = "challenge_mismatch";
    /// The client data is for the other ceremony: a registration where a
    /// sign-in was expected, or the reverse.
    public static final String WRONG_TYPE = "wrong_type";
    /// The ceremony ran on an origin the relying party does not allow.
    public static final String ORIGIN_MISMATCH = "origin_mismatch";
    /// The ceremony ran inside a frame of another origin, which is not allowed.
    public static final String CROSS_ORIGIN = "cross_origin";
    /// The authenticator answered for another relying party.
    public static final String RP_ID_MISMATCH = "rp_id_mismatch";
    /// The authenticator did not see the user: the user-present flag is clear.
    public static final String USER_NOT_PRESENT = "user_not_present";
    /// The ceremony required the user to be verified, and the flag is clear.
    public static final String USER_NOT_VERIFIED = "user_not_verified";
    /// The authenticator data is not what the specification lays out.
    public static final String MALFORMED_AUTHENTICATOR_DATA = "malformed_authenticator_data";
    /// The credential's key is of an algorithm this server does not verify.
    public static final String UNSUPPORTED_ALGORITHM = "unsupported_algorithm";
    /// The credential's key is not a key of the algorithm it names.
    public static final String INVALID_KEY = "invalid_key";
    /// The attestation is in a format this server does not verify.
    public static final String UNSUPPORTED_ATTESTATION = "unsupported_attestation";
    /// The attestation statement does not verify.
    public static final String ATTESTATION_INVALID = "attestation_invalid";
    /// A credential with this id is registered already, to this user or another.
    public static final String CREDENTIAL_EXISTS = "credential_exists";
    /// No credential with this id is registered.
    public static final String UNKNOWN_CREDENTIAL = "unknown_credential";
    /// The credential is not one the ceremony allowed.
    public static final String CREDENTIAL_NOT_ALLOWED = "credential_not_allowed";
    /// The credential belongs to another user than the one the ceremony is for,
    /// or the response names no user where it must.
    public static final String USER_MISMATCH = "user_mismatch";
    /// The signature is not the credential's signature of this ceremony.
    public static final String SIGNATURE_INVALID = "signature_invalid";
    /// The signature counter did not advance: the credential may have been
    /// copied.
    public static final String COUNTER_REGRESSION = "counter_regression";
    /// The backup flags contradict each other, or what was recorded when the
    /// credential was registered.
    public static final String BACKUP_STATE_INVALID = "backup_state_invalid";
    /// The user the credential belongs to cannot sign in here.
    public static final String ACCOUNT_UNAVAILABLE = "account_unavailable";

    private final String reason;

    public WebAuthnException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public WebAuthnException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /// Which check refused the ceremony: one of the constants of this class.
    public String getReason() {
        return reason;
    }
}
