package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;

/// Something the shop sells. `kind` is gems, arena or emotes; `sku` is set for real-money items.
@Mapped
public record ShopItemDto(String id, String name, String kind, int priceCoins, int priceGems, String sku) {
}
