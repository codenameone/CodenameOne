/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.shop;

/// Development and tests: accepts the receipts the simulator's store produces,
/// which carry no signature to check. Refused outside development profiles.
public class SimulatedReceipts implements ReceiptValidator {
    @Override
    public String verify(String store, String sku, String transactionId, String receipt) {
        if (transactionId == null || transactionId.length() == 0 || sku == null) {
            return null;
        }
        return "sim:" + transactionId;
    }
}
