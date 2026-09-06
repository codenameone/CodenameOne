/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HS256 JSON Web Tokens: issue one, and verify one you are handed.
 *
 * Only HS256 is accepted, deliberately. A verifier that reads the algorithm out of
 * the token it is checking is the classic JWT hole - "alg":"none" then verifies
 * anything, and "alg":"HS256" against an RSA public key turns a public value into
 * the signing secret. The algorithm is a property of THIS verifier, not of the
 * token, so the header's alg is checked for agreement and never used to select
 * anything.
 */
public final class Jwt {
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private Jwt() {
    }

    /** Thrown for any token that is not valid and current. */
    public static final class InvalidTokenException extends IOException {
        InvalidTokenException(String message) {
            super(message);
        }
    }

    /**
     * - `claims`: the payload; "iat" and "exp" are set here and overwrite anything
     *   the caller put there
     * - `ttlSeconds`: how long the token is good for
     */
    public static String issue(Map claims, byte[] secret, long ttlSeconds) throws IOException {
        if(secret == null || secret.length < 32) {
            // A short secret makes HS256 brute-forceable offline, and there is no
            // reason to allow one when generating a good one is a single call.
            throw new IOException("The signing secret must be at least 32 bytes");
        }
        long now = System.currentTimeMillis() / 1000L;
        Map payload = new LinkedHashMap();
        if(claims != null) {
            payload.putAll(claims);
        }
        payload.put("iat", new Long(now));
        payload.put("exp", new Long(now + ttlSeconds));
        String signingInput = Base64Url.encode(Crypto.utf8(HEADER))
                + "." + Base64Url.encode(Crypto.utf8(Json.write(payload)));
        byte[] mac = Crypto.hmacSha256(secret, Crypto.utf8(signingInput));
        if(mac == null) {
            throw new IOException("Could not sign the token");
        }
        return signingInput + "." + Base64Url.encode(mac);
    }

    /**
     * Returns the claims of a token that is well-formed, correctly signed and not
     * expired. Throws otherwise; there is no "valid but expired" return.
     */
    public static Map verify(String token, byte[] secret) throws IOException {
        if(token == null || secret == null) {
            throw new InvalidTokenException("No token");
        }
        int firstDot = token.indexOf('.');
        int secondDot = firstDot < 0 ? -1 : token.indexOf('.', firstDot + 1);
        if(firstDot <= 0 || secondDot <= firstDot || token.indexOf('.', secondDot + 1) >= 0) {
            throw new InvalidTokenException("Malformed token");
        }
        String signingInput = token.substring(0, secondDot);
        byte[] provided = Base64Url.decode(token.substring(secondDot + 1));
        byte[] expected = Crypto.hmacSha256(secret, Crypto.utf8(signingInput));
        if(provided == null || expected == null
                || !Crypto.equalsConstantTime(expected, provided)) {
            // One message for every signature failure: distinguishing "bad
            // signature" from "unknown key" tells an attacker which half to work on.
            throw new InvalidTokenException("Bad signature");
        }
        byte[] headerBytes = Base64Url.decode(token.substring(0, firstDot));
        byte[] payloadBytes = Base64Url.decode(token.substring(firstDot + 1, secondDot));
        if(headerBytes == null || payloadBytes == null) {
            throw new InvalidTokenException("Malformed token");
        }
        Map header;
        Map payload;
        try {
            header = Json.parseObject(new String(headerBytes, "UTF-8"));
            payload = Json.parseObject(new String(payloadBytes, "UTF-8"));
        } catch (Exception err) {
            throw new InvalidTokenException("Malformed token");
        }
        // Checked for agreement, never used to choose an algorithm.
        if(!"HS256".equals(header.get("alg"))) {
            throw new InvalidTokenException("Unsupported algorithm");
        }
        Object exp = payload.get("exp");
        if(!(exp instanceof Number)) {
            throw new InvalidTokenException("Token has no expiry");
        }
        if(((Number)exp).longValue() <= System.currentTimeMillis() / 1000L) {
            throw new InvalidTokenException("Token expired");
        }
        return payload;
    }

    /** Pulls the token out of an "Authorization: Bearer ..." header. */
    public static String bearer(String authorizationHeader) {
        if(authorizationHeader == null) {
            return null;
        }
        String prefix = "bearer ";
        if(authorizationHeader.length() <= prefix.length()
                || !authorizationHeader.substring(0, prefix.length()).toLowerCase().equals(prefix)) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }
}
