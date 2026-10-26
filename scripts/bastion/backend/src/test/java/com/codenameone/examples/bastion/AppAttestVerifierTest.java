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

import com.codename1.backend.security.pki.Certificate;
import com.codenameone.examples.bastion.money.AppAttestVerifier;
import com.codenameone.examples.bastion.money.IntegrityVerifier;
import com.codenameone.examples.bastion.money.SimulatedIntegrityVerifier;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Synthetic App Attest attestations, made with openssl: a test root, an
/// intermediate and a credential certificate carrying the nonce extension,
/// in the CBOR attestation object the iOS port sends as
/// `cn1aa1:attest:<keyId>:<attestation>`. The development AAGUID.
class AppAttestVerifierTest {
    // The fixtures hash this exact id. It is assembled so the Initializr's
    // project rename, which rewrites the app's name in every source, leaves
    // it as the fixtures were made with.
    private static final String APP_ID = "TEAMID1234.com.example." + "bas" + "tion";
    private static final String NONCE = "device-public-key";

    private static final String TEST_ROOT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIBazCCARKgAwIBAgIUFCuN/C9j1v7YLkN/Q1iuOCHHn+AwCgYIKoZIzj0EAwIw\n"
            + "EzERMA8GA1UEAwwIVGVzdFJvb3QwIBcNMjYxMDI1MTI1ODMzWhgPMjEyNjEwMDEx\n"
            + "MjU4MzNaMBMxETAPBgNVBAMMCFRlc3RSb290MFkwEwYHKoZIzj0CAQYIKoZIzj0D\n"
            + "AQcDQgAEgnY5dBqACvUFXzRRkdhg3tSUkPVaE8sUuhBKX+oaKdDVFQtdPW5lmc/h\n"
            + "Qk/3K3dxm2JTyFLLyvy1dcABvx3m+qNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n"
            + "HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFJXaF+vRtzcaCq5pNsmxGeKGffTIMAoGCCqG\n"
            + "SM49BAMCA0cAMEQCICtqDF0mBd0BN2ZcbgZI0r1bK/vpQ6aU8XSFPyRgAPbwAiBc\n"
            + "8tfdJ3WjL9EkxZkJRuzMLz6SEGxdbYmfjGvoYrzS0g==\n"
            + "-----END CERTIFICATE-----\n";

    private static final String OTHER_ROOT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIBbjCCARSgAwIBAgIUUnE9PKarZYEmo83h1w19GCADFp8wCgYIKoZIzj0EAwIw\n"
            + "FDESMBAGA1UEAwwJT3RoZXJSb290MCAXDTI2MTAyNTEyNTgzM1oYDzIxMjYxMDAx\n"
            + "MTI1ODMzWjAUMRIwEAYDVQQDDAlPdGhlclJvb3QwWTATBgcqhkjOPQIBBggqhkjO\n"
            + "PQMBBwNCAASYNd3bfGbybnhzwIsqCqaoPn4fvTKUtYZU07Cw70v2bWqgghokdVJY\n"
            + "sYm5UYYyMZyvGW29gmS/6fPB63E0KvyEo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4G\n"
            + "A1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUDrkXLKhtN+0As47lAhydXU+ZD40wCgYI\n"
            + "KoZIzj0EAwIDSAAwRQIgZplx43JBWbGba7WI/MsWcS4opy+HZ5pRWZ5GGuw7bwcC\n"
            + "IQDX9tYh24kHwW67nPbCkPax8I8nKCvSxbTsnupW7aooQg==\n"
            + "-----END CERTIFICATE-----\n";

