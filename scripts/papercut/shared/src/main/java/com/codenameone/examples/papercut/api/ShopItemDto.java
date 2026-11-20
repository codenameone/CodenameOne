/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.api;

import com.codename1.annotations.Mapped;

/// Something the shop sells. `kind` is QUILL (a paper for the quill), ERASERS (two extra
/// erasers for the next run), or COINS (ink coins for real money, which has a `sku`).
@Mapped
public record ShopItemDto(String id, String name, String kind, int priceCoins, String sku, String colour) {
}
