/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;

import java.io.IOException;

/// Device keys for the tests, and signing with them.
///
/// Fixed P-256 pairs rather than generated ones: the tests also run compiled
/// by ParparVM, whose class library has no `java.security`, and the backend's
/// [Crypto] signs and verifies but does not make EC keys. Handed out in turn,
/// so two calls in a row answer two different keys. Test keys only: they
/// are published here and sign nothing but test requests.
final class TestKeys {
    private static final String[][] PAIRS = {
            {"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgju8FJ0vDrHAxiMt3KOvLi4KDkfETw83GixJWm0GYXqmhRANCAARdjfihuTDUWlL-iKJnQJyaFA5pHj4u5_MbSjY9AUMMePOgkkWbOyR7fwzXBDZb6R-05yF9LT6_iuJGk9YOrW2P",
             "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEXY34obkw1FpS_oiiZ0CcmhQOaR4-LufzG0o2PQFDDHjzoJJFmzske38M1wQ2W-kftOchfS0-v4riRpPWDq1tjw"},
            {"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgd3hPSEVByOnMtUgtycFhiUjN5AFfqunFuT0YEW80fFahRANCAASAVNBQBONoXyJIHUZ1x8FyUe5IkEcjDE-Y0RQ93mxFyRbP9dUt9SC3KZf7S8k_NMxc3djq8Am1F-a58i-AUJ31",
             "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgFTQUATjaF8iSB1GdcfBclHuSJBHIwxPmNEUPd5sRckWz_XVLfUgtymX-0vJPzTMXN3Y6vAJtRfmufIvgFCd9Q"},
            {"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg9xBmzTdupOUDuBwEL2KPX18MNAZBTQsrR9g3qaeeKhShRANCAAQ00wLx_sW8pYko5GaHuwE05bLAb-fSr1rH8CuRvU1tZMqZqnJW2IHNNsmuS3twnYiMS_rrXL0_bVXgUlh7h4WC",
             "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENNMC8f7FvKWJKORmh7sBNOWywG_n0q9ax_Arkb1NbWTKmapyVtiBzTbJrkt7cJ2IjEv661y9P21V4FJYe4eFgg"},
            {"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEtrnraBY3Cf7pW8j4NjQJfcVM5dMEgPwuiPAtW2iJAWhRANCAATr5WFd6HhZpCeZpqZIhKqGcONPrRVWpmAx8sSYePYc1IAGbqmsiRf77nmiW0l31dCovDiDEWZtjypF4Q0QFTfo",
             "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE6-VhXeh4WaQnmaamSISqhnDjT60VVqZgMfLEmHj2HNSABm6prIkX--55oltJd9XQqLw4gxFmbY8qReENEBU36A"}
    };
    private static int next;

    final byte[] privateKey;
    final byte[] publicKey;

    private TestKeys(byte[] privateKey, byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    static synchronized TestKeys next() {
        String[] pair = PAIRS[next++ % PAIRS.length];
        return new TestKeys(Base64Url.decode(pair[0]), Base64Url.decode(pair[1]));
    }

    /// The public key as the server takes it: X.509 SubjectPublicKeyInfo, base64url.
    String publicText() {
        return Base64Url.encode(publicKey);
    }

    /// ES256 over `text`, as JOSE r||s.
    byte[] sign(byte[] text) throws IOException {
        return Der.ecdsaDerToJose(Crypto.sign(Crypto.ES256, privateKey, text), 32);
    }

    /// A random hex id of 32 characters, where a test wants a fresh key.
    static String random() {
        try {
            return Ids.next();
        } catch (IOException noRandom) {
            throw new IllegalStateException(noRandom);
        }
    }
}
