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
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidatorResult;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.MacAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Verifies tokens against keys it holds, or keys an issuer publishes.
///
/// ```java
/// JwtDecoder local = DefaultJwtDecoder.withPublicKey(KeyFiles.readPublicKey(path)).build();
/// JwtDecoder remote = DefaultJwtDecoder.withJwkSetUri("https://id.example.com/jwks")
///         .jwsAlgorithms(SignatureAlgorithm.RS256, SignatureAlgorithm.ES256).build();
/// JwtDecoder own = DefaultJwtDecoder.withSecretKey(secret).build();
/// ```
///
/// #### Which algorithm verifies a token
///
/// Not the one the token asks for. A decoder is built with the algorithms it
/// accepts -- RS256 unless told otherwise, or the one a single key is for --
/// and a token is verified only when all of these agree:
///
/// - its `alg` header is one of the accepted algorithms;
/// - the decoder has a key of the kind that algorithm is defined over: an RSA
///   key for RS256/384/512 and PS256, a P-256 key for ES256, a P-384 key for
///   ES384, a shared secret for HS256/384/512;
/// - when the token names a `kid`, that key has it.
///
/// So `alg: none` is refused, because no decoder accepts it; and a token signed
/// with HMAC using an RSA public key as the secret is refused, because the
/// decoder that holds that public key does not accept HS256, and would have no
/// shared secret to check it with if it did.
///
/// #### What else is checked
///
/// The validators given with [#setJwtValidator]; `exp` and `nbf` unless set.
/// A decoder does not check `iss` or `aud` until it is told what they should
/// be -- [JwtDecoders#fromIssuerLocation] tells it the first.
public final class DefaultJwtDecoder implements JwtDecoder {
    /// More than any token is, and little enough to refuse cheaply.
    private static final int MAX_TOKEN_CHARS = 16 * 1024;

    private final JwkSource keys;
    private final List<JwsAlgorithm> algorithms;
    private OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefault();

    private DefaultJwtDecoder(JwkSource keys, List<JwsAlgorithm> algorithms) {
        this.keys = keys;
        this.algorithms = algorithms;
    }

    /// A decoder for tokens signed by the private half of one public key, given
    /// as SubjectPublicKeyInfo DER; see
    /// [com.codename1.backend.security.crypto.KeyFiles#publicKey]. It accepts
    /// RS256 for an RSA key and the curve's algorithm for an EC key.
    public static Builder withPublicKey(byte[] publicKey) {
        Jwk key;
        try {
            key = Jwk.ofPublicKey(publicKey);
        } catch (IOException err) {
            throw new IllegalArgumentException("Not a usable public key: " + err.getMessage(), err);
        }
        Builder builder = new Builder(JwkSet.of(key));
        builder.algorithms.add(Jose.defaultFor(key));
        return builder;
    }

    /// A decoder over these keys; RS256 unless told otherwise.
    public static Builder withJwkSource(JwkSource keys) {
        if (keys == null) {
            throw new IllegalArgumentException("A source of keys is required");
        }
        Builder builder = new Builder(keys);
        builder.algorithms.add(SignatureAlgorithm.RS256);
        return builder;
    }

    /// A decoder over the keys published at `jwkSetUri`, fetched when first
    /// needed; see [RemoteJwkSet]. RS256 unless told otherwise.
    public static Builder withJwkSetUri(String jwkSetUri) {
        return withJwkSource(new RemoteJwkSet(jwkSetUri));
    }

    /// A decoder for tokens this server signed itself with a shared secret;
    /// HS256 unless told otherwise.
    public static Builder withSecretKey(byte[] secret) {
        if (secret == null || secret.length < Jose.minSecretBytes(MacAlgorithm.HS256)) {
            throw new IllegalArgumentException("The secret must be at least 32 bytes; a shorter "
                    + "one can be found by trying");
        }
        Builder builder = new Builder(JwkSet.of(Jwk.ofSecret(secret)));
        builder.algorithms.add(MacAlgorithm.HS256);
        return builder;
    }

    /// Builds a [DefaultJwtDecoder].
    public static final class Builder {
        private final JwkSource keys;
        private final List<JwsAlgorithm> algorithms = new ArrayList<JwsAlgorithm>();

        private Builder(JwkSource keys) {
            this.keys = keys;
        }

        /// The one algorithm accepted, in place of the default.
        public Builder jwsAlgorithm(JwsAlgorithm algorithm) {
            return jwsAlgorithms(algorithm);
        }

        /// The algorithms accepted, in place of the default.
        public Builder jwsAlgorithms(JwsAlgorithm... accepted) {
            if (accepted == null || accepted.length == 0) {
                throw new IllegalArgumentException("At least one algorithm is required");
            }
            algorithms.clear();
            for (JwsAlgorithm algorithm : accepted) {
                if (algorithm == null) {
                    throw new IllegalArgumentException("An algorithm cannot be null");
                }
                algorithms.add(algorithm);
            }
            return this;
        }

        public DefaultJwtDecoder build() {
            return new DefaultJwtDecoder(keys, new ArrayList<JwsAlgorithm>(algorithms));
        }
    }

    /// What a token's claims are put to once its signature has verified;
    /// [JwtValidators#createDefault] unless set.
    public void setJwtValidator(OAuth2TokenValidator<Jwt> jwtValidator) {
        if (jwtValidator == null) {
            throw new IllegalArgumentException("jwtValidator cannot be null");
        }
        synchronized (this) {
            this.validator = jwtValidator;
        }
    }

