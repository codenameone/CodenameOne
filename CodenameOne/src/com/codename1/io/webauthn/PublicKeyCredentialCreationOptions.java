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

/// W3C `PublicKeyCredentialCreationOptionsJSON` -- the options blob your
/// relying-party server sends to start a passkey *registration* ceremony.
///
/// In practice you receive a JSON string from your backend (libraries like
/// `webauthn4j`, `webauthn-rs`, `@simplewebauthn/server` and Auth0 / Firebase's
/// passkey endpoints all emit this shape) and hand it to
/// [WebAuthnClient#create(PublicKeyCredentialCreationOptions)]:
///
/// ```java
/// String optionsJson = httpPost("/passkey/register/challenge", body);
/// PublicKeyCredentialCreationOptions opts =
///         PublicKeyCredentialCreationOptions.fromJson(optionsJson);
/// WebAuthnClient.getInstance().create(opts)
///         .ready(new SuccessCallback<PublicKeyCredential>() {
///             public void onSucess(PublicKeyCredential cred) {
///                 httpPost("/passkey/register/verify", cred.toJson());
///             }
///         });
/// ```
///
/// The class is a thin wrapper over the JSON: it preserves the exact wire
/// representation in [#toJson()] (so the native authenticator sees what your
/// server intended), while exposing convenience accessors for the most-used
/// fields. To synthesise options client-side (rare -- usually only useful in
/// tests) use [#newBuilder()].
///
public final class PublicKeyCredentialCreationOptions {

    /// The full options JSON, preserved verbatim so the authenticator sees
    /// exactly what the relying party intended.
    private final String json;
    private final Map<String, Object> parsed;

    private PublicKeyCredentialCreationOptions(String json, Map<String, Object> parsed) {
        this.json = json;
        this.parsed = parsed;
    }

    /// Parses a PublicKeyCredentialCreationOptionsJSON document. The JSON is
    /// kept intact for forwarding to the native authenticator; any fields
    /// this class doesn't model are still passed through unchanged.
    public static PublicKeyCredentialCreationOptions fromJson(String json) {
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
        return new PublicKeyCredentialCreationOptions(json, parsed);
    }

    /// Returns the original JSON document. The native authenticator receives
    /// this string directly.
    public String toJson() {
        return json;
    }

    /// The full parsed document. Useful for inspecting fields not modelled
    /// directly on this class (`attestation`, `excludeCredentials`,
    /// `authenticatorSelection`, etc.).
    public Map<String, Object> asMap() {
        return parsed;
    }

    /// Relying-party identifier (`rp.id`). On iOS this must match an
    /// `applinks:<rp.id>` Associated Domain. On Android this must match an
    /// `assetlinks.json` published at `https://<rp.id>/.well-known/...`.
    public String getRpId() {
        Object rp = parsed.get("rp");
        if (rp instanceof Map) {
            Object id = ((Map<?, ?>) rp).get("id");
            return id == null ? null : id.toString();
        }
        return null;
    }

    /// Human-readable relying-party name (`rp.name`).
    public String getRpName() {
        Object rp = parsed.get("rp");
        if (rp instanceof Map) {
            Object name = ((Map<?, ?>) rp).get("name");
            return name == null ? null : name.toString();
        }
        return null;
    }

    /// `user.id`, the relying-party-specific user handle (base64url-encoded
    /// in the JSON wire format).
    public String getUserId() {
        Object user = parsed.get("user");
        if (user instanceof Map) {
            Object id = ((Map<?, ?>) user).get("id");
            return id == null ? null : id.toString();
        }
        return null;
    }

    /// `user.name`, usually the email or username the credential is for.
    public String getUserName() {
        Object user = parsed.get("user");
        if (user instanceof Map) {
            Object name = ((Map<?, ?>) user).get("name");
            return name == null ? null : name.toString();
        }
        return null;
    }

