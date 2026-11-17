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
package com.codename1.backend.storage.gcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.oauth2.client.credentials.GoogleServiceAccountCredentials;
import org.junit.jupiter.api.Test;

/// The V4 signed-URL signer against Google's own conformance vectors
/// (github.com/googleapis/conformance-tests, storage/v1/v4_signatures.json,
/// Apache License 2.0). The vectors' canonical request and string to sign carry
/// no secret, so they are checked exactly; the RSA signature is then checked
/// against a key generated here rather than Google's published dummy key.
class GcsStorageSigningTest {
    private static final String EMAIL = "test-iam-credentials@dummy-project-id.iam.gserviceaccount.com";
    private static final String QUERY_PREFIX = "X-Goog-Algorithm=GOOG4-RSA-SHA256"
            + "&X-Goog-Credential=test-iam-credentials%40dummy-project-id.iam.gserviceaccount.com"
            + "%2F";

    private static long millis(int y, int mo, int d, int h) {
        return (com.codename1.impl.backend.CivilTime.daysFromCivil(y, mo, d) * 86400L
                + h * 3600L) * 1000L;
    }

    private static void vector(String method, String bucket, String object, int expiration,
            long now, String path, String date, String stamp, String hash) throws Exception {
        GcsStorage storage = GcsStorage.forMetadataServer("dummy-project-id");
        String[] parts = storage.signingParts(method, bucket, object, expiration, now, EMAIL);
        assertEquals(path, parts[0]);
        assertEquals(QUERY_PREFIX + date + "%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date="
                + stamp + "&X-Goog-Expires=" + expiration + "&X-Goog-SignedHeaders=host", parts[1]);
        assertEquals("GOOG4-RSA-SHA256\n" + stamp + "\n" + date
                + "/auto/storage/goog4_request\n" + hash, parts[2]);
    }

    @Test
    void simpleGet() throws Exception {
        vector("GET", "test-bucket", "test-object", 10, millis(2019, 2, 1, 9),
                "/test-bucket/test-object", "20190201", "20190201T090000Z",
                "00e2fb794ea93d7adb703edaebdd509821fcc7d4f1a79ac5c8d2b394df109320");
    }

    @Test
    void simplePut() throws Exception {
        vector("PUT", "test-bucket", "test-object", 10, millis(2019, 2, 1, 9),
                "/test-bucket/test-object", "20190201", "20190201T090000Z",
                "78742860705da91404222d5d66ff89850292471199c3c2808d116ad12e6177b4");
    }

    @Test
    void varyExpirationAndTimestamp() throws Exception {
        vector("GET", "test-bucket", "test-object", 20, millis(2019, 3, 1, 9),
                "/test-bucket/test-object", "20190301", "20190301T090000Z",
                "779f19fdb6fd381390e2d5af04947cf21750277ee3c20e0c97b7e46a1dff8907");
    }

    @Test
    void varyBucketAndObject() throws Exception {
        vector("GET", "test-bucket2", "test-object2", 10, millis(2019, 2, 1, 9),
                "/test-bucket2/test-object2", "20190201", "20190201T090000Z",
                "a139afbf35ac30e9864f63197f79609731ab1b0ca166e2a456dba156fcd3f9ce");
    }

    @Test
    void forwardSlashesAreKeptAndReservedCharactersEncoded() throws Exception {
        vector("GET", "test-bucket", "/path/with/slashes/under_score/amper&sand/file.ext", 10,
                millis(2019, 2, 1, 9),
                "/test-bucket//path/with/slashes/under_score/amper%26sand/file.ext", "20190201",
                "20190201T090000Z",
                "63c601ecd6ccfec84f1113fc906609cbdf7651395f4300cecd96ddd2c35164f8");
    }

    @Test
    void signedUrlVerifiesUnderTheServiceAccountsPublicKey() throws Exception {
        byte[] privateKey = Crypto.generateRsaKey(2048);
        StringBuilder pem = new StringBuilder("-----BEGIN PRIVATE KEY-----\\n");
        String b64 = Base64.encode(privateKey);
        for (int i = 0; i < b64.length(); i += 64) {
            pem.append(b64, i, Math.min(b64.length(), i + 64)).append("\\n");
        }
        pem.append("-----END PRIVATE KEY-----\\n");
        GoogleServiceAccountCredentials sa = GoogleServiceAccountCredentials.fromJson(
                "{\"type\":\"service_account\",\"client_email\":\"" + EMAIL
                + "\",\"private_key\":\"" + pem + "\",\"project_id\":\"dummy-project-id\"}");
        GcsStorage storage = GcsStorage.forCredentials(sa, null);
        long now = millis(2019, 2, 1, 9);
        String url = storage.presign("GET", "test-bucket", "test-object", 10, now);
        String[] parts = storage.signingParts("GET", "test-bucket", "test-object", 10, now, EMAIL);
        String prefix = "https://storage.googleapis.com" + parts[0] + "?" + parts[1]
                + "&X-Goog-Signature=";
        assertTrue(url.startsWith(prefix), url);
        byte[] signature = hex(url.substring(prefix.length()));
        assertTrue(Crypto.verify(Crypto.RS256, Der.publicKeyOf(privateKey),
                parts[2].getBytes("UTF-8"), signature));
    }

    @Test
    void refusesAnExpiryPastSevenDays() {
        GcsStorage storage = GcsStorage.forMetadataServer("p");
        assertThrows(java.io.IOException.class,
                () -> storage.signingParts("GET", "bucket", "key", 604801, 0, EMAIL));
    }

    @Test
    void refusesABucketNameThatLeavesItsSegment() {
        GcsStorage storage = GcsStorage.forMetadataServer("p");
        assertThrows(IllegalArgumentException.class,
                () -> storage.signingParts("GET", "evil.example/x", "key", 10, 0, EMAIL));
        assertThrows(IllegalArgumentException.class,
                () -> storage.signingParts("GET", "Upper", "key", 10, 0, EMAIL));
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
