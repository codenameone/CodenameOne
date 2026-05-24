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

import com.codename1.security.Hash;
import com.codename1.security.SecureRandom;
import com.codename1.util.Base64;

/// One PKCE pair (RFC 7636). The `code_verifier` is kept by the client; the
/// `code_challenge` (always `S256` here) is sent to the authorization endpoint;
/// the verifier is then presented to the token endpoint to prove possession.
///
/// PKCE is mandatory on every authorization-code flow this client initiates,
/// even when a `client_secret` is configured -- providers like Google and
/// Microsoft both require it for mobile public clients and tolerate it for
/// confidential clients.
///
/// @since 7.0.245
public final class PkceChallenge {

    /// Always `"S256"` -- the only value [OidcClient] emits. RFC 7636 also
    /// defines `"plain"` but it is forbidden by this client.
    public static final String METHOD_S256 = "S256";

    private final String verifier;
    private final String challenge;

    private PkceChallenge(String verifier, String challenge) {
        this.verifier = verifier;
        this.challenge = challenge;
    }

    /// Generates a fresh PKCE pair with a 64-byte (~86 char) verifier. The
    /// verifier characters are drawn from the unreserved set
    /// `[A-Z][a-z][0-9]-._~` via base64url encoding of secure random bytes,
    /// per RFC 7636 section 4.1.
    public static PkceChallenge generate() {
        byte[] random = SecureRandom.bytes(64);
        String verifier = Base64.encodeUrlSafe(random);
        verifier = strip(verifier);
        byte[] digest;
        try {
            digest = Hash.sha256(verifier.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException uee) {
            // UTF-8 is guaranteed by the Java spec on every JVM; reach this
            // branch only on a malformed runtime. Rethrow rather than fall
            // back to the platform default encoding (SpotBugs DM_DEFAULT_ENCODING).
            throw new IllegalStateException("UTF-8 is not available on this JVM", uee);
        }
        String challenge = strip(Base64.encodeUrlSafe(digest));
        return new PkceChallenge(verifier, challenge);
    }

    /// The verifier that must be supplied to the token endpoint as
    /// `code_verifier`.
    public String getVerifier() {
        return verifier;
    }

    /// The challenge to include on the authorization URL as `code_challenge`.
    public String getChallenge() {
        return challenge;
    }

    /// Always returns [#METHOD_S256].
    public String getMethod() {
        return METHOD_S256;
    }

    /// Strip trailing `=` padding and any embedded newlines that older
    /// base64 encoders insert. Doing it here keeps the rest of the client
    /// portable across the standard and url-safe encoders.
    private static String strip(String s) {
        int len = s.length();
        StringBuilder b = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '=') {
                continue;
            }
            b.append(c);
        }
        return b.toString();
    }
}
