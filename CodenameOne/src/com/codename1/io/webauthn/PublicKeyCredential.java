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

import com.codename1.io.JSONParser;
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// The authenticator's response to a passkey ceremony -- either a registration
/// (`type=public-key`, `response.attestationObject` present) or an assertion
/// (`response.signature` + `response.authenticatorData` present).
///
/// Immutable. The most common usage is to call [#toJson()] and POST the result
/// to your relying-party server, which then runs full signature / origin /
/// counter verification using a server-side library. Do not try to verify the
/// attestation or assertion on the device -- that is the relying party's
/// responsibility.
///
/// @since 7.0.246
public final class PublicKeyCredential {

    /// Credential type -- always `"public-key"` for WebAuthn.
    public static final String TYPE_PUBLIC_KEY = "public-key";

    private final String json;
    private final Map<String, Object> parsed;
    private final boolean registration;

    private PublicKeyCredential(String json, Map<String, Object> parsed, boolean registration) {
        this.json = json;
        this.parsed = parsed == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(parsed));
        this.registration = registration;
    }

    /// Parses a RegistrationResponseJSON / AuthenticationResponseJSON document
    /// returned by the native authenticator.
    public static PublicKeyCredential fromJson(String json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        Map<String, Object> parsed;
        try {
            parsed = new JSONParser().parseJSON(new StringReader(json));
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Invalid response JSON: " + ioe.getMessage(), ioe);
        }
        if (parsed == null || parsed.isEmpty()) {
            throw new IllegalArgumentException("Response JSON is empty or unparseable");
        }
        Object response = parsed.get("response");
        boolean registration = false;
        if (response instanceof Map) {
            registration = ((Map<?, ?>) response).get("attestationObject") != null;
        }
        return new PublicKeyCredential(json, parsed, registration);
    }

    /// Returns the original JSON. POST this back to your relying-party server
    /// verbatim.
    public String toJson() {
        return json;
    }

    /// Read-only view of the parsed JSON.
    public Map<String, Object> asMap() {
        return parsed;
    }

    /// `id` -- the credential identifier, base64url-encoded. Stable across
    /// ceremonies for the same authenticator + relying party pair, so this
    /// is what you store on the server.
    public String getId() {
        Object id = parsed.get("id");
        return id == null ? null : id.toString();
    }

    /// `rawId` -- the same identifier as a base64url-encoded byte array.
    public String getRawId() {
        Object id = parsed.get("rawId");
        return id == null ? null : id.toString();
    }

    /// `authenticatorAttachment` -- `"platform"` if a built-in authenticator
    /// (Face ID / Touch ID, Android biometrics) handled the request,
    /// `"cross-platform"` for a hardware key, or `null` if the OS did not
    /// report it.
    public String getAuthenticatorAttachment() {
        Object a = parsed.get("authenticatorAttachment");
        return a == null ? null : a.toString();
    }

    /// `true` if this is a registration (create) response. `false` for an
    /// assertion (get) response.
    public boolean isRegistration() {
        return registration;
    }

    /// `response.clientDataJSON`, base64url-encoded. Decoded server-side and
    /// checked against the original challenge / origin.
    public String getClientDataJSON() {
        Object r = parsed.get("response");
        if (r instanceof Map) {
            Object v = ((Map<?, ?>) r).get("clientDataJSON");
            return v == null ? null : v.toString();
        }
        return null;
    }

    /// `response.attestationObject` for a registration response,
    /// base64url-encoded. `null` on an assertion response.
    public String getAttestationObject() {
        Object r = parsed.get("response");
        if (r instanceof Map) {
            Object v = ((Map<?, ?>) r).get("attestationObject");
            return v == null ? null : v.toString();
        }
        return null;
    }

    /// `response.signature` for an assertion response, base64url-encoded.
    /// `null` on a registration response.
    public String getSignature() {
        Object r = parsed.get("response");
        if (r instanceof Map) {
            Object v = ((Map<?, ?>) r).get("signature");
            return v == null ? null : v.toString();
        }
        return null;
    }

    /// `response.userHandle` for an assertion response, base64url-encoded.
    /// Matches the `user.id` from the registration ceremony.
    public String getUserHandle() {
        Object r = parsed.get("response");
        if (r instanceof Map) {
            Object v = ((Map<?, ?>) r).get("userHandle");
            return v == null ? null : v.toString();
        }
        return null;
    }
}
