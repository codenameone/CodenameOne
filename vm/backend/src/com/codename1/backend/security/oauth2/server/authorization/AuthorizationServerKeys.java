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
package com.codename1.backend.security.oauth2.server.authorization;

import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.KeyFiles;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The keys an authorization server signs with.
///
/// ```
/// cn1.security.authorizationserver.jwk.keys=/etc/acme/signing-2026.pem,/etc/acme/signing-2025.pem
/// ```
///
/// Each is a PEM file holding an RSA or a P-256 private key. The first signs
/// every new token; all of them are published at the JWK Set endpoint, so that
/// a token signed by the key that was first until yesterday still verifies. To
/// rotate, put the new key first and keep the old one in the list for as long
/// as its tokens live.
///
/// A key can be made with
///
/// ```
/// openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out signing.pem
/// ```
///
/// A key brought over from another authorization server keeps the `kid` its
/// tokens already carry, or none of them would find their key. Name the ids in
/// the same order as the files; an empty entry keeps the derived id:
///
/// ```
/// cn1.security.authorizationserver.jwk.keys=/etc/acme/new.pem,/etc/acme/legacy.pem
/// cn1.security.authorizationserver.jwk.key-ids=,legacy-2024-rsa
/// ```
///
/// A key whose private half stayed behind -- the old provider's public JWK Set
/// or a public PEM -- can still verify the tokens it signed. List it under
/// `cn1.security.authorizationserver.jwk.verification-keys`: it is published
/// and verifies, and never signs. A `.json` file is read as a JWK Set and keeps
/// each key's `kid`; any other file is a public key PEM under its derived id.
///
/// On a development profile with no key set, a key is made when the server
/// starts and a warning says so: every restart then invalidates every token.
/// Anywhere else a missing key stops the server from starting.
///
/// An application that issues tokens of its own -- a long-lived token for a
/// build agent -- declares the keys and an encoder as beans, and the
/// authorization server uses the same ones:
///
/// ```java
/// @Bean
/// JwkSource signingKeys(Config config) throws IOException {
///     return AuthorizationServerKeys.load(config);
/// }
///
/// @Bean
/// JwtEncoder jwtEncoder(JwkSource keys) {
///     return new DefaultJwtEncoder(keys);
/// }
/// ```
public final class AuthorizationServerKeys {
    /// The setting that lists the key files, separated by commas.
    public static final String KEYS = "cn1.security.authorizationserver.jwk.keys";
    /// The `kid` of each key in [#KEYS], in the same order; an empty entry keeps
    /// the id derived from the public key.
    public static final String KEY_IDS = "cn1.security.authorizationserver.jwk.key-ids";
    /// Public keys that verify and are published, and never sign.
    public static final String VERIFICATION_KEYS =
            "cn1.security.authorizationserver.jwk.verification-keys";

    private AuthorizationServerKeys() {
    }

    /// The keys the configuration names, the signing one first.
    ///
    /// - `IllegalStateException`: when none is set outside a development
    /// profile, or one of them is not a key this server signs with
    public static JwkSource load(Config config) throws IOException {
        String listed = config.get(KEYS);
        final List<Jwk> keys = new ArrayList<Jwk>();
        List<String> paths = entries(listed);
        List<String> ids = entries(config.get(KEY_IDS));
        if (ids.size() > paths.size()) {
            throw new IllegalStateException(KEY_IDS + " names " + ids.size() + " ids for "
                    + paths.size() + " keys in " + KEYS);
        }
        for (int iter = 0 ; iter < paths.size() ; iter++) {
            String path = paths.get(iter);
            if (path.length() == 0) {
                continue;
            }
            Jwk key = Jwk.ofPrivateKey(KeyFiles.readPrivateKey(path));
            String id = iter < ids.size() ? ids.get(iter) : "";
            keys.add(usable(id.length() == 0 ? key : key.withKeyId(id), path));
        }
        if (keys.isEmpty()) {
            if (!config.isDevelopmentProfile()) {
                throw new IllegalStateException("The authorization server has no key to sign "
                        + "tokens with. Make one with\n    openssl genpkey -algorithm RSA "
                        + "-pkeyopt rsa_keygen_bits:2048 -out signing.pem\nand set " + KEYS
                        + " to its path, or declare a JwkSource bean.");
            }
            System.err.println("cn1: WARNING: " + KEYS + " is not set; the authorization server "
                    + "signs with a key made just now, and every token it issues stops "
                    + "verifying when this process ends. Development only.");
            keys.add(usable(Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)), "the generated key"));
        }
        for (String path : entries(config.get(VERIFICATION_KEYS))) {
            if (path.length() == 0) {
                continue;
            }
            if (path.endsWith(".json")) {
                for (Jwk key : JwkSet.parse(KeyFiles.read(path)).getKeys()) {
                    keys.add(verifying(key, path));
                }
            } else {
                // Read as either kind, so a private key is refused by name rather than
                // as a file that failed to parse.
                keys.add(verifying(Jwk.ofPem(KeyFiles.read(path)), path));
            }
        }
        return of(keys);
    }

    /// The comma-separated entries of a setting, trimmed, empty ones kept so that
    /// a list of ids lines up with its list of files.
    private static List<String> entries(String listed) {
        List<String> out = new ArrayList<String>();
        if (listed == null || listed.trim().length() == 0) {
            return out;
        }
        int start = 0;
        while (true) {
            int comma = listed.indexOf(',', start);
            int end = comma < 0 ? listed.length() : comma;
            out.add(listed.substring(start, end).trim());
            if (comma < 0) {
                return out;
            }
            start = comma + 1;
        }
    }

    /// A public key brought along to verify tokens it signed elsewhere.
    private static Jwk verifying(Jwk key, String where) {
        String algorithm = algorithm(key);
        if (algorithm == null) {
            throw new IllegalStateException("The authorization server verifies with an RSA or "
                    + "a P-256 key, and " + where + " holds one that is neither");
        }
        if (key.isPrivate()) {
            throw new IllegalStateException(VERIFICATION_KEYS + " takes public keys, and " + where
                    + " holds a private one; list it in " + KEYS + " to sign with it");
        }
        if (key.getUse() != null && !"sig".equals(key.getUse())) {
            throw new IllegalStateException(where + " holds a key for use \"" + key.getUse()
                    + "\"; only signature keys verify tokens");
        }
        return key.withUse("sig").withAlgorithm(
                key.getAlgorithm() == null ? algorithm : key.getAlgorithm());
    }

    /// A source of exactly these keys, the signing one first.
    public static JwkSource of(List<Jwk> keys) {
        final List<Jwk> fixed = JwkSet.of(keys.toArray(new Jwk[keys.size()])).getKeys();
        return new JwkSource() {
            @Override
            public List<Jwk> getKeys() {
                return fixed;
            }
        };
    }

    /// `key` marked as a signing key under the algorithm its kind signs with.
    ///
    /// - `IllegalStateException`: when it is neither RSA nor P-256
    public static Jwk usable(Jwk key, String where) {
        String algorithm = algorithm(key);
        if (algorithm == null || !key.isPrivate()) {
            throw new IllegalStateException("The authorization server signs with an RSA or a "
                    + "P-256 private key, and " + where + " is not one");
        }
        return key.withUse("sig").withAlgorithm(algorithm);
    }

    /// `RS256` for an RSA key, `ES256` for a P-256 one, null for any other.
    public static String algorithm(Jwk key) {
        if ("RSA".equals(key.getKeyType())) {
            return "RS256";
        }
        if ("EC".equals(key.getKeyType()) && "P-256".equals(key.getCurve())) {
            return "ES256";
        }
        return null;
    }
}
