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
package com.codenameone.examples.dash.shop;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.dash.Caller;
import com.codenameone.examples.dash.api.AdRewardDto;
import com.codenameone.examples.dash.api.BuyDto;
import com.codenameone.examples.dash.api.EquipDto;
import com.codenameone.examples.dash.api.ReceiptDto;
import com.codenameone.examples.dash.api.ShopApiServer;
import com.codenameone.examples.dash.api.ShopItemDto;
import com.codenameone.examples.dash.api.WalletDto;
import java.util.List;

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
    public WalletDto equip(EquipDto outfit) throws Exception {
        return shop.equip(Caller.name(), outfit == null ? null : outfit.outfit());
    }

    @Override
    public WalletDto receipt(ReceiptDto receipt) throws Exception {
        return shop.receipt(Caller.name(), receipt);
    }

    @Override
    public WalletDto adReward(AdRewardDto reward) throws Exception {
        return shop.adReward(Caller.name());
    }
}
