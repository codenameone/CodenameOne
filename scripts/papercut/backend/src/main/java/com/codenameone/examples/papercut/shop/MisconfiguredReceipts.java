/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.shop;

import com.codename1.backend.ResponseStatusException;

/// Stands in when the store settings are missing, so the server still starts and
/// says what is wrong instead of granting purchases it cannot check.
public class MisconfiguredReceipts implements ReceiptValidator {
    private final String problem;

    public MisconfiguredReceipts(String problem) {
        this.problem = problem;
        System.err.println("[config] " + problem);
    }

    @Override
    public String verify(String store, String sku, String transactionId, String receipt) {
        throw new ResponseStatusException(503, "Purchases are not available right now");
    }
}
