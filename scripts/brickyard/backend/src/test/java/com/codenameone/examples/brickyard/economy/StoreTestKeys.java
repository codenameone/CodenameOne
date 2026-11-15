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
package com.codenameone.examples.brickyard.economy;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// A store of the test's own: an Apple-shaped chain (P-384 root and
/// intermediate, P-256 leaf, hand-built DER certificates) that signs JWS
/// tokens, and a Google-shaped key that signs Pub/Sub OIDC tokens.
public final class StoreTestKeys {
    public static final String BUNDLE = "com.codenameone.brickyard";
    public static final String AUDIENCE = "https://brickyard.test/api/store/notifications/google";
    public static final String PUSHER = "pubsub@brickyard.iam.gserviceaccount.com";
    private static final byte[] ECDSA_SHA384 = {0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x04, 0x03, 0x03};

    public final byte[] root;
    public final byte[] intermediate;
    public final byte[] leaf;
    private final byte[] leafKey;
    private final byte[] googleKey;

    public StoreTestKeys() throws IOException {
        byte[] rootKey = Crypto.generateEcKey(Crypto.P384);
        byte[] interKey = Crypto.generateEcKey(Crypto.P384);
        leafKey = Crypto.generateEcKey(Crypto.P256);
        googleKey = Crypto.generateEcKey(Crypto.P256);
        root = certificate(1, Der.publicKeyOf(rootKey), rootKey);
        intermediate = certificate(2, Der.publicKeyOf(interKey), rootKey);
        leaf = certificate(3, Der.publicKeyOf(leafKey), interKey);
    }

    /// A certificate for `subjectKey` signed with ecdsa-with-SHA384 by `issuerKey`.
    public static byte[] certificate(int serial, byte[] subjectKey, byte[] issuerKey) throws IOException {
        byte[] alg = tlv(0x30, tlv(0x06, ECDSA_SHA384));
        byte[] tbs = tlv(0x30, tlv(0xa0, tlv(0x02, new byte[] {2})), tlv(0x02, new byte[] {(byte) serial}), alg,
                tlv(0x30), tlv(0x30, tlv(0x17, ascii("260101000000Z")), tlv(0x17, ascii("360101000000Z"))),
                tlv(0x30), subjectKey);
        byte[] sig = Crypto.sign(Crypto.ES384, issuerKey, tbs);
        byte[] bits = new byte[sig.length + 1];
        System.arraycopy(sig, 0, bits, 1, sig.length);
        return tlv(0x30, tbs, alg, tlv(0x03, bits));
    }

    /// A JWS of `json` signed by the leaf, carrying the chain.
    public String sign(String json) throws IOException {
        return sign(json, new byte[][] {leaf, intermediate, root});
    }

    public String sign(String json, byte[][] chain) throws IOException {
        StringBuilder x5c = new StringBuilder();
        for (byte[] c : chain) {
            x5c.append(x5c.length() == 0 ? "" : ",").append('"').append(Base64.encode(c)).append('"');
        }
        String input = Base64Url.encode(("{\"alg\":\"ES256\",\"x5c\":[" + x5c + "]}").getBytes("UTF-8"))
                + "." + Base64Url.encode(json.getBytes("UTF-8"));
        byte[] sig = Der.ecdsaDerToJose(Crypto.sign(Crypto.ES256, leafKey, ascii(input)), 32);
        return input + "." + Base64Url.encode(sig);
    }

    /// An App Store Server Notification refunding `transactionId` for `bundle`.
    public String appleRefund(String transactionId, String bundle) throws IOException {
        String txn = sign("{\"transactionId\":\"" + transactionId + "\",\"bundleId\":\"" + bundle + "\"}");
        return "{\"signedPayload\":\"" + sign("{\"notificationType\":\"REFUND\",\"data\":{\"bundleId\":\""
                + bundle + "\",\"signedTransactionInfo\":\"" + txn + "\"}}") + "\"}";
    }

    /// The keys Google would publish.
    public JwkSet googleKeys() throws IOException {
        return JwkSet.of(Jwk.ofPublicKey(Der.publicKeyOf(googleKey)).withKeyId("g1"));
    }

    /// An `Authorization` header as Pub/Sub sends it.
    public String googleBearer(String audience) throws IOException {
        return googleBearer(audience, googleKey);
    }

    public static String googleBearer(String audience, byte[] key) throws IOException {
        long now = System.currentTimeMillis() / 1000;
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(PubSubPushAuth.ISSUER).audience(audience)
                .subject("1234").issuedAt(now).expiresAt(now + 600).claim("email", PUSHER)
                .claim("email_verified", Boolean.TRUE).build();
        DefaultJwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(Jwk.ofPrivateKey(key).withKeyId("g1")));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).keyId("g1").build(), claims)).getTokenValue();
    }

    /// A Pub/Sub push body voiding `orderId`.
    public static String googleVoided(String orderId) throws IOException {
        return "{\"message\":{\"data\":\"" + Base64.encode(("{\"voidedPurchaseNotification\":{\"orderId\":\""
                + orderId + "\"}}").getBytes("UTF-8")) + "\"}}";
    }

    private static byte[] ascii(String s) throws IOException {
        return s.getBytes("US-ASCII");
    }

    private static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            body.write(p, 0, p.length);
        }
        int n = body.size();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (n < 0x80) {
            out.write(n);
        } else if (n < 0x100) {
            out.write(0x81);
            out.write(n);
        } else {
            out.write(0x82);
            out.write(n >> 8);
            out.write(n & 0xff);
        }
        byte[] b = body.toByteArray();
        out.write(b, 0, b.length);
        return out.toByteArray();
    }
}
