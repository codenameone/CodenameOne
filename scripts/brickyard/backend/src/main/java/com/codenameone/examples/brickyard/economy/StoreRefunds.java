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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Json;
import java.io.IOException;
import java.util.Map;

/// Reads the refunds out of the stores' own notification formats: App Store
/// Server Notifications V2 (a JWS `signedPayload` whose
/// `data.signedTransactionInfo` is a second JWS) and a Google Play real-time
/// developer notification pushed by Pub/Sub (`message.data`, base64 JSON).
///
/// Only refunds are acted on. A purchase or renewal is credited when the app
/// posts its receipt, which the server checks with the store; a notification
/// of the same purchase would credit nothing new. A refund is the one event
/// only the store knows about.
///
/// The JWS signatures are not checked here: the endpoint is authenticated by
/// the secret in its URL, which only the store's console knows. Checking
/// Apple's x5c chain against Apple Root CA G3 is the hardening step for a
/// production deployment.
final class StoreRefunds {
    private StoreRefunds() {
    }

    /// The purchase id ("apple|txn") an Apple notification refunds, or null
    /// when it is not a refund.
    static String apple(String body) throws IOException {
        Map outer = Json.parseObject(body);
        Map payload = jws(outer.get("signedPayload"));
        if (payload == null || !"REFUND".equals(payload.get("notificationType"))) {
            return null;
        }
        Object data = payload.get("data");
        Map txn = data instanceof Map ? jws(((Map) data).get("signedTransactionInfo")) : null;
        Object id = txn == null ? null : txn.get("transactionId");
        return id instanceof String && ((String) id).length() > 0
                ? AppleReceiptValidator.STORE + "|" + id : null;
    }

    /// The purchase id ("google|order") a Google notification refunds, or
    /// null when it is not a voided purchase.
    static String google(String body) throws IOException {
        Map push = Json.parseObject(body);
        Object message = push.get("message");
        Object data = message instanceof Map ? ((Map) message).get("data") : null;
        byte[] raw = data instanceof String ? Base64Url.decode(toUrl((String) data)) : null;
        if (raw == null) {
            throw new IOException("Not a Pub/Sub push");
        }
        Map n = Json.parseObject(new String(raw, "UTF-8"));
        Object voided = n.get("voidedPurchaseNotification");
        Object order = voided instanceof Map ? ((Map) voided).get("orderId") : null;
        return order instanceof String && ((String) order).length() > 0
                ? GooglePlayValidator.STORE + "|" + order : null;
    }

    /// The claims of a compact JWS, or null for anything that is not one.
    private static Map jws(Object token) throws IOException {
        if (!(token instanceof String)) {
            return null;
        }
        String t = (String) token;
        int a = t.indexOf('.');
        int b = a < 0 ? -1 : t.indexOf('.', a + 1);
        if (b < 0) {
            return null;
        }
        byte[] claims = Base64Url.decode(t.substring(a + 1, b));
        return claims == null ? null : Json.parseObject(new String(claims, "UTF-8"));
    }

    /// Pub/Sub sends standard, padded base64; [Base64Url] reads the URL alphabet.
    static String toUrl(String b64) {
        StringBuilder sb = new StringBuilder(b64.length());
        for (int i = 0; i < b64.length(); i++) {
            char c = b64.charAt(i);
            if (c == '+') {
                sb.append('-');
            } else if (c == '/') {
                sb.append('_');
            } else if (c != '=') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
