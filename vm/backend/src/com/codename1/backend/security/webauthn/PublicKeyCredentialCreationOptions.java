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

/// What a client is given to make a passkey with: the relying party, the
/// user, a challenge, and what kind of credential is wanted.
///
/// [#toMap] is the WebAuthn JSON form, `PublicKeyCredentialCreationOptionsJSON`
/// -- byte strings in base64url without padding -- which is what a browser's
/// `PublicKeyCredential.parseCreationOptionsFromJSON` and the Codename One
/// client's `PublicKeyCredentialCreationOptions.fromJson` take as it is:
///
/// ```json
/// {"rp": {"id": "example.com", "name": "Example"},
///  "user": {"id": "q83v...", "name": "ada", "displayName": "ada"},
///  "challenge": "9zV1...",
///  "pubKeyCredParams": [{"type": "public-key", "alg": -7},
///                       {"type": "public-key", "alg": -257}],
///  "timeout": 300000,
///  "excludeCredentials": [{"type": "public-key", "id": "mF2k..."}],
///  "authenticatorSelection": {"residentKey": "required", "requireResidentKey": true,
///                             "userVerification": "preferred"},
///  "attestation": "none",
///  "extensions": {"credProps": true}}
/// ```
///
/// Made by [WebAuthnRelyingPartyOperations#createPublicKeyCredentialCreationOptions].
public final class PublicKeyCredentialCreationOptions {
    private final PublicKeyCredentialRpEntity rp;
    private final PublicKeyCredentialUserEntity user;
    private final byte[] challenge;
    private final long timeout;
    private final List<byte[]> excludeCredentials;
    private final List<List<String>> excludeTransports;
    private final String authenticatorAttachment;
    private final String residentKey;
    private final String userVerification;

    PublicKeyCredentialCreationOptions(PublicKeyCredentialRpEntity rp,
            PublicKeyCredentialUserEntity user, byte[] challenge, long timeout,
            List<byte[]> excludeCredentials, List<List<String>> excludeTransports,
            String authenticatorAttachment, String residentKey, String userVerification) {
        this.rp = rp;
        this.user = user;
        this.challenge = challenge.clone();
        this.timeout = timeout;
        this.excludeCredentials = excludeCredentials;
        this.excludeTransports = excludeTransports;
        this.authenticatorAttachment = authenticatorAttachment;
        this.residentKey = residentKey;
        this.userVerification = userVerification;
    }

    public PublicKeyCredentialRpEntity getRp() {
        return rp;
    }

    public PublicKeyCredentialUserEntity getUser() {
        return user;
    }

    /// The challenge the authenticator's answer must carry back.
    public byte[] getChallenge() {
        return challenge.clone();
    }

    /// How long the client may take, in milliseconds: a hint to it.
    public long getTimeout() {
        return timeout;
    }

    /// `required`, `preferred` or `discouraged`.
    public String getUserVerification() {
        return userVerification;
    }

    /// `required`, `preferred` or `discouraged`: whether the credential must be
    /// one a sign-in can find without being told the user.
    public String getResidentKey() {
        return residentKey;
    }

    /// `platform`, `cross-platform`, or null for either.
    public String getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    /// The ids of the credentials the user has already, which the authenticator
    /// holding one of them will not make another beside.
    public List<byte[]> getExcludeCredentials() {
        List<byte[]> out = new ArrayList<byte[]>();
        for (byte[] id : excludeCredentials) {
            out.add(id.clone());
        }
        return out;
    }

