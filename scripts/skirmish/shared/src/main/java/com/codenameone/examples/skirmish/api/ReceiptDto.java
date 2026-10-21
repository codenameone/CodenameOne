package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// A store purchase for the server to verify before it grants anything. `receipt` is the store's proof: the App Store receipt or the Play purchase token.
@Mapped
public record ReceiptDto(String store, String sku, String transactionId, String receipt) {
}
