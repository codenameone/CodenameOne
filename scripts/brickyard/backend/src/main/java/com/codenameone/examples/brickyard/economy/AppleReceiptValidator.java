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

import com.codename1.backend.Json;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Checks an App Store receipt with Apple's `verifyReceipt` endpoint: the
/// app's base64 receipt and the app-specific shared secret go to the
/// production endpoint, and to the sandbox when production answers 21007
/// (a TestFlight or development receipt).
///
/// The receipt is good for a transaction when Apple answers status 0, the
/// receipt is this app's (`bundle_id`), and it lists that transaction for
/// that product without a cancellation. The answer is Apple's
/// `transaction_id`, which is what the purchase is keyed by -- the same id
/// the App Store Server Notifications name, so a purchase that reaches the
/// server both ways is credited once.
public final class AppleReceiptValidator implements ReceiptValidator {
    public static final String STORE = "apple";
    public static final String PRODUCTION = "https://buy.itunes.apple.com/verifyReceipt";
    public static final String SANDBOX = "https://sandbox.itunes.apple.com/verifyReceipt";
    /// "This receipt is from the test environment": ask the sandbox instead.
    static final int SANDBOX_RECEIPT = 21007;

    private final String sharedSecret;
    private final String bundleId;
    private final StoreHttp http;

    public AppleReceiptValidator(String sharedSecret, String bundleId, StoreHttp http) {
        this.sharedSecret = sharedSecret;
        this.bundleId = bundleId;
        this.http = http;
    }

    @Override
    public String verify(String store, String sku, String transactionId, String receipt) throws IOException {
        if (!STORE.equalsIgnoreCase(store) || receipt == null || receipt.length() == 0
                || transactionId == null || transactionId.length() == 0) {
            return null;
        }
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("receipt-data", receipt);
        request.put("password", sharedSecret);
        request.put("exclude-old-transactions", Boolean.FALSE);
        String body = Json.write(request);
        Map answer = ask(PRODUCTION, body);
        if (status(answer) == SANDBOX_RECEIPT) {
            answer = ask(SANDBOX, body);
        }
        return decide(answer, bundleId, sku, transactionId);
    }

    private Map ask(String url, String body) throws IOException {
        List<String> headers = new ArrayList<String>();
        headers.add("Content-Type: application/json");
        StoreHttp.Reply r = http.send("POST", url, headers, body);
        if (r.status() >= 500) {
            throw new IOException("The App Store answered " + r.status());
        }
        if (r.status() != 200) {
            return null;
        }
        return Json.parseObject(r.body());
    }

    static int status(Map answer) {
        Object s = answer == null ? null : answer.get("status");
        return s instanceof Number ? ((Number) s).intValue() : -1;
    }

    /// The decision on Apple's answer: the transaction id when it vouches for
    /// `transactionId` of `sku` in this app, null when it does not. A status
    /// Apple documents as temporary (21005, 21100-21199) throws, so the
    /// player can try again instead of being told the receipt is bad.
    static String decide(Map answer, String bundleId, String sku, String transactionId) throws IOException {
        int status = status(answer);
        if (status == 21005 || (status >= 21100 && status <= 21199)) {
            throw new IOException("The App Store could not check the receipt now (" + status + ")");
        }
        if (status != 0 || !(answer.get("receipt") instanceof Map)) {
            return null;
        }
        Map receipt = (Map) answer.get("receipt");
        if (bundleId == null || !bundleId.equals(receipt.get("bundle_id"))) {
            return null;
        }
        List<Object> rows = new ArrayList<Object>();
        if (receipt.get("in_app") instanceof List) {
            rows.addAll((List) receipt.get("in_app"));
        }
        if (answer.get("latest_receipt_info") instanceof List) {
            rows.addAll((List) answer.get("latest_receipt_info"));
        }
        for (Object o : rows) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map t = (Map) o;
            if (transactionId.equals(text(t.get("transaction_id"))) && sku != null
                    && sku.equals(t.get("product_id")) && t.get("cancellation_date_ms") == null
                    && t.get("cancellation_date") == null) {
                return transactionId;
            }
        }
        return null;
    }

    /// Apple writes ids as strings; tolerate a number.
    static String text(Object o) {
        if (o instanceof Number) {
            return String.valueOf(((Number) o).longValue());
        }
        return o instanceof String ? (String) o : null;
    }
}
