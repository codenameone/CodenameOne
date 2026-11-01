/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebase;

/**
 * firebase_auth's {@code OAuthCredential}: an identity provider's ID token and/or
 * access token, and for Apple the raw nonce whose hash the ID token carries. Built by
 * {@code GoogleAuthProvider.credential} and {@code OAuthProvider(id).credential}.
 */
public class OAuthCredential extends AuthCredential {
    final String idToken;
    final String accessTokenValue;
    final String rawNonce;
    final String secret;

    public OAuthCredential(String providerId, String signInMethod, String idToken, String accessToken,
            String rawNonce, String secret) {
        super(providerId, signInMethod == null ? providerId : signInMethod);
        this.idToken = idToken;
        this.accessTokenValue = accessToken;
        this.rawNonce = rawNonce;
        this.secret = secret;
    }

    public String idToken() {
        return idToken;
    }

    @Override
    public String accessToken() {
        return accessTokenValue;
    }

    public String rawNonce() {
        return rawNonce;
    }

    public String secret() {
        return secret;
    }

    /// The `postBody` of Identity Toolkit's `accounts:signInWithIdp`, a form-encoded
    /// list of what the provider issued.
    String postBody() {
        StringBuilder b = new StringBuilder("providerId=").append(encode(providerId));
        if (idToken != null) {
            b.append("&id_token=").append(encode(idToken));
        }
        if (accessTokenValue != null) {
            b.append("&access_token=").append(encode(accessTokenValue));
        }
        if (rawNonce != null) {
            b.append("&nonce=").append(encode(rawNonce));
        }
        if (secret != null) {
            b.append("&oauth_token_secret=").append(encode(secret));
        }
        return b.toString();
    }

    private static String encode(String v) {
        return com.codename1.io.Util.encodeUrl(v);
    }
}
