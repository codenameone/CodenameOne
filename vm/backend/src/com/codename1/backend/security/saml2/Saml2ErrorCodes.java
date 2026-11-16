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

/// The reasons a SAML sign-in is refused, as the codes Spring Security uses.
public final class Saml2ErrorCodes {
    /// The response is not XML this server reads, or not a SAML response.
    public static final String MALFORMED_RESPONSE_DATA = "malformed_response_data";
    /// The response is addressed to another service.
    public static final String INVALID_DESTINATION = "invalid_destination";
    /// The response or assertion comes from another identity provider.
    public static final String INVALID_ISSUER = "invalid_issuer";
    /// The response answers another request than the one this browser sent.
    public static final String INVALID_IN_RESPONSE_TO = "invalid_in_response_to";
    /// Neither the response nor the assertion carries a signature that verifies.
    public static final String INVALID_SIGNATURE = "invalid_signature";
    /// The assertion is outside its validity, for another audience, or replayed.
    public static final String INVALID_ASSERTION = "invalid_assertion";
    /// The identity provider said the sign-in did not succeed.
    public static final String INVALID_RESPONSE = "invalid_response";
    /// The assertion names no subject.
    public static final String SUBJECT_NOT_FOUND = "subject_not_found";
    /// No sign-in was waiting, or no registration has this id.
    public static final String RELYING_PARTY_REGISTRATION_NOT_FOUND =
            "relying_party_registration_not_found";
    /// Something this server needed was missing or broken.
    public static final String INTERNAL_VALIDATION_ERROR = "internal_validation_error";
    /// An encrypted assertion that could not be decrypted, for whatever reason.
    public static final String DECRYPTION_ERROR = "decryption_error";

    private Saml2ErrorCodes() {
    }
}
