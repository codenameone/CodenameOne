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
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The store validators against canned store answers; nothing leaves the JVM.
class StoreValidatorTest {
    /// Answers each request with the next canned reply and remembers the URLs.
    static final class Canned implements StoreHttp {
        final List<Reply> replies = new ArrayList<Reply>();
        final List<String> urls = new ArrayList<String>();

        Canned then(int status, String body) {
            replies.add(new Reply(status, body));
            return this;
        }

        @Override
        public Reply send(String method, String url, List<String> headers, String body) {
            urls.add(url);
            return replies.remove(0);
        }
    }

    private static String apple(int status, String bundle, String txn, String sku, boolean cancelled) {
        return "{\"status\":" + status + ",\"receipt\":{\"bundle_id\":\"" + bundle + "\",\"in_app\":[{"
                + "\"transaction_id\":\"" + txn + "\",\"product_id\":\"" + sku + "\""
                + (cancelled ? ",\"cancellation_date_ms\":\"1\"" : "") + "}]}}";
    }

    @Test
    void appleAcceptsAMatchingReceiptAndRetriesTheSandbox() throws IOException {
        Canned http = new Canned().then(200, "{\"status\":21007}")
                .then(200, apple(0, "com.example.brickyard", "t-1", "bricks.400", false));
        AppleReceiptValidator v = new AppleReceiptValidator("secret", "com.example.brickyard", http);
        assertEquals("t-1", v.verify("apple", "bricks.400", "t-1", "receipt-data"));
        assertEquals(2, http.urls.size());
        assertTrue(http.urls.get(1).contains("sandbox"));
    }

    @Test
    void appleRefusesTheWrongAppProductTransactionOrARefund() throws IOException {
        String b = "com.example.brickyard";
        assertNull(AppleReceiptValidator.decide(Json.parseObject(apple(0, "other.app", "t", "s", false)), b, "s", "t"));
        assertNull(AppleReceiptValidator.decide(Json.parseObject(apple(0, b, "t", "other", false)), b, "s", "t"));
        assertNull(AppleReceiptValidator.decide(Json.parseObject(apple(0, b, "t2", "s", false)), b, "s", "t"));
        assertNull(AppleReceiptValidator.decide(Json.parseObject(apple(0, b, "t", "s", true)), b, "s", "t"));
        assertNull(AppleReceiptValidator.decide(Json.parseObject("{\"status\":21003}"), b, "s", "t"));
        // Apple's own trouble is an error to retry, not a refusal.
        assertThrows(IOException.class,
                () -> AppleReceiptValidator.decide(Json.parseObject("{\"status\":21005}"), b, "s", "t"));
        assertThrows(IOException.class, () -> new AppleReceiptValidator("x", b, new Canned().then(503, ""))
                .verify("apple", "s", "t", "r"));
    }

    private static String serviceAccount() throws IOException {
        String der = Base64.encode(Crypto.generateRsaKey(2048));
        StringBuilder pem = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < der.length(); i += 64) {
            pem.append(der, i, Math.min(der.length(), i + 64)).append('\n');
        }
        pem.append("-----END PRIVATE KEY-----\n");
        Map<String, Object> key = new LinkedHashMap<String, Object>();
        key.put("client_email", "verifier@example.iam.gserviceaccount.com");
        key.put("private_key", pem.toString());
        key.put("token_uri", "https://oauth2.example/token");
        return Json.write(key);
    }

    @Test
    void googleSignsInOnceAndChecksProductsAndSubscriptions() throws IOException {
        Canned http = new Canned().then(200, "{\"access_token\":\"at-1\",\"expires_in\":3600}")
                .then(200, "{\"purchaseState\":0,\"orderId\":\"GPA.1\"}")
                .then(200, "{\"subscriptionState\":\"SUBSCRIPTION_STATE_ACTIVE\",\"latestOrderId\":\"GPA.2\","
                        + "\"lineItems\":[{\"productId\":\"" + Economy.PLUS_SKU + "\"}]}");
        GooglePlayValidator v = new GooglePlayValidator("com.example.brickyard", serviceAccount(), http);
        assertEquals("GPA.1", v.verify("google", "bricks.400", "GPA.1", "token-1"));
        assertEquals("GPA.2", v.verify("google", Economy.PLUS_SKU, "GPA.2", "token-2"));
        assertEquals("https://oauth2.example/token", http.urls.get(0));
        assertTrue(http.urls.get(1).contains("/products/bricks.400/tokens/token-1"), http.urls.get(1));
        assertTrue(http.urls.get(2).contains("subscriptionsv2"), http.urls.get(2));
        assertEquals(3, http.urls.size());
    }

    @Test
    void googleRefusesPendingCancelledAndExpiredPurchases() throws IOException {
        assertNull(GooglePlayValidator.decideProduct(Json.parseObject("{\"purchaseState\":2,\"orderId\":\"o\"}")));
        assertNull(GooglePlayValidator.decideProduct(Json.parseObject("{\"purchaseState\":1,\"orderId\":\"o\"}")));
        assertNull(GooglePlayValidator.decideSubscription(Json.parseObject(
                "{\"subscriptionState\":\"SUBSCRIPTION_STATE_EXPIRED\",\"latestOrderId\":\"o\","
                        + "\"lineItems\":[{\"productId\":\"p\"}]}"), "p"));
        assertNull(GooglePlayValidator.decideSubscription(Json.parseObject(
                "{\"subscriptionState\":\"SUBSCRIPTION_STATE_ACTIVE\",\"latestOrderId\":\"o\","
                        + "\"lineItems\":[{\"productId\":\"other\"}]}"), "p"));
        Canned http = new Canned().then(200, "{\"access_token\":\"at\",\"expires_in\":3600}").then(401, "{}");
        GooglePlayValidator v = new GooglePlayValidator("com.example.brickyard", serviceAccount(), http);
        assertThrows(IOException.class, () -> v.verify("google", "bricks.400", "o", "t"));
    }
}
