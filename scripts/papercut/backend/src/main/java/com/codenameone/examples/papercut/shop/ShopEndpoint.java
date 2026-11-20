/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.shop;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.papercut.api.AdRewardDto;
import com.codenameone.examples.papercut.api.BuyDto;
import com.codenameone.examples.papercut.api.EquipDto;
import com.codenameone.examples.papercut.api.ReceiptDto;
import com.codenameone.examples.papercut.api.ShopApiServer;
import com.codenameone.examples.papercut.api.ShopItemDto;
import com.codenameone.examples.papercut.api.WalletDto;
import java.util.List;

/// `ShopApi` on the server.
@Component
public class ShopEndpoint implements ShopApiServer {
    private final Shop shop;

    public ShopEndpoint(Shop shop) {
        this.shop = shop;
    }

    @Override
    public List<ShopItemDto> items() throws Exception {
        return Catalog.items();
    }

    @Override
    public WalletDto wallet() throws Exception {
        return shop.wallet(Caller.name());
    }

    @Override
    public WalletDto buy(BuyDto item) throws Exception {
        return shop.buy(Caller.name(), item == null ? null : item.itemId());
    }

    @Override
    public WalletDto equip(EquipDto quill) throws Exception {
        return shop.equip(Caller.name(), quill == null ? null : quill.quill());
    }

    @Override
    public WalletDto receipt(ReceiptDto receipt) throws Exception {
        return shop.receipt(Caller.name(), receipt);
    }

    @Override
    public WalletDto adReward(AdRewardDto reward) throws Exception {
        return shop.adReward(Caller.name(), reward);
    }

    @Override
    public WalletDto useErasers() throws Exception {
        return shop.useErasers(Caller.name());
    }
}
