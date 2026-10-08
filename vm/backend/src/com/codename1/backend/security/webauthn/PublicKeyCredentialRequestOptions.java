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

import com.codename1.backend.Base64Url;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What a client is given to sign in with a passkey: a challenge, the relying
/// party, and -- when the user said who they are first -- which credentials
/// may answer.
///
/// [#toMap] is the WebAuthn JSON form, `PublicKeyCredentialRequestOptionsJSON`,
/// which a browser's `PublicKeyCredential.parseRequestOptionsFromJSON` and the
/// Codename One client's `PublicKeyCredentialRequestOptions.fromJson` take as
/// it is:
///
/// ```json
/// {"challenge": "9zV1...", "timeout": 300000, "rpId": "example.com",
///  "allowCredentials": [], "userVerification": "preferred"}
/// ```
///
/// An empty `allowCredentials` asks the authenticator for any passkey it holds
/// for this relying party; its answer then names the user.
///
/// Made by [WebAuthnRelyingPartyOperations#createCredentialRequestOptions].
public final class PublicKeyCredentialRequestOptions {
    private final byte[] challenge;
    private final long timeout;
    private final String rpId;
    private final List<byte[]> allowCredentials;
    private final List<List<String>> allowTransports;
    private final String userVerification;
    private final byte[] userId;

    PublicKeyCredentialRequestOptions(byte[] challenge, long timeout, String rpId,
            List<byte[]> allowCredentials, List<List<String>> allowTransports,
            String userVerification, byte[] userId) {
        this.challenge = challenge.clone();
        this.timeout = timeout;
        this.rpId = rpId;
        this.allowCredentials = allowCredentials;
        this.allowTransports = allowTransports;
        this.userVerification = userVerification;
        this.userId = userId == null ? null : userId.clone();
    }

    /// The challenge the authenticator's answer must sign.
    public byte[] getChallenge() {
        return challenge.clone();
    }

    /// How long the client may take, in milliseconds: a hint to it.
    public long getTimeout() {
        return timeout;
    }

    public String getRpId() {
        return rpId;
    }

    /// `required`, `preferred` or `discouraged`.
    public String getUserVerification() {
        return userVerification;
    }

    /// The ids of the credentials that may answer; empty for any the
    /// authenticator holds.
    public List<byte[]> getAllowCredentials() {
        List<byte[]> out = new ArrayList<byte[]>();
        for (byte[] id : allowCredentials) {
            out.add(id.clone());
        }
        return out;
    }

    /// The handle of the user who said who they are before the ceremony, whose
    /// credential the answer must then be; null when nobody did. Kept with the
    /// pending ceremony and not sent to the client.
    public byte[] getUserId() {
        return userId == null ? null : userId.clone();
    }

    /// The options as the JSON a client takes; see the class.
    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("challenge", Base64Url.encode(challenge));
        out.put("timeout", Long.valueOf(timeout));
        out.put("rpId", rpId);
        out.put("allowCredentials", PublicKeyCredentialCreationOptions.descriptors(
                allowCredentials, allowTransports));
        out.put("userVerification", userVerification);
        return out;
    }

    /// [#toMap] and, beside it, what the server alone keeps: for where a
    /// ceremony's pending state is stored.
    public Map<String, Object> toStoredMap() {
        Map<String, Object> out = toMap();
        if (userId != null) {
            out.put("userId", Base64Url.encode(userId));
        }
        return out;
    }

    /// The options [#toStoredMap] wrote, read back; null when `stored` is not
    /// that.
    public static PublicKeyCredentialRequestOptions fromMap(Map stored) {
        if (stored == null) {
            return null;
        }
        byte[] challenge = PublicKeyCredentialCreationOptions.bytes(stored.get("challenge"));
        Object rpId = stored.get("rpId");
        Object userVerification = stored.get("userVerification");
        Object timeout = stored.get("timeout");
        if (challenge == null || !(rpId instanceof String)
                || !(userVerification instanceof String)) {
            return null;
        }
        List<byte[]> ids = new ArrayList<byte[]>();
        List<List<String>> transports = new ArrayList<List<String>>();
        if (!PublicKeyCredentialCreationOptions.readDescriptors(stored.get("allowCredentials"),
                ids, transports)) {
            return null;
        }
        byte[] userId = null;
        if (stored.get("userId") != null) {
            userId = PublicKeyCredentialCreationOptions.bytes(stored.get("userId"));
            if (userId == null) {
                return null;
            }
        }
        return new PublicKeyCredentialRequestOptions(challenge,
                timeout instanceof Number ? ((Number) timeout).longValue() : 0, (String) rpId,
                ids, transports, (String) userVerification, userId);
    }
}
