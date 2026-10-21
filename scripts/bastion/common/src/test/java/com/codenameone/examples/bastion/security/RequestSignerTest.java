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
package com.codenameone.examples.bastion.security;

import com.codenameone.examples.bastion.api.SignedRequest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The signature the app sends is the one the server checks.
///
/// The vector below is also in the server's `SigningVectorTest`: the same
/// request, the same canonical text. If either side's form changes, one of the
/// two fails.
class RequestSignerTest {
    static final String BODY = "{\"amount\":1500,\"memo\":\"rent\"}";
    static final String NONCE = "AAECAwQFBgcICQoLDA0ODw";
    static final long TS = 1767225600000L;
    static final String CANONICAL = "POST|/api/accounts/a1/withdraw|"
            + "27faf49a231437b9d9308db1799c1ab06d8665c087da17478d59acbefa20a125|"
            + NONCE + "|" + TS;

    @Test
    void canonicalFormMatchesTheServers() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(BODY.getBytes("UTF-8"));
        assertEquals(CANONICAL, SignedRequest.canonical("POST", "/api/accounts/a1/withdraw",
                digest, NONCE, TS));
        // 16 bytes in base64url, the length RequestSigner.nonce() makes.
        assertTrue(SignedRequest.nonceShaped(NONCE));
    }

    @Test
    void headerVerifiesWithTheJdkAsTheServerDoes() throws Exception {
        byte[] seed = new byte[48];
        for (int iter = 0; iter < seed.length; iter++) {
            seed[iter] = (byte) (iter * 7 + 3);
        }
        byte[] privateKey = P256.newPrivateKey(seed);
        String header = RequestSigner.header("dev1", privateKey, "POST",
                "/api/accounts/a1/withdraw", BODY.getBytes("UTF-8"), NONCE, TS);
        assertTrue(header.startsWith("v1;device=dev1;ts=" + TS + ";nonce=" + NONCE + ";sig="),
                header);
        byte[] jose = Base64.getUrlDecoder().decode(SignedRequest.field(header, "sig"));
        assertEquals(64, jose.length);

        PublicKey key = KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(P256.publicKey(privateKey)));
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(key);
        verifier.update(CANONICAL.getBytes("UTF-8"));
        assertTrue(verifier.verify(der(jose)));
    }

    /// JOSE `r || s` to the DER the JDK verifies.
    static byte[] der(byte[] jose) {
        byte[] r = new BigInteger(1, java.util.Arrays.copyOfRange(jose, 0, 32)).toByteArray();
        byte[] s = new BigInteger(1, java.util.Arrays.copyOfRange(jose, 32, 64)).toByteArray();
        byte[] out = new byte[6 + r.length + s.length];
        out[0] = 0x30;
        out[1] = (byte) (4 + r.length + s.length);
        out[2] = 0x02;
        out[3] = (byte) r.length;
        System.arraycopy(r, 0, out, 4, r.length);
        out[4 + r.length] = 0x02;
        out[5 + r.length] = (byte) s.length;
        System.arraycopy(s, 0, out, 6 + r.length, s.length);
        return out;
    }
}
