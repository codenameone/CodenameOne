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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Json;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Signs tokens with the keys of a [JwkSource].
///
/// ```java
/// JwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(
///         Jwk.ofPrivateKey(KeyFiles.readPrivateKey("/etc/app/signing.pem"))));
/// String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
/// ```
///
/// The key is the first in the source that can sign and fits what the header
/// asks for: the key of that `kid` when the header names one, a key of the
/// algorithm's kind otherwise. With no header the first private key signs,
/// under the algorithm its kind is for -- RS256 for RSA, ES256 or ES384 for an
/// EC key by its curve, HS256 for a secret. The token's `kid` is the key's.
///
/// The source is asked on every call, so a source that starts answering with a
/// new key first has rotated it; see [JwkSource].
public final class DefaultJwtEncoder implements JwtEncoder {
    private final JwkSource keys;

    public DefaultJwtEncoder(JwkSource keys) {
        if (keys == null) {
            throw new IllegalArgumentException("A source of keys is required");
        }
        this.keys = keys;
    }

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters cannot be null");
        }
        JwsHeader header = parameters.getJwsHeader();
        List<Jwk> candidates;
        try {
            candidates = keys.getKeys();
        } catch (IOException err) {
            throw new JwtEncodingException("Could not get a signing key: " + err.getMessage(), err);
        }
        Jwk key = null;
        JwsAlgorithm algorithm = header == null ? null : header.getAlgorithm();
        String keyId = header == null ? null : header.getKeyId();
        for (Jwk candidate : candidates) {
            // The first that can sign, has the id asked for and fits the algorithm.
            if (key == null && candidate.isPrivate()
                    && (keyId == null || keyId.equals(candidate.getKeyId()))
                    && (algorithm == null || Jose.fits(candidate, algorithm))) {
                key = candidate;
            }
        }
        if (key == null) {
            throw new JwtEncodingException("There is no key to sign with"
                    + (algorithm == null ? "" : " under " + algorithm.getName())
                    + (keyId == null ? "" : " with the id " + keyId)
                    + ": the source holds no private key that fits");
        }
        if (algorithm == null) {
            algorithm = Jose.defaultFor(key);
        }
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("alg", algorithm.getName());
        if (header != null) {
            headers.putAll(header.getHeaders());
        }
        if (key.getKeyId() != null) {
            headers.put("kid", key.getKeyId());
        }
        Map<String, Object> claims = parameters.getClaims().getClaims();
        String signingInput = Base64Url.encode(Jose.utf8(Json.write(headers))) + "."
                + Base64Url.encode(Jose.utf8(Json.write(claims)));
        byte[] signature;
        try {
            signature = Jose.sign(algorithm, key, Jose.ascii(signingInput));
        } catch (IOException err) {
            throw new JwtEncodingException("Could not sign the token: " + err.getMessage(), err);
        }
        if (signature == null) {
            throw new JwtEncodingException("Could not sign the token");
        }
        return new Jwt(signingInput + "." + Base64Url.encode(signature), headers, claims);
    }
}