    private synchronized OAuth2TokenValidator<Jwt> validator() {
        return validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Jwt decode(String token) {
        if (token == null || token.length() == 0) {
            throw new BadJwtException("No token was given");
        }
        if (token.length() > MAX_TOKEN_CHARS) {
            throw new BadJwtException("The token is too long to be one");
        }
        int first = token.indexOf('.');
        int second = first < 0 ? -1 : token.indexOf('.', first + 1);
        if (first <= 0 || second <= first + 1) {
            throw new BadJwtException("Malformed token: not three parts joined by dots");
        }
        if (token.indexOf('.', second + 1) >= 0) {
            throw new BadJwtException("Malformed token: an encrypted token, or one of more "
                    + "than three parts, is not supported");
        }
        Map<String, Object> headers = (Map<String, Object>) object(token.substring(0, first),
                "header");
        Object named = headers.get("alg");
        if (!(named instanceof String)) {
            throw new BadJwtException("Malformed token: the header names no algorithm");
        }
        // The token's choice is checked against ours; it is never ours.
        JwsAlgorithm algorithm = null;
        for (JwsAlgorithm accepted : algorithms) {
            if (accepted.getName().equals(named)) {
                algorithm = accepted;
                break;
            }
        }
        if (algorithm == null) {
            throw new BadJwtException("The token's algorithm, " + printable((String) named)
                    + ", is not one this decoder accepts");
        }
        if (headers.containsKey("crit")) {
            throw new BadJwtException("The token has a critical header this decoder does not "
                    + "understand");
        }
        Object type = headers.get("typ");
        if (type != null && !"JWT".equalsIgnoreCase(type.toString())
                && !"at+jwt".equalsIgnoreCase(type.toString())) {
            throw new BadJwtException("The token's type, " + printable(type.toString())
                    + ", is not JWT");
        }
        Object keyId = headers.get("kid");
        if (keyId != null && !(keyId instanceof String)) {
            throw new BadJwtException("Malformed token: the key id is not text");
        }
        byte[] signature = Base64Url.decode(token.substring(second + 1));
        if (signature == null || signature.length == 0) {
            throw new BadJwtException("Malformed token: the signature is not base64url");
        }
        byte[] signingInput = Jose.ascii(token.substring(0, second));
        verify(algorithm, (String) keyId, signingInput, signature);

        // Only now is the payload believed enough to be read.
        Map<String, Object> claims = (Map<String, Object>) object(
                token.substring(first + 1, second), "payload");
        String[] times = {JwtClaimNames.EXP, JwtClaimNames.NBF, JwtClaimNames.IAT};
        for (String time : times) {
            Object value = claims.get(time);
            if (value != null && !(value instanceof Number)) {
                throw new BadJwtException("Malformed token: the " + time + " claim is not a number");
            }
        }
        Jwt jwt = new Jwt(token, headers, claims);
        OAuth2TokenValidatorResult result = validator().validate(jwt);
        if (result.hasErrors()) {
            String description = "Unable to validate Jwt";
            for (OAuth2Error error : result.getErrors()) {
                if (error.getDescription() != null && error.getDescription().length() > 0) {
                    description = error.getDescription();
                    break;
                }
            }
            throw new JwtValidationException(description, result.getErrors());
        }
        return jwt;
    }

    private void verify(JwsAlgorithm algorithm, String keyId, byte[] signingInput,
                        byte[] signature) {
        List<Jwk> candidates;
        try {
            candidates = matching(keys.getKeys(), algorithm, keyId);
            if (candidates.isEmpty() && keys instanceof RemoteJwkSet) {
                // A key the issuer started signing with since the set was fetched.
                candidates = matching(((RemoteJwkSet) keys).refresh(), algorithm, keyId);
            }
        } catch (IOException err) {
            throw new JwtException("Could not get the keys to verify the token with: "
                    + err.getMessage(), err);
        }
        if (candidates.isEmpty()) {
            throw new BadJwtException(keyId == null
                    ? "There is no key to verify a " + algorithm.getName() + " token with"
                    : "There is no " + algorithm.getName() + " key with the token's key id, "
                    + printable(keyId));
        }
        // A token that names no key may be signed by any of the right kind; a
        // handful is every key a real issuer has current at once.
        int tried = 0;
        for (Jwk key : candidates) {
            if (tried == 4) {
                break;
            }
            tried++;
            try {
                if (Jose.verify(algorithm, key, signingInput, signature)) {
                    return;
                }
            } catch (IOException err) {
                throw new JwtException("Could not verify the token: " + err.getMessage(), err);
            }
        }
        throw new BadJwtException("The token's signature does not verify");
    }

    private static List<Jwk> matching(List<Jwk> all, JwsAlgorithm algorithm, String keyId) {
        List<Jwk> out = new ArrayList<Jwk>();
        for (Jwk key : all) {
            if (Jose.fits(key, algorithm) && (keyId == null || keyId.equals(key.getKeyId()))) {
                out.add(key);
            }
        }
        return out;
    }

    /// Something out of the token as it is safe to put in a message, which may
    /// end up in a response header: printable ASCII, without quotes, and short.
    private static String printable(String keyId) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0 ; iter < keyId.length() && iter < 64 ; iter++) {
            char c = keyId.charAt(iter);
            sb.append(c >= 0x20 && c < 0x7f && c != '"' && c != '\\' ? c : '?');
        }
        return sb.toString();
    }

    private static Map object(String part, String what) {
        byte[] decoded = Base64Url.decode(part);
        if (decoded == null) {
            throw new BadJwtException("Malformed token: the " + what + " is not base64url");
        }
        try {
            return Json.parseObject(Jose.string(decoded));
        } catch (IOException err) {
            throw new BadJwtException("Malformed token: the " + what + " is not a JSON object",
                    err);
        } catch (RuntimeException err) {
            // Whatever the parser makes of bytes somebody else chose, the answer
            // is this refusal and not a server error.
            throw new BadJwtException("Malformed token: the " + what + " is not a JSON object",
                    err);
        }
    }
}
