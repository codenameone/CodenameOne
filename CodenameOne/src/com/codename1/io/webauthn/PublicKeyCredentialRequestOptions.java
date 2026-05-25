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
import java.util.Map;

/// W3C `PublicKeyCredentialRequestOptionsJSON` -- the options blob your
/// relying-party server sends to start a passkey *sign-in* (assertion)
/// ceremony.
///
/// Mirrors [PublicKeyCredentialCreationOptions]: receive JSON from the server,
/// parse via [#fromJson(String)], hand to
/// [WebAuthnClient#get(PublicKeyCredentialRequestOptions)], post the result
/// back to your server for verification.
///
/// @since 7.0.245
public final class PublicKeyCredentialRequestOptions {

    private final String json;
    private final Map<String, Object> parsed;

    private PublicKeyCredentialRequestOptions(String json, Map<String, Object> parsed) {
        this.json = json;
        this.parsed = parsed;
    }

    /// Parses a PublicKeyCredentialRequestOptionsJSON document. The JSON is
    /// preserved so any fields this class doesn't model are still passed
    /// through to the authenticator unchanged.
    public static PublicKeyCredentialRequestOptions fromJson(String json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        Map<String, Object> parsed;
        try {
            parsed = new JSONParser().parseJSON(new StringReader(json));
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Invalid options JSON: " + ioe.getMessage(), ioe);
        }
        if (parsed == null) {
            throw new IllegalArgumentException("Options JSON parsed to null");
        }
        return new PublicKeyCredentialRequestOptions(json, parsed);
    }

    public String toJson() {
        return json;
    }

    public Map<String, Object> asMap() {
        return parsed;
    }

    /// `rpId` -- the relying-party identifier the credential was registered
    /// against.
    public String getRpId() {
        Object id = parsed.get("rpId");
        return id == null ? null : id.toString();
    }

    /// `challenge`, base64url-encoded as it appears on the wire.
    public String getChallenge() {
        Object c = parsed.get("challenge");
        return c == null ? null : c.toString();
    }

    /// `userVerification` -- one of `"required"`, `"preferred"`, `"discouraged"`.
    public String getUserVerification() {
        Object v = parsed.get("userVerification");
        return v == null ? null : v.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /// Fluent builder for the rare case of synthesising the options
    /// client-side (e.g. unit tests). Use [#fromJson(String)] for the
    /// production path.
    public static final class Builder {
        private String rpId;
        private String challenge;
        private String userVerification = "preferred";

        public Builder rpId(String v) {
            this.rpId = v;
            return this;
        }

        public Builder challenge(String base64UrlChallenge) {
            this.challenge = base64UrlChallenge;
            return this;
        }

        public Builder userVerification(String v) {
            this.userVerification = v;
            return this;
        }

        public PublicKeyCredentialRequestOptions build() {
            if (rpId == null) {
                throw new IllegalStateException("rpId is required");
            }
            if (challenge == null) {
                throw new IllegalStateException("challenge is required");
            }
            StringBuilder b = new StringBuilder("{")
                    .append("\"rpId\":").append(quote(rpId))
                    .append(",\"challenge\":").append(quote(challenge))
                    .append(",\"userVerification\":").append(quote(userVerification))
                    .append("}");
            return PublicKeyCredentialRequestOptions.fromJson(b.toString());
        }

        private static String quote(String s) {
            StringBuilder out = new StringBuilder(s.length() + 8).append('"');
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':  out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '\n': out.append("\\n");  break;
                    case '\r': out.append("\\r");  break;
                    case '\t': out.append("\\t");  break;
                    default:
                        if (c < 0x20) {
                            String hex = Integer.toHexString(c);
                            out.append("\\u");
                            for (int p = hex.length(); p < 4; p++) {
                                out.append('0');
                            }
                            out.append(hex);
                        } else {
                            out.append(c);
                        }
                }
            }
            return out.append('"').toString();
        }
    }
}