    /// The options as the JSON a client takes; see the class.
    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Map<String, Object> party = new LinkedHashMap<String, Object>();
        party.put("id", rp.getId());
        party.put("name", rp.getName());
        out.put("rp", party);
        Map<String, Object> who = new LinkedHashMap<String, Object>();
        who.put("id", Base64Url.encode(user.getId()));
        who.put("name", user.getName());
        who.put("displayName", user.getDisplayName());
        out.put("user", who);
        out.put("challenge", Base64Url.encode(challenge));
        List<Object> params = new ArrayList<Object>();
        params.add(parameter(CoseKey.ES256));
        params.add(parameter(CoseKey.RS256));
        out.put("pubKeyCredParams", params);
        out.put("timeout", Long.valueOf(timeout));
        out.put("excludeCredentials", descriptors(excludeCredentials, excludeTransports));
        Map<String, Object> selection = new LinkedHashMap<String, Object>();
        if (authenticatorAttachment != null) {
            selection.put("authenticatorAttachment", authenticatorAttachment);
        }
        selection.put("residentKey", residentKey);
        selection.put("requireResidentKey", Boolean.valueOf("required".equals(residentKey)));
        selection.put("userVerification", userVerification);
        out.put("authenticatorSelection", selection);
        // No attestation is asked for: who made the authenticator is not
        // something this server decides anything by.
        out.put("attestation", "none");
        Map<String, Object> extensions = new LinkedHashMap<String, Object>();
        extensions.put("credProps", Boolean.TRUE);
        out.put("extensions", extensions);
        return out;
    }

    private static Map<String, Object> parameter(long algorithm) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("type", "public-key");
        out.put("alg", Long.valueOf(algorithm));
        return out;
    }

    static List<Object> descriptors(List<byte[]> ids, List<List<String>> transports) {
        List<Object> out = new ArrayList<Object>();
        for (int iter = 0 ; iter < ids.size() ; iter++) {
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("type", "public-key");
            one.put("id", Base64Url.encode(ids.get(iter)));
            List<String> how = transports.get(iter);
            if (how != null && !how.isEmpty()) {
                one.put("transports", new ArrayList<Object>(how));
            }
            out.add(one);
        }
        return out;
    }

    /// The options [#toMap] wrote, read back from where a ceremony's pending
    /// state is kept; null when `stored` is not that.
    public static PublicKeyCredentialCreationOptions fromMap(Map stored) {
        if (stored == null) {
            return null;
        }
        Object party = stored.get("rp");
        Object who = stored.get("user");
        Object selection = stored.get("authenticatorSelection");
        byte[] challenge = bytes(stored.get("challenge"));
        if (!(party instanceof Map) || !(who instanceof Map) || !(selection instanceof Map)
                || challenge == null) {
            return null;
        }
        Object rpId = ((Map) party).get("id");
        Object rpName = ((Map) party).get("name");
        Object name = ((Map) who).get("name");
        Object displayName = ((Map) who).get("displayName");
        byte[] userId = bytes(((Map) who).get("id"));
        Object attachment = ((Map) selection).get("authenticatorAttachment");
        Object residentKey = ((Map) selection).get("residentKey");
        Object userVerification = ((Map) selection).get("userVerification");
        Object timeout = stored.get("timeout");
        if (!(rpId instanceof String) || !(name instanceof String) || userId == null
                || userId.length == 0 || userId.length > 64 || ((String) name).length() == 0
                || !(residentKey instanceof String) || !(userVerification instanceof String)) {
            return null;
        }
        List<byte[]> ids = new ArrayList<byte[]>();
        List<List<String>> transports = new ArrayList<List<String>>();
        if (!readDescriptors(stored.get("excludeCredentials"), ids, transports)) {
            return null;
        }
        PublicKeyCredentialRpEntity rp;
        try {
            rp = new PublicKeyCredentialRpEntity((String) rpId,
                    rpName instanceof String ? (String) rpName : null);
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
        return new PublicKeyCredentialCreationOptions(rp,
                new PublicKeyCredentialUserEntity((String) name, userId,
                        displayName instanceof String ? (String) displayName : null),
                challenge, timeout instanceof Number ? ((Number) timeout).longValue() : 0, ids,
                transports, attachment instanceof String ? (String) attachment : null,
                (String) residentKey, (String) userVerification);
    }

    static byte[] bytes(Object encoded) {
        if (!(encoded instanceof String) || ((String) encoded).length() == 0) {
            return null;
        }
        byte[] decoded = Base64Url.decode((String) encoded);
        return decoded == null || decoded.length == 0 ? null : decoded;
    }

    static boolean readDescriptors(Object listed, List<byte[]> ids, List<List<String>> transports) {
        if (listed == null) {
            return true;
        }
        if (!(listed instanceof List)) {
            return false;
        }
        for (Object entry : (List) listed) {
            if (!(entry instanceof Map)) {
                return false;
            }
            byte[] id = bytes(((Map) entry).get("id"));
            if (id == null) {
                return false;
            }
            List<String> how = new ArrayList<String>();
            Object named = ((Map) entry).get("transports");
            if (named instanceof List) {
                for (Object transport : (List) named) {
                    if (transport instanceof String) {
                        how.add((String) transport);
                    }
                }
            }
            ids.add(id);
            transports.add(how);
        }
        return true;
    }
}
