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
package com.codename1.backend.security.apikey;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Makes API keys: a prefix, then 32 random bytes as base64url.
///
/// ```java
/// GeneratedApiKey made = new ApiKeyGenerator("acme_").generate("ci-bot", "deploy");
/// ```
///
/// The prefix says what the string is. A secret scanner can find a leaked key
/// by it, a person can tell a key from a token by it, and a chain that takes
/// both API keys and JWTs as bearer credentials tells them apart by it.
///
/// A key is 256 random bits, so its SHA-256 is as hard to reverse as the key is
/// to guess. That is why a plain hash is what is stored, and not the slow,
/// salted one a password needs: there is nothing here to try a dictionary
/// against, and the lookup runs on every request.
public final class ApiKeyGenerator {
    /// The prefix unless another is given.
    public static final String DEFAULT_PREFIX = "cn1_";

    private final String prefix;

    public ApiKeyGenerator() {
        this(DEFAULT_PREFIX);
    }

    /// @param prefix what every key starts with: letters, digits and `_`
    public ApiKeyGenerator(String prefix) {
        this.prefix = checkPrefix(prefix);
    }

    /// Refuses a prefix that is empty or holds anything but letters, digits
    /// and underscores.
    public static String checkPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0 || prefix.length() > 32) {
            throw new IllegalArgumentException("An API key prefix is 1 to 32 characters");
        }
        for (int iter = 0 ; iter < prefix.length() ; iter++) {
            char c = prefix.charAt(iter);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_';
            if (!ok) {
                throw new IllegalArgumentException("An API key prefix is letters, digits and "
                        + "underscores: " + prefix);
            }
        }
        return prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /// A new key for `owner`, granting `scopes`.
    public GeneratedApiKey generate(String owner, String... scopes) {
        if (owner == null || owner.length() == 0) {
            throw new IllegalArgumentException("An API key needs an owner");
        }
        byte[] secret;
        byte[] id;
        try {
            secret = Crypto.randomBytes(32);
            id = Crypto.randomBytes(9);
        } catch (IOException err) {
            throw new IllegalStateException("Could not generate an API key: " + err.getMessage(),
                    err);
        }
        String plaintext = prefix + Base64Url.encode(secret);
        List<String> granted = new ArrayList<String>();
        if (scopes != null) {
            for (String scope : scopes) {
                granted.add(scope);
            }
        }
        return new GeneratedApiKey(plaintext, new ApiKey(Base64Url.encode(id), owner, granted,
                hash(plaintext), prefix, plaintext.substring(plaintext.length() - 4), false));
    }

    /// The lowercase hexadecimal SHA-256 of a key: what is stored, and what a
    /// presented key is looked up by.
    public static String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext cannot be null");
        }
        byte[] bytes;
        try {
            bytes = plaintext.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required", err);
        }
        byte[] digest = Crypto.sha256(bytes);
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f'};
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) {
            sb.append(digits[(b >> 4) & 0xf]).append(digits[b & 0xf]);
        }
        return sb.toString();
    }
}
