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

/// Chooses the receipt validator. `brickyard.iap.provider=simulated` accepts
/// every receipt and is honoured on a development or test profile only; on
/// any other profile, and until a store validator is written for this
/// sample, receipts are refused with 503 rather than granted for free.
@Configuration
public class Receipts {
    @Bean
    public ReceiptValidator receiptValidator(Config config,
            @Value("${brickyard.iap.provider:}") String chosen) {
        boolean dev = config.isDevelopmentProfile() || "test".equalsIgnoreCase(config.getProfile());
        String kind = chosen.trim().length() > 0 ? chosen.trim() : dev ? "simulated" : "store";
        if ("simulated".equalsIgnoreCase(kind) && dev) {
            return simulated();
        }
        final String why = "simulated".equalsIgnoreCase(kind)
                ? "brickyard.iap.provider=simulated grants every purchase for free and is refused on this profile"
                : "store receipts are not configured for this server";
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
