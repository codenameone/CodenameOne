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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.dash.api.BuyDto;
import com.codenameone.examples.dash.api.EquipDto;
import com.codenameone.examples.dash.api.ShopItemDto;
import com.codenameone.examples.dash.api.WalletDto;
import com.codenameone.examples.dash.engage.Store;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import java.util.List;

/// The shop. Gems and the Royal Pass are real-money purchases through the
/// platform store ([Store]); runners, trails and emotes cost coins or gems and are
/// bought from the game's own server. Either way the server holds the wallet.
public final class ShopForm {
    private ShopForm() {
    }

    public static void show() {
        final Form f = Ui.form("Shop", "shop");
        Ui.back(f, Nav::home);
        Store.listen(wallet -> {
            Ui.toast("Purchase complete");
            Session.refreshUser(() -> Ui.later(ShopForm::show), Ui::failed);
        });
        Api.shop().wallet(Net.to(wallet -> Api.shop().items(Net.to(items -> Ui.later(() -> {
            if (Session.user() == null) {
                // Signed out while the shop was loading.
                return;
            }
            f.add(Ui.wallet(Session.user()));
            fill(f, items, wallet);
            f.revalidate();
        }), Ui::failed)), Ui::failed));
        f.show();
    }

    private static void fill(Form f, List<ShopItemDto> items, WalletDto wallet) {
        for (final ShopItemDto item : items) {
            boolean owned = wallet.owned() != null && wallet.owned().contains(item.id());
            Container row = new Container(new BorderLayout());
            row.setUIID("ShopItem");
            row.add(BorderLayout.CENTER, new Label(item.name(), "ShopItemName"));
            if (item.sku() != null && item.sku().length() > 0) {
                if (owned) {
                    row.add(BorderLayout.EAST, new Label("Owned", "Owned"));
                } else {
                    row.add(BorderLayout.EAST, Ui.button("Buy", "GemButton", "buy-" + item.id(), e -> Store.buy(item.sku())));
                }
            } else if (owned) {
                boolean on = item.id().equals(wallet.outfit());
                row.add(BorderLayout.EAST, item.id().startsWith("trail-") && !on
                        ? Ui.button("Use", "StoneButton", "equip-" + item.id(), e -> equip(item.id()))
                        : new Label(on ? "In use" : "Owned", "Owned"));
            } else {
                String price = item.priceGems() > 0 ? item.priceGems() + " gems" : item.priceCoins() + " coins";
                row.add(BorderLayout.EAST, Ui.button(price, "PriceButton", "buy-" + item.id(), e -> buy(item)));
            }
            f.add(row);
        }
    }

    private static void buy(ShopItemDto item) {
        Api.shop().buy(new BuyDto(item.id()), Net.to(wallet -> {
            Telemetry.itemBought(item.id(), item.priceGems() > 0 ? "gems" : "coins");
            Session.refreshUser(() -> Ui.later(ShopForm::show), Ui::failed);
        }, Ui::failed));
    }

    private static void equip(String outfit) {
        Api.shop().equip(new EquipDto(outfit), Net.to(wallet -> Session.refreshUser(() -> Ui.later(ShopForm::show),
                Ui::failed), Ui::failed));
    }
}
