/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.shop;

import java.io.IOException;

/// Asks a store whether a purchase is real. The server grants nothing on the
/// app's word: a modified client can claim any purchase it likes.
// tag::papercut-receipt-validator[]
public interface ReceiptValidator {
    /// The store's transaction id when `receipt` proves a purchase of `sku`, or
    /// null when it does not.
    String verify(String store, String sku, String transactionId, String receipt) throws IOException;
}
// end::papercut-receipt-validator[]
