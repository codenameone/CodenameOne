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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.CN;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.splat.api.BuyDto;
import com.codenameone.examples.splat.api.EquipDto;
import com.codenameone.examples.splat.api.ShopItemDto;
import com.codenameone.examples.splat.api.WalletDto;
import com.codenameone.examples.splat.engage.Store;
import com.codenameone.examples.splat.engage.Telemetry;
import com.codenameone.examples.splat.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.splat.net.Session;
import java.util.List;

/// The shop. Gems and the Royal Pass are real-money purchases through the
/// platform store ([Store]); markers, paint and comms packs cost coins or gems and are
/// bought from the game's own server. Either way the server holds the wallet.
public final class ShopForm {
    private ShopForm() {
    }

    public static void show() {
        final Form f = Ui.form("ARMORY", "shop");
        Ui.desktopColumn(f);
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
            // The wallet just fetched, not the player cached at sign-in:
            // the shop is where the balance has to be right.
            f.add(Ui.wallet(Session.user().trophies(), wallet.coins(), wallet.gems()));
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
            Container text = new Container(BoxLayout.y());
            text.add(Ui.text(item.name(), "ShopItemName"));
            text.add(new Label(kindOf(item), "ShopItemKind"));
            row.add(BorderLayout.CENTER, text);
            if (item.sku() != null && item.sku().length() > 0) {
                if (owned) {
                    row.add(BorderLayout.EAST, FlowLayout.encloseMiddle(new Label("OWNED", "Owned")));
                } else {
                    String price = Store.price(item.sku());
                    row.add(BorderLayout.EAST, FlowLayout.encloseMiddle(Ui.button(price == null ? "BUY" : price,
                            // no price means the store has not answered; amber
                            // is kept for an offer that can be bought now
                            price == null ? "GhostButton" : "GemButton", "buy-" + item.id(),
                            e -> Store.buy(item.sku()))));
                }
            } else if (owned) {
                boolean on = item.id().equals(wallet.outfit());
                row.add(BorderLayout.EAST, FlowLayout.encloseMiddle(item.id().startsWith("paint-") && !on
                        ? Ui.button("EQUIP", "GhostButton", "equip-" + item.id(), e -> equip(item.id()))
                        : new Label(on ? "EQUIPPED" : "OWNED", "Owned")));
            } else {
                String price = item.priceGems() > 0 ? item.priceGems() + " GEMS" : item.priceCoins() + " COINS";
                row.add(BorderLayout.EAST, FlowLayout.encloseMiddle(Ui.button(price, "PriceButton", "buy-" + item.id(),
                        e -> buy(item))));
            }
            f.add(row);
        }
    }

    private static String kindOf(ShopItemDto item) {
        if ("gems-500".equals(item.id())) {
            // The biggest pack is the one that is cheapest per gem.
            return "BEST VALUE";
        }
        String kind = item.kind() == null ? "" : item.kind();
        if ("runner".equals(kind)) {
            return "MARKER";
        }
        if ("outfit".equals(kind)) {
            return "PAINT";
        }
        if ("emotes".equals(kind)) {
            return "COMMS PACK";
        }
        if ("gems".equals(kind)) {
            return "GEM PACK";
        }
        return "pass".equals(kind) ? "SEASON PASS" : "ITEM";
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
