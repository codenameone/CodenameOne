/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// A store purchase for the server to verify before it grants anything. `receipt` is the
/// store's proof: the App Store receipt or the Play purchase token.
@Mapped
public record ReceiptDto(String store, String sku, String transactionId, String receipt) {
}
