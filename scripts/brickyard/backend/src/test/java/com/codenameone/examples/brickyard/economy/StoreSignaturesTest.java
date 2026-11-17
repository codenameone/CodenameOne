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

import com.codename1.backend.Crypto;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The stores' signatures, checked against a chain and keys made here.
class StoreSignaturesTest {
    private final StoreTestKeys keys;

    StoreSignaturesTest() throws IOException {
        keys = new StoreTestKeys();
    }

    private AppleSignedData pinned() {
        return new AppleSignedData(keys.root);
    }

    @Test
    void anAppleRefundSignedThroughThePinnedRootIsRead() throws Exception {
        assertEquals("apple|t-1", StoreRefunds.apple(keys.appleRefund("t-1", StoreTestKeys.BUNDLE), pinned(),
                StoreTestKeys.BUNDLE));
        String renewal = "{\"signedPayload\":\"" + keys.sign("{\"notificationType\":\"DID_RENEW\","
                + "\"data\":{\"bundleId\":\"" + StoreTestKeys.BUNDLE + "\"}}") + "\"}";
        assertNull(StoreRefunds.apple(renewal, pinned(), StoreTestKeys.BUNDLE));
    }

    @Test
    void aPemRootReadsAsItsDer() throws Exception {
        String pem = "-----BEGIN CERTIFICATE-----\n" + com.codename1.backend.Base64.encode(keys.root)
                + "\n-----END CERTIFICATE-----\n";
        assertEquals(keys.root.length, AppleSignedData.certificateBytes(pem.getBytes("UTF-8")).length);
    }

    @Test
    void aTamperedApplePayloadIsRefused() throws Exception {
        String token = keys.sign("{\"notificationType\":\"REFUND\"}");
        int a = token.indexOf('.');
        int b = token.indexOf('.', a + 1);
        String forged = token.substring(0, a + 1)
                + com.codename1.backend.Base64Url.encode("{\"notificationType\":\"REFUNE\"}".getBytes("UTF-8"))
                + token.substring(b);
        assertThrows(IOException.class, () -> pinned().claims(forged));
    }

    @Test
    void aChainEndingInAnotherRootIsRefused() throws Exception {
        StoreTestKeys other = new StoreTestKeys();
        // A complete, valid chain -- just not Apple's.
        assertThrows(IOException.class, () -> pinned().claims(other.sign("{}")));
        // The pinned root appended under someone else's intermediate.
        byte[] otherKey = Crypto.generateEcKey(Crypto.P384);
        byte[] fakeIntermediate = StoreTestKeys.certificate(9,
                com.codename1.backend.security.crypto.Der.publicKeyOf(otherKey), otherKey);
        assertThrows(IOException.class, () -> pinned().claims(
                keys.sign("{}", new byte[][] {keys.leaf, fakeIntermediate, keys.root})));
    }

    @Test
    void aNotificationForAnotherAppIsRefused() throws Exception {
        assertThrows(IOException.class, () -> StoreRefunds.apple(keys.appleRefund("t-2", "com.other.app"),
                pinned(), StoreTestKeys.BUNDLE));
    }

    @Test
    void aGooglePushIsCheckedForKeyAudienceAndSender() throws Exception {
        PubSubPushAuth auth = new PubSubPushAuth(keys.googleKeys(), StoreTestKeys.AUDIENCE, StoreTestKeys.PUSHER);
        auth.check(keys.googleBearer(StoreTestKeys.AUDIENCE));
        assertThrows(IOException.class, () -> auth.check(keys.googleBearer("https://elsewhere.example")));
        assertThrows(IOException.class, () -> auth.check(
                StoreTestKeys.googleBearer(StoreTestKeys.AUDIENCE, Crypto.generateEcKey(Crypto.P256))));
        assertThrows(IOException.class, () -> auth.check(null));
        PubSubPushAuth otherSender = new PubSubPushAuth(keys.googleKeys(), StoreTestKeys.AUDIENCE,
                "someone@else.example");
        assertThrows(IOException.class, () -> otherSender.check(keys.googleBearer(StoreTestKeys.AUDIENCE)));
    }
}
