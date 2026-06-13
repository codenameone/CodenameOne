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
package com.codename1.social;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link FirebaseAuth.FirebaseUser}, constructed directly through
 * its package-private constructor (the test shares the {@code social} package).
 * Exercises both the refresh-flow (snake_case) and sign-in-flow (camelCase)
 * field mappings, the expiry parsing helper, and the id-token claim decoding /
 * email fallback.
 */
class FirebaseUserTest {

    /** Builds a compact JWT (header.payload.signature) whose payload is the
     * given JSON. Only the payload segment is meaningful to the decoder. */
    private static String jwt(String payloadJson) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes("UTF-8"));
            return "eyJhbGciOiJub25lIn0." + payload + ".sig";
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void signInFlowMapsCamelCaseFields() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("idToken", jwt("{\"sub\":\"u1\"}"));
        json.put("refreshToken", "refresh-abc");
        json.put("localId", "uid-123");
        json.put("email", "user@example.com");
        json.put("expiresIn", "3600");

        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertEquals("refresh-abc", u.getRefreshToken());
        assertEquals("uid-123", u.getUid());
        assertEquals("user@example.com", u.getEmail());
        assertNotNull(u.getIdToken());
        assertNotNull(u.getExpiresAt());
        // ~now + 3600s; allow generous slack for slow CI.
        long delta = u.getExpiresAt().getTime() - System.currentTimeMillis();
        assertTrue(delta > 3000_000L && delta <= 3600_000L, "expiry should be roughly an hour out");
    }

    @Test
    void refreshFlowMapsSnakeCaseFields() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("id_token", jwt("{\"email\":\"claims@example.com\"}"));
        json.put("refresh_token", "refresh-xyz");
        json.put("user_id", "uid-999");
        json.put("expires_in", "3600");

        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, true);
        assertEquals("refresh-xyz", u.getRefreshToken());
        assertEquals("uid-999", u.getUid());
        assertNotNull(u.getExpiresAt());
        // No explicit email field in the refresh flow -> falls back to the claim.
        assertEquals("claims@example.com", u.getEmail());
    }

    @Test
    void missingExpiryYieldsNullExpiresAt() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("localId", "u");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertNull(u.getExpiresAt());
    }

    @Test
    void zeroExpirySecondsYieldsNullExpiresAt() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("expiresIn", "0");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertNull(u.getExpiresAt());
    }

    @Test
    void decimalExpiresInIsTruncatedNotRejected() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("expires_in", "3600.9");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, true);
        assertNotNull(u.getExpiresAt());
    }

    @Test
    void nonNumericExpiresInYieldsNullExpiresAt() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("expiresIn", "not-a-number");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertNull(u.getExpiresAt());
    }

    @Test
    void nullIdTokenYieldsNullClaimsAndNullEmail() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("localId", "u");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertNull(u.getIdToken());
        assertNull(u.getIdTokenClaims());
        assertNull(u.getEmail());
    }

    @Test
    void malformedIdTokenYieldsEmptyClaims() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("idToken", "this-is-not-a-jwt");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertNotNull(u.getIdTokenClaims());
        assertTrue(u.getIdTokenClaims().isEmpty());
        // No explicit email and no decodable claims -> null.
        assertNull(u.getEmail());
    }

    @Test
    void explicitEmailWinsOverClaimEmail() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("idToken", jwt("{\"email\":\"claims@example.com\"}"));
        json.put("email", "explicit@example.com");
        FirebaseAuth.FirebaseUser u = new FirebaseAuth.FirebaseUser(json, false);
        assertEquals("explicit@example.com", u.getEmail());
    }
}
