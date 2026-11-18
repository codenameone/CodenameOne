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
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codenameone.examples.brickyard.Clock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Checks a Google Play purchase token with the Play Developer API, signed
/// in as a service account (its JSON key file: `client_email`,
/// `private_key`, `token_uri`).
///
/// A one-time product is good when `purchases.products` answers
/// `purchaseState` 0 (purchased); Plus, a subscription, when
/// `purchases.subscriptionsv2` answers an active or grace-period state with
/// a line item for the SKU. The answer is the Play order id (the latest one
/// for a subscription, so each renewal is a purchase of its own), which is
/// the purchase's key whichever way it reached the server.
public final class GooglePlayValidator implements ReceiptValidator {
    public static final String STORE = "google";
    public static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    static final String API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/";

    private final String packageName;
    private final String clientEmail;
    private final byte[] privateKey;
    private final String tokenUri;
    private final StoreHttp http;
    private String accessToken;
    private long accessTokenExpires;

    public GooglePlayValidator(String packageName, String serviceAccountJson, StoreHttp http) throws IOException {
        Map key = Json.parseObject(serviceAccountJson);
        this.packageName = packageName;
        this.clientEmail = (String) key.get("client_email");
        String pem = (String) key.get("private_key");
        if (clientEmail == null || pem == null) {
            throw new IOException("The service account key needs client_email and private_key");
        }
        this.privateKey = KeyFiles.privateKey(pem);
        Object uri = key.get("token_uri");
        this.tokenUri = uri instanceof String ? (String) uri : "https://oauth2.googleapis.com/token";
        this.http = http;
    }

    /// Reads the service account key from `path`.
    public static GooglePlayValidator fromKeyFile(String packageName, String path, StoreHttp http)
            throws IOException {
        return new GooglePlayValidator(packageName, KeyFiles.read(path), http);
    }

    @Override
    public String verify(String store, String sku, String transactionId, String receipt) throws IOException {
        if (!STORE.equalsIgnoreCase(store) || sku == null || receipt == null || receipt.length() == 0) {
            return null;
        }
        boolean subscription = Economy.PLUS_SKU.equals(sku);
        String url = API + OAuth2Parameters.encode(packageName) + (subscription
                ? "/purchases/subscriptionsv2/tokens/" + OAuth2Parameters.encode(receipt)
                : "/purchases/products/" + OAuth2Parameters.encode(sku) + "/tokens/"
                        + OAuth2Parameters.encode(receipt));
        List<String> headers = new ArrayList<String>();
        headers.add("Accept: application/json");
        headers.add("Authorization: Bearer " + token());
        StoreHttp.Reply r = http.send("GET", url, headers, null);
        if (r.status() >= 500 || r.status() == 401 || r.status() == 403) {
            // 401/403 is our credential, not the player's purchase.
            synchronized (this) {
                accessToken = null;
            }
            throw new IOException("Google Play answered " + r.status());
        }
        if (r.status() != 200) {
            return null;
        }
        Map answer = Json.parseObject(r.body());
        String order = subscription ? decideSubscription(answer, sku) : decideProduct(answer);
        if (order == null) {
            return null;
        }
        // An order id the app sent has to be the one Google names.
        if (!subscription && transactionId != null && transactionId.length() > 0
                && !transactionId.equals(order)) {
            return null;
        }
        return order;
    }

    /// A one-time purchase: purchased (not cancelled or pending) and with an order id.
    static String decideProduct(Map answer) {
        Object state = answer == null ? null : answer.get("purchaseState");
        if (!(state instanceof Number) || ((Number) state).intValue() != 0) {
            return null;
        }
        Object order = answer.get("orderId");
        return order instanceof String && ((String) order).length() > 0 ? (String) order : null;
    }

    /// A subscription: live, with a line item for `sku`; the latest order id.
    static String decideSubscription(Map answer, String sku) {
        if (answer == null) {
            return null;
        }
        Object state = answer.get("subscriptionState");
        if (!"SUBSCRIPTION_STATE_ACTIVE".equals(state) && !"SUBSCRIPTION_STATE_IN_GRACE_PERIOD".equals(state)) {
            return null;
        }
        boolean found = false;
        if (answer.get("lineItems") instanceof List) {
            for (Object o : (List) answer.get("lineItems")) {
                if (o instanceof Map && sku.equals(((Map) o).get("productId"))) {
                    found = true;
                }
            }
        }
        Object order = answer.get("latestOrderId");
        return found && order instanceof String && ((String) order).length() > 0 ? (String) order : null;
    }

    /// An OAuth access token for the service account, kept until a minute
    /// before it runs out.
    synchronized String token() throws IOException {
        long now = Clock.now();
        if (accessToken != null && now < accessTokenExpires) {
            return accessToken;
        }
        String form = "grant_type=" + OAuth2Parameters.encode("urn:ietf:params:oauth:grant-type:jwt-bearer")
                + "&assertion=" + OAuth2Parameters.encode(assertion(now));
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/x-www-form-urlencoded");
        StoreHttp.Reply r = http.send("POST", tokenUri, headers, form);
        if (r.status() != 200) {
            throw new IOException("Google refused the service account (" + r.status() + ")");
        }
        Map answer = Json.parseObject(r.body());
        Object token = answer.get("access_token");
        if (!(token instanceof String)) {
            throw new IOException("Google answered no access token");
        }
        long life = answer.get("expires_in") instanceof Number ? ((Number) answer.get("expires_in")).longValue() : 3600;
        accessToken = (String) token;
        accessTokenExpires = now + Math.max(0, life - 60) * 1000;
        return accessToken;
    }

    /// The signed JWT a service account trades for an access token.
    String assertion(long now) throws IOException {
        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        claims.put("iss", clientEmail);
        claims.put("scope", SCOPE);
        claims.put("aud", tokenUri);
        claims.put("iat", Long.valueOf(now / 1000));
        claims.put("exp", Long.valueOf(now / 1000 + 3600));
        String input = Base64Url.encode(Json.write(header).getBytes("UTF-8")) + "."
                + Base64Url.encode(Json.write(claims).getBytes("UTF-8"));
        return input + "." + Base64Url.encode(Crypto.sign(Crypto.RS256, privateKey, input.getBytes("UTF-8")));
    }
}
