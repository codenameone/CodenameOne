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
package com.codenameone.examples.dash.shop;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/// Verifies receipts with the stores themselves.
///
/// - Apple: the receipt is posted to `verifyReceipt` with the app's shared
///   secret; a sandbox receipt (status 21007) is retried against the sandbox.
///   Apple now recommends the App Store Server API for new code; this endpoint
///   still answers, and keeps the sample free of a signing-key setup. Swap the
///   method for a call to `/inApps/v1/transactions/{id}` when you outgrow it.
/// - Google Play: the purchase token is checked with the Play Developer API,
///   authorized by an OAuth access token for a service account with the
///   "View financial data" perlevel (`dash.iap.google.token`). Minting
///   that token from the service account key is left to your deployment.
// tag::dash-store-receipts[]
public class StoreReceipts implements ReceiptValidator {
    static final String APPLE_PRODUCTION = "https://buy.itunes.apple.com/verifyReceipt";
    static final String APPLE_SANDBOX = "https://sandbox.itunes.apple.com/verifyReceipt";

    private final String appleSecret;
    private final String googlePackage;
    private final String googleToken;

    public StoreReceipts(String appleSecret, String googlePackage, String googleToken) {
        this.appleSecret = appleSecret;
        this.googlePackage = googlePackage;
        this.googleToken = googleToken;
    }

    @Override
    public String verify(String store, String sku, String transactionId, String receipt) throws IOException {
        if ("itunes".equals(store)) {
            return apple(sku, transactionId, receipt);
        }
        if ("play".equals(store)) {
            return google(sku, receipt);
        }
        return null;
    }

    private String apple(String sku, String transactionId, String receipt) throws IOException {
        if (appleSecret.length() == 0) {
            return null;
        }
        String body = "{\"receipt-data\":" + Json.write(receipt) + ",\"password\":" + Json.write(appleSecret)
                + ",\"exclude-old-transactions\":true}";
        Map answer = Json.parseObject(Web.postJson(APPLE_PRODUCTION, body, null).getBodyAsString());
        if (answer.get("status") instanceof Number && ((Number) answer.get("status")).intValue() == 21007) {
            answer = Json.parseObject(Web.postJson(APPLE_SANDBOX, body, null).getBodyAsString());
        }
        if (!(answer.get("status") instanceof Number) || ((Number) answer.get("status")).intValue() != 0) {
            return null;
        }
        Object r = answer.get("receipt");
        Object inApp = r instanceof Map ? ((Map) r).get("in_app") : null;
        if (!(inApp instanceof List)) {
            return null;
        }
        for (Object each : (List) inApp) {
            if (each instanceof Map) {
                Map t = (Map) each;
                if (sku.equals(t.get("product_id")) && transactionId != null
                        && transactionId.equals(t.get("transaction_id"))) {
                    return "itunes:" + transactionId;
                }
            }
        }
        return null;
    }

    private String google(String sku, String purchaseToken) throws IOException {
        if (googleToken.length() == 0 || googlePackage.length() == 0 || purchaseToken == null) {
            return null;
        }
        String url = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"
                + googlePackage + "/purchases/products/" + sku + "/tokens/" + purchaseToken;
        Web.Result answer = Web.getJson(url, googleToken);
        if (!answer.isSuccess()) {
            return null;
        }
        Map purchase = Json.parseObject(answer.getBodyAsString());
        Object state = purchase.get("purchaseState");
        Object order = purchase.get("orderId");
        if (!(state instanceof Number) || ((Number) state).intValue() != 0 || !(order instanceof String)) {
            return null;
        }
        return "play:" + order;
    }
}
// end::dash-store-receipts[]