    /// Counter 0: everything as Apple produces it.
    private static final String GOOD = "cn1aa1:attest:SSKj1P4iTdvpXXp0K601Th5rNQ7ShK3megPft6I3Hhc=:o2NmbXRvYXBwbGUtYXBwY"
            + "XR0ZXN0Z2F0dFN0bXSiY3g1Y4JZAbEwggGtMIIBVKADAgECAhRSocUBeYildqff1mzq7VJAqxdyPDAKB"
            + "ggqhkjOPQQDAjAUMRIwEAYDVQQDDAlUZXN0SW50ZXIwIBcNMjYxMDI1MTI1ODMzWhgPMjEyNjEwMDExM"
            + "jU4MzNaMA8xDTALBgNVBAMMBGxlYWYwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARXFNjR/EiYc8Zgk"
            + "mI+3xGvRYQHuJUMMnA3JP8GjQumltWHXFYUf1a99P9gs+Sznu/jLyo05QlEOwvAmFnfErOio4GGMIGDM"
            + "AwGA1UdEwEB/wQCMAAwMwYJKoZIhvdjZAgCBCYwJKEiBCAjWAmklU6fZ7nJVr7S8LN0KHxTi6KpFXnEY"
            + "uhtPujQhTAdBgNVHQ4EFgQUcY+7dxPyXNxrs4MGSZ2x4nY9JTYwHwYDVR0jBBgwFoAU8A72D3/cA89NF"
            + "uuoUmf9nO2jcrkwCgYIKoZIzj0EAwIDRwAwRAIgc2I2drA0mn7Awxh7kTscXmqk4wpPLhPifMS7IY7TA"
            + "ZMCIHyE0y1AbAd8YEGybGoixo9LwDG8KUw4h1ouc4TY5VjNWQGSMIIBjjCCATSgAwIBAgIUIVVPPbDFo"
            + "TNSpWv7kU7FYTk3F6wwCgYIKoZIzj0EAwIwEzERMA8GA1UEAwwIVGVzdFJvb3QwIBcNMjYxMDI1MTI1O"
            + "DMzWhgPMjEyNjEwMDExMjU4MzNaMBQxEjAQBgNVBAMMCVRlc3RJbnRlcjBZMBMGByqGSM49AgEGCCqGS"
            + "M49AwEHA0IABOjPY1CQ0Brrys7QhtMbLHUAYjWz9CpukyGlV6IRtO9y5+aE89saVw5JQkzAXgIiSQ0vi"
            + "zC9frm0zOwNITGY4WGjYzBhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWB"
            + "BTwDvYPf9wDz00W66hSZ/2c7aNyuTAfBgNVHSMEGDAWgBSV2hfr0bc3GgquaTbJsRnihn30yDAKBggqh"
            + "kjOPQQDAgNIADBFAiEAvcbvxZXC4oMbX4Olq4X5kglvE1/95K+4WiXa91+Va/gCIDdt/RhWo6mLAAbX0"
            + "SvhGU/LrnTTl85fm4V+VivRzIMkZ3JlY2VpcHRHcmVjZWlwdGhhdXRoRGF0YVhYW41yuv86eHAmJ3EKc"
            + "RFuqMIRTAYW5JAo051065aUxeNBAAAAAGFwcGF0dGVzdGRldmVsb3AAIEkio9T+Ik3b6V16dCutNU4ea"
            + "zUO0oSt5noD37eiNx4XoA==";

    /// Counter 1, with a nonce extension that matches it, so only the counter is wrong.
    private static final String COUNTER_ONE = "cn1aa1:attest:XG0vuIuoKuZ72DoGYnQ7l0vWTqbC7EfMhYT07oMes3k=:o2NmbXRvYXBwbGUtYXBwY"
            + "XR0ZXN0Z2F0dFN0bXSiY3g1Y4JZAbMwggGvMIIBVaADAgECAhRSocUBeYildqff1mzq7VJAqxdyPTAKB"
            + "ggqhkjOPQQDAjAUMRIwEAYDVQQDDAlUZXN0SW50ZXIwIBcNMjYxMDI1MTI1ODMzWhgPMjEyNjEwMDExM"
            + "jU4MzNaMBAxDjAMBgNVBAMMBWxlYWYyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnUnp/GgqgSPC+"
            + "JmMKu0TxHvlxOO0DrN2+2sdy1iINom3KIliwbQ8pV6vYkjhkfugtcGnjLeuRgZiGewbp1c6o6OBhjCBg"
            + "zAMBgNVHRMBAf8EAjAAMDMGCSqGSIb3Y2QIAgQmMCShIgQgf3qIXjuwWkQWtEsC65bxm81IruSV/BayB"
            + "leLkIUP75cwHQYDVR0OBBYEFD12b6ZNMPegs86h63jzU852Ea3yMB8GA1UdIwQYMBaAFPAO9g9/3APPT"
            + "RbrqFJn/Zzto3K5MAoGCCqGSM49BAMCA0gAMEUCIQCGy0O6TpEO8hh7K/6HzNtTdrwgqJu4Ft5neCVzg"
            + "vGrCwIgGy6qIIoHiImNi67wlzTBlg/PSccoEt8op+cG3LA5s/5ZAZIwggGOMIIBNKADAgECAhQhVU89s"
            + "MWhM1Kla/uRTsVhOTcXrDAKBggqhkjOPQQDAjATMREwDwYDVQQDDAhUZXN0Um9vdDAgFw0yNjEwMjUxM"
            + "jU4MzNaGA8yMTI2MTAwMTEyNTgzM1owFDESMBAGA1UEAwwJVGVzdEludGVyMFkwEwYHKoZIzj0CAQYIK"
            + "oZIzj0DAQcDQgAE6M9jUJDQGuvKztCG0xssdQBiNbP0Km6TIaVXohG073Ln5oTz2xpXDklCTMBeAiJJD"
            + "S+LML1+ubTM7A0hMZjhYaNjMGEwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OB"
            + "BYEFPAO9g9/3APPTRbrqFJn/Zzto3K5MB8GA1UdIwQYMBaAFJXaF+vRtzcaCq5pNsmxGeKGffTIMAoGC"
            + "CqGSM49BAMCA0gAMEUCIQC9xu/FlcLigxtfg6WrhfmSCW8TX/3kr7haJdr3X5Vr+AIgN239GFajqYsAB"
            + "tfRK+EZT8uudNOXzl+bhX5WK9HMgyRncmVjZWlwdEdyZWNlaXB0aGF1dGhEYXRhWFhbjXK6/zp4cCYnc"
            + "QpxEW6owhFMBhbkkCjTnXTrlpTF40EAAAABYXBwYXR0ZXN0ZGV2ZWxvcAAgXG0vuIuoKuZ72DoGYnQ7l"
            + "0vWTqbC7EfMhYT07oMes3mg";

