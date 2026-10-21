package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// Buying a shop item with coins or gems.
@Mapped
public record BuyDto(String itemId) {
}
