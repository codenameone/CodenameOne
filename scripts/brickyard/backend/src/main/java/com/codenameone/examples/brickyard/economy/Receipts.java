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

import com.codename1.backend.Config;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Value;
import java.io.IOException;

/// Chooses the receipt validator.
///
/// `brickyard.iap.provider=simulated` accepts every receipt and is honoured
/// on a development or test profile only. Otherwise each store is checked
/// with the store itself, once it is configured:
///
/// | Store | Settings |
/// |---|---|
/// | `apple` | `brickyard.iap.apple.shared-secret`, `brickyard.iap.apple.bundle-id` |
/// | `google` | `brickyard.iap.google.package-name`, `brickyard.iap.google.service-account-key` (a path to the JSON key) |
///
/// A store that is not configured answers 503 with a message naming the
/// settings, rather than granting anything for free.
@Configuration
public class Receipts {
    @Bean
    public ReceiptValidator receiptValidator(Config config,
            @Value("${brickyard.iap.provider:}") String chosen,
            @Value("${brickyard.iap.apple.shared-secret:}") String appleSecret,
            @Value("${brickyard.iap.apple.bundle-id:}") String appleBundle,
            @Value("${brickyard.iap.google.package-name:}") String googlePackage,
            @Value("${brickyard.iap.google.service-account-key:}") String googleKey) throws IOException {
        boolean dev = config.isDevelopmentProfile() || "test".equalsIgnoreCase(config.getProfile());
        String kind = chosen.trim().length() > 0 ? chosen.trim() : dev ? "simulated" : "store";
        if ("simulated".equalsIgnoreCase(kind) && dev) {
            return simulated();
        }
        if ("simulated".equalsIgnoreCase(kind)) {
            return refusing("brickyard.iap.provider=simulated grants every purchase for free and is refused "
                    + "on this profile");
        }
        StoreHttp http = StoreHttp.web();
        ReceiptValidator apple = appleSecret.trim().length() > 0 && appleBundle.trim().length() > 0
                ? new AppleReceiptValidator(appleSecret.trim(), appleBundle.trim(), http)
                : refusing("App Store receipts are not configured for this server: set "
                        + "brickyard.iap.apple.shared-secret and brickyard.iap.apple.bundle-id");
        ReceiptValidator google = googlePackage.trim().length() > 0 && googleKey.trim().length() > 0
                ? GooglePlayValidator.fromKeyFile(googlePackage.trim(), googleKey.trim(), http)
                : refusing("Google Play receipts are not configured for this server: set "
                        + "brickyard.iap.google.package-name and brickyard.iap.google.service-account-key");
        return byStore(apple, google);
    }

    /// Sends each receipt to its store's validator; an unknown store is not a receipt.
    public static ReceiptValidator byStore(final ReceiptValidator apple, final ReceiptValidator google) {
        return (store, sku, transactionId, receipt) -> {
            if (AppleReceiptValidator.STORE.equalsIgnoreCase(store)) {
                return apple.verify(store, sku, transactionId, receipt);
            }
            if (GooglePlayValidator.STORE.equalsIgnoreCase(store)) {
                return google.verify(store, sku, transactionId, receipt);
            }
            return null;
        };
    }

    static ReceiptValidator refusing(final String why) {
        return (store, sku, transactionId, receipt) -> {
            throw new ResponseStatusException(503, why);
        };
    }

    public static ReceiptValidator simulated() {
        return (store, sku, transactionId, receipt) ->
                transactionId == null || transactionId.length() == 0 || sku == null
                        ? null : "sim:" + transactionId;
    }
}