    /// `user.displayName` (the human-friendly name shown on the OS sheet).
    public String getUserDisplayName() {
        Object user = parsed.get("user");
        if (user instanceof Map) {
            Object name = ((Map<?, ?>) user).get("displayName");
            return name == null ? null : name.toString();
        }
        return null;
    }

    /// The challenge bytes, base64url-encoded (as they appear on the wire).
    public String getChallenge() {
        Object c = parsed.get("challenge");
        return c == null ? null : c.toString();
    }

    /// Builder for the rare case where you need to synthesise the options
    /// client-side -- e.g. unit tests. The standard usage is
    /// [#fromJson(String)] with a server-supplied document.
    public static Builder newBuilder() {
        return new Builder();
    }

    /// Fluent builder for [PublicKeyCredentialCreationOptions]. The resulting
    /// JSON is W3C-compliant and forwarded verbatim to the OS authenticator.
    public static final class Builder {
        private String rpId;
        private String rpName;
        private String userId;
        private String userName;
        private String userDisplayName;
        private String challenge;
        private String authenticatorAttachment;
        private String userVerification = "preferred";
        private String residentKey = "preferred";

        public Builder rp(String id, String name) {
            this.rpId = id;
            this.rpName = name;
            return this;
        }

        public Builder user(String id, String name, String displayName) {
            this.userId = id;
            this.userName = name;
            this.userDisplayName = displayName;
            return this;
        }

        /// Challenge bytes, base64url-encoded.
        public Builder challenge(String base64UrlChallenge) {
            this.challenge = base64UrlChallenge;
            return this;
        }

        /// `"platform"` to require a platform authenticator (Face ID / Touch ID
        /// on iOS, Android biometrics) or `"cross-platform"` for hardware
        /// keys. `null` (the default) lets the OS pick.
        public Builder authenticatorAttachment(String v) {
            this.authenticatorAttachment = v;
            return this;
        }

        /// `"required"`, `"preferred"` (default) or `"discouraged"`.
        public Builder userVerification(String v) {
            this.userVerification = v;
            return this;
        }

        /// `"required"` (the modern default for passkeys), `"preferred"` or
        /// `"discouraged"`.
        public Builder residentKey(String v) {
            this.residentKey = v;
            return this;
        }

        public PublicKeyCredentialCreationOptions build() {
            if (rpId == null) {
                throw new IllegalStateException("rp.id is required");
            }
            if (userId == null) {
                throw new IllegalStateException("user.id is required");
            }
            if (challenge == null) {
                throw new IllegalStateException("challenge is required");
            }
            StringBuilder b = new StringBuilder("{");
            b.append("\"rp\":{")
                    .append("\"id\":").append(quote(rpId));
            if (rpName != null) {
                b.append(",\"name\":").append(quote(rpName));
            }
            b.append("},");
            b.append("\"user\":{")
                    .append("\"id\":").append(quote(userId));
            if (userName != null) {
                b.append(",\"name\":").append(quote(userName));
            }
            if (userDisplayName != null) {
                b.append(",\"displayName\":").append(quote(userDisplayName));
            }
            b.append("},");
            b.append("\"challenge\":").append(quote(challenge)).append(",");
            // ES256 (-7) and RS256 (-257) are universally supported.
            b.append("\"pubKeyCredParams\":[")
                    .append("{\"type\":\"public-key\",\"alg\":-7},")
                    .append("{\"type\":\"public-key\",\"alg\":-257}],");
            b.append("\"authenticatorSelection\":{")
                    .append("\"userVerification\":").append(quote(userVerification))
                    .append(",\"residentKey\":").append(quote(residentKey));
            if ("required".equals(residentKey)) {
                b.append(",\"requireResidentKey\":true");
            }
            if (authenticatorAttachment != null) {
                b.append(",\"authenticatorAttachment\":")
                        .append(quote(authenticatorAttachment));
            }
            b.append("},");
            b.append("\"attestation\":\"none\"");
            b.append("}");
            return PublicKeyCredentialCreationOptions.fromJson(b.toString());
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