    private static AppAttestVerifier verifier(String root, String appId) throws Exception {
        return new AppAttestVerifier(Collections.singletonList(Certificate.fromPem(root)), appId,
                false, SimulatedIntegrityVerifier.forProfile(true));
    }

    private static boolean attested(AppAttestVerifier v, String token, String nonce) throws Exception {
        return v.verify("ios", token, nonce, false, true).attested();
    }

    @Test
    void acceptsAValidAttestation() throws Exception {
        assertTrue(attested(verifier(TEST_ROOT, APP_ID), GOOD, NONCE));
    }

    @Test
    void rejectsTheWrongNonce() throws Exception {
        assertFalse(attested(verifier(TEST_ROOT, APP_ID), GOOD, "another-key"));
        assertFalse(attested(verifier(TEST_ROOT, APP_ID), GOOD, null));
    }

    @Test
    void rejectsTheWrongAppId() throws Exception {
        assertFalse(attested(verifier(TEST_ROOT, "OTHERTEAM.com.example.bastion"), GOOD, NONCE));
    }

    @Test
    void rejectsAnUntrustedRoot() throws Exception {
        assertFalse(attested(verifier(OTHER_ROOT, APP_ID), GOOD, NONCE));
        assertFalse(attested(AppAttestVerifier.apple(APP_ID, false,
                SimulatedIntegrityVerifier.forProfile(true)), GOOD, NONCE));
    }

    @Test
    void rejectsANonZeroCounter() throws Exception {
        assertFalse(attested(verifier(TEST_ROOT, APP_ID), COUNTER_ONE, NONCE));
    }

    @Test
    void rejectsTheProductionAaguidMismatch() throws Exception {
        AppAttestVerifier production = new AppAttestVerifier(
                Collections.singletonList(Certificate.fromPem(TEST_ROOT)), APP_ID, true,
                SimulatedIntegrityVerifier.forProfile(true));
        assertFalse(attested(production, GOOD, NONCE));
    }

    @Test
    void rejectsMalformedTokensAndAssertions() throws Exception {
        AppAttestVerifier v = verifier(TEST_ROOT, APP_ID);
        assertFalse(attested(v, "cn1aa1:attest:AAAA:AAAA", NONCE));
        assertFalse(attested(v, "cn1aa1:assert:AAAA:AAAA:AAAA", NONCE));
        assertFalse(attested(v, GOOD.substring(0, GOOD.length() - 40), NONCE));
    }

    @Test
    void otherPlatformsGoToTheDelegate() throws Exception {
        IntegrityVerifier.Verdict verdict = verifier(TEST_ROOT, APP_ID)
                .verify("android", "token", NONCE, false, true);
        assertTrue(verdict.attested());
        assertNotNull(Certificate.fromPem(AppAttestVerifier.APPLE_APP_ATTESTATION_ROOT_CA));
    }
}
